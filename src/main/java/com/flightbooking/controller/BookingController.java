package com.flightbooking.controller;

import com.flightbooking.model.request.BookingRequest;
import com.flightbooking.model.request.PaymentRequest;
import com.flightbooking.model.response.BookingResponse;
import com.flightbooking.model.response.PaymentResponse;
import com.flightbooking.service.BookingService;
import com.flightbooking.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<BookingResponse> bookFlight(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.bookFlight(request);
        URI location = URI.create("/api/v1/bookings/" + response.bookingId());
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable UUID bookingId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(bookingId, request);
        return ResponseEntity.ok(response);
    }
}
