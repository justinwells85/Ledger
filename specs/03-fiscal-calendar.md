# Fiscal Calendar

> Source: DPI Accruals v2 Spec, Section 2.2

---

## 1. Overview

The system uses a Disney fiscal year calendar running October through September. All budgets, forecasts, and actuals are organized by fiscal period.

---

## 2. Structure

- A **Fiscal Year** spans 12 months (e.g., FY26 = Oct 2025 through Sep 2026)
- Each fiscal year contains **4 quarters** of 3 months each
- Each quarter contains **3 fiscal periods** (months)
- Fiscal periods are the finest granularity for budget planning and reporting

---

## 3. Multi-Year Support

The system must support multiple fiscal years simultaneously to enable:
- **Capital project tracking** that spans fiscal year boundaries
- **Q4 → Q1 transition planning** (planning FY27 Q1 while still in FY26 Q4)
- **Historical comparison** (FY25 actuals vs. FY26 plan)

Fiscal years are created as reference data. There is no hard limit on how many fiscal years exist in the system.

---

## 4. Period-to-Quarter Mapping

| Quarter | Month 1 | Month 2 | Month 3 |
|---------|---------|---------|---------|
| Q1 | October | November | December |
| Q2 | January | February | March |
| Q3 | April | May | June |
| Q4 | July | August | September |

---

## 5. Period Key Convention

Fiscal periods use a sort_order (1-12) for internal ordering. The display format is:

```
{Fiscal Year} {Quarter} - {Month Name}
Example: FY26 Q1 - October
```

**Legacy format:** The existing Excel uses keys like `FY26-01-OCT`. These will be stored as a `legacy_key` field for migration purposes but are not the primary identifier.

---

## 6. Posting Date to Period Resolution

When importing SAP actuals, the `posting_date` must be resolved to a fiscal period:

```
posting_date: 2026-01-15
  → calendar_month: 2026-01 (January)
  → fiscal_year: FY26 (Oct 2025 - Sep 2026)
  → quarter: Q2
  → period: FY26 Q2 - January
```

**Edge case:** A posting_date of October 1, 2025 maps to FY26 Q1, not FY25 Q4. The resolution logic must account for the October start of the fiscal year.

**Rule:** `fiscal_year = if month >= October then current calendar year + 1 else current calendar year` (expressed as FY{YY}).
