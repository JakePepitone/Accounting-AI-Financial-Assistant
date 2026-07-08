package com.accountingai.model;

import java.io.File;

/**
 * User-configurable application settings.
 * <p>
 * Persisted (by the settings service) as a simple properties file. Provides a
 * {@link #defaults()} factory with sensible starting values and a copy
 * constructor so callers can edit a snapshot without mutating the cached
 * instance.
 */
public class Settings {

    /** Folder used to pre-select the save location for exports. */
    private String defaultExportFolder;

    /** Default export format: one of "CSV", "XLSX", "PDF". */
    private String defaultExportFormat;

    /** UI theme: "Light" or "Dark". */
    private String theme;

    /** Number of rows to show per page in list/table views. */
    private int pageSize;

    /** No-arg constructor required for framework/database mapping. */
    public Settings() {
    }

    /**
     * All-args constructor.
     *
     * @param defaultExportFolder the default export folder
     * @param defaultExportFormat the default export format ("CSV"/"XLSX"/"PDF")
     * @param theme               the UI theme ("Light"/"Dark")
     * @param pageSize            the rows-per-page value
     */
    public Settings(String defaultExportFolder, String defaultExportFormat, String theme, int pageSize) {
        this.defaultExportFolder = defaultExportFolder;
        this.defaultExportFormat = defaultExportFormat;
        this.theme = theme;
        this.pageSize = pageSize;
    }

    /**
     * Copy constructor. Produces an independent copy of {@code other}.
     *
     * @param other the settings to copy (must not be null)
     */
    public Settings(Settings other) {
        this.defaultExportFolder = other.defaultExportFolder;
        this.defaultExportFormat = other.defaultExportFormat;
        this.theme = other.theme;
        this.pageSize = other.pageSize;
    }

    /**
     * Builds a {@link Settings} instance with sensible default values:
     * export folder = the user's Documents directory (or "" if unavailable),
     * format = "CSV", theme = "Light", page size = 50.
     *
     * @return a new default {@link Settings}
     */
    public static Settings defaults() {
        String documents = "";
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            File docs = new File(home, "Documents");
            // Prefer the Documents folder if it exists; otherwise leave blank.
            if (docs.isDirectory()) {
                documents = docs.getAbsolutePath();
            }
        }
        return new Settings(documents, "CSV", "Light", 50);
    }

    public String getDefaultExportFolder() {
        return defaultExportFolder;
    }

    public void setDefaultExportFolder(String defaultExportFolder) {
        this.defaultExportFolder = defaultExportFolder;
    }

    public String getDefaultExportFormat() {
        return defaultExportFormat;
    }

    public void setDefaultExportFormat(String defaultExportFormat) {
        this.defaultExportFormat = defaultExportFormat;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "defaultExportFolder='" + defaultExportFolder + '\'' +
                ", defaultExportFormat='" + defaultExportFormat + '\'' +
                ", theme='" + theme + '\'' +
                ", pageSize=" + pageSize +
                '}';
    }
}
