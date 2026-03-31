package com.flightbooking.strategy;

import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeatAllocationStrategyTest {

    private Flight flight;

    @BeforeEach
    void setUp() {
        flight = Flight.builder()
                .flightNumber("TEST01")
                .totalSeats(12)
                .pricePerSeat(new BigDecimal("1000"))
                .build();
        flight.generateSeatMap();
    }

    @Test
    void sequential_returnsFirstNAvailableInOrder() {
        flight.getSeatMap().put("1A", SeatStatus.BOOKED);
        flight.getSeatMap().put("1B", SeatStatus.BOOKED);

        SequentialSeatAllocationStrategy strategy = new SequentialSeatAllocationStrategy();
        List<String> seats = strategy.allocateSeats(flight, 2);

        assertThat(seats).containsExactly("1C", "1D");
    }

    @Test
    void sequential_returnsCorrectCountOfSeats() {
        SequentialSeatAllocationStrategy strategy = new SequentialSeatAllocationStrategy();
        List<String> seats = strategy.allocateSeats(flight, 4);

        assertThat(seats).hasSize(4);
        assertThat(seats).containsExactly("1A", "1B", "1C", "1D");
    }

    @Test
    void random_returnsNAvailableSeatsNoDuplicates() {
        RandomSeatAllocationStrategy strategy = new RandomSeatAllocationStrategy();
        List<String> seats = strategy.allocateSeats(flight, 5);

        assertThat(seats).hasSize(5);
        assertThat(new HashSet<>(seats)).hasSize(5);
        for (String seat : seats) {
            assertThat(flight.getSeatMap().get(seat)).isEqualTo(SeatStatus.AVAILABLE);
        }
    }

    @Test
    void random_doesNotReturnBookedSeats() {
        flight.getSeatMap().put("1A", SeatStatus.BOOKED);
        flight.getSeatMap().put("1B", SeatStatus.BOOKED);

        RandomSeatAllocationStrategy strategy = new RandomSeatAllocationStrategy();
        List<String> seats = strategy.allocateSeats(flight, 3);

        assertThat(seats).hasSize(3);
        assertThat(seats).doesNotContain("1A", "1B");
    }
}
