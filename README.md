# Accounting AI Financial Assistant

A JavaFX desktop application that imports bank/financial statement PDFs, parses
their contents into a structured SQLite database, previews documents, searches
transactions, and exports statements to CSV, Excel (.xlsx), and PDF.

> **Course:** Farmingdale State College - CSC 325 Capstone
> **Group:** 02 - Summer 2026

---

## Overview

Small businesses and individuals receive financial statements as PDFs that are
tedious to read, search, and reconcile by hand. The Accounting AI Financial
Assistant streamlines that workflow: upload a statement PDF, and the app extracts
the account details, balance summary, and individual transactions, stores them in
a local database, and lets you preview, search, and re-export the data in the
format you need. Everything runs locally - no cloud services and no data leaves
your machine.

## Team - Group 02

| Role | Member |
| --- | --- |
| Project Manager | Jake Pepitone |
| Backend | Mohammed |
| Database | Ramon |
| Frontend | Kundan |
| QA | Gabriel |

## Objectives

- Provide a simple, local, desktop tool for turning statement PDFs into
  structured, searchable financial data.
- Persist parsed accounts, statements, and transactions in an embedded SQLite
  database that requires zero setup from the user.
- Support re-exporting statement data in multiple common formats (CSV, Excel, PDF).
- Generate local AI-style document insights, including semantic metadata and a
  concise imported-statement summary.
- Keep the codebase readable and well-tested as a student capstone reference.

## MVP Features (10)

1. **User login** - secure sign-in backed by SHA-256 password hashing.
2. **PDF upload & import** - select a statement PDF and import it in one click.
3. **Text extraction** - pull the raw text out of each PDF (Apache PDFBox).
4. **Statement parsing** - detect the period, balance summary, and transactions.
5. **Local database storage** - persist accounts, statements, and transactions
   in SQLite.
6. **Document preview** - render the first page as an image plus its extracted text.
7. **Transaction & text search** - find matches within the current document and
   across stored transactions.
8. **Batch / folder import** - import multiple PDFs at once with progress reporting.
9. **Multi-format export** - export a statement to CSV, Excel (.xlsx), or PDF.
10. **Settings** - configure the default export folder, export format, theme, and
    page size.

### AI Backend Analysis

Imported PDFs now receive local AI-style analysis during the backend import
pipeline. The app classifies the document, extracts semantic metadata such as
customer/account/period/transaction counts, and generates a short financial
summary. The implementation is deterministic and local: no cloud API is called
and no financial data leaves the user's machine.

---

## Build & Run

### Requirements

- **JDK 21+** (the project compiles on JDK 22 as well).
- **Maven 3.9+** - or simply open the project in **IntelliJ IDEA**, **Eclipse**,
  or **NetBeans**, which bundle their own Maven and can run the goals below from
  the IDE without a separate Maven install.

### Run the application

```bash
mvn clean javafx:run
```

### Run the tests

```bash
mvn test
```

### Default login

| Username | Password |
| --- | --- |
| `admin` | `1234` |

### Database location

The SQLite database **self-creates on first launch** in the user's application-data
directory - it is not committed to the repository and you do not need to create it
by hand:

- **Windows:** `%LOCALAPPDATA%\AccountingAI\accounting-ai.db`
- **macOS / Linux (fallback):** `~/.accounting-ai/accounting-ai.db`

On that first launch the app also runs the bundled `schema.sql` and `seed.sql`,
which create the tables and insert the sample John Smith statement plus the default
`admin` user.

---

## Tech Stack

- **Language / Runtime:** Java 21
- **UI:** JavaFX 21.0.2 (Controls, FXML, Swing interop)
- **Database:** SQLite via `org.xerial:sqlite-jdbc`
- **PDF:** Apache PDFBox 3.0.5
- **Excel:** Apache POI 5.3.0 (`XSSFWorkbook`)
- **CSV:** Apache Commons CSV 1.11.0
- **Tests:** JUnit 5 (Jupiter)
- **Build:** Maven

## Project Structure

```
accounting-ai/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/accountingai/      # app, controllers, model, db, service, util
│   │   └── resources/
│   │       ├── com/accountingai/       # FXML views + styles.css
│   │       └── sql/                    # schema.sql, seed.sql
│   └── test/
│       └── java/com/accountingai/      # JUnit 5 tests
├── assets/                             # tracked project assets
└── docs/                               # QA test strategy & UAT plan
```

## License

Academic project for CSC 325 at Farmingdale State College. Not licensed for
commercial use.
