/**
 * Playwright API mocking — intercepts all /api/v1/* requests and returns
 * fixture data so E2E tests run without a live backend.
 */
import { Page, Route } from '@playwright/test';

// ─── Fixture data (mirrors src/test/fixtures.ts) ───────────────────────────

export const CONTRACT_ID = 'aaaaaaaa-0000-0000-0000-000000000001';
export const NEW_CONTRACT_ID = 'new-contract-id';
export const PROJECT_ID = 'PR13752';
export const MILESTONE_ID = 'bbbbbbbb-0000-0000-0000-000000000002';
export const ACTUAL_ID = 'cccccccc-0000-0000-0000-000000000003';
export const IMPORT_ID = 'dddddddd-0000-0000-0000-000000000004';

export const contractFixture = {
  contractId: CONTRACT_ID,
  name: 'Globant ADM',
  vendor: 'Globant',
  ownerUser: 'Rob',
  startDate: '2025-10-01',
  endDate: null,
  status: 'ACTIVE',
};

export const projectsFixture = [
  { projectId: PROJECT_ID, name: 'DPI Photopass', wbse: '1174905.SU.ES', fundingSource: 'OPEX', status: 'ACTIVE' },
];

export const projectFixture = {
  projectId: PROJECT_ID,
  name: 'DPI Photopass',
  wbse: '1174905.SU.ES',
  fundingSource: 'OPEX',
  status: 'ACTIVE',
  contract: { contractId: CONTRACT_ID, name: 'Globant ADM' },
};

