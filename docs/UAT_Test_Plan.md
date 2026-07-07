# User Acceptance Test (UAT) Plan — Accounting AI Financial Assistant

**Project:** Farmingdale State College · CSC 325 · Group 02 · Summer 2026
**QA Lead:** Gabriel
**Release under test:** MVP (v1.0-SNAPSHOT)
**Test type:** Manual, black-box, performed against the running JavaFX application.

---

## 1. Purpose

This plan validates that each of the **10 MVP features** works from an end user's
point of view. Each feature has a dedicated test case with preconditions, steps,
test data, and an expected result. A tester records **Pass** or **Fail** and any
notes, then signs off at the bottom.

For the automated unit/integration coverage that runs under `mvn test`, see
`Test_Strategy.md`.

---

## 2. Pre-test setup (applies to all cases)

1. Ensure **JDK 21+** and **Maven 3.9+** are installed (or open the project in
   IntelliJ / Eclipse / NetBeans, which bundle Maven).
2. From the project root, launch the app:
   ```bash
   mvn clean javafx:run
   ```
3. On first launch the SQLite database self-creates in the user's app-data
   directory (`%LOCALAPPDATA%/AccountingAI` on Windows, or
   `~/.accounting-ai` otherwise) and is seeded with the John Smith sample and the
   default `admin` user.
4. Have a **sample statement PDF** ready. The dev team can generate one that
   matches the expected format, or reuse `Sample_Financial_Statement.pdf`. It must
   contain: `Customer: John Smith`, `Account Number: 123456789`,
   `Period: 2026-06-01 to 2026-06-30`, the four balances, and the seven
   transaction lines.

**Default credentials:** username `admin`, password `1234`.

---

## 3. MVP feature → test case map

| # | MVP Feature | Test Case |
| :--- | :--- | :--- |
| 1 | User login / authentication | UAT-01 |
| 2 | Upload a single PDF statement | UAT-02 |
| 3 | Automatic text extraction & preview | UAT-03 |
| 4 | Statement parsing (balances + transactions) | UAT-04 |
| 5 | Persist imported data to the local database | UAT-05 |
| 6 | Document list & first-page image preview | UAT-06 |
| 7 | Search within document text & transactions | UAT-07 |
| 8 | Export a statement (PDF / CSV / Excel) | UAT-08 |
| 9 | Batch import a folder of PDFs | UAT-09 |
| 10 | Settings (default export folder, format, theme, page size) | UAT-10 |

---

## 4. Test cases

### UAT-01 — User login / authentication
| Field | Detail |
| :--- | :--- |
| **Precondition** | App launched; login screen visible. |
| **Test data** | Valid: `admin` / `1234`. Invalid: `admin` / `wrong`. Empty: blank fields. |
| **Steps** | 1. Leave both fields empty, click **Login**. 2. Enter `admin` / `wrong`, click **Login**. 3. Enter `admin` / `1234`, click **Login**. |
| **Expected result** | Step 1 → error "Please fill in all fields." Step 2 → error "Incorrect username or password." and the password field clears. Step 3 → main window opens (≈900×600, resizable). |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

### UAT-02 — Upload a single PDF statement
| Field | Detail |
| :--- | :--- |
| **Precondition** | Logged in; main window open. |
| **Test data** | The sample John Smith statement PDF. |
| **Steps** | 1. Click **Upload PDF**. 2. In the file chooser (PDF filter) select the sample PDF. 3. Confirm. |
| **Expected result** | Import succeeds; the status bar shows a success message; the document appears in the **Documents** list by file name. |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

### UAT-03 — Automatic text extraction & preview
| Field | Detail |
| :--- | :--- |
| **Precondition** | UAT-02 completed; the document is in the list. |
| **Test data** | Same sample PDF. |
| **Steps** | 1. Select the uploaded document in the **Documents** list. 2. Observe the **Document Preview** panel. |
| **Expected result** | The preview text area shows extracted text including "John Smith", "123456789", and "Payroll Deposit". No error in the status bar. |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

### UAT-04 — Statement parsing (balances + transactions)
| Field | Detail |
| :--- | :--- |
| **Precondition** | A sample statement has been imported (UAT-02). |
| **Test data** | Sample statement: ending balance 6226.44; Amazon Purchase -125.99; Payroll Deposit +2500.00. |
| **Steps** | 1. Export the selected statement to CSV (see UAT-08) **or** inspect via the DB. 2. Verify the parsed period, four balances, and 7 transactions. |
| **Expected result** | Period 2026-06-01 → 2026-06-30; ending balance 6226.44; exactly 7 transactions with correct signed amounts (deposits positive, withdrawals negative). |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

