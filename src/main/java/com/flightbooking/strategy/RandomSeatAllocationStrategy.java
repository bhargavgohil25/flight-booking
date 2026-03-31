package com.flightbooking.strategy;

import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.SeatStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RandomSeatAllocationStrategy implements SeatAllocationStrategy {

    @Override
    public List<String> allocateSeats(Flight flight, int numberOfSeats) {
        List<String> availableSeats = new ArrayList<>();
        for (var entry : flight.getSeatMap().entrySet()) {
            if (entry.getValue() == SeatStatus.AVAILABLE) {
                availableSeats.add(entry.getKey());
            }
        }
        Collections.shuffle(availableSeats);
        return availableSeats.subList(0, numberOfSeats);
    }
}
