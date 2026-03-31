package com.flightbooking.controller;

import com.flightbooking.exception.DuplicateFlightException;
import com.flightbooking.mapper.BookingMapper;
import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.request.FlightRequest;
import com.flightbooking.model.response.FlightResponse;
import com.flightbooking.repository.FlightRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightRepository flightRepository;
    private final BookingMapper bookingMapper;

    @PostMapping
    public ResponseEntity<FlightResponse> addFlight(@Valid @RequestBody FlightRequest request) {
        if (flightRepository.existsByFlightNumber(request.flightNumber())) {
            throw new DuplicateFlightException("Flight already exists: " + request.flightNumber());
        }

        Flight flight = Flight.builder()
                .flightNumber(request.flightNumber())
                .airline(request.airline())
                .origin(request.origin())
                .destination(request.destination())
                .departureTime(request.departureTime())
                .arrivalTime(request.arrivalTime())
                .totalSeats(request.totalSeats())
                .pricePerSeat(request.pricePerSeat())
                .build();
        flight.generateSeatMap();

        flightRepository.save(flight);

        URI location = URI.create("/api/v1/flights/" + flight.getFlightNumber());
        return ResponseEntity.created(location).body(bookingMapper.toFlightResponse(flight));
    }
}
