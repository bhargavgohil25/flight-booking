package com.flightbooking.repository;

import com.flightbooking.model.entity.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final ConcurrentHashMap<UUID, Booking> bookings = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        bookings.put(booking.getBookingId(), booking);
        log.info("Saved booking: {} for flight {} (seats: {})", booking.getBookingId(), booking.getFlightNumber(), booking.getAllocatedSeats());
        return booking;
    }

    @Override
    public boolean existsByEmailAndFlightNumber(String email, String flightNumber) {
        return bookings.values().stream()
                .anyMatch(b -> b.getPassengerEmail().equals(email) && b.getFlightNumber().equals(flightNumber));
    }
}
