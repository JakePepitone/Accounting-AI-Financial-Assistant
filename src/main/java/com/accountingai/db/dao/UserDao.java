package com.accountingai.db.dao;

import com.accountingai.db.DatabaseManager;
import com.accountingai.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Data-access object for {@link User} rows in the {@code users} table.
 *
 * <p>Only the SHA-256 password <em>hash</em> is ever stored or compared — plain
 * passwords never touch this layer.</p>
 */
public class UserDao {

    private final DatabaseManager db;

    /**
     * @param db the database manager used to obtain connections
     */
    public UserDao(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Finds a user by their unique username.
     *
     * @param username the username to look up
     * @return an {@link Optional} user
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT user_id, full_name, username, email, password_hash FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by username", e);
        }
    }

    /**
     * Finds a user by their unique email address.
     *
     * @param email the email address to look up
     * @return an {@link Optional} user
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT user_id, full_name, username, email, password_hash "
                + "FROM users WHERE lower(email) = lower(?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email", e);
        }
    }

    /**
     * @return true when the username already belongs to a user
     */
    public boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    /**
     * @return true when the email already belongs to a user
     */
    public boolean emailExists(String email) {
        return findByEmail(email).isPresent();
    }

    /**
     * Verifies a login by checking that a user with the given username exists
     * and has exactly the supplied password hash.
     *
     * @param username     the username
     * @param passwordHash the expected SHA-256 hash (lowercase hex)
     * @return {@code true} if the username exists and the hash matches exactly
     */
    public boolean verify(String username, String passwordHash) {
        Optional<User> user = findByUsername(username);
        // Defensive null check: a stored hash should never be null, but guard anyway.
        return user.isPresent()
                && user.get().getPasswordHash() != null
                && user.get().getPasswordHash().equals(passwordHash);
    }

    /**
     * Inserts a user and returns their generated primary key.
     *
     * @param u the user to persist
     * @return the generated user id (or -1 if none was returned)
     */
    public int insert(User u) {
        String sql = "INSERT INTO users(full_name, username, email, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPasswordHash());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    u.setId(id);
                    return id;
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert user", e);
        }
    }

    /** Maps the current row to a {@link User}. */
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"));
    }
}
