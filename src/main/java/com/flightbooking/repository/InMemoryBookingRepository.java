package com.flightbooking.repository;

import com.flightbooking.model.entity.Booking;
import com.flightbooking.model.enums.BookingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final ConcurrentHashMap<UUID, Booking> bookings = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        bookings.put(booking.getBookingId(), booking);
        log.info("Saved booking: {} for flight {} (status: {}, seats: {})", booking.getBookingId(), booking.getFlightNumber(), booking.getStatus(), booking.getAllocatedSeats());
        return booking;
    }

    @Override
    public Optional<Booking> findByBookingId(UUID bookingId) {
        return Optional.ofNullable(bookings.get(bookingId));
    }

    @Override
    public boolean existsByEmailAndFlightNumber(String email, String flightNumber) {
        return bookings.values().stream()
                .anyMatch(b -> b.getPassengerEmail().equals(email)
                        && b.getFlightNumber().equals(flightNumber)
                        && (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING_PAYMENT));
    }

    @Override
    public List<Booking> findExpiredPendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        return bookings.values().stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING_PAYMENT
                        && b.getPaymentDeadline() != null
                        && b.getPaymentDeadline().isBefore(now))
                .toList();
    }
}
