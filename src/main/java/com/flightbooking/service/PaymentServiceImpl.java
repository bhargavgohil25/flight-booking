package com.flightbooking.service;

import com.flightbooking.exception.BookingExpiredException;
import com.flightbooking.exception.BookingNotFoundException;
import com.flightbooking.exception.PaymentException;
import com.flightbooking.model.entity.Booking;
import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.entity.Payment;
import com.flightbooking.model.enums.BookingStatus;
import com.flightbooking.model.enums.PaymentMethod;
import com.flightbooking.model.enums.PaymentStatus;
import com.flightbooking.model.enums.SeatStatus;
import com.flightbooking.model.request.PaymentRequest;
import com.flightbooking.model.response.PaymentResponse;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.repository.PaymentRepository;
import com.flightbooking.strategy.PaymentProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final PaymentRepository paymentRepository;
    private final Map<PaymentMethod, PaymentProcessor> processors;
    private final ConcurrentHashMap<UUID, ReentrantLock> bookingLocks = new ConcurrentHashMap<>();

    public PaymentServiceImpl(BookingRepository bookingRepository,
                              FlightRepository flightRepository,
                              PaymentRepository paymentRepository,
                              List<PaymentProcessor> processorList) {
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
        this.paymentRepository = paymentRepository;
        this.processors = processorList.stream()
                .collect(Collectors.toMap(PaymentProcessor::getPaymentMethod, Function.identity()));
    }

    @Override
    public PaymentResponse processPayment(UUID bookingId, PaymentRequest request) {
        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));

        ReentrantLock lock = bookingLocks.computeIfAbsent(bookingId, k -> new ReentrantLock());
        lock.lock();
        try {
            // Re-read under lock to get fresh state
            booking = bookingRepository.findByBookingId(bookingId).orElseThrow();

            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                throw new PaymentException("Booking is already paid and confirmed");
            }
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                throw new BookingExpiredException("Booking has been cancelled due to payment timeout");
            }
            if (booking.getStatus() == BookingStatus.PAYMENT_FAILED) {
                throw new PaymentException("Booking payment has failed. Please create a new booking");
            }
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                throw new PaymentException("Booking is not in a payable state: " + booking.getStatus());
            }

            // Check deadline
            if (booking.getPaymentDeadline() != null && LocalDateTime.now().isAfter(booking.getPaymentDeadline())) {
                cancelBookingAndReleaseSeats(booking);
                throw new BookingExpiredException("Payment deadline has expired. Booking has been cancelled");
            }

            PaymentProcessor processor = processors.get(request.paymentMethod());
            if (processor == null) {
                throw new PaymentException("Unsupported payment method: " + request.paymentMethod());
            }

            // Validate payment-method-specific fields
            processor.validate(request);

            // Create payment record in PENDING
            Payment payment = Payment.builder()
                    .paymentId(UUID.randomUUID())
                    .bookingId(bookingId)
                    .amount(booking.getTotalPrice())
                    .paymentMethod(request.paymentMethod())
                    .status(PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(payment);

            // Process payment (simulated)
            try {
                String transactionRef = processor.processPayment(booking.getTotalPrice(), request);

                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setTransactionReference(transactionRef);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setPaymentId(payment.getPaymentId());
                bookingRepository.save(booking);

                log.info("Payment successful for booking {}: txn={}, method={}", bookingId, transactionRef, request.paymentMethod());

                return toResponse(payment, booking);
            } catch (Exception e) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                booking.setStatus(BookingStatus.PAYMENT_FAILED);
                bookingRepository.save(booking);

                releaseSeats(booking);

                log.error("Payment failed for booking {}: {}", bookingId, e.getMessage());
                throw new PaymentException("Payment processing failed: " + e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    private void cancelBookingAndReleaseSeats(Booking booking) {
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        releaseSeats(booking);
        log.info("Booking {} cancelled due to payment expiry, seats released", booking.getBookingId());
    }

    private void releaseSeats(Booking booking) {
        flightRepository.findByFlightNumber(booking.getFlightNumber()).ifPresent(flight -> {
            for (String seat : booking.getAllocatedSeats()) {
                flight.getSeatMap().put(seat, SeatStatus.AVAILABLE);
            }
            log.info("Released seats {} on flight {}", booking.getAllocatedSeats(), booking.getFlightNumber());
        });
    }

    private PaymentResponse toResponse(Payment payment, Booking booking) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getBookingId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getTransactionReference(),
                booking.getStatus(),
                payment.getUpdatedAt()
        );
    }
}
