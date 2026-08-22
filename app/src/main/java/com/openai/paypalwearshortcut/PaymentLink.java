package com.openai.paypalwearshortcut;

import java.math.BigDecimal;

final class PaymentLink {
    private PaymentLink() {}

    static BigDecimal parseAmount(String raw) {
        BigDecimal amount = new BigDecimal(raw == null ? "" : raw.trim());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NumberFormatException("Amount must be positive");
        }
        return amount;
    }

    static String buildPayPalMeUrl(String recipient, BigDecimal amount) {
        if (recipient == null || !recipient.matches("[A-Za-z0-9]{1,20}")) {
            throw new IllegalArgumentException("Invalid PayPal.Me recipient");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        String amountText = amount.stripTrailingZeros().toPlainString();
        return "https://www.paypal.me/" + recipient + "/" + amountText + "USD";
    }
}
