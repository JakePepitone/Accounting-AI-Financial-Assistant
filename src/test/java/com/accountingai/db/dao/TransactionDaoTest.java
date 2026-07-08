package com.accountingai.db.dao;

import com.accountingai.db.DatabaseManager;
import com.accountingai.db.DbTestSupport;
import com.accountingai.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TransactionDao} against a freshly-seeded temp SQLite database.
 *
 * <p>Confirms the 7 seeded transactions round-trip, that description search works
 * case-insensitively, and that {@code insertBatch} adds multiple rows in one go.</p>
 */
class TransactionDaoTest {

    @TempDir
    Path tmp;

    @Test
    void seededStatementHasSevenTransactions() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("txn.db"));
        TransactionDao dao = new TransactionDao(db);

        List<Transaction> txns = dao.findByStatementId(101);
        assertEquals(7, txns.size(), "Statement 101 should have 7 seeded transactions.");

        // Payroll Deposit = +2500.00
        Transaction payroll = txns.stream()
                .filter(t -> "Payroll Deposit".equals(t.getDescription()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Payroll Deposit not found"));
        assertEquals(0, new BigDecimal("2500.00").compareTo(payroll.getAmount()));

        // Amazon Purchase = -125.99
        Transaction amazon = txns.stream()
                .filter(t -> "Amazon Purchase".equals(t.getDescription()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Amazon Purchase not found"));
        assertEquals(0, new BigDecimal("-125.99").compareTo(amazon.getAmount()));
    }

    @Test
    void searchByDescriptionFindsAmazon() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("txn2.db"));
        TransactionDao dao = new TransactionDao(db);

        List<Transaction> results = dao.searchByDescription("Amazon");
        assertEquals(1, results.size(), "Exactly one transaction matches 'Amazon'.");
        assertEquals("Amazon Purchase", results.get(0).getDescription());
    }

    @Test
    void searchByDescriptionIsCaseInsensitive() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("txn3.db"));
        TransactionDao dao = new TransactionDao(db);

        // Lower-case query should still match "Amazon Purchase".
        List<Transaction> results = dao.searchByDescription("amazon");
        assertTrue(results.stream().anyMatch(t -> "Amazon Purchase".equals(t.getDescription())),
                "Search should be case-insensitive.");
    }

    @Test
    void insertBatchAddsAllRows() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("txn4.db"));
        TransactionDao dao = new TransactionDao(db);

        List<Transaction> newTxns = new ArrayList<>();
        newTxns.add(new Transaction(0, 101, LocalDate.parse("2026-06-28"), "Coffee Shop", new BigDecimal("-4.50")));
        newTxns.add(new Transaction(0, 101, LocalDate.parse("2026-06-29"), "Bookstore", new BigDecimal("-19.99")));
        newTxns.add(new Transaction(0, 101, LocalDate.parse("2026-06-30"), "Refund", new BigDecimal("30.00")));

        dao.insertBatch(newTxns);

        // 7 seeded + 3 new = 10.
        List<Transaction> all = dao.findByStatementId(101);
        assertEquals(10, all.size(), "insertBatch should add all three rows.");
        assertTrue(all.stream().anyMatch(t -> "Coffee Shop".equals(t.getDescription())));
        assertTrue(all.stream().anyMatch(t -> "Bookstore".equals(t.getDescription())));
        assertTrue(all.stream().anyMatch(t -> "Refund".equals(t.getDescription())));
    }

    @Test
    void singleInsertIsRetrievable() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("txn5.db"));
        TransactionDao dao = new TransactionDao(db);

        Transaction t = new Transaction(0, 101, LocalDate.parse("2026-06-27"), "Pharmacy", new BigDecimal("-22.10"));
        int id = dao.insert(t);
        assertTrue(id > 0, "Insert should return a generated id.");

        assertTrue(dao.findByStatementId(101).stream()
                .anyMatch(x -> "Pharmacy".equals(x.getDescription())));
    }
}
