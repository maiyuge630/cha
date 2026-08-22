package com.openai.paypalwearshortcut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;

import org.junit.Test;

public class PaymentLinkTest {
    @Test
    public void parsesAndNormalizesAmount() {
        BigDecimal amount = PaymentLink.parseAmount(" 25.00 ");
        assertEquals("25.00", amount.toPlainString());
        assertEquals(
                "https://www.paypal.me/2137765821/25USD",
                PaymentLink.buildPayPalMeUrl("2137765821", amount));
    }

    @Test
    public void keepsSmallDecimalAmount() {
        BigDecimal amount = PaymentLink.parseAmount("0.50");
        assertEquals(
                "https://www.paypal.me/2137765821/0.5USD",
                PaymentLink.buildPayPalMeUrl("2137765821", amount));
    }

    @Test
    public void rejectsZeroAndNegativeAmounts() {
        assertThrows(NumberFormatException.class, () -> PaymentLink.parseAmount("0"));
        assertThrows(NumberFormatException.class, () -> PaymentLink.parseAmount("-1"));
    }

    @Test
    public void rejectsInvalidRecipient() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentLink.buildPayPalMeUrl("bad/name", new BigDecimal("1")));
    }
}
