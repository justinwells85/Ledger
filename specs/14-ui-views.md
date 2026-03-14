# UI Views & Wireframes

> React frontend view definitions. Each view maps to one or more API endpoints.
> All views respect the global time machine date when active.

---

## 1. Application Shell

### 1.1 Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│  LEDGER                                    [Time Machine: OFF ▾]   │
│                                            Justin Anderson         │
├──────────┬──────────────────────────────────────────────────────────┤
│          │                                                         │
│  NAV     │  CONTENT AREA                                           │
│          │                                                         │
│ Dashboard│                                                         │
│ Contracts│                                                         │
│ Import   │                                                         │
│ Reconcile│                                                         │
│ Reports  │                                                         │
│ Journal  │                                                         │
│ Settings │                                                         │
│          │                                                         │
└──────────┴──────────────────────────────────────────────────────────┘
```

### 1.2 Global Components

**Top Bar:**
- App name / logo (left)
- Time machine control (right) — date picker, when active shows banner: "Viewing as of: February 15, 2026" with a dismiss button
- Current user display (right)

**Side Navigation:**
- Dashboard
- Contracts (with nested project/milestone drill-down)
- Import (SAP file upload)
- Reconcile (reconciliation workspace)
- Reports (sub-menu: Budget, Variance, Reconciliation, Forecast, Funding, Open Accruals)
- Journal (ledger viewer)
- Settings (configuration)

### 1.3 Time Machine Banner

When active, a persistent colored banner appears below the top bar:

```
┌─────────────────────────────────────────────────────────────────────┐
│ ⏱ TIME MACHINE ACTIVE — Viewing as of: February 15, 2026  [Reset] │
└─────────────────────────────────────────────────────────────────────┘
```

All data on the page reflects the state as of that date. The banner is visible on every page.

---

## 2. Dashboard

**Route:** `/`
**API:** Multiple report endpoints

**Purpose:** At-a-glance portfolio health.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Dashboard                                    Fiscal Year: [FY26 ▾]│
├─────────────────┬───────────────────┬───────────────────────────────┤
│                 │                   │                               │
│  TOTAL BUDGET   │  TOTAL ACTUALS    │  OVERALL VARIANCE             │
│  $5,100,000     │  $3,200,000       │  +$1,900,000 (37.3%)         │
│                 │                   │  ████████████░░░░ 62.7% spent │
│                 │                   │                               │
├─────────────────┴───────────────────┴───────────────────────────────┤
│                                                                     │
│  CONTRACT SUMMARY                                                   │
│  ┌──────────────────┬──────────┬──────────┬──────────┬──────────┐  │
│  │ Contract         │ Planned  │ Actual   │ Variance │ Status   │  │
│  ├──────────────────┼──────────┼──────────┼──────────┼──────────┤  │
│  │ Globant ADM      │ $2.11M   │ $1.45M   │ +$660K   │ 🟢      │  │
│  │ Globant Web      │ $697K    │ $420K    │ +$277K   │ 🟢      │  │
│  │ Backend Services │ $696K    │ $510K    │ +$186K   │ 🟡      │  │
│  │ Globant QA       │ $762K    │ $580K    │ +$182K   │ 🟢      │  │
│  │ ...              │          │          │          │          │  │
│  └──────────────────┴──────────┴──────────┴──────────┴──────────┘  │
│  Click a row to navigate to contract detail                        │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ALERTS                                                             │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ ⚠ 3 open accruals aging > 60 days                           │  │
│  │ 🔴 2 milestones over budget                                  │  │
│  │ 📥 Last SAP import: March 1, 2026 (14 days ago)              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
├────────────────────────────────┬────────────────────────────────────┤
│  FUNDING BREAKDOWN             │  UNRECONCILED ACTUALS              │
│  ┌──────────┬────────┐        │  12 lines totaling $145,000        │
│  │ OPEX     │ $3.2M  │        │  [Go to Reconciliation →]          │
│  │ CAPEX    │ $850K  │        │                                    │
│  │ Other    │ $150K  │        │                                    │
│  └──────────┴────────┘        │                                    │
└────────────────────────────────┴────────────────────────────────────┘
```

