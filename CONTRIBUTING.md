# Contributing

Thanks for contributing to the **Accounting AI Financial Assistant** (Farmingdale
CSC 325 Capstone, Group 02). This guide covers how to build, run, and test the
project, our branch and commit conventions, where runtime files live, and the
list of features that are intentionally out of scope for the MVP.

---

## Prerequisites

- **JDK 21+** (compiles on JDK 22 too).
- **Maven 3.9+**, or an IDE (IntelliJ IDEA / Eclipse / NetBeans) with bundled Maven.

## Build, Run & Test

| Task | Command |
| --- | --- |
| Compile & launch the app | `mvn clean javafx:run` |
| Run the unit tests | `mvn test` |
| Build the jar | `mvn clean package` |
| Clean build output | `mvn clean` |

Default login for manual testing: **`admin` / `1234`**.

---

## Branching

Create a feature branch off of the default branch - never commit directly to it.

```
feature/<short-descriptive-name>
```

Examples:

```
feature/csv-exporter
feature/settings-dialog
feature/batch-import-progress
```

For fixes you may use `fix/<name>` and for documentation `docs/<name>`, following
the same lowercase, hyphenated style.

## Commit Messages

Use **Conventional Commits**:

```
<type>(<optional scope>): <short summary>
```

Common types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `build`.

Examples:

```
feat(export): add XLSX exporter using Apache POI
fix(db): open a fresh connection per DAO method
test(parser): cover missing-balance fallback
docs(readme): add Build & Run section
```

Keep the summary in the imperative mood ("add", not "added") and under ~72
characters. Add a body when the *why* is not obvious from the summary.

## Pull Requests

1. Branch from the default branch using the naming scheme above.
2. Make sure `mvn test` passes locally before opening the PR.
3. Describe **what** changed and **why**, and reference any related task/issue.
4. Request review from the relevant owner (see the team table in the README).

---

## Where Runtime & Gitignored Files Live

These are created at runtime and are **git-ignored** - do not commit them:

- **SQLite database** - the app self-creates it in the user app-data directory:
  - Windows: `%LOCALAPPDATA%\AccountingAI\accounting-ai.db`
  - macOS / Linux fallback: `~/.accounting-ai/accounting-ai.db`
- **Imported PDFs** - copied into `<app-data>/documents/`.
- **Settings file** - `<app-data>/settings.properties`.
- **Exports** - saved wherever you choose in the save dialog; the default export
  folder is your `Documents` directory (or `<app-data>/exports/` as a fallback).
- **Build output** - everything under `target/`.

The `.gitignore` already excludes `target/`, `*.db*`, `*.sqlite`, `exports/`,
`data/`, `*.log`, and IDE files. Note that `assets/` **is** tracked - commit
shared project assets there.

---

## Do NOT Add (FUTURE / out of scope for the MVP)

The following are deliberately **not** part of this capstone MVP. Please do not
add code, dependencies, or PRs for them:

- **AI / LLM integration** of any kind (OpenAI, Gemini, Claude, etc.). The
  "AI Insights" panel is a labeled placeholder only.
- **OCR** or scanned-image PDF processing (we handle text-based PDFs only).
- **Word (.docx) export** - the `DOCX` export format is a placeholder that reports
  "coming soon"; do not implement it.
- **Cloud storage / sync / remote databases** - the app is local-only (SQLite).
- **Extra third-party libraries** beyond those declared in `pom.xml` (no Lombok,
  Gson/Jackson, Guava, etc.).

Keeping scope tight keeps the build simple, the tests fast, and the code readable.
Thanks!
