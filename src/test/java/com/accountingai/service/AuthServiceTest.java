package com.accountingai.service;

import com.accountingai.db.DatabaseManager;
import com.accountingai.db.DbTestSupport;
import com.accountingai.db.dao.UserDao;
import com.accountingai.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void verifiesSeededAdminLoginThroughBackendService() {
        DatabaseManager db = DbTestSupport.freshDb(tempDir.resolve("auth.db"));
        AuthService service = new AuthService(new UserDao(db));

        assertTrue(service.verifyLogin("admin", "1234"));
        assertFalse(service.verifyLogin("admin", "wrong-password"));
        assertFalse(service.verifyLogin("", "1234"));
    }

    @Test
    void registersUserWithValidationAndNormalizedEmail() {
        DatabaseManager db = DbTestSupport.freshDb(tempDir.resolve("register.db"));
        UserDao userDao = new UserDao(db);
        AuthService service = new AuthService(userDao);

        AuthService.RegistrationResult created = service.register(
                "Jane Doe", "jane", "JANE@Example.COM", "pass123", "pass123");

        assertTrue(created.success());
        assertTrue(created.userId() > 0);
        User stored = userDao.findByUsername("jane").orElseThrow();
        assertEquals("jane@example.com", stored.getEmail());
        assertTrue(service.verifyLogin("jane", "pass123"));
    }

    @Test
    void rejectsInvalidOrDuplicateRegistration() {
        DatabaseManager db = DbTestSupport.freshDb(tempDir.resolve("duplicates.db"));
        AuthService service = new AuthService(new UserDao(db));

        assertFalse(service.register("", "jane", "jane@example.com", "pass", "pass").success());
        assertFalse(service.register("Jane Doe", "jane", "not-an-email", "pass", "pass").success());
        assertFalse(service.register("Jane Doe", "jane", "jane@example.com", "pass", "mismatch").success());

        assertTrue(service.register("Jane Doe", "jane", "jane@example.com", "pass", "pass").success());
        assertFalse(service.register("Other Person", "jane", "other@example.com", "pass", "pass").success());
        assertFalse(service.register("Other Person", "other", "jane@example.com", "pass", "pass").success());
    }
}
