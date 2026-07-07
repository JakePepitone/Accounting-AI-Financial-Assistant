-- ============================================================
-- Accounting AI Financial Assistant - Database Schema (SQLite)
-- ============================================================
-- All statements use "CREATE TABLE IF NOT EXISTS" so this script is
-- safe to run on every application launch. Money columns are stored as
-- REAL and dates/timestamps are stored as ISO-8601 TEXT strings.
--
-- Tables:
--   accounts           - one row per bank account / customer
--   statements         - one row per imported statement period
--   transactions       - individual line items belonging to a statement
--   document_metadata  - metadata about each imported PDF document
--   users              - application login accounts
-- ============================================================

-- Bank accounts. account_number is unique so a customer's account is
-- looked up (findOrCreate) rather than duplicated on re-import.
CREATE TABLE IF NOT EXISTS accounts (
    account_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_name  TEXT NOT NULL,
    account_number TEXT NOT NULL UNIQUE
);

-- A single statement period for an account, with its balance summary.
-- Balances are REAL (dollar amounts). Period dates are ISO TEXT (yyyy-MM-dd).
CREATE TABLE IF NOT EXISTS statements (
    statement_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id         INTEGER NOT NULL,
    period_start       TEXT,
    period_end         TEXT,
    beginning_balance  REAL,
    total_deposits     REAL,
    total_withdrawals  REAL,
    ending_balance     REAL,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

-- Individual transactions belonging to a statement. amount is REAL:
-- positive for deposits, negative for withdrawals.
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    statement_id     INTEGER NOT NULL,
    transaction_date TEXT,
    description      TEXT,
    amount           REAL,
    FOREIGN KEY (statement_id) REFERENCES statements(statement_id)
);

-- Metadata captured for each imported PDF file. statement_id is nullable
-- because metadata may be recorded before/without a linked statement.
CREATE TABLE IF NOT EXISTS document_metadata (
    document_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name       TEXT NOT NULL,
    file_path       TEXT,
    file_size_bytes INTEGER,
    page_count      INTEGER,
    title           TEXT,
    author          TEXT,
    uploaded_at     TEXT,
    statement_id    INTEGER,
    status          TEXT,
    FOREIGN KEY (statement_id) REFERENCES statements(statement_id)
);

-- Application users. password_hash is a lowercase-hex SHA-256 string.
CREATE TABLE IF NOT EXISTS users (
    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);
