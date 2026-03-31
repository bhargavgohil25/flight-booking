package com.flightbooking.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightResponse(
        String flightNumber,
        String airline,
        String origin,
        String destination,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        int totalSeats,
        long availableSeats,
        BigDecimal pricePerSeat
) {}
