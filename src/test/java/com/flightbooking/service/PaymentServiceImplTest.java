package com.flightbooking.service;

import com.flightbooking.exception.BookingExpiredException;
import com.flightbooking.exception.BookingNotFoundException;
import com.flightbooking.exception.PaymentException;
import com.flightbooking.model.entity.Booking;
import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.*;
import com.flightbooking.model.request.PaymentRequest;
import com.flightbooking.model.response.PaymentResponse;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.repository.PaymentRepository;
import com.flightbooking.strategy.CardPaymentProcessor;
import com.flightbooking.strategy.GiftCardPaymentProcessor;
import com.flightbooking.strategy.PaymentProcessor;
import com.flightbooking.strategy.UpiPaymentProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private FlightRepository flightRepository;
    @Mock private PaymentRepository paymentRepository;

    private PaymentServiceImpl paymentService;
    private UUID bookingId;
    private Booking pendingBooking;

    @BeforeEach
    void setUp() {
        List<PaymentProcessor> processors = List.of(
                new UpiPaymentProcessor(),
                new CardPaymentProcessor(),
                new GiftCardPaymentProcessor()
        );
        paymentService = new PaymentServiceImpl(bookingRepository, flightRepository, paymentRepository, processors);

        bookingId = UUID.randomUUID();
        pendingBooking = Booking.builder()
                .bookingId(bookingId)
                .flightNumber("EK502")
                .passengerName("John")
                .passengerEmail("john@test.com")
                .numberOfSeats(2)
                .allocatedSeats(List.of("1A", "1B"))
                .totalPrice(new BigDecimal("50000.00"))
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentDeadline(LocalDateTime.now().plusMinutes(10))
                .build();
    }

    @Test
    void processPayment_upi_success() {
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentRequest request = new PaymentRequest(PaymentMethod.UPI, "user@upi", null, null);
        PaymentResponse response = paymentService.processPayment(bookingId, request);

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.transactionReference()).startsWith("UPI-");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    void processPayment_card_success() {
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD, null, "4111111111111111", null);
        PaymentResponse response = paymentService.processPayment(bookingId, request);

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.transactionReference()).startsWith("CARD-");
    }

    @Test
    void processPayment_giftCard_success() {
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentRequest request = new PaymentRequest(PaymentMethod.GIFT_CARD, null, null, "GIFT12345678");
        PaymentResponse response = paymentService.processPayment(bookingId, request);

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.transactionReference()).startsWith("GC-");
    }

    @Test
    void processPayment_bookingNotFound_throws() {
        UUID unknownId = UUID.randomUUID();
        when(bookingRepository.findByBookingId(unknownId)).thenReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest(PaymentMethod.UPI, "user@upi", null, null);

        assertThatThrownBy(() -> paymentService.processPayment(unknownId, request))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void processPayment_alreadyConfirmed_throws() {
        pendingBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));

        PaymentRequest request = new PaymentRequest(PaymentMethod.UPI, "user@upi", null, null);

        assertThatThrownBy(() -> paymentService.processPayment(bookingId, request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void processPayment_cancelled_throws() {
        pendingBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));

        PaymentRequest request = new PaymentRequest(PaymentMethod.UPI, "user@upi", null, null);

        assertThatThrownBy(() -> paymentService.processPayment(bookingId, request))
                .isInstanceOf(BookingExpiredException.class);
    }

    @Test
    void processPayment_expired_cancelsAndThrows() {
        pendingBooking.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight flight = Flight.builder().flightNumber("EK502").totalSeats(12).build();
        flight.generateSeatMap();
        flight.getSeatMap().put("1A", SeatStatus.BOOKED);
        flight.getSeatMap().put("1B", SeatStatus.BOOKED);
        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));

        PaymentRequest request = new PaymentRequest(PaymentMethod.UPI, "user@upi", null, null);

        assertThatThrownBy(() -> paymentService.processPayment(bookingId, request))
                .isInstanceOf(BookingExpiredException.class)
                .hasMessageContaining("expired");

        assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(flight.getSeatMap().get("1A")).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(flight.getSeatMap().get("1B")).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void processPayment_upiWithoutId_throws() {
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));

        PaymentRequest request = new PaymentRequest(PaymentMethod.UPI, null, null, null);

        assertThatThrownBy(() -> paymentService.processPayment(bookingId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UPI ID is required");
    }

    @Test
    void processPayment_cardWithInvalidNumber_throws() {
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD, null, "123", null);

        assertThatThrownBy(() -> paymentService.processPayment(bookingId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid card number");
    }

    @Test
    void processPayment_giftCardWithShortCode_throws() {
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));

        PaymentRequest request = new PaymentRequest(PaymentMethod.GIFT_CARD, null, null, "ABC");

        assertThatThrownBy(() -> paymentService.processPayment(bookingId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid gift card code");
    }

    @Test
    void processPayment_paymentFailed_throws() {
        pendingBooking.setStatus(BookingStatus.PAYMENT_FAILED);
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));

        PaymentRequest request = new PaymentRequest(PaymentMethod.UPI, "user@upi", null, null);

        assertThatThrownBy(() -> paymentService.processPayment(bookingId, request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("payment has failed");
    }

    @Test
    void processPayment_responseContainsCorrectBookingId() {
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentRequest request = new PaymentRequest(PaymentMethod.UPI, "user@upi", null, null);
        PaymentResponse response = paymentService.processPayment(bookingId, request);

        assertThat(response.bookingId()).isEqualTo(bookingId);
        assertThat(response.paymentId()).isNotNull();
        assertThat(response.processedAt()).isNotNull();
    }

    @Test
    void processPayment_amountMatchesBookingTotalPrice() {
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD, null, "4111111111111111", null);
        PaymentResponse response = paymentService.processPayment(bookingId, request);

        assertThat(response.amount()).isEqualByComparingTo(pendingBooking.getTotalPrice());
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void processPayment_upiInvalidFormat_throws() {
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));

        PaymentRequest request = new PaymentRequest(PaymentMethod.UPI, "no-at-symbol", null, null);

        assertThatThrownBy(() -> paymentService.processPayment(bookingId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UPI ID format");
    }
}
