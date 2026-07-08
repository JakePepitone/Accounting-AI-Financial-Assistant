package com.accountingai.service.export;

import com.accountingai.model.ExportData;

/**
 * Strategy interface for turning a statement's {@link ExportData} into the raw
 * bytes of a specific file format (CSV, XLSX, PDF, ...).
 * <p>
 * Implementations are format specific and stateless; the {@link ExportService}
 * picks the right one and is responsible for actually writing the returned bytes
 * to disk. Kept package-private on purpose — callers use {@link ExportService}.
 */
interface Exporter {

    /**
     * Serializes the given export data into a byte array in this exporter's format.
     *
     * @param data the account/statement/transactions to render (must not be null)
     * @return the encoded file contents as bytes
     * @throws ExportException if serialization fails (e.g. an I/O error)
     */
    byte[] toBytes(ExportData data) throws ExportException;
}
