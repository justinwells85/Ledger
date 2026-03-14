# Time Machine

> Source: DPI Accruals v2 Spec, Section 8

---

## 1. Overview

The time machine allows a user to select any date and view the state of the system as it was known on that date. It is a **query filter**, not a data modification. When active, all views, calculations, and reports show only data that existed as of the selected date.

---

## 2. What Gets Filtered

| Data Type | Filter Field | Logic |
|-----------|-------------|-------|
| Milestone planned amounts | `milestone_version.effective_date` | Show version with highest version_number where effective_date <= target date |
| SAP actuals | `actual_line.created_at` | Show only actuals imported on or before target date |
| Reconciliation links | `reconciliation.reconciled_at` | Show only reconciliations made on or before target date |
| Journal entries | `journal_entry.effective_date` | Show only entries effective on or before target date |
| Contracts | `contract.created_at` | Show only contracts that existed as of target date |
| Projects | `project.created_at` | Show only projects that existed as of target date |
| Milestones | `milestone.created_at` | Show only milestones that existed as of target date |

---

## 3. Derived Values Under Time Machine

All derived values respect the time machine date:

| Derived Value | Calculation Under Time Machine |
|--------------|-------------------------------|
| Planned amount (milestone) | Latest version where effective_date <= target date |
| Planned total (project) | Sum of all milestone planned amounts as of target date |
| Actual total (milestone) | Sum of reconciled actuals where both actual.created_at and reconciliation.reconciled_at <= target date |
| Variance | Planned (as of date) - Actual (as of date) |
| Reconciliation status | Based on planned and actual amounts as of target date |
| Open accruals | Accruals reconciled before target date without a reversal reconciled before target date |
| Forecast | Actuals as of date + remaining plan as of date |

---

## 4. Milestones That "Don't Exist Yet"

If a milestone was created **after** the time machine date, it should not appear in any view. Similarly, if a milestone version with planned_amount = 0 (cancellation) has an effective_date before or on the target date, the milestone should show as cancelled ($0 planned).

**Edge case:** A milestone created on 2026-03-01 with effective_date 2026-02-15 (backdated). Under time machine:
- As of 2026-02-14: milestone does not exist (created_at > target date? or effective_date > target date?)
- **Decision:** Use `effective_date` as the governing field for time machine, not `created_at`. This allows backdating of budget entries. If the milestone's first version has effective_date 2026-02-15, it "exists" for any time machine date >= 2026-02-15.

**Revised rule:** A milestone appears in time machine views if it has at least one version with effective_date <= target date.

---

## 5. User Experience

### Activation

- The time machine is a **global date picker** accessible from any page
- When the user selects a date, all views immediately re-render with filtered data
- A persistent banner or indicator shows the time machine is active: e.g., "Viewing as of: February 15, 2026"

### Deactivation

- User clicks "Return to current" or clears the date picker
- All views revert to showing current state (no date filter)

### Constraints

- The time machine date cannot be in the future
- The earliest valid date is the system's first fiscal year start date

---

## 6. Implementation Notes

### Query Pattern

Every service method that returns financial data should accept an optional `asOfDate` parameter. When provided, all queries include the appropriate date filter.

```java
// Service method signature pattern
public BudgetSummary getBudgetSummary(UUID contractId, UUID periodId, LocalDate asOfDate);

// If asOfDate is null → return current state
// If asOfDate is provided → apply time machine filters
```

### Index Considerations

The following fields will be frequently filtered by date and should be indexed:
- `milestone_version.effective_date`
- `actual_line.created_at`
- `reconciliation.reconciled_at`
- `journal_entry.effective_date`

### Testing Strategy

Time machine behavior is highly testable. Every test case should have a variant that:
1. Sets up data at multiple points in time
2. Queries at each point and verifies the correct snapshot is returned
3. Verifies that data created "after" the time machine date is excluded

See [10-business-rules.md](./10-business-rules.md) for time-machine-related business rules (BR-19, BR-22).
