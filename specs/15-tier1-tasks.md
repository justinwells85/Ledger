# Tier 1 Task Breakdown

> Implementation tasks in dependency order. Each task includes scope, spec references,
> test cases (Given/When/Then), and the definition of done.
>
> **TDD Protocol (enforced on every task):**
> 1. Read the spec references listed for the task
> 2. Pick the first test case — write the test — run it — confirm RED (fails)
> 3. Write MINIMUM code to make it GREEN (pass)
> 4. REFACTOR if needed — all tests must stay green
> 5. Pick the next test case — repeat from step 2
> 6. After all tests for the task are green, run the FULL test suite
> 7. Verify the implementation against the spec — any gaps?
>
> See `16-test-plan.md` Section 1 for the complete TDD protocol and rules.
> See `16-test-plan.md` Section 1.5 for when E2E tests run relative to phases.

---

## Task Execution Order

```
Phase 1: Foundation
  T01  Project Skeleton
  T02  Database + Flyway
  T03  Fiscal Calendar Entity + API
  T04  Contract Entity + API
  T05  Project Entity + API

Phase 2: Core Financial Engine
  T06  Journal Ledger Service
  T07  Milestone + Version Entity
  T08  Milestone Creation (v1 + PLAN_CREATE journal)
  T09  Milestone Version Adjustment (PLAN_ADJUST journal)
  T10  Milestone Cancel

Phase 3: SAP Integration
  T11  SAP Import — File Upload + Parse
  T12  SAP Import — Dedup + Stage
  T13  SAP Import — Commit (ACTUAL_IMPORT journal)

Phase 4: Reconciliation
  T14  Reconciliation — Create (assign actual to milestone)
  T15  Reconciliation — Undo
  T16  Reconciliation — Status Derivation
  T17  Accrual Lifecycle Tracking

Phase 5: Time Machine
  T18  Time Machine — Milestone Version Queries
  T19  Time Machine — Actuals + Reconciliation Queries
  T20  Time Machine — Cross-cutting (all services respect asOfDate)

Phase 6: Reporting APIs
  T21  Budget Plan Report
  T22  Variance Report
  T23  Reconciliation Status Report
  T24  Open Accruals Report

Phase 7: Audit
  T25  Entity Audit Log
  T26  Audit Query APIs

Phase 8: Configuration
  T27  System Config CRUD

Phase 9: Frontend
  T28  React Skeleton + Shell
  T29  Dashboard View
  T30  Contract + Project + Milestone Views
  T31  SAP Import Flow
  T32  Reconciliation Workspace
  T33  Report Views
  T34  Journal Viewer
  T35  Time Machine UI
  T36  Settings View
```

---

## Phase 1: Foundation

### T01 — Project Skeleton

**Scope:** Initialize Spring Boot project with Gradle, configure dependencies, set up test infrastructure.

**Spec refs:** CLAUDE.md

**Work items:**
- Initialize Spring Boot 3.x project with Java 21
- `build.gradle` with dependencies: Spring Web, Spring Data JPA, Flyway, PostgreSQL driver, Testcontainers, JUnit 5, AssertJ
- Application properties for PostgreSQL connection + Flyway
- Testcontainers base test class with PostgreSQL
- React project in `/frontend` via Create React App or Vite
- Verify: `./gradlew build` passes, Testcontainers PostgreSQL starts in tests

**Tests:**
```
TEST T01-1: "Application context loads"
  GIVEN a Spring Boot application with all config
  WHEN  the application starts
  THEN  the context loads without errors

TEST T01-2: "Testcontainers PostgreSQL starts"
  GIVEN a test extending the base test class
  WHEN  the test runs
  THEN  a PostgreSQL container is running and accessible
```

**Done when:** `./gradlew test` passes with a running PostgreSQL test container.

---

### T02 — Database + Flyway

**Scope:** Integrate existing Flyway migrations, verify schema creation.

**Spec refs:** 12-database-schema.md, V001–V008

**Work items:**
- Ensure Flyway runs all 8 migrations on startup
- Verify all tables, indexes, constraints, and trigger are created
- Verify seed data (FY25/26/27 fiscal calendars)

**Tests:**
```
TEST T02-1: "All migrations run successfully"
  GIVEN an empty PostgreSQL database
  WHEN  the application starts
  THEN  Flyway reports 8 migrations applied
  AND   all 10 tables exist

TEST T02-2: "Fiscal calendar seed data present"
  GIVEN migrations have run
  WHEN  querying fiscal_period
  THEN  36 periods exist (12 per FY × 3 fiscal years)
  AND   FY26 Q1 contains Oct, Nov, Dec
  AND   FY26 Q2 contains Jan, Feb, Mar

TEST T02-3: "System config defaults present"
  GIVEN migrations have run
  WHEN  querying system_config
  THEN  tolerance_percent = 0.02
  AND   tolerance_absolute = 50.00
  AND   accrual_aging_warning_days = 60
  AND   accrual_aging_critical_days = 90

TEST T02-4: "Journal balance trigger exists"
  GIVEN migrations have run
  WHEN  inserting an unbalanced journal entry (debit $100, credit $50) and committing
  THEN  transaction is rejected with balance error
```

