package com.flightbooking.model.entity;

import com.flightbooking.model.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Flight {

    private String flightNumber;
    private String airline;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private int totalSeats;
    private BigDecimal pricePerSeat;
    @Builder.Default
    private Map<String, SeatStatus> seatMap = new LinkedHashMap<>();

    public long getAvailableSeats() {
        return seatMap.values().stream()
                .filter(status -> status == SeatStatus.AVAILABLE)
                .count();
    }

    public void generateSeatMap() {
        seatMap = new LinkedHashMap<>();
        int rows = (int) Math.ceil((double) totalSeats / 6);
        char[] columns = {'A', 'B', 'C', 'D', 'E', 'F'};
        int seatsCreated = 0;
        for (int row = 1; row <= rows && seatsCreated < totalSeats; row++) {
            for (char col : columns) {
                if (seatsCreated >= totalSeats) break;
                seatMap.put(row + String.valueOf(col), SeatStatus.AVAILABLE);
                seatsCreated++;
            }
        }
    }
}