export const milestonesFixture = [
  {
    milestoneId: MILESTONE_ID,
    name: 'January Sustainment',
    currentVersion: { versionNumber: 1, plannedAmount: 25000, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2025-11-01' },
  },
];

export const versionsFixture = [
  { versionId: 'v1', versionNumber: 1, plannedAmount: 25000, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2025-11-01', reason: 'Initial' },
];

export const reconciliationStatusFixture = {
  plannedAmount: 25000,
  reconciledAmount: 15000,
  invoiceTotal: 15000,
  accrualNet: 0,
  remaining: 10000,
  status: 'PARTIALLY_MATCHED',
};

export const varianceReportFixture = {
  fiscalYear: 'FY26',
  asOfDate: null,
  rows: [
    { contractId: CONTRACT_ID, contractName: 'Globant ADM', projectId: PROJECT_ID, projectName: 'DPI Photopass', totalPlanned: 25000, totalActual: 15000, totalVariance: 10000, totalVariancePercent: 40, totalStatus: 'UNDER_BUDGET' },
    { contractId: CONTRACT_ID, contractName: 'Globant ADM', projectId: 'PR99999', projectName: 'Other Project', totalPlanned: 30000, totalActual: 32000, totalVariance: -2000, totalVariancePercent: -6.67, totalStatus: 'OVER_BUDGET' },
  ],
};

export const openAccrualsFixture = {
  fiscalYear: 'FY26',
  rows: [
    { contractName: 'Globant ADM', projectName: 'DPI Photopass', milestoneName: 'January Sustainment', fiscalPeriod: 'FY26-04-JAN', openAccrualCount: 2, ageDays: 65, accrualStatus: 'WARNING' },
  ],
};

export const importsFixture = [
  { importId: IMPORT_ID, filename: 'SAP_FY26_Jan.csv', importedAt: '2026-01-15T10:00:00Z', status: 'COMMITTED', totalLines: 150, newLines: 45, duplicateLines: 105, errorLines: 0 },
  { importId: 'staged-id', filename: 'SAP_FY26_Feb.csv', importedAt: '2026-02-01T10:00:00Z', status: 'STAGED', totalLines: 38, newLines: 38, duplicateLines: 0, errorLines: 0 },
];

export const importDetailFixture = {
  importId: IMPORT_ID, filename: 'SAP_FY26_Jan.csv', importedAt: '2026-01-15T10:00:00Z',
  status: 'STAGED', totalLines: 3, newLines: 2, duplicateLines: 1, errorLines: 0,
};

export const importLinesFixture = [
  { actualId: 'a1', sapDocumentNumber: 'DOC1', postingDate: '2026-01-15', amount: 25000, vendorName: 'Globant', wbse: '1174905.SU.ES', description: 'Invoice', duplicate: false },
  { actualId: 'a2', sapDocumentNumber: 'DOC2', postingDate: '2026-01-15', amount: 10000, vendorName: 'Globant', wbse: '1174905.SU.ES', description: 'Accrual', duplicate: false },
  { actualId: 'a3', sapDocumentNumber: 'DOC3', postingDate: '2025-12-31', amount: 5000, vendorName: 'Globant', wbse: '1174905.SU.ES', description: 'Old line', duplicate: true },
];

export const unreconciledFixture = [
  { actualId: ACTUAL_ID, sapDocumentNumber: 'DOC1', postingDate: '2026-03-15', amount: 25000, vendorName: 'Globant S.A.', wbse: '1174905.SU.ES', description: 'Invoice March' },
  { actualId: 'act2', sapDocumentNumber: 'DOC2', postingDate: '2026-03-15', amount: -18000, vendorName: 'Globant S.A.', wbse: '1174905.SU.ES', description: 'Reversal' },
];

export const candidatesFixture = [
  { milestoneId: MILESTONE_ID, milestoneName: 'January Sustainment', projectId: PROJECT_ID, plannedAmount: 25000, relevanceScore: 10 },
  { milestoneId: 'ms-other', milestoneName: 'Other Milestone', projectId: 'PR99999', plannedAmount: 30000, relevanceScore: 0 },
];

export const configFixture = [
  { configKey: 'tolerance_percent', configValue: '0.02', description: 'Tolerance (%)', dataType: 'PERCENTAGE', displayGroup: 'RECONCILIATION TOLERANCE', displayName: 'Tolerance (%)', displayOrder: 1 },
  { configKey: 'tolerance_absolute', configValue: '50.00', description: 'Tolerance ($)', dataType: 'DECIMAL', displayGroup: 'RECONCILIATION TOLERANCE', displayName: 'Tolerance ($)', displayOrder: 2 },
  { configKey: 'accrual_aging_warning_days', configValue: '60', description: 'Warning threshold', dataType: 'INTEGER', displayGroup: 'ACCRUAL AGING', displayName: 'Warning Threshold (days)', displayOrder: 1 },
  { configKey: 'accrual_aging_critical_days', configValue: '90', description: 'Critical threshold', dataType: 'INTEGER', displayGroup: 'ACCRUAL AGING', displayName: 'Critical Threshold (days)', displayOrder: 2 },
];

export const USER_ID = 'aaaaaaaa-1111-0000-0000-000000000001';
export const ANALYST_ID = 'aaaaaaaa-2222-0000-0000-000000000002';

export const usersFixture = [
  { userId: USER_ID, username: 'admin', displayName: 'System Admin', email: 'admin@ledger.local', role: 'ADMIN', active: true },
  { userId: ANALYST_ID, username: 'alice', displayName: 'Alice', email: 'alice@ledger.local', role: 'ANALYST', active: true },
];

export const fiscalYearsFixture = [
  { fiscalYear: 'FY25', startDate: '2024-10-01', endDate: '2025-09-30' },
  { fiscalYear: 'FY26', startDate: '2025-10-01', endDate: '2026-09-30' },
  { fiscalYear: 'FY27', startDate: '2026-10-01', endDate: '2027-09-30' },
];

export const referenceDataFixture: Record<string, unknown[]> = {
  FUNDING_SOURCE: [
    { code: 'OPEX', displayName: 'Operating Expenditure', description: null, active: true, sortOrder: 1 },
    { code: 'CAPEX', displayName: 'Capital Expenditure', description: null, active: false, sortOrder: 2 },
  ],
  CONTRACT_STATUS: [
    { code: 'ACTIVE', displayName: 'Active', description: null, active: true, sortOrder: 1 },
    { code: 'INACTIVE', displayName: 'Inactive', description: null, active: true, sortOrder: 2 },
  ],
  PROJECT_STATUS: [
    { code: 'ACTIVE', displayName: 'Active', description: null, active: true, sortOrder: 1 },
    { code: 'CLOSED', displayName: 'Closed', description: null, active: true, sortOrder: 2 },
  ],
  RECONCILIATION_CATEGORY: [
    { code: 'INVOICE', displayName: 'Invoice', description: null, active: true, sortOrder: 1, affectsAccrualLifecycle: false },
    { code: 'ACCRUAL', displayName: 'Accrual', description: null, active: true, sortOrder: 2, affectsAccrualLifecycle: true },
  ],
};

export const auditLogFixture = [
  { auditId: 'a1', entityType: 'CONTRACT', entityId: CONTRACT_ID, action: 'CREATE', createdBy: 'admin', reason: null, createdAt: '2026-01-15T10:00:00Z', changes: null },
  { auditId: 'a2', entityType: 'CONTRACT', entityId: CONTRACT_ID, action: 'UPDATE', createdBy: 'alice', reason: 'Status change', createdAt: '2026-02-01T10:00:00Z', changes: { status: { before: 'ACTIVE', after: 'INACTIVE' } } },
];

export const budgetReportFixture = {
  fiscalYear: 'FY26',
  asOfDate: null,
  rows: [
    { contractId: CONTRACT_ID, contractName: 'Globant ADM', projectId: PROJECT_ID, projectName: 'DPI Photopass', fundingSource: 'OPEX', periods: { 'FY26-04-JAN': 25000, 'FY26-05-FEB': 20000 }, total: 45000 },
  ],
  grandTotal: 45000,
};

export const reconciliationReportFixture = {
  fiscalYear: 'FY26',
  asOfDate: null,
  rows: [
    { contractName: 'Globant ADM', projectName: 'DPI Photopass', milestoneName: 'January Sustainment', fiscalPeriod: 'FY26-04-JAN', planned: 25000, invoiceTotal: 15000, accrualNet: 0, totalActual: 15000, remaining: 10000, status: 'PARTIALLY_MATCHED', openAccrualCount: 0 },
    { contractName: 'Globant ADM', projectName: 'DPI Photopass', milestoneName: 'Feb Sustainment', fiscalPeriod: 'FY26-05-FEB', planned: 20000, invoiceTotal: 20000, accrualNet: 0, totalActual: 20000, remaining: 0, status: 'FULLY_RECONCILED', openAccrualCount: 0 },
  ],
};

export const journalFixture = [
  { entryId: 'e1', entryDate: '2026-01-15T00:00:00Z', effectiveDate: '2026-01-15', entryType: 'PLAN_CREATE', description: 'Initial budget', createdBy: 'system' },
  { entryId: 'e2', entryDate: '2026-02-01T00:00:00Z', effectiveDate: '2026-02-01', entryType: 'ACTUAL_IMPORT', description: 'SAP import: 38 lines', createdBy: 'system' },
  { entryId: 'e3', entryDate: '2026-02-15T00:00:00Z', effectiveDate: '2026-02-15', entryType: 'PLAN_ADJUST', description: 'Scope cut -$5,000', createdBy: 'system' },
];

export const journalLinesFixture = [
  { lineId: 'l1', account: 'PLANNED', projectId: PROJECT_ID, milestoneId: MILESTONE_ID, fiscalPeriodId: 'fp1', debit: 0, credit: 25000 },
  { lineId: 'l2', account: 'VARIANCE_RESERVE', projectId: PROJECT_ID, milestoneId: MILESTONE_ID, fiscalPeriodId: 'fp1', debit: 25000, credit: 0 },
];

export const fiscalPeriodsFixture = [
  { periodId: 'fp1', periodKey: 'FY26-04-JAN' },
  { periodId: 'fp2', periodKey: 'FY26-05-FEB' },
];

// ─── Route registration ──────────────────────────────────────────────────────

function json(route: Route, body: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

/**
 * Register all API mock routes on the given Playwright page.
 * Also injects an auth token into localStorage so the app's auth guard passes.
 * Call this in beforeEach or at the start of each test.
 *
 * Pass `{ injectAuth: false }` to skip token injection — use this for login flow tests
 * where the app must start unauthenticated.
 */
export async function mockApis(page: Page, { injectAuth = true }: { injectAuth?: boolean } = {}) {
  if (injectAuth) {
    // Inject a fake JWT into localStorage before page load so auth guard passes.
    // The AuthContext reads ledger_token on init and decodes role/displayName.
    await page.addInitScript(() => {
      const payload = btoa(JSON.stringify({ role: 'ADMIN', displayName: 'Test Admin', sub: 'admin' }));
      localStorage.setItem('ledger_token', `fake.${payload}.sig`);
    });
  }

  // Mock auth endpoints
  await page.route('**/api/v1/auth/login', r => {
    const payload = btoa(JSON.stringify({ role: 'ADMIN', displayName: 'Test Admin', sub: 'admin' }));
    return json(r, { token: `fake.${payload}.sig`, role: 'ADMIN', displayName: 'Test Admin' });
  });
  await page.route('**/api/v1/reports/variance**', r => json(r, varianceReportFixture));
  await page.route('**/api/v1/reports/open-accruals**', r => json(r, openAccrualsFixture));
  await page.route('**/api/v1/reports/budget**', r => json(r, budgetReportFixture));
  await page.route('**/api/v1/reports/reconciliation-status**', r => json(r, reconciliationReportFixture));

  await page.route('**/api/v1/imports/*/lines', r => json(r, importLinesFixture));
  await page.route('**/api/v1/imports/*/commit', r => json(r, {}, 200));
  await page.route('**/api/v1/imports/*/reject', r => json(r, {}, 200));
  await page.route(`**/api/v1/imports/${IMPORT_ID}`, r => json(r, importDetailFixture));
  await page.route('**/api/v1/imports/staged-id', r => json(r, { ...importDetailFixture, importId: 'staged-id', filename: 'SAP_FY26_Feb.csv', status: 'STAGED' }));
  await page.route('**/api/v1/imports', r => json(r, importsFixture));

  // Reconciliation routes: register wildcard first (lowest priority — LIFO in Playwright)
  // then specific routes last so they match before the wildcard catch-all.
  await page.route('**/api/v1/reconciliation/**', r => json(r, {}));
  await page.route('**/api/v1/reconciliation/unreconciled', r => json(r, unreconciledFixture));
  await page.route('**/api/v1/reconciliation/candidates/**', r => json(r, candidatesFixture));
  await page.route('**/api/v1/reconciliation', r => {
    if (r.request().method() === 'POST') {
      return json(r, {
        reconciliationId: 'rec-001',
        actualId: ACTUAL_ID,
        milestoneId: MILESTONE_ID,
        category: 'INVOICE',
        matchNotes: null,
        reconciledAt: '2026-03-15T00:00:00Z',
        reconciledBy: 'system',
      }, 201);
    }
    return json(r, []);
  });

  await page.route('**/api/v1/journal/*/lines', r => json(r, journalLinesFixture));
  await page.route('**/api/v1/journal', r => json(r, journalFixture));

  await page.route('**/api/v1/config/**', r => {
    if (r.request().method() === 'PUT') return json(r, {});
    return json(r, configFixture);
  });
  await page.route('**/api/v1/config', r => json(r, configFixture));

  // Reconciliation status (must be before generic reconciliation route)
  await page.route('**/api/v1/reconciliation/status/**', r => json(r, reconciliationStatusFixture));

  // Contracts — ordered specific-before-generic (Playwright: last-registered = first-tried)
  await page.route('**/api/v1/contracts', r => {
    if (r.request().method() === 'POST') {
      return json(r, { ...contractFixture, contractId: NEW_CONTRACT_ID, name: 'New Contract', vendor: 'New Vendor' }, 201);
    }
    return json(r, [contractFixture]);
  });
  await page.route('**/api/v1/contracts/*/projects', r => {
    if (r.request().method() === 'POST') return json(r, {}, 201);
    return json(r, projectsFixture);
  });
  await page.route('**/api/v1/contracts/*', r => {
    if (r.request().method() === 'PUT') return json(r, contractFixture);
    return json(r, contractFixture);
  });

  // Projects
  await page.route('**/api/v1/projects/*/milestones/*/reconciliation-status', r => json(r, reconciliationStatusFixture));
  await page.route('**/api/v1/projects/*/milestones', r => {
    if (r.request().method() === 'POST') return json(r, {}, 201);
    return json(r, milestonesFixture);
  });
  await page.route(`**/api/v1/projects/${PROJECT_ID}`, r => json(r, projectFixture));
  await page.route('**/api/v1/projects/*', r => json(r, projectFixture));

  // Milestones
  await page.route('**/api/v1/milestones/*/cancel', r => json(r, {}));
  await page.route('**/api/v1/milestones/*/versions', r => {
    if (r.request().method() === 'POST') return json(r, {}, 201);
    return json(r, versionsFixture);
  });

  // Fiscal periods for milestone creation form
  await page.route('**/api/v1/fiscal-years/*/periods', r => json(r, fiscalPeriodsFixture));

  // Admin: fiscal years
  await page.route('**/api/v1/fiscal-years', r => {
    if (r.request().method() === 'POST') return json(r, { fiscalYear: 'FY28', startDate: '2027-10-01', endDate: '2028-09-30' }, 201);
    return json(r, fiscalYearsFixture);
  });

  // Admin: users
  await page.route('**/api/v1/admin/users/*/deactivate', r => json(r, {}));
  await page.route('**/api/v1/admin/users/*/reactivate', r => json(r, {}));
  await page.route('**/api/v1/admin/users', r => {
    if (r.request().method() === 'POST') return json(r, {}, 201);
    return json(r, usersFixture);
  });

  // Admin: reference data
  await page.route('**/api/v1/admin/reference-data/*/toggle-active', r => json(r, {}));
  await page.route('**/api/v1/admin/reference-data/*', r => {
    if (r.request().method() === 'POST') return json(r, {}, 201);
    const url = r.request().url();
    const type = url.split('/admin/reference-data/')[1]?.split('?')[0] ?? 'FUNDING_SOURCE';
    return json(r, referenceDataFixture[type] ?? []);
  });

  // Admin: audit log
  await page.route('**/api/v1/audit/export.csv', r => r.fulfill({ status: 200, contentType: 'text/csv', body: 'auditId,entityType\na1,CONTRACT' }));
  await page.route('**/api/v1/audit**', r => json(r, auditLogFixture));
}
