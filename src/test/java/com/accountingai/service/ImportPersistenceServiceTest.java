package com.accountingai.service;

import com.accountingai.TestFixtures;
import com.accountingai.db.DatabaseManager;
import com.accountingai.db.DbTestSupport;
import com.accountingai.db.dao.AccountDao;
import com.accountingai.db.dao.DocumentDao;
import com.accountingai.db.dao.StatementDao;
import com.accountingai.db.dao.TransactionDao;
import com.accountingai.model.Account;
import com.accountingai.model.DocumentMetadata;
import com.accountingai.model.ImportResult;
import com.accountingai.model.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for persisting parsed import results through the backend service layer.
 */
class ImportPersistenceServiceTest {

    @TempDir
    Path tmp;

    @Test
    void persistsAccountStatementTransactionsAndDocumentMetadata() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("persist.db"));
        AccountDao accountDao = new AccountDao(db);
        StatementDao statementDao = new StatementDao(db);
        TransactionDao transactionDao = new TransactionDao(db);
        DocumentDao documentDao = new DocumentDao(db);
        ImportPersistenceService service =
                new ImportPersistenceService(accountDao, statementDao, transactionDao, documentDao);

        Account account = new Account(0, "Jane Doe", "ACCT-555");
        Statement statement = TestFixtures.sampleStatement();
        statement.setId(0);
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setFileName("jane_statement.pdf");
        metadata.setFilePath("/tmp/jane_statement.pdf");
        metadata.setFileSizeBytes(4096);
        metadata.setPageCount(2);
        metadata.setUploadedAt(LocalDateTime.now());
        metadata.setStatus("IMPORTED");
        metadata.setAiDocumentType("BANK_STATEMENT");
        metadata.setAiSummary("Bank statement contains 7 transactions.");
        metadata.setAiProvider("LOCAL");
        metadata.setAiModel("HEURISTIC_V2");

        int documentId = service.persist(ImportResult.ok("/tmp/jane_statement.pdf", statement, account, metadata));

        assertTrue(documentId > 0);
        DocumentMetadata stored = documentDao.findById(documentId).orElseThrow();
        assertTrue(stored.getStatementId() != null && stored.getStatementId() > 0);
        assertEquals("BANK_STATEMENT", stored.getAiDocumentType());
        assertEquals("LOCAL", stored.getAiProvider());
        assertEquals("HEURISTIC_V2", stored.getAiModel());
        assertEquals(7, transactionDao.findByStatementId(stored.getStatementId()).size());
        assertTrue(accountDao.findByAccountNumber("ACCT-555").isPresent());
        assertTrue(statementDao.findById(stored.getStatementId()).isPresent());
    }

    @Test
    void rejectsFailedImports() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("reject.db"));
        ImportPersistenceService service = new ImportPersistenceService(
                new AccountDao(db), new StatementDao(db), new TransactionDao(db), new DocumentDao(db));

        assertThrows(IllegalArgumentException.class, () -> service.persist(ImportResult.fail("bad")));
    }
}
