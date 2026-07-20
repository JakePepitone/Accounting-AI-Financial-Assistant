package com.accountingai.service;

import com.accountingai.db.DatabaseManager;
import com.accountingai.db.DbTestSupport;
import com.accountingai.db.dao.DocumentDao;
import com.accountingai.db.dao.TransactionDao;
import com.accountingai.model.DocumentMetadata;
import com.accountingai.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SearchService}.
 *
 * <p>Covers two responsibilities: in-memory line search over extracted document
 * text (case-insensitive, blank query returns nothing) and transaction search
 * backed by a seeded temp-database {@link TransactionDao}.</p>
 */
class SearchServiceTest {

    @TempDir
    Path tmp;

    /** Sample multi-line text to search within. */
    private static final String TEXT =
            "Customer: John Smith\n"
            + "Account Number: 123456789\n"
            + "Payroll Deposit 2500.00\n"
            + "Amazon Purchase -125.99\n"
            + "Grocery Store -152.00\n";

    private SearchService newServiceWithSeededDao() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("search.db"));
        TransactionDao dao = new TransactionDao(db);
        return new SearchService(dao);
    }

    private SearchService newServiceWithDocumentDao() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("search_docs.db"));
        DocumentDao documentDao = new DocumentDao(db);
        DocumentMetadata d = new DocumentMetadata();
        d.setFileName("statement_ai.pdf");
        d.setFilePath("/tmp/statement_ai.pdf");
        d.setFileSizeBytes(1024);
        d.setPageCount(1);
        d.setUploadedAt(java.time.LocalDateTime.now());
        d.setStatus("IMPORTED");
        d.setAiDocumentType("BANK_STATEMENT");
        d.setAiExtractedMetadata("customer_name=John Smith\ntransaction_count=7");
        d.setAiSummary("Bank statement contains payroll and 7 transactions.");
        documentDao.insert(d);
        return new SearchService(new TransactionDao(db), documentDao);
    }

    @Test
    void searchInTextFindsCaseInsensitiveMatches() {
        SearchService service = newServiceWithSeededDao();

        // "smith" (lower-case) should match the "John Smith" line.
        List<SearchResult> results = service.searchInText(TEXT, "smith");
        assertFalse(results.isEmpty(), "Case-insensitive search should find 'smith'.");
        assertTrue(results.stream().anyMatch(r -> r.context().toLowerCase().contains("smith")));
    }

    @Test
    void searchInTextReturnsEmptyForBlankQuery() {
        SearchService service = newServiceWithSeededDao();

        assertTrue(service.searchInText(TEXT, "").isEmpty(), "Blank query returns no results.");
        assertTrue(service.searchInText(TEXT, "   ").isEmpty(), "Whitespace query returns no results.");
        assertTrue(service.searchInText(TEXT, null).isEmpty(), "Null query returns no results.");
    }

    @Test
    void searchInTextHandlesNullTextGracefully() {
        SearchService service = newServiceWithSeededDao();
        assertTrue(service.searchInText(null, "smith").isEmpty(),
                "Null document text should yield no results, not an exception.");
    }

    @Test
    void searchTransactionsFindsAmazonRowFromDatabase() {
        SearchService service = newServiceWithSeededDao();

        List<SearchResult> results = service.searchTransactions("Amazon");
        assertFalse(results.isEmpty(), "Seeded Amazon transaction should be found.");
        assertTrue(results.stream().anyMatch(r -> r.context().contains("Amazon")
                        || r.label().contains("Amazon")),
                "A search result should reference the Amazon transaction.");
    }

    @Test
    void searchDocumentsFindsAiSummaryRows() {
        SearchService service = newServiceWithDocumentDao();

        List<SearchResult> results = service.searchDocuments("payroll");

        assertFalse(results.isEmpty(), "AI summary search should find the inserted document.");
        assertTrue(results.stream().anyMatch(r -> "DOCUMENT".equals(r.source())
                && r.label().contains("statement_ai")));
    }
}
