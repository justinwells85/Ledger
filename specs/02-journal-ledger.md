# Double-Entry Journal Ledger

> Source: DPI Accruals v2 Spec, Section 3

---

## 1. Principles

1. **Every financial event** produces a journal entry with two or more lines
2. **Every journal entry must balance**: total debits = total credits
3. **Nothing is ever deleted or mutated** — corrections are new entries that offset prior ones
4. **All reporting views are derived** from journal queries — there is no separate "current state" table that gets updated
5. **The journal is the source of truth** for all financial data in the system

---

## 2. Accounts

Three accounts form the chart of accounts:

| Account | Code | Meaning | Normal Balance |
|---------|------|---------|----------------|
| Planned | `PLANNED` | Total committed/forecasted spend | Debit |
| Actual | `ACTUAL` | Total spend recorded from SAP | Debit |
| Variance Reserve | `VARIANCE_RESERVE` | Balancing account — gap between planned and actual | Credit |

**Derived views (not accounts):**
- **Variance** = PLANNED balance - ACTUAL balance for a given scope
- **Reconciled** = ACTUAL entries that have a reconciliation link
- **Unreconciled Actuals** = ACTUAL entries without a reconciliation link
- **Unmatched Plan** = Milestones without fully reconciled actuals

---

## 3. Journal Entry Structure

### Journal Entry (Header)

| Field | Type | Constraints |
|-------|------|-------------|
| `entry_id` | UUID (PK) | Auto-generated |
| `entry_date` | Timestamp | When the event occurred |
| `effective_date` | Date | When it takes effect for reporting / time machine |
| `entry_type` | Enum | See Section 4 |
| `description` | String | Required, human-readable summary |
| `created_at` | Timestamp | Auto-set |
| `created_by` | String | System user |

### Journal Line (Detail)

| Field | Type | Constraints |
|-------|------|-------------|
| `line_id` | UUID (PK) | Auto-generated |
| `entry_id` | UUID (FK → Journal Entry) | Required |
| `account` | Enum | PLANNED, ACTUAL, VARIANCE_RESERVE |
| `contract_id` | UUID (FK → Contract) | Nullable (unassigned actuals at import) |
| `project_id` | String (FK → Project) | Nullable |
| `milestone_id` | UUID (FK → Milestone) | Nullable |
| `fiscal_period_id` | UUID (FK → Fiscal Period) | Required |
| `debit` | Decimal(15,2) | >= 0, one of debit/credit must be 0 |
| `credit` | Decimal(15,2) | >= 0, one of debit/credit must be 0 |
| `reference_type` | String | What triggered this line (e.g., "MilestoneVersion", "ActualLine", "Reconciliation") |
| `reference_id` | UUID | FK to the triggering entity |

**Invariant:** For every journal entry, `SUM(debit) = SUM(credit)` across all lines.

---

## 4. Entry Types

| Entry Type | Trigger | Debit Account | Credit Account |
|------------|---------|---------------|----------------|
| `PLAN_CREATE` | New milestone version (v1) | PLANNED | VARIANCE_RESERVE |
| `PLAN_ADJUST` | New milestone version (v2+), records the delta | PLANNED ↔ VARIANCE_RESERVE (direction depends on increase/decrease) |
| `ACTUAL_IMPORT` | SAP line committed from import | ACTUAL | VARIANCE_RESERVE |
| `RECONCILE` | User matches actual to milestone | Informational only — no $ movement, records the linkage |
| `RECONCILE_UNDO` | User removes a reconciliation | Informational only — records the unlinkage |

---

## 5. Entry Examples

### 5.1 PLAN_CREATE — New milestone, $25,250

```
Entry Type:      PLAN_CREATE
Effective Date:  2025-11-01
Description:     "New milestone: January Sustainment for DPI FY26 Sustainment"

  Account         Contract    Project   Milestone  Period     Debit      Credit
  ─────────────   ─────────   ───────   ─────────  ────────   ─────────  ─────────
  PLANNED         Glob ADM    PR01570   MS-001     FY26-JAN   $25,250
  VARIANCE_RESV   Glob ADM    PR01570   MS-001     FY26-JAN              $25,250

  Balance: $25,250 = $25,250 ✓
```

### 5.2 PLAN_ADJUST — Reduce milestone from $25,250 to $20,000

Delta = -$5,250 (decrease)

```
Entry Type:      PLAN_ADJUST
Effective Date:  2026-02-15
Description:     "Budget reduction: January Sustainment reduced per Q2 scope cut"

  Account         Contract    Project   Milestone  Period     Debit      Credit
  ─────────────   ─────────   ───────   ─────────  ────────   ─────────  ─────────
  VARIANCE_RESV   Glob ADM    PR01570   MS-001     FY26-JAN   $5,250
  PLANNED         Glob ADM    PR01570   MS-001     FY26-JAN              $5,250

  Balance: $5,250 = $5,250 ✓
  Net PLANNED for MS-001: $25,250 - $5,250 = $20,000
```

