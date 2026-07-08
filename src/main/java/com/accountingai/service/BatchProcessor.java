package com.accountingai.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.accountingai.model.BatchItemResult;
import com.accountingai.model.BatchResult;
import com.accountingai.model.ImportResult;

/**
 * Imports a whole list of PDF files in one go by delegating each file to a
 * {@link PdfImportService}. Processing continues even if individual files fail,
 * and progress is reported through a {@link BatchProgressListener}.
 */
public class BatchProcessor {

    private final PdfImportService importService;

    /**
     * @param importService the single-file import service used for each PDF
     */
    public BatchProcessor(PdfImportService importService) {
        this.importService = importService;
    }

    /**
     * Processes every file in {@code files}, in order, reporting progress.
     *
     * @param files    the PDFs to import (may be null or empty)
     * @param listener progress callback; if null, {@link BatchProgressListener#NOOP} is used
     * @return a {@link BatchResult} summarizing per-file outcomes
     */
    public BatchResult processFiles(List<Path> files, BatchProgressListener listener) {
        // Null listener -> use the shared no-op so we never NPE on callback.
        BatchProgressListener safeListener = (listener != null) ? listener : BatchProgressListener.NOOP;

        List<BatchItemResult> results = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return new BatchResult(results);
        }

        int total = files.size();
        for (int i = 0; i < total; i++) {
            Path file = files.get(i);
            String fileName = (file != null) ? file.getFileName().toString() : "(unknown)";
            BatchItemResult itemResult;

            try {
                ImportResult importResult = importService.importPdf(file);
                if (importResult != null && importResult.isSuccess()) {
                    itemResult = new BatchItemResult(fileName, true, "Imported");
                } else {
                    String message = (importResult != null) ? importResult.getMessage() : "Import failed";
                    itemResult = new BatchItemResult(fileName, false, message);
                }
            } catch (Exception e) {
                // Continue-on-error: capture the failure and move on to the next file.
                String message = (e.getMessage() != null) ? e.getMessage() : e.toString();
                itemResult = new BatchItemResult(fileName, false, message);
            }

            results.add(itemResult);
            // Report progress (completed count is 1-based).
            safeListener.onItem(i + 1, total, itemResult);
        }

        return new BatchResult(results);
    }
}
