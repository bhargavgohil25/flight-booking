package com.flightbooking.config;

import com.flightbooking.model.entity.Booking;
import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.BookingStatus;
import com.flightbooking.model.enums.SeatStatus;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentExpirySchedulerTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private FlightRepository flightRepository;
    @InjectMocks private PaymentExpiryScheduler scheduler;

    @Test
    void cancelExpiredBookings_cancelsAndReleasesSeats() {
        Booking expired = Booking.builder()
                .bookingId(UUID.randomUUID())
                .flightNumber("EK502")
                .passengerName("John")
                .passengerEmail("john@test.com")
                .numberOfSeats(2)
                .allocatedSeats(List.of("1A", "1B"))
                .totalPrice(new BigDecimal("50000"))
                .bookingTime(LocalDateTime.now().minusMinutes(15))
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentDeadline(LocalDateTime.now().minusMinutes(5))
                .build();

        Flight flight = Flight.builder().flightNumber("EK502").totalSeats(12).pricePerSeat(new BigDecimal("25000")).build();
        flight.generateSeatMap();
        flight.getSeatMap().put("1A", SeatStatus.BOOKED);
        flight.getSeatMap().put("1B", SeatStatus.BOOKED);

        when(bookingRepository.findExpiredPendingBookings()).thenReturn(List.of(expired));
        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.cancelExpiredBookings();

        assertThat(expired.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(flight.getSeatMap().get("1A")).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(flight.getSeatMap().get("1B")).isEqualTo(SeatStatus.AVAILABLE);
        verify(bookingRepository).save(expired);
    }

    @Test
    void cancelExpiredBookings_noExpired_doesNothing() {
        when(bookingRepository.findExpiredPendingBookings()).thenReturn(List.of());

        scheduler.cancelExpiredBookings();

        verify(bookingRepository, never()).save(any());
        verify(flightRepository, never()).findByFlightNumber(any());
    }

    @Test
    void cancelExpiredBookings_multipleExpired_cancelsAll() {
        Booking expired1 = Booking.builder()
                .bookingId(UUID.randomUUID()).flightNumber("EK502")
                .allocatedSeats(List.of("1A")).status(BookingStatus.PENDING_PAYMENT)
                .paymentDeadline(LocalDateTime.now().minusMinutes(5)).build();
        Booking expired2 = Booking.builder()
                .bookingId(UUID.randomUUID()).flightNumber("EK502")
                .allocatedSeats(List.of("1B")).status(BookingStatus.PENDING_PAYMENT)
                .paymentDeadline(LocalDateTime.now().minusMinutes(2)).build();

        Flight flight = Flight.builder().flightNumber("EK502").totalSeats(12).pricePerSeat(new BigDecimal("25000")).build();
        flight.generateSeatMap();
        flight.getSeatMap().put("1A", SeatStatus.BOOKED);
        flight.getSeatMap().put("1B", SeatStatus.BOOKED);

        when(bookingRepository.findExpiredPendingBookings()).thenReturn(List.of(expired1, expired2));
        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.cancelExpiredBookings();

        assertThat(expired1.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(expired2.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(flight.getSeatMap().get("1A")).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(flight.getSeatMap().get("1B")).isEqualTo(SeatStatus.AVAILABLE);
        verify(bookingRepository, times(2)).save(any());
    }
}
