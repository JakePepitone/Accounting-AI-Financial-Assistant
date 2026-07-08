package com.accountingai.util;

import java.io.File;
import java.nio.file.Path;

/**
 * Validates that a file is a usable PDF before import.
 * <p>
 * The checks are intentionally lightweight (name suffix + existence + non-empty)
 * rather than parsing the PDF structure; deeper validation happens later when
 * PDFBox actually loads the document. All methods are static.
 */
public final class FileFormatValidator {

    /** Utility class: no instances. */
    private FileFormatValidator() {
    }

    /**
     * Checks whether the given file looks like a valid, readable PDF.
     *
     * @param f the file to check
     * @return true if {@code f} is non-null, ends with ".pdf" (case-insensitive),
     *         exists, and is non-empty
     */
    public static boolean isPdf(File f) {
        return f != null
                && f.getName().toLowerCase().endsWith(".pdf")
                && f.exists()
                && f.length() > 0;
    }

    /**
     * {@link Path} overload of {@link #isPdf(File)}.
     *
     * @param p the path to check
     * @return true if the path points to a valid PDF
     */
    public static boolean isPdf(Path p) {
        return p != null && isPdf(p.toFile());
    }

    /**
     * Validates a file and returns a human-readable problem description, or
     * {@code null} if the file is a valid PDF.
     *
     * @param f the file to validate
     * @return null if valid; otherwise a message such as "File does not exist",
     *         "File is empty", or "Not a PDF file"
     */
    public static String validate(File f) {
        if (f == null || !f.exists()) {
            return "File does not exist";
        }
        // Check the extension before the size so a mislabeled file reports the
        // most specific/actionable problem.
        if (!f.getName().toLowerCase().endsWith(".pdf")) {
            return "Not a PDF file";
        }
        if (f.length() <= 0) {
            return "File is empty";
        }
        return null;
    }
}
