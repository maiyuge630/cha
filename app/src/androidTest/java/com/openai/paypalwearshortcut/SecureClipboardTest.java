package com.openai.paypalwearshortcut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SecureClipboardTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        SecureClipboard.clearIfOwned(context);
    }

    @After
    public void tearDown() {
        SecureClipboard.clearIfOwned(context);
    }

    @Test
    public void copiesSensitiveOwnedClipAndClearsIt() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            assertTrue(SecureClipboard.copyTemporary(context, "clip-secret", 5_000));
            assertTrue(SecureClipboard.isOwnedClip(context));

            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = clipboard.getPrimaryClip();
            assertTrue(clip != null && clip.getItemCount() == 1);
            assertEquals("clip-secret", clip.getItemAt(0).coerceToText(context).toString());

            if (Build.VERSION.SDK_INT >= 33) {
                assertTrue(SecureClipboard.isMarkedSensitive(context));
            }

            SecureClipboard.clearIfOwned(context);
            assertFalse(SecureClipboard.isOwnedClip(context));
        }
    }

    @Test
    public void shortTtlAutomaticallyClearsOwnedClip() throws Exception {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            assertTrue(SecureClipboard.copyTemporary(context, "short-lived", 150));
            assertTrue(SecureClipboard.isOwnedClip(context));
            Thread.sleep(700);
            assertFalse(SecureClipboard.isOwnedClip(context));
        }
    }
}
