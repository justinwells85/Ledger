# Domain Model

> Source: DPI Accruals v2 Spec, Section 2

---

## 1. Entity Overview

```
Fiscal Year
  └── Fiscal Period (12 per year, Oct-Sep)

Contract
  └── Project (1:M, each with WBSE + funding source)
        └── Milestone (1:M, informational deliverables)
              └── MilestoneVersion (1:M, independently versioned)

SAP Import
  └── Actual Line (1:M, one per SAP row)

Reconciliation (links Actual Line ↔ Milestone, 1:1)

Journal Entry
  └── Journal Line (1:M, minimum 2 per entry, must balance)
```

---

## 2. Entity Definitions

### 2.1 Fiscal Year

Represents a Disney fiscal year (October through September).

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| `fiscal_year` | String (PK) | Format: `FY{YY}` | `FY26` |
| `start_date` | Date | Required | `2025-10-01` |
| `end_date` | Date | Required, must be after start_date | `2026-09-30` |

### 2.2 Fiscal Period

A single month within a fiscal year.

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| `period_id` | UUID (PK) | Auto-generated | |
| `fiscal_year` | String (FK → Fiscal Year) | Required | `FY26` |
| `period_key` | String (Unique) | Format: `FY{YY}-{seq}-{MON}` | `FY26-01-OCT` |
| `quarter` | String | Q1, Q2, Q3, Q4 | `Q1` |
| `calendar_month` | YearMonth | Required | `2025-10` |
| `display_name` | String | Required | `October 2025` |
| `sort_order` | Integer | 1-12 within fiscal year | `1` |

**Fiscal Calendar:**

| Quarter | Period 1 | Period 2 | Period 3 |
|---------|----------|----------|----------|
| Q1 | FY26-01-OCT (Oct 2025) | FY26-02-NOV (Nov 2025) | FY26-03-DEC (Dec 2025) |
| Q2 | FY26-04-JAN (Jan 2026) | FY26-05-FEB (Feb 2026) | FY26-06-MAR (Mar 2026) |
| Q3 | FY26-07-APR (Apr 2026) | FY26-08-MAY (May 2026) | FY26-09-JUN (Jun 2026) |
| Q4 | FY26-10-JUL (Jul 2026) | FY26-11-AUG (Aug 2026) | FY26-12-SEP (Sep 2026) |

**Note:** The period_key format in the current Excel uses a non-sequential numbering (FY26-01-OCT, FY26-02-NOV, FY26-01-DEC). The new system should use sequential sort_order (1-12) as the canonical ordering. The legacy period_key format can be stored for migration compatibility but should not be the primary identifier.

### 2.3 Contract

A vendor agreement representing a distinct billing relationship.

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| `contract_id` | UUID (PK) | Auto-generated | |
| `name` | String | Required, unique | `Globant ADM` |
| `vendor` | String | Required | `Globant` |
| `description` | String | Optional | |
| `owner_user` | String | Required | `Rob Moore` |
| `start_date` | Date | Required | `2025-10-01` |
| `end_date` | Date | Optional (null = open-ended) | `2026-09-30` |
| `status` | Enum | ACTIVE, CLOSED, TERMINATED | `ACTIVE` |
| `created_at` | Timestamp | Auto-set | |
| `created_by` | String | System user | |

### 2.4 Project

A work stream within a contract, identified by a WBSE code and Project ID.

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| `project_id` | String (PK) | Business key, e.g., PR##### | `PR13752` |
| `contract_id` | UUID (FK → Contract) | Required | |
| `wbse` | String | Required | `1174905.SU.ES` |
| `name` | String | Required | `DPI - Photopass - SUS Break/Fix` |
| `funding_source` | Enum | OPEX, CAPEX, OTHER_TEAM | `OPEX` |
| `status` | Enum | ACTIVE, CLOSED | `ACTIVE` |
| `created_at` | Timestamp | Auto-set | |
| `created_by` | String | System user | |

**Business rules:**
- The same Project ID should not appear under multiple contracts (unique constraint)
- WBSE + Project ID combination should be unique
- Funding source is set at the project level and applies to all milestones within

### 2.5 Milestone

