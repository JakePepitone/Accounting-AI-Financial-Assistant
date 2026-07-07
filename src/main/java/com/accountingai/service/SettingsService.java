package com.accountingai.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.accountingai.model.Settings;
import com.accountingai.util.AppPaths;

/**
 * Loads and persists user {@link Settings} to a {@code settings.properties}
 * file using {@link Properties}. Values are cached after the first load; the
 * cache is refreshed by {@link #updateAndSave(Settings)}.
 *
 * <p>Property keys on disk: {@code exportFolder}, {@code exportFormat},
 * {@code theme}, {@code pageSize}. Any missing or malformed value falls back to
 * the corresponding {@link Settings#defaults()} value; {@code pageSize} is
 * additionally clamped to the range [5, 200]. IO failures are surfaced as an
 * unchecked {@link SettingsException}.</p>
 */
public class SettingsService {

    // Property keys (kept as constants so load/save stay in sync).
    private static final String KEY_EXPORT_FOLDER = "exportFolder";
    private static final String KEY_EXPORT_FORMAT = "exportFormat";
    private static final String KEY_THEME = "theme";
    private static final String KEY_PAGE_SIZE = "pageSize";

    // pageSize clamp bounds.
    private static final int MIN_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 200;

    private final Path file;

    /** Cached settings; lazily populated by {@link #getCurrent()}. */
    private Settings cached;

    /** Uses the standard application settings file location. */
    public SettingsService() {
        this(AppPaths.settingsFile());
    }

    /**
     * @param file the properties file to read/write (useful for tests)
     */
    public SettingsService(Path file) {
        this.file = file;
    }

    /**
     * Returns the current settings, loading (and creating the file with
     * defaults on first run) if not already cached.
     *
     * @return the cached {@link Settings}
     */
    public Settings getCurrent() {
        if (cached == null) {
            if (!Files.exists(file)) {
                // First run: materialize defaults to disk so the file always exists.
                Settings defaults = Settings.defaults();
                save(defaults);
                cached = defaults;
            } else {
                cached = load();
            }
        }
        return cached;
    }

    /**
     * Reads settings from disk, applying per-field fallbacks and the pageSize
     * clamp. Does not update the cache.
     *
     * @return a fully-populated {@link Settings}
     */
    public Settings load() {
        Settings defaults = Settings.defaults();

        // If there is no file yet, just hand back defaults.
        if (!Files.exists(file)) {
            return new Settings(defaults);
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            throw new SettingsException("Failed to read settings file: " + file, e);
        }

        Settings settings = new Settings();

        // Export folder: fall back to default when missing/blank.
        String folder = props.getProperty(KEY_EXPORT_FOLDER);
        settings.setDefaultExportFolder(
                (folder != null && !folder.isBlank()) ? folder : defaults.getDefaultExportFolder());

        // Export format: must be one of CSV/XLSX/PDF, else default.
        String format = props.getProperty(KEY_EXPORT_FORMAT);
        settings.setDefaultExportFormat(isValidFormat(format) ? format : defaults.getDefaultExportFormat());

        // Theme: must be Light/Dark, else default.
        String theme = props.getProperty(KEY_THEME);
        settings.setTheme(isValidTheme(theme) ? theme : defaults.getTheme());

        // Page size: parse, clamp, or fall back to default on garbage.
        settings.setPageSize(parsePageSize(props.getProperty(KEY_PAGE_SIZE), defaults.getPageSize()));

        return settings;
    }

    /**
     * Writes the given settings to disk (creating parent directories as needed).
     * Does not update the cache.
     *
     * @param s the settings to persist (null falls back to defaults)
     */
    public void save(Settings s) {
        Settings toSave = (s != null) ? s : Settings.defaults();

        Properties props = new Properties();
        props.setProperty(KEY_EXPORT_FOLDER, nullToEmpty(toSave.getDefaultExportFolder()));
        props.setProperty(KEY_EXPORT_FORMAT, nullToEmpty(toSave.getDefaultExportFormat()));
        props.setProperty(KEY_THEME, nullToEmpty(toSave.getTheme()));
        // Clamp on the way out too, so the file never stores an out-of-range value.
        props.setProperty(KEY_PAGE_SIZE, Integer.toString(clampPageSize(toSave.getPageSize())));

        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Accounting AI settings");
            }
        } catch (IOException e) {
            throw new SettingsException("Failed to write settings file: " + file, e);
        }
    }

    /**
     * Saves the settings and refreshes the in-memory cache to match.
     *
     * @param s the settings to persist and cache
     */
    public void updateAndSave(Settings s) {
        save(s);
        // Cache a defensive copy so external mutations don't leak into the cache.
        this.cached = (s != null) ? new Settings(s) : Settings.defaults();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private boolean isValidFormat(String format) {
        return "CSV".equals(format) || "XLSX".equals(format) || "PDF".equals(format);
    }

    private boolean isValidTheme(String theme) {
        return "Light".equals(theme) || "Dark".equals(theme);
    }

    /** Parses a page-size string, clamping valid numbers and defaulting garbage. */
    private int parsePageSize(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return clampPageSize(fallback);
        }
        try {
            return clampPageSize(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return clampPageSize(fallback);
        }
    }

    /** Clamps a page size into the allowed [MIN, MAX] range. */
    private int clampPageSize(int value) {
        if (value < MIN_PAGE_SIZE) {
            return MIN_PAGE_SIZE;
        }
        if (value > MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        return value;
    }

    private String nullToEmpty(String s) {
        return (s != null) ? s : "";
    }

    /**
     * Unchecked exception used to wrap IO failures so callers are not forced to
     * handle checked exceptions for settings persistence.
     */
    public static class SettingsException extends RuntimeException {
        public SettingsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
