package com.accountingai.service.export;

/**
 * Checked exception thrown when an export operation fails.
 * <p>
 * Wraps lower level problems (such as {@link java.io.IOException} from writing a
 * file or serializing a workbook) so that callers in the UI layer have a single,
 * meaningful exception type to catch and present to the user.
 */
public class ExportException extends Exception {

    /**
     * Creates an export exception with a descriptive message.
     *
     * @param message human readable description of what went wrong
     */
    public ExportException(String message) {
        super(message);
    }

    /**
     * Creates an export exception with a message and an underlying cause.
     *
     * @param message human readable description of what went wrong
     * @param cause   the original exception that triggered this failure
     */
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
