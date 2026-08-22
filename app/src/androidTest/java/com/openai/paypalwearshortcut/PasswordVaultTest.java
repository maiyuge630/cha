package com.openai.paypalwearshortcut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PasswordVaultTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        PasswordVault.delete(context);
    }

    @After
    public void tearDown() {
        PasswordVault.delete(context);
    }

    @Test
    public void saveRoundTripAndCiphertextIsNotPlaintext() throws Exception {
        String password = "test-secret-PayPal-123!";
        PasswordVault.save(context, password);

        assertTrue(PasswordVault.hasPassword(context));
        assertEquals(password, PasswordVault.get(context));

        String stored = PasswordVault.storedCiphertextForTest(context);
        assertFalse(stored.isEmpty());
        assertFalse(stored.equals(password));
        assertFalse(stored.contains(password));
    }

    @Test
    public void deleteRemovesPassword() throws Exception {
        PasswordVault.save(context, "temporary-secret");
        PasswordVault.delete(context);

        assertFalse(PasswordVault.hasPassword(context));
        assertEquals("", PasswordVault.get(context));
    }
}
