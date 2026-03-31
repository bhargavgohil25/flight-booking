package com.flightbooking.repository;

import com.flightbooking.model.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final ConcurrentHashMap<UUID, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        payments.put(payment.getPaymentId(), payment);
        log.info("Saved payment: {} for booking {} (status: {})", payment.getPaymentId(), payment.getBookingId(), payment.getStatus());
        return payment;
    }

    @Override
    public Optional<Payment> findByPaymentId(UUID paymentId) {
        return Optional.ofNullable(payments.get(paymentId));
    }

    @Override
    public Optional<Payment> findByBookingId(UUID bookingId) {
        return payments.values().stream()
                .filter(p -> p.getBookingId().equals(bookingId))
                .findFirst();
    }
}