A deliverable or payment obligation within a project, tied to a fiscal period.

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| `milestone_id` | UUID (PK) | Auto-generated | |
| `project_id` | String (FK → Project) | Required | `PR13752` |
| `name` | String | Required | `January Sustainment` |
| `description` | String | Optional | Informational details |
| `created_at` | Timestamp | Auto-set | |
| `created_by` | String | System user | |

**Business rules:**
- A milestone must have at least one MilestoneVersion (v1, created at milestone creation time)
- Deleting a milestone = creating a version with planned_amount = 0

### 2.6 MilestoneVersion

A point-in-time snapshot of a milestone's planned amount and target period. See [04-milestone-versioning.md](./04-milestone-versioning.md) for full versioning rules.

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| `version_id` | UUID (PK) | Auto-generated | |
| `milestone_id` | UUID (FK → Milestone) | Required | |
| `version_number` | Integer | Sequential per milestone, starting at 1 | `1` |
| `planned_amount` | Decimal(15,2) | Required, can be 0 for cancellations | `25250.00` |
| `fiscal_period_id` | UUID (FK → Fiscal Period) | Required | |
| `effective_date` | Date | Required, >= prior version's effective_date | `2025-11-01` |
| `reason` | String | Required | `Initial budget allocation` |
| `created_at` | Timestamp | Auto-set | |
| `created_by` | String | System user | |

### 2.7 SAP Import

A record of a file upload from SAP.

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| `import_id` | UUID (PK) | Auto-generated | |
| `filename` | String | Required | `SAP_FY26_March.csv` |
| `imported_at` | Timestamp | Auto-set | |
| `imported_by` | String | System user | |
| `fiscal_year` | String | Optional | `FY26` |
| `status` | Enum | STAGED, COMMITTED, REJECTED | `COMMITTED` |
| `total_lines` | Integer | Set during staging | `150` |
| `new_lines` | Integer | Set during staging | `45` |
| `duplicate_lines` | Integer | Set during staging | `105` |

### 2.8 Actual Line

A single line item from an SAP export.

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| `actual_id` | UUID (PK) | Auto-generated | |
| `import_id` | UUID (FK → SAP Import) | Required | |
| `sap_document_number` | String | Optional (sparse) | `5100012345` |
| `posting_date` | Date | Required | `2026-01-31` |
| `fiscal_period_id` | UUID (FK → Fiscal Period) | Derived from posting_date | |
| `amount` | Decimal(15,2) | Required, positive or negative | `25000.00` |
| `vendor_name` | String | Optional (sparse) | `Globant S.A.` |
| `cost_center` | String | Optional (sparse) | |
| `wbse` | String | Optional (sparse) | `1174905.SU.ES` |
| `gl_account` | String | Optional (sparse) | |
| `description` | String | Optional | Free text from SAP |
| `line_hash` | String | SHA-256 of all fields, unique | |
| `is_duplicate` | Boolean | Default false | |
| `created_at` | Timestamp | Auto-set | |

### 2.9 Reconciliation

A link between an actual line and a milestone, with classification.

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| `reconciliation_id` | UUID (PK) | Auto-generated | |
| `actual_id` | UUID (FK → Actual Line, UNIQUE) | Required, one reconciliation per actual | |
| `milestone_id` | UUID (FK → Milestone) | Required | |
| `category` | Enum | INVOICE, ACCRUAL, ACCRUAL_REVERSAL, ALLOCATION | `INVOICE` |
| `match_notes` | String | Optional | User explanation |
| `reconciled_at` | Timestamp | Auto-set | |
| `reconciled_by` | String | System user | |

---

## 3. Funding Source Enum

| Value | Description |
|-------|-------------|
| `OPEX` | Operating expense — standard budget |
| `CAPEX` | Capital expense — capitalized project |
| `OTHER_TEAM` | Charged from / allocated by another department |

---

## 4. Key Relationships

| Relationship | Cardinality | Notes |
|-------------|-------------|-------|
| Fiscal Year → Fiscal Period | 1:M | 12 periods per year |
| Contract → Project | 1:M | |
| Project → Milestone | 1:M | |
| Milestone → MilestoneVersion | 1:M | Independently versioned |
| SAP Import → Actual Line | 1:M | |
| Actual Line → Reconciliation | 1:0..1 | An actual may or may not be reconciled |
| Milestone → Reconciliation | 1:M | A milestone can have many actuals reconciled to it |
| Journal Entry → Journal Line | 1:M | Minimum 2 lines, must balance |
