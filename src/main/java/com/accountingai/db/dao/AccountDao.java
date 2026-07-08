package com.accountingai.db.dao;

import com.accountingai.db.DatabaseManager;
import com.accountingai.model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for {@link Account} rows in the {@code accounts} table.
 *
 * <p>Every method opens its own short-lived JDBC connection through the shared
 * {@link DatabaseManager} and closes it via try-with-resources.</p>
 */
public class AccountDao {

    private final DatabaseManager db;

    /**
     * @param db the database manager used to obtain connections
     */
    public AccountDao(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Inserts an account and returns its generated primary key.
     *
     * <p>If the supplied account already carries a positive id, that id is
     * inserted explicitly (useful for seeding fixed ids); otherwise SQLite
     * auto-assigns one and the generated key is returned.</p>
     *
     * @param a the account to persist (must not be {@code null})
     * @return the account id (generated or supplied)
     */
    public int insert(Account a) {
        // When an explicit id is provided we include the account_id column.
        boolean withExplicitId = a.getId() > 0;
        String sql = withExplicitId
                ? "INSERT INTO accounts(account_id, customer_name, account_number) VALUES (?, ?, ?)"
                : "INSERT INTO accounts(customer_name, account_number) VALUES (?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int index = 1;
            if (withExplicitId) {
                ps.setInt(index++, a.getId());
            }
            ps.setString(index++, a.getCustomerName());
            ps.setString(index, a.getAccountNumber());
            ps.executeUpdate();

            if (withExplicitId) {
                return a.getId();
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert account", e);
        }
    }

    /**
     * Finds an account by its primary key.
     *
     * @param id the account id
     * @return an {@link Optional} account
     */
    public Optional<Account> findById(int id) {
        String sql = "SELECT account_id, customer_name, account_number FROM accounts WHERE account_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find account by id", e);
        }
    }

    /**
     * Finds an account by its unique account number.
     *
     * @param num the account number
     * @return an {@link Optional} account
     */
    public Optional<Account> findByAccountNumber(String num) {
        String sql = "SELECT account_id, customer_name, account_number FROM accounts WHERE account_number = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, num);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find account by number", e);
        }
    }

    /**
     * @return all accounts ordered by id
     */
    public List<Account> findAll() {
        String sql = "SELECT account_id, customer_name, account_number FROM accounts ORDER BY account_id";
        List<Account> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list accounts", e);
        }
    }

    /**
     * Returns the id of an existing account matching the account number, or
     * inserts the account and returns the new id.
     *
     * @param a the account to look up or create
     * @return the id of the existing or newly created account
     */
    public int findOrCreate(Account a) {
        Optional<Account> existing = findByAccountNumber(a.getAccountNumber());
        return existing.map(Account::getId).orElseGet(() -> insert(a));
    }

    /**
     * Maps the current {@link ResultSet} row to an {@link Account}.
     */
    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
                rs.getInt("account_id"),
                rs.getString("customer_name"),
                rs.getString("account_number"));
    }
}
