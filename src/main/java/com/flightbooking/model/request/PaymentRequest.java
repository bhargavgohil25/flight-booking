package com.flightbooking.model.request;

import com.flightbooking.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String upiId,

        String cardNumber,

        String giftCardCode
) {}
