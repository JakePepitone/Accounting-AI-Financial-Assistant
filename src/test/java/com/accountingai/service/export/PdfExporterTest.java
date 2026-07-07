package com.accountingai.service.export;

import com.accountingai.TestFixtures;
import com.accountingai.model.ExportData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PdfExporter}.
 *
 * <p>Exports the sample fixture to PDF bytes, then re-parses those bytes with
 * PDFBox 3.x ({@code Loader.loadPDF(byte[])}) and asserts the rendered text
 * contains the customer name and a transaction description, and that the produced
 * document has at least one page.</p>
 */
class PdfExporterTest {

    @Test
    void exportedPdfContainsExpectedText() throws ExportException, IOException {
        ExportData data = TestFixtures.sampleExportData();
        PdfExporter exporter = new PdfExporter();

        byte[] bytes = exporter.toBytes(data);
        assertNotNull(bytes);

        // PDFBox 3.x: load directly from a byte array.
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            assertTrue(doc.getNumberOfPages() >= 1, "Exported PDF should have at least one page.");

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            assertTrue(text.contains("John Smith"), "PDF text should contain the customer name.");
            assertTrue(text.contains("Payroll Deposit"), "PDF text should contain a transaction description.");
        }
    }
}
