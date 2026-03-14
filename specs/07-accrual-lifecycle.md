# Accrual Lifecycle

> Source: DPI Accruals v2 Spec, Section 7

---

## 1. Overview

Accruals are provisional cost entries made by Finance when work is completed but no invoice has been received. They follow a predictable lifecycle: accrue → reverse → (re-accrue → reverse →) invoice. The system must track this lifecycle and flag anomalies.

---

## 2. Lifecycle

### Normal 2-month cycle (work done, invoice arrives next month)

```
Month 1:  ACCRUAL           +$25,000  →  Milestone X
Month 2:  ACCRUAL_REVERSAL  -$25,000  →  Milestone X
Month 2:  INVOICE           +$25,000  →  Milestone X
─────────────────────────────────────────────────────
Net actual on Milestone X:   $25,000  (correct)
```

### Extended cycle (invoice delayed multiple months)

```
Month 1:  ACCRUAL           +$25,000  →  Milestone X
Month 2:  ACCRUAL_REVERSAL  -$25,000  →  Milestone X
Month 2:  ACCRUAL           +$25,000  →  Milestone X   (re-accrued, still no invoice)
Month 3:  ACCRUAL_REVERSAL  -$25,000  →  Milestone X
Month 3:  ACCRUAL           +$25,000  →  Milestone X   (re-accrued again)
Month 4:  ACCRUAL_REVERSAL  -$25,000  →  Milestone X
Month 4:  INVOICE           +$25,000  →  Milestone X   (finally invoiced)
─────────────────────────────────────────────────────
Net actual on Milestone X:   $25,000  (correct)
```

### Key property

At any point in time, the net actual on the milestone correctly reflects the incurred cost. The accrual/reversal pairs cancel out, leaving only the real cost (either the current open accrual or the final invoice).

---

## 3. Tracking Rules

| Rule | Description |
|------|-------------|
| A-01 | A reconciliation with category = `ACCRUAL` creates an **open accrual** against that milestone |
| A-02 | A reconciliation with category = `ACCRUAL_REVERSAL` against the same milestone **closes** the most recent open accrual |
| A-03 | Open accruals = count of ACCRUALs minus count of ACCRUAL_REVERSALs for a given milestone. If > 0, there are open accruals |
| A-04 | Net accrual balance per milestone = SUM(ACCRUAL amounts) + SUM(ACCRUAL_REVERSAL amounts). Should trend toward $0 over time as invoices replace accruals |
| A-05 | `ACCRUAL_REVERSAL` amounts are expected to be **negative**. System warns (does not block) if a positive amount is categorized as ACCRUAL_REVERSAL |

---

## 4. Aging Alerts

Open accruals that persist beyond a configurable threshold indicate a potential issue (invoice delayed, accrual forgotten, etc.).

| Configuration | Default | Description |
|--------------|---------|-------------|
| `accrual_aging_warning_days` | 60 | Days after which an open accrual triggers a warning |
| `accrual_aging_critical_days` | 90 | Days after which an open accrual triggers a critical alert |

**Aging calculation:**
```
accrual_age = current_date - reconciliation.reconciled_at (of the ACCRUAL entry)
```

If a milestone has an open accrual (no matching reversal) and the accrual_age exceeds the threshold, it appears in the aging report.

---

## 5. Accrual Status per Milestone (Derived)

```
Accrual Count           = COUNT where category = ACCRUAL
Reversal Count          = COUNT where category = ACCRUAL_REVERSAL
Open Accrual Count      = Accrual Count - Reversal Count

Net Accrual Balance     = SUM(amount) where category IN (ACCRUAL, ACCRUAL_REVERSAL)

Oldest Open Accrual Age = MIN(reconciled_at) among unmatched ACCRUALs

Status:
  CLEAN            — Open Accrual Count = 0
  OPEN             — Open Accrual Count > 0, age < warning threshold
  AGING_WARNING    — Open Accrual Count > 0, age >= warning threshold
  AGING_CRITICAL   — Open Accrual Count > 0, age >= critical threshold
```

---

## 6. Reporting

### Open Accruals Report

Shows all milestones with open accruals, sorted by age (oldest first):

| Contract | Project | Milestone | Accrual Amount | Accrual Date | Age (days) | Status |
|----------|---------|-----------|---------------|-------------|------------|--------|

**Filters:** Contract, project, status (all, warning, critical)

### Accrual History for a Milestone

Shows the full accrual/reversal/invoice sequence for a single milestone:

| Date | Category | Amount | Running Net | Notes |
|------|----------|--------|------------|-------|
| 2026-01-31 | ACCRUAL | +$25,000 | $25,000 | Finance month-end accrual |
| 2026-02-28 | ACCRUAL_REVERSAL | -$25,000 | $0 | Standard reversal |
| 2026-02-28 | INVOICE | +$25,000 | $25,000 | Globant Invoice #12345 |
