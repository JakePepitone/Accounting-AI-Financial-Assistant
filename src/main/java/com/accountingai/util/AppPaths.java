package com.accountingai.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central resolver for all application file-system paths.
 * <p>
 * Everything lives under a per-user app-data directory so the SQLite database,
 * imported documents, exports, and settings are kept out of the project tree.
 * On Windows the base is {@code %LOCALAPPDATA%/AccountingAI}; elsewhere (or if
 * the env var is unset) it falls back to {@code user.home/.accounting-ai}.
 * <p>
 * All methods are static. Directory-returning methods create the directory if
 * it is missing and wrap any {@link IOException} as an
 * {@link UncheckedIOException} so callers do not need checked-exception handling.
 */
public final class AppPaths {

    /** Utility class: no instances. */
    private AppPaths() {
    }

    /**
     * Resolves (and creates if needed) the base application-data directory.
     *
     * @return the app-data directory path
     */
    public static Path appDataDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base;
        if (localAppData != null && !localAppData.isBlank()) {
            // Windows: %LOCALAPPDATA%/AccountingAI
            base = Paths.get(localAppData, "AccountingAI");
        } else {
            // Cross-platform fallback: ~/.accounting-ai
            String home = System.getProperty("user.home", ".");
            base = Paths.get(home, ".accounting-ai");
        }
        return createDir(base);
    }

    /**
     * @return the path to the SQLite database file (not created here; the DB
     *         layer creates the file on first connection)
     */
    public static Path databaseFile() {
        return appDataDir().resolve("accounting-ai.db");
    }

    /**
     * Resolves (and creates if needed) the directory where imported PDFs are copied.
     *
     * @return the document import-store directory
     */
    public static Path importStoreDir() {
        return createDir(appDataDir().resolve("documents"));
    }

    /**
     * @return the path to the settings properties file (not created here)
     */
    public static Path settingsFile() {
        return appDataDir().resolve("settings.properties");
    }

    /**
     * Resolves the default directory to use when prompting the user to save an
     * export. Prefers the user's Documents folder; if that does not exist, falls
     * back to (and creates) an {@code exports} directory under the app-data dir.
     *
     * @return the default export directory
     */
    public static Path defaultExportDir() {
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            Path documents = Paths.get(home, "Documents");
            if (Files.isDirectory(documents)) {
                return documents;
            }
        }
        return createDir(appDataDir().resolve("exports"));
    }

    /**
     * Creates the given directory (and parents) if it does not exist.
     *
     * @param dir the directory to create
     * @return the same directory path
     * @throws UncheckedIOException if the directory cannot be created
     */
    private static Path createDir(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create directory: " + dir, e);
        }
    }
}
