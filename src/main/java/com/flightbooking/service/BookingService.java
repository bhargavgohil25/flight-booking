package com.flightbooking.service;

import com.flightbooking.model.request.BookingRequest;
import com.flightbooking.model.response.BookingResponse;

public interface BookingService {
    BookingResponse bookFlight(BookingRequest request);
}