**Interactions:**
- Fiscal year selector filters entire dashboard
- Contract summary rows are clickable → navigates to Contract Detail
- Alert items are clickable → navigates to relevant report
- "Go to Reconciliation" → navigates to Reconcile view

---

## 3. Contract Detail

**Route:** `/contracts/{contractId}`
**API:** `GET /contracts/{id}`, `GET /contracts/{id}/projects`, `GET /reports/variance`

```
┌─────────────────────────────────────────────────────────────────────┐
│  ← Contracts  /  Globant ADM                          [Edit]       │
├─────────────────────────────────────────────────────────────────────┤
│  Vendor: Globant    Owner: Rob Moore    Status: ACTIVE             │
│  Period: Oct 2025 — Sep 2026                                       │
├──────────┬──────────┬──────────┬──────────┬─────────────────────────┤
│ PLANNED  │ ACTUAL   │ VARIANCE │ FORECAST │ RECONCILED             │
│ $2.11M   │ $1.45M   │ +$660K   │ $2.10M   │ 87% of actuals        │
├──────────┴──────────┴──────────┴──────────┴─────────────────────────┤
│                                                                     │
│  BUDGET vs ACTUAL BY PERIOD              Period view: [Month ▾]    │
│  ┌────────┬────────┬────────┬────────┬────────┬────────┬────────┐  │
│  │        │ Oct    │ Nov    │ Dec    │ Jan    │ Feb    │ ...    │  │
│  ├────────┼────────┼────────┼────────┼────────┼────────┼────────┤  │
│  │Planned │175,824 │175,824 │175,824 │175,824 │175,824 │        │  │
│  │Actual  │175,824 │175,824 │175,824 │175,824 │175,824 │        │  │
│  │Variance│   0    │   0    │   0    │   0    │   0    │        │  │
│  └────────┴────────┴────────┴────────┴────────┴────────┴────────┘  │
│  Variance cells colored: green (under) / red (over) / neutral (0)  │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  PROJECTS                                           [+ Add Project]│
│  ┌──────────────┬────────────────────────────┬──────┬──────┬─────┐ │
│  │ Project ID   │ Name                       │ WBSE │ Fund │ $   │ │
│  ├──────────────┼────────────────────────────┼──────┼──────┼─────┤ │
│  │ PR13752      │ Photopass - SUS Break/Fix  │ 1174 │ OPEX │$1.2M│ │
│  │ PR15573      │ Photopass - Minor Enhance  │ 1205 │ OPEX │$185K│ │
│  │ PR01570      │ FY26 Sustainment           │ 1235 │ OPEX │$303K│ │
│  │ ...          │                            │      │      │     │ │
│  └──────────────┴────────────────────────────┴──────┴──────┴─────┘ │
│  Click a row to navigate to Project Detail                         │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  CHANGE HISTORY (recent)                          [View Full Log →]│
│  ┌────────────┬─────────────────────────────────────┬─────────────┐│
│  │ Date       │ Change                              │ By          ││
│  │ 2026-02-15 │ PR13752: Jan milestone reduced $5K  │ J. Anderson ││
│  │ 2026-01-15 │ PR18116: New milestone $42,800      │ B. Flechtner││
│  └────────────┴─────────────────────────────────────┴─────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. Project Detail

**Route:** `/projects/{projectId}`
**API:** `GET /projects/{id}`, `GET /projects/{id}/milestones`

```
┌─────────────────────────────────────────────────────────────────────┐
│  ← Globant ADM  /  PR13752 — Photopass SUS Break/Fix   [Edit]     │
├─────────────────────────────────────────────────────────────────────┤
│  WBSE: 1174905.SU.ES    Funding: OPEX    Status: ACTIVE           │
├──────────┬──────────┬──────────┬────────────────────────────────────┤
│ PLANNED  │ ACTUAL   │ VARIANCE │                                   │
│ $1.23M   │ $850K    │ +$377K   │                                   │
├──────────┴──────────┴──────────┴────────────────────────────────────┤
│                                                                     │
│  MILESTONES                                      [+ Add Milestone] │
│  ┌──────────────────┬─────────┬────────┬────────┬────────┬───────┐ │
│  │ Milestone        │ Period  │Planned │ Actual │Remaing │Status │ │
│  ├──────────────────┼─────────┼────────┼────────┼────────┼───────┤ │
│  │ Oct Sustainment  │ Oct '25 │112,129 │112,129 │      0 │  🟢  │ │
│  │ Nov Sustainment  │ Nov '25 │109,947 │109,947 │      0 │  🟢  │ │
│  │ Dec Sustainment  │ Dec '25 │109,947 │109,947 │      0 │  🟢  │ │
│  │ Jan Sustainment  │ Jan '26 │ 67,147 │ 72,000 │ -4,853 │  🔴  │ │
│  │ Feb Sustainment  │ Feb '26 │ 90,627 │ 85,000 │  5,627 │  🟢  │ │
│  │ ...              │         │        │        │        │       │ │
│  └──────────────────┴─────────┴────────┴────────┴────────┴───────┘ │
│  Click a row to expand milestone detail inline                     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5. Milestone Detail (Inline Expansion or Modal)

