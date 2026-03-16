# Spec 20 — E2E Scenario Matrix & Two-Tier Playwright Test Strategy

> Living document. Tests reference scenario IDs defined here. When a new scenario is added to
> `17-personas-and-use-cases.md`, a corresponding row must be added to Section 4 and the summary
> table in Section 6. When a test is implemented, update the status in Section 6.

---

## 1. Overview

This document defines the end-to-end (E2E) test strategy for the Ledger frontend using a
**two-tier Playwright model**. The goal is to provide both fast, developer-friendly feedback and
genuine confidence that the full stack — UI, API, database — works together correctly.

### Why Two Tiers?

Tier 1 (mocked) tests intercept every API call with `page.route()`. They run fast and catch UI
bugs: broken navigation, form validation errors, components that render incorrectly, state that
does not update after a mutation. But mocks always succeed. A Tier 1 test cannot catch a bug
where the backend silently ignores a request, stores data in the wrong table, or where the UI
reads data from a different endpoint than where it was written.

Tier 2 (real backend) tests run against the actual Docker Compose stack. They catch exactly the
class of bugs that Tier 1 hides — for example, the known dashboard visibility bug documented in
Section 3, where contracts created via `POST /contracts` do not appear on the dashboard because
the dashboard reads from `GET /reports/variance`, which only includes contracts with milestones.

### How Scenario IDs Map to Test Files

Each scenario from `17-personas-and-use-cases.md` has a stable ID (e.g., `P1-S1`). Test files are
named using that ID so coverage is unambiguous:

- Tier 1: `frontend/e2e/p1-s1-create-contract.spec.ts`
- Tier 2: `frontend/e2e/tier2/p1-s1-create-contract.tier2.spec.ts`

When multiple closely related scenarios share a single workflow, they may be grouped in one file
(e.g., `p2-s2-s3-review-import.spec.ts`), but the scenario IDs must appear in the `describe`
block names.

---

## 2. Test Tier Definitions

### Tier 1 — Mocked (Fast)

| Property | Value |
|----------|-------|
| Tool | Playwright with `page.route()` intercepting all API calls |
| Location | `frontend/e2e/*.spec.ts` |
| Purpose | Validate UI structure, form interactions, navigation, and visual state changes |
| Runs in | CI on every PR; target runtime < 2 minutes total |
| Limitation | Cannot catch backend data persistence bugs — mocks always return what the test tells them to return |

**Stateful mock pattern:** When a Tier 1 test performs a POST or PATCH mutation, the subsequent
GET mocks for the same resource must return updated data. For example, after intercepting
`POST /contracts` and returning a new contract, the mock for `GET /contracts` must include that
contract in its response array. Tests that fail to update the GET mock after a mutation are
testing a scenario that cannot exist in production.

### Tier 2 — Real Backend (Integration)

| Property | Value |
|----------|-------|
| Tool | Playwright running against the real Docker Compose stack |
| Location | `frontend/e2e/tier2/*.spec.ts` |
| Purpose | Validate that data created in the UI is actually persisted and reflected across all views |
| Runs in | CI on merge to main, nightly schedule, or locally via `npm run test:e2e:tier2` |
| Precondition | Docker stack running with a clean test database (see Section 5) |

---

## 3. Known Design Issue: Dashboard Contract Visibility

