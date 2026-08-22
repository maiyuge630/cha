package com.openai.paypalreceiver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IncomingPayPalNotificationTest {
    @Test
    public void acceptsEnglishIncomingPayment() {
        assertTrue(IncomingPayPalNotification.shouldForward(
                IncomingPayPalNotification.PAYPAL_PACKAGE,
                "PayPal", "Alex sent you $25.00", ""));
    }

    @Test
    public void acceptsChineseIncomingPayment() {
        assertTrue(IncomingPayPalNotification.shouldForward(
                IncomingPayPalNotification.PAYPAL_PACKAGE,
                "PayPal", "你收到了一笔 $10.00 的付款", ""));
    }

    @Test
    public void rejectsOutgoingPayment() {
        assertFalse(IncomingPayPalNotification.shouldForward(
                IncomingPayPalNotification.PAYPAL_PACKAGE,
                "PayPal", "You sent $12.00 to Alex", ""));
    }

    @Test
    public void rejectsOtherApps() {
        assertFalse(IncomingPayPalNotification.shouldForward(
                "com.example.other", "PayPal", "Alex sent you $25.00", ""));
    }

    @Test
    public void rejectsGenericPayPalMarketing() {
        assertFalse(IncomingPayPalNotification.shouldForward(
                IncomingPayPalNotification.PAYPAL_PACKAGE,
                "PayPal", "Check out your latest rewards", ""));
    }
}