**Route:** `/milestones/{milestoneId}` (or inline expand within Project Detail)
**API:** `GET /milestones/{id}`, `GET /milestones/{id}/versions`

```
┌─────────────────────────────────────────────────────────────────────┐
│  Jan Sustainment                              [+ New Version]      │
│  Period: January 2026    Planned: $67,147 (v2)                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  RECONCILIATION SUMMARY                                             │
│  ┌────────────┬────────────┬────────────┬────────────┐             │
│  │ Invoice    │ Accrual Net│ Allocation │ Total      │             │
│  │ $47,000    │ $25,000    │ $0         │ $72,000    │             │
│  └────────────┴────────────┴────────────┴────────────┘             │
│  Remaining: -$4,853 (OVER BUDGET)    Open Accruals: 1              │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  RECONCILED ACTUALS                                                 │
│  ┌───────────┬──────────┬───────────────────┬──────────┬──────────┐│
│  │ Date      │ Amount   │ Description       │ Category │ By       ││
│  ├───────────┼──────────┼───────────────────┼──────────┼──────────┤│
│  │ 2025-12-31│ +$25,000 │ Globant accrual   │ ACCRUAL  │ Brad     ││
│  │ 2026-01-31│ -$25,000 │ Accrual reversal  │ ACCR_REV │ Brad     ││
│  │ 2026-01-31│ +$25,000 │ Globant accrual   │ ACCRUAL  │ Brad     ││
│  │ 2026-01-15│ +$47,000 │ Invoice #12345    │ INVOICE  │ Justin   ││
│  └───────────┴──────────┴───────────────────┴──────────┴──────────┘│
│  Each row has an [Undo] action                                     │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  VERSION HISTORY                                                    │
│  ┌────┬────────────┬──────────┬─────────┬──────────────────────────┐│
│  │ V# │ Effective  │ Amount   │ Period  │ Reason                   ││
│  ├────┼────────────┼──────────┼─────────┼──────────────────────────┤│
│  │ 1  │ 2025-11-01 │ $109,947 │ Jan '26 │ Initial budget           ││
│  │ 2  │ 2026-01-10 │ $67,147  │ Jan '26 │ $42,800 reclass to 18116││
│  └────┴────────────┴──────────┴─────────┴──────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

---

## 6. SAP Import

**Route:** `/import`
**API:** `POST /imports/upload`, `GET /imports/{id}`, `GET /imports/{id}/lines`, `POST /imports/{id}/commit`, `POST /imports/{id}/reject`

### 6.1 Upload Step

```
┌─────────────────────────────────────────────────────────────────────┐
│  SAP Import                                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                                                               │  │
│  │        Drag and drop SAP export file here                     │  │
│  │              or click to browse                               │  │
│  │                                                               │  │
│  │        Accepted: .csv, .xlsx, .xls                            │  │
│  │                                                               │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  IMPORT HISTORY                                                     │
│  ┌────────────┬───────────────────┬─────┬─────┬──────┬──────────┐  │
│  │ Date       │ File              │ New │ Dup │ Err  │ Status   │  │
│  ├────────────┼───────────────────┼─────┼─────┼──────┼──────────┤  │
│  │ 2026-03-01 │ SAP_FY26_Mar.csv  │  45 │ 105 │   0  │COMMITTED│  │
│  │ 2026-02-01 │ SAP_FY26_Feb.csv  │  38 │  72 │   0  │COMMITTED│  │
│  │ 2026-01-15 │ SAP_FY26_Jan.csv  │  62 │   0 │   2  │COMMITTED│  │
│  └────────────┴───────────────────┴─────┴─────┴──────┴──────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 Review Step (after upload)

