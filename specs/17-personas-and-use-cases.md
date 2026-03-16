# Personas & Use Cases

> Defines the user types who interact with Ledger and the specific scenarios they need to accomplish.
> Use cases are grouped by persona. Implementation backlog items reference these by ID (e.g., P1-S1).

---

## Persona Overview

| ID | Persona | Primary Goal |
|----|---------|-------------|
| P1 | Finance Manager / Budget Owner | Plan and monitor vendor contract budgets |
| P2 | SAP Data Administrator | Import and validate monthly actuals from SAP |
| P3 | Reconciliation Specialist | Match actuals to planned milestones each period |
| P4 | Finance Leadership / Executive | Portfolio-level visibility and reporting |
| P5 | Project / Contract Manager | Day-to-day tracking of specific contracts |
| P6 | System Administrator | Configure system, manage reference data, and maintain audit integrity |

---

## P1 — Finance Manager / Budget Owner

Owns one or more vendor contracts. Accountable for budget planning and variance monitoring at the portfolio level. Establishes the initial plan, adjusts it as scope changes, and is responsible for explaining variances to leadership.

| Scenario ID | Description | Priority |
|-------------|-------------|----------|
| P1-S1 | Create a new contract for a vendor | Core |
| P1-S2 | Add projects to a contract | Core |
| P1-S3 | Create milestones with initial planned amounts and fiscal period assignments | Core |
| P1-S4 | Adjust a milestone's planned amount mid-year with a documented reason | Core |
| P1-S5 | Cancel a milestone that is no longer in scope | Core |
| P1-S6 | View the portfolio dashboard — total budget, actuals, variance across all contracts | Core |
| P1-S7 | Drill down from dashboard → contract → project → milestone | Core |
| P1-S8 | Export the budget plan to CSV for use in leadership presentations | Core |
| P1-S9 | Adjust reconciliation tolerance thresholds when business rules change | Core |
| P1-S10 | Use the time machine to compare the current plan to what it looked like on a prior date | Core |
| P1-S11 | See a full version history for a milestone — who changed it, when, and why | Extended |
| P1-S12 | Close or terminate a contract that has ended | Core |

---

## P2 — SAP Data Administrator

Responsible for pulling the monthly SAP export and getting it into Ledger cleanly. Monitors deduplication and ensures the actuals data is trustworthy before reconciliation begins.

| Scenario ID | Description | Priority |
|-------------|-------------|----------|
| P2-S1 | Upload a SAP CSV or Excel export file | Core |
| P2-S2 | Review the import summary showing new vs. duplicate line counts | Core |
| P2-S3 | Inspect individual lines before committing to verify data quality | Core |
| P2-S4 | Commit a valid import to create actual line records | Core |
| P2-S5 | Reject a bad import and upload a corrected file | Core |
| P2-S6 | View the history of all past imports with their status | Core |
| P2-S7 | Re-upload a corrected file after rejection (deduplication handles overlap) | Core |
| P2-S8 | Understand why a specific line was flagged as a duplicate | Extended |

---

## P3 — Reconciliation Specialist / Finance Analyst

Manually matches SAP actuals to planned milestones each period. Manages the accrual lifecycle — ensuring accruals are reversed and invoices are recorded. Monitors aging alerts.

| Scenario ID | Description | Priority |
|-------------|-------------|----------|
| P3-S1 | View all unreconciled actuals in the reconciliation workspace | Core |
| P3-S2 | Select an actual and see a ranked list of candidate milestones | Core |
| P3-S3 | Reconcile an actual to a milestone with category classification and optional notes | Core |
| P3-S4 | Undo an incorrect reconciliation with a documented reason | Core |
| P3-S5 | Filter unreconciled actuals by vendor, amount range, or fiscal period | Core |
| P3-S6 | See which actuals remain unreconciled after a work session | Core |
| P3-S7 | View open accruals sorted by age and status to prioritize work | Core |
| P3-S8 | Track reconciliation completion rate per milestone | Core |
| P3-S9 | Classify reconciliations as INVOICE, ACCRUAL, ACCRUAL_REVERSAL, or ALLOCATION | Core |
| P3-S10 | See the full reconciliation history for a specific milestone | Extended |

---