### 5.3 PLAN_ADJUST — Increase milestone from $20,000 to $22,000

Delta = +$2,000 (increase)

```
Entry Type:      PLAN_ADJUST
Effective Date:  2026-03-01
Description:     "Scope addition: added testing deliverable"

  Account         Contract    Project   Milestone  Period     Debit      Credit
  ─────────────   ─────────   ───────   ─────────  ────────   ─────────  ─────────
  PLANNED         Glob ADM    PR01570   MS-001     FY26-JAN   $2,000
  VARIANCE_RESV   Glob ADM    PR01570   MS-001     FY26-JAN              $2,000

  Balance: $2,000 = $2,000 ✓
  Net PLANNED for MS-001: $20,000 + $2,000 = $22,000
```

### 5.4 PLAN_ADJUST — Period shift (Jan to Feb) with amount change

Old: $20,000 in FY26-JAN. New: $22,000 in FY26-FEB.

```
Entry Type:      PLAN_ADJUST
Effective Date:  2026-03-01
Description:     "Shifted January Sustainment to February, increased to $22,000"

  -- Remove from old period
  VARIANCE_RESV   Glob ADM    PR01570   MS-001     FY26-JAN   $20,000
  PLANNED         Glob ADM    PR01570   MS-001     FY26-JAN              $20,000

  -- Add to new period
  PLANNED         Glob ADM    PR01570   MS-001     FY26-FEB   $22,000
  VARIANCE_RESV   Glob ADM    PR01570   MS-001     FY26-FEB              $22,000

  Balance: $42,000 = $42,000 ✓
```

### 5.5 ACTUAL_IMPORT — SAP line, $25,000

At import time, contract/project/milestone are unassigned. WBSE may be populated from SAP.

```
Entry Type:      ACTUAL_IMPORT
Effective Date:  2026-01-31
Description:     "SAP import: Doc# 5100012345, Globant invoice"

  Account         Contract    Project   Milestone  Period     Debit      Credit
  ─────────────   ─────────   ───────   ─────────  ────────   ─────────  ─────────
  ACTUAL          (null)      (null)    (null)     FY26-JAN   $25,000
  VARIANCE_RESV   (null)      (null)    (null)     FY26-JAN              $25,000

  Balance: $25,000 = $25,000 ✓
```

### 5.6 RECONCILE — Link actual to milestone

No money movement. Records the association for reporting.

```
Entry Type:      RECONCILE
Effective Date:  2026-02-05
Description:     "Reconciled SAP Doc# 5100012345 to January Sustainment as INVOICE"

  (No debit/credit lines — this is a metadata event)
  Reference: Reconciliation record linking actual_id → milestone_id
```

---

## 6. Balance Queries

### Current planned balance for a milestone

```sql
SELECT
  SUM(jl.debit) - SUM(jl.credit) AS planned_balance
FROM journal_line jl
JOIN journal_entry je ON jl.entry_id = je.entry_id
WHERE jl.account = 'PLANNED'
  AND jl.milestone_id = :milestoneId
```

### Current actual balance for a milestone (reconciled actuals only)

```sql
SELECT
  SUM(jl.debit) - SUM(jl.credit) AS actual_balance
FROM journal_line jl
JOIN journal_entry je ON jl.entry_id = je.entry_id
JOIN reconciliation r ON jl.reference_id = r.actual_id
WHERE jl.account = 'ACTUAL'
  AND r.milestone_id = :milestoneId
```

### Variance for a contract in a fiscal period

```sql
SELECT
  planned.balance - actual.balance AS variance
FROM
  (SELECT SUM(debit) - SUM(credit) AS balance
   FROM journal_line WHERE account = 'PLANNED'
   AND contract_id = :contractId AND fiscal_period_id = :periodId) planned,
  (SELECT SUM(debit) - SUM(credit) AS balance
   FROM journal_line WHERE account = 'ACTUAL'
   AND contract_id = :contractId AND fiscal_period_id = :periodId) actual
```

### Time-machine variant (add effective_date filter)

Append to any query:
```sql
AND je.effective_date <= :timeMachineDate
```

---

## 7. Reconciliation and Journal Interaction

When a user reconciles an actual to a milestone:

1. The **Reconciliation** record is created (actual_id, milestone_id, category)
2. A **RECONCILE journal entry** is created (informational, no $ lines)
3. The **existing ACTUAL_IMPORT journal lines** for that actual are **not modified** — they remain with null contract/project/milestone

**To get "actual spend for a milestone,"** the query joins through the Reconciliation table:

```
Actual Lines → Reconciliation (actual_id) → Milestone (milestone_id)
```

This means the journal lines themselves don't need to be updated when reconciliation happens. The Reconciliation table serves as the mapping layer.

**Why this design:**
- Journal entries are immutable (Principle #3)
- Reconciliation can be undone without reversing journal entries
- The actual's journal entry retains its original import context
