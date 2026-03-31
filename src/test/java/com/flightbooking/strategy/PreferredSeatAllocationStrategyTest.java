package com.flightbooking.strategy;

import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.SeatPreference;
import com.flightbooking.model.enums.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreferredSeatAllocationStrategyTest {

    private PreferredSeatAllocationStrategy strategy;
    private Flight flight;

    @BeforeEach
    void setUp() {
        strategy = new PreferredSeatAllocationStrategy();
        flight = Flight.builder()
                .flightNumber("TEST01")
                .totalSeats(12)
                .pricePerSeat(new BigDecimal("1000"))
                .build();
        flight.generateSeatMap();
        // Seats: 1A,1B,1C,1D,1E,1F,2A,2B,2C,2D,2E,2F
        // Window: A,F  Middle: B,E  Aisle: C,D
    }

    @Test
    void windowPreference_returnsWindowSeatsFirst() {
        List<String> seats = strategy.allocateSeats(flight, 3, SeatPreference.WINDOW);
        // Window seats: 1A, 1F, 2A, 2F — should get first 3
        assertThat(seats).containsExactly("1A", "1F", "2A");
    }

    @Test
    void aislePreference_returnsAisleSeatsFirst() {
        List<String> seats = strategy.allocateSeats(flight, 3, SeatPreference.AISLE);
        // Aisle seats: 1C, 1D, 2C, 2D
        assertThat(seats).containsExactly("1C", "1D", "2C");
    }

    @Test
    void middlePreference_returnsMiddleSeatsFirst() {
        List<String> seats = strategy.allocateSeats(flight, 3, SeatPreference.MIDDLE);
        // Middle seats: 1B, 1E, 2B, 2E
        assertThat(seats).containsExactly("1B", "1E", "2B");
    }

    @Test
    void windowPreference_fallsBackToOtherSeatsWhenNotEnough() {
        // Book all window seats
        flight.getSeatMap().put("1A", SeatStatus.BOOKED);
        flight.getSeatMap().put("1F", SeatStatus.BOOKED);
        flight.getSeatMap().put("2A", SeatStatus.BOOKED);
        flight.getSeatMap().put("2F", SeatStatus.BOOKED);

        List<String> seats = strategy.allocateSeats(flight, 2, SeatPreference.WINDOW);
        // No window seats left — falls back to first 2 available (1B, 1C)
        assertThat(seats).hasSize(2);
        assertThat(seats).containsExactly("1B", "1C");
    }

    @Test
    void preference_partialMatch_fillsPreferredThenFallback() {
        // Book some window seats, leaving only 1F
        flight.getSeatMap().put("1A", SeatStatus.BOOKED);
        flight.getSeatMap().put("2A", SeatStatus.BOOKED);
        flight.getSeatMap().put("2F", SeatStatus.BOOKED);

        List<String> seats = strategy.allocateSeats(flight, 3, SeatPreference.WINDOW);
        // 1F is the only window seat left, then fallback to 1B, 1C
        assertThat(seats).hasSize(3);
        assertThat(seats.get(0)).isEqualTo("1F");
    }
}
