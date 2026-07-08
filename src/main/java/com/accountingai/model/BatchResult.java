package com.accountingai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated results of a batch import run.
 * <p>
 * Holds a list of {@link BatchItemResult} entries (one per file) and exposes
 * convenience counts for the UI summary ("X succeeded, Y failed").
 */
public class BatchResult {

    /** The per-file results in processing order. */
    private final List<BatchItemResult> items;

    /**
     * All-args constructor.
     *
     * @param items the per-file results (null becomes an empty list)
     */
    public BatchResult(List<BatchItemResult> items) {
        // Defensive: never store null so the count helpers are safe.
        this.items = (items != null) ? items : new ArrayList<>();
    }

    /**
     * @return the list of per-file results
     */
    public List<BatchItemResult> getItems() {
        return items;
    }

    /**
     * @return the total number of files processed
     */
    public int total() {
        return items.size();
    }

    /**
     * @return the number of files that imported successfully
     */
    public int successCount() {
        int count = 0;
        for (BatchItemResult item : items) {
            if (item != null && item.success()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the number of files that failed to import
     */
    public int failureCount() {
        return total() - successCount();
    }

    @Override
    public String toString() {
        return "BatchResult{" +
                "total=" + total() +
                ", success=" + successCount() +
                ", failure=" + failureCount() +
                '}';
    }
}
