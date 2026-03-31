package com.flightbooking.config;

import com.flightbooking.strategy.RandomSeatAllocationStrategy;
import com.flightbooking.strategy.SeatAllocationStrategy;
import com.flightbooking.strategy.SequentialSeatAllocationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SeatAllocationConfig {

    @Bean
    @Primary
    public SeatAllocationStrategy seatAllocationStrategy(
            @Value("${booking.seat-allocation-strategy:sequential}") String strategy,
            SequentialSeatAllocationStrategy sequential,
            RandomSeatAllocationStrategy random) {
        return switch (strategy.toLowerCase()) {
            case "random" -> random;
            default -> sequential;
        };
    }
}
