package com.accountingai.service.export;

import com.accountingai.TestFixtures;
import com.accountingai.model.ExportData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CsvExporter}.
 *
 * <p>Exports the sample fixture and inspects the resulting UTF-8 CSV text to make
 * sure the transaction rows (description and signed amount) and the summary
 * details survived the round trip.</p>
 */
class CsvExporterTest {

    @Test
    void csvContainsTransactionRows() throws ExportException {
        ExportData data = TestFixtures.sampleExportData();
        CsvExporter exporter = new CsvExporter();

        byte[] bytes = exporter.toBytes(data);
        assertNotNull(bytes);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("Amazon Purchase"), "CSV should contain the transaction description.");
        assertTrue(csv.contains("-125.99"), "CSV should contain the signed transaction amount.");
        assertTrue(csv.contains("Payroll Deposit"), "CSV should contain the deposit description.");
    }

    @Test
    void csvContainsSummaryDetails() throws ExportException {
        ExportData data = TestFixtures.sampleExportData();
        CsvExporter exporter = new CsvExporter();

        String csv = new String(exporter.toBytes(data), StandardCharsets.UTF_8);
        assertTrue(csv.contains("John Smith"), "CSV summary should include the customer name.");
        assertTrue(csv.contains("123456789"), "CSV summary should include the account number.");
    }

    @Test
    void csvHasTransactionHeader() throws ExportException {
        ExportData data = TestFixtures.sampleExportData();
        CsvExporter exporter = new CsvExporter();

        String csv = new String(exporter.toBytes(data), StandardCharsets.UTF_8);
        // The transactions table header row.
        assertTrue(csv.contains("Date") && csv.contains("Description") && csv.contains("Amount"),
                "CSV should contain the Date/Description/Amount header.");
    }
}
