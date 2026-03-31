package com.flightbooking.repository;

import com.flightbooking.model.entity.Booking;

public interface BookingRepository {
    Booking save(Booking booking);
    boolean existsByEmailAndFlightNumber(String email, String flightNumber);
}
