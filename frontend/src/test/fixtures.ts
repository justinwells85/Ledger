/**
 * Shared test fixtures matching backend API response shapes.
 * Spec: 13-api-design.md
 */

export const CONTRACT_ID = 'aaaaaaaa-0000-0000-0000-000000000001';
export const PROJECT_ID = 'PR13752';
export const MILESTONE_ID = 'bbbbbbbb-0000-0000-0000-000000000002';
export const ACTUAL_ID = 'cccccccc-0000-0000-0000-000000000003';
export const IMPORT_ID = 'dddddddd-0000-0000-0000-000000000004';
export const NEW_IMPORT_ID = 'eeeeeeee-0000-0000-0000-000000000005';

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
  { versionId: 'v1', versionNumber: 1, plannedAmount: 25000, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2025-11-01', reason: 'Initial', createdBy: 'system' },
];

export const versionsWithDeltaFixture = [
  { versionId: 'v1', versionNumber: 1, plannedAmount: 25000, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2025-11-01', reason: 'Budget allocation', createdBy: 'system' },
  { versionId: 'v2', versionNumber: 2, plannedAmount: 30000, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2025-12-01', reason: 'Scope increase', createdBy: 'alice' },
  { versionId: 'v3', versionNumber: 3, plannedAmount: 0, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2026-01-01', reason: 'Cancelled per PM', createdBy: 'bob' },
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

export const openAccrualsEmptyFixture = { fiscalYear: 'FY26', rows: [] };

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

export const USER_ID = 'aaaaaaaa-1111-0000-0000-000000000001';
export const ANALYST_ID = 'aaaaaaaa-2222-0000-0000-000000000002';

export const usersFixture = [
  { userId: USER_ID, username: 'admin', displayName: 'System Admin', email: 'admin@ledger.local', role: 'ADMIN', active: true },
  { userId: ANALYST_ID, username: 'alice', displayName: 'Alice', email: 'alice@ledger.local', role: 'ANALYST', active: true },
];

export const fiscalPeriodsFixture = [
  { periodId: 'fp1', periodKey: 'FY26-04-JAN' },
  { periodId: 'fp2', periodKey: 'FY26-05-FEB' },
];
