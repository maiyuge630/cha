package com.openai.paypalreceiver;

import java.util.Locale;

final class IncomingPayPalNotification {
    static final String PAYPAL_PACKAGE = "com.paypal.android.p2pmobile";

    private static final String[] INCOMING_MARKERS = new String[] {
            "sent you", "paid you", "payment from", "money from", "funds from",
            "you received", "you've received", "you have received", "received a payment",
            "you got paid", "you've got money", "money is waiting",
            "收到", "收到了", "收款", "到账", "已入账", "入账",
            "向你发送", "向您发送", "给你发", "给您发", "给你转", "给您转",
            "付款给你", "付款给您"
    };

    private static final String[] OUTGOING_MARKERS = new String[] {
            "you sent", "you paid", "payment sent", "sent a payment", "purchase complete",
            "你已发送", "您已发送", "你支付了", "您支付了", "付款成功", "已付款"
    };

    private IncomingPayPalNotification() {}

    static boolean shouldForward(String packageName, String title, String text, String bigText) {
        if (!PAYPAL_PACKAGE.equals(packageName)) return false;
        String combined = join(title, text, bigText).toLowerCase(Locale.ROOT);
        if (combined.trim().isEmpty()) return false;

        for (String marker : OUTGOING_MARKERS) {
            if (combined.contains(marker.toLowerCase(Locale.ROOT))) return false;
        }
        for (String marker : INCOMING_MARKERS) {
            if (combined.contains(marker.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    static String bestTitle(String title) {
        if (title == null || title.trim().isEmpty()) return "PayPal 收款";
        return title.trim();
    }

    static String bestText(String text, String bigText) {
        if (bigText != null && !bigText.trim().isEmpty()) return bigText.trim();
        if (text != null && !text.trim().isEmpty()) return text.trim();
        return "收到一条 PayPal 收款通知";
    }

    private static String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                if (builder.length() > 0) builder.append(' ');
                builder.append(value.trim());
            }
        }
        return builder.toString();
    }
}
