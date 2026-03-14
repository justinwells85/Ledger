# Ledger — Project Guide

## What This Is

Ledger is a financial planning and reconciliation system for tracking vendor contract costs. It replaces an Excel-based accrual tracking workbook with a structured data model, double-entry accounting, version-controlled budgets, and SAP actuals reconciliation.

## Tech Stack

- **Backend:** Java 21 + Spring Boot 3.x
- **API:** REST (Spring Web)
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA / Hibernate
- **Migrations:** Flyway
- **Frontend:** React (separate frontend directory)
- **Build:** Gradle
- **Testing:** JUnit 5 + AssertJ + Testcontainers (PostgreSQL)

## Project Structure

```
Ledger/
├── specs/                    # Source of truth — all business requirements
│   ├── README.md             # Spec index and methodology overview
│   ├── 01-domain-model.md
│   ├── 02-journal-ledger.md
│   ├── 03-fiscal-calendar.md
│   ├── 04-milestone-versioning.md
│   ├── 05-sap-ingestion.md
│   ├── 06-reconciliation.md
│   ├── 07-accrual-lifecycle.md
│   ├── 08-time-machine.md
│   ├── 09-reporting.md
│   ├── 10-business-rules.md
│   └── 11-change-management.md
├── src/main/java/            # Spring Boot application
├── src/test/java/            # Tests (derived from specs)
├── frontend/                 # React application
├── CLAUDE.md                 # This file
└── build.gradle
```

## Development Methodology

**Spec-driven development with strict Red-Green-Refactor TDD.**

### TDD Cycle (enforced on every task)

```
┌─────────────────────────────────────────────────────────────────┐
│  1. SPEC    → Read the relevant spec file(s) and business rules │
│  2. RED     → Write test(s) that fail — compile but assert fail │
│  3. GREEN   → Write the minimum implementation to pass tests    │
│  4. REFACTOR→ Clean up without changing behavior, tests stay ✅  │
│  5. VERIFY  → Confirm implementation satisfies the spec         │
│  6. REPEAT  → Next test case within the task                    │
└─────────────────────────────────────────────────────────────────┘
```

### Rules

1. **RED first, always.** Write the test. Run it. Confirm it fails. If it passes without new code, the test is not testing anything new — fix the test or skip it.
2. **GREEN means minimal.** Write only enough production code to make the failing test pass. Do not anticipate future tests or add unrequested behavior.
3. **Refactor only when green.** All tests must pass before and after refactoring. Refactoring changes structure, not behavior.
4. **One test at a time.** Do not write 10 tests and then implement. Write one test (red), implement (green), refactor, then write the next test.
5. **Tests must reference a spec.** Every test class or method documents which spec section and/or business rule it validates (e.g., `// Spec: 02-journal-ledger.md, BR-01`).
6. **Do not write implementation code without a failing test.** If there is no test for it, there is no reason to write it.
7. **Do not write tests without referencing a specific spec section or business rule.** Tests exist to validate the spec, not to test random behavior.
8. **Run the full test suite after each green.** Catch regressions immediately.

### Test Layers and When to Use Each

| Layer | When | Framework | TDD Cycle |
|-------|------|-----------|-----------|
| **Unit** | Pure logic (validation, computation, hash) — no DB, no Spring | JUnit 5 + AssertJ, mocked deps | Fast red-green cycles |
| **Integration** | Service + repository + DB interaction | Spring Boot Test + Testcontainers | Slower cycles, run per-task |
| **E2E / Acceptance** | Complete multi-step workflow validation | Spring Boot Test + REST Assured | Run after phase completion |
| **Frontend** | Component rendering, interaction, API calls | Vitest + React Testing Library + MSW | Component-level red-green |

### Task Execution Flow (Concrete Example)

For task T06 (Journal Ledger Service):

```
Step 1: Read spec 02-journal-ledger.md, note BR-01, BR-02

Step 2: Write U-JRN-01 — "Balanced entry accepted"
  → Create JournalValidationTest.java
  → Write test: create entry with debit $25K, credit $25K, assert no exception
  → Run test → RED (JournalService doesn't exist yet)

Step 3: Create JournalService with createEntry() — just enough to pass
  → Run test → GREEN

Step 4: Write U-JRN-02 — "Unbalanced entry rejected"
  → Write test: create entry with debit $25K, credit $20K, assert exception
  → Run test → RED (no validation yet)

Step 5: Add balance validation to createEntry()
  → Run ALL tests → GREEN (U-JRN-01 still passes, U-JRN-02 now passes)

Step 6: Write U-JRN-03 — "Entry with 0 lines rejected"
  → RED → implement → GREEN

...continue for each test case in the task...

Step N: Refactor JournalService (extract methods, clean up)
  → Run ALL tests → still GREEN

Step N+1: Verify against spec — does the implementation satisfy
  all requirements in 02-journal-ledger.md?
```

## Key Architecture Decisions

- **Double-entry journal** is the source of truth for all financial data (spec 02)
- **Milestone versioning** is independent per milestone, not whole-plan snapshots (spec 04)
- **Reconciliation** is a mapping layer between actuals and milestones — journal entries for actuals are not modified when reconciled (spec 06)
- **Time machine** is a query filter (asOfDate parameter), not a data copy (spec 08)
- **All derived values** (variance, status, forecast) are computed from journal/reconciliation queries, never stored

## Conventions

- Use `BigDecimal` for all monetary amounts, never `double` or `float`
- Use `UUID` for all primary keys except Project (which uses the business key `PR#####`)
- Use `LocalDate` for dates, `Instant` for timestamps
- All service methods that return financial data accept an optional `LocalDate asOfDate` parameter for time machine support
- Entity field validation happens in the service layer, not the controller
- Journal balance invariant (BR-01) is enforced at the service layer AND verified in tests

## Business Rules Reference

See `specs/10-business-rules.md` for the complete rule catalog. Key rules:
- **BR-01:** Every journal entry must balance (debits = credits)
- **BR-06:** An actual can be reconciled to at most one milestone
- **BR-08:** SAP imports deduplicated by SHA-256 hash
- **BR-41:** Time machine uses effective_date for plan queries
- **BR-42:** Milestone version changes require a reason

## Current Phase

**Planning complete.** 15 spec files, 8 Flyway migrations, 36 implementation tasks with 124+ test cases defined. Ready for implementation starting at T01 (Project Skeleton).
