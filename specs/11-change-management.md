# Change Management & Audit

> Source: DPI Accruals v2 Spec, Section 10

---

## 1. Overview

Every mutation in the system is recorded for audit purposes. The double-entry journal provides financial audit. This spec covers the broader change management layer for all entity changes.

---

## 2. Audit Sources

The system has two complementary audit mechanisms:

### 2.1 Journal Ledger (Financial Audit)

Covers all financial events. See [02-journal-ledger.md](./02-journal-ledger.md).

| Event | Journal Entry Type |
|-------|-------------------|
| Milestone created | PLAN_CREATE |
| Milestone version added | PLAN_ADJUST |
| SAP actual imported | ACTUAL_IMPORT |
| Actual reconciled to milestone | RECONCILE |
| Reconciliation undone | RECONCILE_UNDO |

### 2.2 Entity Audit Log (Non-Financial Changes)

Covers metadata changes that don't produce journal entries.

| Event | What's Captured |
|-------|----------------|
| Contract created | All field values, created_by, created_at |
| Contract updated | Changed fields (old → new), updated_by, updated_at, reason |
| Project created | All field values, created_by, created_at |
| Project updated | Changed fields (old → new), updated_by, updated_at, reason |
| Contract status change | Old status → new status, reason |
| Project status change | Old status → new status, reason |
| Configuration changed | Setting name, old value, new value, changed_by |

---

## 3. Audit Log Entity

| Field | Type | Description |
|-------|------|-------------|
| `audit_id` | UUID (PK) | Auto-generated |
| `entity_type` | String | CONTRACT, PROJECT, CONFIGURATION |
| `entity_id` | UUID | ID of the changed entity |
| `action` | String | CREATE, UPDATE, STATUS_CHANGE |
| `changes` | JSON | `{"field": {"old": "value", "new": "value"}, ...}` |
| `reason` | String | Required for updates and status changes |
| `created_at` | Timestamp | When the change occurred |
| `created_by` | String | System user who made the change |

---

## 4. Required Reasons

The following actions **require** a reason/comment:

| Action | Why |
|--------|-----|
| Creating a new milestone version (v2+) | Captured in MilestoneVersion.reason |
| Undoing a reconciliation | Captured in RECONCILE_UNDO journal entry description |
| Updating contract metadata | Captured in audit_log.reason |
| Updating project metadata | Captured in audit_log.reason |
| Changing contract/project status | Captured in audit_log.reason |

The following actions capture context **automatically**:

| Action | Auto-Captured Context |
|--------|----------------------|
| SAP import | Filename, line counts, user, timestamp |
| Reconciliation | Category, optional notes, user, timestamp |
| Milestone creation (v1) | Inherent in milestone + version details |

---

## 5. Audit Queries

### What changed on a specific contract?

All journal entries + audit log entries for that contract_id, ordered by date.

### What did a specific user do?

All journal entries + audit log entries where created_by = user, ordered by date.

### History of a specific milestone

1. All MilestoneVersions (plan history)
2. All Reconciliations (actual assignments)
3. All journal entries referencing that milestone_id

### What changed between two dates?

All journal entries + audit log entries where created_at is between date A and date B, grouped by entity type.

---

## 6. User Identity

The system should capture the current authenticated user for all audit records. The `created_by` / `updated_by` / `reconciled_by` fields are populated from the security context (session user).

**Tier 1 implementation:** Simple user identification from the application session. No role-based access control. All users can perform all actions.

**Future consideration:** Role-based permissions (e.g., only contract owners can adjust milestones for their contracts). Deferred to Tier 2+.