## P4 — Finance Leadership / Executive

Needs portfolio-level visibility for strategic decision-making and reporting to stakeholders. Does not perform data entry — consumes reports and monitors health indicators.

| Scenario ID | Description | Priority |
|-------------|-------------|----------|
| P4-S1 | View KPI cards: total budget, actuals, variance, and % spent at a glance | Core |
| P4-S2 | Identify which contracts are over or under budget | Core |
| P4-S3 | See how spend is split across OPEX, CAPEX, and other funding sources | Extended |
| P4-S4 | Compare the current plan to what was planned at the start of the fiscal year | Core |
| P4-S5 | View a full-year forecast (actuals to date plus remaining planned) | Extended |
| P4-S6 | Export the variance report for board or stakeholder presentations | Core |
| P4-S7 | See active alerts — contracts over budget, aging accruals | Core |
| P4-S8 | Drill into a specific contract or project from the dashboard | Core |

---

## P5 — Project / Contract Manager

Owns day-to-day tracking of a specific contract or set of projects. Creates and maintains the project plan, monitors spend, and needs to understand what changed and why.

| Scenario ID | Description | Priority |
|-------------|-------------|----------|
| P5-S1 | View all milestones for a project with current status and remaining amounts | Core |
| P5-S2 | Create a new milestone for a new deliverable | Core |
| P5-S3 | Update a milestone amount when scope changes, with a documented reason | Core |
| P5-S4 | Cancel a milestone that is no longer needed | Core |
| P5-S5 | See the version history for a milestone — all changes with reasons | Extended |
| P5-S6 | See which SAP actuals have been matched to milestones in my project | Extended |
| P5-S7 | View remaining budget per milestone after reconciled actuals | Core |
| P5-S8 | Filter the budget report to a single contract or project | Core |
| P5-S9 | See the audit trail for a contract — who made changes and when | Extended |

---

## P6 — System Administrator

Maintains system configuration, reference data, and audit integrity. Ensures the system is configured correctly for each fiscal year and that all operational data (users, fiscal calendars, categories) is current and accurate.

| Scenario ID | Description | Priority |
|-------------|-------------|----------|
| P6-S1 | Manage user accounts — create, deactivate, assign roles | Core |
| P6-S2 | Add a new fiscal year with automatically generated 12-period calendar | Core |
| P6-S3 | Manage reference data — funding sources, contract statuses, project statuses | Core |
| P6-S4 | Manage reconciliation categories (add, rename, deactivate) | Extended |
| P6-S5 | View and update all system configuration settings (data-driven, not hardcoded) | Core |
| P6-S6 | Browse the audit log — filter by entity, user, or date range | Core |
| P6-S7 | Export the audit log for compliance or external audit purposes | Extended |
| P6-S8 | View the full version history for any milestone in the system | Extended |

---

## Current Implementation Coverage

> For the full E2E test scenario matrix (Tier 1 and Tier 2 coverage, per-scenario action steps and UI validations), see `20-e2e-scenario-matrix.md`.

