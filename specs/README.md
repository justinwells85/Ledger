# Ledger — Specification Index

These specs define the complete behavior of the Ledger application. They are the **source of truth** for all implementation work. Tests are derived from these specs. Code implements these specs.

## Spec Files

| # | File | Domain | Key Business Rules |
|---|------|--------|-------------------|
| 01 | [domain-model.md](./01-domain-model.md) | Entity definitions, relationships, field types | BR-06, BR-10, BR-11, BR-12, BR-13 |
| 02 | [journal-ledger.md](./02-journal-ledger.md) | Double-entry accounting, accounts, entry types, balance queries | BR-01, BR-02 |
| 03 | [fiscal-calendar.md](./03-fiscal-calendar.md) | Fiscal year/quarter/period structure, date resolution | BR-73 |
| 04 | [milestone-versioning.md](./04-milestone-versioning.md) | Independent versioning, plan adjustments, period shifts | BR-03, BR-04, BR-05, BR-40 through BR-45 |
| 05 | [sap-ingestion.md](./05-sap-ingestion.md) | File import, dedup, staging, commit workflow | BR-08, BR-09, BR-70 through BR-73 |
| 06 | [reconciliation.md](./06-reconciliation.md) | Manual matching, categories, tolerance, undo | BR-06, BR-07, BR-30 through BR-32 |
| 07 | [accrual-lifecycle.md](./07-accrual-lifecycle.md) | Accrual/reversal/invoice cycles, aging alerts | BR-20 through BR-24 |
| 08 | [time-machine.md](./08-time-machine.md) | Point-in-time queries, date filtering across all views | BR-41, BR-52, BR-53 |
| 09 | [reporting.md](./09-reporting.md) | Report definitions, columns, filters, aggregations | BR-50, BR-51, BR-52 |
| 10 | [business-rules.md](./10-business-rules.md) | Consolidated rule reference with test derivation guide | All |
| 11 | [change-management.md](./11-change-management.md) | Audit trail, required reasons, entity change tracking | BR-60 through BR-63 |
| 12 | [database-schema.md](./12-database-schema.md) | Schema diagram, migrations, constraints, indexes | All |
| 13 | [api-design.md](./13-api-design.md) | REST API — 37 endpoints, request/response contracts | All |
| 14 | [ui-views.md](./14-ui-views.md) | React views, wireframes, routes, view-to-API mapping | All |
| 15 | [tier1-tasks.md](./15-tier1-tasks.md) | 36 implementation tasks, dependency order, 124+ test cases | All |
| 16 | [test-plan.md](./16-test-plan.md) | Complete test plan: unit, integration, E2E, frontend, traceability matrix | All |

## Development Methodology

This project follows **spec-driven development with TDD**:

1. **Spec** defines the requirement (these files)
2. **Test spec** derives test cases from business rules (Given/When/Then)
3. **Test code** implements the test cases (JUnit 5) — these fail initially (red)
4. **Implementation** makes the tests pass (green)
5. **Verification** confirms implementation satisfies the spec

## Delivery Tiers

### Tier 1: Core (Current Focus)
- Domain model + CRUD (specs 01, 03)
- Journal ledger (spec 02)
- Milestone versioning (spec 04)
- SAP import with dedup (spec 05)
- Manual reconciliation with categories (spec 06)
- Accrual lifecycle tracking (spec 07)
- Time machine (spec 08)
- Budget plan, actuals, variance, and reconciliation reports (spec 09)
- Audit trail (specs 02, 11)
- Business rules enforcement (spec 10)

### Tier 2: Reporting + Forecasting
- Forecast report with adjustable remaining plan
- Funding source summary
- Cross-contract portfolio view
- Period-over-period comparison
- CSV/Excel export

### Tier 3: Smart Reconciliation
- Auto-match suggestion engine
- Confidence scoring
- Bulk reconciliation
- Pattern learning
- Notifications