```
┌─────────────────────────────────────────────────────────────────────┐
│  SAP Import — Review                                                │
├─────────────────────────────────────────────────────────────────────┤
│  File: SAP_FY26_March.csv    Uploaded: Mar 14, 2026 10:30 AM      │
│                                                                     │
│  ┌───────────────┬───────────────┬───────────────┬───────────────┐  │
│  │ TOTAL LINES   │ NEW           │ DUPLICATES    │ ERRORS        │  │
│  │     150       │     45        │     105       │      0        │  │
│  └───────────────┴───────────────┴───────────────┴───────────────┘  │
│                                                                     │
│  New lines: Oct 2025 — Mar 2026    Total: $1,245,000               │
│                                                                     │
│  Filter: [All ▾] [New ▾] [Duplicates ▾] [Errors ▾]                │
│                                                                     │
│  ┌───────────┬──────────┬──────────────────┬───────┬──────────────┐│
│  │ Post Date │ Amount   │ Vendor           │ WBSE  │ Description  ││
│  ├───────────┼──────────┼──────────────────┼───────┼──────────────┤│
│  │ 2026-03-15│ $25,000  │ Globant S.A.     │ 1174..│ Invoice #..  ││
│  │ 2026-03-15│ -$18,000 │ Globant S.A.     │ 1174..│ Accrual rev..││
│  │ 2026-03-01│ $18,000  │ Globant S.A.     │ 1174..│ Accrual ..   ││
│  │ ...       │          │                  │       │              ││
│  └───────────┴──────────┴──────────────────┴───────┴──────────────┘│
│                                                                     │
│                                    [Reject Import]  [Commit Import]│
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. Reconciliation Workspace

**Route:** `/reconcile`
**API:** `GET /reconciliation/unreconciled`, `GET /reconciliation/candidates/{actualId}`, `POST /reconciliation`

### 7.1 Split-Panel Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│  Reconciliation                                                     │
├───────────────────────────────┬─────────────────────────────────────┤
│  UNRECONCILED ACTUALS         │  MATCH TO MILESTONE                 │
│                               │                                     │
│  Filters:                     │  (Select an actual to see           │
│  Period: [All ▾]              │   candidate milestones)             │
│  Vendor: [________]           │                                     │
│  Amount: [min] — [max]        │                                     │
│                               │                                     │
│  12 unreconciled ($145,000)   │                                     │
│                               │                                     │
│  ┌──────────┬────────┬──────┐ │                                     │
│  │ Date     │ Amount │Vendor│ │                                     │
│  ├──────────┼────────┼──────┤ │                                     │
│  │ 03/15/26 │$25,000 │Globan│ │                                     │
│  │ 03/15/26 │-18,000 │Globan│ │                                     │
│  │ 03/01/26 │$18,000 │Globan│ │                                     │
│  │►02/28/26 │$12,000 │CapGem│ │                                     │
│  │ 02/15/26 │ $8,500 │Globan│ │                                     │
│  │ ...      │        │      │ │                                     │
│  └──────────┴────────┴──────┘ │                                     │
│                               │                                     │
└───────────────────────────────┴─────────────────────────────────────┘
```

