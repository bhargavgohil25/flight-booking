package com.flightbooking.strategy;

import com.flightbooking.exception.InsufficientSeatsException;
import com.flightbooking.model.entity.Flight;
import org.springframework.stereotype.Component;

@Component
public class DefaultSeatValidationStrategy implements SeatValidationStrategy {

    @Override
    public void validate(Flight flight, int requestedSeats) {
        long available = flight.getAvailableSeats();
        if (available < requestedSeats) {
            throw new InsufficientSeatsException(
                    "Insufficient seats: requested " + requestedSeats + ", available " + available);
        }
    }
}
