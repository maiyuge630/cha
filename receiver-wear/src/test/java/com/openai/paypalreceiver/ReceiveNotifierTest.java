package com.openai.paypalreceiver;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReceiveNotifierTest {
    @Test
    public void paypalTitleUsesMessageOnly() {
        assertEquals("Alex sent you $25.00",
                ReceiveNotifier.composeContent("PayPal", "Alex sent you $25.00"));
    }

    @Test
    public void senderTitleIsPreserved() {
        assertEquals("Alex · sent you $25.00",
                ReceiveNotifier.composeContent("Alex", "sent you $25.00"));
    }
}
