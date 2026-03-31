package com.flightbooking.service;

import com.flightbooking.model.request.PaymentRequest;
import com.flightbooking.model.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse processPayment(UUID bookingId, PaymentRequest request);
}
