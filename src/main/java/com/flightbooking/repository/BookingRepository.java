package com.flightbooking.repository;

import com.flightbooking.model.entity.Booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findByBookingId(UUID bookingId);
    boolean existsByEmailAndFlightNumber(String email, String flightNumber);
    List<Booking> findExpiredPendingBookings();
}
