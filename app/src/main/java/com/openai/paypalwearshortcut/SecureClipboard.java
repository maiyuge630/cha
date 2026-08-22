package com.openai.paypalwearshortcut;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;

final class SecureClipboard {
    static final long DEFAULT_TTL_MS = 20_000L;
    private static final String LABEL = "PayPal temporary password";
    private static final String PREFS = "paypal_temp_clipboard_v09";
    private static final String EXPIRES_AT = "expires_at";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private SecureClipboard() {}

    static boolean copyTemporary(Context context, String value) {
        return copyTemporary(context, value, DEFAULT_TTL_MS);
    }

    static boolean copyTemporary(Context context, String value, long ttlMs) {
        if (value == null || value.isEmpty()) return false;
        ClipboardManager clipboard = clipboard(context);
        if (clipboard == null) return false;

        ClipData clip = ClipData.newPlainText(LABEL, value);
        if (Build.VERSION.SDK_INT >= 33) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
            clip.getDescription().setExtras(extras);
        }
        clipboard.setPrimaryClip(clip);

        long delay = Math.max(1L, ttlMs);
        long expiresAt = System.currentTimeMillis() + delay;
        prefs(context).edit().putLong(EXPIRES_AT, expiresAt).apply();

        Context appContext = context.getApplicationContext();
        MAIN_HANDLER.postDelayed(() -> clearIfOwned(appContext), delay);
        return true;
    }

    static void clearExpiredIfOwned(Context context) {
        long expiresAt = prefs(context).getLong(EXPIRES_AT, 0L);
        if (expiresAt > 0L && System.currentTimeMillis() >= expiresAt) {
            clearIfOwned(context);
        }
    }

    static void clearIfOwned(Context context) {
        ClipboardManager clipboard = clipboard(context);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipDescription description = clipboard.getPrimaryClipDescription();
            if (description != null && description.getLabel() != null &&
                    LABEL.contentEquals(description.getLabel())) {
                clipboard.clearPrimaryClip();
            }
        }
        prefs(context).edit().remove(EXPIRES_AT).apply();
    }

    static boolean isOwnedClip(Context context) {
        ClipboardManager clipboard = clipboard(context);
        if (clipboard == null || !clipboard.hasPrimaryClip()) return false;
        ClipDescription description = clipboard.getPrimaryClipDescription();
        return description != null && description.getLabel() != null &&
                LABEL.contentEquals(description.getLabel());
    }

    static boolean isMarkedSensitive(Context context) {
        ClipboardManager clipboard = clipboard(context);
        if (clipboard == null) return false;
        ClipDescription description = clipboard.getPrimaryClipDescription();
        if (description == null || description.getExtras() == null || Build.VERSION.SDK_INT < 33) {
            return false;
        }
        return description.getExtras().getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false);
    }

    private static ClipboardManager clipboard(Context context) {
        return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
