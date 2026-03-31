package com.flightbooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightbooking.model.enums.PaymentMethod;
import com.flightbooking.model.request.BookingRequest;
import com.flightbooking.model.request.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String createBookingAndGetId(String flight, String email) throws Exception {
        BookingRequest bookingReq = new BookingRequest(flight, "Pay Tester", email, 1, null);
        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("bookingId").asText();
    }

    @Test
    void fullFlow_bookThenPayWithUpi_returns200Confirmed() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "upi-flow@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.UPI, "user@okbank", null, null);
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.transactionReference").exists())
                .andExpect(jsonPath("$.paymentMethod").value("UPI"));
    }

    @Test
    void fullFlow_bookThenPayWithCard_returns200Confirmed() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "card-flow@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.CARD, null, "4111111111111111", null);
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.transactionReference").exists());
    }

    @Test
    void fullFlow_bookThenPayWithGiftCard_returns200Confirmed() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "gc-flow@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.GIFT_CARD, null, null, "GIFT12345678");
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));
    }

    @Test
    void payment_nonExistentBooking_returns404() throws Exception {
        PaymentRequest payReq = new PaymentRequest(PaymentMethod.UPI, "user@upi", null, null);
        mockMvc.perform(post("/api/v1/bookings/" + UUID.randomUUID() + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    void payment_duplicatePayment_returns400() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "dup-pay@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.UPI, "user@okbank", null, null);
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk());

        // Pay again — already confirmed
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Booking is already paid and confirmed"));
    }

    @Test
    void payment_missingUpiId_returns400() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "no-upi@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.UPI, null, null, null);
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("UPI ID is required for UPI payment"));
    }

    @Test
    void payment_invalidCardNumber_returns400() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "bad-card@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.CARD, null, "123", null);
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid card number"));
    }

    @Test
    void payment_missingPaymentMethod_returns400() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "no-method@test.com");

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"upiId\": \"user@upi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payment_invalidUpiFormat_returns400() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "bad-upi-fmt@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.UPI, "nope-no-at", null, null);
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UPI ID format"));
    }

    @Test
    void payment_giftCardEmptyCode_returns400() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "empty-gc@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.GIFT_CARD, null, null, "");
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Gift card code is required for gift card payment"));
    }

    @Test
    void payment_cardWithSpaces_succeeds() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "card-spaces@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.CARD, null, "4111 1111 1111 1111", null);
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionReference").exists());
    }

    @Test
    void fullFlow_bookingStatusIsPendingBeforePayment() throws Exception {
        BookingRequest bookingReq = new BookingRequest("SG101", "Status Check", "status-chk@test.com", 1, null);
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.paymentDeadline").exists())
                .andExpect(jsonPath("$.paymentId").doesNotExist());
    }

    @Test
    void fullFlow_paymentResponseContainsAllFields() throws Exception {
        String bookingId = createBookingAndGetId("SG101", "all-fields@test.com");

        PaymentRequest payReq = new PaymentRequest(PaymentMethod.UPI, "user@okbank", null, null);
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.amount").isNumber())
                .andExpect(jsonPath("$.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.transactionReference").exists())
                .andExpect(jsonPath("$.processedAt").exists());
    }
}
