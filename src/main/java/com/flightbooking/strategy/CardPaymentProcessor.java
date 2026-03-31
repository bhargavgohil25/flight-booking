package com.flightbooking.strategy;

import com.flightbooking.model.enums.PaymentMethod;
import com.flightbooking.model.request.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class CardPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public void validate(PaymentRequest request) {
        if (request.cardNumber() == null || request.cardNumber().isBlank()) {
            throw new IllegalArgumentException("Card number is required for card payment");
        }
        String digits = request.cardNumber().replaceAll("\\s+", "");
        if (digits.length() < 13 || digits.length() > 19 || !digits.matches("\\d+")) {
            throw new IllegalArgumentException("Invalid card number");
        }
    }

    @Override
    public String processPayment(BigDecimal amount, PaymentRequest request) {
        String masked = "****" + request.cardNumber().replaceAll("\\s+", "")
                .substring(request.cardNumber().replaceAll("\\s+", "").length() - 4);
        log.info("Processing card payment of {} on card {}", amount, masked);
        return "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
