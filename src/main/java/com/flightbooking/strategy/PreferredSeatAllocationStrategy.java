package com.flightbooking.strategy;

import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.SeatPreference;
import com.flightbooking.model.enums.SeatStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PreferredSeatAllocationStrategy {

    /**
     * Allocates seats prioritizing the given preference. Preferred seats are filled first;
     * if not enough preferred seats are available, remaining seats are filled from any available.
     */
    public List<String> allocateSeats(Flight flight, int numberOfSeats, SeatPreference preference) {
        List<String> preferred = new ArrayList<>();
        List<String> fallback = new ArrayList<>();

        for (var entry : flight.getSeatMap().entrySet()) {
            if (entry.getValue() != SeatStatus.AVAILABLE) continue;
            if (preference.matches(entry.getKey())) {
                preferred.add(entry.getKey());
            } else {
                fallback.add(entry.getKey());
            }
        }

        List<String> allocated = new ArrayList<>();
        for (String seat : preferred) {
            if (allocated.size() >= numberOfSeats) break;
            allocated.add(seat);
        }
        for (String seat : fallback) {
            if (allocated.size() >= numberOfSeats) break;
            allocated.add(seat);
        }

        return allocated;
    }
}
