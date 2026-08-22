package com.openai.paypalwearshortcut;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class CredentialStore {
    private static final String PREFS = "paypal_local_vault";
    private static final String KEY_ALIAS = "paypal_local_autofill_key_v1";
    private static final String ACCOUNT = "account";
    private static final String PASSWORD = "password";
    private static final String EXPECTED_LAUNCH = "expected_paypal_launch";
    private static final long EXPECTED_WINDOW_MS = 120_000L;

    private CredentialStore() {}

    public static void saveCredential(Context context, String account, String password) throws Exception {
        SharedPreferences p = prefs(context);
        p.edit()
                .putString(ACCOUNT, encrypt(account))
                .putString(PASSWORD, encrypt(password))
                .apply();
    }

    public static boolean hasCredential(Context context) {
        SharedPreferences p = prefs(context);
        return p.contains(ACCOUNT) && p.contains(PASSWORD);
    }

    public static String getAccount(Context context) {
        try {
            return decrypt(prefs(context).getString(ACCOUNT, null));
        } catch (Exception e) {
            return "";
        }
    }

    public static String getPassword(Context context) {
        try {
            return decrypt(prefs(context).getString(PASSWORD, null));
        } catch (Exception e) {
            return "";
        }
    }

    public static void deleteCredential(Context context) {
        prefs(context).edit().remove(ACCOUNT).remove(PASSWORD).apply();
    }

    public static void markExpectedPayPalLaunch(Context context) {
        prefs(context).edit().putLong(EXPECTED_LAUNCH, System.currentTimeMillis()).apply();
    }

    public static boolean isRecentExpectedPayPalLaunch(Context context) {
        long t = prefs(context).getLong(EXPECTED_LAUNCH, 0L);
        return t > 0L && System.currentTimeMillis() - t <= EXPECTED_WINDOW_MS;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }

    private static String encrypt(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private static String decrypt(String value) throws Exception {
        if (value == null || value.isEmpty()) return "";
        String[] parts = value.split(":", 2);
        if (parts.length != 2) return "";
        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }
}
