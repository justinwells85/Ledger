# Admin Configuration & Reference Data Management

> Defines all system configuration, reference data, and administrative capabilities required by the System Administrator persona (P6).
> These requirements apply across all tiers. Items marked Core are prerequisites for production use.

---

## 1. User Management

### 1.1 Overview

The current single-user model (one shared `admin` account, all writes recorded as `"system"`) must be replaced with individual user accounts to support:

- Meaningful audit trails (BR-63 requires user identity on every write)
- Role-based access control
- Individual accountability for budget changes

### 1.2 User Entity

| Field | Type | Notes |
|-------|------|-------|
| `user_id` | UUID | System-generated |
| `username` | VARCHAR(100) | Unique, used for login |
| `display_name` | VARCHAR(200) | Shown in UI and audit log |
| `email` | VARCHAR(200) | Unique |
| `role` | ENUM | See 1.3 |
| `is_active` | BOOLEAN | Deactivated users cannot log in; their history is preserved |
| `created_at` | TIMESTAMPTZ | |
| `created_by` | VARCHAR(100) | Admin who created the account |

### 1.3 Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| `ADMIN` | System administrator | Full access including user management, reference data, config |
| `FINANCE_MANAGER` | Budget owner | Create/edit contracts, projects, milestones; view all reports |
| `ANALYST` | Reconciliation specialist | Reconcile actuals; view all data; no budget creation |
| `READ_ONLY` | Executive / leadership | View all data and reports; no mutations |

### 1.4 Business Rules

| ID | Rule |
|----|------|
| BR-80 | Every authenticated user has exactly one role |
| BR-81 | Deactivated users cannot authenticate; their historical records are preserved |
| BR-82 | Only ADMIN users can manage user accounts, reference data, and system configuration |
| BR-83 | `created_by` on all mutations is populated from the authenticated user's username |
| BR-84 | The last ADMIN account cannot be deactivated |

### 1.5 Admin UI: User Management

**Route:** `/admin/users`

**Capabilities:**
- List all users with role, status, and last-modified
- Create new user (username, display name, email, role)
- Edit user (display name, email, role)
- Deactivate / reactivate user
- Cannot delete users (preserve audit history)

---

## 2. Fiscal Year Management

### 2.1 Overview

Fiscal years are currently seeded via a Flyway migration (V008). Each new fiscal year (Oct–Sep) requires a code change and deployment. The admin must be able to add a new fiscal year through the UI.

### 2.2 Business Rules

| ID | Rule |
|----|------|
| BR-85 | A fiscal year label follows the format `FY##` (e.g., FY28) |
| BR-86 | A fiscal year spans exactly 12 consecutive calendar months from October 1 to September 30 |
| BR-87 | When a fiscal year is created, all 12 fiscal periods are automatically generated |
| BR-88 | Period keys follow the existing format: `{FY}-{NN}-{MMM}` (e.g., `FY28-01-OCT`) |
| BR-89 | Fiscal years cannot be deleted if any milestones, actuals, or journal entries reference their periods |
| BR-90 | Fiscal years must be created in sequence — no gaps allowed |

### 2.3 Auto-Generation of Periods

When a new fiscal year is created, the system generates 12 periods automatically:

| Sort Order | Quarter | Calendar Month | Period Key Pattern |
|-----------|---------|---------------|-------------------|
| 1 | Q1 | October | `{FY}-01-OCT` |
| 2 | Q1 | November | `{FY}-02-NOV` |
| 3 | Q1 | December | `{FY}-03-DEC` |
| 4 | Q2 | January | `{FY}-04-JAN` |
| 5 | Q2 | February | `{FY}-05-FEB` |
| 6 | Q2 | March | `{FY}-06-MAR` |
| 7 | Q3 | April | `{FY}-07-APR` |
| 8 | Q3 | May | `{FY}-08-MAY` |
| 9 | Q3 | June | `{FY}-09-JUN` |
| 10 | Q4 | July | `{FY}-10-JUL` |
| 11 | Q4 | August | `{FY}-11-AUG` |
| 12 | Q4 | September | `{FY}-12-SEP` |

### 2.4 Admin UI: Fiscal Year Management

**Route:** `/admin/fiscal-years`

**Capabilities:**
- List all fiscal years with period count and usage status
- Add new fiscal year (enter the year number; system calculates dates and generates periods)
- View periods for a fiscal year
- Cannot delete a fiscal year that has data

---

## 3. Reference Data Management

### 3.1 Overview

Several business values are currently hardcoded as Java enums. These must be moved to database-managed reference tables with admin CRUD, so values can be adjusted without a code deployment.

### 3.2 Funding Sources

Currently: `OPEX`, `CAPEX`, `OTHER_TEAM` (Java enum `FundingSource`)

| Field | Type | Notes |
|-------|------|-------|
| `code` | VARCHAR(50) | Primary key, used in data |
| `display_name` | VARCHAR(100) | Shown in dropdowns |
| `description` | VARCHAR(500) | Explains what the funding source represents |
| `is_active` | BOOLEAN | Inactive codes hidden from new-entry dropdowns; existing data preserved |
| `sort_order` | INT | Controls display order in dropdowns |

**Business Rules:**

| ID | Rule |
|----|------|
| BR-91 | Funding source codes must be unique and uppercase alphanumeric with underscores |
| BR-92 | Deactivated funding sources are hidden from creation forms but preserved on existing projects |
| BR-93 | A funding source with active projects cannot be deleted; it can only be deactivated |

### 3.3 Contract Statuses

Currently: `ACTIVE`, `CLOSED`, `TERMINATED` (Java enum `ContractStatus`)

Managed the same way as funding sources. Fields: `code`, `display_name`, `description`, `is_active`, `sort_order`.

