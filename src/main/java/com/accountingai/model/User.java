package com.accountingai.model;

/**
 * Represents an application user (login credential).
 * <p>
 * Maps to the {@code users} table: (user_id, full_name, username, email, password_hash).
 * The password is never stored in plain text; only its SHA-256 hash is kept.
 */
public class User {

    /** Primary key (user_id). 0 means "not yet persisted". */
    private int id;

    /** The unique login username. */
    private String username;

    /** The lowercase-hex SHA-256 hash of the user's password. */
    private String passwordHash;

    /** The user's full name stored in the users.full_name column. */
    private String fullName;

    /** The user's unique email address stored in the users.email column */
    private String email;

    /** No-arg constructor required for framework/database mapping. */
    public User() {
    }

    /**
     * All-args constructor.
     *
     * @param id           the user id (0 if not persisted)
     * @param fullName     the user's full name
     * @param username     the login username
     * @param email        the user's email address
     * @param passwordHash the SHA-256 password hash
     */
    public User(int id, String fullName, String username, String email, String passwordHash) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public User(String firstName, String lastName, String username, String email, String passwordHash) {
        this.fullName = firstName + " " + lastName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public User(String fullName, String username, String email, String passwordHash) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
