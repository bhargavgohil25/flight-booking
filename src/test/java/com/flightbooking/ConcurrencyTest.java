package com.flightbooking;

import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.SeatStatus;
import com.flightbooking.model.request.BookingRequest;
import com.flightbooking.model.response.BookingResponse;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrencyTest {

    @Autowired private BookingService bookingService;
    @Autowired private FlightRepository flightRepository;

    @BeforeEach
    void setUp() {
        Flight flight = Flight.builder()
                .flightNumber("RACE01")
                .airline("Test Air")
                .origin("A")
                .destination("B")
                .departureTime(LocalDateTime.now().plusDays(1))
                .arrivalTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .totalSeats(5)
                .pricePerSeat(new BigDecimal("1000"))
                .build();
        flight.generateSeatMap();
        // 5 seats: 1A-1E (totalSeats=5 → row 1, columns A-E)
        flightRepository.save(flight);
    }

    @Test
    void concurrentBookings_noOverbooking() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<BookingResponse> successfulBookings = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    BookingRequest request = new BookingRequest(
                            "RACE01", "Passenger " + idx, "p" + idx + "@test.com", 1, null);
                    BookingResponse response = bookingService.bookFlight(request);
                    successCount.incrementAndGet();
                    successfulBookings.add(response);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);

        Flight flight = flightRepository.findByFlightNumber("RACE01").orElseThrow();
        assertThat(flight.getAvailableSeats()).isZero();

        // Verify no duplicate seat assignments
        Set<String> allAllocatedSeats = new HashSet<>();
        for (BookingResponse booking : successfulBookings) {
            for (String seat : booking.allocatedSeats()) {
                assertThat(allAllocatedSeats.add(seat))
                        .as("Seat %s should not be assigned twice", seat)
                        .isTrue();
            }
        }
        assertThat(allAllocatedSeats).hasSize(5);

        // All allocated seats should be marked BOOKED
        for (String seat : allAllocatedSeats) {
            assertThat(flight.getSeatMap().get(seat)).isEqualTo(SeatStatus.BOOKED);
        }
    }

    @Test
    void concurrentMultiSeatBookings_noOverbooking() throws Exception {
        // Create flight with 6 seats
        Flight flight = Flight.builder()
                .flightNumber("RACE02")
                .airline("Test Air")
                .origin("X")
                .destination("Y")
                .departureTime(LocalDateTime.now().plusDays(1))
                .arrivalTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .totalSeats(6)
                .pricePerSeat(new BigDecimal("2000"))
                .build();
        flight.generateSeatMap();
        flightRepository.save(flight);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Each thread tries to book 2 seats — only 3 can succeed (6 seats / 2 per booking)
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    BookingRequest request = new BookingRequest(
                            "RACE02", "Passenger " + idx, "multi" + idx + "@test.com", 2, null);
                    bookingService.bookFlight(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failCount.get()).isEqualTo(2);

        Flight result = flightRepository.findByFlightNumber("RACE02").orElseThrow();
        assertThat(result.getAvailableSeats()).isZero();
    }
}
