package com.accountingai.db;

import com.accountingai.db.dao.UserDao;
import com.accountingai.db.dao.DocumentDao;
import com.accountingai.model.DocumentMetadata;
import com.accountingai.model.User;
import com.accountingai.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void initializeMigratesLegacyUsersTable() throws Exception {
        DatabaseManager db = new DatabaseManager(tempDir.resolve("legacy-users.db"));
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users ("
                    + "user_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT NOT NULL UNIQUE,"
                    + "password_hash TEXT NOT NULL)");
            stmt.execute("INSERT INTO users (username, password_hash) VALUES "
                    + "('legacy', '" + PasswordUtil.sha256("1234") + "')");
        }

        db.initialize();

        UserDao userDao = new UserDao(db);
        User legacy = userDao.findByUsername("legacy").orElseThrow();
        assertEquals("legacy", legacy.getFullName());
        assertEquals("legacy@accounting-ai.local", legacy.getEmail());
        assertTrue(userDao.verify("legacy", PasswordUtil.sha256("1234")));
        assertTrue(tableExists(db, "accounts"));
        assertTrue(tableExists(db, "statements"));
        assertTrue(tableExists(db, "transactions"));
        assertTrue(tableExists(db, "document_metadata"));
    }

    @Test
    void initializeMigratesLegacyDocumentMetadataTableForAiFields() throws Exception {
        DatabaseManager db = new DatabaseManager(tempDir.resolve("legacy-documents.db"));
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE document_metadata ("
                    + "document_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "file_name TEXT NOT NULL,"
                    + "file_path TEXT,"
                    + "file_size_bytes INTEGER,"
                    + "page_count INTEGER,"
                    + "title TEXT,"
                    + "author TEXT,"
                    + "uploaded_at TEXT,"
                    + "statement_id INTEGER,"
                    + "status TEXT)");
        }

        db.initialize();

        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setFileName("legacy.pdf");
        metadata.setFilePath("/tmp/legacy.pdf");
        metadata.setFileSizeBytes(100);
        metadata.setPageCount(1);
        metadata.setUploadedAt(LocalDateTime.now());
        metadata.setStatus("IMPORTED");
        metadata.setAiDocumentType("BANK_STATEMENT");
        metadata.setAiExtractedMetadata("customer_name=Legacy User");
        metadata.setAiSummary("Legacy statement summary.");
        metadata.setAiAnalyzedAt(LocalDateTime.now());
        metadata.setAiProvider("LOCAL");
        metadata.setAiModel("HEURISTIC_V2");

        DocumentDao documentDao = new DocumentDao(db);
        int id = documentDao.insert(metadata);
        DocumentMetadata stored = documentDao.findById(id).orElseThrow();
        assertEquals("BANK_STATEMENT", stored.getAiDocumentType());
        assertEquals("LOCAL", stored.getAiProvider());
        assertEquals("HEURISTIC_V2", stored.getAiModel());
        assertTrue(stored.getAiSummary().contains("Legacy"));
    }

    private boolean tableExists(DatabaseManager db, String tableName) throws Exception {
        String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = '" + tableName + "'";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
