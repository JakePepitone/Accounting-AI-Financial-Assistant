package com.accountingai.service.export;

import com.accountingai.model.ExportData;

import java.nio.file.Path;

/**
 * High level facade for exporting a statement's {@link ExportData} to a file on disk.
 * <p>
 * Implementations choose the appropriate {@link Exporter} for the requested
 * {@link ExportFormat}, serialize the data, and write it to the target path
 * (fixing up the file extension as needed).
 */
public interface ExportService {

    /**
     * Exports the given data to {@code target} in the requested format.
     *
     * @param data   the account/statement/transactions to export (must not be null)
     * @param format the desired output format
     * @param target the destination file path (extension is normalized if needed)
     * @return the path that was actually written (may differ from {@code target}
     *         if the extension was appended)
     * @throws ExportException if the format is unsupported or writing fails
     */
    Path export(ExportData data, ExportFormat format, Path target) throws ExportException;
}
