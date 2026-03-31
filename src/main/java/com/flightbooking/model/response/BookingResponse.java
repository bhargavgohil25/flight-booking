package com.flightbooking.model.response;

import com.flightbooking.model.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID bookingId,
        String flightNumber,
        String passengerName,
        String passengerEmail,
        int numberOfSeats,
        List<String> allocatedSeats,
        BigDecimal totalPrice,
        LocalDateTime bookingTime,
        BookingStatus status
) {}