---

### T03 — Fiscal Calendar Entity + API

**Scope:** JPA entities, repository, service, controller for fiscal years and periods.

**Spec refs:** 01-domain-model.md (2.1, 2.2), 03-fiscal-calendar.md, 13-api-design.md (Section 2)

**Work items:**
- `FiscalYear` entity + repository
- `FiscalPeriod` entity + repository
- `FiscalCalendarService` with `resolvePeriod(LocalDate postingDate)` method
- REST controller: `GET /fiscal-years`, `GET /fiscal-years/{fy}/periods`

**Tests:**
```
TEST T03-1: "List fiscal years returns all years"
  GIVEN seed data with FY25, FY26, FY27
  WHEN  GET /api/v1/fiscal-years
  THEN  response contains 3 fiscal years ordered by start_date

TEST T03-2: "List periods for FY26 returns 12 periods"
  GIVEN FY26 seed data
  WHEN  GET /api/v1/fiscal-years/FY26/periods
  THEN  response contains 12 periods
  AND   sorted by sort_order 1–12
  AND   Q1 contains Oct, Nov, Dec

TEST T03-3: "Resolve posting date to fiscal period"
  GIVEN FY26 seed data
  WHEN  resolving posting date 2026-01-15
  THEN  returns FY26 Q2 January period

TEST T03-4: "Resolve October date maps to correct FY"
  GIVEN FY26 seed data
  WHEN  resolving posting date 2025-10-15
  THEN  returns FY26 Q1 October period (not FY25)

TEST T03-5: "Resolve date with no matching FY returns empty"
  GIVEN only FY25-FY27 exist
  WHEN  resolving posting date 2028-01-15
  THEN  returns empty/null (no matching period)
```

---

### T04 — Contract Entity + API

**Scope:** JPA entity, repository, service, controller for contracts.

**Spec refs:** 01-domain-model.md (2.3), 13-api-design.md (Section 3), 11-change-management.md

**Work items:**
- `Contract` entity + repository
- `ContractService` with create, update, list, get
- REST controller: `GET /contracts`, `GET /contracts/{id}`, `POST /contracts`, `PUT /contracts/{id}`
- Audit log entry on create and update

**Tests:**
```
TEST T04-1: "Create contract"
  GIVEN a valid contract request
  WHEN  POST /api/v1/contracts
  THEN  returns 201 with contract_id
  AND   contract is persisted with status = ACTIVE

TEST T04-2: "Create contract — name uniqueness"
  GIVEN a contract named "Globant ADM" already exists
  WHEN  POST /api/v1/contracts with name "Globant ADM"
  THEN  returns 409 Conflict

TEST T04-3: "Update contract requires reason"
  GIVEN an existing contract
  WHEN  PUT /api/v1/contracts/{id} without reason field
  THEN  returns 400 Bad Request

TEST T04-4: "Update contract creates audit log entry"
  GIVEN an existing contract with owner "Rob"
  WHEN  PUT /api/v1/contracts/{id} changing owner to "Brad" with reason
  THEN  audit_log has entry with entity_type=CONTRACT, action=UPDATE
  AND   changes JSON shows {"ownerUser": {"old": "Rob", "new": "Brad"}}

TEST T04-5: "List contracts filters by status"
  GIVEN 2 ACTIVE and 1 CLOSED contract
  WHEN  GET /api/v1/contracts?status=ACTIVE
  THEN  returns 2 contracts

TEST T04-6: "Contract end_date must be after start_date"
  GIVEN a contract request with end_date before start_date
  WHEN  POST /api/v1/contracts
  THEN  returns 400 Bad Request
```

---

### T05 — Project Entity + API

**Scope:** JPA entity, repository, service, controller for projects.

**Spec refs:** 01-domain-model.md (2.4), 13-api-design.md (Section 4)

**Work items:**
- `Project` entity + repository
- `ProjectService` with create, update, list, get
- REST controller: `GET /contracts/{id}/projects`, `POST /contracts/{id}/projects`, `GET /projects/{id}`, `PUT /projects/{id}`

**Tests:**
```
TEST T05-1: "Create project under contract"
  GIVEN an existing contract
  WHEN  POST /api/v1/contracts/{id}/projects with valid project data
  THEN  returns 201 with project_id
  AND   project has correct contract_id and funding_source

TEST T05-2: "Project ID uniqueness"
  GIVEN project PR13752 already exists
  WHEN  POST with project_id = PR13752 under a different contract
  THEN  returns 409 Conflict

TEST T05-3: "WBSE + Project ID uniqueness"
  GIVEN project with wbse "1174905.SU.ES" and id "PR13752" exists
  WHEN  POST with same wbse and project_id
  THEN  returns 409 Conflict

TEST T05-4: "List projects for contract"
  GIVEN contract with 3 projects
  WHEN  GET /api/v1/contracts/{id}/projects
  THEN  returns 3 projects

TEST T05-5: "Filter projects by funding source"
  GIVEN 2 OPEX projects and 1 CAPEX project
  WHEN  GET /api/v1/contracts/{id}/projects?fundingSource=CAPEX
  THEN  returns 1 project

TEST T05-6: "Funding source must be valid enum"
  GIVEN a project create request with fundingSource = "INVALID"
  WHEN  POST /api/v1/contracts/{id}/projects
  THEN  returns 400 Bad Request
```

