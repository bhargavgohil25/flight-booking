package com.flightbooking.model.entity;

import com.flightbooking.model.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    private UUID bookingId;
    private String passengerName;
    private String passengerEmail;
    private String flightNumber;
    private int numberOfSeats;
    private List<String> allocatedSeats;
    private BigDecimal totalPrice;
    private LocalDateTime bookingTime;
    private BookingStatus status;
    private UUID paymentId;
    private LocalDateTime paymentDeadline;
}
