package com.accountingai.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small password-hashing helper.
 * <p>
 * The application stores only SHA-256 hashes of passwords, never plain text.
 * {@link #sha256(String)} is contract-locked: {@code sha256("1234")} must equal
 * {@code "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"},
 * which is the seeded {@code admin} user's password hash.
 * <p>
 * Note: plain SHA-256 (no salt) is intentionally simple for this student
 * capstone; it is not recommended for real production credential storage.
 */
public final class PasswordUtil {

    /** Utility class: no instances. */
    private PasswordUtil() {
    }

    /**
     * Computes the lowercase-hex SHA-256 hash of the given input.
     *
     * @param input the string to hash (UTF-8 encoded); must not be null
     * @return the 64-character lowercase-hex SHA-256 digest
     * @throws IllegalArgumentException if {@code input} is null
     * @throws IllegalStateException    if SHA-256 is unavailable (should never happen)
     */
    public static String sha256(String input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a required algorithm on every JVM; this is unreachable.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts a byte array to a lowercase hexadecimal string.
     *
     * @param bytes the bytes to encode
     * @return the lowercase hex representation
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // & 0xff to treat the byte as unsigned; %02x pads to two hex digits.
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