---

## Phase 2: Core Financial Engine

### T06 — Journal Ledger Service

**Scope:** JPA entities, service for creating and querying journal entries. This is the core engine.

**Spec refs:** 02-journal-ledger.md, 10-business-rules.md (BR-01, BR-02)

**Work items:**
- `JournalEntry` entity + repository
- `JournalLine` entity + repository
- `JournalService.createEntry(type, effectiveDate, description, lines[])` — validates balance, persists
- Balance query methods: `getPlannedBalance(scope, period, asOfDate)`, `getActualBalance(scope, period, asOfDate)`

**Tests:**
```
TEST T06-1: "Create balanced journal entry"
  GIVEN a PLAN_CREATE entry with debit $25,000 PLANNED and credit $25,000 VARIANCE_RESERVE
  WHEN  createEntry is called
  THEN  entry is persisted with 2 lines
  AND   SUM(debit) = SUM(credit) = $25,000

TEST T06-2: "Reject unbalanced entry — BR-01"
  GIVEN an entry with debit $25,000 and credit $20,000
  WHEN  createEntry is called
  THEN  exception is thrown: "Journal entry is unbalanced"
  AND   nothing is persisted

TEST T06-3: "Reject entry with fewer than 2 lines — BR-02"
  GIVEN an entry with only 1 line
  WHEN  createEntry is called
  THEN  exception is thrown: "Journal entry must have at least 2 lines"

TEST T06-4: "Reject entry with 0 lines"
  GIVEN an entry with no lines
  WHEN  createEntry is called
  THEN  exception is thrown

TEST T06-5: "A line cannot have both debit and credit > 0"
  GIVEN a journal line with debit = $100 and credit = $50
  WHEN  createEntry is called
  THEN  exception is thrown

TEST T06-6: "Query planned balance for milestone"
  GIVEN journal entry: debit PLANNED $25,000 for milestone MS-001
  WHEN  getPlannedBalance(milestoneId=MS-001)
  THEN  returns $25,000

TEST T06-7: "Query planned balance with multiple entries"
  GIVEN PLAN_CREATE $25,000 and PLAN_ADJUST -$5,000 for MS-001
  WHEN  getPlannedBalance(milestoneId=MS-001)
  THEN  returns $20,000

TEST T06-8: "Query planned balance with asOfDate"
  GIVEN PLAN_CREATE effective 2025-11-01 $25,000 and PLAN_ADJUST effective 2026-02-15 -$5,000
  WHEN  getPlannedBalance(milestoneId=MS-001, asOfDate=2026-01-01)
  THEN  returns $25,000 (the adjustment isn't visible yet)

TEST T06-9: "4-line entry for period shift balances"
  GIVEN a period shift entry: -$20K from JAN, +$22K to FEB (4 lines)
  WHEN  createEntry is called
  THEN  entry persists with 4 lines
  AND   SUM(debit) = SUM(credit) = $42,000
```

---

### T07 — Milestone + Version Entity

**Scope:** JPA entities and repositories for Milestone and MilestoneVersion.

**Spec refs:** 01-domain-model.md (2.5, 2.6), 04-milestone-versioning.md

**Work items:**
- `Milestone` entity + repository
- `MilestoneVersion` entity + repository
- Repository methods for version queries (current, as-of-date, history)

**Tests:**
```
TEST T07-1: "MilestoneVersion unique constraint on (milestone_id, version_number)"
  GIVEN milestone MS-001 with version 1
  WHEN  inserting another version with version_number = 1 for MS-001
  THEN  constraint violation

TEST T07-2: "Find current version (highest version_number)"
  GIVEN milestone with versions 1, 2, 3
  WHEN  findCurrentVersion(milestoneId)
  THEN  returns version 3

TEST T07-3: "Find version as of date"
  GIVEN version 1 (effective 2025-11-01) and version 2 (effective 2026-02-15)
  WHEN  findVersionAsOfDate(milestoneId, 2026-01-01)
  THEN  returns version 1

TEST T07-4: "Find version as of date — exact match"
  GIVEN version 1 (effective 2025-11-01) and version 2 (effective 2026-02-15)
  WHEN  findVersionAsOfDate(milestoneId, 2026-02-15)
  THEN  returns version 2

TEST T07-5: "Find version as of date — before any version"
  GIVEN version 1 (effective 2025-11-01)
  WHEN  findVersionAsOfDate(milestoneId, 2025-10-01)
  THEN  returns null (milestone doesn't exist yet)
```