### 7.2 After Selecting an Actual (right panel populates)

```
┌───────────────────────────────┬─────────────────────────────────────┐
│  UNRECONCILED ACTUALS         │  MATCH TO MILESTONE                 │
│                               │                                     │
│  ...                          │  Selected: $12,000 — CapGemini      │
│  │►02/28/26 │$12,000 │CapGem│ │  Posted: Feb 28, 2026              │
│  ...                          │  Desc: "License allocation Q2"      │
│                               │                                     │
│                               │  CANDIDATES (sorted by relevance)   │
│                               │  ┌─────────────────────────────┐    │
│                               │  │ ★95 Cap Gemini              │    │
│                               │  │ Advanced Auto - Live Ops    │    │
│                               │  │ Feb '26 | Plan: $0          │    │
│                               │  │ Remaining: $0               │    │
│                               │  ├─────────────────────────────┤    │
│                               │  │ ★72 Globant Automation      │    │
│                               │  │ Advanced Auto - Live Ops    │    │
│                               │  │ Feb '26 | Plan: $66,660     │    │
│                               │  │ Remaining: $12,000          │    │
│                               │  ├─────────────────────────────┤    │
│                               │  │ ★45 MSA-ADM                 │    │
│                               │  │ Automation - Enhancements   │    │
│                               │  │ Feb '26 | Plan: $3,905      │    │
│                               │  │ Remaining: $3,905           │    │
│                               │  └─────────────────────────────┘    │
│                               │                                     │
│                               │  Or search: [________________]      │
│                               │                                     │
└───────────────────────────────┴─────────────────────────────────────┘
```

### 7.3 Assignment Dialog (after clicking a candidate)

```
┌─────────────────────────────────────────────────┐
│  Reconcile Actual to Milestone                  │
├─────────────────────────────────────────────────┤
│                                                 │
│  Actual:     $12,000 — CapGemini (Feb 28)       │
│  Milestone:  Advanced Auto - Live Ops (Feb '26) │
│  Contract:   Automation Support (Globant)       │
│                                                 │
│  Category:   ○ Invoice                          │
│              ○ Accrual                          │
│              ● Allocation                       │
│              ○ Accrual Reversal                 │
│                                                 │
│  Notes:      [License allocation for Q2_______] │
│              (optional)                         │
│                                                 │
│              [Cancel]              [Reconcile]  │
└─────────────────────────────────────────────────┘
```

---

## 8. Reports

### 8.1 Budget Plan Report

**Route:** `/reports/budget`
**API:** `GET /reports/budget`

```
┌─────────────────────────────────────────────────────────────────────┐
│  Budget Plan Report                                                 │
│  Fiscal Year: [FY26 ▾]  Group By: [Project ▾]  Period: [Month ▾]  │
│  Funding: [All ▾]  Contract: [All ▾]           [Export CSV]        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────┬────────┬────────┬────────┬───────┬─────┐ │
│  │ Contract / Project   │ Oct    │ Nov    │ Dec    │ ...   │Total│ │
│  ├──────────────────────┼────────┼────────┼────────┼───────┼─────┤ │
│  │▼Globant ADM          │175,824 │175,824 │175,824 │       │2.11M│ │
│  │  PR13752 Photopass   │112,129 │109,947 │109,947 │       │1.23M│ │
│  │  PR15573 Minor Enh   │ 15,445 │ 15,445 │ 15,445 │       │ 185K│ │
│  │  PR01570 Sustainment │ 25,250 │ 25,250 │ 25,250 │       │ 303K│ │
│  │  ...                 │        │        │        │       │     │ │
│  │▼Globant Web          │ 56,000 │ 63,000 │ 56,000 │       │ 697K│ │
│  │  PR13752 Photopass   │ 17,280 │ 18,100 │ 13,140 │       │ 156K│ │
│  │  ...                 │        │        │        │       │     │ │
│  ├──────────────────────┼────────┼────────┼────────┼───────┼─────┤ │
│  │ GRAND TOTAL          │575,409 │646,409 │575,409 │       │5.10M│ │
│  └──────────────────────┴────────┴────────┴────────┴───────┴─────┘ │
│                                                                     │
│  Rows expandable/collapsible. Contract level → Project level.      │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 Variance Report

**Route:** `/reports/variance`
**API:** `GET /reports/variance`

Same pivot layout as Budget Plan, but each cell shows planned / actual / variance with color coding. Clicking a cell drills into the reconciled actuals for that scope + period.

```
  Cell example:
  ┌────────────┐
  │ Plan: $25K │
  │ Act:  $27K │
  │ Var: -$2K  │  ← red background
  └────────────┘
