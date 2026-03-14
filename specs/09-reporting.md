# Reporting & Visualization

> Source: DPI Accruals v2 Spec, Section 9

---

## 1. Overview

All reports are derived views over the journal ledger, milestone versions, and reconciliation data. No report has its own stored data. All reports respect the time machine when active.

---

## 2. Report Definitions

### 2.1 Budget Plan Report

**Purpose:** Show what we plan to spend.

**Dimensions (groupable and filterable):**
- Contract
- Project / WBSE
- Funding source (OPEX, CAPEX, OTHER_TEAM)
- Fiscal period (month, quarter, year)
- Milestone (leaf level)

**Columns per row:**

| Column | Source |
|--------|--------|
| Contract | contract.name |
| Project | project.name |
| WBSE | project.wbse |
| Funding Source | project.funding_source |
| Milestone | milestone.name |
| Fiscal Period | milestone_version.fiscal_period (current or as-of-date version) |
| Planned Amount | milestone_version.planned_amount (current or as-of-date version) |
| Version # | milestone_version.version_number |
| Last Modified | milestone_version.effective_date |
| Modified By | milestone_version.created_by |

**Aggregations:**
- Milestone → Project: SUM(planned_amount)
- Project → Contract: SUM(planned_amount)
- Contract → Portfolio: SUM(planned_amount)
- Any level grouped by fiscal period (month → quarter → year)
- Any level grouped by funding source

**Layout options:**
1. **Flat table** — one row per milestone, sortable/filterable
2. **Pivot by period** — contracts/projects as rows, fiscal months as columns (similar to current Excel FY2026 sheet)

---

### 2.2 Actuals Report

**Purpose:** Show what SAP says we've spent.

**Columns:**

| Column | Source |
|--------|--------|
| Posting Date | actual_line.posting_date |
| Fiscal Period | fiscal_period.display_name |
| Amount | actual_line.amount |
| Vendor | actual_line.vendor_name |
| WBSE | actual_line.wbse |
| Description | actual_line.description |
| SAP Doc # | actual_line.sap_document_number |
| Import Date | actual_line.created_at |
| Recon Status | "Reconciled" or "Unreconciled" |
| Category | reconciliation.category (if reconciled) |
| Matched Milestone | milestone.name (if reconciled) |
| Matched Contract | contract.name (if reconciled) |

**Filters:**
- Fiscal period
- Reconciliation status (All, Reconciled, Unreconciled)
- Category (Invoice, Accrual, Accrual Reversal, Allocation)
- Vendor name (contains)
- Amount range
- Contract (for reconciled actuals)

---

### 2.3 Variance Report (Plan vs. Actual)

**Purpose:** The primary operational view — are we on budget?

**Granularity options:**
- By contract + fiscal period
- By project + fiscal period
- By milestone

**Columns:**

| Column | Source |
|--------|--------|
| Contract | contract.name |
| Project | project.name (if project-level granularity) |
| Milestone | milestone.name (if milestone-level granularity) |
| Fiscal Period | fiscal_period.display_name |
| Planned | SUM(journal_line.debit - credit) WHERE account = PLANNED |
| Actual | SUM(reconciled actual amounts) |
| Variance ($) | Planned - Actual |
| Variance (%) | (Planned - Actual) / Planned * 100 |
| Status | UNDER_BUDGET, WITHIN_TOLERANCE, OVER_BUDGET |

**Visual indicators:**
| Status | Color |
|--------|-------|
| Under budget (variance > 0) | Green |
| Within tolerance | Neutral / light green |
| Approaching budget (>90% spent) | Yellow |
| Over budget (variance < 0) | Red |

**Drill-down:** Clicking a row shows the underlying reconciled actuals with their categories.

**Pivot layout:** Contracts/projects as rows, fiscal months as columns, cell value = variance amount with color coding. This is the closest equivalent to the current Excel FY2026 sheet.

---

### 2.4 Reconciliation Status Report

**Purpose:** Show which milestones have been reconciled and where gaps exist.

**Columns:**

| Column | Source |
|--------|--------|
| Contract | contract.name |
| Project | project.name |
| Milestone | milestone.name |
| Fiscal Period | current version's fiscal_period |
| Planned | current version's planned_amount |
| Invoice Total | SUM(actual.amount) WHERE category = INVOICE |
| Accrual (net) | SUM(actual.amount) WHERE category IN (ACCRUAL, ACCRUAL_REVERSAL) |
| Allocation Total | SUM(actual.amount) WHERE category = ALLOCATION |
| Total Actual | Invoice + Accrual Net + Allocation |
| Remaining | Planned - Total Actual |
| Open Accruals | Count of unresolved accruals |
| Status | FULLY_RECONCILED, PARTIALLY_MATCHED, UNMATCHED, OVER_BUDGET |

**Filters:** Contract, project, fiscal period, status

---

### 2.5 Forecast Report

**Purpose:** Projected total spend = actuals to date + remaining plan.

**Calculation per scope (contract, project, or milestone):**

```
Actuals YTD    = SUM(reconciled actual amounts) for periods up to current period
Remaining Plan = SUM(planned amounts) for periods after current period (current versions)
Forecast Total = Actuals YTD + Remaining Plan
Original Plan  = SUM(all planned amounts, all periods, current versions)
Forecast Delta = Forecast Total - Original Plan
```

**Columns:**

| Column | Source |
|--------|--------|
| Contract / Project | entity name |
| Original Plan (full year) | SUM of all milestone planned amounts |
| Actuals YTD | SUM of reconciled actuals through current period |
| Remaining Plan | SUM of planned amounts for future periods |
| Forecast Total | Actuals YTD + Remaining Plan |
| Forecast vs. Plan | Forecast Total - Original Plan |

**Note:** "Remaining Plan" uses the **current** milestone versions, which may have been adjusted. This is intentional — the forecast reflects the latest known plan, not the original baseline.

---

### 2.6 Funding Source Summary

**Purpose:** Roll up costs by funding type across the portfolio.

**Columns:**

| Column | Source |
|--------|--------|
| Funding Source | project.funding_source |
| Planned Total | SUM(planned amounts) for all projects with this funding source |
| Actual YTD | SUM(reconciled actuals) for all projects with this funding source |
| Forecast | Actuals YTD + Remaining Plan |
| Variance | Planned - Actual |

**Drill-down:** Click a funding source to see the underlying contracts and projects.

---

## 3. Common Report Features

### 3.1 Time Machine

All reports accept the global time machine date. When active, all calculations use the as-of-date variants of queries.

### 3.2 Export

All tabular reports should support export to:
- CSV
- Excel (XLSX)

### 3.3 Sorting and Filtering

All table columns should be sortable. Key columns should be filterable via dropdown or text search.

### 3.4 Aggregation Toggles

Where applicable, users should be able to:
- Expand/collapse grouping levels (contract → project → milestone)
- Toggle between monthly and quarterly period grouping
- Show/hide zero-amount rows
