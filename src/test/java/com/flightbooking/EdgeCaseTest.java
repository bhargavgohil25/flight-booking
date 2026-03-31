package com.flightbooking;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class EdgeCaseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void zeroSeats_returns400() throws Exception {
        BookingRequest request = new BookingRequest("EK502", "John", "john@edge.com", 0, null);
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeSeats_returns400() throws Exception {
        BookingRequest request = new BookingRequest("EK502", "John", "john@neg.com", -1, null);
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankName_returns400() throws Exception {
        BookingRequest request = new BookingRequest("EK502", "", "john@blank.com", 1, null);
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidEmail_returns400() throws Exception {
        BookingRequest request = new BookingRequest("EK502", "John", "not-an-email", 1, null);
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nullRequestBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void seatsExceedTotalSeats_returns409() throws Exception {
        // UK833 has 12 total seats
        BookingRequest request = new BookingRequest("UK833", "Big Group", "big@group.com", 100, null);
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void whitespaceOnlyName_returns400() throws Exception {
        BookingRequest request = new BookingRequest("EK502", "   ", "ws@test.com", 1, null);
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
