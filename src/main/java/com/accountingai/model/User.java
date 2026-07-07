package com.accountingai.model;

/**
 * Represents an application user (login credential).
 * <p>
 * Maps to the {@code users} table: (user_id, username, password_hash).
 * The password is never stored in plain text; only its SHA-256 hash is kept.
 */
public class User {

    /** Primary key (user_id). 0 means "not yet persisted". */
    private int id;

    /** The unique login username. */
    private String username;

    /** The lowercase-hex SHA-256 hash of the user's password. */
    private String passwordHash;

    /** No-arg constructor required for framework/database mapping. */
    public User() {
    }

    /**
     * All-args constructor.
     *
     * @param id           the user id (0 if not persisted)
     * @param username     the login username
     * @param passwordHash the SHA-256 password hash
     */
    public User(int id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String toString() {
        // Do NOT print the password hash in full for basic hygiene.
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                '}';
    }
}
