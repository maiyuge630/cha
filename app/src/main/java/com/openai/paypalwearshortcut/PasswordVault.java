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

final class PasswordVault {
    private static final String PREFS = "paypal_password_v09";
    private static final String PASSWORD = "password_ciphertext";
    private static final String KEY_ALIAS = "paypal_password_clipboard_v09_aes";

    private PasswordVault() {}

    static void save(Context context, String password) throws Exception {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is empty");
        }
        prefs(context).edit().putString(PASSWORD, encrypt(password)).commit();
    }

    static boolean hasPassword(Context context) {
        return prefs(context).contains(PASSWORD);
    }

    static String get(Context context) {
        try {
            return decrypt(prefs(context).getString(PASSWORD, null));
        } catch (Exception e) {
            return "";
        }
    }

    static void delete(Context context) {
        prefs(context).edit().remove(PASSWORD).commit();
    }

    static String storedCiphertextForTest(Context context) {
        return prefs(context).getString(PASSWORD, "");
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

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
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

    private static String decrypt(String stored) throws Exception {
        if (stored == null || stored.isEmpty()) return "";
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return "";

        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }
}
