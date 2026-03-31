package com.flightbooking.model.request;

import com.flightbooking.model.enums.SeatPreference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotBlank(message = "Flight number is required")
        String flightNumber,

        @NotBlank(message = "Passenger name is required")
        String passengerName,

        @NotBlank(message = "Passenger email is required")
        @Email(message = "Invalid email format")
        String passengerEmail,

        @NotNull(message = "Number of seats is required")
        @Min(value = 1, message = "Number of seats must be at least 1")
        Integer numberOfSeats,

        SeatPreference seatPreference
) {}
