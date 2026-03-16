# Business Rules

> Source: DPI Accruals v2 Spec, Section 11

---

## 1. Data Integrity

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-01 | Every journal entry must balance: SUM(debit) = SUM(credit) | Database constraint + service validation |
| BR-02 | Every journal entry must have at least 2 lines | Service validation |
| BR-03 | Every milestone must have at least one version (v1 created at milestone creation) | Service logic — milestone creation always produces v1 |
| BR-04 | Milestone version numbers are sequential per milestone and immutable | Service logic — auto-assigned, never user-provided |
| BR-05 | A milestone version's effective_date must be >= the prior version's effective_date | Service validation |
| BR-06 | An actual line can be reconciled to at most one milestone | Database UNIQUE constraint on reconciliation.actual_id |
| BR-07 | Reconciliation category is required and must be one of: INVOICE, ACCRUAL, ACCRUAL_REVERSAL, ALLOCATION | Enum validation |
| BR-08 | SAP imports are deduplicated by full-line SHA-256 hash before committing | Import pipeline logic |
| BR-09 | Duplicate SAP lines are recorded (is_duplicate = true) but not committed as financial actuals | Import pipeline logic |
| BR-10 | All amounts are in USD with Decimal(15,2) precision | Schema constraint |
| BR-11 | Funding source is assigned at the Project/WBSE level and applies to all milestones within | Domain model — funding_source on Project entity |
| BR-12 | Project ID must be unique across the system | Database UNIQUE constraint |
| BR-13 | WBSE + Project ID combination must be unique | Database UNIQUE constraint |

---

## 2. Accrual Rules

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-20 | Reconciliations with category = ACCRUAL are tracked as "open" until a corresponding ACCRUAL_REVERSAL exists on the same milestone | Derived status query |
| BR-21 | Open accruals exceeding the aging warning threshold generate a warning | Reporting query |
| BR-22 | Open accruals exceeding the aging critical threshold generate a critical alert | Reporting query |
| BR-23 | The net of all ACCRUAL + ACCRUAL_REVERSAL amounts for a milestone should trend toward $0 | Informational — reported, not enforced |
| BR-24 | ACCRUAL_REVERSAL amounts are expected to be negative; system warns if positive | Service validation (warning, not blocking) |

---

## 3. Tolerance Rules

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-30 | A milestone is "within tolerance" if \|Remaining\| <= absolute_threshold OR \|Remaining\|/Planned <= percentage_threshold | Derived status calculation |
| BR-31 | Tolerance thresholds are system-level configuration values | Configuration table |
| BR-32 | Default tolerance: 2% or $50, whichever is met first | Configuration defaults |

---

## 4. Versioning Rules

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-40 | The "current plan" for a milestone = the version with the highest version_number | Query logic |
| BR-41 | The "plan as of date X" = the latest version with effective_date <= X | Query logic |
| BR-42 | A new milestone version (v2+) requires a reason | Service validation |
| BR-43 | Every new milestone version produces a PLAN_ADJUST journal entry with the delta | Service logic |
| BR-44 | Cancelling a milestone = new version with planned_amount = 0 | Service logic |
| BR-45 | A milestone's fiscal period can change between versions; journal reflects period shift | Service logic — 4-line journal entry |

---

## 5. Reporting Rules

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-50 | Forecast = Actuals YTD + Remaining Plan (current versions for future periods) | Report calculation |
| BR-51 | Variance = Planned - Actual for a given scope; positive = under budget | Report calculation |
| BR-52 | All reports respect the time machine date when active | Service layer — asOfDate parameter |
| BR-53 | Time machine date cannot be in the future | Service validation |

---

## 6. Audit Rules

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-60 | All entity mutations are recorded in the journal or audit log | Service logic |
| BR-61 | Milestone version changes require a reason | Service validation |
| BR-62 | Reconciliation undo requires a reason | Service validation |
| BR-63 | User identity (created_by) is captured from system context on every write | Service/security layer |

---