---

### T08 — Milestone Creation (v1 + PLAN_CREATE)

**Scope:** MilestoneService.createMilestone — creates milestone, v1, and PLAN_CREATE journal entry.

**Spec refs:** 04-milestone-versioning.md (Section 4 — Creating), 02-journal-ledger.md (5.1)

**Work items:**
- `MilestoneService.createMilestone(projectId, name, description, plannedAmount, fiscalPeriodId, effectiveDate, reason)`
- Creates Milestone + MilestoneVersion(v1) + PLAN_CREATE journal entry
- REST: `POST /api/v1/projects/{id}/milestones`

**Tests:**
```
TEST T08-1: "Create milestone produces v1 and journal entry"
  GIVEN a valid project
  WHEN  createMilestone(projectId, "Jan Sustainment", ..., $25,250, januaryPeriod, ...)
  THEN  milestone is created
  AND   milestone_version v1 exists with planned_amount = $25,250
  AND   journal_entry exists with type = PLAN_CREATE
  AND   journal has 2 lines: debit PLANNED $25,250, credit VARIANCE_RESERVE $25,250

TEST T08-2: "Create milestone — journal entry balances"
  GIVEN a milestone creation
  WHEN  checking the resulting journal entry
  THEN  SUM(debit) = SUM(credit)

TEST T08-3: "Create milestone — version references correct period"
  GIVEN a milestone created for January 2026
  WHEN  querying the version
  THEN  fiscal_period_id references the FY26 January period

TEST T08-4: "Create milestone — API returns full response"
  WHEN  POST /api/v1/projects/{id}/milestones
  THEN  response includes milestone_id, name, and currentVersion with all fields

TEST T08-5: "Create milestone — planned amount must be >= 0"
  WHEN  POST with plannedAmount = -1000
  THEN  returns 400 Bad Request
```

---

### T09 — Milestone Version Adjustment (PLAN_ADJUST)

**Scope:** MilestoneService.createVersion — new version with delta journal entry.

**Spec refs:** 04-milestone-versioning.md (Section 4 — Adjusting), 02-journal-ledger.md (5.2, 5.3, 5.4)

**Work items:**
- `MilestoneService.createVersion(milestoneId, plannedAmount, fiscalPeriodId, effectiveDate, reason)`
- Computes delta, creates MilestoneVersion, creates PLAN_ADJUST journal
- Handles same-period adjustments (2-line journal) and period shifts (4-line journal)
- REST: `POST /api/v1/milestones/{id}/versions`

**Tests:**
```
TEST T09-1: "Adjust amount down — same period"
  GIVEN milestone with v1: $25,250 in JAN
  WHEN  createVersion(milestoneId, $20,000, JAN, 2026-02-15, "scope cut")
  THEN  v2 created with planned_amount = $20,000
  AND   PLAN_ADJUST journal: debit VARIANCE_RESERVE $5,250, credit PLANNED $5,250

TEST T09-2: "Adjust amount up — same period"
  GIVEN milestone with v1: $20,000 in JAN
  WHEN  createVersion(milestoneId, $25,000, JAN, 2026-03-01, "added testing")
  THEN  v2 created with planned_amount = $25,000
  AND   PLAN_ADJUST journal: debit PLANNED $5,000, credit VARIANCE_RESERVE $5,000

TEST T09-3: "Period shift with amount change"
  GIVEN milestone with v1: $20,000 in JAN
  WHEN  createVersion(milestoneId, $22,000, FEB, 2026-03-01, "shifted to Feb")
  THEN  v2 created with planned_amount = $22,000, period = FEB
  AND   PLAN_ADJUST journal has 4 lines totaling $42,000 debit = $42,000 credit

TEST T09-4: "Version number auto-increments"
  GIVEN milestone with v1
  WHEN  creating two more versions
  THEN  version_numbers are 2 and 3

TEST T09-5: "Effective date must be >= prior version — BR-05"
  GIVEN milestone v1 effective 2026-02-15
  WHEN  createVersion with effectiveDate = 2026-02-01
  THEN  rejected with validation error

TEST T09-6: "Reason is required — BR-42"
  WHEN  createVersion with empty reason
  THEN  rejected with validation error

TEST T09-7: "Net planned balance after multiple adjustments"
  GIVEN v1: $25,000 → v2: $20,000 → v3: $22,000 (all same period)
  WHEN  querying planned balance
  THEN  balance = $22,000

TEST T09-8: "Version history returns all versions ordered"
  GIVEN milestone with 3 versions
  WHEN  GET /api/v1/milestones/{id}/versions
  THEN  returns 3 versions ordered by version_number ascending
```

---

### T10 — Milestone Cancel

**Scope:** Convenience endpoint to cancel a milestone.

**Spec refs:** 04-milestone-versioning.md (Section 4 — Cancelling)

