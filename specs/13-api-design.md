# API Design

> REST API for the Ledger application.
> All endpoints return JSON. All timestamps are ISO 8601. All amounts are string-encoded decimals (e.g., "25250.00") to avoid floating-point issues in JSON.

---

## 1. Conventions

### Base Path
```
/api/v1
```

### Common Query Parameters

| Parameter | Type | Description | Used By |
|-----------|------|-------------|---------|
| `asOfDate` | `YYYY-MM-DD` | Time machine — filters all data to this point in time. Omit for current state. | All GET endpoints |
| `page` | Integer | Page number (0-based) | List endpoints |
| `size` | Integer | Page size (default 50) | List endpoints |
| `sort` | String | Sort field and direction, e.g., `name,asc` | List endpoints |

### Standard Error Response
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Milestone version effective_date must be >= prior version effective_date",
  "timestamp": "2026-03-14T10:30:00Z"
}
```

### Audit Fields (included on all entity responses)
```json
{
  "createdAt": "2026-03-14T10:30:00Z",
  "createdBy": "Justin Anderson"
}
```

---

## 2. Fiscal Calendar

### GET /api/v1/fiscal-years

List all fiscal years.

**Response:**
```json
[
  {
    "fiscalYear": "FY26",
    "startDate": "2025-10-01",
    "endDate": "2026-09-30"
  }
]
```

### GET /api/v1/fiscal-years/{fiscalYear}/periods

List all periods for a fiscal year.

**Response:**
```json
[
  {
    "periodId": "uuid",
    "fiscalYear": "FY26",
    "periodKey": "FY26-01-OCT",
    "quarter": "Q1",
    "calendarMonth": "2025-10",
    "displayName": "October 2025",
    "sortOrder": 1
  }
]
```

---

## 3. Contracts

### GET /api/v1/contracts

List all contracts.

**Query params:** `status` (ACTIVE, CLOSED, TERMINATED), `asOfDate`

**Response:**
```json
{
  "content": [
    {
      "contractId": "uuid",
      "name": "Globant ADM",
      "vendor": "Globant",
      "description": "...",
      "ownerUser": "Rob Moore",
      "startDate": "2025-10-01",
      "endDate": "2026-09-30",
      "status": "ACTIVE",
      "projectCount": 9,
      "totalPlanned": "2109888.00",
      "totalActual": "1450000.00",
      "createdAt": "2026-01-15T10:00:00Z",
      "createdBy": "Brad Flechtner"
    }
  ],
  "totalElements": 8,
  "totalPages": 1
}
```

### GET /api/v1/contracts/{contractId}

Get a single contract with summary.

### POST /api/v1/contracts

Create a new contract.

**Request:**
```json
{
  "name": "Globant ADM",
  "vendor": "Globant",
  "description": "ADM services contract",
  "ownerUser": "Rob Moore",
  "startDate": "2025-10-01",
  "endDate": "2026-09-30"
}
```

**Response:** 201 Created with the full contract object.

### PUT /api/v1/contracts/{contractId}

Update contract metadata. Requires `reason` for audit.

**Request:**
```json
{
  "name": "Globant ADM",
  "vendor": "Globant",
  "description": "Updated description",
  "ownerUser": "Rob Moore",
  "startDate": "2025-10-01",
  "endDate": "2026-09-30",
  "status": "ACTIVE",
  "reason": "Updated description to reflect new scope"
}
```

---

## 4. Projects

### GET /api/v1/contracts/{contractId}/projects

List projects for a contract.

**Query params:** `status`, `fundingSource`, `asOfDate`

**Response:**
```json
{
  "content": [
    {
      "projectId": "PR13752",
      "contractId": "uuid",
      "wbse": "1174905.SU.ES",
      "name": "DPI - Photopass - SUS Break/Fix",
      "fundingSource": "OPEX",
      "status": "ACTIVE",
      "milestoneCount": 12,
      "totalPlanned": "1227626.00",
      "totalActual": "850000.00",
      "createdAt": "2026-01-15T10:00:00Z",
      "createdBy": "Brad Flechtner"
    }
  ]
}
```

### GET /api/v1/projects/{projectId}

Get a single project with summary.

### POST /api/v1/contracts/{contractId}/projects

Create a project under a contract.

**Request:**
```json
{
  "projectId": "PR13752",
  "wbse": "1174905.SU.ES",
  "name": "DPI - Photopass - SUS Break/Fix",
  "fundingSource": "OPEX"
}
```

### PUT /api/v1/projects/{projectId}

Update project metadata. Requires `reason`.

---

## 5. Milestones

### GET /api/v1/projects/{projectId}/milestones

List milestones for a project, with current (or as-of-date) version details.

**Query params:** `fiscalPeriodId`, `asOfDate`

**Response:**
```json
{
  "content": [
    {
      "milestoneId": "uuid",
      "projectId": "PR13752",
      "name": "January Sustainment",
      "description": "Monthly sustainment payment",
      "currentVersion": {
        "versionId": "uuid",
        "versionNumber": 2,
        "plannedAmount": "20000.00",
        "fiscalPeriodId": "uuid",
        "fiscalPeriodDisplay": "January 2026",
        "effectiveDate": "2026-02-15",
        "reason": "Q2 scope cut",
        "createdAt": "2026-02-15T14:00:00Z",
        "createdBy": "Justin Anderson"
      },
      "reconciliationSummary": {
        "invoiceTotal": "0.00",
        "accrualNet": "25000.00",
        "allocationTotal": "0.00",
        "totalActual": "25000.00",
        "remaining": "-5000.00",
        "openAccrualCount": 1,
        "status": "OVER_BUDGET"
      },
      "createdAt": "2025-11-01T09:00:00Z",
      "createdBy": "Brad Flechtner"
    }
  ]
}
```

### GET /api/v1/milestones/{milestoneId}

Get a single milestone with current version and reconciliation summary.

### POST /api/v1/projects/{projectId}/milestones

Create a milestone (automatically creates version 1).

**Request:**
```json
{
  "name": "January Sustainment",
  "description": "Monthly sustainment payment",
  "plannedAmount": "25250.00",
  "fiscalPeriodId": "uuid",
  "effectiveDate": "2025-11-01",
  "reason": "Initial budget allocation"
}
```

**Response:** 201 Created. The response includes the milestone with its v1.

### GET /api/v1/milestones/{milestoneId}/versions

List all versions of a milestone (full history).

**Response:**
```json
[
  {
    "versionId": "uuid",
    "versionNumber": 1,
    "plannedAmount": "25250.00",
    "fiscalPeriodId": "uuid",
    "fiscalPeriodDisplay": "January 2026",
    "effectiveDate": "2025-11-01",
    "reason": "Initial budget allocation",
    "createdAt": "2025-11-01T09:00:00Z",
    "createdBy": "Brad Flechtner"
  },
  {
    "versionId": "uuid",
    "versionNumber": 2,
    "plannedAmount": "20000.00",
    "fiscalPeriodId": "uuid",
    "fiscalPeriodDisplay": "January 2026",
    "effectiveDate": "2026-02-15",
    "reason": "Q2 scope cut",
    "createdAt": "2026-02-15T14:00:00Z",
    "createdBy": "Justin Anderson"
  }
]
```

### POST /api/v1/milestones/{milestoneId}/versions

Create a new version (plan adjustment). Creates the PLAN_ADJUST journal entry.

**Request:**
```json
{
  "plannedAmount": "20000.00",
  "fiscalPeriodId": "uuid",
  "effectiveDate": "2026-02-15",
  "reason": "Q2 scope cut — removed testing deliverable"
}
```

**Validations:**
- `effectiveDate` >= prior version's effective_date (BR-05)
- `reason` required (BR-42)
- `plannedAmount` >= 0 (BR-10)

### POST /api/v1/milestones/{milestoneId}/cancel

Convenience endpoint to cancel a milestone (creates version with amount = 0).

**Request:**
```json
{
  "effectiveDate": "2026-03-01",
  "reason": "Project descoped from FY26"
}
```

---

## 6. SAP Import

### POST /api/v1/imports/upload

Upload a SAP export file. Returns a staged import with summary.

**Request:** `multipart/form-data` with file field.

**Response:** 201 Created
```json
{
  "importId": "uuid",
  "filename": "SAP_FY26_March.csv",
  "status": "STAGED",
  "totalLines": 150,
  "newLines": 45,
  "duplicateLines": 105,
  "errorLines": 0,
  "importedAt": "2026-03-14T10:30:00Z",
  "importedBy": "Justin Anderson"
}
```

### GET /api/v1/imports/{importId}

Get import details including summary counts.

### GET /api/v1/imports/{importId}/lines

Get the parsed lines for a staged import (for review).

**Query params:** `filter` (NEW, DUPLICATE, ERROR), `page`, `size`

**Response:**
```json
{
  "content": [
    {
      "lineIndex": 1,
      "sapDocumentNumber": "5100012345",
      "postingDate": "2026-01-31",
      "amount": "25000.00",
      "vendorName": "Globant S.A.",
      "costCenter": "",
      "wbse": "1174905.SU.ES",
      "glAccount": "",
      "description": "Invoice #12345 - January services",
      "status": "NEW",
      "duplicateOfImportId": null
    }
  ]
}
```

### POST /api/v1/imports/{importId}/commit

Commit a staged import. Creates actual lines and journal entries for new lines.

**Validations:**
- Import must be in STAGED status
- At least one new line must exist

**Response:**
```json
{
  "importId": "uuid",
  "status": "COMMITTED",
  "newLinesCommitted": 45,
  "journalEntriesCreated": 45
}
```

### POST /api/v1/imports/{importId}/reject

Reject a staged import. No actuals are created.

**Response:**
```json
{
  "importId": "uuid",
  "status": "REJECTED"
}
```

### GET /api/v1/imports

List all imports (history).

**Query params:** `status`, `page`, `size`, `sort`

---

## 7. Actual Lines

### GET /api/v1/actuals

List actual lines with reconciliation status.

**Query params:**
- `reconciled` — `true`, `false`, or omit for all
- `fiscalPeriodId` — filter to a specific period
- `vendorName` — contains match
- `description` — contains match
- `minAmount`, `maxAmount` — amount range
- `postingDateFrom`, `postingDateTo` — date range
- `asOfDate` — time machine
- `page`, `size`, `sort`

**Response:**
```json
{
  "content": [
    {
      "actualId": "uuid",
      "importId": "uuid",
      "sapDocumentNumber": "5100012345",
      "postingDate": "2026-01-31",
      "fiscalPeriodDisplay": "January 2026",
      "amount": "25000.00",
      "vendorName": "Globant S.A.",
      "costCenter": "",
      "wbse": "1174905.SU.ES",
      "glAccount": "",
      "description": "Invoice #12345",
      "importedAt": "2026-03-14T10:30:00Z",
      "reconciliation": {
        "reconciliationId": "uuid",
        "milestoneId": "uuid",
        "milestoneName": "January Sustainment",
        "projectId": "PR13752",
        "contractName": "Globant ADM",
        "category": "INVOICE",
        "matchNotes": "Matched by vendor + amount",
        "reconciledAt": "2026-03-14T11:00:00Z",
        "reconciledBy": "Justin Anderson"
      }
    }
  ]
}
```

When `reconciliation` is `null`, the actual is unreconciled.

---

## 8. Reconciliation

### GET /api/v1/reconciliation/unreconciled

List unreconciled actuals (convenience alias for `GET /api/v1/actuals?reconciled=false`).

Same query params and response as `GET /api/v1/actuals`.

### GET /api/v1/reconciliation/candidates/{actualId}

Get candidate milestones for reconciling a specific actual.

**Response:**
```json
[
  {
    "milestoneId": "uuid",
    "milestoneName": "January Sustainment",
    "projectId": "PR13752",
    "projectName": "DPI - Photopass - SUS Break/Fix",
    "contractId": "uuid",
    "contractName": "Globant ADM",
    "wbse": "1174905.SU.ES",
    "fiscalPeriodDisplay": "January 2026",
    "plannedAmount": "25250.00",
    "reconciledTotal": "0.00",
    "remaining": "25250.00",
    "relevanceScore": 95
  }
]
```

Candidates are sorted by `relevanceScore` (descending). Score is based on:
1. WBSE match (if actual has a WBSE)
2. Same fiscal period
3. Closest amount match

### POST /api/v1/reconciliation

Create a reconciliation (assign actual to milestone).

**Request:**
```json
{
  "actualId": "uuid",
  "milestoneId": "uuid",
  "category": "INVOICE",
  "matchNotes": "Matched by vendor name + amount + period"
}
```

**Validations:**
- Actual must not already be reconciled (BR-06)
- Actual must not be a duplicate (is_duplicate = false)
- Category must be valid (BR-07)

**Response:** 201 Created
```json
{
  "reconciliationId": "uuid",
  "actualId": "uuid",
  "milestoneId": "uuid",
  "category": "INVOICE",
  "matchNotes": "...",
  "reconciledAt": "2026-03-14T11:00:00Z",
  "reconciledBy": "Justin Anderson"
}
```

### DELETE /api/v1/reconciliation/{reconciliationId}

Undo a reconciliation. Requires reason (BR-62).

**Request:**
```json
{
  "reason": "Matched to wrong milestone — should be February not January"
}
```

**Response:** 200 OK

---

## 9. Journal

### GET /api/v1/journal

List journal entries with their lines.

**Query params:**
- `entryType` — filter by type
- `contractId` — filter by contract (via journal lines)
- `milestoneId` — filter by milestone
- `effectiveDateFrom`, `effectiveDateTo` — date range
- `createdBy` — filter by user
- `asOfDate` — time machine
- `page`, `size`, `sort`

**Response:**
```json
{
  "content": [
    {
      "entryId": "uuid",
      "entryDate": "2026-02-15T14:00:00Z",
      "effectiveDate": "2026-02-15",
      "entryType": "PLAN_ADJUST",
      "description": "Budget reduction: January Sustainment reduced per Q2 scope cut",
      "createdBy": "Justin Anderson",
      "lines": [
        {
          "lineId": "uuid",
          "account": "VARIANCE_RESERVE",
          "contractName": "Globant ADM",
          "projectId": "PR01570",
          "milestoneName": "January Sustainment",
          "fiscalPeriodDisplay": "January 2026",
          "debit": "5250.00",
          "credit": "0.00"
        },
        {
          "lineId": "uuid",
          "account": "PLANNED",
          "contractName": "Globant ADM",
          "projectId": "PR01570",
          "milestoneName": "January Sustainment",
          "fiscalPeriodDisplay": "January 2026",
          "debit": "0.00",
          "credit": "5250.00"
        }
      ]
    }
  ]
}
```

---

## 10. Reports

### GET /api/v1/reports/budget

Budget plan report.

**Query params:**
- `contractId` — optional filter
- `projectId` — optional filter
- `fiscalYear` — required
- `fundingSource` — optional filter
- `groupBy` — `contract`, `project`, `milestone` (default: `project`)
- `periodGrouping` — `month`, `quarter` (default: `month`)
- `asOfDate` — time machine

**Response:**
```json
{
  "fiscalYear": "FY26",
  "asOfDate": null,
  "rows": [
    {
      "contractId": "uuid",
      "contractName": "Globant ADM",
      "projectId": "PR13752",
      "projectName": "DPI - Photopass - SUS Break/Fix",
      "fundingSource": "OPEX",
      "periods": {
        "FY26-01-OCT": "112129.00",
        "FY26-02-NOV": "109947.00",
        "FY26-03-DEC": "109947.00"
      },
      "total": "1227626.00"
    }
  ],
  "grandTotal": "5100000.00"
}
```

### GET /api/v1/reports/variance

Variance report (plan vs. actual).

**Query params:**
- `contractId`, `projectId`, `fiscalYear` (required), `fundingSource`
- `groupBy` — `contract`, `project`, `milestone`
- `periodGrouping` — `month`, `quarter`
- `asOfDate`

**Response:**
```json
{
  "fiscalYear": "FY26",
  "rows": [
    {
      "contractName": "Globant ADM",
      "projectName": "DPI - Photopass - SUS Break/Fix",
      "periods": {
        "FY26-04-JAN": {
          "planned": "67147.00",
          "actual": "72000.00",
          "variance": "-4853.00",
          "variancePercent": -7.2,
          "status": "OVER_BUDGET"
        }
      },
      "totalPlanned": "1227626.00",
      "totalActual": "850000.00",
      "totalVariance": "377626.00",
      "totalVariancePercent": 30.8,
      "totalStatus": "UNDER_BUDGET"
    }
  ]
}
```

### GET /api/v1/reports/reconciliation-status

Reconciliation status per milestone.

**Query params:**
- `contractId`, `projectId`, `fiscalYear`, `fiscalPeriodId`
- `status` — `FULLY_RECONCILED`, `PARTIALLY_MATCHED`, `UNMATCHED`, `OVER_BUDGET`
- `asOfDate`

**Response:**
```json
{
  "rows": [
    {
      "milestoneId": "uuid",
      "milestoneName": "January Sustainment",
      "contractName": "Globant ADM",
      "projectName": "DPI : FY26 Sustainment",
      "fiscalPeriodDisplay": "January 2026",
      "planned": "25250.00",
      "invoiceTotal": "25000.00",
      "accrualNet": "0.00",
      "allocationTotal": "0.00",
      "totalActual": "25000.00",
      "remaining": "250.00",
      "openAccrualCount": 0,
      "status": "WITHIN_TOLERANCE"
    }
  ]
}
```

### GET /api/v1/reports/forecast

Forecast report.

**Query params:**
- `contractId`, `projectId`, `fiscalYear` (required), `fundingSource`
- `groupBy` — `contract`, `project`

**Response:**
```json
{
  "fiscalYear": "FY26",
  "rows": [
    {
      "contractName": "Globant ADM",
      "originalPlan": "2109888.00",
      "actualsYtd": "1450000.00",
      "remainingPlan": "659888.00",
      "forecastTotal": "2109888.00",
      "forecastVsPlan": "0.00"
    }
  ]
}
```

### GET /api/v1/reports/funding-summary

Funding source summary.

**Query params:** `fiscalYear` (required), `asOfDate`

**Response:**
```json
{
  "rows": [
    {
      "fundingSource": "OPEX",
      "plannedTotal": "3200000.00",
      "actualYtd": "1800000.00",
      "forecastTotal": "3150000.00",
      "variance": "50000.00"
    }
  ]
}
```

### GET /api/v1/reports/open-accruals

Open accruals aging report.

**Query params:** `contractId`, `status` (OPEN, AGING_WARNING, AGING_CRITICAL)

**Response:**
```json
{
  "rows": [
    {
      "milestoneId": "uuid",
      "milestoneName": "February Services",
      "contractName": "Globant ADM",
      "projectName": "DPI - Photopass",
      "accrualAmount": "25000.00",
      "accrualDate": "2026-02-28",
      "ageDays": 14,
      "status": "OPEN"
    }
  ]
}
```

---

## 11. Audit

### GET /api/v1/audit/contract/{contractId}

All changes for a contract (journal entries + audit log).

### GET /api/v1/audit/milestone/{milestoneId}

Full history for a milestone (versions + reconciliations + journal entries).

### GET /api/v1/audit/user/{username}

All changes by a specific user.

**Query params:** `dateFrom`, `dateTo`, `page`, `size`

### GET /api/v1/audit/changes

All changes in a date range.

**Query params:** `dateFrom` (required), `dateTo` (required), `entityType`, `page`, `size`

---

## 12. Configuration

### GET /api/v1/config

List all system configuration values.

### PUT /api/v1/config/{configKey}

Update a configuration value.

**Request:**
```json
{
  "value": "0.03",
  "reason": "Increased tolerance to 3% per Q2 review"
}
```

---

## 13. Endpoint Summary

| Method | Path | Purpose | Spec |
|--------|------|---------|------|
| GET | `/fiscal-years` | List fiscal years | 03 |
| GET | `/fiscal-years/{fy}/periods` | List periods | 03 |
| GET | `/contracts` | List contracts | 01 |
| GET | `/contracts/{id}` | Get contract | 01 |
| POST | `/contracts` | Create contract | 01 |
| PUT | `/contracts/{id}` | Update contract | 01, 11 |
| GET | `/contracts/{id}/projects` | List projects | 01 |
| POST | `/contracts/{id}/projects` | Create project | 01 |
| GET | `/projects/{id}` | Get project | 01 |
| PUT | `/projects/{id}` | Update project | 01, 11 |
| GET | `/projects/{id}/milestones` | List milestones | 04 |
| POST | `/projects/{id}/milestones` | Create milestone + v1 | 04 |
| GET | `/milestones/{id}` | Get milestone | 04 |
| GET | `/milestones/{id}/versions` | Version history | 04 |
| POST | `/milestones/{id}/versions` | Create new version | 04 |
| POST | `/milestones/{id}/cancel` | Cancel milestone | 04 |
| POST | `/imports/upload` | Upload SAP file | 05 |
| GET | `/imports/{id}` | Get import details | 05 |
| GET | `/imports/{id}/lines` | Get import lines | 05 |
| POST | `/imports/{id}/commit` | Commit import | 05 |
| POST | `/imports/{id}/reject` | Reject import | 05 |
| GET | `/imports` | List imports | 05 |
| GET | `/actuals` | List actuals | 05 |
| GET | `/reconciliation/unreconciled` | Unreconciled actuals | 06 |
| GET | `/reconciliation/candidates/{actualId}` | Match candidates | 06 |
| POST | `/reconciliation` | Create reconciliation | 06 |
| DELETE | `/reconciliation/{id}` | Undo reconciliation | 06 |
| GET | `/journal` | List journal entries | 02 |
| GET | `/reports/budget` | Budget plan report | 09 |
| GET | `/reports/variance` | Variance report | 09 |
| GET | `/reports/reconciliation-status` | Reconciliation status | 09 |
| GET | `/reports/forecast` | Forecast report | 09 |
| GET | `/reports/funding-summary` | Funding source summary | 09 |
| GET | `/reports/open-accruals` | Accrual aging report | 07 |
| GET | `/audit/contract/{id}` | Contract audit trail | 11 |
| GET | `/audit/milestone/{id}` | Milestone audit trail | 11 |
| GET | `/audit/user/{username}` | User activity | 11 |
| GET | `/audit/changes` | Changes in date range | 11 |
| GET | `/config` | List config | 10 |
| PUT | `/config/{key}` | Update config | 10 |

**Total: 37 endpoints**
