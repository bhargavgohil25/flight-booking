package com.flightbooking.strategy;

import com.flightbooking.model.enums.PaymentMethod;
import com.flightbooking.model.request.PaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentProcessorTest {

    // --- UPI Processor ---

    private final UpiPaymentProcessor upi = new UpiPaymentProcessor();

    @Test
    void upi_getPaymentMethod() {
        assertThat(upi.getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
    }

    @Test
    void upi_validate_validId_noException() {
        upi.validate(new PaymentRequest(PaymentMethod.UPI, "john@okbank", null, null));
    }

    @Test
    void upi_validate_nullId_throws() {
        assertThatThrownBy(() -> upi.validate(new PaymentRequest(PaymentMethod.UPI, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UPI ID is required");
    }

    @Test
    void upi_validate_blankId_throws() {
        assertThatThrownBy(() -> upi.validate(new PaymentRequest(PaymentMethod.UPI, "  ", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UPI ID is required");
    }

    @Test
    void upi_validate_noAtSymbol_throws() {
        assertThatThrownBy(() -> upi.validate(new PaymentRequest(PaymentMethod.UPI, "invalid-upi", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UPI ID format");
    }

    @Test
    void upi_processPayment_returnsUpiPrefix() {
        String txn = upi.processPayment(new BigDecimal("5000"), new PaymentRequest(PaymentMethod.UPI, "user@upi", null, null));
        assertThat(txn).startsWith("UPI-");
        assertThat(txn).hasSize(12); // "UPI-" + 8 chars
    }

    // --- Card Processor ---

    private final CardPaymentProcessor card = new CardPaymentProcessor();

    @Test
    void card_getPaymentMethod() {
        assertThat(card.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void card_validate_validNumber_noException() {
        card.validate(new PaymentRequest(PaymentMethod.CARD, null, "4111111111111111", null));
    }

    @Test
    void card_validate_withSpaces_noException() {
        card.validate(new PaymentRequest(PaymentMethod.CARD, null, "4111 1111 1111 1111", null));
    }

    @Test
    void card_validate_nullNumber_throws() {
        assertThatThrownBy(() -> card.validate(new PaymentRequest(PaymentMethod.CARD, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number is required");
    }

    @Test
    void card_validate_tooShort_throws() {
        assertThatThrownBy(() -> card.validate(new PaymentRequest(PaymentMethod.CARD, null, "12345", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid card number");
    }

    @Test
    void card_validate_tooLong_throws() {
        assertThatThrownBy(() -> card.validate(new PaymentRequest(PaymentMethod.CARD, null, "12345678901234567890", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid card number");
    }

    @Test
    void card_validate_nonDigits_throws() {
        assertThatThrownBy(() -> card.validate(new PaymentRequest(PaymentMethod.CARD, null, "4111abcd11111111", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid card number");
    }

    @Test
    void card_processPayment_returnsCardPrefix() {
        String txn = card.processPayment(new BigDecimal("10000"), new PaymentRequest(PaymentMethod.CARD, null, "4111111111111111", null));
        assertThat(txn).startsWith("CARD-");
        assertThat(txn).hasSize(13); // "CARD-" + 8 chars
    }

    // --- Gift Card Processor ---

    private final GiftCardPaymentProcessor gc = new GiftCardPaymentProcessor();

    @Test
    void giftCard_getPaymentMethod() {
        assertThat(gc.getPaymentMethod()).isEqualTo(PaymentMethod.GIFT_CARD);
    }

    @Test
    void giftCard_validate_validCode_noException() {
        gc.validate(new PaymentRequest(PaymentMethod.GIFT_CARD, null, null, "GIFT12345678"));
    }

    @Test
    void giftCard_validate_nullCode_throws() {
        assertThatThrownBy(() -> gc.validate(new PaymentRequest(PaymentMethod.GIFT_CARD, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gift card code is required");
    }

    @Test
    void giftCard_validate_blankCode_throws() {
        assertThatThrownBy(() -> gc.validate(new PaymentRequest(PaymentMethod.GIFT_CARD, null, null, "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gift card code is required");
    }

    @Test
    void giftCard_validate_tooShort_throws() {
        assertThatThrownBy(() -> gc.validate(new PaymentRequest(PaymentMethod.GIFT_CARD, null, null, "ABC")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid gift card code");
    }

    @Test
    void giftCard_processPayment_returnsGcPrefix() {
        String txn = gc.processPayment(new BigDecimal("8000"), new PaymentRequest(PaymentMethod.GIFT_CARD, null, null, "GIFT12345678"));
        assertThat(txn).startsWith("GC-");
        assertThat(txn).hasSize(11); // "GC-" + 8 chars
    }
}