**Tests:**
```
TEST T10-1: "Cancel milestone creates version with amount = 0"
  GIVEN milestone with v1: $25,000
  WHEN  POST /api/v1/milestones/{id}/cancel with reason
  THEN  new version created with planned_amount = 0
  AND   PLAN_ADJUST journal reverses full amount

TEST T10-2: "Cancelled milestone shows $0 in budget queries"
  GIVEN cancelled milestone
  WHEN  querying planned balance
  THEN  returns $0
```

---

## Phase 3: SAP Integration

### T11 — SAP Import — File Upload + Parse

**Scope:** File upload endpoint, CSV/Excel parsing, column mapping.

**Spec refs:** 05-sap-ingestion.md (Steps 1-2)

**Tests:**
```
TEST T11-1: "Upload CSV file and parse lines"
  GIVEN a valid CSV with 10 lines
  WHEN  POST /api/v1/imports/upload
  THEN  returns 201 with import_id and total_lines = 10

TEST T11-2: "Upload Excel file and parse lines"
  GIVEN a valid XLSX with 5 lines
  WHEN  POST /api/v1/imports/upload
  THEN  returns 201 with total_lines = 5

TEST T11-3: "Rows missing posting_date are flagged as errors"
  GIVEN a CSV with 10 lines, 2 missing posting_date
  WHEN  uploading
  THEN  total_lines = 10, error_lines = 2, new_lines = 8

TEST T11-4: "Rows missing amount are flagged as errors"
  GIVEN a CSV with a row where amount is blank
  WHEN  uploading
  THEN  that row is flagged as error

TEST T11-5: "Corrupt file returns error"
  GIVEN a binary file that is not CSV or Excel
  WHEN  POST /api/v1/imports/upload
  THEN  returns 400 with meaningful error message
```

---

### T12 — SAP Import — Dedup + Stage

**Scope:** Hash computation, duplicate detection, staging.

**Spec refs:** 05-sap-ingestion.md (Steps 3-4), 10-business-rules.md (BR-08, BR-09)

**Tests:**
```
TEST T12-1: "First import — all lines are new"
  GIVEN no prior imports
  WHEN  uploading a file with 10 lines
  THEN  new_lines = 10, duplicate_lines = 0

TEST T12-2: "Re-import same file — all lines are duplicates"
  GIVEN the same file was previously imported and committed
  WHEN  uploading the same file again
  THEN  new_lines = 0, duplicate_lines = 10

TEST T12-3: "Partial overlap"
  GIVEN 10 lines previously committed
  WHEN  uploading a file with 15 lines (10 old + 5 new)
  THEN  new_lines = 5, duplicate_lines = 10

TEST T12-4: "Hash normalization — whitespace differences"
  GIVEN a committed line with vendor "Globant S.A."
  WHEN  importing a line with vendor " Globant S.A. " (extra spaces)
  THEN  detected as duplicate (hashes match after normalization)

TEST T12-5: "Hash normalization — case differences"
  GIVEN a committed line with description "Invoice #123"
  WHEN  importing a line with description "INVOICE #123"
  THEN  detected as duplicate

TEST T12-6: "Different amount = different hash = new line"
  GIVEN a committed line with amount $25,000
  WHEN  importing a line identical except amount = $25,001
  THEN  detected as new (not duplicate)
```

---

### T13 — SAP Import — Commit (ACTUAL_IMPORT journal)

**Scope:** Commit staged import, create actual lines and journal entries.

**Spec refs:** 05-sap-ingestion.md (Steps 5-6), 02-journal-ledger.md (5.5), 10-business-rules.md (BR-70-73)

**Tests:**
```
TEST T13-1: "Commit import creates actual lines"
  GIVEN a staged import with 5 new lines
  WHEN  POST /api/v1/imports/{id}/commit
  THEN  5 actual_line records created
  AND   import status = COMMITTED

TEST T13-2: "Each actual line gets a journal entry"
  GIVEN 5 new lines committed
  WHEN  checking journal entries
  THEN  5 ACTUAL_IMPORT journal entries exist
  AND   each has debit ACTUAL, credit VARIANCE_RESERVE

TEST T13-3: "Posting date resolved to fiscal period"
  GIVEN an actual with posting_date 2026-01-15
  WHEN  committed
  THEN  actual_line.fiscal_period_id = FY26 January period

TEST T13-4: "Negative amount (accrual reversal) creates valid journal"
  GIVEN an actual with amount = -$25,000
  WHEN  committed
  THEN  ACTUAL_IMPORT journal entry has debit ACTUAL -$25,000 (effectively a credit)
  AND   entry balances

TEST T13-5: "Reject import discards staged data"
  GIVEN a staged import
  WHEN  POST /api/v1/imports/{id}/reject
  THEN  import status = REJECTED
  AND   no actual_lines created

TEST T13-6: "Cannot commit an already committed import"
  GIVEN a committed import
  WHEN  POST /api/v1/imports/{id}/commit
  THEN  returns 400 (BR-72)

TEST T13-7: "Duplicate lines are stored but marked is_duplicate"
  GIVEN an import with 3 new and 2 duplicate lines
  WHEN  committed
  THEN  3 actual_lines with is_duplicate = false
  AND   2 actual_lines with is_duplicate = true (or not stored — design decision)
```

