package com.accountingai.service.export;

import com.accountingai.TestFixtures;
import com.accountingai.model.ExportData;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link XlsxExporter}.
 *
 * <p>Exports the sample fixture, reopens the produced bytes with POI's
 * {@link XSSFWorkbook}, and asserts the two expected sheets exist and the
 * Transactions sheet has one data row per sample transaction.</p>
 */
class XlsxExporterTest {

    @Test
    void workbookHasBothSheets() throws ExportException, IOException {
        ExportData data = TestFixtures.sampleExportData();
        XlsxExporter exporter = new XlsxExporter();

        byte[] bytes = exporter.toBytes(data);
        assertNotNull(bytes);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertNotNull(wb.getSheet("Statement"), "Workbook should have a 'Statement' sheet.");
            assertNotNull(wb.getSheet("Transactions"), "Workbook should have a 'Transactions' sheet.");
        }
    }

    @Test
    void transactionsSheetHasSevenDataRows() throws ExportException, IOException {
        ExportData data = TestFixtures.sampleExportData();
        XlsxExporter exporter = new XlsxExporter();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(exporter.toBytes(data)))) {
            Sheet txnSheet = wb.getSheet("Transactions");
            assertNotNull(txnSheet);

            // Row 0 is the header; 7 transactions -> last row index should be 7.
            int lastRow = txnSheet.getLastRowNum();
            assertTrue(lastRow >= 7,
                    "Transactions sheet should have a header plus 7 data rows (last index >= 7). Found: " + lastRow);
        }
    }
}
