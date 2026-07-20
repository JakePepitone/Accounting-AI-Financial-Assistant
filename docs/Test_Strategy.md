# Test Strategy — Accounting AI Financial Assistant

**Project:** Farmingdale State College · CSC 325 · Group 02 · Summer 2026
**QA Lead:** Gabriel
**Document owner:** QA
**Applies to:** MVP release (v1.0-SNAPSHOT)

---

## 1. Purpose

This document describes *how* the Accounting AI Financial Assistant is tested. It
defines the two testing tracks we use — **automated unit/integration tests**
(JUnit 5, run by Maven) and **manual User Acceptance Testing (UAT)** — explains
what each track covers, and documents the fixture approach so any team member can
reproduce a test run.

The companion document `UAT_Test_Plan.md` contains the step-by-step manual test
cases that map to the 10 MVP features.

---

## 2. Testing objectives

- Prove that the core data pipeline (PDF → extracted text → parsed statement →
  persisted rows → exported file) works end to end.
- Protect the **seed contract**: the default `admin` / `1234` login and the John
  Smith sample data must never silently break.
- Catch regressions early via a fast, deterministic automated suite that runs on
  every developer machine with a single command.
- Confirm, through manual UAT, that the JavaFX user interface behaves correctly
  for a real user (upload, preview, search, export, settings).

---

## 3. Scope

### In scope
- Domain models (`com.accountingai.model`)
- Utilities (`com.accountingai.util`)
- Database manager + DAOs (`com.accountingai.db`, `com.accountingai.db.dao`)
- Services (`com.accountingai.service`) — PDF text extraction, statement parsing,
  metadata extraction, local AI document analysis, import, batch import, search,
  settings
- Exporters (`com.accountingai.service.export`) — CSV, XLSX, PDF, and the
  DefaultExportService dispatcher
- UI behavior (manual UAT only)

### Out of scope (MVP)
- Cloud AI / OpenAI / Gemini / Claude analysis
- OCR of scanned/image-only PDFs
- Word (.docx) export (present as a disabled, "coming soon" format)
- Cloud storage / multi-user sync

---

## 4. Test levels

| Level | Track | Tooling | Where |
| :--- | :--- | :--- | :--- |
| Unit | Automated | JUnit 5 (Jupiter) | `src/test/java/**` |
| Integration (DB, PDF round-trip) | Automated | JUnit 5 + real SQLite/PDFBox in a `@TempDir` | `src/test/java/**` |
| System / Acceptance | Manual | Human tester following `UAT_Test_Plan.md` | Running JavaFX app |

We deliberately keep the "integration" tests in the same JUnit suite: they use a
**real** SQLite database file and **real** PDFBox documents, but everything is
created fresh inside a JUnit `@TempDir`, so they need no external services and
stay fast and repeatable.

---

## 5. Automated test suite

### 5.1 How to run

From the project root:

```bash
mvn test
```

This compiles `src/main/java` and `src/test/java`, then the **Maven Surefire**
plugin discovers and runs every `*Test` class. A green build means all automated
checks passed. To run the whole build including packaging:

```bash
mvn clean verify
```

To run a single test class while developing:

```bash
mvn -Dtest=StatementParserTest test
```

`SmokeTest` is the canary: if it fails, the JUnit 5 / Surefire wiring itself is
broken, independent of any application logic.

### 5.2 What the automated tests cover

| Area | Test class(es) | Key assertions |
| :--- | :--- | :--- |
| Harness wiring | `SmokeTest` | JUnit 5 + Surefire run at all |
| Password / seed contract | `PasswordUtilTest` | `sha256("1234")` equals the seeded admin hash |
| File saving | `FileSaverTest` | Bytes written, parent dirs created, extension enforced |
| Accounts DAO | `AccountDaoTest` | Seed round-trips; insert + findOrCreate (no duplicates) |
| Statements DAO | `StatementDaoTest` | Seed statement 101 + balances; insert round-trip |
| Transactions DAO | `TransactionDaoTest` | 7 seeded rows; case-insensitive search; `insertBatch` |
| Document DAO | `DaoSmokeTest` | Insert/find/search/updateStatus round-trip |
| Statement parsing | `StatementParserTest` | Dates, balances, 7 txns, account name/number, never throws |
| PDF text extraction | `PdfTextExtractorTest` | Extracts sample text; validates real vs non-PDF |
| Metadata extraction | `MetadataExtractorTest` | Page count, file name/size, IMPORTED status |
| Local AI analysis | `DocumentAiServiceTest` | Document classification, semantic metadata, summary |
| PDF import | `PdfImportServiceTest` | Success + copied file; non-PDF/empty file fails |
| Search | `SearchServiceTest` | Case-insensitive text search; blank query empty; DB txn search |
| Settings | `SettingsServiceTest` | Defaults created; round-trip; page size clamped [5,200] |
| Batch import | `BatchProcessorTest` | Success/failure counts, continue-on-error, listener callbacks |
| CSV export | `CsvExporterTest` | Contains descriptions, signed amounts, header, summary |
| XLSX export | `XlsxExporterTest` | Two sheets; 7 transaction data rows |
| PDF export | `PdfExporterTest` | Re-extracted text contains sample; ≥ 1 page |
| Export dispatch | `DefaultExportServiceTest` | CSV/XLSX write files; DOCX throws |