---

## Phase 4: Reconciliation

### T14 — Reconciliation — Create

**Scope:** Assign an actual to a milestone with category.

**Spec refs:** 06-reconciliation.md (Section 3), 10-business-rules.md (BR-06, BR-07)

**Tests:**
```
TEST T14-1: "Reconcile actual to milestone"
  GIVEN an unreconciled actual and a milestone
  WHEN  POST /api/v1/reconciliation with actualId, milestoneId, category=INVOICE
  THEN  reconciliation created
  AND   RECONCILE journal entry created

TEST T14-2: "Cannot reconcile same actual twice — BR-06"
  GIVEN an actual already reconciled
  WHEN  POST /api/v1/reconciliation with same actualId
  THEN  returns 409 Conflict

TEST T14-3: "Category is required — BR-07"
  WHEN  POST /api/v1/reconciliation without category
  THEN  returns 400 Bad Request

TEST T14-4: "Invalid category rejected"
  WHEN  POST with category = "INVALID"
  THEN  returns 400

TEST T14-5: "Cannot reconcile a duplicate actual"
  GIVEN an actual with is_duplicate = true
  WHEN  POST /api/v1/reconciliation
  THEN  returns 400

TEST T14-6: "Candidates endpoint returns sorted candidates"
  GIVEN an unreconciled actual with wbse = "1174905.SU.ES"
  AND   milestones exist for projects with matching and non-matching WBSEs
  WHEN  GET /api/v1/reconciliation/candidates/{actualId}
  THEN  matching WBSE milestones appear first
  AND   each candidate has relevanceScore
```

---

### T15 — Reconciliation — Undo

**Spec refs:** 06-reconciliation.md (Section 3.6), 10-business-rules.md (BR-62)

**Tests:**
```
TEST T15-1: "Undo reconciliation"
  GIVEN a reconciled actual
  WHEN  DELETE /api/v1/reconciliation/{id} with reason
  THEN  reconciliation removed
  AND   actual is now unreconciled
  AND   RECONCILE_UNDO journal entry created

TEST T15-2: "Undo requires reason — BR-62"
  WHEN  DELETE without reason
  THEN  returns 400

TEST T15-3: "After undo, actual can be re-reconciled"
  GIVEN a reconciliation was undone
  WHEN  POST /api/v1/reconciliation with same actualId, different milestoneId
  THEN  succeeds
```

---

### T16 — Reconciliation Status Derivation

**Scope:** Compute reconciliation status per milestone.

**Spec refs:** 06-reconciliation.md (Section 4, 5)

**Tests:**
```
TEST T16-1: "UNMATCHED — no actuals reconciled"
  GIVEN milestone with $25,000 planned and 0 reconciled
  THEN  status = UNMATCHED

TEST T16-2: "PARTIALLY_MATCHED"
  GIVEN milestone with $25,000 planned and $15,000 reconciled
  THEN  status = PARTIALLY_MATCHED, remaining = $10,000

TEST T16-3: "FULLY_RECONCILED — exact match"
  GIVEN $25,000 planned and $25,000 reconciled
  THEN  status = FULLY_RECONCILED

TEST T16-4: "FULLY_RECONCILED — within tolerance (percentage)"
  GIVEN $25,000 planned and $24,600 reconciled (remaining $400 = 1.6%)
  AND   tolerance_percent = 2%
  THEN  status = FULLY_RECONCILED (within tolerance)

TEST T16-5: "FULLY_RECONCILED — within tolerance (absolute)"
  GIVEN $1,000 planned and $960 reconciled (remaining $40)
  AND   tolerance_absolute = $50
  THEN  status = FULLY_RECONCILED (within $50 threshold)

TEST T16-6: "OVER_BUDGET"
  GIVEN $25,000 planned and $27,000 reconciled
  THEN  status = OVER_BUDGET, remaining = -$2,000

TEST T16-7: "Category breakdown"
  GIVEN milestone with $15,000 INVOICE + $10,000 ACCRUAL + -$10,000 ACCRUAL_REVERSAL
  THEN  invoiceTotal = $15,000, accrualNet = $0, totalActual = $15,000
```

---

### T17 — Accrual Lifecycle Tracking

**Spec refs:** 07-accrual-lifecycle.md, 10-business-rules.md (BR-20 through BR-24)

