package com.accountingai.service;

import com.accountingai.db.dao.UserDao;
import com.accountingai.model.User;
import com.accountingai.util.PasswordUtil;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Backend authentication and registration workflow.
 *
 * <p>Controllers pass raw form values here; this service owns normalization,
 * validation, duplicate checks, password hashing, and persistence.</p>
 */
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE);

    private final UserDao userDao;

    /**
     * @param userDao user persistence
     */
    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Verifies a username/password pair.
     *
     * @param username raw username from the login form
     * @param password raw password from the login form
     * @return true only when the credentials match a stored user
     */
    public boolean verifyLogin(String username, String password) {
        String normalizedUsername = normalize(username);
        if (normalizedUsername == null || password == null || password.isBlank()) {
            return false;
        }
        return userDao.verify(normalizedUsername, PasswordUtil.sha256(password));
    }

    /**
     * Validates and creates a new user account.
     *
     * @return a success/failure result with a display-safe message
     */
    public RegistrationResult register(String fullName, String username, String email,
                                       String password, String confirmPassword) {
        String normalizedName = normalize(fullName);
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalizeEmail(email);

        if (normalizedName == null || normalizedUsername == null || normalizedEmail == null
                || password == null || password.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            return RegistrationResult.failure("Please fill in all fields.");
        }

        if (!isValidEmail(normalizedEmail)) {
            return RegistrationResult.failure("Please enter a valid email address.");
        }

        if (!password.equals(confirmPassword)) {
            return RegistrationResult.failure("Passwords do not match.");
        }

        if (userDao.usernameExists(normalizedUsername)) {
            return RegistrationResult.failure("That username is already taken.");
        }

        if (userDao.emailExists(normalizedEmail)) {
            return RegistrationResult.failure("That email is already registered.");
        }

        try {
            User user = new User(
                    normalizedName,
                    normalizedUsername,
                    normalizedEmail,
                    PasswordUtil.sha256(password));
            int userId = userDao.insert(user);
            return userId > 0
                    ? RegistrationResult.success(userId)
                    : RegistrationResult.failure("Unable to create account.");
        } catch (RuntimeException e) {
            return RegistrationResult.failure(
                    "Unable to create account. The username or email may already be in use.");
        }
    }

    private static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeEmail(String email) {
        String normalized = normalize(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Result returned from registration attempts.
     *
     * @param success true when a user was created
     * @param message display-safe status message
     * @param userId  created user id, or -1 on failure
     */
    public record RegistrationResult(boolean success, String message, int userId) {

        public static RegistrationResult success(int userId) {
            return new RegistrationResult(true, "Account created. You can return to login.", userId);
        }

        public static RegistrationResult failure(String message) {
            return new RegistrationResult(false, message, -1);
        }
    }
}