> **BUG (tracked in GitHub Issue #XX):** The Dashboard CONTRACT SUMMARY table is derived from the
> variance report (`GET /reports/variance`). The variance report only includes contracts that have
> milestones with planned amounts. A newly-created contract with no milestones is invisible on the
> dashboard immediately after creation.
>
> **Root Cause:** `Dashboard.tsx` builds `contractSummary` from `variance.rows` rather than
> fetching `GET /contracts` separately. Contracts with no milestones produce no variance rows and
> are therefore omitted.
>
> **Fix Required:** Dashboard should fetch `GET /contracts` and merge with variance data so all
> contracts appear. Variance data provides the KPI columns (Planned, Actuals, Variance); contracts
> with no milestones default to 0 for all three columns.
>
> **Impact on Tier 2 tests:** The P1-S1 Tier 2 scenario asserts that a newly created contract
> appears in the Dashboard CONTRACT SUMMARY. This assertion will **FAIL** until the above fix is
> applied. This is intentional — the failing Tier 2 test serves as a regression guard. Do not skip
> or work around this assertion; fix the root cause instead.

---

## 4. Scenario Matrix

---

### P1 — Finance Manager / Budget Owner

---

#### P1-S1: Create New Contract

| Property | Value |
|----------|-------|
| Scenario ID | P1-S1 |
| Description | Finance Manager creates a new vendor contract |
| Tier 1 file | `frontend/e2e/p1-s1-create-contract.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p1-s1-create-contract.tier2.spec.ts` |

**Actions**

1. Navigate to `/contracts` (or Dashboard).
2. Click the "+ New Contract" button.
3. Fill **Name** = `Test Vendor Contract`, **Vendor** = `Test Corp`, **Owner** = `Finance Manager`.
4. Click "Create Contract".

**Tier 1 Validations**

- The application navigates to `/contracts/{id}` (URL changes, no error page).
- The contract name `Test Vendor Contract` is visible in the ContractDetail page header.
- The vendor `Test Corp` is visible in the ContractDetail metadata section.
- The PROJECTS table is visible and initially empty (or shows a "No projects" placeholder).
- Stateful mock: after POST, the GET `/contracts` mock must include the new contract.

**Tier 2 Additional Validations**

1. After creation, visit `/` (Dashboard).
2. Assert `Test Vendor Contract` appears in the Dashboard CONTRACT SUMMARY table (this will fail until the dashboard bug in Section 3 is fixed).
3. Make a direct `GET /api/v1/contracts` request (via Playwright `request` context) and assert the response JSON contains a contract with `name = "Test Vendor Contract"`.
4. Reload `/contracts/{id}` from a fresh browser context and assert the contract name and vendor are still displayed correctly.

---

#### P1-S2: Add Project to Contract

| Property | Value |
|----------|-------|
| Scenario ID | P1-S2 |
| Description | Finance Manager adds a project to an existing contract |
| Tier 1 file | `frontend/e2e/p1-s2-add-project.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p1-s2-add-project.tier2.spec.ts` |

**Actions**

1. Navigate to a ContractDetail page (`/contracts/{id}`).
2. Click "+ Add Project".
3. Fill **Project ID**, **Name**, and **WBSE** in the drawer form.
4. Click "Add Project".

**Tier 1 Validations**

- The drawer closes after submission.
- The new project name appears in the PROJECTS table on ContractDetail.
- Stateful mock: after POST, the GET `/contracts/{id}/projects` mock returns the updated project list.

**Tier 2 Additional Validations**

1. After the drawer closes, hard-reload the ContractDetail page (`/contracts/{id}`).
2. Assert the new project name still appears in the PROJECTS table (confirms persistence).
3. Click the project row to navigate to ProjectDetail (`/projects/{id}`).
4. Assert the project name appears in the ProjectDetail page header.

---

#### P1-S3: Create Milestone

| Property | Value |
|----------|-------|
| Scenario ID | P1-S3 |
| Description | Finance Manager creates a milestone on a project |
| Tier 1 file | `frontend/e2e/p1-s3-create-milestone.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p1-s3-create-milestone.tier2.spec.ts` |

**Actions**

1. Navigate to a ProjectDetail page (`/projects/{id}`).
2. Click "+ Add Milestone".
3. Fill **Name**, **Amount**, and select a **Fiscal Period** in the drawer form.
4. Click "Add Milestone".

**Tier 1 Validations**

- The drawer closes after submission.
- The new milestone name appears in the milestones table on ProjectDetail.
- The amount and fiscal period are displayed in the milestone row.
- Stateful mock: after POST, the GET `/projects/{id}/milestones` mock includes the new milestone.

**Tier 2 Additional Validations**

1. Hard-reload ProjectDetail (`/projects/{id}`).
2. Assert the new milestone name still appears in the milestones table.
3. Assert the amount and fiscal period are still displayed correctly.

---

#### P1-S4: Adjust Milestone Planned Amount

| Property | Value |
|----------|-------|
| Scenario ID | P1-S4 |
| Description | Finance Manager adds a new version to a milestone to adjust its planned amount |
| Tier 1 file | `frontend/e2e/p1-s4-adjust-milestone.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p1-s4-adjust-milestone.tier2.spec.ts` |

**Actions**

1. Navigate to ProjectDetail (`/projects/{id}`).
2. Click a milestone row to expand it.
3. Click "+ New Version".
4. Fill the new **Amount** and a **Reason** for the change.
5. Click "Save Version".

**Tier 1 Validations**

- The version form collapses after save.
- The VERSION HISTORY section within the expanded milestone shows 2 entries (original + new version).
- The updated amount is displayed in the milestone row (or summary section).
- Stateful mock: after POST, the GET for milestone versions returns both versions.

**Tier 2 Additional Validations**

1. Hard-reload ProjectDetail (`/projects/{id}`).
2. Expand the same milestone row.
3. Assert VERSION HISTORY shows exactly 2 entries.
4. Assert the displayed planned amount reflects the latest (updated) version.

---

#### P1-S5 / P5-S4: Cancel Milestone

| Property | Value |
|----------|-------|
| Scenario ID | P1-S5, P5-S4 |
| Description | Finance Manager or Project Manager cancels a milestone that is no longer in scope |
| Tier 1 file | `frontend/e2e/p1-s5-cancel-milestone.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p1-s5-cancel-milestone.tier2.spec.ts` |

**Actions**

1. Navigate to ProjectDetail (`/projects/{id}`).
2. Click a milestone row to expand it.
3. Click "Cancel Milestone".
4. Fill the **Reason** field in the confirmation dialog.
5. Click "Confirm Cancel".

**Tier 1 Validations**

- The confirmation dialog closes.
- The milestone row in the milestones table shows `CANCELLED` status (badge or label).
- Stateful mock: after PATCH, the GET for this milestone returns `status: "CANCELLED"`.

**Tier 2 Additional Validations**

1. Hard-reload ProjectDetail (`/projects/{id}`).
2. Assert the milestone row still shows `CANCELLED` status.

---

#### P1-S6 / P4-S1: View Dashboard KPI Cards

| Property | Value |
|----------|-------|
| Scenario ID | P1-S6, P4-S1 |
| Description | Finance Manager or Finance Leadership views the portfolio dashboard KPI cards |
| Tier 1 file | `frontend/e2e/p1-s6-dashboard-kpis.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p1-s6-dashboard-kpis.tier2.spec.ts` |

**Actions**

1. Navigate to `/` (Dashboard).

**Tier 1 Validations**

- The `TOTAL BUDGET` KPI card is visible and displays a numeric value.
- The `TOTAL ACTUALS` KPI card is visible and displays a numeric value.
- The `OVERALL VARIANCE` KPI card is visible and displays a numeric value.
- The CONTRACT SUMMARY table is visible and contains at least one row.
- Each row in CONTRACT SUMMARY shows: contract name, planned amount, actuals, variance, and a status indicator.

**Tier 2 Additional Validations**

1. Make a direct `GET /api/v1/reports/variance` request via Playwright `request` context.
2. Sum the `planned`, `actuals`, and `variance` fields across all rows.
3. Assert the TOTAL BUDGET KPI card value matches the summed `planned` total.
4. Assert the TOTAL ACTUALS KPI card value matches the summed `actuals` total.
5. Assert the OVERALL VARIANCE KPI card value matches the summed `variance` total.

---

#### P1-S7 / P4-S8: Drill-Down Navigation

| Property | Value |
|----------|-------|
| Scenario ID | P1-S7, P4-S8 |
| Description | User drills from dashboard to contract to project to milestone |
| Tier 1 file | `frontend/e2e/p1-s7-drilldown-navigation.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to `/` (Dashboard).
2. Click a contract row in the CONTRACT SUMMARY table.
3. On ContractDetail, click a project row in the PROJECTS table.
4. On ProjectDetail, verify milestones are displayed.

**Tier 1 Validations**

- Clicking the contract row navigates to `/contracts/{id}` (URL changes).
- ContractDetail page shows the contract name in the header and a PROJECTS table with at least one row.
- Clicking the project row navigates to `/projects/{id}`.
- ProjectDetail page shows the project name in the header and a milestones table.

**Note:** Navigation correctness is fully testable with mocks. Tier 2 not required.

---

#### P1-S9: Adjust Reconciliation Tolerance

| Property | Value |
|----------|-------|
| Scenario ID | P1-S9 |
| Description | Finance Manager updates the reconciliation tolerance threshold in system settings |
| Tier 1 file | `frontend/e2e/p1-s9-tolerance-setting.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p1-s9-tolerance-setting.tier2.spec.ts` |

**Actions**

1. Navigate to `/settings`.
2. Locate the **Tolerance (%)** configuration row.
3. Change the value (e.g., from `5` to `7`).
4. Fill the **Reason** field if prompted.
5. Click "Save".

**Tier 1 Validations**

- No error message appears after save.
- The settings row updates to reflect the new value, or the drawer/modal closes without error.
- Stateful mock: after PATCH, the GET `/config` mock returns the updated value.

**Tier 2 Additional Validations**

1. After save, make a direct `GET /api/v1/config` request via Playwright `request` context.
2. Assert the response contains the tolerance key with the updated value (e.g., `7`).

---

#### P1-S10 / P4-S4: Use Time Machine

| Property | Value |
|----------|-------|
| Scenario ID | P1-S10, P4-S4 |
| Description | User sets the time machine date to view the plan as it appeared on a prior date |
| Tier 1 file | `frontend/e2e/p1-s10-time-machine.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to any page with the time machine input visible.
2. Enter a past date in the time machine date field.
3. Observe the banner that appears.
4. Click the "Reset" button to clear the date.

**Tier 1 Validations**

- After entering a date, a banner appears indicating the active as-of date.
- The banner contains the entered date.
- A "Reset" button is visible in the banner.
- Clicking "Reset" hides the banner and clears the date input.

**Note:** The time machine passes a date parameter to API requests. Tier 1 route interception can
validate that requests include the correct `asOfDate` query parameter. Tier 2 is not required
because the API-level behavior is covered by backend integration tests.

---

#### P1-S12: Close a Contract

| Property | Value |
|----------|-------|
| Scenario ID | P1-S12 |
| Description | Finance Manager closes or terminates a contract that has ended |
| Tier 1 file | `frontend/e2e/p1-s12-close-contract.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p1-s12-close-contract.tier2.spec.ts` |

**Actions**

1. Navigate to ContractDetail (`/contracts/{id}`).
2. Click "Edit" (or the status change control).
3. Change the status to `CLOSED`.
4. Fill the **Reason** field.
5. Click "Save Changes".

**Tier 1 Validations**

- The edit drawer/modal closes after save.
- The contract status in the ContractDetail header shows `CLOSED`.
- Stateful mock: after PATCH, the GET `/contracts/{id}` mock returns `status: "CLOSED"`.

**Tier 2 Additional Validations**

1. Hard-reload ContractDetail (`/contracts/{id}`).
2. Assert the contract status still shows `CLOSED` in the header.

---

### P2 — SAP Data Administrator

---

#### P2-S1: Upload SAP CSV

| Property | Value |
|----------|-------|
| Scenario ID | P2-S1 |
| Description | SAP Data Administrator uploads a SAP CSV export file |
| Tier 1 file | `frontend/e2e/p2-s1-upload-sap-csv.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p2-s1-upload-sap-csv.tier2.spec.ts` |

**Actions**

1. Navigate to `/import`.
2. Use the file input to select and upload a valid SAP CSV test fixture.

**Tier 1 Validations**

- After upload, the import history table shows a new entry with `STAGED` status.
- The entry displays the uploaded filename.

**Tier 2 Additional Validations**

1. Make a direct `GET /api/v1/imports` request and assert the response contains the newly uploaded import with `status: "STAGED"`.

---

#### P2-S2 / P2-S3: Review Import Summary and Lines

| Property | Value |
|----------|-------|
| Scenario ID | P2-S2, P2-S3 |
| Description | SAP Data Administrator reviews the import summary and inspects individual lines |
| Tier 1 file | `frontend/e2e/p2-s2-s3-review-import.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p2-s2-s3-review-import.tier2.spec.ts` |

**Actions**

1. Navigate to `/import`.
2. Click a `STAGED` import row to open the import review page.
3. Observe the summary section and the lines table.

**Tier 1 Validations**

- The import review page loads without error.
- Summary counts are visible: **New lines**, **Duplicate lines**, and **Error lines** (counts may be zero but labels must be present).
- The lines table is populated with at least one row showing line-level data (vendor, amount, period, etc.).

**Tier 2 Additional Validations**

1. Make a direct `GET /api/v1/imports/{id}/lines` request.
2. Assert the count of lines in the API response matches the count displayed in the lines table.
3. Assert the "New" count shown in the UI summary matches the count of lines with `status: "NEW"` in the API response.

---

#### P2-S4: Commit Import

| Property | Value |
|----------|-------|
| Scenario ID | P2-S4 |
| Description | SAP Data Administrator commits a valid staged import |
| Tier 1 file | `frontend/e2e/p2-s4-commit-import.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p2-s4-commit-import.tier2.spec.ts` |

**Actions**

1. Navigate to the import review page for a `STAGED` import.
2. Click "Commit Import".

**Tier 1 Validations**

- The application navigates back to `/import`.
- The committed import shows `COMMITTED` status in the history table.
- Stateful mock: after POST commit, the GET `/imports` mock returns the import with `status: "COMMITTED"`.

**Tier 2 Additional Validations**

1. Make a direct `GET /api/v1/imports/{id}` request and assert `status: "COMMITTED"`.
2. Make a direct `GET /api/v1/reconciliation/unreconciled` request and assert it contains actual lines from the committed import.

---

#### P2-S5: Reject Import

| Property | Value |
|----------|-------|
| Scenario ID | P2-S5 |
| Description | SAP Data Administrator rejects a bad import |
| Tier 1 file | `frontend/e2e/p2-s5-reject-import.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p2-s5-reject-import.tier2.spec.ts` |

**Actions**

1. Navigate to the import review page for a `STAGED` import.
2. Click "Reject Import".
3. Confirm the rejection in the confirmation dialog.

**Tier 1 Validations**

- The confirmation dialog closes.
- The application navigates back to `/import`.
- Stateful mock: after POST reject, the GET `/imports` mock returns the import with `status: "REJECTED"`.

**Tier 2 Additional Validations**

1. Make a direct `GET /api/v1/imports/{id}` request and assert `status: "REJECTED"`.
2. Make a direct `GET /api/v1/reconciliation/unreconciled` request and assert it does **not** contain actual lines from the rejected import.

---

#### P2-S6: View Import History

| Property | Value |
|----------|-------|
| Scenario ID | P2-S6 |
| Description | SAP Data Administrator views the history of all past imports |
| Tier 1 file | `frontend/e2e/p2-s6-import-history.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to `/import`.

**Tier 1 Validations**

- The import history table is visible.
- Each row shows: filename, status badge, and import date.
- Multiple statuses (STAGED, COMMITTED, REJECTED) appear correctly styled.

**Note:** History display is UI rendering only. Tier 2 not required.

---

### P3 — Reconciliation Specialist

---

#### P3-S1 / P3-S2: View Unreconciled Actuals and Select

| Property | Value |
|----------|-------|
| Scenario ID | P3-S1, P3-S2 |
| Description | Reconciliation Specialist views unreconciled actuals and selects one to see candidates |
| Tier 1 file | `frontend/e2e/p3-s1-s2-view-and-select-actuals.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to `/reconcile`.
2. Observe the UNRECONCILED ACTUALS panel on the left.
3. Click one of the actual rows.

**Tier 1 Validations**

- The UNRECONCILED ACTUALS panel is visible and contains at least one row.
- Each actual row shows vendor name, amount, and fiscal period.
- After clicking an actual row, a candidate milestones panel appears on the right.
- The candidate panel shows at least one milestone candidate with a match score or ranking indicator.

---

#### P3-S3: Reconcile Actual to Milestone

| Property | Value |
|----------|-------|
| Scenario ID | P3-S3 |
| Description | Reconciliation Specialist reconciles an actual to a milestone |
| Tier 1 file | `frontend/e2e/p3-s3-reconcile.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p3-s3-reconcile.tier2.spec.ts` |

**Actions**

1. Navigate to `/reconcile`.
2. Click an actual in the UNRECONCILED ACTUALS panel.
3. Click a candidate milestone in the candidate panel.
4. Click "Reconcile".

**Tier 1 Validations**

- The reconciled actual is removed from the UNRECONCILED ACTUALS panel.
- A RECENTLY RECONCILED section appears showing the reconciled actual.
- An "Undo" button is visible in the RECENTLY RECONCILED section.
- Stateful mock: after POST reconcile, the GET `/reconciliation/unreconciled` mock excludes the reconciled actual.

**Tier 2 Additional Validations**

1. Make a direct `GET /api/v1/reconciliation/unreconciled` request and assert the reconciled actual is no longer in the list.
2. Navigate to the ProjectDetail page for the milestone's project.
3. Expand the milestone row and assert the RECONCILIATION SUMMARY section shows the reconciled actual (or shows an updated reconciled count or Remaining amount reflecting the reconciliation).

---

#### P3-S4: Undo Reconciliation

| Property | Value |
|----------|-------|
| Scenario ID | P3-S4 |
| Description | Reconciliation Specialist undoes an incorrect reconciliation |
| Tier 1 file | `frontend/e2e/p3-s4-undo-reconciliation.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p3-s4-undo-reconciliation.tier2.spec.ts` |

**Actions**

1. Starting from the state after a successful reconciliation (RECENTLY RECONCILED section visible).
2. Click "Undo" on the recently reconciled entry.
3. Fill the **Reason** field in the confirmation dialog.
4. Click "Confirm Undo".

**Tier 1 Validations**

- The RECENTLY RECONCILED section disappears (or the undone entry is removed from it).
- Stateful mock: after POST undo, the GET `/reconciliation/unreconciled` mock includes the actual again.

**Tier 2 Additional Validations**

1. Make a direct `GET /api/v1/reconciliation/unreconciled` request and assert the actual has reappeared in the list.

---

#### P3-S5: Filter Actuals

| Property | Value |
|----------|-------|
| Scenario ID | P3-S5 |
| Description | Reconciliation Specialist filters the unreconciled actuals list |
| Tier 1 file | `frontend/e2e/p3-s5-filter-actuals.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to `/reconcile`.
2. Type a vendor name in the vendor filter input.
3. Select or enter an amount range in the amount filter.

**Tier 1 Validations**

- After typing in the vendor filter, only actuals with a matching vendor name are shown in the UNRECONCILED ACTUALS panel.
- Actuals with non-matching vendors are hidden.
- When the filter matches no actuals, a "No actuals match filter" message (or equivalent empty state) is shown.
- Clearing the filter restores the full list.

---

### P4 — Finance Leadership / Executive

---

#### P4-S2: Identify Over/Under Budget Contracts

| Property | Value |
|----------|-------|
| Scenario ID | P4-S2 |
| Description | Finance Leadership identifies which contracts are over or under budget from the dashboard |
| Tier 1 file | `frontend/e2e/p4-s2-budget-status-indicators.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to `/` (Dashboard).
2. Observe the CONTRACT SUMMARY table.

**Tier 1 Validations**

- Each row in the CONTRACT SUMMARY table has a status dot or color indicator.
- At least one row shows an "over budget" indicator (e.g., red dot) when the mock data includes an over-budget contract.
- At least one row shows an "under budget" or "on track" indicator (e.g., green dot) when mock data includes such a contract.

---

#### P4-S7: See Active Alerts

| Property | Value |
|----------|-------|
| Scenario ID | P4-S7 |
| Description | Finance Leadership views active alerts on the dashboard |
| Tier 1 file | `frontend/e2e/p4-s7-active-alerts.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to `/` (Dashboard).
2. Observe the alerts section.

**Tier 1 Validations**

- An alerts section is visible on the Dashboard.
- When mock data includes open accruals, the alerts section shows a count of open accruals.
- When mock data has no alerts, the section shows "No active alerts" (or equivalent empty state).

---

### P5 — Project / Contract Manager

---

#### P5-S1: View Milestones with Status and Remaining

| Property | Value |
|----------|-------|
| Scenario ID | P5-S1 |
| Description | Project Manager views all milestones for a project with status and remaining amounts |
| Tier 1 file | `frontend/e2e/p5-s1-view-milestones.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to ProjectDetail (`/projects/{id}`).
2. Observe the milestones table.
3. Click a milestone row to expand it.

**Tier 1 Validations**

- The milestones table shows at least one row.
- Each milestone row displays: name, planned amount, and fiscal period.
- Expanding a milestone row reveals a RECONCILIATION SUMMARY section.
- The RECONCILIATION SUMMARY shows a **Remaining** amount.
- The status of the milestone is visible in the expanded view or milestone row.

---

#### P5-S7: View Remaining Budget Per Milestone

| Property | Value |
|----------|-------|
| Scenario ID | P5-S7 |
| Description | Project Manager views remaining budget per milestone after reconciled actuals |
| Tier 1 file | `frontend/e2e/p5-s7-remaining-budget.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to ProjectDetail (`/projects/{id}`).
2. Click a milestone row to expand it.

**Tier 1 Validations**

- The expanded milestone row shows a RECONCILIATION SUMMARY section.
- The Remaining amount is displayed (numeric value).
- The reconciliation status is visible (e.g., UNRECONCILED, PARTIALLY_RECONCILED, RECONCILED).

---

#### P5-S8: Filter Budget Report to Contract/Project

| Property | Value |
|----------|-------|
| Scenario ID | P5-S8 |
| Description | Project Manager filters the budget report to a single contract or project |
| Tier 1 file | `frontend/e2e/p5-s8-filter-budget-report.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to `/reports/budget`.
2. Type a contract or project name in the filter input.

**Tier 1 Validations**

- After typing in the filter, only rows matching the search term are visible.
- Non-matching rows are hidden.
- The grand total row updates to reflect only the visible (filtered) rows.
- Clearing the filter restores all rows and the original grand total.

---

#### P5-S9: Audit Trail from Contract

| Property | Value |
|----------|-------|
| Scenario ID | P5-S9 |
| Description | Project Manager views the audit trail for a specific contract |
| Tier 1 file | `frontend/e2e/p5-s9-contract-audit-trail.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p5-s9-contract-audit-trail.tier2.spec.ts` |

**Actions**

1. Navigate to ContractDetail (`/contracts/{id}`).
2. Click the "Audit Trail" button.

**Tier 1 Validations**

- The application navigates to `/admin/audit`.
- The URL or page state includes `entityType=CONTRACT` and `entityId={id}` as pre-filled filter values.
- The audit log table is visible and shows entries (from mock data).

**Tier 2 Additional Validations**

1. Navigate to `/admin/audit` with filters for the specific contract.
2. Assert the audit log table shows at least one entry related to the contract (e.g., a CREATE event for this contract's ID).
3. Make a direct `GET /api/v1/audit?entityType=CONTRACT&entityId={id}` request and assert the response is non-empty.

---

### P6 — System Administrator

---

#### P6-S1: Create User

| Property | Value |
|----------|-------|
| Scenario ID | P6-S1 |
| Description | System Administrator creates a new user account |
| Tier 1 file | `frontend/e2e/p6-s1-create-user.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p6-s1-create-user.tier2.spec.ts` |

**Actions**

1. Navigate to `/admin/users`.
2. Click "+ New User".
3. Fill **Username**, **Display Name**, **Email**, and **Password**.
4. Click "Create User".

**Tier 1 Validations**

- The drawer closes after submission.
- Stateful mock: after POST, the GET `/users` mock includes the new user in the table.

**Tier 2 Additional Validations**

1. Hard-reload `/admin/users`.
2. Assert the new user appears in the user table with the correct username and display name.

---

#### P6-S1b: Deactivate User

| Property | Value |
|----------|-------|
| Scenario ID | P6-S1b |
| Description | System Administrator deactivates an existing user account |
| Tier 1 file | `frontend/e2e/p6-s1b-deactivate-user.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p6-s1b-deactivate-user.tier2.spec.ts` |

**Actions**

1. Navigate to `/admin/users`.
2. Click "Deactivate" for an active user.

**Tier 1 Validations**

- The "Deactivate" button changes to "Reactivate" for that user row.
- Stateful mock: after PATCH, the GET `/users` mock returns the user with `active: false`.

**Tier 2 Additional Validations**

1. Hard-reload `/admin/users`.
2. Assert the deactivated user shows as inactive (e.g., "Reactivate" button visible, or an inactive badge).

---

#### P6-S2: Create Fiscal Year

| Property | Value |
|----------|-------|
| Scenario ID | P6-S2 |
| Description | System Administrator adds a new fiscal year |
| Tier 1 file | `frontend/e2e/p6-s2-create-fiscal-year.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p6-s2-create-fiscal-year.tier2.spec.ts` |

**Actions**

1. Navigate to `/admin/fiscal-years`.
2. Click "+ New Fiscal Year".
3. Enter the fiscal year **Name**.
4. Click "Create".

**Tier 1 Validations**

- The form or drawer closes after submission.
- Stateful mock: after POST, the GET `/fiscal-years` mock includes the new fiscal year.

**Tier 2 Additional Validations**

1. Hard-reload `/admin/fiscal-years`.
2. Assert the new fiscal year appears in the table with the correct name.

---

#### P6-S3: Manage Reference Data

| Property | Value |
|----------|-------|
| Scenario ID | P6-S3 |
| Description | System Administrator adds a new entry to a reference data category |
| Tier 1 file | `frontend/e2e/p6-s3-manage-reference-data.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p6-s3-manage-reference-data.tier2.spec.ts` |

**Actions**

1. Navigate to `/admin/reference-data`.
2. Verify all 4 category tabs are present and switch between them.
3. On one tab, click "+ Add".
4. Fill the **Code** and **Display Name** fields.
5. Click "Save".

**Tier 1 Validations**

- All 4 category tabs are visible and clickable.
- Switching tabs updates the table contents.
- The add form or drawer closes after save.
- Stateful mock: after POST, the GET for that category returns the new entry.

**Tier 2 Additional Validations**

1. Hard-reload `/admin/reference-data` and navigate to the same tab.
2. Assert the new entry appears in the table with the correct Code and Display Name.

---

#### P6-S5: Update System Settings

| Property | Value |
|----------|-------|
| Scenario ID | P6-S5 |
| Description | System Administrator updates a system configuration value |
| Tier 1 file | `frontend/e2e/p6-s5-update-system-settings.spec.ts` |
| Tier 2 required | Yes |
| Tier 2 file | `frontend/e2e/tier2/p6-s5-update-system-settings.tier2.spec.ts` |

**Actions**

1. Navigate to `/settings`.
2. Change a configuration value.
3. Fill the **Reason** field if prompted.
4. Click "Save".

**Tier 1 Validations**

- Save completes without an error message.
- The settings row reflects the updated value, or the edit form closes.
- Stateful mock: after PATCH, the GET `/config` mock returns the updated value.

**Tier 2 Additional Validations**

1. Make a direct `GET /api/v1/config` request and assert the response contains the updated value for the changed key.

---

#### P6-S6 / P6-S7: Browse and Export Audit Log

| Property | Value |
|----------|-------|
| Scenario ID | P6-S6, P6-S7 |
| Description | System Administrator browses the audit log with filters and exports it as CSV |
| Tier 1 file | `frontend/e2e/p6-s6-s7-audit-log.spec.ts` |
| Tier 2 required | No |

**Actions**

1. Navigate to `/admin/audit`.
2. Select an entity type in the filter.
3. Type a username in the user filter.
4. Click "Export CSV".

**Tier 1 Validations**

- The audit log table is visible and contains rows (from mock data).
- Selecting an entity type filter narrows the table to rows matching that entity type.
- Typing a username filter narrows the table to rows matching that user.
- The "Export CSV" link or button has the correct `download` attribute set (e.g., `download="audit-log.csv"`).

---

## 5. Tier 2 Test Infrastructure

### Prerequisites

1. **Docker Compose running:** Execute `docker compose up` from the project root. The full stack
   must be healthy — backend API, PostgreSQL, and frontend nginx proxy — before Tier 2 tests run.

2. **Clean test database:** Each Tier 2 run must start from a known state. Options:
   - Preferred: `POST /api/v1/dev/seed` endpoint that resets the database to a standard fixture
     set (not available in production builds — guarded by an active profile flag, e.g.,
     `spring.profiles.active=test`).
   - Alternative: Docker Compose spins up a fresh database volume per run using
     `docker compose down -v && docker compose up`.

3. **Playwright configuration:** Set `baseURL: "http://localhost:80"` (the Docker frontend nginx
   reverse proxy) in `playwright.config.tier2.ts`.

4. **Authentication:** Tier 2 tests use the real login form. Each test (or a global setup fixture)
   must POST to the real `POST /api/v1/auth/login` endpoint with test credentials (e.g.,
   `username: "test-admin"`, `password: "test-password"`) and store the session cookie or JWT
   token for subsequent requests.

### Test Isolation

Each Tier 2 test that creates data must use unique identifiers to avoid interference with
other tests running in parallel or sequentially. Recommended approach:

- Prefix names with the test run ID or a timestamp: e.g., `Test Vendor Contract [run-{uuid}]`.
- Use Playwright's `test.use({ storageState: "..." })` to isolate auth state per worker.
- Avoid hardcoding IDs — capture the ID from the API response or page URL after creation.

### Run Commands

| Command | What it runs |
|---------|-------------|
| `npm run test:e2e` | Tier 1 only (`frontend/e2e/*.spec.ts`) |
| `npm run test:e2e:tier2` | Tier 2 only (`frontend/e2e/tier2/*.spec.ts`) |
| `npm run test:e2e:all` | Both tiers |

### CI Gates

| Gate | When | Tiers |
|------|------|-------|
| PR check | Every pull request | Tier 1 only |
| Merge to main | On merge | Tier 1 + Tier 2 |
| Nightly | Scheduled (e.g., 02:00 UTC) | Tier 1 + Tier 2 |

---

## 6. Scenario Coverage Matrix

| Scenario ID | Description | Tier 1 Status | Tier 2 Status | Tier 1 Test File |
|-------------|-------------|:---:|:---:|-----------------|
| P1-S1 | Create new contract | ✅ Implemented | 🔲 Not yet implemented | `p1-s1-create-contract.spec.ts` |
| P1-S2 | Add project to contract | ✅ Implemented | 🔲 Not yet implemented | `p1-s2-add-project.spec.ts` |
| P1-S3 | Create milestone | ✅ Implemented | 🔲 Not yet implemented | `p1-s3-create-milestone.spec.ts` |
| P1-S4 | Adjust milestone planned amount | ✅ Implemented | 🔲 Not yet implemented | `p1-s4-adjust-milestone.spec.ts` |
| P1-S5 | Cancel milestone | ✅ Implemented | 🔲 Not yet implemented | `p1-s5-cancel-milestone.spec.ts` |
| P1-S6 | View dashboard KPI cards | ✅ Implemented | 🔲 Not yet implemented | `p1-s6-dashboard-kpis.spec.ts` |
| P1-S7 | Drill-down navigation | ✅ Implemented | N/A | `p1-s7-drilldown-navigation.spec.ts` |
| P1-S9 | Adjust reconciliation tolerance | ✅ Implemented | 🔲 Not yet implemented | `p1-s9-tolerance-setting.spec.ts` |
| P1-S10 | Use time machine | ✅ Implemented | N/A | `p1-s10-time-machine.spec.ts` |
| P1-S12 | Close a contract | ✅ Implemented | 🔲 Not yet implemented | `p1-s12-close-contract.spec.ts` |
| P2-S1 | Upload SAP CSV | ✅ Implemented | 🔲 Not yet implemented | `p2-s1-upload-sap-csv.spec.ts` |
| P2-S2 | Review import summary | ✅ Implemented | 🔲 Not yet implemented | `p2-s2-s3-review-import.spec.ts` |
| P2-S3 | Inspect import lines | ✅ Implemented | 🔲 Not yet implemented | `p2-s2-s3-review-import.spec.ts` |
| P2-S4 | Commit import | ✅ Implemented | 🔲 Not yet implemented | `p2-s4-commit-import.spec.ts` |
| P2-S5 | Reject import | ✅ Implemented | 🔲 Not yet implemented | `p2-s5-reject-import.spec.ts` |
| P2-S6 | View import history | ✅ Implemented | N/A | `p2-s6-import-history.spec.ts` |
| P3-S1 | View unreconciled actuals | ✅ Implemented | N/A | `p3-s1-s2-view-and-select-actuals.spec.ts` |
| P3-S2 | Select actual, see candidates | ✅ Implemented | N/A | `p3-s1-s2-view-and-select-actuals.spec.ts` |
| P3-S3 | Reconcile actual to milestone | ✅ Implemented | 🔲 Not yet implemented | `p3-s3-reconcile.spec.ts` |
| P3-S4 | Undo reconciliation | ✅ Implemented | 🔲 Not yet implemented | `p3-s4-undo-reconciliation.spec.ts` |
| P3-S5 | Filter actuals | ✅ Implemented | N/A | `p3-s5-filter-actuals.spec.ts` |
| P4-S1 | View dashboard KPI cards | ✅ Implemented | 🔲 Not yet implemented | `p1-s6-dashboard-kpis.spec.ts` |
| P4-S2 | Identify over/under budget contracts | ✅ Implemented | N/A | `p4-s2-budget-status-indicators.spec.ts` |
| P4-S4 | Use time machine | ✅ Implemented | N/A | `p1-s10-time-machine.spec.ts` |
| P4-S7 | See active alerts | ✅ Implemented | N/A | `p4-s7-active-alerts.spec.ts` |
| P4-S8 | Drill-down navigation | ✅ Implemented | N/A | `p1-s7-drilldown-navigation.spec.ts` |
| P5-S1 | View milestones with status and remaining | ✅ Implemented | N/A | `p5-s1-view-milestones.spec.ts` |
| P5-S4 | Cancel milestone | ✅ Implemented | 🔲 Not yet implemented | `p1-s5-cancel-milestone.spec.ts` |
| P5-S7 | View remaining budget per milestone | ✅ Implemented | N/A | `p5-s7-remaining-budget.spec.ts` |
| P5-S8 | Filter budget report | ✅ Implemented | N/A | `p5-s8-filter-budget-report.spec.ts` |
| P5-S9 | Audit trail from contract | ✅ Implemented | 🔲 Not yet implemented | `p5-s9-contract-audit-trail.spec.ts` |
| P6-S1 | Create user | ✅ Implemented | 🔲 Not yet implemented | `p6-s1-create-user.spec.ts` |
| P6-S1b | Deactivate user | ✅ Implemented | 🔲 Not yet implemented | `p6-s1b-deactivate-user.spec.ts` |
| P6-S2 | Create fiscal year | ✅ Implemented | 🔲 Not yet implemented | `p6-s2-create-fiscal-year.spec.ts` |
| P6-S3 | Manage reference data | ✅ Implemented | 🔲 Not yet implemented | `p6-s3-manage-reference-data.spec.ts` |
| P6-S5 | Update system settings | ✅ Implemented | 🔲 Not yet implemented | `p6-s5-update-system-settings.spec.ts` |
| P6-S6 | Browse audit log | ✅ Implemented | N/A | `p6-s6-s7-audit-log.spec.ts` |
| P6-S7 | Export audit log | ✅ Implemented | N/A | `p6-s6-s7-audit-log.spec.ts` |

**Status key:**
- ✅ Implemented — test exists and is passing
- 🔲 Not yet implemented — test is defined in this spec but not yet written
- ⚠️ Partial — test exists but does not cover all validations listed in Section 4
- N/A — Tier 2 not required for this scenario (navigation-only or display-only)

---

## 7. Maintenance Notes

- **Adding a scenario:** When a new scenario is added to `17-personas-and-use-cases.md`, add a
  corresponding entry to Section 4 of this document (with Actions, Tier 1 Validations, and Tier 2
  Validations if applicable) and a row to the summary table in Section 6.

- **Implementing a test:** When a Tier 1 or Tier 2 test is written and passing, update the status
  column in Section 6 from 🔲 to ✅. If a test covers only some of the validations listed in
  Section 4, mark it as ⚠️ Partial and add a note describing what is missing.

- **Scenario IDs are stable:** Do not renumber or rename scenario IDs after they are defined. Test
  file names, describe block labels, GitHub issues, and this document all reference them. If a
  scenario is retired, mark it as deprecated in the table with a note rather than removing the row.

- **Source of truth:** This document is the source of truth for which scenarios have E2E test
  coverage. GitHub issue progress is tracked against scenario IDs from this document. When filing
  a bug found by a Tier 2 test, reference the scenario ID in the issue title
  (e.g., "P1-S1 Tier 2: contract not visible on dashboard — dashboard bug").

- **Known bug tracking:** The dashboard contract visibility bug documented in Section 3 is tracked
  as GitHub Issue #XX. The P1-S1 Tier 2 assertion for dashboard visibility must remain in the test
  file even while the bug is open. Do not skip or conditionally disable this assertion — a failing
  test is a prompt to fix the underlying issue, not to silence the test.