## 7. Import Rules

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-70 | SAP import lines missing posting_date or amount are rejected | Parse validation |
| BR-71 | SAP import must be staged and reviewed before committing | Workflow enforcement — status transitions |
| BR-72 | Committed imports cannot be un-committed (actuals can only be addressed through reconciliation) | Service logic |
| BR-73 | Posting date is resolved to fiscal period using the fiscal calendar | Import pipeline logic |

---

## 9. Admin & Configuration Rules

### User Management

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-80 | Every authenticated user has exactly one role (ADMIN, FINANCE_MANAGER, ANALYST, READ_ONLY) | Service/security layer |
| BR-81 | Deactivated users cannot authenticate; their historical write records are preserved | Auth layer |
| BR-82 | Only ADMIN role can manage user accounts, reference data, and system configuration | Security layer (@PreAuthorize) |
| BR-83 | `created_by` on all mutations is populated from the authenticated user's username, not a hardcoded value | Service layer |
| BR-84 | The last remaining active ADMIN account cannot be deactivated | Service validation |

### Fiscal Year Management

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-85 | Fiscal year labels follow the format `FY##` (e.g., FY28) | Input validation |
| BR-86 | A fiscal year spans exactly October 1 through September 30 of the following calendar year | Service logic |
| BR-87 | Creating a fiscal year automatically generates all 12 fiscal periods with correct keys, quarters, and sort orders | Service logic |
| BR-88 | Period keys follow the format `{FY}-{NN}-{MMM}` (e.g., FY28-01-OCT) | Service logic |
| BR-89 | A fiscal year cannot be deleted if any milestones, actuals, or journal entries reference its periods | Service validation |
| BR-90 | Fiscal years must be created in sequential order — no gaps allowed | Service validation |

### Reference Data Management

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-91 | Reference data codes (funding sources, statuses, categories) must be unique, uppercase, and contain only alphanumeric characters and underscores | Input validation |
| BR-92 | Deactivated reference values are hidden from new-entry dropdowns but preserved on all existing records | Service/query layer |
| BR-93 | A reference value with active records referencing it cannot be deleted; it can only be deactivated | Service validation |
| BR-94 | The ACTIVE contract status is system-reserved and cannot be deactivated | Service validation |
| BR-95 | Contract status transitions are restricted: ACTIVE → CLOSED and ACTIVE → TERMINATED only | Service validation |
| BR-96 | ACCRUAL and ACCRUAL_REVERSAL reconciliation categories are system-reserved and cannot be deactivated | Service validation |
| BR-97 | A reconciliation category cannot be deleted if any reconciliation records reference it | Service validation |

### System Configuration

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-98 | All system configuration changes require a documented reason (extends BR-31) | Controller validation |
| BR-99 | Configuration values are validated against their declared data_type before saving | Service validation |
| BR-100 | `accrual_aging_critical_days` must always be greater than `accrual_aging_warning_days` | Cross-field service validation |
| BR-101 | New configuration keys can be added via admin UI without code changes | Data-driven Settings UI |

---

## 8. Test Derivation Guide

Each business rule maps to one or more test cases. Use this table to ensure coverage:

| Rule | Test Category | Example Test |
|------|--------------|-------------|
| BR-01 | Unit | "Journal entry with unbalanced lines is rejected" |
| BR-01 | Unit | "PLAN_CREATE entry balances: debit PLANNED = credit VARIANCE_RESERVE" |
| BR-05 | Unit | "Version with effective_date before prior version is rejected" |
| BR-06 | Integration | "Reconciling an already-reconciled actual returns error" |
| BR-08 | Integration | "Importing same file twice produces zero new lines on second import" |
| BR-20 | Integration | "Accrual without reversal shows as open accrual" |
| BR-24 | Integration | "Positive ACCRUAL_REVERSAL produces a warning" |
| BR-30 | Unit | "Milestone with $45 remaining on $25,000 plan is within tolerance at 2%/$50" |
| BR-41 | Integration | "Time machine query returns correct version for given date" |
| BR-44 | Integration | "Cancelled milestone shows $0 planned in budget report" |
| BR-52 | Integration | "Variance report with time machine excludes future data" |
| BR-72 | Integration | "Attempting to un-commit an import returns error" |

See individual spec files for more detailed test scenarios.
