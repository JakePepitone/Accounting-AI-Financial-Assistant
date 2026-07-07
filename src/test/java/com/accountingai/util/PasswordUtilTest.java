package com.accountingai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for {@link PasswordUtil}.
 *
 * <p>Locks the seed contract: {@code sha256("1234")} MUST equal the exact hash
 * stored in {@code seed.sql} for the default {@code admin} user, otherwise the
 * default login would break. Also sanity-checks lower-case hex output and that
 * different inputs hash differently.</p>
 */
class PasswordUtilTest {

    /** The lowercase-hex SHA-256 of the string "1234", as seeded for admin. */
    private static final String EXPECTED_1234_HASH =
            "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4";

    @Test
    void sha256OfDefaultPasswordMatchesSeed() {
        assertEquals(EXPECTED_1234_HASH, PasswordUtil.sha256("1234"),
                "sha256(\"1234\") must match the seeded admin password hash.");
    }

    @Test
    void outputIsLowercaseHexOf64Chars() {
        String hash = PasswordUtil.sha256("anything");
        assertEquals(64, hash.length(), "SHA-256 hex output should be 64 characters.");
        assertEquals(hash.toLowerCase(), hash, "Hash should be lower-case hex.");
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        assertNotEquals(PasswordUtil.sha256("1234"), PasswordUtil.sha256("12345"),
                "Distinct inputs should hash to distinct values.");
    }
}
