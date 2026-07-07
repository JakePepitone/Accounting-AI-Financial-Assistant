package com.accountingai.service;

import com.accountingai.model.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SettingsService}.
 *
 * <p>Uses a settings file inside a {@code @TempDir} so the real user settings are
 * never touched. Verifies first-call creation with defaults, a save/reload
 * round-trip, and that an out-of-range page size is clamped to [5, 200].</p>
 */
class SettingsServiceTest {

    @TempDir
    Path tmp;

    @Test
    void firstGetCurrentCreatesFileWithDefaults() {
        Path file = tmp.resolve("settings.properties");
        SettingsService service = new SettingsService(file);

        Settings current = service.getCurrent();
        assertNotNull(current);
        // Defaults per contract: format CSV, theme Light, pageSize 50.
        assertEquals("CSV", current.getDefaultExportFormat());
        assertEquals("Light", current.getTheme());
        assertEquals(50, current.getPageSize());

        assertTrue(Files.exists(file), "getCurrent() should create the settings file on first call.");
    }

    @Test
    void saveAndReloadRoundTrips() {
        Path file = tmp.resolve("roundtrip.properties");
        SettingsService service = new SettingsService(file);

        Settings s = Settings.defaults();
        s.setDefaultExportFormat("XLSX");
        s.setTheme("Dark");
        s.setPageSize(75);
        s.setDefaultExportFolder(tmp.toString());
        service.save(s);

        // A fresh service instance reading the same file should see the saved values.
        SettingsService reopened = new SettingsService(file);
        Settings loaded = reopened.load();
        assertEquals("XLSX", loaded.getDefaultExportFormat());
        assertEquals("Dark", loaded.getTheme());
        assertEquals(75, loaded.getPageSize());
        assertEquals(tmp.toString(), loaded.getDefaultExportFolder());
    }

    @Test
    void pageSizeIsClampedAboveMaximum() {
        Path file = tmp.resolve("clamp_high.properties");
        SettingsService service = new SettingsService(file);

        Settings s = Settings.defaults();
        s.setPageSize(9999); // way above the 200 max
        service.save(s);

        Settings loaded = new SettingsService(file).load();
        assertEquals(200, loaded.getPageSize(), "Page size above 200 should clamp to 200.");
    }

    @Test
    void pageSizeIsClampedBelowMinimum() {
        Path file = tmp.resolve("clamp_low.properties");
        SettingsService service = new SettingsService(file);

        Settings s = Settings.defaults();
        s.setPageSize(1); // below the 5 min
        service.save(s);

        Settings loaded = new SettingsService(file).load();
        assertEquals(5, loaded.getPageSize(), "Page size below 5 should clamp to 5.");
    }

    @Test
    void updateAndSaveUpdatesCache() {
        Path file = tmp.resolve("update.properties");
        SettingsService service = new SettingsService(file);
        service.getCurrent(); // prime the cache with defaults

        Settings s = Settings.defaults();
        s.setTheme("Dark");
        service.updateAndSave(s);

        assertEquals("Dark", service.getCurrent().getTheme(),
                "updateAndSave should refresh the in-memory cache.");
    }
}
