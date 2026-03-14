# Reconciliation

> Source: DPI Accruals v2 Spec, Section 6

---

## 1. Overview

Reconciliation is the manual process of associating an imported SAP actual line with a planned milestone. Every reconciliation must be classified into one of four categories. This is a human-driven process (Tier 1 — no auto-matching).

---

## 2. Categories

| Category | Code | Description | Terminal? |
|----------|------|-------------|-----------|
| Invoice | `INVOICE` | Vendor has billed. This is final spend. | Yes |
| Accrual | `ACCRUAL` | Work completed, no invoice yet. Finance records estimated cost. | No — must be reversed |
| Accrual Reversal | `ACCRUAL_REVERSAL` | Offsets a prior accrual. | Yes (it IS the follow-up) |
| Allocation | `ALLOCATION` | Indirect cost from another department (chargebacks, licenses, labor). | Yes |

---

## 3. Reconciliation Workflow

### 3.1 View Unreconciled Actuals

Display all actual lines that do not have a reconciliation link.

**Filterable by:**
- Fiscal period
- Amount range (min/max)
- Vendor name (contains)
- Description (contains)
- Posting date range

**Sortable by:**
- Posting date
- Amount (ascending/descending)
- Vendor name

**Displayed columns:**
- Posting date
- Amount
- Vendor name
- WBSE (if present)
- Description
- SAP document number (if present)
- Import date

### 3.2 Select an Actual to Reconcile

User clicks on an unreconciled actual line.

### 3.3 Display Candidate Milestones

System shows milestones that could match this actual.

**Ordering heuristic (Tier 1 — simple, not ML):**
1. If actual has a WBSE, show milestones from matching projects first
2. Within that, sort by closest period match (same fiscal period first)
3. Then by closest amount match (smallest absolute difference)
4. Then show remaining milestones from other projects/contracts

**Displayed columns for each candidate:**
- Contract name
- Project name / WBSE
- Milestone name
- Fiscal period
- Planned amount (current version)
- Already reconciled total
- Remaining (planned - reconciled)

### 3.4 Assign and Classify

User selects a milestone and chooses a category:
- `INVOICE`, `ACCRUAL`, `ACCRUAL_REVERSAL`, or `ALLOCATION`

User optionally enters `match_notes` explaining why this match was made.

### 3.5 System Actions on Reconciliation

1. Create `Reconciliation` record (actual_id, milestone_id, category, match_notes, reconciled_by, reconciled_at)
2. Create `RECONCILE` journal entry (informational — records who linked what, when)
3. Update milestone reconciliation status (derived, not stored)

### 3.6 Undo a Reconciliation

User can remove a reconciliation link. This:
1. Deletes the `Reconciliation` record (or marks it as undone)
2. Creates `RECONCILE_UNDO` journal entry (audit trail — who unlinked what, when, why)
3. Actual line returns to the unreconciled pool

**Reason is required** when undoing a reconciliation.

---

## 4. Reconciliation Status per Milestone

All status values are **derived** (computed, never stored):

```
Planned Amount       = latest MilestoneVersion.planned_amount
Reconciled Amount    = SUM(actual.amount) for all linked reconciliations
  ├── Invoice Total  = SUM where category = INVOICE
  ├── Accrual Net    = SUM where category IN (ACCRUAL, ACCRUAL_REVERSAL)
  └── Allocation Tot = SUM where category = ALLOCATION
Remaining            = Planned Amount - Reconciled Amount
```

### Status Enum (Derived)

| Status | Condition |
|--------|-----------|
| `FULLY_RECONCILED` | \|Remaining\| within tolerance |
| `OVER_BUDGET` | Remaining < 0 (and outside tolerance) |
| `PARTIALLY_MATCHED` | Remaining > 0 and Reconciled Amount > 0 |
| `UNMATCHED` | Reconciled Amount = 0 |

---

## 5. Tolerance

Configurable thresholds determine when a milestone is "close enough" to fully reconciled.

| Threshold | Type | Default | Example |
|-----------|------|---------|---------|
| `tolerance_percent` | Percentage | 2% | Planned $25,000, tolerance = $500 |
| `tolerance_absolute` | Dollar amount | $50 | Fixed regardless of planned amount |

**Rule:** A milestone is "within tolerance" if **either** condition is met:
- `|Remaining| / Planned Amount <= tolerance_percent`
- `|Remaining| <= tolerance_absolute`

Tolerance values are system-level configuration.

---

## 6. Constraints

| Rule | Description |
|------|-------------|
| R-01 | An actual line can be reconciled to at most one milestone (1:1) |
| R-02 | A milestone can have many actuals reconciled to it (M:1) |
| R-03 | Reconciliation category is required |
| R-04 | Reconciliation can be undone (with required reason) |
| R-05 | Undoing a reconciliation creates an audit journal entry |
| R-06 | Duplicate actual lines (is_duplicate = true) cannot be reconciled |
