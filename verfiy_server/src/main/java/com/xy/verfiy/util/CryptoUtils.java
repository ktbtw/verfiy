package com.xy.verfiy.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class CryptoUtils {
    private CryptoUtils() {}

    public static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decryptRc4Base64(String base64Cipher, String key) {
        byte[] cipherBytes = Base64.getDecoder().decode(base64Cipher);
        byte[] plain = rc4(cipherBytes, key.getBytes(StandardCharsets.UTF_8));
        return new String(plain, StandardCharsets.UTF_8);
    }

    public static String encryptRc4ToBase64(String plain, String key) {
        byte[] data = plain.getBytes(StandardCharsets.UTF_8);
        byte[] cipher = rc4(data, key.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipher);
    }

    private static byte[] rc4(byte[] data, byte[] key) {
        byte[] s = new byte[256];
        for (int i = 0; i < 256; i++) s[i] = (byte) i;
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + key[i % key.length]) & 0xFF;
            byte tmp = s[i]; s[i] = s[j]; s[j] = tmp;
        }
        byte[] out = new byte[data.length];
        int i = 0; j = 0;
        for (int k = 0; k < data.length; k++) {
            i = (i + 1) & 0xFF;
            j = (j + s[i]) & 0xFF;
            byte tmp = s[i]; s[i] = s[j]; s[j] = tmp;
            int t = (s[i] + s[j]) & 0xFF;
            out[k] = (byte) (data[k] ^ s[t]);
        }
        return out;
    }

    public static String decryptAesCbcBase64(String base64Cipher, String key) {
        try {
            byte[] cipher = Base64.getDecoder().decode(base64Cipher);
            byte[] k = normalizeKey(key);
            byte[] iv = new byte[16]; // 全零 IV（示例）
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(k, "AES"), new IvParameterSpec(iv));
            byte[] plain = c.doFinal(cipher);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    public static String encryptAesCbcToBase64(String plain, String key) {
        try {
            byte[] k = normalizeKey(key);
            byte[] iv = new byte[16];
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, "AES"), new IvParameterSpec(iv));
            byte[] enc = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    private static byte[] normalizeKey(String key) {
        byte[] kb = key.getBytes(StandardCharsets.UTF_8);
        if (kb.length == 16 || kb.length == 24 || kb.length == 32) return kb;
        // pad or trim to 16 bytes by default
        byte[] out = new byte[16];
        for (int i = 0; i < out.length; i++) out[i] = (i < kb.length) ? kb[i] : 0;
        return out;
    }
}


