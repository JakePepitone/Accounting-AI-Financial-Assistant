package com.accountingai.model;

/**
 * A single search hit.
 * <p>
 * Immutable record describing where a match was found.
 *
 * @param source     the origin of the hit, e.g. {@code "TEXT"} or {@code "TRANSACTION"}
 * @param label      a short label for the match (e.g. the matched value or field)
 * @param lineNumber the 1-based line number within the source (or 0 if N/A)
 * @param context    surrounding context text for the match
 */
public record SearchResult(String source, String label, int lineNumber, String context) {
}
