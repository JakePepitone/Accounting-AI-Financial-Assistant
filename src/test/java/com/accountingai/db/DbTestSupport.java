package com.accountingai.db;

import java.nio.file.Path;

/**
 * Small helper for DAO/database tests.
 *
 * <p>Every DB-touching test wants the same thing: a brand-new SQLite database at
 * a temp path that has already been {@code initialize()}d (schema + seed applied).
 * This factory centralizes that so the tests stay focused on assertions.</p>
 */
public final class DbTestSupport {

    private DbTestSupport() {
        // Static helper — not instantiable.
    }

    /**
     * Creates a {@link DatabaseManager} pointed at {@code dbFile}, initializes it
     * (creating the schema and seeding the John Smith sample on first use), and
     * returns it ready to use.
     *
     * @param dbFile the SQLite file path (typically inside a {@code @TempDir})
     * @return an initialized {@link DatabaseManager}
     */
    public static DatabaseManager freshDb(Path dbFile) {
        DatabaseManager db = new DatabaseManager(dbFile);
        db.initialize();
        return db;
    }
}
