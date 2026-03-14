# SAP Actuals Ingestion

> Source: DPI Accruals v2 Spec, Section 5

---

## 1. Overview

The system imports SAP cost reports (CSV or Excel) to capture actual expenditures. Imports happen at least monthly, typically as a full fiscal-year re-pull. The system must deduplicate against previously imported lines to prevent double-counting.

---

## 2. Import Pipeline

```
UPLOAD → PARSE → DEDUP → STAGE → REVIEW → COMMIT (or REJECT)
```

### Step 1: Upload

User uploads a SAP export file (CSV or Excel format).

**Accepted formats:**
- CSV (comma or tab delimited)
- XLSX / XLS

### Step 2: Parse

System reads all rows from the file and maps columns to the Actual Line schema.

**Column mapping** should be configurable or auto-detected from headers. Expected SAP fields:

| SAP Field | Maps To | Required | Typically Present |
|-----------|---------|----------|-------------------|
| Document Number | `sap_document_number` | No | Sometimes |
| Posting Date | `posting_date` | Yes | Yes |
| Amount | `amount` | Yes | Yes |
| Vendor Name | `vendor_name` | No | Sometimes |
| Cost Center | `cost_center` | No | Sometimes |
| WBS Element | `wbse` | No | Sometimes |
| GL Account | `gl_account` | No | Sometimes |
| Text / Description | `description` | No | Usually |

**Validation during parse:**
- Rows missing `posting_date` or `amount` are flagged as errors
- Amount must be a valid number (positive or negative)
- Posting date must be a valid date

### Step 3: Dedup

For each parsed line, compute a hash for dedup comparison:

```
line_hash = SHA-256(
  normalize(sap_document_number) +
  normalize(posting_date) +
  normalize(amount) +
  normalize(vendor_name) +
  normalize(cost_center) +
  normalize(wbse) +
  normalize(gl_account) +
  normalize(description)
)
```

**Normalization rules:**
- Trim leading/trailing whitespace
- Convert to uppercase
- Standardize number format (remove currency symbols, normalize to 2 decimal places)
- Null/empty fields normalized to empty string
- Date format normalized to ISO 8601 (YYYY-MM-DD)

**Dedup logic:**
- Search existing `actual_line` records for matching `line_hash`
- If match found → mark as `is_duplicate = true`, will be skipped on commit
- If no match → mark as `is_duplicate = false`, will be imported on commit

### Step 4: Stage

Create an `SAP Import` record with status = `STAGED`. Populate summary counts:
- `total_lines` = all parsed rows
- `new_lines` = rows where `is_duplicate = false`
- `duplicate_lines` = rows where `is_duplicate = true`
- Rows with parse errors are flagged separately

### Step 5: Review

Present the staged import to the user for review:

**Summary view:**
- Filename, upload date, uploaded by
- Total lines, new lines, duplicate lines, error lines
- Date range of new lines (earliest/latest posting date)
- Total amount of new lines

**Detail view (optional drill-down):**
- List of all new lines with SAP fields
- List of duplicate lines (with reference to when they were originally imported)
- List of error lines with error descriptions

**User actions:**
- **Commit** — proceed with importing new lines
- **Reject** — discard the entire import

### Step 6: Commit

For each non-duplicate, non-error line:
1. Create `Actual Line` record with all fields + `line_hash`
2. Resolve `fiscal_period_id` from `posting_date` (see [03-fiscal-calendar.md](./03-fiscal-calendar.md) Section 6)
3. Create `ACTUAL_IMPORT` journal entry:
   - Debit ACTUAL [amount] for [fiscal_period]
   - Credit VARIANCE_RESERVE [amount] for [fiscal_period]
   - Contract/project/milestone are null at this point
4. Update `SAP Import` status to `COMMITTED`

**For negative amounts (accrual reversals, credit memos):**
- The journal entry is still Debit ACTUAL / Credit VARIANCE_RESERVE
- A negative debit effectively reduces the ACTUAL balance
- This is correct — the net ACTUAL balance reflects true spend

---

## 3. Dedup Edge Cases

| Scenario | Behavior |
|----------|----------|
| Exact duplicate (same hash) from a re-pull | Marked as duplicate, skipped |
| SAP corrected a line (same doc number, different amount) | Different hash → imported as new line. Both exist. User resolves during reconciliation |
| Same amount/date/vendor but different description | Different hash → imported as separate lines. May be a true duplicate or two distinct charges. User resolves during reconciliation |
| Empty doc number on both lines but all other fields match | Same hash → dedup works correctly |

---

## 4. Error Handling

| Error | Behavior |
|-------|----------|
| File cannot be parsed (corrupt, wrong format) | Reject upload with error message |
| Individual row missing required field | Flag row as error, continue parsing remaining rows |
| No new lines in import (all duplicates) | Allow commit (records the import attempt) but warn user |
| Posting date outside any defined fiscal year | Flag row as warning — import proceeds but period resolution marks as "unresolved period" |

---

## 5. Import History

All imports are retained regardless of status:
- `STAGED` — uploaded but not yet reviewed (should be cleaned up after a timeout)
- `COMMITTED` — successfully imported
- `REJECTED` — user chose not to import

Import history is queryable for audit purposes: who imported what, when, how many lines, how many duplicates.
