# Ledger Project Memory

## Project
- Path: `/Users/justin.w.anderson/development/Ledger` (case-insensitive: `/Development/Ledger`)
- Build: Gradle (`./gradlew test`)
- Stack: Java 21 + Spring Boot 3.4.3 + PostgreSQL + Flyway + Testcontainers

## TDD Progress (as of 2026-03-15)
- T01–T24: Complete (reports phase done)
- T25: Entity Audit Log ✅ (3 tests)
- T26: Audit Query APIs ✅ (3 tests)
- T27: System Config CRUD ✅ (4 tests)
- T37: RBAC + individual user accounts ✅ (8 backend + 8 frontend tests)
- T38: Fiscal year creation API + AdminFiscalYears page ✅ (6 backend + 4 frontend)
- T39: Reference data tables V011 + AdminReferenceData page ✅ (5 backend + 4 frontend)
- T40: Settings data-driven V012 + updated Settings.tsx ✅ (2 backend + 3 frontend)
- T41: Audit log viewer (filtered GET + CSV export) + AdminAuditLog page ✅ (5 backend + 4 frontend)
- T43: ReconcileWorkspace undo + filters ✅
- T45: ForecastReport + FundingReport ✅
- Backend total: **166 tests, all passing**
- Frontend total: **65 tests, all passing**
- Next: T42 (Core CRUD UI), T44 (Milestone detail version history)

## Key Patterns — Fiscal Year Entity
- `FiscalYear` has manual String ID (not @GeneratedValue) — `repository.save()` calls `merge()`, must use **returned** managed entity
- `FiscalPeriod` uses `@GeneratedValue(strategy = GenerationType.UUID)` (added in T38)
- `FiscalYear` has `@PrePersist` setting `createdAt` (added in T38)

## Key Patterns — Security
- `@PreAuthorize("hasRole('ADMIN')")` on methods throws `AccessDeniedException` which goes through `GlobalExceptionHandler`
- `GlobalExceptionHandler` now has explicit `AccessDeniedException` → 403 handler (added T38)
- `IllegalStateException` → 409 Conflict handler also added in T38

## Key Patterns

### Test Isolation (FK-safe delete order)
```
auditLogRepository.deleteAll();
reconciliationRepository.deleteAll();
journalLineRepository.deleteAll();
journalEntryRepository.deleteAll();
actualLineRepository.deleteAll();
sapImportRepository.deleteAll();
milestoneVersionRepository.deleteAll();
milestoneRepository.deleteAll();
projectRepository.deleteAll();
contractRepository.deleteAll();
```

### System Config Mutation
- `SystemConfigTest` modifies config table; must restore in `@AfterEach` via `resetConfig(key, value)`
- `DatabaseMigrationTest` asserts default config values → cross-test contamination if not restored

### Audit Log Changes Map Keys
- Uses `"before"` / `"after"` (NOT "old"/"new")
- ContractService, ProjectService, SystemConfigController all use "before"/"after"

### PATCH + PUT same endpoint
- Use `@RequestMapping(value="/{id}", method={RequestMethod.PUT, RequestMethod.PATCH})`
- Stacking `@PutMapping` + `@PatchMapping` on same method does NOT work in Spring

## Important File Locations
- Migrations: `src/main/resources/db/migration/V00X__*.sql`
- Entities: `src/main/java/com/ledger/entity/`
- Services: `src/main/java/com/ledger/service/`
- Controllers: `src/main/java/com/ledger/controller/`
- Tests: `src/test/java/com/ledger/`
