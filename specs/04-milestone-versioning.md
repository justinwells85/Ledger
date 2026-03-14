# Milestone Versioning

> Source: DPI Accruals v2 Spec, Section 4

---

## 1. Overview

Each milestone is independently versioned. A version captures the planned amount and target fiscal period at a point in time. The "current plan" is the latest version of each milestone. The "plan as of date X" is reconstructed by taking each milestone's latest version with effective_date <= X.

---

## 2. Version Model

```
Milestone: "January Sustainment" (MS-001)
├── Version 1  |  effective: 2025-11-01  |  amount: $25,250  |  period: FY26-JAN  |  reason: "Initial budget"
├── Version 2  |  effective: 2026-02-15  |  amount: $20,000  |  period: FY26-JAN  |  reason: "Q2 scope cut"
└── Version 3  |  effective: 2026-03-01  |  amount: $22,000  |  period: FY26-FEB  |  reason: "Shifted to Feb, added testing"
```

---

## 3. Rules

| Rule | Description |
|------|-------------|
| V-01 | Version numbers are sequential per milestone (1, 2, 3, ...) and immutable |
| V-02 | `effective_date` is required — this is when the version becomes active for reporting |
| V-03 | `effective_date` must be >= the prior version's effective_date |
| V-04 | `reason` is required — free text explaining why the change was made |
| V-05 | Every new version (v2+) creates a `PLAN_ADJUST` journal entry recording the delta |
| V-06 | Version 1 is created automatically when a milestone is created, producing a `PLAN_CREATE` journal entry |
| V-07 | The "current plan" for a milestone = the version with the highest version_number |
| V-08 | The "plan as of date X" for a milestone = the version with the highest version_number where effective_date <= X |
| V-09 | Cancelling a milestone = creating a new version with planned_amount = $0 and a reason like "Cancelled" |
| V-10 | A milestone's fiscal period can change between versions (payment shifted to a different month) |

---

## 4. Version Creation Workflow

### Creating a new milestone (v1)

```
Input:
  - project_id
  - name, description
  - planned_amount
  - fiscal_period_id
  - effective_date
  - reason (defaults to "Initial budget" if not provided)

Actions:
  1. Create Milestone record
  2. Create MilestoneVersion (version_number = 1)
  3. Create PLAN_CREATE journal entry:
     Debit  PLANNED         [planned_amount] for [fiscal_period]
     Credit VARIANCE_RESERVE [planned_amount] for [fiscal_period]
```

### Adjusting a milestone (v2+) — same period

```
Input:
  - milestone_id
  - new planned_amount
  - effective_date
  - reason (required)

Derived:
  - delta = new_amount - current_version.planned_amount
  - fiscal_period = current_version.fiscal_period_id (unchanged)

Actions:
  1. Create MilestoneVersion (version_number = current + 1)
  2. Create PLAN_ADJUST journal entry:
     If delta > 0 (increase):
       Debit  PLANNED         [delta] for [fiscal_period]
       Credit VARIANCE_RESERVE [delta] for [fiscal_period]
     If delta < 0 (decrease):
       Debit  VARIANCE_RESERVE [abs(delta)] for [fiscal_period]
       Credit PLANNED          [abs(delta)] for [fiscal_period]
```

### Adjusting a milestone (v2+) — period shift

```
Input:
  - milestone_id
  - new planned_amount
  - new fiscal_period_id
  - effective_date
  - reason (required)

Derived:
  - old_amount = current_version.planned_amount
  - old_period = current_version.fiscal_period_id
  - new_amount = input amount
  - new_period = input period

Actions:
  1. Create MilestoneVersion (version_number = current + 1)
  2. Create PLAN_ADJUST journal entry (4 lines):
     -- Remove from old period
     Debit  VARIANCE_RESERVE [old_amount] for [old_period]
     Credit PLANNED          [old_amount] for [old_period]
     -- Add to new period
     Debit  PLANNED          [new_amount] for [new_period]
     Credit VARIANCE_RESERVE [new_amount] for [new_period]
```

### Cancelling a milestone

```
Input:
  - milestone_id
  - effective_date
  - reason (required, e.g., "Project descoped")

Actions:
  1. Create MilestoneVersion (version_number = current + 1, planned_amount = 0)
  2. Create PLAN_ADJUST journal entry:
     Debit  VARIANCE_RESERVE [current_amount] for [current_period]
     Credit PLANNED          [current_amount] for [current_period]
```

---

## 5. Querying Versions

### Get current plan for a milestone

```sql
SELECT * FROM milestone_version
WHERE milestone_id = :milestoneId
ORDER BY version_number DESC
LIMIT 1
```

### Get plan as of a specific date (time machine)

```sql
SELECT * FROM milestone_version
WHERE milestone_id = :milestoneId
  AND effective_date <= :asOfDate
ORDER BY version_number DESC
LIMIT 1
```

### Get full version history for a milestone

```sql
SELECT * FROM milestone_version
WHERE milestone_id = :milestoneId
ORDER BY version_number ASC
```

### Get all milestones for a project with their current versions

```sql
SELECT m.*, mv.*
FROM milestone m
JOIN milestone_version mv ON m.milestone_id = mv.milestone_id
WHERE m.project_id = :projectId
  AND mv.version_number = (
    SELECT MAX(version_number)
    FROM milestone_version
    WHERE milestone_id = m.milestone_id
  )
```

### Get plan for a project at a point in time

```sql
SELECT m.*, mv.*
FROM milestone m
JOIN milestone_version mv ON m.milestone_id = mv.milestone_id
WHERE m.project_id = :projectId
  AND mv.effective_date <= :asOfDate
  AND mv.version_number = (
    SELECT MAX(version_number)
    FROM milestone_version mv2
    WHERE mv2.milestone_id = m.milestone_id
      AND mv2.effective_date <= :asOfDate
  )
```
