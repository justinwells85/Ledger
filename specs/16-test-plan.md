# Test Plan

> Comprehensive test strategy covering unit, integration, end-to-end, and frontend tests.
> All tests follow strict **Red-Green-Refactor TDD**. All tests reference specific business rules and spec sections.

---

## 1. TDD Protocol

### 1.1 Red-Green-Refactor Cycle

Every test in this plan is implemented using the following cycle:

```
RED    → Write the test. Run it. It MUST fail.
         If it passes, the test isn't testing new behavior — revise the test.

GREEN  → Write the MINIMUM production code to make the failing test pass.
         Do not anticipate future tests. Do not add behavior beyond what
         the single failing test requires.

REFACTOR → With all tests green, improve code structure.
           Extract methods, rename variables, reduce duplication.
           Run tests after refactoring — they must still pass.
```

### 1.2 Execution Rules

1. **One test at a time.** Write one test → red → green → refactor → next test.
2. **Run the full suite after each green.** Catch regressions immediately.
3. **Never skip red.** If a test passes on first run, it is not adding value. Either the behavior already exists (move on) or the test is wrong (fix it).
4. **Tests are first-class code.** They are reviewed, refactored, and maintained with the same rigor as production code.
5. **Each test documents its spec reference.** Use annotations or comments:
   ```java
   // Spec: 02-journal-ledger.md | BR-01
   @Test
   void balancedJournalEntryIsAccepted() { ... }
   ```

### 1.3 TDD Within Each Task

Each task in `15-tier1-tasks.md` lists its test cases. The implementation order within a task is:

```
Task T06 — Journal Ledger Service
│
├── Write U-JRN-01 → RED → implement createEntry() → GREEN → refactor
├── Write U-JRN-02 → RED → add balance validation → GREEN → refactor
├── Write U-JRN-03 → RED → add line count check → GREEN → refactor
├── ...continue through all unit tests for this task...
│
├── Write I-JRN-01 → RED → wire up repository → GREEN → refactor
├── Write I-JRN-02 → RED → verify DB trigger → GREEN → refactor
├── ...continue through all integration tests...
│
└── Run full suite → all green → task complete
```

### 1.4 When to Write Each Test Layer

| Phase | Write These Tests | Why |
|-------|------------------|-----|
| During the task | **Unit + Integration tests** listed for that task | Core TDD — these drive the implementation |
| After each phase | **Relevant E2E tests** that the phase enables | Validate cross-service workflows |
| During frontend tasks | **Frontend component tests** | UI-level TDD |
| After Tier 1 complete | **All remaining E2E tests** | Full acceptance validation |

### 1.5 E2E Test Phase Mapping

| E2E Test | Run After Phase |
|----------|----------------|
| E2E-01 (Budget Setup) | Phase 2 (after T10) |
| E2E-02 (Budget Adjustment) | Phase 2 (after T10) |
| E2E-03 (SAP Import + Reconciliation) | Phase 4 (after T17) |
| E2E-04 (Accrual Lifecycle) | Phase 4 (after T17) |
| E2E-05 (Time Machine) | Phase 5 (after T20) |
| E2E-06 (Variance Report) | Phase 6 (after T24) |
| E2E-07 (Multi-Project Reconciliation) | Phase 4 (after T17) |
| E2E-08 (Reconciliation Undo) | Phase 4 (after T17) |
| E2E-09 (Audit Trail) | Phase 7 (after T26) |
| E2E-10 (Allocation) | Phase 4 (after T17) |
| E2E-11 (Multi-Year) | Phase 2 (after T10) |
| E2E-12 (Funding Source) | Phase 6 (after T24) |
| E2E-13 (Full Lifecycle) | Phase 8 (after T27) — full backend validation |

---

## 2. Test Layers

```
┌─────────────────────────────────────────────────────┐
│  E2E / Acceptance Tests                              │  Full workflow scenarios
│  (API-level, test real HTTP + DB)                    │  ~13 scenarios
├─────────────────────────────────────────────────────┤
│  Integration Tests                                   │  Service + DB interactions
│  (Spring Boot + Testcontainers PostgreSQL)           │  ~60 tests
├─────────────────────────────────────────────────────┤
│  Unit Tests                                          │  Pure logic, no DB
│  (JUnit + AssertJ, mocked dependencies)              │  ~45 tests
├─────────────────────────────────────────────────────┤
│  Frontend Tests                                      │  Component + interaction
│  (React Testing Library + Vitest)                    │  ~40 tests
└─────────────────────────────────────────────────────┘
```

---

## 3. Unit Tests

Pure logic validation. No database, no Spring context. Dependencies mocked.

### 2.1 Journal Balance Validation

| Test ID | Description | Rule |
|---------|-------------|------|
| U-JRN-01 | Balanced entry (2 lines, equal debit/credit) accepted | BR-01 |
| U-JRN-02 | Unbalanced entry (debit != credit) rejected | BR-01 |
| U-JRN-03 | Entry with 0 lines rejected | BR-02 |
| U-JRN-04 | Entry with 1 line rejected | BR-02 |
| U-JRN-05 | Line with both debit > 0 and credit > 0 rejected | Accounting rule |
| U-JRN-06 | Line with debit = 0 and credit = 0 is allowed (informational) | RECONCILE entries |
| U-JRN-07 | 4-line period-shift entry balances | BR-01 |
| U-JRN-08 | Negative debit is rejected (debits must be >= 0) | BR-10 |

