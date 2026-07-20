package com.accountingai.db;

import com.accountingai.util.AppPaths;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central SQLite access point for the Accounting AI application.
 *
 * <p>Owns the location of the on-disk database file and knows how to hand out
 * JDBC {@link Connection}s (with foreign-key enforcement turned on). It can also
 * bootstrap a brand-new database by running the bundled {@code schema.sql} and
 * {@code seed.sql} scripts from the classpath. {@link #initialize()} is safe to
 * call on every launch: it only creates tables/seed data when the database is
 * still empty.</p>
 */
public class DatabaseManager {

    /** Absolute path to the SQLite database file this manager talks to. */
    private final Path dbFile;

    /**
     * Creates a manager pointing at an explicit database file. Useful for tests
     * that want an isolated, throw-away database.
     *
     * @param dbFile the SQLite file to use (may not yet exist on disk)
     */
    public DatabaseManager(Path dbFile) {
        this.dbFile = dbFile;
    }

    /**
     * Creates a manager pointing at the default per-user database location
     * (see {@link AppPaths#databaseFile()}).
     */
    public DatabaseManager() {
        this(AppPaths.databaseFile());
    }

    /**
     * Opens a fresh JDBC connection to the SQLite database and enables foreign
     * key constraint enforcement (SQLite leaves this OFF by default).
     *
     * <p>The caller owns the returned connection and MUST close it — DAOs use
     * try-with-resources for exactly this reason.</p>
     *
     * @return an open {@link Connection}
     * @throws SQLException if the connection could not be established
     */
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toString());
        // SQLite disables FK enforcement per-connection unless explicitly enabled.
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys=ON;");
        } catch (SQLException e) {
            // If enabling the pragma fails, don't leak the half-open connection.
            connection.close();
            throw e;
        }
        return connection;
    }

    /**
     * Ensures the database is ready for use.
     *
     * <p>If the database contains no user tables yet, this runs the bundled
     * {@code /sql/schema.sql} followed by {@code /sql/seed.sql}. If tables
     * already exist, it does nothing. This makes it safe to invoke on every
     * application start.</p>
     */
    public void initialize() {
        try (Connection connection = getConnection()) {
            if (isEmptyDatabase(connection)) {
                runScript(connection, "/sql/schema.sql");
                runScript(connection, "/sql/seed.sql");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize the database", e);
        }
    }

    /**
     * @return the SQLite file backing this manager
     */
    public Path getDbFile() {
        return dbFile;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Determines whether the database has any user-defined tables. SQLite keeps
     * its own bookkeeping tables prefixed with {@code sqlite_}; those are ignored.
     *
     * @param connection an open connection
     * @return {@code true} if no user tables exist yet
     * @throws SQLException if the query fails
     */
    private boolean isEmptyDatabase(Connection connection) throws SQLException {
        String sql = "SELECT count(*) FROM sqlite_master "
                + "WHERE type='table' AND name NOT LIKE 'sqlite_%'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    /**
     * Loads a SQL script from the classpath, splits it into individual
     * statements on {@code ';'}, and executes each non-blank statement.
     *
     * @param connection   an open connection
     * @param resourcePath classpath resource path, e.g. {@code "/sql/schema.sql"}
     * @throws SQLException if any statement fails to execute
     */
    private void runScript(Connection connection, String resourcePath) throws SQLException {
        String script = readResource(resourcePath);
        try (Statement stmt = connection.createStatement()) {
            // Split on ';' — our bundled scripts are simple enough that this is
            // sufficient (no semicolons embedded inside string literals).
            for (String rawStatement : script.split(";")) {
                String sql = rawStatement.trim();
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }
        }
    }

    /**
     * Reads an entire classpath resource into a UTF-8 string.
     *
     * @param resourcePath classpath resource path
     * @return the file contents as a string
     */
    private String readResource(String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resourcePath);
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource: " + resourcePath, e);
        }
    }
}
