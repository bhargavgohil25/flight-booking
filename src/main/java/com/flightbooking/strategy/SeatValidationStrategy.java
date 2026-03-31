package com.flightbooking.strategy;

import com.flightbooking.model.entity.Flight;

public interface SeatValidationStrategy {
    void validate(Flight flight, int requestedSeats);
}