### 2.2 Milestone Version Validation

| Test ID | Description | Rule |
|---------|-------------|------|
| U-VER-01 | Version number auto-increments from prior | BR-04 |
| U-VER-02 | Effective date before prior version rejected | BR-05 |
| U-VER-03 | Effective date equal to prior version accepted | BR-05 |
| U-VER-04 | Empty reason rejected | BR-42 |
| U-VER-05 | Planned amount < 0 rejected | BR-10 |
| U-VER-06 | Planned amount = 0 accepted (cancellation) | BR-44 |
| U-VER-07 | Delta computed correctly for increase | Spec 04 |
| U-VER-08 | Delta computed correctly for decrease | Spec 04 |
| U-VER-09 | Period shift detected when old period != new period | Spec 04 |

### 2.3 SAP Hash Computation

| Test ID | Description | Rule |
|---------|-------------|------|
| U-HASH-01 | Identical fields produce identical hash | BR-08 |
| U-HASH-02 | Different amount produces different hash | BR-08 |
| U-HASH-03 | Whitespace differences normalized (same hash) | BR-08 |
| U-HASH-04 | Case differences normalized (same hash) | BR-08 |
| U-HASH-05 | Null fields normalized to empty string | BR-08 |
| U-HASH-06 | Number format normalized (25000.0 = 25000.00) | BR-08 |
| U-HASH-07 | Date format normalized to ISO 8601 | BR-08 |

### 2.4 Tolerance Calculation

| Test ID | Description | Rule |
|---------|-------------|------|
| U-TOL-01 | Within percentage threshold → FULLY_RECONCILED | BR-30 |
| U-TOL-02 | Within absolute threshold → FULLY_RECONCILED | BR-30 |
| U-TOL-03 | Outside both thresholds → not within tolerance | BR-30 |
| U-TOL-04 | Either threshold met is sufficient | BR-30 |
| U-TOL-05 | Zero planned amount — division by zero handled | Edge case |
| U-TOL-06 | Exact match ($0 remaining) → FULLY_RECONCILED | BR-30 |

### 2.5 Reconciliation Status Derivation

| Test ID | Description | Rule |
|---------|-------------|------|
| U-STAT-01 | No actuals → UNMATCHED | Spec 06 |
| U-STAT-02 | Partial actuals → PARTIALLY_MATCHED | Spec 06 |
| U-STAT-03 | Actuals = planned → FULLY_RECONCILED | Spec 06 |
| U-STAT-04 | Actuals > planned → OVER_BUDGET | Spec 06 |
| U-STAT-05 | Actuals within tolerance → FULLY_RECONCILED | BR-30 |
| U-STAT-06 | Category breakdown sums correctly | Spec 06 |

### 2.6 Fiscal Period Resolution

