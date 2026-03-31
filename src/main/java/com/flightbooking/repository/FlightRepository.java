package com.flightbooking.repository;

import com.flightbooking.model.entity.Flight;

import java.util.Optional;

public interface FlightRepository {
    Optional<Flight> findByFlightNumber(String flightNumber);
    Flight save(Flight flight);
    boolean existsByFlightNumber(String flightNumber);
}
