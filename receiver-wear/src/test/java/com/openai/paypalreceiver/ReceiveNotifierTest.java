package com.openai.paypalreceiver;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReceiveNotifierTest {
    @Test
    public void paypalTitleUsesMessageAndBoaAction() {
        assertEquals("Alex sent you $25.00\n点此提现到 BOA",
                ReceiveNotifier.composeContent("PayPal", "Alex sent you $25.00"));
    }

    @Test
    public void senderTitleIsPreservedWithBoaAction() {
        assertEquals("Alex · sent you $25.00\n点此提现到 BOA",
                ReceiveNotifier.composeContent("Alex", "sent you $25.00"));
    }

    @Test
    public void emptyNotificationStillHasUsefulAction() {
        assertEquals("收到一笔 PayPal 款项\n点此提现到 BOA",
                ReceiveNotifier.composeContent("", ""));
    }
}
