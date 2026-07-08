package com.accountingai.service;

import com.accountingai.model.BatchItemResult;

/**
 * Callback invoked once per file while a {@link BatchProcessor} works through a
 * batch of PDFs. Lets the UI (or a test) observe progress incrementally.
 *
 * <p>It is a {@link FunctionalInterface} so callers can pass a simple lambda,
 * e.g. {@code (completed, total, result) -> updateProgressBar(...)}.</p>
 */
@FunctionalInterface
public interface BatchProgressListener {

    /**
     * Called after each file in the batch has been processed.
     *
     * @param completed number of files processed so far (1-based)
     * @param total     total number of files in the batch
     * @param result    the per-file result just produced
     */
    void onItem(int completed, int total, BatchItemResult result);

    /**
     * A no-op listener that ignores all callbacks. Handy as a default when the
     * caller does not care about progress.
     */
    BatchProgressListener NOOP = (completed, total, result) -> { };
}
