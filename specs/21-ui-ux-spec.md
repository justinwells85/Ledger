# Spec 21 — UI/UX Specification

**Status:** Authoritative
**Governs:** All frontend implementation decisions
**Supersedes:** Spec 14 (UI Views) for all visual and interaction concerns
**Last updated:** 2026-03-15

---

## Table of Contents

1. [Purpose and Scope](#1-purpose-and-scope)
2. [Personas](#2-personas)
3. [Design System](#3-design-system)
4. [Information Architecture](#4-information-architecture)
5. [Interaction Patterns](#5-interaction-patterns)
6. [Page Specifications](#6-page-specifications)
7. [Common Components](#7-common-components)
8. [Data Display Conventions](#8-data-display-conventions)
9. [Responsive Behavior](#9-responsive-behavior)
10. [Implementation Notes](#10-implementation-notes)

---

## 1. Purpose and Scope

This document is the authoritative UI/UX specification for the Ledger frontend. It governs all visual design decisions, interaction patterns, page layouts, component behavior, and data display conventions.

Ledger is a financial planning and reconciliation tool used by a finance team to track vendor contract budgets, planned milestone payments, SAP actuals, reconciliation, and accruals. Everything is tied to fiscal periods at monthly granularity. The backend is complete and stable. This specification governs the frontend redesign only — no backend changes are implied.

When this specification conflicts with any prior specification (including Spec 14), this document takes precedence for all UI/UX matters.

---

## 2. Personas

The system serves five personas. Navigation is unified across all personas; the ordering and default landing page are optimized for the most common workflows.

**P1 — Budget Owner**
Plans and monitors vendor contracts and associated budgets. Determines specific milestones and tracks delivery against them. Implements updates based on changes in direction, scope, or available funding. Primary entry point: Portfolio Dashboard. Primary workflows: Contract Detail, Project Detail, Cashflow Table editing.

**P2 — Actuals Analyst** *(often a sub-role of the Budget Owner)*
Gathers actuals from SAP charged against projects and reconciles them against planned payments. Weekly cadence: pull SAP report, identify new invoices, reconcile against plan. Primary workflows: SAP Import, Reconciliation Workspace.

**P3 — Financial Controller**
Creates accruals and reversals for completed work lacking an invoice in a given month. Manages allocations to team budgets (labor, licenses). Enters forecast for planned expenses into overall financial systems. Monthly cadence. Primary workflows: Reconciliation Workspace, Reports Hub (Open Accruals, Variance tabs).

**P4 — Program Executive**
Monitors cross-program financial performance. Needs to understand disconnects between original plan and current state. Monthly cadence: review variance to plan, verify actuals entered, confirm accruals are happening as planned. Primarily a consumer — does not perform data entry. Primary entry point: Portfolio Dashboard. Primary workflows: Reports Hub.

**P5 — System Administrator**
Manages reference data for data entry and reporting. Handles credentials, general support, and user needs. Primary workflows: Settings, Users, Reference Data, Fiscal Years, Audit Log.

### Persona-to-Navigation Alignment

The nav item ordering reflects the priority of the most common users:

| Persona | Primary Path |
|---|---|
| P1 Budget Owner | Portfolio → Contracts → (edit inline) |
| P2 Actuals Analyst | Import → Reconciliation |
| P3 Financial Controller | Reconciliation → Reports (Open Accruals, Variance) |
| P4 Program Executive | Portfolio → Reports |
| P5 System Admin | Settings → Users → Reference Data |

---

## 2. Design System

### 2.1 Brand

McKinsey & Company visual identity — used as a clean, authoritative base. The design should feel institutional and information-dense rather than consumer-oriented. Color palette and typography may be adjusted in a later phase; these tokens are the source of truth during implementation.

### 2.2 Color Tokens

```css
/* Brand */
--color-navy:           #051C2C;   /* primary brand, nav background, key headings */
--color-blue:           #2251FF;   /* primary action, links, active states */
--color-blue-hover:     #1A3FCC;   /* hover state for primary blue */
--color-white:          #FFFFFF;
--color-surface:        #FAFAFA;   /* page background */
--color-surface-raised: #FFFFFF;   /* card/panel background */

/* Borders */
--color-border:         #E8E8E8;   /* table borders, dividers */
--color-border-strong:  #C8C8C8;   /* emphasized borders */

/* Text */
--color-text-primary:   #1A1A1A;   /* body text */
--color-text-secondary: #5C5C5C;   /* secondary labels, metadata */
--color-text-disabled:  #A0A0A0;   /* placeholder, disabled */
--color-text-inverse:   #FFFFFF;   /* text on navy background */

/* Status — subtle, not garish */
--color-positive:       #006633;   /* under budget, reconciled */
--color-positive-bg:    #F0F7F4;
--color-warning:        #8C5E00;   /* approaching budget, aging accrual */
--color-warning-bg:     #FFF8EC;
--color-negative:       #C0001E;   /* over budget, critical */
--color-negative-bg:    #FFF2F4;
--color-neutral:        #5C5C5C;   /* variance with explanation */
--color-neutral-bg:     #F5F5F5;

/* Variance states */
--color-variance-explained:   #5C5C5C;   /* variance exists, explanation on record */
--color-variance-unexplained: #C0001E;   /* variance exists, no explanation */
```

All colors must be referenced by token name in implementation. Hard-coded hex values are not permitted in component code.

### 2.3 Typography

```css
font-family: "Inter", -apple-system, BlinkMacSystemFont, sans-serif;
/*
 * Inter is the closest publicly available approximation to McKinsey Sans.
 * Load Inter from Google Fonts or bundle it locally.
 */

/* Size scale */
--font-size-xs:   11px;
--font-size-sm:   12px;
--font-size-base: 14px;
--font-size-md:   15px;
--font-size-lg:   18px;
--font-size-xl:   22px;
--font-size-2xl:  28px;

/* Weight */
--font-weight-regular:  400;
--font-weight-medium:   500;
--font-weight-semibold: 600;
--font-weight-bold:     700;

/* Line height */
--line-height-tight:   1.2;
--line-height-base:    1.5;
--line-height-relaxed: 1.7;
```

**Usage conventions:**
- Page headings: `--font-size-xl`, `--font-weight-semibold`, `--color-navy`
- Card/section headings: `--font-size-lg`, `--font-weight-semibold`, `--color-navy`
- Table column headers: `--font-size-sm`, `--font-weight-medium`, uppercase, letter-spacing `0.04em`, `--color-text-secondary`
- Table data: `--font-size-base`, `--font-weight-regular`, `--color-text-primary`
- KPI primary values: `--font-size-2xl`, `--font-weight-bold`, `--color-navy`
- Secondary/metadata text: `--font-size-sm`, `--color-text-secondary`

### 2.4 Spacing Scale

Base unit: 4px. All margins, padding, and gaps must use tokens from this scale.

```css
--space-1:   4px;
--space-2:   8px;
--space-3:  12px;
--space-4:  16px;
--space-5:  20px;
--space-6:  24px;
--space-8:  32px;
--space-10: 40px;
--space-12: 48px;
--space-16: 64px;
```

### 2.5 Elevation and Shadow

```css
--shadow-card:  0 1px 3px rgba(5,28,44,0.08), 0 1px 2px rgba(5,28,44,0.05);
--shadow-panel: 0 4px 16px rgba(5,28,44,0.12);
--shadow-modal: 0 8px 32px rgba(5,28,44,0.18);
```

### 2.6 Border Radius

```css
--radius-sm:  2px;
--radius-md:  4px;
--radius-lg:  8px;
--radius-xl: 12px;
```

Buttons: `--radius-md`. Cards: `--radius-lg`. Status badges: `--radius-md`. Inputs: `--radius-md`.

---

## 4. Information Architecture

### 4.1 Navigation

A single unified navigation is used for all roles. The team is small with mixed responsibilities; role-based nav hiding adds complexity with minimal benefit.

**Left sidebar — fixed, 220px wide:**

```
┌──────────────────────┐
│  ▪ LEDGER            │  ← navy bg (#051C2C), white text, 56px tall header
├──────────────────────┤
│                      │
│  Portfolio           │  ← dashboard/home
│  Contracts           │  ← contract list
│                      │
│  ── OPERATIONS ──    │  ← section divider, 11px uppercase, --color-text-disabled
│  Import              │
│  Reconciliation      │
│                      │
│  ── REPORTING ──     │
│  Reports             │
│  Journal             │
│                      │
│  ── ADMIN ──         │
│  Settings            │
│  Users               │
│  Reference Data      │
│  Audit Log           │
│                      │
├──────────────────────┤
│  [avatar] J. Anderson│  ← current user, bottom of sidebar
│           Sign out   │
└──────────────────────┘
```

**Nav item states:**

| State | Style |
|---|---|
| Default | `--color-text-inverse` at 80% opacity |
| Hover | Background `rgba(5,28,44,0.04)` |
| Active | Left border `4px solid --color-blue`, background `rgba(34,81,255,0.06)`, text `--color-navy` |

Section divider labels: 11px, uppercase, letter-spacing `0.08em`, `--color-text-disabled`, not clickable. Vertical padding `--space-3` above each section divider.

Nav item height: 40px. Item padding: `0 --space-4`. Icon (16px, optional) followed by label at `--font-size-base`.

User block at bottom: separated by a 1px `--color-border` line. Avatar is a 28px circle with initials. Display name at `--font-size-sm` semibold, "Sign out" at `--font-size-sm` regular. Clicking the entire block opens a small dropdown with "My Profile" and "Sign out".

### 4.2 Routes

```
/                         → Portfolio (Dashboard)
/contracts                → Contract List
/contracts/:contractId    → Contract Detail
/projects/:projectId      → Project Detail
/import                   → SAP Import (history + upload)
/import/:importId         → Import Review
/reconcile                → Reconciliation Workspace
/reports                  → Reports Hub (tabbed)
/journal                  → Journal Viewer
/settings                 → System Configuration
/admin/users              → User Management
/admin/reference-data     → Reference Data
/admin/fiscal-years       → Fiscal Year Management
/admin/audit              → Audit Log
```

All routes listed above are valid entry points (bookmarkable, shareable). The router must handle direct navigation to any route without requiring a redirect through `/`.

---

## 5. Interaction Patterns

### 5.1 Data Tables — General Principles

All financial tables follow Excel-like density: compact rows, all months visible in one horizontal view without scrolling when possible. Density-first, but with breathing room between contracts and projects.

**Row specifications:**
- Data row height: 36px
- Header row height: 44px
- Cell padding: 8px horizontal, 0px vertical (vertically centered within row height)

**Borders:**
- 1px `--color-border` horizontal only (no vertical cell borders)
- Exception: column group separators (e.g., between the milestone name column and the first month column) use `1px --color-border-strong`

**Typography:**
- Column headers: `--font-size-sm`, `--font-weight-medium`, uppercase, letter-spacing `0.04em`
- Data cells: `--font-size-base`, `--font-weight-regular`

**Alignment:**
- Numbers: right-aligned
- Text: left-aligned
- Status badges: left-aligned

**Sorting:**
Tables that support sorting show a sort icon (↑↓) in the column header. Active sort column shows the active direction icon only. Clicking an already-sorted column reverses the direction.

**Loading state:**
Skeleton rows (grey shimmer bars) replace content during initial load. Skeleton rows match the height of real rows.

**Error state:**
If a table fails to load, display an inline error message within the table area: "Could not load data. [Retry]" in `--color-negative`.

### 5.2 Inline Editing and Bottom Detail Card

This is the primary interaction model for all data modification in the cashflow tables. **There are no drawers or modals for editing existing records.**

**Trigger:**
User clicks any editable cell in a cashflow table.

**Cell response:**
The clicked cell immediately gains a `2px solid --color-blue` border and becomes directly editable (cursor placed in the field, existing value selected).

**Bottom Detail Card:**
Simultaneously, a panel slides up from the bottom of the viewport. The panel is 380px tall. It does not obscure the table; the page scrolls to ensure the active cell is visible above the panel. The panel uses `--shadow-panel` and `--color-surface-raised` background.

**Bottom Detail Card anatomy:**

```
┌─────────────────────────────────────────────────────────────────────┐
│  [Contract / Project name]  —  [Period]              [×] close      │
│  ─────────────────────────────────────────────────────────────────  │
│                                       │                             │
│  LEFT COLUMN                          │  RIGHT COLUMN              │
│  (form / primary action, ~55% width)  │  (read-only context, ~45%) │
│                                       │                             │
│  [form fields appropriate             │  [actuals table, version    │
│   to the clicked context]             │   history, or related data] │
│                                       │                             │
│                            [Cancel]  [Save Changes]                │
└─────────────────────────────────────────────────────────────────────┘
```

The vertical divider between left and right columns is `1px --color-border`.

**Content by context:**

| Clicked cell type | Left column | Right column |
|---|---|---|
| Milestone/cashflow amount | Amount field, reason field, period selector | Actuals matched to this milestone (invoice, accrual, reversal rows) |
| Variance cell | Comment/explanation textarea (pre-filled if reason exists) | Breakdown of what drives the variance |
| Status cell | Status selector, reason field | History of status changes (date, from, to, by) |
| Actuals cell (reconciliation) | Reconciliation form, category selector, notes | Candidate milestones sorted by relevance |

**Keyboard behavior:**
- `Enter`: saves and closes the card (same as clicking Save Changes)
- `Escape`: reverts unsaved changes and closes the card
- `Tab` within the card: moves between form fields

**Card close behavior:**
Clicking anywhere outside the card or on the × button reverts unsaved changes and closes. If the user has modified any field, a confirmation prompt appears: "Discard unsaved changes?" with [Keep Editing] and [Discard] actions.

**Save behavior:**
On save, the card closes, the table cell updates optimistically, and a success toast ("Changes saved") appears for 3 seconds in the top-right corner. On API error, the toast shows the error message in red and the card reopens with the user's values preserved.

### 5.3 Right-Side Creation Panel

For creating new records (new contract, new project, new milestone), a panel slides in from the right edge of the viewport. This is distinct from the Bottom Detail Card because there is no existing cell to click into.

**Specifications:**
- Width: 480px
- Height: full viewport height minus the top bar
- Background: `--color-surface-raised`
- Shadow: `--shadow-panel`
- A semi-transparent overlay (`rgba(5,28,44,0.24)`) covers the content behind the panel but does not block the sidebar nav

**Panel anatomy:**
```
┌─────────────────────────────────────────────────────────────────┐
│  [Panel title — e.g., "New Contract"]                 [×] close │
│  ─────────────────────────────────────────────────────────────  │
│                                                                 │
│  [form fields]                                                  │
│                                                                 │
│  ─────────────────────────────────────────────────────────────  │
│                              [Cancel]  [Create / Save]          │
└─────────────────────────────────────────────────────────────────┘
```

On successful creation, the panel closes, the new record appears in the table (scrolled into view if needed), and a success toast confirms.

### 5.4 Card-Based Project Grouping

Within contract and portfolio views, projects are displayed as cards rather than flat table rows. This provides data density within each card while giving clear visual separation between projects.

**Card anatomy:**
- **Card header:** project ID (monospace, `--font-size-sm`), project name (`--font-size-md`, semibold), WBSE code, funding source badge, status badge, planned total (KPI format), actual total (KPI format), health indicator dot. All on one line if space permits; wraps to two lines otherwise.
- **Card body:** the cashflow table for this project (see Section 6.3)
- **Card footer** (optional): a summary row showing project totals if not already in the table

**Card visual specs:**
- Background: `--color-surface-raised`
- Shadow: `--shadow-card`
- Border radius: `--radius-lg`
- Padding: `--space-4` header, 0px body (table bleeds to card edges horizontally)
- Gap between cards: `--space-4` (16px)

**Clickable regions within a card:**
- Project name/ID in the header → navigates to `/projects/:projectId`
- Any cashflow table cell → opens Bottom Detail Card (does not navigate)
- Status badge → opens Bottom Detail Card for status editing

### 5.5 Toast Notifications

Toasts appear in the top-right corner of the viewport, stacked if multiple are visible. They do not overlap the sidebar.

| Type | Background | Icon |
|---|---|---|
| Success | `--color-positive-bg`, `--color-positive` border | ✓ |
| Error | `--color-negative-bg`, `--color-negative` border | ✗ |
| Warning | `--color-warning-bg`, `--color-warning` border | ⚠ |
| Info | `--color-neutral-bg`, `--color-border-strong` border | ℹ |

Auto-dismiss: 3 seconds (success, info), 6 seconds (warning), manual dismiss only (error). Each toast has an × dismiss button.

---

## 6. Page Specifications

### 6.1 Portfolio Dashboard

**Route:** `/`
**Primary personas:** P1 (Budget Owner), P4 (Program Executive)
**Purpose:** At-a-glance health across all contracts for the selected fiscal year.

**Page header:**
```
┌─────────────────────────────────────────────────────────────────────┐
│  Portfolio                     Fiscal Year: [FY26 ▾]  [+ New Contract] │
└─────────────────────────────────────────────────────────────────────┘
```
Title: `--font-size-xl`, `--font-weight-semibold`, `--color-navy`. Fiscal year selector: a styled select input, 140px wide. [+ New Contract] button: primary style, opens the Right-Side Creation Panel.

**KPI strip (4 cards, full-width row):**

```
┌──────────────┬──────────────┬──────────────┬──────────────────────────┐
│ TOTAL BUDGET │ TOTAL ACTUAL │   VARIANCE   │  RECONCILIATION RATE    │
│  $5,100,000  │  $3,200,000  │  $1,900,000  │         87%             │
│              │              │  ▲ 37.3%     │  of actuals matched     │
└──────────────┴──────────────┴──────────────┴──────────────────────────┘
```

KPI card specs:
- Label: `--font-size-sm`, uppercase, letter-spacing `0.04em`, `--color-text-secondary`
- Primary value: `--font-size-2xl`, `--font-weight-bold`, `--color-navy`
- Secondary line: `--font-size-sm`, `--color-text-secondary`
- Card background: `--color-surface-raised`, shadow `--shadow-card`, radius `--radius-lg`, padding `--space-6`
- Equal-width columns in a 4-column CSS grid with `--space-4` gap

Variance KPI coloring:
- `variance > 0` (money unspent): `--color-warning` with ▲ indicator (under budget but may indicate delayed delivery)
- `variance < 0` (over budget): `--color-negative` with ▼ indicator
- `variance = 0`: `--color-text-primary`

**Alerts strip** (displayed only when alerts exist, positioned above the contract card list):

```
┌─────────────────────────────────────────────────────────────────────┐
│  ⚠ 3 accruals aging >60 days  ·  ● 2 milestones over budget  ·    │
│  ↓ Last SAP import 14 days ago                  [View Details →]   │
└─────────────────────────────────────────────────────────────────────┘
```

Specs: `--color-navy` background, `--color-text-inverse` text, 48px tall, `--font-size-sm`. Each alert item is a clickable link that navigates to the relevant page/filter. The [View Details →] link navigates to the first alert's relevant page.

Suppress the strip if there are no alerts.

**Contract card list:**

One card per contract, rendered in order of total planned budget (descending). Each card is a project grouping card (Section 5.4) at the contract level, with the contract's projects as rows inside.

Contract card header row:
- Contract name: `--font-size-md`, semibold, `--color-navy`, clickable → navigates to `/contracts/:contractId`
- Status badge
- Planned total / Actual total (abbreviated format: `$2.11M / $1.45M`)
- Health indicator dot (rightmost)

Below the header: a compact cashflow table showing all projects under the contract (see Section 6.3 for full cashflow table spec). Months are columns, projects are rows. Within each cell, show plan and actual only (not the three-line format) to preserve density at the portfolio level.

**Empty state:**

When no contracts exist for the selected fiscal year:
```
  No contracts for FY26.  [+ New Contract]
```
Centered, `--color-text-secondary`, `--font-size-base`.

---

### 6.2 Contract List

**Route:** `/contracts`
**Purpose:** Searchable, filterable index of all contracts across fiscal years.

**Layout:**
- Filter bar: search input (contract name or vendor), status filter dropdown, fiscal year filter dropdown
- Table: Contract Name, Vendor, Owner, Status badge, Fiscal Year, Planned, Actual, Variance, Actions

Actions column: [View] link (navigates to Contract Detail). No inline editing in this view.

Sort: default by fiscal year descending, then contract name ascending.

**Empty state:** "No contracts match the current filters."

---

### 6.3 Contract Detail

**Route:** `/contracts/:contractId`
**Primary personas:** P1 (Budget Owner)
**Purpose:** Full view of a single contract with all associated projects and their cashflow tables.

**Breadcrumb:** `Portfolio / [Contract Name]`

**Contract header:**
```
  [Contract Name]                                             [Edit Contract]
  Vendor: [name]  ·  Owner: [name]  ·  Status: [badge]
  [FY Start Date] — [FY End Date]  ·  Start: [date]
```

Title: `--font-size-xl`, `--font-weight-semibold`, `--color-navy`. Metadata row: `--font-size-base`, `--color-text-secondary`. [Edit Contract] button: secondary style, opens Right-Side Creation Panel pre-populated with existing values.

**Contract KPI strip (4 cards):**

```
  PLANNED   ACTUAL    VARIANCE   FORECAST
  $2.11M    $1.45M    +$660K ⚠   $2.10M
```

Same specs as Portfolio KPI cards. Variance coloring applies.

**Project list:**

Each project is rendered as a card (Section 5.4). Cards stacked vertically with `--space-4` gap.

Toolbar above project list:
```
  [+ Add Project]                                        [Audit Trail →]
```

[+ Add Project] opens the Right-Side Creation Panel. [Audit Trail →] navigates to `/admin/audit` pre-filtered for this contract.

**Project card — header row:**
- Project ID (monospace badge), Project Name (semibold), WBSE, Funding Source badge, Status badge
- Planned total, Actual total, Health indicator dot

**Project card — body:**
The full cashflow table for this project (see Section 6.4).

**Project card — empty state:**
If a project has no milestones:
```
  No milestones yet.  [+ Add Milestone]
```

---

### 6.4 Cashflow Table

**Shared component** used in Portfolio Dashboard, Contract Detail, Project Detail, and Reports Hub.

This is the core data visualization component of Ledger. It must be designed for density and correctness above all else.

**Structure:**
- **Rows:** one row per milestone (a planned payment/deliverable)
- **Columns:** all 12 fiscal months for the active fiscal year, plus a pinned "Total" column on the right
- **Column pinning:** the milestone name column (200px) is pinned left; the Total column is pinned right. Both remain visible during horizontal scroll.
- **Current month:** column background at `rgba(34,81,255,0.06)` to highlight the current fiscal period.

**Column header row:**
```
  Milestone Name          | Oct   | Nov   | Dec   | Jan   | Feb   | Mar   | ... | Total
```
- 44px tall
- `--font-size-sm`, uppercase, `--font-weight-medium`, `--color-text-secondary`, letter-spacing `0.04em`
- Month headers: right-aligned within column
- Milestone Name header: left-aligned

**Data cells — three-line format:**

Each cell in the month columns shows three lines when plan or actuals exist:

```
┌────────────────┐
│     $112,129   │  ← planned (14px regular, --color-text-primary)
│      $112,129  │  ← actual (14px, colored per variance status)
│           $0   │  ← variance (12px, colored per variance rules)
└────────────────┘
```

Cell height with three lines: 56px. Cell padding: 8px horizontal.

For months with neither plan nor actuals: display `—` in `--color-text-disabled`, single line, 36px row height.

For months with plan but no actuals yet:
- Current or past period: actual line shows `—` in `--color-warning` (pending/overdue)
- Future period: actual line shows `—` in `--color-text-disabled`

**Variance cell coloring rules:**

| Condition | Color token | Display |
|---|---|---|
| `variance = 0`, actuals present | none | `—` |
| `variance > 0` (under budget), explanation on record | `--color-variance-explained` | amount in grey with tooltip showing explanation |
| `variance > 0` (under budget), no explanation | `--color-variance-unexplained` | amount in red with tooltip "No explanation recorded — click to add" |
| `variance < 0` (over budget) | `--color-negative` | amount in red, always requires explanation |
| No actuals, plan exists, current or past period | `--color-warning` | `—` in amber |
| No actuals, plan exists, future period | `--color-text-disabled` | `—` in grey |

**Milestone name column (pinned left, 200px):**
- 4px colored status dot, then milestone name (`--font-size-base`, regular)
- On hover: blue underline on the name, cursor pointer
- Clicking the name opens the Bottom Detail Card for that milestone

**Row actions (visible only on row hover):**
- A `⋮` menu button appears at the far right of the row (outside the Total column)
- Menu items: Edit Milestone, Add Version, Cancel Milestone

**Summary row (bottom of each project's cashflow table):**
- Background: `--color-neutral-bg`
- Shows sum of all milestones for each month column and the total
- Label: "Project Total" or "Contract Total" in semibold

**Empty state:**
```
  No milestones for this project.  [+ Add Milestone]
```
Centered, `--color-text-secondary`, 80px tall.

---

### 6.5 Project Detail

**Route:** `/projects/:projectId`
**Purpose:** Deep dive into a single project. Reached by clicking a project card's name/ID header.

**Breadcrumb:** `Portfolio / [Contract Name] / [Project Name]`

**Project header:**
```
  [Project ID]  [Project Name]                             [Edit Project]
  WBSE: [code]  ·  Contract: [contract name link]  ·  Funding: [source]
  Status: [badge]  ·  Start: [date]
```

**Project KPI strip (4 cards):** Planned, Actual, Variance, Remaining.

**Full cashflow table:**
Same as Section 6.4, but without the fixed-height card constraint. The table is scrollable vertically if there are many milestone rows.

**Reconciliation summary section** (below the cashflow table):

```
  RECONCILIATION SUMMARY
  ───────────────────────────────────────────
  Fully reconciled:      $X  (N milestones)
  Partially reconciled:  $X  (N milestones)
  Unreconciled:          $X  (N milestones)
  ───────────────────────────────────────────
  [View in Reconciliation Workspace →]
```

**Audit trail tab:**
A tab bar below the project header with two tabs: [Cashflow] (default) and [Audit Trail]. The Audit Trail tab shows a table of changes to this project and its milestones (date, action, field, old value, new value, user). Identical content to the global audit log but scoped to this project.

---

### 6.6 Reconciliation Workspace

**Route:** `/reconcile`
**Primary personas:** P2 (Actuals Analyst), P3 (Financial Controller)
**Purpose:** Match SAP actuals to planned milestones.

**Page header:**
```
  Reconciliation       Period: [Mar 2026 ▾]   Vendor: [All ▾]   Contract: [All ▾]
```

The period selector defaults to the current fiscal period. Changing the period filter reloads all three zones.

**Three-zone layout:**

```
┌─────────────────────────────────────────────────────────────────────┐
│  MATCHED ZONE                                                       │
│  (50% of viewport height, scrollable)                               │
│                                                                     │
│  Milestone       Period    Planned   Matched Actuals    Variance    │
│  ─────────────────────────────────────────────────────────────────  │
│  ▼ Jan Sustain   Jan '26   $67,147   $72,000            +$4,853     │
│       Accrual    12/31/25            +$25,000 ACCR                  │
│       Accrual Rev01/31/26            -$25,000 RVRSL                 │
│       Invoice #  01/15/26            +$47,000 INV                   │
│  ▼ Feb Sustain   Feb '26   $90,627   $85,000            -$5,627     │
│       Invoice #  02/28/26            +$85,000 INV                   │
│                                                                     │
├────────────────────────────┬────────────────────────────────────────┤
│  PENDING FORECAST          │  UNMATCHED ACTUALS                    │
│  (25% viewport height)     │  (25% viewport height)                │
│                            │                                       │
│  Milestone       Planned   │  Date        Amount    Vendor         │
│  ─────────────── ────────  │  ─────────── ───────── ───────        │
│  Mar Sustainment $90,627   │  03/15/26    $25,000   Globant        │
│  Feb License     $12,000   │  03/15/26   ($18,000)  Globant        │
│  Mar Labor        $8,500   │  02/28/26    $12,000   CapGem         │
│                            │                                       │
│  6 items · $245,127        │  12 items · $145,000                  │
└────────────────────────────┴────────────────────────────────────────┘
```

**Zone specifications:**

*Matched zone (top, 50% viewport height):*
- Milestone rows are collapsible/expandable (▼/▶ toggle). Default: expanded for current period milestones, collapsed for prior.
- Expanded rows show each individual actual line with: date, amount, category badge (INVOICE / ACCRUAL / ACCRUAL_REVERSAL / ALLOCATION), and an [Undo] button
- [Undo] removes the match (returns the actual to Unmatched Actuals zone)
- Variance column: colored per standard variance rules
- Scrollable within its zone

*Pending Forecast zone (bottom-left, 25% viewport height):*
- Shows plan entries for the current period and prior periods that have no matched actuals
- Future period forecasts are NOT shown here (visible only in the Cashflow Table)
- Click a pending row → highlights it and opens Bottom Detail Card
  - Card left: candidate unmatched actuals sorted by relevance (amount similarity, vendor match, date proximity), each showing a match score (★ 1–5)
  - Card right: confirmation form with category selector (INVOICE / ACCRUAL / ACCRUAL_REVERSAL / ALLOCATION) and optional notes field
  - "Match Selected" button confirms the match

*Unmatched Actuals zone (bottom-right, 25% viewport height):*
- Shows all actuals for the selected period/vendor/contract that are not yet matched to a milestone
- Negative amounts (reversals) shown in `--color-negative`
- Click an unmatched actual row → highlights it and opens Bottom Detail Card
  - Card left: candidate milestones sorted by relevance score (★ 1–5)
  - Card right: confirmation form (category selector, optional notes)
  - "Match Selected" button confirms the match

*Divider between bottom-left and bottom-right zones:*
- 1px `--color-border`, draggable to resize zones horizontally
- Minimum width per zone: 300px

**Summary footers:**
Each zone has a footer row showing item count and total amount.

**Bulk match action:**
Checkbox column in both pending and unmatched tables. When any checkboxes are selected: a contextual action bar appears above the matched zone: "[N] selected  [Match Selected]  [Dismiss]".

---

### 6.7 Reports Hub

**Route:** `/reports`
**Primary personas:** P1 (Budget Owner), P3 (Financial Controller), P4 (Program Executive)
**Purpose:** Unified reporting. All report types are tabs within this single page — no separate routes per report type.

**Page header:**
```
  Reports
```

**Tab bar (6 tabs):**
```
[ Forecast & Budget ]  [ Variance ]  [ Reconciliation Status ]  [ Open Accruals ]  [ Funding ]  [ Journal ]
```

Tab styling: `--font-size-base`, `--font-weight-medium`. Active tab: `--color-navy` text, `2px solid --color-blue` bottom border. Inactive: `--color-text-secondary`. Hover: `--color-text-primary`.

Each tab maintains its own filter state independently. Navigating away and returning restores the last-used filter state (stored in component state, not URL, unless deep-linking is required).

**Common elements across all tabs:**
- Filter bar below the tab bar
- Export CSV button (top-right of each tab)

#### 6.7.1 Forecast & Budget Tab (default)

Replaces the prior separate Budget and Forecast reports.

**Filters:** Fiscal Year, Contract (multi-select dropdown), Funding Source (multi-select), View toggle: [By Project | By Contract]

**Layout:** Project cards stacked vertically. Each card contains a period-pivot table.

**Card structure:**
```
┌─────────────────────────────────────────────────────────────────────┐
│  PR13752  Photopass SUS Break/Fix  ·  Globant ADM  ·  OPEX         │
├──────────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬───────┤
│          │ Oct  │ Nov  │ Dec  │ Jan  │ Feb  │ Mar  │ ...  │ Total │
├──────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┼───────┤
│ Plan     │ 112K │ 110K │ 110K │  67K │  91K │      │      │ 1.23M │
│ Actual   │ 112K │ 110K │ 110K │  72K │  85K │      │      │  850K │
│ Forecast │      │      │      │      │      │  91K │      │  380K │
│ Variance │   $0 │   $0 │   $0 │  -$5K│  +$6K│      │      │   +$1K│
├──────────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┴───────┤
│  Remaining: $380K  ·  Forecast Total: $1,230K  ·  Delta: -$1K      │
└─────────────────────────────────────────────────────────────────────┘
```

- Rows: Plan, Actual, Forecast, Variance (fixed 4-row structure per project)
- Row labels: `--font-size-sm`, `--font-weight-medium`, `--color-text-secondary`, 120px column
- Amount cells: `--font-size-base`, right-aligned, abbreviated (K/M)
- Variance row: colored per standard variance rules
- Footer row: neutral background, shows remaining budget, total forecast, delta (forecast vs. plan)
- Empty cell (no plan, no actuals, no forecast): `—` in `--color-text-disabled`

**Clicking a month column header (e.g., "Jan"):**
A Side Panel slides in from the right (440px wide). Panel title: "[Project ID] — [Month Year]". Panel contents:
1. **Transactions table:** all reconciled actuals for this project in this period (date, amount, vendor, category badge, description). Scrollable.
2. **Variance explanation field:** textarea, pre-filled with the milestone adjustment reason if one exists. User can supplement or add a new comment. Save button below the textarea.
3. Close (×) button in panel header.

#### 6.7.2 Variance Tab

Same card/pivot structure as Forecast & Budget. Each month cell shows Plan / Actual / Variance (three-line format). Color rules identical to Section 6.4 variance coloring.

**Filters:** Fiscal Year, Period Range (from/to month), Contract (multi-select), Variance Type: [All | Unexplained Only | Over Budget].

Clicking a cell opens the same Side Panel as the Forecast & Budget tab.

#### 6.7.3 Reconciliation Status Tab

Flat table (not a pivot/period grid).

**Columns:** Milestone, Contract, Project, Period, Planned, Reconciled, Remaining, Status badge.

**Status badge values for reconciliation:** FULLY_RECONCILED (green), PARTIALLY_RECONCILED (amber), UNRECONCILED (red).

**Filters:** Contract, Period, Status (multi-select checkbox group: FULLY_RECONCILED, PARTIALLY_RECONCILED, UNRECONCILED).

**Sort:** Default by period descending, then status (UNRECONCILED first).

Clicking a row → opens Bottom Detail Card with full milestone details on the left and actuals summary on the right.

#### 6.7.4 Open Accruals Tab

Table sorted by age, oldest first.

**Columns:** Milestone, Contract, Project, Period, Accrual Amount, Age (days), Status badge.

**Row color coding:**
- OPEN, age ≤ 60 days: default row background
- OPEN, age 61–90 days: `--color-warning-bg` row background
- OPEN, age > 90 days: `--color-negative-bg` row background

**Status badge values:** OPEN (neutral), REVERSED (positive, grey).

**Filters:** Contract, Period, Status (OPEN / REVERSED / ALL), Age: [All | >30d | >60d | >90d].

Clicking a row → opens Bottom Detail Card with full accrual details on left (including reversal status, posting date, description) and milestone context on right.

#### 6.7.5 Funding Tab

Simple grouped table. Not a pivot.

**Groups:** OPEX, CAPEX, OTHER (in that order). Each group has a subtotal row. A grand total row at the bottom.

**Columns:** Funding Source, Contract Count, Total Planned, Total Actual, Variance.

**Filters:** Fiscal Year.

No click-through behavior (read-only summary view).

#### 6.7.6 Journal Tab

Mirrors the standalone `/journal` route. Replicated here so P3 and P4 can access journal entries without leaving the Reports context.

**Columns:** Date, Period, Entry Type, Amount, Description, Related Milestone, Posted By.

**Filters:** Period, Entry Type (multi-select), Amount range.

---

### 6.8 SAP Import

**Route:** `/import`
**Primary persona:** P2 (Actuals Analyst)
**Purpose:** Upload SAP export files and review import results.

**Layout — two sections, stacked vertically:**

**Section 1: Upload zone**

```
┌─────────────────────────────────────────────────────────────────────┐
│  SAP Import                                                         │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                                                               │  │
│  │            ↑  Drag SAP export file here, or click to browse  │  │
│  │                                                               │  │
│  │                   Accepted: .csv, .xlsx, .xls                 │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

Drop zone specs:
- Height: 160px
- Border: `2px dashed --color-border`
- Border radius: `--radius-lg`
- Background: `--color-surface`
- Label: `--font-size-base`, `--color-text-secondary`, centered horizontally and vertically

Drag-over state:
- Border: `2px dashed --color-blue`
- Background: `rgba(34,81,255,0.04)`

File selected (before upload submit):
- Drop zone collapses to 80px
- Shows filename and file size
- Shows [Upload] (primary) and [Clear] (text link) buttons

Upload in progress:
- Progress bar replaces the upload button
- Percentage complete shown

Upload complete:
- Drop zone shows "Upload complete. Processing..." with a spinner
- On processing complete, page scrolls to the new import row in the history table and highlights it for 3 seconds

**Section 2: Import History table**

Columns: Date/Time, File Name, New Lines, Duplicates, Errors, Status badge.

Status badge values: STAGED (amber), COMMITTED (green), REJECTED (red), PROCESSING (spinner).

Clicking any row → navigates to `/import/:importId` (Import Review).

Default sort: most recent import first.

---

### 6.9 Import Review

**Route:** `/import/:importId`
**Purpose:** Review a specific import's lines before committing or rejecting.

**Breadcrumb:** `Import / [Filename] — [Date]`

**KPI strip (4 cards):** Total Lines, New Lines, Duplicates, Errors.

**Filter buttons (inline toggle group):** [All] [New] [Duplicates] [Errors]

**Lines table:**

Columns: Line, Date, Vendor, WBSE, Amount, Category, Description, Status badge.

Status badge values: NEW (blue), DUPLICATE (amber), ERROR (red).

Error rows: display an error reason column (why the line failed validation). Tooltip on the error badge shows the full validation message.

**Primary actions (fixed to bottom of page, full-width action bar):**

```
┌─────────────────────────────────────────────────────────────────────┐
│  Import ID: imp-2026-031  ·  N lines (M new, X duplicates, Y errors)│
│                             [Reject Import]   [Commit Import]       │
└─────────────────────────────────────────────────────────────────────┘
```

- [Reject Import]: destructive secondary button (`--color-negative` border and text). Opens a confirmation dialog: "Reject this import? This cannot be undone. [Cancel] [Reject]"
- [Commit Import]: primary button (`--color-navy` background). Only enabled when there are zero ERROR lines (or when the user has acknowledged them). Disabled state: button greyed out with tooltip "Resolve all errors before committing."

---

### 6.10 Journal Viewer

**Route:** `/journal`
**Purpose:** View all journal entries. Read-only for most users; journal entries are generated by system operations (reconciliation, accrual creation).

**Layout:** Page title "Journal", filter bar, entries table.

**Filters:** Period (month/year selector), Entry Type (ACCRUAL / REVERSAL / ALLOCATION / ADJUSTMENT), Amount range (from/to).

**Table columns:** Date, Period, Entry Type badge, Amount, Description, Related Milestone (link to milestone), Posted By.

**Amount coloring:** Positive amounts in `--color-text-primary`, negative amounts (reversals) in `--color-negative`.

No create/edit actions on this page.

---

### 6.11 Admin Pages

All admin pages follow a consistent pattern: page title, optional toolbar with a primary action button, then content (table or form).

#### 6.11.1 System Configuration (`/settings`)

**Layout:** Grouped table, where groups are configuration sections (e.g., "Reconciliation", "Import", "Accruals").

Each group renders as a section with a heading (`--font-size-md`, semibold, `--color-navy`, 32px top margin), followed by a table of settings within that group.

**Table columns:** Setting Name, Value (inline editable input), Reason (inline text input, required to save), Save (per-row button).

Inline editing behavior:
- The Value and Reason fields are directly editable in the table row. No modal, no drawer.
- The per-row Save button is disabled until both Value and Reason are non-empty and different from the persisted value.
- On save, the row shows a brief "Saved ✓" indicator, then returns to normal state.

#### 6.11.2 User Management (`/admin/users`)

**Toolbar:** [+ New User] button (opens Right-Side Creation Panel — not inline, because user creation requires a guided form with password field).

**Table columns:** Username, Display Name, Email, Role badge, Status badge, Actions.

Role badge values: ADMIN (navy), STANDARD (neutral).
Status badge values: ACTIVE (green), INACTIVE (grey).

Actions column: [Deactivate] for active users, [Reactivate] for inactive users. Each is a text button that triggers a confirmation toast before executing.

#### 6.11.3 Reference Data (`/admin/reference-data`)

**Tab bar for entity types:** Funding Sources, Contract Statuses, Project Statuses, Reconciliation Categories.

Each tab contains:
- An inline add form above the table (a compact 1–2 field form block, not a side panel): "Code" input + "Display Name" input + [Add] button
- A table of existing entries: Code, Display Name, Status badge, Actions (Deactivate/Reactivate)

#### 6.11.4 Fiscal Year Management (`/admin/fiscal-years`)

**Inline add form above table:** Start Date input + End Date input + [Add Fiscal Year] button.

Validation: dates must be sequential with existing fiscal years (backend enforces this per BR-90). Display a field-level error message below the inputs if validation fails.

**Table columns:** Fiscal Year label (derived), Start Date, End Date, Actions ([Delete], only for future fiscal years with no data).

#### 6.11.5 Audit Log (`/admin/audit`)

**Filters (filter bar):** Entity Type dropdown, Changed By dropdown, Date Range (from/to date inputs), Action type (CREATE / UPDATE / DELETE).

**Table columns:** Timestamp, Entity Type, Entity ID (link to the record if it still exists), Action badge, Changed By, Changes (collapsed summary with "Show more" expand).

Changes column: shows a compact diff — "field: old → new". Multiple changes on one record show the first 2 and a "+N more" link that expands inline.

**Export:** [Export CSV] button in the toolbar. Exports the current filtered view.

---

## 7. Common Components

### 7.1 Breadcrumb

Every detail page shows a breadcrumb trail immediately below the main nav, above the page header.

```
Portfolio  /  Globant ADM  /  PR13752  /  Jan Sustainment
```

Specs:
- `--font-size-base`, `--color-text-secondary`
- All segments except the last are clickable links (`--color-blue` on hover)
- Last segment is the current page: not a link, `--color-text-primary`
- Separator `/`: `--color-text-disabled`
- 32px top margin, 16px bottom margin from the page heading

### 7.2 Status Badge

Pill-shaped, inline element.

Specs:
- `--font-size-sm`, `--font-weight-medium`
- Border radius: `--radius-md` (4px)
- Padding: 2px vertical, 6px horizontal
- No icon

| Badge value | Background | Text color |
|---|---|---|
| ACTIVE | `--color-navy` | `--color-text-inverse` |
| CLOSED | `--color-neutral-bg` | `--color-text-secondary` |
| DRAFT | transparent, `--color-blue` border | `--color-blue` |
| COMMITTED | `--color-positive` | `--color-text-inverse` |
| STAGED | `--color-warning-bg`, `--color-warning` border | `--color-warning` |
| REJECTED | `--color-negative` | `--color-text-inverse` |
| FULLY_RECONCILED | `--color-positive-bg`, `--color-positive` border | `--color-positive` |
| PARTIALLY_RECONCILED | `--color-warning-bg`, `--color-warning` border | `--color-warning` |
| UNRECONCILED | `--color-negative-bg`, `--color-negative` border | `--color-negative` |
| INVOICE | `--color-neutral-bg` | `--color-text-secondary` |
| ACCRUAL | `--color-warning-bg` | `--color-warning` |
| ACCRUAL_REVERSAL | `--color-positive-bg` | `--color-positive` |
| ALLOCATION | `--color-neutral-bg` | `--color-text-secondary` |
| ADMIN | `--color-navy` | `--color-text-inverse` |
| STANDARD | `--color-neutral-bg` | `--color-text-secondary` |

### 7.3 Health Indicator Dot

A 4px × 4px filled circle, rendered inline with text or in a dedicated column.

| State | Color |
|---|---|
| On track / under budget | `#006633` (positive green) |
| Within 10% of budget / aging warning | `#8C5E00` (warning amber) |
| Over budget / critical | `#C0001E` (negative red) |
| No data / future period | `#A0A0A0` (disabled grey) |

### 7.4 Amount Formatting

All monetary amounts in USD. A dedicated formatting utility must be used throughout — no ad-hoc formatting.

| Context | Format | Example |
|---|---|---|
| KPI card primary value | Abbreviated with suffix | `$2.11M`, `$850K` |
| Table cell (plan/actual) | Full value, no cents | `$112,129` |
| Table cell (zero) | Em dash | `—` |
| Negative value (table cell) | Parenthetical, red | `($4,853)` |
| Detail card / Bottom Detail Card | Full precision with cents | `$112,129.00` |
| Export CSV | Full precision with cents | `112129.00` |

Abbreviation thresholds:
- Millions: `$X.XXM` (2 decimal places)
- Hundreds of thousands: `$XXXK` (0 decimal places)
- Below $10,000: show full value `$X,XXX`

### 7.5 Variance Display

A variance value is never shown without context. Rules:

- `variance = 0`: show `—`
- `variance ≠ 0`, explanation on record: show amount in `--color-variance-explained` grey, with a tooltip showing the explanation text
- `variance ≠ 0`, no explanation: show amount in `--color-variance-unexplained` red, with tooltip "No explanation recorded — click to add"
- `variance < 0` (over budget): always red, always requires explanation to change color

The `+` prefix (under budget) is not inherently positive. A tooltip must accompany all `+` variance values: "Under budget — may indicate delayed delivery." This prevents misreading a positive variance as unambiguously good news.

Explanations come from two sources (in priority order):
1. Milestone adjustment reason (auto-populated from the milestone record)
2. User-typed comment entered directly on the variance cell via the Bottom Detail Card

### 7.6 Empty States

Each table and list has a contextual empty state. Standard format: centered text at `--color-text-secondary`, 80px tall, with an action link if applicable.

| Context | Message |
|---|---|
| No contracts | "No contracts yet.  [+ New Contract]" |
| No projects for a contract | "No projects yet.  [+ Add Project]" |
| No milestones for a project | "No milestones for this project.  [+ Add Milestone]" |
| No unmatched actuals | "All actuals reconciled for this period. ✓" |
| No pending forecast | "No pending forecast items." |
| No import history | "No imports yet." |
| No audit entries | "No audit log entries match the current filters." |
| No contracts matching filters | "No contracts match the current filters." |

### 7.7 Time Machine Banner

When the system is in "time machine" mode (viewing data as of a past date), a 44px banner appears between the top bar and the page content on every page.

```
┌─────────────────────────────────────────────────────────────────────┐
│  ⏱  TIME MACHINE — Viewing as of February 15, 2026         [Reset] │
└─────────────────────────────────────────────────────────────────────┘
```

Specs:
- Background: `--color-warning-bg` (`#FFF8EC`)
- Border-bottom: `1px solid --color-warning`
- Text: `--font-size-sm`, `--font-weight-medium`, `--color-warning`
- [Reset] button: text link style, navigates to current date view

The banner must persist across all pages while time machine mode is active. All data queries must pass the time machine date to the API.

### 7.8 Primary Button

- Background: `--color-navy`
- Text: `--color-text-inverse`, `--font-size-base`, `--font-weight-medium`
- Padding: 8px 16px
- Border radius: `--radius-md`
- Hover: `rgba(5,28,44,0.85)` background
- Disabled: `--color-text-disabled` background, not-allowed cursor

### 7.9 Secondary Button

- Background: transparent
- Border: `1px solid --color-border-strong`
- Text: `--color-text-primary`, `--font-size-base`, `--font-weight-medium`
- Padding: 8px 16px
- Border radius: `--radius-md`
- Hover: `--color-neutral-bg` background

### 7.10 Destructive Button

- Background: transparent
- Border: `1px solid --color-negative`
- Text: `--color-negative`, `--font-size-base`, `--font-weight-medium`
- Padding: 8px 16px
- Border radius: `--radius-md`
- Hover: `--color-negative-bg` background

### 7.11 Text Input

- Border: `1px solid --color-border`
- Border radius: `--radius-md`
- Height: 36px
- Padding: 0 8px
- Font: `--font-size-base`, `--color-text-primary`
- Placeholder: `--color-text-disabled`
- Focus: border `1px solid --color-blue`, `box-shadow: 0 0 0 2px rgba(34,81,255,0.15)`
- Error state: border `1px solid --color-negative`, error message below in `--font-size-sm` `--color-negative`

### 7.12 Select / Dropdown

Same visual specs as Text Input. Custom styled (not native `<select>`). Options list uses `--shadow-panel` and `--color-surface-raised` background. Selected option: `rgba(34,81,255,0.06)` background with `--color-blue` text.

---

## 8. Data Display Conventions

### 8.1 Currency

- All amounts are in USD.
- Abbreviated in KPI cards and summary rows: `$2.11M`, `$850K`, `$145K`
- Full value (no cents) in table data cells: `$112,129`
- Full precision (with cents) in Bottom Detail Cards and exports: `$112,129.00`
- Negative values (over budget, reversals) displayed in parentheses and colored `--color-negative`: `($4,853)`
- Zero displayed as em dash in tables: `—`

### 8.2 Dates

| Context | Format |
|---|---|
| Fiscal period column headers | `Jan '26`, `Feb '26` |
| Full date in detail cards | `January 15, 2026` |
| Date in tables (not period) | `01/15/26` |
| ISO format in form inputs | `2026-01-15` |
| Timestamp in audit log | `Mar 15, 2026 at 14:32` |

### 8.3 Variance Framing

- `+` prefix means under budget (money remaining in plan). This is NOT inherently positive — it may indicate delayed or undelivered work. Tooltip always explains.
- `-` prefix means over budget. Always red. Always requires explanation.
- The sign is relative to plan: `actual - plan`. A positive result means actuals are less than plan (under-spend).

### 8.4 Percentages

- Completion and reconciliation rates in KPI cards: `87%` (0 decimal places)
- All other percentages: `37.3%` (1 decimal place)

### 8.5 Fiscal Periods

- Current period column: `rgba(34,81,255,0.06)` column background
- Future period columns: `--color-text-disabled` text for empty cells, no actuals expected
- Past periods with no actuals: `--color-warning` indicator on the cell's actual-line

---

## 9. Responsive Behavior

### 9.1 Primary Target

Desktop and laptop screens at minimum 1280px viewport width. The application is not optimized for mobile and does not need to function on mobile devices.

### 9.2 Layout Breakpoints

| Breakpoint | Behavior |
|---|---|
| ≥ 1280px (target) | Full layout: 220px fixed sidebar + content area |
| 1024px–1279px | Sidebar collapses to icon-only mode (40px), content area expands |
| < 1024px | Not supported; display a banner: "Ledger is optimized for desktop browsers at 1280px or wider." |

### 9.3 Sidebar

Fixed 220px. Never collapses at target width. At 1024–1279px: collapses to 40px icon-only mode with tooltips on hover.

### 9.4 Content Area

`width: calc(100vw - 220px)`. Minimum content width: 1060px (enforces minimum viewport of 1280px in practice).

### 9.5 Cashflow Tables

Horizontally scrollable within their card container if the viewport is too narrow to display all 12 months. The milestone name column is sticky (CSS `position: sticky; left: 0`) and always visible during horizontal scroll. The Total column is sticky-right where CSS supports it.

### 9.6 Bottom Detail Card

At minimum viewport (1280px), the card is 380px tall and slides up from the bottom without covering the active table row. If the viewport is shorter than 768px (which is below the supported minimum), the card falls back to a modal centered in the viewport.

---

## 10. Implementation Notes

### 10.1 What Changes from the Current Implementation

The following changes are required from the current frontend implementation. Each item is a discrete scope of work.

1. **Remove all drawer components.** Replace with the Right-Side Creation Panel (for new record creation) and Bottom Detail Card (for editing existing records). No modal dialogs for record editing.

2. **Redesign navigation.** Add section groupings (Operations, Reporting, Admin) with uppercase 11px divider labels. Reorder nav items to reflect persona priority order.

3. **Introduce card-based project grouping.** Projects render as cards within contract views, not as flat table rows. Each card has a header with KPIs and a body with the cashflow table.

4. **Implement the Cashflow Table component.** This is a new core component. It replaces the current milestone list. Key requirements: pinned milestone name column, pinned Total column, three-line cell format (plan/actual/variance), variance coloring logic, current-month column highlight, horizontal scroll with sticky columns.

5. **Redesign the Reconciliation Workspace.** Replace with the three-zone layout: matched top (50%), pending forecast bottom-left (25%), unmatched actuals bottom-right (25%). Add the draggable zone divider. Implement candidate-suggestion logic in the Bottom Detail Card.

6. **Implement the Reports Hub.** Consolidate all report pages into a single tabbed page at `/reports`. Old individual report routes may redirect to the hub with the appropriate tab pre-selected.

7. **Implement the Bottom Detail Card component.** This is a new global interaction component. It must: slide up from the viewport bottom, show contextual content based on the clicked cell type, support form submission, handle cancel/discard, and close on Escape.

8. **Implement design tokens.** All colors, typography sizes, weights, spacing values, shadows, and border radii must be defined as CSS custom properties and referenced by token name throughout the component codebase. No hard-coded values.

9. **Implement the amount formatting utility.** A single shared utility function must handle all monetary display: abbreviated KPI format, full table format, parenthetical negatives, em dash for zero, and full-precision export format.

10. **Standardize the Status Badge component.** A single Badge component accepts a `status` prop and renders the correct color/border/text combination per Section 7.2. No ad-hoc badge styling in individual components.

### 10.2 What Stays the Same

The following are out of scope for this frontend redesign:

- **All API contracts.** No backend changes. API calls remain unchanged.
- **Authentication flow.** Login, session management, and token handling are unchanged.
- **Time Machine context.** The mechanism for passing the time machine date to API calls is unchanged; only the banner UI is new.
- **All backend data models.** Entity structure, field names, and business rules are governed by Spec 10 and the backend implementation.
- **Route paths.** All routes listed in Section 4.2 are preserved. The Reports Hub consolidates report sub-routes but the `/reports` route itself is existing.

### 10.3 Technology Decisions (Deferred)

The following decisions are explicitly deferred and must not be assumed from this spec:

- CSS framework (Tailwind, CSS Modules, styled-components, etc.)
- Component library (MUI, Radix, Headless UI, etc.)
- State management approach (Redux, Zustand, React Context, etc.)
- Data fetching strategy (React Query, SWR, raw fetch, etc.)
- Build tooling version

This spec governs the visual and behavioral output, not the implementation mechanism.

### 10.4 Accessibility Baseline

The following accessibility requirements apply to all components:

- All interactive elements (buttons, links, inputs) must be keyboard-focusable with a visible focus indicator (`box-shadow: 0 0 0 2px rgba(34,81,255,0.4)`)
- Color must not be the sole means of conveying information (e.g., variance cells must also have a tooltip or text label)
- All icons used without visible text labels must have an `aria-label` or `title` attribute
- Table column headers must use `<th scope="col">` and table row headers must use `<th scope="row">`
- Status badges must include the status text, not only color
- WCAG AA contrast ratio required for all text/background combinations

WCAG AAA and full screen reader optimization are not required in this phase but must not be actively broken.

### 10.5 Performance Expectations

- Initial page load (Portfolio Dashboard): first contentful paint under 2 seconds on a standard office network
- Cashflow table with 50 milestone rows and 12 month columns: render time under 200ms
- Bottom Detail Card open animation: 150ms ease-out
- Right-Side Creation Panel open animation: 200ms ease-out
- All table filter/sort interactions: under 100ms (client-side, no round-trip)

Data that requires API calls (e.g., loading a detail card's actuals) may show a skeleton loader within the card while the request is in flight.
