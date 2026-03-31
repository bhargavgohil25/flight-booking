package com.flightbooking.service;

import com.flightbooking.exception.DuplicateBookingException;
import com.flightbooking.exception.FlightNotFoundException;
import com.flightbooking.factory.BookingFactory;
import com.flightbooking.mapper.BookingMapper;
import com.flightbooking.model.entity.Booking;
import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.SeatStatus;
import com.flightbooking.model.request.BookingRequest;
import com.flightbooking.model.response.BookingResponse;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.strategy.PreferredSeatAllocationStrategy;
import com.flightbooking.strategy.SeatAllocationStrategy;
import com.flightbooking.strategy.SeatValidationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;
    private final SeatValidationStrategy seatValidationStrategy;
    private final SeatAllocationStrategy seatAllocationStrategy;
    private final PreferredSeatAllocationStrategy preferredSeatAllocationStrategy;
    private final BookingFactory bookingFactory;
    private final BookingMapper bookingMapper;

    private final ConcurrentHashMap<String, ReentrantLock> flightLocks = new ConcurrentHashMap<>();

    @Override
    public BookingResponse bookFlight(BookingRequest request) {
        String flightNumber = request.flightNumber();

        Flight flight = flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found: " + flightNumber));

        ReentrantLock lock = flightLocks.computeIfAbsent(flightNumber, k -> new ReentrantLock());
        lock.lock();
        try {
            if (bookingRepository.existsByEmailAndFlightNumber(request.passengerEmail(), flightNumber)) {
                throw new DuplicateBookingException("Duplicate booking: passenger already booked on this flight");
            }

            seatValidationStrategy.validate(flight, request.numberOfSeats());

            List<String> allocatedSeats = request.seatPreference() != null
                    ? preferredSeatAllocationStrategy.allocateSeats(flight, request.numberOfSeats(), request.seatPreference())
                    : seatAllocationStrategy.allocateSeats(flight, request.numberOfSeats());

            for (String seat : allocatedSeats) {
                flight.getSeatMap().put(seat, SeatStatus.BOOKED);
            }

            Booking booking = bookingFactory.createBooking(request, flight, allocatedSeats);
            bookingRepository.save(booking);

            log.info("Booking created (pending payment): {} on flight {} - seats {} - pay by {}",
                    booking.getBookingId(), flightNumber, allocatedSeats, booking.getPaymentDeadline());

            return bookingMapper.toResponse(booking);
        } finally {
            lock.unlock();
        }
    }
}
