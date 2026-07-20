package com.accountingai.db.dao;

import com.accountingai.db.DatabaseManager;
import com.accountingai.db.DbTestSupport;
import com.accountingai.model.DocumentMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for {@link DocumentDao}: insert + find + search + status update.
 *
 * <p>Named "DaoSmokeTest" to keep the document-metadata DAO coverage together;
 * it verifies the CRUD paths that the upload/import flow relies on.</p>
 */
class DaoSmokeTest {

    @TempDir
    Path tmp;

    private DocumentMetadata newDoc(String fileName) {
        DocumentMetadata d = new DocumentMetadata();
        d.setFileName(fileName);
        d.setFilePath("/some/path/" + fileName);
        d.setFileSizeBytes(2048L);
        d.setPageCount(1);
        d.setTitle("Sample Title");
        d.setAuthor("Sample Author");
        d.setUploadedAt(LocalDateTime.now());
        d.setStatementId(null);
        d.setStatus("IMPORTED");
        d.setAiDocumentType("BANK_STATEMENT");
        d.setAiExtractedMetadata("customer_name=John Smith\ntransaction_count=7");
        d.setAiSummary("Bank statement contains 7 transactions.");
        d.setAiAnalyzedAt(LocalDateTime.now());
        return d;
    }

    @Test
    void insertAndFindAllRoundTrip() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("doc.db"));
        DocumentDao dao = new DocumentDao(db);

        int id = dao.insert(newDoc("statement_june.pdf"));
        assertTrue(id > 0, "Insert should return a generated document id.");

        List<DocumentMetadata> all = dao.findAll();
        assertFalse(all.isEmpty());
        assertTrue(all.stream().anyMatch(d -> "statement_june.pdf".equals(d.getFileName())));
    }

    @Test
    void findByIdReturnsInsertedDocument() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("doc2.db"));
        DocumentDao dao = new DocumentDao(db);

        int id = dao.insert(newDoc("report.pdf"));
        Optional<DocumentMetadata> found = dao.findById(id);
        assertTrue(found.isPresent());
        assertEquals("report.pdf", found.get().getFileName());
        assertEquals(1, found.get().getPageCount());
        assertEquals("BANK_STATEMENT", found.get().getAiDocumentType());
        assertTrue(found.get().getAiSummary().contains("7 transactions"));
        assertTrue(found.get().getAiExtractedMetadata().contains("customer_name=John Smith"));
        assertTrue(found.get().getAiAnalyzedAt() != null);
    }

    @Test
    void searchByFileNameMatches() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("doc3.db"));
        DocumentDao dao = new DocumentDao(db);

        dao.insert(newDoc("invoice_2026.pdf"));
        dao.insert(newDoc("statement_2026.pdf"));

        List<DocumentMetadata> results = dao.searchByFileName("invoice");
        assertTrue(results.stream().anyMatch(d -> "invoice_2026.pdf".equals(d.getFileName())));
        assertFalse(results.stream().anyMatch(d -> "statement_2026.pdf".equals(d.getFileName())),
                "Search should not return non-matching files.");
    }

    @Test
    void updateStatusChangesStatus() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("doc4.db"));
        DocumentDao dao = new DocumentDao(db);

        int id = dao.insert(newDoc("to_process.pdf"));
        dao.updateStatus(id, "PROCESSED");

        Optional<DocumentMetadata> found = dao.findById(id);
        assertTrue(found.isPresent());
        assertEquals("PROCESSED", found.get().getStatus());
    }
}
