package com.flightbooking.strategy;

import com.flightbooking.model.enums.PaymentMethod;
import com.flightbooking.model.request.PaymentRequest;

import java.math.BigDecimal;

public interface PaymentProcessor {
    PaymentMethod getPaymentMethod();
    void validate(PaymentRequest request);
    String processPayment(BigDecimal amount, PaymentRequest request);
}