| Scenario | Frontend | Backend | Tier 1 E2E | Known Issues |
|----------|----------|---------|------------|--------------|
| P1-S1 Create contract | ✅ Dashboard drawer | ✅ | ✅ | ⚠️ New contract invisible on dashboard until it has milestones (see spec 20) |
| P1-S2 Add project | ✅ ContractDetail drawer | ✅ | ✅ | — |
| P1-S3 Create milestone | ✅ ProjectDetail drawer | ✅ | ✅ | — |
| P1-S4 Adjust milestone | ✅ MilestonePanel new version form | ✅ | ✅ | — |
| P1-S5 Cancel milestone | ✅ MilestonePanel cancel form | ✅ | ✅ | — |
| P1-S6 Dashboard KPI | ✅ | ✅ | ✅ | — |
| P1-S7 Drill-down navigation | ✅ | ✅ | ✅ | — |
| P1-S8 Export CSV | ✅ Budget Report CSV | ✅ | ✅ | — |
| P1-S9 Tolerance settings | ✅ Settings page | ✅ | ✅ | — |
| P1-S10 Time machine | ✅ | ✅ | ✅ | — |
| P1-S11 Milestone version history | ✅ MilestonePanel | ✅ | ⚠️ Partial | — |
| P1-S12 Close contract | ✅ Edit drawer status field | ✅ | ✅ | — |
| P2-S1 Upload CSV | ✅ SAP Import page | ✅ | ✅ | — |
| P2-S2 Review summary | ✅ | ✅ | ✅ | — |
| P2-S3 Inspect lines | ✅ | ✅ | ✅ | — |
| P2-S4 Commit import | ✅ | ✅ | ✅ | — |
| P2-S5 Reject import | ✅ | ✅ | ✅ | — |
| P2-S6 Import history | ✅ | ✅ | ✅ | — |
| P2-S7 Re-upload corrected file | ✅ | ✅ | ⚠️ Partial | — |
| P2-S8 Duplicate explanation | ❌ | ❌ | ❌ | Extended priority |
| P3-S1 View unreconciled | ✅ Reconcile Workspace | ✅ | ✅ | — |
| P3-S2 Select actual + candidates | ✅ | ✅ | ✅ | — |
| P3-S3 Reconcile actual | ✅ | ✅ | ✅ | — |
| P3-S4 Undo reconciliation | ✅ | ✅ | ✅ | — |
| P3-S5 Filter actuals | ✅ | ✅ | ✅ | — |
| P3-S6 Remaining unreconciled view | ✅ | ✅ | ⚠️ Partial | — |
| P3-S7 Open accruals report | ✅ | ✅ | ✅ | — |
| P3-S8 Reconciliation completion rate | ✅ Reconciliation status report | ✅ | ⚠️ Partial | — |
| P3-S9 Category classification | ✅ Reconcile form radio buttons | ✅ | ✅ | — |
| P4-S1 KPI cards | ✅ Dashboard | ✅ | ✅ | — |
| P4-S2 Over/under budget | ✅ Dashboard contract table | ✅ | ✅ | — |
| P4-S3 Funding breakdown | ✅ Funding Report page | ✅ | ⚠️ Partial | — |
| P4-S4 Time machine compare | ✅ | ✅ | ✅ | — |
| P4-S5 Forecast | ✅ Forecast Report page | ✅ | ⚠️ Partial | — |
| P4-S6 Export variance | ✅ Variance Report CSV | ✅ | ⚠️ Partial | — |
| P4-S7 Alerts | ✅ Dashboard alerts section | ✅ | ✅ | — |
| P4-S8 Drill into contract | ✅ | ✅ | ✅ | — |
| P5-S1 View milestones | ✅ ProjectDetail | ✅ | ✅ | — |
| P5-S2 Create milestone | ✅ (same as P1-S3) | ✅ | ✅ | — |
| P5-S3 Update milestone amount | ✅ (same as P1-S4) | ✅ | ✅ | — |
| P5-S4 Cancel milestone | ✅ (same as P1-S5) | ✅ | ✅ | — |
| P5-S5 Version history | ✅ MilestonePanel | ✅ | ⚠️ Partial | — |
| P5-S6 SAP actuals matched to project | ⚠️ Via reconciliation status | ✅ | ⚠️ Partial | Extended priority |
| P5-S7 Remaining budget | ✅ Reconciliation summary | ✅ | ✅ | — |
| P5-S8 Budget report filter | ✅ Budget Report filter input | ✅ | ✅ | — |
| P5-S9 Contract audit trail | ✅ Audit Trail button on ContractDetail | ✅ | ✅ | — |
| P6-S1 User management | ✅ /admin/users | ✅ | ✅ | — |
| P6-S2 Fiscal year management | ✅ /admin/fiscal-years | ✅ | ✅ | — |
| P6-S3 Reference data management | ✅ /admin/reference-data | ✅ | ✅ | — |
| P6-S4 Reconciliation categories | ✅ /admin/reference-data tab | ✅ | ✅ | — |
| P6-S5 System config | ✅ /settings (data-driven) | ✅ | ✅ | — |
| P6-S6 Audit log viewer | ✅ /admin/audit | ✅ | ✅ | — |
| P6-S7 Audit log export | ✅ Export CSV link | ✅ | ✅ | — |
| P6-S8 Milestone version history | ✅ MilestonePanel | ✅ | ⚠️ Partial | — |
