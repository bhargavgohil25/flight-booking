package com.flightbooking.strategy;

import com.flightbooking.model.enums.PaymentMethod;
import com.flightbooking.model.request.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class UpiPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.UPI;
    }

    @Override
    public void validate(PaymentRequest request) {
        if (request.upiId() == null || request.upiId().isBlank()) {
            throw new IllegalArgumentException("UPI ID is required for UPI payment");
        }
        if (!request.upiId().contains("@")) {
            throw new IllegalArgumentException("Invalid UPI ID format");
        }
    }

    @Override
    public String processPayment(BigDecimal amount, PaymentRequest request) {
        log.info("Processing UPI payment of {} to UPI ID: {}", amount, request.upiId());
        return "UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
