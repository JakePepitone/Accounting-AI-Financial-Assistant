-- ============================================================
-- Accounting AI Financial Assistant - Seed Data (SQLite)
-- ============================================================
-- Uses "INSERT OR IGNORE" so re-running on an already-seeded database
-- is harmless (the UNIQUE / PRIMARY KEY constraints skip duplicates).
--
-- Seeds:
--   * One sample customer: John Smith (account_id 1)
--   * One sample statement: id 101 (June 2026)
--   * Seven sample transactions: ids 1001..1007
--   * One default login user: admin / 1234
-- ============================================================

-- Sample account. Explicit account_id 1 so foreign keys are predictable.
INSERT OR IGNORE INTO accounts (account_id, customer_name, account_number)
VALUES (1, 'John Smith', '123456789');

-- Sample statement for June 2026.
-- beginning 5000.00, deposits 2500.00, withdrawals 1273.56, ending 6226.44.
INSERT OR IGNORE INTO statements
    (statement_id, account_id, period_start, period_end,
     beginning_balance, total_deposits, total_withdrawals, ending_balance)
VALUES
    (101, 1, '2026-06-01', '2026-06-30', 5000.00, 2500.00, 1273.56, 6226.44);

-- Seven sample transactions. Deposits are positive, withdrawals negative.
INSERT OR IGNORE INTO transactions
    (transaction_id, statement_id, transaction_date, description, amount)
VALUES
    (1001, 101, '2026-06-01', 'Payroll Deposit',   2500.00),
    (1002, 101, '2026-06-03', 'Amazon Purchase',   -125.99),
    (1003, 101, '2026-06-05', 'Utility Bill',        -85.12),
    (1004, 101, '2026-06-10', 'Restaurant',          -62.45),
    (1005, 101, '2026-06-15', 'Gas Station',         -48.00),
    (1006, 101, '2026-06-20', 'Grocery Store',      -152.00),
    (1007, 101, '2026-06-25', 'Internet Service',    -75.00);

-- Default login user.
-- password_hash is the lowercase-hex SHA-256 of the password "1234".
-- PasswordUtil.sha256("1234") MUST equal this literal.
INSERT OR IGNORE INTO users (user_id, full_name, username, email, password_hash)
VALUES
    (1, 'Admin User', 'admin', 'admin@accounting-ai.local',
     '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4');
