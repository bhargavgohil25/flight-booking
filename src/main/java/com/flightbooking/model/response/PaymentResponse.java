package com.flightbooking.model.response;

import com.flightbooking.model.enums.BookingStatus;
import com.flightbooking.model.enums.PaymentMethod;
import com.flightbooking.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID bookingId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String transactionReference,
        BookingStatus bookingStatus,
        LocalDateTime processedAt
) {}
