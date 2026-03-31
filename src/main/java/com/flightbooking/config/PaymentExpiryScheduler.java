package com.flightbooking.config;

import com.flightbooking.model.entity.Booking;
import com.flightbooking.model.enums.BookingStatus;
import com.flightbooking.model.enums.SeatStatus;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    @Scheduled(fixedRate = 30000) // every 30 seconds
    public void cancelExpiredBookings() {
        List<Booking> expired = bookingRepository.findExpiredPendingBookings();
        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            flightRepository.findByFlightNumber(booking.getFlightNumber()).ifPresent(flight -> {
                for (String seat : booking.getAllocatedSeats()) {
                    flight.getSeatMap().put(seat, SeatStatus.AVAILABLE);
                }
            });

            log.info("Expired booking {} cancelled, released seats {} on flight {}",
                    booking.getBookingId(), booking.getAllocatedSeats(), booking.getFlightNumber());
        }
    }
}
