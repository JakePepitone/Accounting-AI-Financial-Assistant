package com.accountingai.db.dao;

import com.accountingai.db.DatabaseManager;
import com.accountingai.model.Statement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for {@link Statement} rows in the {@code statements} table.
 *
 * <p>Dates are stored as ISO-8601 strings and money values as REAL (doubles).
 * Transactions are intentionally NOT loaded here — callers use
 * {@link TransactionDao#findByStatementId(int)} when they need them.</p>
 */
public class StatementDao {

    private final DatabaseManager db;

    /**
     * @param db the database manager used to obtain connections
     */
    public StatementDao(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Inserts a statement and returns its generated primary key.
     *
     * @param s the statement to persist
     * @return the generated statement id (or -1 if none was returned)
     */
    public int insert(Statement s) {
        String sql = "INSERT INTO statements("
                + "account_id, period_start, period_end, beginning_balance, "
                + "total_deposits, total_withdrawals, ending_balance) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, s.getAccountId());
            ps.setString(2, dateToString(s.getPeriodStart()));
            ps.setString(3, dateToString(s.getPeriodEnd()));
            ps.setDouble(4, moneyToDouble(s.getBeginningBalance()));
            ps.setDouble(5, moneyToDouble(s.getTotalDeposits()));
            ps.setDouble(6, moneyToDouble(s.getTotalWithdrawals()));
            ps.setDouble(7, moneyToDouble(s.getEndingBalance()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert statement", e);
        }
    }

    /**
     * Finds a statement by its primary key.
     *
     * @param id the statement id
     * @return an {@link Optional} statement (without its transactions loaded)
     */
    public Optional<Statement> findById(int id) {
        String sql = baseSelect() + " WHERE statement_id = ?";
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
            throw new RuntimeException("Failed to find statement by id", e);
        }
    }

    /**
     * Finds all statements belonging to an account.
     *
     * @param accountId the owning account id
     * @return statements for that account, ordered by id
     */
    public List<Statement> findByAccountId(int accountId) {
        String sql = baseSelect() + " WHERE account_id = ? ORDER BY statement_id";
        List<Statement> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find statements by account", e);
        }
    }

    /**
     * @return every statement, ordered by id
     */
    public List<Statement> findAll() {
        String sql = baseSelect() + " ORDER BY statement_id";
        List<Statement> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list statements", e);
        }
    }

    // ------------------------------------------------------------------
    // Row mapping & conversion helpers
    // ------------------------------------------------------------------

    /** Common SELECT clause shared by the finder methods. */
    private String baseSelect() {
        return "SELECT statement_id, account_id, period_start, period_end, "
                + "beginning_balance, total_deposits, total_withdrawals, ending_balance "
                + "FROM statements";
    }

    /**
     * Maps the current row to a {@link Statement}. Transactions are left as the
     * empty list initialized by the model.
     */
    private Statement mapRow(ResultSet rs) throws SQLException {
        Statement s = new Statement();
        s.setId(rs.getInt("statement_id"));
        s.setAccountId(rs.getInt("account_id"));
        s.setPeriodStart(stringToDate(rs.getString("period_start")));
        s.setPeriodEnd(stringToDate(rs.getString("period_end")));
        s.setBeginningBalance(doubleToMoney(rs.getDouble("beginning_balance")));
        s.setTotalDeposits(doubleToMoney(rs.getDouble("total_deposits")));
        s.setTotalWithdrawals(doubleToMoney(rs.getDouble("total_withdrawals")));
        s.setEndingBalance(doubleToMoney(rs.getDouble("ending_balance")));
        return s;
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