**Tests:**
```
TEST T17-1: "Accrual creates open accrual"
  GIVEN reconciliation with category = ACCRUAL
  THEN  milestone has openAccrualCount = 1

TEST T17-2: "Accrual + reversal closes accrual"
  GIVEN ACCRUAL +$25,000 then ACCRUAL_REVERSAL -$25,000 on same milestone
  THEN  openAccrualCount = 0

TEST T17-3: "Full lifecycle — accrue, reverse, invoice"
  GIVEN ACCRUAL +$25K, ACCRUAL_REVERSAL -$25K, INVOICE +$25K
  THEN  openAccrualCount = 0
  AND   totalActual = $25,000
  AND   status = FULLY_RECONCILED

TEST T17-4: "Extended lifecycle — multiple accrual cycles"
  GIVEN ACCRUAL +$25K, REVERSAL -$25K, ACCRUAL +$25K, REVERSAL -$25K, INVOICE +$25K
  THEN  openAccrualCount = 0
  AND   totalActual = $25,000

TEST T17-5: "Aging — open accrual beyond warning threshold"
  GIVEN ACCRUAL reconciled 70 days ago with no reversal
  AND   accrual_aging_warning_days = 60
  THEN  accrual status = AGING_WARNING

TEST T17-6: "Positive ACCRUAL_REVERSAL produces warning — BR-24"
  WHEN  reconciling with category = ACCRUAL_REVERSAL and amount = +$25,000
  THEN  operation succeeds but response includes warning
```

---

## Phase 5: Time Machine

### T18 — Time Machine — Milestone Version Queries

**Spec refs:** 08-time-machine.md, 04-milestone-versioning.md (Section 5)

**Tests:**
```
TEST T18-1: "Plan as of date returns correct version"
  GIVEN v1 effective 2025-11-01 ($25K), v2 effective 2026-02-15 ($20K)
  WHEN  GET /api/v1/projects/{id}/milestones?asOfDate=2026-01-01
  THEN  milestone shows planned = $25,000 (v1)

TEST T18-2: "Plan as of date after v2"
  WHEN  asOfDate=2026-03-01
  THEN  milestone shows planned = $20,000 (v2)

TEST T18-3: "Milestone doesn't exist before first version"
  WHEN  asOfDate=2025-10-01
  THEN  milestone not in results

TEST T18-4: "Cancelled milestone shows $0 after cancellation date"
  GIVEN v1 $25K effective 2025-11-01, v2 $0 effective 2026-03-01
  WHEN  asOfDate=2026-03-15
  THEN  planned = $0
```

---

### T19 — Time Machine — Actuals + Reconciliation Queries

**Spec refs:** 08-time-machine.md (Section 2)

**Tests:**
```
TEST T19-1: "Actuals filtered by import date"
  GIVEN actuals imported on Mar 1 and Mar 14
  WHEN  asOfDate=2026-03-10
  THEN  only Mar 1 actuals appear

TEST T19-2: "Reconciliations filtered by reconciled_at"
  GIVEN reconciliation made on Mar 5
  WHEN  asOfDate=2026-03-01
  THEN  actual shows as unreconciled

TEST T19-3: "Variance report respects time machine"
  GIVEN plan v1 ($25K), actual imported Mar 1 ($20K), plan v2 effective Mar 10 ($22K)
  WHEN  variance report with asOfDate=2026-03-05
  THEN  planned = $25K (v1), actual = $20K, variance = $5K
```

---

### T20 — Time Machine — Cross-cutting

**Scope:** Ensure all service methods accept and propagate asOfDate.

**Tests:**
```
TEST T20-1: "Contract detail respects asOfDate"
TEST T20-2: "Project milestones respect asOfDate"
TEST T20-3: "Budget report respects asOfDate"
TEST T20-4: "Reconciliation status respects asOfDate"
```

---

## Phase 6: Reporting APIs

### T21 — Budget Plan Report

**Spec refs:** 09-reporting.md (2.1), 13-api-design.md (Section 10)

**Tests:**
```
TEST T21-1: "Budget report groups by project with monthly columns"
  GIVEN contracts with projects and milestones across FY26
  WHEN  GET /reports/budget?fiscalYear=FY26&groupBy=project
  THEN  rows grouped by project, each with monthly period amounts and total

TEST T21-2: "Budget report filters by contract"
TEST T21-3: "Budget report filters by funding source"
TEST T21-4: "Budget report with quarterly period grouping sums 3 months"
TEST T21-5: "Budget report grand total matches sum of all rows"
```

---

### T22 — Variance Report

**Spec refs:** 09-reporting.md (2.3)

**Tests:**
```
TEST T22-1: "Variance = planned - actual per scope and period"
TEST T22-2: "Status reflects UNDER_BUDGET, WITHIN_TOLERANCE, OVER_BUDGET"
TEST T22-3: "Variance report with asOfDate shows historical variance"
TEST T22-4: "Unreconciled actuals do not appear in variance"
```

---

### T23 — Reconciliation Status Report

**Spec refs:** 09-reporting.md (2.4)

**Tests:**
```
TEST T23-1: "Report shows category breakdown per milestone"
TEST T23-2: "Filter by status returns only matching milestones"
TEST T23-3: "Open accrual count is accurate"
```

---

### T24 — Open Accruals Report

**Spec refs:** 09-reporting.md (2.6), 07-accrual-lifecycle.md (Section 6)

**Tests:**
```
TEST T24-1: "Report shows open accruals sorted by age"
TEST T24-2: "Status reflects WARNING and CRITICAL thresholds"
TEST T24-3: "Resolved accruals do not appear"
```

