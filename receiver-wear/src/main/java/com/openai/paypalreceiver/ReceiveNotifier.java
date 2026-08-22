package com.openai.paypalreceiver;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

final class ReceiveNotifier {
    private static final String CHANNEL_ID = "paypal_receive";
    private static final String PREFS = "pending_receive";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TEXT = "text";
    private static final String KEY_TIME = "time";

    private ReceiveNotifier() {}

    static void showOrStore(Context context, String title, String text, long postedAt) {
        if (!canPostNotifications(context)) {
            storePending(context, title, text, postedAt);
            return;
        }
        show(context, title, text, postedAt);
    }

    static void flushPending(Context context) {
        if (!canPostNotifications(context)) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_TIME)) return;
        String title = prefs.getString(KEY_TITLE, "PayPal 收款");
        String text = prefs.getString(KEY_TEXT, "收到一条 PayPal 收款通知");
        long time = prefs.getLong(KEY_TIME, System.currentTimeMillis());
        prefs.edit().clear().apply();
        show(context, title, text, time);
    }

    static String composeContent(String title, String text) {
        String cleanTitle = title == null ? "" : title.trim();
        String cleanText = text == null ? "" : text.trim();
        String payment;
        if (cleanTitle.isEmpty() || "PayPal".equalsIgnoreCase(cleanTitle)) payment = cleanText;
        else if (cleanText.isEmpty()) payment = cleanTitle;
        else payment = cleanTitle + " · " + cleanText;
        if (payment.isEmpty()) payment = "收到一笔 PayPal 款项";
        return payment + "\n点此提现到 BOA";
    }

    private static void show(Context context, String title, String text, long postedAt) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        ensureChannel(manager);

        Intent openIntent = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_WITHDRAW, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) (postedAt ^ (postedAt >>> 32)),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String content = composeContent(title, text);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("PayPal 收款 · 提现到 BOA")
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setWhen(postedAt)
                .setShowWhen(true);

        manager.notify((int) (postedAt ^ (postedAt >>> 32)), builder.build());
    }

    private static void storePending(Context context, String title, String text, long postedAt) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TITLE, title == null ? "PayPal 收款" : title)
                .putString(KEY_TEXT, text == null ? "收到一条 PayPal 收款通知" : text)
                .putLong(KEY_TIME, postedAt)
                .apply();
    }

    private static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static void ensureChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "PayPal 收款 → BOA",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("收到 PayPal 款项后快速进入 BOA 提现流程");
            manager.createNotificationChannel(channel);
        }
    }
}
