package com.flightbooking.strategy;

import com.flightbooking.model.entity.Flight;

import java.util.List;

public interface SeatAllocationStrategy {
    List<String> allocateSeats(Flight flight, int numberOfSeats);
}