---

## Phase 7: Audit

### T25 — Entity Audit Log

**Spec refs:** 11-change-management.md

**Work items:**
- `AuditLog` entity + repository
- Service integration: contract update, project update → create audit_log entry

**Tests:**
```
TEST T25-1: "Contract update creates audit log with before/after"
TEST T25-2: "Project update creates audit log"
TEST T25-3: "Audit log captures changes as JSON"
```

---

### T26 — Audit Query APIs

**Spec refs:** 11-change-management.md (Section 5), 13-api-design.md (Section 11)

**Tests:**
```
TEST T26-1: "GET /audit/contract/{id} returns journal + audit entries"
TEST T26-2: "GET /audit/milestone/{id} returns versions + reconciliations + journal"
TEST T26-3: "GET /audit/user/{username} returns all activity"
TEST T26-4: "GET /audit/changes with date range filters correctly"
```

---

## Phase 8: Configuration

### T27 — System Config CRUD

**Spec refs:** 10-business-rules.md (BR-30 through BR-32), 13-api-design.md (Section 12)

**Tests:**
```
TEST T27-1: "GET /config returns all config values"
TEST T27-2: "PUT /config/tolerance_percent updates value"
TEST T27-3: "Config change creates audit log entry"
TEST T27-4: "Updated tolerance affects reconciliation status calculation"
```

---

## Phase 9: Frontend

### T28 — React Skeleton + Shell

**Scope:** React project setup, routing, layout shell, navigation.

**Work items:**
- Vite + React + TypeScript setup
- React Router with routes from spec 14
- Layout shell: sidebar nav, top bar, content area
- API client (fetch wrapper with base URL)
- Placeholder pages for all routes

**Done when:** All routes render placeholder content, navigation works.

---

### T29 — Dashboard View

**Scope:** Dashboard with KPIs, contract summary, alerts.

**Work items:**
- Summary cards (total budget, actual, variance)
- Contract summary table with status indicators
- Alerts section (open accruals, over-budget, last import)
- Funding breakdown
- Click-through to contract detail

---

### T30 — Contract + Project + Milestone Views

**Scope:** Contract detail, project detail, milestone detail with version history.

**Work items:**
- Contract detail page with budget vs. actual grid
- Project list within contract
- Project detail with milestone table
- Milestone detail (inline or page) with reconciliation summary, actuals list, version history
- Create/edit forms for contract, project, milestone (drawer or modal)
- New version form with reason field

---

### T31 — SAP Import Flow

**Scope:** Upload, review, commit/reject.

**Work items:**
- File upload (drag-and-drop zone)
- Import review page with summary stats and line table
- New/Duplicate/Error filter tabs
- Commit and Reject buttons
- Import history table

---

### T32 — Reconciliation Workspace

**Scope:** Split-panel reconciliation UI.

**Work items:**
- Left panel: unreconciled actuals list with filters
- Right panel: candidate milestones for selected actual
- Assignment dialog with category radio buttons and notes
- Undo capability on reconciled actuals
- Visual feedback on successful reconciliation (actual moves out of list)

---

### T33 — Report Views

**Scope:** All 4 Tier 1 reports (budget, variance, recon status, open accruals).

**Work items:**
- Budget plan pivot table with expandable rows
- Variance report with colored cells
- Reconciliation status table with status filters
- Open accruals table with aging indicators
- Filter controls per report
- CSV export button

---

### T34 — Journal Viewer

**Scope:** Filterable journal entry list with expandable line detail.

**Work items:**
- Journal entry list with filters (type, date range, contract, user)
- Expandable rows showing debit/credit lines
- Pagination

---

### T35 — Time Machine UI

**Scope:** Global date picker and banner.

**Work items:**
- Date picker in top bar
- Persistent banner when active
- All views re-fetch with asOfDate parameter
- Reset button to return to current state
- Date cannot be in the future

---

### T36 — Settings View

**Scope:** Configuration management page.

**Work items:**
- Display all config values
- Inline edit with save
- Reason required for changes

---

## Summary

| Phase | Tasks | Backend | Frontend | Est. Test Count |
|-------|-------|---------|----------|----------------|
| 1. Foundation | T01–T05 | 5 | 0 | ~22 |
| 2. Financial Engine | T06–T10 | 5 | 0 | ~28 |
| 3. SAP Integration | T11–T13 | 3 | 0 | ~18 |
| 4. Reconciliation | T14–T17 | 4 | 0 | ~21 |
| 5. Time Machine | T18–T20 | 3 | 0 | ~11 |
| 6. Reporting | T21–T24 | 4 | 0 | ~13 |
| 7. Audit | T25–T26 | 2 | 0 | ~7 |
| 8. Config | T27 | 1 | 0 | ~4 |
| 9. Frontend | T28–T36 | 0 | 9 | UI testing TBD |
| **Total** | **36** | **27** | **9** | **~124+ backend** |
