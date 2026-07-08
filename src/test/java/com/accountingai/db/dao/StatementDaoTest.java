package com.accountingai.db.dao;

import com.accountingai.db.DatabaseManager;
import com.accountingai.db.DbTestSupport;
import com.accountingai.model.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link StatementDao} against a freshly-seeded temp SQLite database.
 *
 * <p>Confirms the seeded statement 101 round-trips and that a newly inserted
 * statement is retrievable. BigDecimal values are compared with {@code compareTo}
 * so scale differences (5000 vs 5000.00) do not cause false failures.</p>
 */
class StatementDaoTest {

    @TempDir
    Path tmp;

    @Test
    void seededStatementFoundByAccountId() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("stmt.db"));
        StatementDao dao = new StatementDao(db);

        List<Statement> byAccount = dao.findByAccountId(1);
        assertFalse(byAccount.isEmpty(), "Account 1 should have the seeded statement.");
        assertTrue(byAccount.stream().anyMatch(s -> s.getId() == 101));
    }

    @Test
    void seededStatementFoundByIdWithCorrectBalances() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("stmt2.db"));
        StatementDao dao = new StatementDao(db);

        Optional<Statement> found = dao.findById(101);
        assertTrue(found.isPresent(), "Seeded statement 101 should exist.");
        Statement s = found.get();

        assertEquals(1, s.getAccountId());
        assertEquals(LocalDate.parse("2026-06-01"), s.getPeriodStart());
        assertEquals(LocalDate.parse("2026-06-30"), s.getPeriodEnd());
        assertEquals(0, new BigDecimal("5000.00").compareTo(s.getBeginningBalance()));
        assertEquals(0, new BigDecimal("2500.00").compareTo(s.getTotalDeposits()));
        assertEquals(0, new BigDecimal("1273.56").compareTo(s.getTotalWithdrawals()));
        assertEquals(0, new BigDecimal("6226.44").compareTo(s.getEndingBalance()));
    }

    @Test
    void findAllContainsSeededStatement() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("stmt3.db"));
        StatementDao dao = new StatementDao(db);

        List<Statement> all = dao.findAll();
        assertTrue(all.stream().anyMatch(s -> s.getId() == 101));
    }

    @Test
    void insertNewStatementIsRetrievable() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("stmt4.db"));
        StatementDao dao = new StatementDao(db);

        Statement s = new Statement(
                0,
                1,
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-31"),
                new BigDecimal("6226.44"),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("6726.44"),
                new java.util.ArrayList<>());

        int newId = dao.insert(s);
        assertTrue(newId > 0, "Insert should return a generated id.");

        Optional<Statement> found = dao.findById(newId);
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("6726.44").compareTo(found.get().getEndingBalance()));
    }
}
