package com.flightbooking.config;

import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.SeatStatus;
import com.flightbooking.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FlightRepository flightRepository;

    @Override
    public void run(String... args) {
        createFlight("EK502", "Emirates", "Mumbai", "Dubai",
                LocalDateTime.of(2026, 4, 15, 6, 0),
                LocalDateTime.of(2026, 4, 15, 8, 30),
                30, new BigDecimal("25000.00"), 5);

        createFlight("AI302", "Air India", "Delhi", "London",
                LocalDateTime.of(2026, 4, 16, 14, 0),
                LocalDateTime.of(2026, 4, 16, 19, 30),
                42, new BigDecimal("45000.00"), 10);

        createFlight("SG101", "SpiceJet", "Bangalore", "Goa",
                LocalDateTime.of(2026, 4, 17, 9, 0),
                LocalDateTime.of(2026, 4, 17, 10, 30),
                18, new BigDecimal("4500.00"), 0);

        createFlight("6E205", "IndiGo", "Chennai", "Kolkata",
                LocalDateTime.of(2026, 4, 18, 11, 0),
                LocalDateTime.of(2026, 4, 18, 13, 30),
                24, new BigDecimal("6000.00"), 20);

        createFlight("UK833", "Vistara", "Hyderabad", "Mumbai",
                LocalDateTime.of(2026, 4, 19, 16, 0),
                LocalDateTime.of(2026, 4, 19, 18, 0),
                12, new BigDecimal("8500.00"), 3);

        createFlight("QR501", "Qatar Airways", "Delhi", "Doha",
                LocalDateTime.of(2026, 4, 20, 2, 0),
                LocalDateTime.of(2026, 4, 20, 5, 0),
                36, new BigDecimal("32000.00"), 8);

        log.info("Sample flight data loaded successfully");
    }

    private void createFlight(String flightNumber, String airline, String origin, String destination,
                              LocalDateTime departure, LocalDateTime arrival, int totalSeats,
                              BigDecimal price, int preBookedCount) {
        Flight flight = Flight.builder()
                .flightNumber(flightNumber)
                .airline(airline)
                .origin(origin)
                .destination(destination)
                .departureTime(departure)
                .arrivalTime(arrival)
                .totalSeats(totalSeats)
                .pricePerSeat(price)
                .build();
        flight.generateSeatMap();

        if (preBookedCount > 0) {
            List<String> seats = new ArrayList<>(flight.getSeatMap().keySet());
            Collections.shuffle(seats);
            seats.stream().limit(preBookedCount).forEach(s -> flight.getSeatMap().put(s, SeatStatus.BOOKED));
        }

        flightRepository.save(flight);
        log.info("Loaded flight {} ({} -> {}): {} total seats, {} available",
                flightNumber, origin, destination, totalSeats, flight.getAvailableSeats());
    }
}