| Test ID | Description | Rule |
|---------|-------------|------|
| U-FP-01 | January date → FY Q2 | Spec 03 |
| U-FP-02 | October date → correct FY (next year's FY) | Spec 03 |
| U-FP-03 | September date → FY Q4 | Spec 03 |
| U-FP-04 | Date outside any fiscal year → null/error | Spec 03 |

### 2.7 Accrual Status Derivation

| Test ID | Description | Rule |
|---------|-------------|------|
| U-ACC-01 | 1 accrual, 0 reversals → openCount = 1 | BR-20 |
| U-ACC-02 | 1 accrual, 1 reversal → openCount = 0 | BR-20 |
| U-ACC-03 | 2 accruals, 1 reversal → openCount = 1 | BR-20 |
| U-ACC-04 | Net accrual balance computed correctly | BR-23 |
| U-ACC-05 | Age computed from accrual reconciled_at | Spec 07 |
| U-ACC-06 | Age > warning threshold → AGING_WARNING | Spec 07 |
| U-ACC-07 | Age > critical threshold → AGING_CRITICAL | Spec 07 |

---

## 4. Integration Tests

Test service + repository + database interactions using Testcontainers.

### 4.1 Contract + Project CRUD

| Test ID | Description | Rule |
|---------|-------------|------|
| I-CON-01 | Create contract persists all fields | Spec 01 |
| I-CON-02 | Duplicate contract name rejected | Unique constraint |
| I-CON-03 | Update contract creates audit log entry | BR-60 |
| I-CON-04 | Update without reason rejected | BR-61 |
| I-CON-05 | List contracts filters by status | Spec 13 |
| I-PRJ-01 | Create project under contract | Spec 01 |
| I-PRJ-02 | Duplicate project ID rejected | BR-12 |
| I-PRJ-03 | List projects filters by funding source | Spec 13 |

### 4.2 Journal Ledger Persistence

| Test ID | Description | Rule |
|---------|-------------|------|
| I-JRN-01 | Balanced entry persists with all lines | BR-01 |
| I-JRN-02 | DB trigger rejects unbalanced entry at commit | BR-01 |
| I-JRN-03 | Planned balance query returns correct sum | Spec 02 |
| I-JRN-04 | Balance query with asOfDate filters correctly | Spec 02, 08 |
| I-JRN-05 | Multiple entries aggregate correctly | Spec 02 |

### 4.3 Milestone Lifecycle

| Test ID | Description | Rule |
|---------|-------------|------|
| I-MIL-01 | Create milestone produces v1 + journal entry | Spec 04 |
| I-MIL-02 | Adjust milestone produces v2 + journal entry with delta | Spec 04 |
| I-MIL-03 | Period shift produces 4-line journal entry | Spec 04 |
| I-MIL-04 | Cancel produces version with amount 0 | BR-44 |
| I-MIL-05 | Current version query returns highest version_number | BR-40 |
| I-MIL-06 | As-of-date version query returns correct version | BR-41 |
| I-MIL-07 | Version history returns all versions ordered | Spec 04 |
| I-MIL-08 | Net planned balance after create + adjust + adjust | Spec 02 |

### 4.4 SAP Import Pipeline

| Test ID | Description | Rule |
|---------|-------------|------|
| I-SAP-01 | Upload + parse CSV creates staged import | Spec 05 |
| I-SAP-02 | Upload + parse Excel creates staged import | Spec 05 |
| I-SAP-03 | First import: all lines are new | BR-08 |
| I-SAP-04 | Re-import: all lines are duplicates | BR-08 |
| I-SAP-05 | Partial overlap: correct new/dup counts | BR-08 |
| I-SAP-06 | Commit creates actual_line + journal entries | Spec 05 |
| I-SAP-07 | Commit resolves posting date to fiscal period | BR-73 |
| I-SAP-08 | Reject sets status without creating actuals | Spec 05 |
| I-SAP-09 | Cannot commit already committed import | BR-72 |
| I-SAP-10 | Duplicate lines stored with is_duplicate = true | BR-09 |

### 4.5 Reconciliation

| Test ID | Description | Rule |
|---------|-------------|------|
| I-REC-01 | Reconcile actual to milestone creates link + journal | Spec 06 |
| I-REC-02 | Cannot reconcile same actual twice | BR-06 |
| I-REC-03 | Cannot reconcile duplicate actual | BR-09 |
| I-REC-04 | Undo removes link + creates journal entry | Spec 06 |
| I-REC-05 | After undo, actual can be re-reconciled | Spec 06 |
| I-REC-06 | Reconciliation status derived correctly per milestone | Spec 06 |
| I-REC-07 | Tolerance thresholds applied from system_config | BR-30 |
| I-REC-08 | Candidate milestones sorted by relevance | Spec 06 |

### 4.6 Accrual Tracking

| Test ID | Description | Rule |
|---------|-------------|------|
| I-ACC-01 | Accrual category creates open accrual | BR-20 |
| I-ACC-02 | Reversal closes open accrual | BR-20 |
| I-ACC-03 | Open accrual aging computed correctly | Spec 07 |
| I-ACC-04 | Open accruals report returns correct data | Spec 07 |

### 4.7 Time Machine (Cross-Cutting)

| Test ID | Description | Rule |
|---------|-------------|------|
| I-TM-01 | Milestone query with asOfDate returns correct version | BR-41 |
| I-TM-02 | Actuals filtered by created_at <= asOfDate | Spec 08 |
| I-TM-03 | Reconciliations filtered by reconciled_at <= asOfDate | Spec 08 |
| I-TM-04 | Milestone not visible before first version effective_date | Spec 08 |
| I-TM-05 | Budget report respects asOfDate | BR-52 |
| I-TM-06 | Variance report respects asOfDate | BR-52 |
| I-TM-07 | Future asOfDate rejected | BR-53 |

### 4.8 Reports

| Test ID | Description | Rule |
|---------|-------------|------|
| I-RPT-01 | Budget report aggregates by contract + period | Spec 09 |
| I-RPT-02 | Budget report filters by funding source | Spec 09 |
| I-RPT-03 | Variance report: planned - actual = variance | BR-51 |
| I-RPT-04 | Variance report status reflects over/under/tolerance | Spec 09 |
| I-RPT-05 | Reconciliation status report shows category breakdown | Spec 09 |
| I-RPT-06 | Open accruals report sorted by age | Spec 07 |
| I-RPT-07 | Grand total matches sum of all rows | Spec 09 |

### 4.9 Audit

| Test ID | Description | Rule |
|---------|-------------|------|
| I-AUD-01 | Contract audit trail includes journal + audit_log | Spec 11 |
| I-AUD-02 | Milestone audit trail includes versions + reconciliations | Spec 11 |
| I-AUD-03 | User activity query returns all events | Spec 11 |
| I-AUD-04 | Date range query filters correctly | Spec 11 |

### 4.10 Configuration

| Test ID | Description | Rule |
|---------|-------------|------|
| I-CFG-01 | Get all config returns defaults | Spec 10 |
| I-CFG-02 | Update config persists new value | Spec 10 |
| I-CFG-03 | Config change creates audit log | BR-60 |
| I-CFG-04 | Updated tolerance reflected in recon status | BR-30 |

---

## 5. End-to-End / Acceptance Tests

Full workflow scenarios running against the real API with a test database. These validate that the system works as a whole, not just individual services.

### E2E-01: Complete Budget Setup

```
SCENARIO: Set up a contract with projects and milestones

GIVEN the system has FY26 fiscal calendar

WHEN  I create contract "Globant ADM" (vendor: Globant, owner: Rob Moore)
AND   I create project PR13752 (wbse: 1174905.SU.ES, funding: OPEX) under the contract
AND   I create project PR01570 (wbse: 1235664.IT.SU.IT, funding: OPEX) under the contract
AND   I create 12 milestones for PR13752 (one per fiscal month, $109,947 each)
AND   I create 12 milestones for PR01570 (one per fiscal month, $25,250 each)

THEN  GET /contracts/{id} shows totalPlanned = $1,622,364
AND   GET /reports/budget?fiscalYear=FY26&contractId={id} shows 24 milestone rows
AND   each month column sums to $135,197
AND   grand total = $1,622,364
AND   journal has 24 PLAN_CREATE entries, all balanced
```

### E2E-02: Budget Adjustment Mid-Year

```
SCENARIO: Adjust milestone amounts and shift a payment period

GIVEN contract "Globant ADM" with milestone "Jan Sustainment" at $109,947 in January

WHEN  I create a new version reducing it to $67,147 (reason: "$42,800 reclass to PR18116")
      with effective_date = 2026-01-10

THEN  GET /milestones/{id} shows currentVersion.plannedAmount = $67,147
AND   GET /milestones/{id}/versions shows 2 versions
AND   journal has PLAN_ADJUST entry: debit VARIANCE_RESERVE $42,800, credit PLANNED $42,800
AND   GET /reports/budget shows January column for PR13752 = $67,147

WHEN  I then create a new milestone for PR18116 "MDX App Gallery Refresh" at $42,800 in January
THEN  the total planned for "Globant ADM" in January remains $175,824 (budget reclass is neutral)
```

### E2E-03: SAP Import and Reconciliation

```
SCENARIO: Import SAP actuals and reconcile them to milestones

GIVEN milestones exist for Globant ADM

WHEN  I upload a SAP CSV with 20 lines covering October–December actuals
THEN  import shows status=STAGED, newLines=20, duplicateLines=0

WHEN  I commit the import
THEN  20 actual_line records exist with is_duplicate=false
AND   20 ACTUAL_IMPORT journal entries exist, all balanced
AND   GET /reconciliation/unreconciled returns 20 lines

WHEN  I reconcile actual ($112,129, Oct, Globant) to "Oct Sustainment" as INVOICE
THEN  reconciliation created
AND   GET /reconciliation/unreconciled returns 19 lines
AND   milestone "Oct Sustainment" shows totalActual = $112,129, status = WITHIN_TOLERANCE

WHEN  I re-upload the same SAP CSV
THEN  import shows newLines=0, duplicateLines=20

WHEN  I upload an expanded CSV with 25 lines (original 20 + 5 new January lines)
THEN  import shows newLines=5, duplicateLines=20
AND   after commit, total actual_lines (active) = 25
```

### E2E-04: Full Accrual Lifecycle

```
SCENARIO: Accrual → reversal → re-accrual → reversal → invoice

GIVEN milestone "Feb Sustainment" with planned $25,250

STEP 1: Month-end accrual
WHEN  I import a SAP line: +$25,000, posting_date=2026-02-28, desc="Globant accrual"
AND   I commit the import
AND   I reconcile it to "Feb Sustainment" as ACCRUAL
THEN  milestone shows totalActual=$25,000, openAccrualCount=1, status=PARTIALLY_MATCHED

STEP 2: Next month reversal + re-accrual
WHEN  I import two SAP lines:
      -$25,000 posting_date=2026-03-01 desc="Accrual reversal"
      +$25,000 posting_date=2026-03-01 desc="Globant accrual"
AND   I commit and reconcile the reversal as ACCRUAL_REVERSAL to "Feb Sustainment"
AND   I reconcile the new accrual as ACCRUAL to "Feb Sustainment"
THEN  milestone shows totalActual=$25,000 (net), openAccrualCount=1
AND   accrualNet=$0 (old pair cancelled), but new accrual is open

STEP 3: Invoice arrives
WHEN  I import two SAP lines:
      -$25,000 posting_date=2026-04-01 desc="Accrual reversal"
      +$25,250 posting_date=2026-04-01 desc="Globant Invoice #456"
AND   I commit and reconcile reversal as ACCRUAL_REVERSAL
AND   I reconcile invoice as INVOICE
THEN  milestone shows totalActual=$25,250, openAccrualCount=0
AND   status=FULLY_RECONCILED
AND   invoiceTotal=$25,250, accrualNet=$0
AND   open accruals report does NOT include this milestone
```

### E2E-05: Time Machine

```
SCENARIO: View system state at different points in time

GIVEN:
  - 2025-11-01: Milestone "Jan Sustainment" created at $109,947
  - 2026-01-10: Milestone adjusted to $67,147 (reclass)
  - 2026-02-01: SAP import with January actuals ($72,000)
  - 2026-02-05: Actual reconciled to "Jan Sustainment" as INVOICE
  - 2026-03-01: Milestone adjusted to $70,000 (budget increase)

TIME MACHINE: 2025-12-01
THEN  milestone planned = $109,947 (v1)
AND   no actuals exist
AND   variance = $109,947 (fully under budget — nothing spent yet)

TIME MACHINE: 2026-01-15
THEN  milestone planned = $67,147 (v2 — reclass happened on Jan 10)
AND   no actuals exist (imported on Feb 1)
AND   variance = $67,147

TIME MACHINE: 2026-02-03
THEN  milestone planned = $67,147 (v2)
AND   actual = $72,000 (imported Feb 1)
AND   actual shows as UNRECONCILED (reconciled on Feb 5)
AND   variance report shows no reconciled data for this milestone

TIME MACHINE: 2026-02-10
THEN  milestone planned = $67,147 (v2)
AND   actual = $72,000 reconciled as INVOICE
AND   variance = -$4,853 (OVER_BUDGET)

TIME MACHINE: 2026-03-15
THEN  milestone planned = $70,000 (v3)
AND   actual = $72,000
AND   variance = -$2,000 (still over, but less)

CURRENT (no time machine)
THEN  same as 2026-03-15
```

### E2E-06: Variance Report Accuracy

```
SCENARIO: Variance report matches expected values across contracts

GIVEN:
  - Contract A: 3 milestones, total planned $100,000
  - Contract B: 2 milestones, total planned $50,000
  - SAP actuals imported and reconciled:
    Contract A: $95,000 (under budget)
    Contract B: $55,000 (over budget)

WHEN  GET /reports/variance?fiscalYear=FY26

THEN  Contract A: planned=$100K, actual=$95K, variance=+$5K, status=UNDER_BUDGET
AND   Contract B: planned=$50K, actual=$55K, variance=-$5K, status=OVER_BUDGET
AND   grand total: planned=$150K, actual=$150K, variance=$0
```

### E2E-07: Multi-Project Reconciliation

```
SCENARIO: Same vendor across multiple contracts — actuals reconciled to different milestones

GIVEN:
  - Contract "Globant ADM" with milestone A ($25,000, January)
  - Contract "Globant QA" with milestone B ($12,000, January)
  - SAP import with lines from "Globant" for January

WHEN  I reconcile $25,000 line to milestone A as INVOICE
AND   I reconcile $12,000 line to milestone B as INVOICE

THEN  milestone A: totalActual=$25K, FULLY_RECONCILED
AND   milestone B: totalActual=$12K, FULLY_RECONCILED
AND   variance report shows both contracts on budget
AND   journal has separate RECONCILE entries for each
```

### E2E-08: Reconciliation Undo and Re-reconcile

```
SCENARIO: Reconcile to wrong milestone, undo, reconcile to correct one

GIVEN:
  - Milestone A "Jan Sustainment" ($25,000)
  - Milestone B "Feb Sustainment" ($25,000)
  - Actual line: $25,000, posting Jan 31

WHEN  I reconcile the actual to milestone B as INVOICE (mistake)
THEN  milestone B shows totalActual=$25,000
AND   milestone A shows totalActual=$0

WHEN  I undo the reconciliation (reason: "Wrong milestone")
THEN  actual is back in unreconciled pool
AND   milestone B shows totalActual=$0
AND   journal has RECONCILE_UNDO entry

WHEN  I reconcile the actual to milestone A as INVOICE (correct)
THEN  milestone A shows totalActual=$25,000, FULLY_RECONCILED
AND   full audit trail shows: reconcile to B → undo → reconcile to A
```

### E2E-09: Change Management Audit Trail

```
SCENARIO: Full audit trail for a contract over time

GIVEN:
  - Contract created by Brad on Nov 1
  - Project added by Brad on Nov 1
  - Milestone created by Brad on Nov 1
  - Milestone adjusted by Justin on Feb 15
  - SAP import by Justin on Mar 1
  - Reconciliation by Justin on Mar 5

WHEN  GET /audit/contract/{id}
THEN  returns all events in chronological order
AND   each event shows who, when, what changed

WHEN  GET /audit/milestone/{id}
THEN  shows version history + reconciliation history + journal entries

WHEN  GET /audit/user/Justin Anderson?dateFrom=2026-03-01&dateTo=2026-03-31
THEN  shows the SAP import and reconciliation events
AND   does NOT show Brad's November events
```

### E2E-10: Allocation Category

```
SCENARIO: Allocations from other departments

GIVEN milestone "License Allocation Q2" with planned $15,000

WHEN  I import a SAP line: $12,500, desc="IT License chargeback Q2"
AND   I reconcile it as ALLOCATION
THEN  milestone shows totalActual=$12,500
AND   allocationTotal=$12,500
AND   invoiceTotal=$0, accrualNet=$0
AND   remaining=$2,500, status=PARTIALLY_MATCHED
```

### E2E-11: Multiple Fiscal Years

```
SCENARIO: Plan spans FY26 and FY27

GIVEN FY26 and FY27 fiscal calendars exist

WHEN  I create a contract with start_date=2025-10-01, end_date=2027-09-30
AND   I create milestones in FY26 periods AND FY27 periods

THEN  GET /reports/budget?fiscalYear=FY26 shows only FY26 milestones
AND   GET /reports/budget?fiscalYear=FY27 shows only FY27 milestones
AND   contract detail shows total planned across both years
```

### E2E-12: Funding Source Reporting

```
SCENARIO: Costs broken down by OPEX vs CAPEX

GIVEN:
  - Project A (OPEX) with $100,000 planned
  - Project B (CAPEX) with $50,000 planned
  - Actuals reconciled: A=$90K, B=$45K

WHEN  GET /reports/funding-summary?fiscalYear=FY26
THEN  OPEX: planned=$100K, actual=$90K, variance=+$10K
AND   CAPEX: planned=$50K, actual=$45K, variance=+$5K
```

### E2E-13: Zero-to-Complete Contract Lifecycle

```
SCENARIO: A contract from creation through full fiscal year

GIVEN empty system with FY26 calendar

STEP 1: Setup
  Create contract "Test Vendor" with owner Justin
  Create project PR99999 (OPEX)
  Create 4 quarterly milestones at $10,000 each ($40,000 total)
  → Budget report shows $40,000 planned

STEP 2: Q1 actuals
  Import SAP with Q1 invoice ($10,000)
  Reconcile as INVOICE to Q1 milestone
  → Variance report: $10K actual, $30K remaining

STEP 3: Q2 budget adjustment
  Increase Q2 milestone from $10,000 to $15,000 (reason: "scope addition")
  → Total planned = $45,000
  → Budget report January shows $15,000

STEP 4: Q2 accrual cycle
  Import accrual (+$15,000)
  Reconcile as ACCRUAL
  → Open accruals report shows 1 open
  Import reversal (-$15,000) + invoice (+$14,800)
  Reconcile reversal as ACCRUAL_REVERSAL
  Reconcile invoice as INVOICE
  → Status: WITHIN_TOLERANCE (remaining $200 < $50 absolute? No. $200/$15K = 1.3% < 2%? Yes)

STEP 5: Q3 cancellation
  Cancel Q3 milestone (reason: "project descoped")
  → Total planned = $35,000

STEP 6: Time machine
  View as of Step 1 completion → planned $40,000, no actuals
  View as of Step 3 completion → planned $45,000, $10K actual
  View current → planned $35,000, $24,800 actual

STEP 7: Audit
  Full audit trail shows every action in order
```

---

## 6. Frontend Tests

### 6.1 Test Framework

- **Vitest** — test runner (Vite-native, fast)
- **React Testing Library** — component rendering and interaction
- **MSW (Mock Service Worker)** — API mocking for component tests

### 6.2 Component Tests

#### Dashboard

| Test ID | Description |
|---------|-------------|
| FE-DASH-01 | Renders summary cards with correct totals from API |
| FE-DASH-02 | Contract summary table renders all contracts |
| FE-DASH-03 | Clicking a contract row navigates to contract detail |
| FE-DASH-04 | Alerts section shows open accruals count |
| FE-DASH-05 | Fiscal year selector triggers data reload |

#### Contract + Project + Milestone

| Test ID | Description |
|---------|-------------|
| FE-CON-01 | Contract detail renders header with correct info |
| FE-CON-02 | Budget vs actual grid renders monthly columns |
| FE-CON-03 | Variance cells colored correctly (green/red/neutral) |
| FE-CON-04 | Project list renders with correct counts |
| FE-PRJ-01 | Project detail renders milestone table |
| FE-PRJ-02 | Milestone status indicators display correctly |
| FE-MIL-01 | Milestone detail shows reconciliation summary |
| FE-MIL-02 | Version history table renders all versions |
| FE-MIL-03 | "New Version" form validates required fields |
| FE-MIL-04 | "New Version" form requires reason |
| FE-MIL-05 | Create milestone form submits correctly |

#### SAP Import

| Test ID | Description |
|---------|-------------|
| FE-IMP-01 | File upload zone accepts CSV and XLSX |
| FE-IMP-02 | File upload zone rejects invalid file types |
| FE-IMP-03 | Review page shows new/duplicate/error counts |
| FE-IMP-04 | Filter tabs switch between new/duplicate/error lines |
| FE-IMP-05 | Commit button calls API and shows success |
| FE-IMP-06 | Reject button calls API and returns to upload |
| FE-IMP-07 | Import history table renders past imports |

#### Reconciliation Workspace

| Test ID | Description |
|---------|-------------|
| FE-REC-01 | Left panel renders unreconciled actuals |
| FE-REC-02 | Selecting an actual populates right panel with candidates |
| FE-REC-03 | Candidates sorted by relevance score |
| FE-REC-04 | Assignment dialog renders with category radio buttons |
| FE-REC-05 | Submitting assignment removes actual from unreconciled list |
| FE-REC-06 | Undo button prompts for reason |
| FE-REC-07 | Filters on left panel update the actual list |

#### Reports

| Test ID | Description |
|---------|-------------|
| FE-RPT-01 | Budget report renders pivot table with expandable rows |
| FE-RPT-02 | Variance report cells have correct color coding |
| FE-RPT-03 | Reconciliation status report filters by status |
| FE-RPT-04 | Open accruals report shows aging indicators |
| FE-RPT-05 | CSV export button triggers download |

#### Journal Viewer

| Test ID | Description |
|---------|-------------|
| FE-JRN-01 | Journal entries render in table |
| FE-JRN-02 | Expanding a row shows debit/credit lines |
| FE-JRN-03 | Filters update the entry list |

#### Time Machine

| Test ID | Description |
|---------|-------------|
| FE-TM-01 | Date picker appears in top bar |
| FE-TM-02 | Selecting a date shows the time machine banner |
| FE-TM-03 | Resetting dismisses the banner |
| FE-TM-04 | Future dates are disabled in the picker |
| FE-TM-05 | All API calls include asOfDate when time machine is active |

#### Settings

| Test ID | Description |
|---------|-------------|
| FE-SET-01 | Config values render with current values |
| FE-SET-02 | Editing a value and saving calls PUT API |

---

## 7. Test Count Summary

| Layer | Count | Coverage Focus |
|-------|-------|----------------|
| Unit tests | ~45 | Business logic, validation, computation |
| Integration tests | ~60 | Service + DB, data integrity, queries |
| E2E / Acceptance tests (Java) | ~13 scenarios (~50 assertions) | Complete workflows, cross-service |
| Playwright Tier 1 (mocked) | ~134 tests | UI structure, form interactions, navigation, visual state |
| Playwright Tier 2 (real backend) | 🔲 To be implemented | Data persistence visible across all UI locations |
| Frontend component tests | ~40 | Component rendering, interaction, API integration |
| **Total** | **~158 + 134 + 40 tests** | |

---

## 10. Playwright UI Testing Strategy

> Detailed persona-scenario matrix with per-scenario action steps, UI validations, and test file locations lives in **`20-e2e-scenario-matrix.md`**. This section defines the two-tier structure and how it integrates with the overall test plan.

### 10.1 Two-Tier Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  Tier 1: Playwright + Mocked APIs                                 │
│  Location: frontend/e2e/*.spec.ts                                 │
│  Speed: < 2 minutes                                               │
│  Run on: Every PR / every local test run                          │
│  Validates: UI structure, form interactions, navigation, visual   │
│             state changes after mutations (stateful mocks)        │
│  Limitation: Cannot catch backend persistence bugs               │
├──────────────────────────────────────────────────────────────────┤
│  Tier 2: Playwright + Real Docker Backend                         │
│  Location: frontend/e2e/tier2/*.spec.ts                           │
│  Speed: 5–15 minutes                                              │
│  Run on: Merge to main, nightly                                   │
│  Validates: Data written in UI actually persists and appears in   │
│             ALL displayed locations (full round-trip)             │
│  Requires: docker compose up with clean test database             │
└──────────────────────────────────────────────────────────────────┘
```

### 10.2 Tier 1 Requirements

**Stateful mocks for write operations:** After a POST/PUT mutation, the mock for the subsequent GET must return updated data. This is the primary mechanism for validating post-action state in Tier 1.

```typescript
// Example: stateful mock pattern (required for all write-flow tests)
let posted = false;
await page.route('**/api/v1/contracts/*/projects', r => {
  if (r.request().method() === 'POST') {
    posted = true;
    return r.fulfill({ status: 201, ... });
  }
  return r.fulfill({ status: 200, body: JSON.stringify(posted ? updatedList : originalList) });
});
// Then assert the new item is visible:
await expect(page.getByText('New Project Name')).toBeVisible();
```

**Assertion completeness:** Every write-flow test must assert the post-action state, not just that the form closed. The test is not complete until it verifies the new/updated data is visible in the UI.

### 10.3 Tier 2 Requirements

**Infrastructure:**
- Docker Compose stack must be running: `docker compose up`
- Database seeded to a known state before each test run
- Playwright `baseURL`: `http://localhost:80`
- Auth: login via real `POST /api/v1/auth/login` (not `addInitScript`)
- Test isolation: use unique name prefixes per test run (e.g. timestamp prefix) to avoid conflicts

**Run commands:**
```bash
npm run test:e2e         # Tier 1 only (default)
npm run test:e2e:tier2   # Tier 2 only (requires Docker)
npm run test:e2e:all     # Both tiers
```

**Validation pattern:** Each Tier 2 scenario navigates to ALL pages where the created/modified data should appear. For example, creating a contract must be verified on:
1. The contract detail page (navigated to immediately after creation)
2. The dashboard CONTRACT SUMMARY table (navigate back to `/`)

### 10.4 Known Test Coverage Gaps (Tier 2)

All Tier 2 tests are currently `🔲 Not yet implemented`. See the coverage matrix in `20-e2e-scenario-matrix.md` Section 6 for the full list.

Priority order for Tier 2 implementation (highest risk of mock-hidden bugs):
1. P1-S1 Create contract → appears on dashboard (blocked by dashboard bug fix)
2. P1-S2 Add project → appears on fresh contract detail reload
3. P1-S3 Create milestone → appears on fresh project detail reload
4. P2-S4 Commit import → actual lines appear in reconciliation workspace
5. P3-S3 Reconcile actual → actual removed from unreconciled list after fresh load

### 10.5 Relationship to Java E2E Tests

The Java E2E tests (Section 5, E2E-01 through E2E-13) test at the API level — they validate that the correct data is returned from HTTP endpoints, without a browser. The Playwright Tier 2 tests complement this by validating the full browser-to-database round-trip: that data entered via the UI appears correctly in the DOM after page reloads.

---

## 8. Test Organization (Java Package Structure)

```
src/test/java/com/ledger/
├── unit/
│   ├── journal/
│   │   └── JournalValidationTest.java          (U-JRN-*)
│   ├── milestone/
│   │   └── MilestoneVersionValidationTest.java  (U-VER-*)
│   ├── sap/
│   │   └── LineHashComputationTest.java         (U-HASH-*)
│   ├── reconciliation/
│   │   ├── ToleranceCalculationTest.java        (U-TOL-*)
│   │   ├── ReconciliationStatusTest.java        (U-STAT-*)
│   │   └── AccrualStatusTest.java               (U-ACC-*)
│   └── fiscal/
│       └── FiscalPeriodResolutionTest.java       (U-FP-*)
├── integration/
│   ├── ContractIntegrationTest.java              (I-CON-*)
│   ├── ProjectIntegrationTest.java               (I-PRJ-*)
│   ├── JournalIntegrationTest.java               (I-JRN-*)
│   ├── MilestoneIntegrationTest.java             (I-MIL-*)
│   ├── SapImportIntegrationTest.java             (I-SAP-*)
│   ├── ReconciliationIntegrationTest.java        (I-REC-*)
│   ├── AccrualIntegrationTest.java               (I-ACC-*)
│   ├── TimeMachineIntegrationTest.java           (I-TM-*)
│   ├── ReportIntegrationTest.java                (I-RPT-*)
│   ├── AuditIntegrationTest.java                 (I-AUD-*)
│   └── ConfigIntegrationTest.java                (I-CFG-*)
├── e2e/
│   ├── BudgetSetupE2ETest.java                   (E2E-01)
│   ├── BudgetAdjustmentE2ETest.java              (E2E-02)
│   ├── SapImportReconciliationE2ETest.java       (E2E-03)
│   ├── AccrualLifecycleE2ETest.java              (E2E-04)
│   ├── TimeMachineE2ETest.java                   (E2E-05)
│   ├── VarianceReportE2ETest.java                (E2E-06)
│   ├── MultiProjectReconciliationE2ETest.java    (E2E-07)
│   ├── ReconciliationUndoE2ETest.java            (E2E-08)
│   ├── AuditTrailE2ETest.java                    (E2E-09)
│   ├── AllocationE2ETest.java                    (E2E-10)
│   ├── MultiFiscalYearE2ETest.java               (E2E-11)
│   ├── FundingSourceE2ETest.java                 (E2E-12)
│   └── FullLifecycleE2ETest.java                 (E2E-13)
└── BaseIntegrationTest.java                       (Testcontainers setup)
```

---

## 9. Test-to-Business-Rule Traceability

| Business Rule | Unit Tests | Integration Tests | E2E Tests |
|---------------|-----------|-------------------|-----------|
| BR-01 (journal balance) | U-JRN-01 to 08 | I-JRN-01, 02 | E2E-01, 03, 04 |
| BR-02 (min 2 lines) | U-JRN-03, 04 | — | — |
| BR-04 (version sequential) | U-VER-01 | I-MIL-02 | E2E-02 |
| BR-05 (effective_date order) | U-VER-02, 03 | — | — |
| BR-06 (1:1 reconciliation) | — | I-REC-02 | E2E-08 |
| BR-07 (category required) | — | I-REC-01 | E2E-03, 04 |
| BR-08 (dedup hash) | U-HASH-01 to 07 | I-SAP-03 to 05 | E2E-03 |
| BR-10 (amounts >= 0) | U-VER-05, U-JRN-08 | — | — |
| BR-20 (open accrual tracking) | U-ACC-01 to 03 | I-ACC-01, 02 | E2E-04 |
| BR-24 (positive reversal warning) | U-ACC-05 (note: should be a separate test) | — | — |
| BR-30 (tolerance) | U-TOL-01 to 06 | I-REC-07 | E2E-13 |
| BR-40 (current version) | — | I-MIL-05 | — |
| BR-41 (version as of date) | — | I-MIL-06, I-TM-01 | E2E-05 |
| BR-42 (reason required) | U-VER-04 | — | — |
| BR-44 (cancel = $0 version) | U-VER-06 | I-MIL-04 | E2E-13 |
| BR-51 (variance = planned - actual) | — | I-RPT-03 | E2E-06 |
| BR-52 (reports respect asOfDate) | — | I-TM-05, 06 | E2E-05 |
| BR-53 (no future asOfDate) | — | I-TM-07 | — |
| BR-60 (mutations audited) | — | I-AUD-01 to 04 | E2E-09 |
| BR-62 (undo requires reason) | — | I-REC-04 | E2E-08 |
| BR-72 (no un-commit) | — | I-SAP-09 | — |
| BR-73 (posting date → period) | U-FP-01 to 04 | I-SAP-07 | E2E-03 |
