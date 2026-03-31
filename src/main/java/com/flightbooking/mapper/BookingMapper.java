package com.flightbooking.mapper;

import com.flightbooking.model.entity.Booking;
import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.response.BookingResponse;
import com.flightbooking.model.response.FlightResponse;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getBookingId(),
                booking.getFlightNumber(),
                booking.getPassengerName(),
                booking.getPassengerEmail(),
                booking.getNumberOfSeats(),
                booking.getAllocatedSeats(),
                booking.getTotalPrice(),
                booking.getBookingTime(),
                booking.getStatus()
        );
    }

    public FlightResponse toFlightResponse(Flight flight) {
        return new FlightResponse(
                flight.getFlightNumber(),
                flight.getAirline(),
                flight.getOrigin(),
                flight.getDestination(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getTotalSeats(),
                flight.getAvailableSeats(),
                flight.getPricePerSeat()
        );
    }
}