---

## 6. Fixture approach

We do **not** commit any binary PDF fixtures to the repository. Instead:

- **`TestPdfs`** (`com.accountingai.TestPdfs`) generates PDFs at runtime with
  Apache PDFBox directly into a JUnit `@TempDir`.
  - `writeSampleStatement(Path)` produces a text-based PDF containing the exact
    John Smith sample (customer, account number, period, four balances, and the
    seven transaction lines).
  - `writeEmptyPdf(Path)` produces a one-page blank PDF for negative tests.
- **`TestFixtures`** (`com.accountingai.TestFixtures`) supplies in-memory sample
  domain objects (`sampleAccount`, `sampleStatement`, `sampleTransactions`,
  `sampleExportData`) so parser/exporter tests share one known-good dataset.
- **`DbTestSupport`** (`com.accountingai.db.DbTestSupport`) creates and
  `initialize()`s a fresh SQLite database in a temp path (schema + seed applied),
  giving every DAO test an isolated, pre-seeded database.

**Benefits:** no committed binaries, no test pollution of the real user database
or settings file, deterministic data, and every test is self-contained and
order-independent.

---

## 7. Test data

The canonical dataset is the John Smith sample:

- Account 1 — John Smith, account number `123456789`
- Statement 101 — 2026-06-01 to 2026-06-30; beginning 5000.00, deposits 2500.00,
  withdrawals 1273.56, ending 6226.44
- Seven transactions (Payroll Deposit +2500.00, Amazon Purchase -125.99, Utility
  Bill -85.12, Restaurant -62.45, Gas Station -48.00, Grocery Store -152.00,
  Internet Service -75.00)

Any change to this data must be made in **both** `seed.sql` and `TestFixtures` /
`TestPdfs`, and the affected assertions updated.

---

## 8. Environment

| Item | Requirement |
| :--- | :--- |
| JDK | 21+ (compiles on 22) |
| Build tool | Maven 3.9+ (or the Maven bundled in IntelliJ/Eclipse/NetBeans) |
| Test framework | JUnit 5 (`org.junit.jupiter`) |
| DB | SQLite via `org.xerial:sqlite-jdbc` (file-based, created in temp dirs for tests) |
| PDF | Apache PDFBox 3.0.5 (`org.apache.pdfbox.Loader.loadPDF`) |
| Excel | Apache POI 5.3.0 (`XSSFWorkbook`) |
| CSV | `org.apache.commons:commons-csv` 1.11.0 |

Automated tests are headless — they do **not** launch the JavaFX UI, so they run
on CI or any developer machine without a display.

---

## 9. Entry / exit criteria

**Entry (before a release candidate):**
- All source compiles (`mvn clean compile` succeeds).
- The full automated suite is expected to pass locally.

**Exit (release ready):**
- `mvn test` is green (100% of automated tests pass).
- All 10 UAT cases in `UAT_Test_Plan.md` are executed and marked **Pass**.
- No open Severity-1 (blocker) or Severity-2 (major) defects.

---

## 10. Defect severity guide

| Severity | Meaning | Example |
| :--- | :--- | :--- |
| S1 – Blocker | App unusable / data loss | Cannot log in; DB fails to initialize |
| S2 – Major | Core feature broken | Import succeeds but no rows persisted |
| S3 – Minor | Feature works with a workaround | Export dialog remembers wrong folder |
| S4 – Cosmetic | Visual / wording only | Misaligned label, typo |

---

## 11. Roles

| Role | Owner | Responsibility |
| :--- | :--- | :--- |
| QA Lead | Gabriel | Owns this strategy, the UAT plan, and sign-off |
| Backend | Mohammed | Fixes service/parser defects |
| Database | Ramon | Fixes schema/DAO defects |
| Frontend | Kundan | Fixes UI/UAT defects |
| PM | Jake Pepitone | Prioritizes defects, approves release |
