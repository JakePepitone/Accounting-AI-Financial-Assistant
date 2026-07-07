package com.accountingai.db.dao;

import com.accountingai.db.DatabaseManager;
import com.accountingai.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for {@link Transaction} rows in the {@code transactions}
 * table.
 *
 * <p>Dates are stored as ISO-8601 strings and amounts as REAL (doubles).
 * Negative amounts represent withdrawals; positive amounts represent deposits.</p>
 */
public class TransactionDao {

    private final DatabaseManager db;

    /**
     * @param db the database manager used to obtain connections
     */
    public TransactionDao(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Inserts a single transaction and returns its generated primary key.
     *
     * @param t the transaction to persist
     * @return the generated transaction id (or -1 if none was returned)
     */
    public int insert(Transaction t) {
        String sql = "INSERT INTO transactions(statement_id, transaction_date, description, amount) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindTransaction(ps, t);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert transaction", e);
        }
    }

    /**
     * Inserts many transactions efficiently using a single connection and a
     * batched, transactional insert.
     *
     * @param txns the transactions to persist (a {@code null} or empty list is a no-op)
     */
    public void insertBatch(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO transactions(statement_id, transaction_date, description, amount) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection()) {
            // Wrap the batch in an explicit transaction for speed and atomicity.
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Transaction t : txns) {
                    if (t == null) {
                        continue;
                    }
                    bindTransaction(ps, t);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert transaction batch", e);
        }
    }

    /**
     * Finds all transactions for a statement, ordered by id.
     *
     * @param statementId the owning statement id
     * @return the statement's transactions
     */
    public List<Transaction> findByStatementId(int statementId) {
        String sql = baseSelect() + " WHERE statement_id = ? ORDER BY transaction_id";
        List<Transaction> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, statementId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transactions by statement", e);
        }
    }

    /**
     * Case-insensitive substring search over transaction descriptions.
     *
     * @param query the text to search for
     * @return matching transactions (empty for a {@code null}/blank query)
     */
    public List<Transaction> searchByDescription(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>();
        }
        // LIKE in SQLite is case-insensitive for ASCII by default. The '%'||?||'%'
        // pattern lets us bind the raw query without manual escaping of wildcards.
        String sql = baseSelect()
                + " WHERE description LIKE '%' || ? || '%' ORDER BY transaction_id";
        List<Transaction> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, query);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search transactions", e);
        }
    }

    // ------------------------------------------------------------------
    // Row mapping & conversion helpers
    // ------------------------------------------------------------------

    /** Common SELECT clause shared by the finder methods. */
    private String baseSelect() {
        return "SELECT transaction_id, statement_id, transaction_date, description, amount "
                + "FROM transactions";
    }

    /** Binds a transaction's fields onto the standard 4-parameter insert. */
    private void bindTransaction(PreparedStatement ps, Transaction t) throws SQLException {
        ps.setInt(1, t.getStatementId());
        ps.setString(2, dateToString(t.getDate()));
        ps.setString(3, t.getDescription());
        ps.setDouble(4, moneyToDouble(t.getAmount()));
    }

    /** Maps the current row to a {@link Transaction}. */
    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("transaction_id"));
        t.setStatementId(rs.getInt("statement_id"));
        t.setDate(stringToDate(rs.getString("transaction_date")));
        t.setDescription(rs.getString("description"));
        t.setAmount(doubleToMoney(rs.getDouble("amount")));
        return t;
    }

    /** ISO string for a date, or {@code null}. */
    private static String dateToString(LocalDate date) {
        return date == null ? null : date.toString();
    }

    /** Parses an ISO date string, tolerating {@code null}/blank. */
    private static LocalDate stringToDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    /** Money to double, treating {@code null} as 0. */
    private static double moneyToDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    /** REAL back to BigDecimal via string to avoid binary-float drift. */
    private static BigDecimal doubleToMoney(double value) {
        return new BigDecimal(Double.toString(value));
    }
}
