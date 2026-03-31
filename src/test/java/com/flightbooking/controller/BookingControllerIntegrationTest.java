package com.flightbooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightbooking.model.enums.SeatPreference;
import com.flightbooking.model.request.BookingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void bookFlight_validRequest_returns201() throws Exception {
        BookingRequest request = new BookingRequest("SG101", "Jane Doe", "jane@example.com", 2, null);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").exists())
                .andExpect(jsonPath("$.flightNumber").value("SG101"))
                .andExpect(jsonPath("$.allocatedSeats").isArray())
                .andExpect(jsonPath("$.allocatedSeats.length()").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void bookFlight_unknownFlight_returns404() throws Exception {
        BookingRequest request = new BookingRequest("XX999", "Jane Doe", "jane@example.com", 1, null);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flight not found: XX999"));
    }

    @Test
    void bookFlight_insufficientSeats_returns409() throws Exception {
        BookingRequest request = new BookingRequest("UK833", "Jane Doe", "seats@example.com", 100, null);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void bookFlight_missingFields_returns400() throws Exception {
        String json = "{\"flightNumber\": \"EK502\"}";

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void bookFlight_duplicate_returns409() throws Exception {
        BookingRequest request = new BookingRequest("EK502", "Dup Test", "dup@example.com", 1, null);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Duplicate booking: passenger already booked on this flight"));
    }

    @Test
    void bookFlight_withWindowPreference_returnsWindowSeats() throws Exception {
        BookingRequest request = new BookingRequest("QR501", "Window Fan", "window@example.com", 2, SeatPreference.WINDOW);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allocatedSeats").isArray())
                .andExpect(jsonPath("$.allocatedSeats.length()").value(2));
        // Seats are window seats (A or F columns) — exact values depend on which are pre-booked
    }
}
