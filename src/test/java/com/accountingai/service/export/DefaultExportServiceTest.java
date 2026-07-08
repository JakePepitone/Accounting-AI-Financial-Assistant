package com.accountingai.service.export;

import com.accountingai.TestFixtures;
import com.accountingai.model.ExportData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DefaultExportService}.
 *
 * <p>Confirms that a CSV export writes a real file (with the ".csv" extension
 * enforced) and that requesting the unsupported DOCX format raises an
 * {@link ExportException}.</p>
 */
class DefaultExportServiceTest {

    @TempDir
    Path tmp;

    @Test
    void csvExportWritesFileAndEnforcesExtension() throws ExportException {
        ExportData data = TestFixtures.sampleExportData();
        DefaultExportService service = new DefaultExportService();

        // Deliberately omit the extension to prove ensureExtension appends ".csv".
        Path target = tmp.resolve("export_result");
        Path written = service.export(data, ExportFormat.CSV, target);

        assertTrue(Files.exists(written), "The exported CSV file should exist on disk.");
        assertTrue(written.getFileName().toString().toLowerCase().endsWith(".csv"),
                "The exporter should ensure a .csv extension.");
    }

    @Test
    void docxExportThrowsExportException() {
        ExportData data = TestFixtures.sampleExportData();
        DefaultExportService service = new DefaultExportService();
        Path target = tmp.resolve("unsupported.docx");

        assertThrows(ExportException.class,
                () -> service.export(data, ExportFormat.DOCX, target),
                "DOCX export is not available in the MVP and must throw.");
    }

    @Test
    void xlsxExportWritesFile() throws ExportException {
        ExportData data = TestFixtures.sampleExportData();
        DefaultExportService service = new DefaultExportService();

        Path target = tmp.resolve("book");
        Path written = service.export(data, ExportFormat.XLSX, target);

        assertTrue(Files.exists(written));
        assertTrue(written.getFileName().toString().toLowerCase().endsWith(".xlsx"));
    }
}