```

### 8.3 Reconciliation Status Report

**Route:** `/reports/reconciliation`
**API:** `GET /reports/reconciliation-status`

Flat table (not pivot). Filterable by status.

### 8.4 Forecast Report

**Route:** `/reports/forecast`
**API:** `GET /reports/forecast`

Table with: Original Plan, Actuals YTD, Remaining Plan, Forecast Total, Delta.

### 8.5 Funding Source Summary

**Route:** `/reports/funding`
**API:** `GET /reports/funding-summary`

Simple table grouped by OPEX / CAPEX / OTHER_TEAM.

### 8.6 Open Accruals Report

**Route:** `/reports/accruals`
**API:** `GET /reports/open-accruals`

Table sorted by age (oldest first). Color-coded by status (OPEN / WARNING / CRITICAL).

---

## 9. Journal Viewer

**Route:** `/journal`
**API:** `GET /journal`

```
┌─────────────────────────────────────────────────────────────────────┐
│  Journal Ledger                                                     │
│  Date Range: [2026-01-01] to [2026-03-14]                          │
│  Type: [All ▾]  Contract: [All ▾]  User: [All ▾]                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────┬──────────────┬──────────────────────────────┬─────┐│
│  │ Date       │ Type         │ Description                  │ By  ││
│  ├────────────┼──────────────┼──────────────────────────────┼─────┤│
│  │ 2026-03-14 │ ACTUAL_IMPORT│ SAP: Doc# 5100012345         │ JA  ││
│  │ 2026-03-14 │ RECONCILE    │ Matched to Jan Sustainment   │ JA  ││
│  │ 2026-02-15 │ PLAN_ADJUST  │ Q2 scope cut: -$5,250        │ JA  ││
│  │ 2026-02-01 │ ACTUAL_IMPORT│ SAP: 38 lines committed      │ BF  ││
│  └────────────┴──────────────┴──────────────────────────────┴─────┘│
│                                                                     │
│  Click a row to expand and see journal lines (debit/credit detail) │
│                                                                     │
│  Expanded:                                                          │
│  ┌──────────────────┬──────────┬──────────┬──────────┬────────────┐│
│  │ Account          │ Scope    │ Period   │ Debit    │ Credit     ││
│  ├──────────────────┼──────────┼──────────┼──────────┼────────────┤│
│  │ VARIANCE_RESERVE │ Glob ADM │ Jan '26  │ $5,250   │            ││
│  │ PLANNED          │ Glob ADM │ Jan '26  │          │ $5,250     ││
│  └──────────────────┴──────────┴──────────┴──────────┴────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

---

## 10. Settings

**Route:** `/settings`
**API:** `GET /config`, `PUT /config/{key}`

