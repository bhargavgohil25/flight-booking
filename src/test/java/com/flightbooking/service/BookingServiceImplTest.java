package com.flightbooking.service;

import com.flightbooking.exception.DuplicateBookingException;
import com.flightbooking.exception.FlightNotFoundException;
import com.flightbooking.exception.InsufficientSeatsException;
import com.flightbooking.factory.BookingFactory;
import com.flightbooking.mapper.BookingMapper;
import com.flightbooking.model.entity.Booking;
import com.flightbooking.model.entity.Flight;
import com.flightbooking.model.enums.BookingStatus;
import com.flightbooking.model.enums.SeatStatus;
import com.flightbooking.model.request.BookingRequest;
import com.flightbooking.model.response.BookingResponse;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.strategy.SeatAllocationStrategy;
import com.flightbooking.strategy.SeatValidationStrategy;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private FlightRepository flightRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private SeatValidationStrategy seatValidationStrategy;
    @Mock private SeatAllocationStrategy seatAllocationStrategy;
    @Mock private BookingFactory bookingFactory;
    @Mock private BookingMapper bookingMapper;

    @InjectMocks private BookingServiceImpl bookingService;

    private Flight flight;
    private BookingRequest request;

    @BeforeEach
    void setUp() {
        flight = Flight.builder()
                .flightNumber("EK502")
                .airline("Emirates")
                .origin("Mumbai")
                .destination("Dubai")
                .totalSeats(12)
                .pricePerSeat(new BigDecimal("25000.00"))
                .build();
        flight.generateSeatMap();

        request = new BookingRequest("EK502", "John Doe", "john@example.com", 2);
    }

    @Test
    void bookFlight_success_returnsBookingResponse() {
        UUID bookingId = UUID.randomUUID();
        List<String> seats = List.of("1A", "1B");
        Booking booking = Booking.builder()
                .bookingId(bookingId).flightNumber("EK502").passengerName("John Doe")
                .passengerEmail("john@example.com").numberOfSeats(2).allocatedSeats(seats)
                .totalPrice(new BigDecimal("50000.00")).bookingTime(LocalDateTime.now())
                .status(BookingStatus.CONFIRMED).build();
        BookingResponse expectedResponse = new BookingResponse(bookingId, "EK502", "John Doe",
                "john@example.com", 2, seats, new BigDecimal("50000.00"), booking.getBookingTime(), BookingStatus.CONFIRMED);

        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));
        when(bookingRepository.existsByEmailAndFlightNumber("john@example.com", "EK502")).thenReturn(false);
        when(seatAllocationStrategy.allocateSeats(flight, 2)).thenReturn(seats);
        when(bookingFactory.createBooking(request, flight, seats)).thenReturn(booking);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(expectedResponse);

        BookingResponse result = bookingService.bookFlight(request);

        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.allocatedSeats()).containsExactly("1A", "1B");
        assertThat(result.totalPrice()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(result.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void bookFlight_flightNotFound_throwsException() {
        when(flightRepository.findByFlightNumber("EK999")).thenReturn(Optional.empty());
        BookingRequest req = new BookingRequest("EK999", "John", "john@example.com", 1);

        assertThatThrownBy(() -> bookingService.bookFlight(req))
                .isInstanceOf(FlightNotFoundException.class)
                .hasMessageContaining("Flight not found: EK999");
    }

    @Test
    void bookFlight_insufficientSeats_throwsException() {
        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));
        when(bookingRepository.existsByEmailAndFlightNumber(anyString(), eq("EK502"))).thenReturn(false);
        doThrow(new InsufficientSeatsException("Insufficient seats: requested 50, available 12"))
                .when(seatValidationStrategy).validate(flight, 50);

        BookingRequest req = new BookingRequest("EK502", "John", "john@example.com", 50);

        assertThatThrownBy(() -> bookingService.bookFlight(req))
                .isInstanceOf(InsufficientSeatsException.class)
                .hasMessageContaining("Insufficient seats");
    }

    @Test
    void bookFlight_duplicateBooking_throwsException() {
        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));
        when(bookingRepository.existsByEmailAndFlightNumber("john@example.com", "EK502")).thenReturn(true);

        assertThatThrownBy(() -> bookingService.bookFlight(request))
                .isInstanceOf(DuplicateBookingException.class)
                .hasMessageContaining("Duplicate booking");
    }

    @Test
    void bookFlight_seatsMarkedAsBooked() {
        List<String> seats = List.of("1A", "1B");
        Booking booking = Booking.builder().bookingId(UUID.randomUUID()).flightNumber("EK502")
                .passengerName("John").passengerEmail("john@example.com").numberOfSeats(2)
                .allocatedSeats(seats).totalPrice(new BigDecimal("50000.00"))
                .bookingTime(LocalDateTime.now()).status(BookingStatus.CONFIRMED).build();

        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));
        when(bookingRepository.existsByEmailAndFlightNumber(anyString(), anyString())).thenReturn(false);
        when(seatAllocationStrategy.allocateSeats(flight, 2)).thenReturn(seats);
        when(bookingFactory.createBooking(any(), any(), any())).thenReturn(booking);
        when(bookingRepository.save(any())).thenReturn(booking);
        when(bookingMapper.toResponse(any())).thenReturn(mock(BookingResponse.class));

        bookingService.bookFlight(request);

        assertThat(flight.getSeatMap().get("1A")).isEqualTo(SeatStatus.BOOKED);
        assertThat(flight.getSeatMap().get("1B")).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void bookFlight_totalPriceComputedCorrectly() {
        List<String> seats = List.of("1A", "1B");
        BigDecimal expectedPrice = new BigDecimal("50000.00");
        Booking booking = Booking.builder().bookingId(UUID.randomUUID()).flightNumber("EK502")
                .passengerName("John").passengerEmail("john@example.com").numberOfSeats(2)
                .allocatedSeats(seats).totalPrice(expectedPrice)
                .bookingTime(LocalDateTime.now()).status(BookingStatus.CONFIRMED).build();
        BookingResponse response = new BookingResponse(booking.getBookingId(), "EK502", "John",
                "john@example.com", 2, seats, expectedPrice, booking.getBookingTime(), BookingStatus.CONFIRMED);

        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));
        when(bookingRepository.existsByEmailAndFlightNumber(anyString(), anyString())).thenReturn(false);
        when(seatAllocationStrategy.allocateSeats(flight, 2)).thenReturn(seats);
        when(bookingFactory.createBooking(any(), any(), any())).thenReturn(booking);
        when(bookingRepository.save(any())).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        BookingResponse result = bookingService.bookFlight(request);

        assertThat(result.totalPrice()).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    void bookFlight_bookingIdGenerated() {
        UUID bookingId = UUID.randomUUID();
        List<String> seats = List.of("1A");
        Booking booking = Booking.builder().bookingId(bookingId).flightNumber("EK502")
                .passengerName("John").passengerEmail("john@example.com").numberOfSeats(1)
                .allocatedSeats(seats).totalPrice(new BigDecimal("25000.00"))
                .bookingTime(LocalDateTime.now()).status(BookingStatus.CONFIRMED).build();
        BookingResponse response = new BookingResponse(bookingId, "EK502", "John",
                "john@example.com", 1, seats, new BigDecimal("25000.00"), booking.getBookingTime(), BookingStatus.CONFIRMED);

        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));
        when(bookingRepository.existsByEmailAndFlightNumber(anyString(), anyString())).thenReturn(false);
        when(seatAllocationStrategy.allocateSeats(any(), anyInt())).thenReturn(seats);
        when(bookingFactory.createBooking(any(), any(), any())).thenReturn(booking);
        when(bookingRepository.save(any())).thenReturn(booking);
        when(bookingMapper.toResponse(any())).thenReturn(response);

        BookingRequest req = new BookingRequest("EK502", "John", "john@example.com", 1);
        BookingResponse result = bookingService.bookFlight(req);

        assertThat(result.bookingId()).isNotNull();
    }

    @Test
    void bookFlight_allocatedSeatsCountMatchesRequest() {
        List<String> seats = List.of("1A", "1B", "1C");
        Booking booking = Booking.builder().bookingId(UUID.randomUUID()).flightNumber("EK502")
                .passengerName("John").passengerEmail("john@example.com").numberOfSeats(3)
                .allocatedSeats(seats).totalPrice(new BigDecimal("75000.00"))
                .bookingTime(LocalDateTime.now()).status(BookingStatus.CONFIRMED).build();
        BookingResponse response = new BookingResponse(booking.getBookingId(), "EK502", "John",
                "john@example.com", 3, seats, new BigDecimal("75000.00"), booking.getBookingTime(), BookingStatus.CONFIRMED);

        when(flightRepository.findByFlightNumber("EK502")).thenReturn(Optional.of(flight));
        when(bookingRepository.existsByEmailAndFlightNumber(anyString(), anyString())).thenReturn(false);
        when(seatAllocationStrategy.allocateSeats(flight, 3)).thenReturn(seats);
        when(bookingFactory.createBooking(any(), any(), any())).thenReturn(booking);
        when(bookingRepository.save(any())).thenReturn(booking);
        when(bookingMapper.toResponse(any())).thenReturn(response);

        BookingRequest req = new BookingRequest("EK502", "John", "john@example.com", 3);
        BookingResponse result = bookingService.bookFlight(req);

        assertThat(result.allocatedSeats()).hasSize(3);
        assertThat(result.numberOfSeats()).isEqualTo(3);
    }
}
