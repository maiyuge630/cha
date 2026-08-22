package com.openai.paypalreceiver;

import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class PayPalNotificationListener extends NotificationListenerService {
    private static final String PREFS = "receiver_state";
    private static final String KEY_LAST_SIGNATURE = "last_signature";
    private static final String KEY_LAST_TIME = "last_time";
    private static final long DUPLICATE_WINDOW_MS = 5000L;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        if (!IncomingPayPalNotification.PAYPAL_PACKAGE.equals(sbn.getPackageName())) return;

        Notification notification = sbn.getNotification();
        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) return;

        Bundle extras = notification.extras;
        String title = asString(extras.getCharSequence(Notification.EXTRA_TITLE));
        String text = asString(extras.getCharSequence(Notification.EXTRA_TEXT));
        String bigText = asString(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));

        if (!IncomingPayPalNotification.shouldForward(sbn.getPackageName(), title, text, bigText)) return;

        String bestTitle = IncomingPayPalNotification.bestTitle(title);
        String bestText = IncomingPayPalNotification.bestText(text, bigText);
        String signature = sbn.getKey() + "|" + bestTitle + "|" + bestText;
        long now = System.currentTimeMillis();

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (signature.equals(prefs.getString(KEY_LAST_SIGNATURE, ""))
                && now - prefs.getLong(KEY_LAST_TIME, 0L) < DUPLICATE_WINDOW_MS) {
            return;
        }
        prefs.edit()
                .putString(KEY_LAST_SIGNATURE, signature)
                .putLong(KEY_LAST_TIME, now)
                .apply();

        WearBridge.sendReceiveAlert(this, sbn.getKey(), bestTitle, bestText, sbn.getPostTime());
    }

    private static String asString(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