### UAT-05 — Persist imported data to the local database
| Field | Detail |
| :--- | :--- |
| **Precondition** | A statement has been imported. |
| **Test data** | John Smith / 123456789 sample. |
| **Steps** | 1. Import the sample PDF. 2. Fully close the application. 3. Relaunch and log in. 4. Observe the **Documents** list. |
| **Expected result** | The previously imported document is still listed after restart (data persisted to SQLite; no duplicate account created for the same account number). |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

### UAT-06 — Document list & first-page image preview
| Field | Detail |
| :--- | :--- |
| **Precondition** | At least one document imported. |
| **Test data** | Sample PDF. |
| **Steps** | 1. Select a document in the list. 2. Observe the preview image area. |
| **Expected result** | The document list shows each imported file by name; selecting one renders a first-page image of the PDF in the preview panel (aspect ratio preserved). |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

### UAT-07 — Search within document text & transactions
| Field | Detail |
| :--- | :--- |
| **Precondition** | A document is selected and its preview text is loaded. |
| **Test data** | Query `Amazon`; query `amazon` (lower-case); empty query. |
| **Steps** | 1. Type `Amazon` in the search box, click **Search**. 2. Repeat with `amazon`. 3. Clear the box and search again. |
| **Expected result** | Steps 1–2 → results list shows the Amazon match (search is case-insensitive) and the status bar shows a hit count. Step 3 → empty query resets the results. |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

### UAT-08 — Export a statement (PDF / CSV / Excel)
| Field | Detail |
| :--- | :--- |
| **Precondition** | A statement-linked document is selected. |
| **Test data** | Sample statement. |
| **Steps** | 1. Open the **Export** menu. 2. Choose **CSV**, pick a save location, save. 3. Repeat for **Excel (.xlsx)** and **PDF**. 4. Open each exported file. |
| **Expected result** | Three files are created with the correct extensions (.csv, .xlsx, .pdf). Each contains the account/period summary and all 7 transactions (e.g. "Amazon Purchase", "-125.99"). The Excel file has "Statement" and "Transactions" sheets. |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

### UAT-09 — Batch import a folder of PDFs
| Field | Detail |
| :--- | :--- |
| **Precondition** | Logged in. A folder containing several PDFs, at least one valid and one invalid/non-PDF. |
| **Test data** | 2 valid sample PDFs + 1 non-PDF (e.g. a `.txt` renamed) or corrupt file. |
| **Steps** | 1. Click **Import Folder**. 2. Multi-select all files. 3. Confirm and wait for processing. |
| **Expected result** | Processing runs in the background (UI stays responsive); a summary alert reports "X succeeded, Y failed" (valid files succeed, the invalid one fails without aborting the batch); the document list refreshes with the successful imports. |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

### UAT-10 — Settings (export folder, format, theme, page size)
| Field | Detail |
| :--- | :--- |
| **Precondition** | Logged in. |
| **Test data** | Export format `XLSX`; theme `Dark`; rows per page `75`; any valid folder. |
| **Steps** | 1. Click **Settings**. 2. Change default format, theme, rows per page; browse to an export folder. 3. Click **Save**. 4. Reopen **Settings**. |
| **Expected result** | The settings dialog is modal. Saved values persist and are shown when the dialog is reopened (written to `settings.properties`). Page size is limited to the 5–200 range by the spinner. |
| **Result** | ☐ Pass ☐ Fail |
| **Notes** | |

---

## 5. Defect log

| ID | Test case | Severity (S1–S4) | Description | Status |
| :--- | :--- | :--- | :--- | :--- |
| | | | | |
| | | | | |

(Severity guide: S1 Blocker, S2 Major, S3 Minor, S4 Cosmetic — see `Test_Strategy.md` §10.)

---

## 6. Summary & sign-off

| Metric | Value |
| :--- | :--- |
| Total test cases | 10 |
| Passed | |
| Failed | |
| Open defects (S1/S2) | |

**Overall UAT result:** ☐ Pass ☐ Fail (conditional) ☐ Fail

| Role | Name | Signature | Date |
| :--- | :--- | :--- | :--- |
| QA Lead | Gabriel | | |
| Project Manager | Jake Pepitone | | |
| Backend | Mohammed | | |
| Database | Ramon | | |
| Frontend | Kundan | | |
