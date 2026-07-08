package com.accountingai.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Small helper for writing byte content to disk.
 * <p>
 * Used by the export layer to persist generated CSV/XLSX/PDF bytes. Ensures the
 * target's parent directories exist and can normalize a file extension. All
 * methods are static.
 */
public final class FileSaver {

    /** Utility class: no instances. */
    private FileSaver() {
    }

    /**
     * Writes the given bytes to {@code target}, creating any missing parent
     * directories first. Overwrites the file if it already exists.
     *
     * @param data   the bytes to write
     * @param target the destination path
     * @return the {@code target} path
     * @throws IOException if the parent cannot be created or the write fails
     */
    public static Path save(byte[] data, Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, data);
        return target;
    }

    /**
     * Ensures the target file name ends with {@code "." + ext}. If it already
     * ends with that extension (case-insensitive), the target is returned
     * unchanged; otherwise a sibling path with the extension appended is returned.
     *
     * @param target the path to normalize
     * @param ext    the desired extension without a leading dot (e.g. "csv")
     * @return the target with the extension guaranteed
     */
    public static Path ensureExtension(Path target, String ext) {
        String fileName = target.getFileName().toString();
        String suffix = "." + ext;
        // Case-insensitive comparison so "Report.CSV" is accepted for ext "csv".
        if (fileName.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
            return target;
        }
        // Append the extension by resolving a sibling with the new name.
        return target.resolveSibling(fileName + suffix);
    }
}
