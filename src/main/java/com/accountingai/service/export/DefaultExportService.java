package com.accountingai.service.export;

import com.accountingai.model.ExportData;
import com.accountingai.util.FileSaver;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Default {@link ExportService} implementation.
 * <p>
 * Picks the right {@link Exporter} for the requested {@link ExportFormat},
 * serializes the data to bytes, normalizes the target file's extension, and
 * writes the bytes to disk via {@link FileSaver}.
 */
public class DefaultExportService implements ExportService {

    // The concrete exporters are stateless, so a single instance of each is reused.
    private final Exporter csvExporter = new CsvExporter();
    private final Exporter xlsxExporter = new XlsxExporter();
    private final Exporter pdfExporter = new PdfExporter();

    /**
     * {@inheritDoc}
     */
    @Override
    public Path export(ExportData data, ExportFormat format, Path target) throws ExportException {
        if (data == null) {
            throw new ExportException("Export data is null");
        }
        if (format == null) {
            throw new ExportException("Export format is null");
        }
        if (target == null) {
            throw new ExportException("Export target path is null");
        }

        // Select the exporter for the requested format.
        Exporter exporter = exporterFor(format);

        // Serialize the data to bytes.
        byte[] bytes = exporter.toBytes(data);

        // Make sure the target file has the correct extension, then write it.
        Path normalized = FileSaver.ensureExtension(target, format.getExtension());
        try {
            return FileSaver.save(bytes, normalized);
        } catch (IOException e) {
            throw new ExportException("Failed to save export to " + normalized, e);
        }
    }

    /**
     * Resolves the appropriate exporter for a format, rejecting the not-yet-supported
     * Word format.
     *
     * @param format the requested format
     * @return the matching exporter
     * @throws ExportException for {@link ExportFormat#DOCX} or any unknown format
     */
    private Exporter exporterFor(ExportFormat format) throws ExportException {
        switch (format) {
            case CSV:
                return csvExporter;
            case XLSX:
                return xlsxExporter;
            case PDF:
                return pdfExporter;
            case DOCX:
                throw new ExportException("Word export is not available in the MVP");
            default:
                // Defensive: covers any format added later without an exporter.
                throw new ExportException("Unsupported export format: " + format);
        }
    }
}
