package com.accountingai.service.export;

/**
 * Enumeration of the file formats the application can export a bank statement to.
 * <p>
 * Each constant carries a lowercase file {@code extension} (without the dot) and a
 * human friendly {@code label} suitable for showing in the UI. The {@code DOCX}
 * value is intentionally present but not supported in the MVP; {@link #isSupported()}
 * lets callers filter it out gracefully.
 */
public enum ExportFormat {

    /** Comma separated values. */
    CSV("csv", "CSV"),
    /** Microsoft Excel workbook (Office Open XML). */
    XLSX("xlsx", "Excel (.xlsx)"),
    /** Portable Document Format. */
    PDF("pdf", "PDF"),
    /** Microsoft Word document — placeholder, not implemented in the MVP. */
    DOCX("docx", "Word (coming soon)");

    /** File extension for this format, lowercase, no leading dot (e.g. "csv"). */
    private final String extension;

    /** Human readable label for menus and dialogs (e.g. "Excel (.xlsx)"). */
    private final String label;

    /**
     * Creates a format constant.
     *
     * @param extension lowercase file extension without the dot
     * @param label     user facing display name
     */
    ExportFormat(String extension, String label) {
        this.extension = extension;
        this.label = label;
    }

    /**
     * @return the lowercase file extension (no leading dot) for this format
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @return the human readable label for this format
     */
    public String getLabel() {
        return label;
    }

    /**
     * Indicates whether this format is actually implemented in the MVP.
     *
     * @return {@code true} for every format except {@link #DOCX}
     */
    public boolean isSupported() {
        return this != DOCX;
    }
}
