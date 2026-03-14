# Database Schema

> Implements: 01-domain-model.md
> Migrations: `src/main/resources/db/migration/V001__*.sql` through `V008__*.sql`

---

## 1. Migration Inventory

| Migration | Tables | Purpose |
|-----------|--------|---------|
| V001 | `fiscal_year`, `fiscal_period` | Fiscal calendar reference data |
| V002 | `contract`, `project` | Vendor agreements and work streams |
| V003 | `milestone`, `milestone_version` | Deliverables with independent versioning |
| V004 | `journal_entry`, `journal_line` | Double-entry ledger + balance trigger |
| V005 | `sap_import`, `actual_line` | SAP file imports and line items |
| V006 | `reconciliation` | Actual-to-milestone mapping with categories |
| V007 | `audit_log`, `system_config` | Change tracking and configuration |
| V008 | (seed data) | FY25, FY26, FY27 fiscal calendars |

---

## 2. Schema Diagram

```
┌──────────────┐     ┌──────────────────┐
│ fiscal_year  │────→│  fiscal_period    │
│──────────────│  1:M│──────────────────│
│ fiscal_year  │PK   │ period_id        │PK
│ start_date   │     │ fiscal_year      │FK
│ end_date     │     │ period_key       │UQ
└──────────────┘     │ quarter          │
                     │ calendar_month   │
                     │ sort_order       │
                     └───────┬──────────┘
                             │
          ┌──────────────────┼─────────────────────────────┐
          │                  │                             │
          ▼                  ▼                             ▼
┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────┐
│    contract      │  │ milestone_version │  │     actual_line       │
│─────────────────│  │──────────────────│  │──────────────────────│
│ contract_id     │PK│ version_id       │PK│ actual_id            │PK
│ name            │UQ│ milestone_id     │FK│ import_id            │FK
│ vendor          │  │ version_number   │  │ sap_document_number  │
│ owner_user      │  │ planned_amount   │  │ posting_date         │
│ start_date      │  │ fiscal_period_id │FK│ fiscal_period_id     │FK
│ end_date        │  │ effective_date   │  │ amount               │
│ status          │  │ reason           │  │ vendor_name          │
└────────┬────────┘  │ created_by       │  │ wbse                 │
         │           └────────┬─────────┘  │ description          │
         │ 1:M                │ M:1        │ line_hash            │UQ(partial)
         ▼                    │            │ is_duplicate         │
┌─────────────────┐  ┌───────▼──────────┐  └──────────┬───────────┘
│    project       │  │    milestone      │             │
│─────────────────│  │──────────────────│             │ 1:0..1
│ project_id      │PK│ milestone_id     │PK           │
│ contract_id     │FK│ project_id       │FK           ▼
│ wbse            │  │ name             │  ┌──────────────────────┐
│ name            │  │ description      │  │   reconciliation      │
│ funding_source  │  └──────────────────┘  │──────────────────────│
│ status          │           ▲            │ reconciliation_id    │PK
└────────┬────────┘           │            │ actual_id            │FK,UQ
         │ 1:M                │ M:1        │ milestone_id         │FK
         └───────────────────→┘            │ category             │
                                           │ match_notes          │
                                           │ reconciled_by        │
                                           └──────────────────────┘

┌──────────────────┐     ┌──────────────────┐
│  journal_entry   │────→│   journal_line    │
│──────────────────│ 1:M │──────────────────│
│ entry_id        │PK    │ line_id          │PK
│ entry_date      │      │ entry_id         │FK
│ effective_date  │      │ account          │
│ entry_type      │      │ contract_id      │FK (nullable)
│ description     │      │ project_id       │FK (nullable)
│ created_by      │      │ milestone_id     │FK (nullable)
└──────────────────┘     │ fiscal_period_id │FK
                         │ debit            │
                         │ credit           │
                         │ reference_type   │
                         │ reference_id     │
                         └──────────────────┘

┌──────────────────┐     ┌──────────────────┐
│   sap_import     │     │    audit_log      │
│──────────────────│     │──────────────────│
│ import_id       │PK    │ audit_id         │PK
│ filename        │      │ entity_type      │
│ imported_by     │      │ entity_id        │
│ status          │      │ action           │
│ total_lines     │      │ changes (JSONB)  │
│ new_lines       │      │ reason           │
│ duplicate_lines │      │ created_by       │
└──────────────────┘     └──────────────────┘

┌──────────────────┐
│  system_config   │
│──────────────────│
│ config_key      │PK
│ config_value    │
│ description     │
└──────────────────┘
```

---

## 3. Key Constraints

| Table | Constraint | Type | Business Rule |
|-------|-----------|------|---------------|
| `journal_line` | `SUM(debit) = SUM(credit)` per entry | Deferred trigger | BR-01 |
| `journal_line` | Debit and credit cannot both be > 0 | Check | Accounting standard |
| `milestone_version` | `(milestone_id, version_number)` unique | Unique | BR-04 |
| `milestone_version` | `planned_amount >= 0` | Check | BR-10 |
| `actual_line` | `line_hash` unique where `is_duplicate = false` | Partial unique index | BR-08 |
| `reconciliation` | `actual_id` unique | Unique | BR-06 |
| `reconciliation` | `category` in allowed values | Check | BR-07 |
| `project` | `project_id` unique (PK) | Primary key | BR-12 |
| `project` | `(wbse, project_id)` unique | Unique | BR-13 |
| `contract` | `name` unique | Unique | Domain rule |

---

## 4. Key Indexes

| Index | Table | Columns | Purpose |
|-------|-------|---------|---------|
| `idx_milestone_version_lookup` | `milestone_version` | `(milestone_id, effective_date, version_number DESC)` | Time machine: latest version as of date |
| `idx_journal_line_balance` | `journal_line` | `(account, contract_id, fiscal_period_id)` | Variance/balance aggregation queries |
| `idx_reconciliation_accruals` | `reconciliation` | `(milestone_id, category)` WHERE category IN ('ACCRUAL','ACCRUAL_REVERSAL') | Accrual lifecycle queries |
| `idx_actual_line_not_dup` | `actual_line` | `(is_duplicate)` WHERE `is_duplicate = false` | Filter to active actuals |
| `idx_journal_entry_effective` | `journal_entry` | `(effective_date)` | Time machine filter |

---

## 5. Database-Level Enforcement vs. Service-Level

| Rule | DB Enforcement | Service Enforcement |
|------|---------------|-------------------|
| Journal balance (BR-01) | Deferred constraint trigger | Validation before save |
| Min 2 journal lines (BR-02) | — | Validation before save |
| Version effective_date ordering (BR-05) | — | Validation before save |
| Actual dedup (BR-08) | Partial unique index | Hash computation + lookup |
| One reconciliation per actual (BR-06) | Unique constraint | Check before create |
| Category enum (BR-07) | Check constraint | Enum validation |
| Amounts in BigDecimal (BR-10) | DECIMAL(15,2) | BigDecimal in Java |

The philosophy: **the database provides safety nets; the service layer provides user-friendly validation and error messages.**
