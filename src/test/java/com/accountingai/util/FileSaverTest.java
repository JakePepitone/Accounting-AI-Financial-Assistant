package com.accountingai.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FileSaver}.
 *
 * <p>Verifies that {@code save} writes the exact bytes (creating any missing
 * parent directories) and that {@code ensureExtension} appends the extension only
 * when it is not already present (case-insensitively).</p>
 */
class FileSaverTest {

    @TempDir
    Path tmp;

    @Test
    void saveWritesBytesAndCreatesParentDirs() throws IOException {
        byte[] payload = "hello world".getBytes(StandardCharsets.UTF_8);
        // Target sits under a directory that does not exist yet.
        Path target = tmp.resolve("nested/deeper/out.bin");

        Path written = FileSaver.save(payload, target);

        assertEquals(target, written, "save should return the target path.");
        assertTrue(Files.exists(target), "The file should have been created.");
        assertArrayEquals(payload, Files.readAllBytes(target), "The written bytes should match the input.");
    }

    @Test
    void ensureExtensionAppendsWhenMissing() {
        Path target = tmp.resolve("report");
        Path result = FileSaver.ensureExtension(target, "csv");
        assertTrue(result.getFileName().toString().endsWith(".csv"),
                "Extension should be appended when missing.");
    }

    @Test
    void ensureExtensionDoesNotDoubleAppend() {
        Path target = tmp.resolve("report.csv");
        Path result = FileSaver.ensureExtension(target, "csv");
        assertEquals("report.csv", result.getFileName().toString(),
                "Extension should not be appended twice.");
    }

    @Test
    void ensureExtensionIsCaseInsensitive() {
        Path target = tmp.resolve("REPORT.CSV");
        Path result = FileSaver.ensureExtension(target, "csv");
        assertEquals("REPORT.CSV", result.getFileName().toString(),
                "An existing extension in different case should be accepted as-is.");
    }
}
