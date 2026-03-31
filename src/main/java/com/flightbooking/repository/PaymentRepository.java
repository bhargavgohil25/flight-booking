package com.flightbooking.repository;

import com.flightbooking.model.entity.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findByPaymentId(UUID paymentId);
    Optional<Payment> findByBookingId(UUID bookingId);
}
