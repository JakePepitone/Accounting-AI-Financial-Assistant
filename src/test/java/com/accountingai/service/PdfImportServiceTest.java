package com.accountingai.service;

import com.accountingai.TestPdfs;
import com.accountingai.model.ImportResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PdfImportService}.
 *
 * <p>Wires the real extractor/parser/metadata collaborators with an isolated
 * temp import store. Verifies a happy-path import (success, non-null statement,
 * copied file) plus the failure paths for a non-PDF and an empty file.</p>
 */
class PdfImportServiceTest {

    @TempDir
    Path tmp;

    /** Builds an import service whose store is inside the test temp dir. */
    private PdfImportService newService() {
        Path store = tmp.resolve("import-store");
        return new PdfImportService(
                new PdfTextExtractor(),
                new StatementParser(),
                new MetadataExtractor(),
                store);
    }

    @Test
    void importValidPdfSucceedsAndCopiesFile() throws IOException {
        Path pdf = tmp.resolve("valid_statement.pdf");
        TestPdfs.writeSampleStatement(pdf);

        PdfImportService service = newService();
        ImportResult result = service.importPdf(pdf);

        assertTrue(result.isSuccess(), "Importing a valid PDF should succeed.");
        assertNotNull(result.getStatement(), "A parsed statement should be attached.");
        assertNotNull(result.getAccount(), "A parsed account should be attached.");
        assertNotNull(result.getMetadata(), "Document metadata should be attached.");
        assertNotNull(result.getStoredFilePath(), "The stored file path should be set.");
        assertTrue("John Smith".equals(result.getAccount().getCustomerName()));
        assertNotNull(result.getMetadata().getAiSummary(), "AI summary should be attached.");
        assertTrue("LOCAL".equals(result.getMetadata().getAiProvider()));
        assertTrue("HEURISTIC_V2".equals(result.getMetadata().getAiModel()));
        assertTrue(result.getMetadata().getAiSummary().contains("7 transactions"));
        assertTrue(result.getMetadata().getAiExtractedMetadata().contains("customer_name=John Smith"));

        // The file should have been copied into the import store.
        Path stored = Path.of(result.getStoredFilePath());
        assertTrue(Files.exists(stored), "The imported PDF should have been copied to the store.");
    }

    @Test
    void importParsesExpectedStatementData() throws IOException {
        Path pdf = tmp.resolve("parse_me.pdf");
        TestPdfs.writeSampleStatement(pdf);

        PdfImportService service = newService();
        ImportResult result = service.importPdf(pdf);

        assertTrue(result.isSuccess());
        assertEquals7Transactions(result);
    }

    private void assertEquals7Transactions(ImportResult result) {
        assertNotNull(result.getStatement().getTransactions());
        assertTrue(result.getStatement().getTransactions().size() == 7,
                "Parsed statement should contain the 7 sample transactions.");
    }

    @Test
    void importNonPdfFails() throws IOException {
        Path txt = tmp.resolve("bad.txt");
        Files.writeString(txt, "not a pdf");

        PdfImportService service = newService();
        ImportResult result = service.importPdf(txt);

        assertFalse(result.isSuccess(), "A non-PDF file must fail validation.");
        assertNotNull(result.getMessage(), "A failure should carry a message.");
    }

    @Test
    void importEmptyFileFails() throws IOException {
        Path empty = tmp.resolve("empty.pdf");
        Files.createFile(empty); // zero bytes

        PdfImportService service = newService();
        ImportResult result = service.importPdf(empty);

        assertFalse(result.isSuccess(), "An empty file must fail validation.");
    }
}
