package com.flightbooking.controller;

import com.flightbooking.model.request.BookingRequest;
import com.flightbooking.model.response.BookingResponse;
import com.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> bookFlight(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.bookFlight(request);
        URI location = URI.create("/api/v1/bookings/" + response.bookingId());
        return ResponseEntity.created(location).body(response);
    }
}
