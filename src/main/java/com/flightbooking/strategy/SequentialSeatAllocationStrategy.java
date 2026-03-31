package com.flightbooking.strategy;

import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.SeatStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Primary
public class SequentialSeatAllocationStrategy implements SeatAllocationStrategy {

    @Override
    public List<String> allocateSeats(Flight flight, int numberOfSeats) {
        List<String> allocated = new ArrayList<>();
        for (var entry : flight.getSeatMap().entrySet()) {
            if (allocated.size() >= numberOfSeats) break;
            if (entry.getValue() == SeatStatus.AVAILABLE) {
                allocated.add(entry.getKey());
            }
        }
        return allocated;
    }
}