```
┌─────────────────────────────────────────────────────────────────────┐
│  Settings                                                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  RECONCILIATION TOLERANCE                                           │
│  ┌──────────────────────────┬────────────┬────────────────────────┐│
│  │ Setting                  │ Value      │                        ││
│  ├──────────────────────────┼────────────┼────────────────────────┤│
│  │ Tolerance (%)            │ [2___] %   │ Match if % within this ││
│  │ Tolerance ($)            │ [$50__]    │ Match if $ within this ││
│  └──────────────────────────┴────────────┴────────────────────────┘│
│                                                                     │
│  ACCRUAL AGING                                                      │
│  ┌──────────────────────────┬────────────┬────────────────────────┐│
│  │ Warning threshold        │ [60__] days│ Show warning indicator ││
│  │ Critical threshold       │ [90__] days│ Show critical indicator││
│  └──────────────────────────┴────────────┴────────────────────────┘│
│                                                                     │
│                                              [Save Changes]         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 11. Common UI Patterns

### 11.1 Forms (Create/Edit)

All create and edit forms use a side panel (drawer) or modal:
- Required fields marked with asterisk
- Validation errors shown inline below the field
- "Reason" field always visible and required for edits (not creates)

### 11.2 Tables

- Sortable columns (click header)
- Filterable via controls above table
- Pagination at bottom
- Row click navigates to detail or expands inline
- Export button (CSV) where applicable

### 11.3 Status Indicators

| Status | Visual |
|--------|--------|
| Under budget / Fully reconciled | Green dot or green background |
| Within tolerance | Light green |
| Approaching budget (>90%) | Yellow / amber |
| Over budget | Red dot or red background |
| Unmatched / No data | Gray |
| Accrual aging warning | Orange |
| Accrual aging critical | Red |

### 11.4 Navigation

Breadcrumb trail at the top of every page:
```
Dashboard / Globant ADM / PR13752 — Photopass / Jan Sustainment
```
Each segment is clickable.

### 11.5 Amounts

- Always formatted with commas: `$1,227,626.00`
- Negative values in parentheses and red: `($4,853.00)`
- Zero shown as `$0.00` or dash `—` depending on context

---

## 12. View-to-API Mapping

| View | Primary API Endpoints |
|------|----------------------|
| Dashboard | `GET /reports/variance`, `GET /reports/funding-summary`, `GET /reports/open-accruals`, `GET /reconciliation/unreconciled` |
| Contract Detail | `GET /contracts/{id}`, `GET /contracts/{id}/projects`, `GET /reports/variance?contractId=` |
| Project Detail | `GET /projects/{id}`, `GET /projects/{id}/milestones` |
| Milestone Detail | `GET /milestones/{id}`, `GET /milestones/{id}/versions` |
| SAP Import | `POST /imports/upload`, `GET /imports/{id}/lines`, `POST /imports/{id}/commit` |
| Reconciliation | `GET /reconciliation/unreconciled`, `GET /reconciliation/candidates/{id}`, `POST /reconciliation` |
| Budget Report | `GET /reports/budget` |
| Variance Report | `GET /reports/variance` |
| Recon Status Report | `GET /reports/reconciliation-status` |
| Forecast Report | `GET /reports/forecast` |
| Funding Report | `GET /reports/funding-summary` |
| Open Accruals Report | `GET /reports/open-accruals` |
| Journal Viewer | `GET /journal` |
| Settings | `GET /config`, `PUT /config/{key}` |

---

## 13. React Route Structure

```
/                           → Dashboard
/contracts                  → Contract list (redirect to dashboard for now)
/contracts/:contractId      → Contract Detail
/projects/:projectId        → Project Detail
/milestones/:milestoneId    → Milestone Detail (standalone view)
/import                     → SAP Import
/import/:importId           → Import Review
/reconcile                  → Reconciliation Workspace
/reports/budget             → Budget Plan Report
/reports/variance           → Variance Report
/reports/reconciliation     → Reconciliation Status Report
/reports/forecast           → Forecast Report
/reports/funding            → Funding Source Summary
/reports/accruals           → Open Accruals Report
/journal                    → Journal Viewer
/settings                   → Configuration
```
