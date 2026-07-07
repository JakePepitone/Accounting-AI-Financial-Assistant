package com.accountingai.model;

/**
 * The result of processing one file within a batch import.
 * <p>
 * Immutable record used by the batch processor to report per-file outcomes.
 *
 * @param fileName the name of the file that was processed
 * @param success  true if that file imported successfully
 * @param message  a human-readable status or error message
 */
public record BatchItemResult(String fileName, boolean success, String message) {
}
