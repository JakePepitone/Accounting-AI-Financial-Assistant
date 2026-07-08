package com.accountingai.db.dao;

import com.accountingai.db.DatabaseManager;
import com.accountingai.db.DbTestSupport;
import com.accountingai.model.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AccountDao} against a freshly-seeded temp SQLite database.
 *
 * <p>Verifies that the seeded John Smith account round-trips through the various
 * finder methods and that insert / findOrCreate behave as specified.</p>
 */
class AccountDaoTest {

    @TempDir
    Path tmp;

    @Test
    void seededAccountRoundTripsByAccountNumber() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("accounts.db"));
        AccountDao dao = new AccountDao(db);

        Optional<Account> found = dao.findByAccountNumber("123456789");
        assertTrue(found.isPresent(), "Seeded account should be found by account number.");
        assertEquals("John Smith", found.get().getCustomerName());
        assertEquals(1, found.get().getId());
    }

    @Test
    void seededAccountRoundTripsById() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("accounts2.db"));
        AccountDao dao = new AccountDao(db);

        Optional<Account> found = dao.findById(1);
        assertTrue(found.isPresent(), "Seeded account should be found by id 1.");
        assertEquals("123456789", found.get().getAccountNumber());
    }

    @Test
    void findAllContainsSeededAccount() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("accounts3.db"));
        AccountDao dao = new AccountDao(db);

        List<Account> all = dao.findAll();
        assertFalse(all.isEmpty(), "There should be at least the seeded account.");
        assertTrue(all.stream().anyMatch(a -> "123456789".equals(a.getAccountNumber())));
    }

    @Test
    void insertAssignsGeneratedIdAndCanBeFound() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("accounts4.db"));
        AccountDao dao = new AccountDao(db);

        Account jane = new Account(0, "Jane Doe", "987654321");
        int newId = dao.insert(jane);
        assertTrue(newId > 0, "Insert should return a generated primary key.");

        Optional<Account> found = dao.findByAccountNumber("987654321");
        assertTrue(found.isPresent());
        assertEquals("Jane Doe", found.get().getCustomerName());
        assertEquals(newId, found.get().getId());
    }

    @Test
    void findOrCreateReturnsExistingIdWhenAccountNumberExists() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("accounts5.db"));
        AccountDao dao = new AccountDao(db);

        // The seeded account already uses 123456789 -> findOrCreate must reuse id 1.
        Account dup = new Account(0, "John Smith", "123456789");
        int id = dao.findOrCreate(dup);
        assertEquals(1, id, "findOrCreate should return the existing account's id.");

        // Only one account with that number should exist.
        long count = dao.findAll().stream()
                .filter(a -> "123456789".equals(a.getAccountNumber()))
                .count();
        assertEquals(1, count, "findOrCreate must not create a duplicate.");
    }

    @Test
    void findOrCreateInsertsWhenAccountNumberIsNew() {
        DatabaseManager db = DbTestSupport.freshDb(tmp.resolve("accounts6.db"));
        AccountDao dao = new AccountDao(db);

        Account fresh = new Account(0, "New Customer", "555000111");
        int id = dao.findOrCreate(fresh);
        assertTrue(id > 0, "A newly created account should get a positive id.");

        assertTrue(dao.findByAccountNumber("555000111").isPresent());
    }
}
