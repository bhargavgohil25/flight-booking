package com.flightbooking.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightRequest(
        @NotBlank(message = "Flight number is required")
        String flightNumber,

        @NotBlank(message = "Airline is required")
        String airline,

        @NotBlank(message = "Origin is required")
        String origin,

        @NotBlank(message = "Destination is required")
        String destination,

        @NotNull(message = "Departure time is required")
        LocalDateTime departureTime,

        @NotNull(message = "Arrival time is required")
        LocalDateTime arrivalTime,

        @NotNull(message = "Total seats is required")
        @Min(value = 1, message = "Total seats must be at least 1")
        Integer totalSeats,

        @NotNull(message = "Price per seat is required")
        BigDecimal pricePerSeat
) {}
