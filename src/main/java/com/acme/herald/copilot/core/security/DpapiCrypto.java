package com.acme.herald.copilot.core.security;

import com.sun.jna.platform.win32.Crypt32Util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class DpapiCrypto {

    private DpapiCrypto() {
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    public static String encryptToBase64(String plaintext) {
        byte[] input = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] protectedBytes = Crypt32Util.cryptProtectData(input);
        return Base64.getEncoder().encodeToString(protectedBytes);
    }

    public static String decryptFromBase64(String base64Ciphertext) {
        byte[] protectedBytes = Base64.getDecoder().decode(base64Ciphertext);
        byte[] unprotected = Crypt32Util.cryptUnprotectData(protectedBytes);
        return new String(unprotected, StandardCharsets.UTF_8);
    }
}