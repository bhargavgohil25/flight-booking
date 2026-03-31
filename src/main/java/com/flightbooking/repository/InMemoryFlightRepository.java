package com.flightbooking.repository;

import com.flightbooking.model.entity.Flight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class InMemoryFlightRepository implements FlightRepository {

    private final ConcurrentHashMap<String, Flight> flights = new ConcurrentHashMap<>();

    @Override
    public Optional<Flight> findByFlightNumber(String flightNumber) {
        return Optional.ofNullable(flights.get(flightNumber));
    }

    @Override
    public Flight save(Flight flight) {
        flights.put(flight.getFlightNumber(), flight);
        log.info("Saved flight: {} ({} -> {})", flight.getFlightNumber(), flight.getOrigin(), flight.getDestination());
        return flight;
    }

    @Override
    public boolean existsByFlightNumber(String flightNumber) {
        return flights.containsKey(flightNumber);
    }
}