| ID | Rule |
|----|------|
| BR-94 | The `ACTIVE` status is required and cannot be deactivated |
| BR-95 | Status transitions are validated: ACTIVE → CLOSED, ACTIVE → TERMINATED only |

### 3.4 Project Statuses

Currently: `ACTIVE`, `CLOSED` (Java enum `ProjectStatus`)

Same management pattern as above.

### 3.5 Reconciliation Categories

Currently: `INVOICE`, `ACCRUAL`, `ACCRUAL_REVERSAL`, `ALLOCATION` (Java enum `ReconciliationCategory`)

| Field | Type | Notes |
|-------|------|-------|
| `code` | VARCHAR(50) | Primary key |
| `display_name` | VARCHAR(100) | |
| `description` | VARCHAR(500) | Explains when to use this category |
| `affects_accrual_lifecycle` | BOOLEAN | TRUE for ACCRUAL and ACCRUAL_REVERSAL — drives aging tracking |
| `is_active` | BOOLEAN | |
| `sort_order` | INT | |

| ID | Rule |
|----|------|
| BR-96 | ACCRUAL and ACCRUAL_REVERSAL categories are system-reserved and cannot be deactivated |
| BR-97 | A reconciliation category cannot be deleted if any reconciliation records reference it |

### 3.6 Admin UI: Reference Data

**Route:** `/admin/reference-data`

**Tabs:** Funding Sources | Contract Statuses | Project Statuses | Reconciliation Categories

**Capabilities per tab:**
- List all values with active/inactive status
- Add new value (code, display name, description)
- Edit display name and description
- Toggle active/inactive
- Reorder (drag or up/down arrows)
- Cannot delete values that are in use

---

## 4. System Configuration (Data-Driven)

### 4.1 Overview

The Settings page currently hardcodes the four config rows. Any new configuration key requires a code change. The UI must become fully data-driven, rendering all rows from the `system_config` table dynamically.

### 4.2 Current Config Keys

| Key | Default | Description |
|-----|---------|-------------|
| `tolerance_percent` | `0.02` | Reconciliation tolerance as decimal (2%) |
| `tolerance_absolute` | `50.00` | Reconciliation tolerance in absolute dollars |
| `accrual_aging_warning_days` | `60` | Days before aging warning is raised |
| `accrual_aging_critical_days` | `90` | Days before aging critical alert is raised |

### 4.3 Extended Config Schema

Add `data_type` and `display_group` to `system_config` to support data-driven rendering:

| Added Field | Type | Values | Purpose |
|-------------|------|--------|---------|
| `data_type` | VARCHAR(20) | `DECIMAL`, `INTEGER`, `PERCENTAGE`, `TEXT` | Drives input field type and validation |
| `display_group` | VARCHAR(100) | e.g., `RECONCILIATION TOLERANCE`, `ACCRUAL AGING` | Groups rows in the Settings UI |
| `display_name` | VARCHAR(200) | e.g., `Tolerance (%)` | Label shown in the UI |
| `display_order` | INTEGER | | Order within the group |

### 4.4 Business Rules

| ID | Rule |
|----|------|
| BR-98 | All config changes require a reason (existing BR-31) |
| BR-99 | Config values are validated by data_type before saving |
| BR-100 | `accrual_aging_critical_days` must be greater than `accrual_aging_warning_days` |
| BR-101 | New config keys can be added via admin UI without code changes |

---

## 5. Audit Log Viewer

### 5.1 Overview

The backend audit APIs (`/api/v1/audit/*`) are fully implemented. The admin needs a UI to browse and export the audit log.

### 5.2 View Capabilities

**Route:** `/admin/audit`

**Filters:**
- Entity type (CONTRACT, PROJECT, CONFIGURATION, MILESTONE)
- Entity ID (free-text search)
- User (who made the change)
- Date range (from / to)
- Action (CREATE, UPDATE, STATUS_CHANGE)

**Table columns:**
- Timestamp
- Entity type + ID
- Action
- User
- Changed fields (collapsed summary; expand to see before/after values)
- Reason

**Export:** CSV download of filtered results.

### 5.3 Milestone Version History

The full version history for any milestone is accessible via `/api/v1/audit/milestone/{id}`. This must be surfaced on the Milestone Detail page as a collapsible timeline showing:

- Version number
- Effective date
- Planned amount
- Fiscal period
- Delta from prior version
- Reason
- Created by

---

## 6. Application Shell Updates

### 6.1 Navigation

Add **Admin** section to the side navigation, visible only to users with `ADMIN` role:

```
...
Settings
─────────
Admin ▾
  Users
  Fiscal Years
  Reference Data
  Audit Log
```

### 6.2 User Display

The top bar currently shows a static name. It must display the authenticated user's `display_name` and role badge. Clicking opens a dropdown with "Sign out".

---

## 7. Implementation Backlog

Ordered by priority for the next development phase:

| # | Item | Spec Refs | Complexity |
|---|------|-----------|------------|
| T37 | User table + authentication refactor (individual users, roles) | BR-80–84 | High |
| T38 | Fiscal year management UI + API | BR-85–90 | Medium |
| T39 | Reference data tables + admin CRUD UI | BR-91–97 | Medium |
| T40 | Settings page: data-driven rendering | BR-98–101 | Low |
| T41 | Audit log viewer + export | 11-change-management.md | Medium |
| T42 | Core CRUD UI: Add Contract, Add Milestone, Edit/Cancel Milestone | 13-api-design.md | Medium |
| T43 | Reconcile workspace: undo + filters | P3-S4, P3-S5 | Medium |
| T44 | Milestone detail: version history timeline | P5-S5 | Low |
| T45 | Forecast Report and Funding Report (complete stubs) | 09-reporting.md | Medium |
