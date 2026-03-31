package com.flightbooking.strategy;

import com.flightbooking.model.enums.PaymentMethod;
import com.flightbooking.model.request.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class GiftCardPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.GIFT_CARD;
    }

    @Override
    public void validate(PaymentRequest request) {
        if (request.giftCardCode() == null || request.giftCardCode().isBlank()) {
            throw new IllegalArgumentException("Gift card code is required for gift card payment");
        }
        if (request.giftCardCode().length() < 8) {
            throw new IllegalArgumentException("Invalid gift card code");
        }
    }

    @Override
    public String processPayment(BigDecimal amount, PaymentRequest request) {
        log.info("Processing gift card payment of {} with code {}****",
                amount, request.giftCardCode().substring(0, 4));
        return "GC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
