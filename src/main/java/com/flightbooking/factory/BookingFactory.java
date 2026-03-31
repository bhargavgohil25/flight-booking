package com.flightbooking.factory;

import com.flightbooking.model.entity.Booking;
import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.BookingStatus;
import com.flightbooking.model.request.BookingRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class BookingFactory {

    public Booking createBooking(BookingRequest request, Flight flight, List<String> allocatedSeats) {
        return Booking.builder()
                .bookingId(UUID.randomUUID())
                .passengerName(request.passengerName())
                .passengerEmail(request.passengerEmail())
                .flightNumber(request.flightNumber())
                .numberOfSeats(request.numberOfSeats())
                .allocatedSeats(allocatedSeats)
                .totalPrice(flight.getPricePerSeat().multiply(BigDecimal.valueOf(request.numberOfSeats())))
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentDeadline(LocalDateTime.now().plusMinutes(10))
                .build();
    }
}
