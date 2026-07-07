package com.accountingai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trivial smoke test.
 *
 * <p>Its only job is to prove that the test harness itself is wired up correctly:
 * the Maven Surefire plugin discovers JUnit 5 (Jupiter) tests, compiles the test
 * source tree, and runs it. If this test executes and passes, the whole test
 * infrastructure is functional.</p>
 */
class SmokeTest {

    /** Always-true assertion — confirms Surefire + JUnit 5 are working. */
    @Test
    void junitIsWired() {
        assertTrue(true, "JUnit 5 and Surefire are correctly wired.");
    }
}
