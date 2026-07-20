# Database Schema & Sample Data Mapping

## 1. Project Context
The system requires a local relational database (SQLite) to persist document metadata, extracted text, and historical financial records between sessions (Functional Requirements FR-2.4, FR-4.1, FR-4.2). 

This document defines the relational schema mapping for extracted financial statement data.

---

## 2. Relational Database Schema
The database consists of three core tables to track user accounts, individual statement periods, and granular transaction line items.

### 2.1 Accounts Table
Stores unique customer account details.
* **`account_id`** (INT, Primary Key, Auto-Increment)
* **`customer_name`** (VARCHAR)
* **`account_number`** (VARCHAR)

### 2.2 Statements Table
Tracks statement summaries, balancing data, and time periods.
* **`statement_id`** (INT, Primary Key, Auto-Increment)
* **`account_id`** (INT, Foreign Key referencing `Accounts(account_id)`)
* **`period_start`** (DATE)
* **`period_end`** (DATE)
* **`beginning_balance`** (DECIMAL)
* **`total_deposits`** (DECIMAL)
* **`total_withdrawals`** (DECIMAL)
* **`ending_balance`** (DECIMAL)

### 2.3 Transactions Table
Stores itemized transaction data parsed during the text extraction phase (FR-2.3).
* **`transaction_id`** (INT, Primary Key, Auto-Increment)
* **`statement_id`** (INT, Foreign Key referencing `Statements(statement_id)`)
* **`transaction_date`** (DATE)
* **`description`** (VARCHAR)
* **`amount`** (DECIMAL)

---

## 3. Sample Data Mapping (`Sample_Financial_Statement.pdf`)
Below is the concrete data mapping extracted from the sample document into the defined schema tables.

### Accounts Data
| account_id | customer_name | account_number |
| :--- | :--- | :--- |
| `1` | John Smith | 123456789 |

### Statements Data
| statement_id | account_id | period_start | period_end | beginning_balance | total_deposits | total_withdrawals | ending_balance |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `101` | `1` | 2026-06-01 | 2026-06-30 | 5000.00 | 2500.00 | 1273.56 | 6226.44 |

### Transactions Data
| transaction_id | statement_id | transaction_date | description | amount |
| :--- | :--- | :--- | :--- | :--- |
| `1001` | `101` | 2026-06-01 | Payroll Deposit | 2500.00 |
| `1002` | `101` | 2026-06-03 | Amazon Purchase | -125.99 |
| `1003` | `101` | 2026-06-05 | Utility Bill | -85.12 |
| `1004` | `101` | 2026-06-10 | Restaurant | -62.45 |
| `1005` | `101` | 2026-06-15 | Gas Station | -48.00 |
| `1006` | `101` | 2026-06-20 | Grocery Store | -152.00 |
| `1007` | `101` | 2026-06-25 | Internet Service | -75.00 |

---

## 4. SQL Seeding Script (DML)
To populate local SQLite environment instances for development and Integration Testing (Section 6.2), use the following script:

```sql
-- 1. Insert Sample Account
INSERT INTO Accounts (account_id, customer_name, account_number)
VALUES (1, 'John Smith', '123456789');

-- 2. Insert Sample Statement Summary
INSERT INTO Statements (statement_id, account_id, period_start, period_end, beginning_balance, total_deposits, total_withdrawals, ending_balance)
VALUES (101, 1, '2026-06-01', '2026-06-30', 5000.00, 2500.00, 1273.56, 6226.44);

-- 3. Insert Sample Ledger Transactions
INSERT INTO Transactions (transaction_id, statement_id, transaction_date, description, amount) VALUES
(1001, 101, '2026-06-01', 'Payroll Deposit', 2500.00),
(1002, 101, '2026-06-03', 'Amazon Purchase', -125.99),
(1003, 101, '2026-06-05', 'Utility Bill', -85.12),
(1004, 101, '2026-06-10', 'Restaurant', -62.45),
(1005, 101, '2026-06-15', 'Gas Station', -48.00),
(1006, 101, '2026-06-20', 'Grocery Store', -152.00),
(1007, 101, '2026-06-25', 'Internet Service', -75.00);