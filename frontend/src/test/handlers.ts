import { http, HttpResponse } from 'msw';
import {
  varianceReportFixture,
  openAccrualsFixture,
  budgetReportFixture,
  reconciliationReportFixture,
  importsFixture,
  importDetailFixture,
  importLinesFixture,
  unreconciledFixture,
  candidatesFixture,
  journalFixture,
  journalLinesFixture,
  configFixture,
  versionsFixture,
  reconciliationStatusFixture,
  fiscalPeriodsFixture,
  usersFixture,
} from './fixtures';

export const handlers = [
  http.get('/api/v1/reports/variance', () => HttpResponse.json(varianceReportFixture)),
  http.get('/api/v1/reports/open-accruals', () => HttpResponse.json(openAccrualsFixture)),
  http.get('/api/v1/reports/budget', () => HttpResponse.json(budgetReportFixture)),
  http.get('/api/v1/reports/reconciliation-status', () => HttpResponse.json(reconciliationReportFixture)),

  http.get('/api/v1/imports', () => HttpResponse.json(importsFixture)),
  http.get('/api/v1/imports/:importId', () => HttpResponse.json(importDetailFixture)),
  http.get('/api/v1/imports/:importId/lines', () => HttpResponse.json(importLinesFixture)),
  http.post('/api/v1/imports/:importId/commit', () => HttpResponse.json({}, { status: 200 })),
  http.post('/api/v1/imports/:importId/reject', () => HttpResponse.json({}, { status: 200 })),

  http.get('/api/v1/reconciliation/unreconciled', () => HttpResponse.json(unreconciledFixture)),
  http.get('/api/v1/reconciliation/candidates/:actualId', () => HttpResponse.json(candidatesFixture)),
  http.post('/api/v1/reconciliation', () => HttpResponse.json({
    reconciliationId: 'rec-001',
    actualId: 'cccccccc-0000-0000-0000-000000000003',
    milestoneId: 'bbbbbbbb-0000-0000-0000-000000000002',
    category: 'INVOICE',
    matchNotes: null,
    reconciledAt: '2026-03-15T00:00:00Z',
    reconciledBy: 'system',
  }, { status: 201 })),
  http.delete('/api/v1/reconciliation/:id', () => HttpResponse.json({})),

  http.get('/api/v1/journal', () => HttpResponse.json(journalFixture)),
  http.get('/api/v1/journal/:entryId/lines', () => HttpResponse.json(journalLinesFixture)),

  http.get('/api/v1/config', () => HttpResponse.json(configFixture)),
  http.put('/api/v1/config/:key', () => HttpResponse.json({}, { status: 200 })),

  http.get('/api/v1/milestones/:milestoneId/versions', () => HttpResponse.json(versionsFixture)),
  http.get('/api/v1/reconciliation/status/:milestoneId', () => HttpResponse.json(reconciliationStatusFixture)),
  http.get('/api/v1/fiscal-years/:fiscalYear/periods', () => HttpResponse.json(fiscalPeriodsFixture)),

  http.get('/api/v1/contracts', () => HttpResponse.json([
    { contractId: 'aaaaaaaa-0000-0000-0000-000000000001', name: 'Globant ADM', vendor: 'Globant', status: 'ACTIVE' },
  ])),
  http.post('/api/v1/contracts', () => HttpResponse.json({ contractId: 'new-contract-id' }, { status: 201 })),

  http.get('/api/v1/fiscal-years', () => HttpResponse.json([
    { fiscalYear: 'FY26', startDate: '2025-10-01', endDate: '2026-09-30' },
  ])),
  http.post('/api/v1/fiscal-years', async ({ request }) => {
    const body = await request.json() as { label?: string };
    const label = body.label ?? 'FY28';
    const year = parseInt(label.replace('FY', '')) + 2000;
    return HttpResponse.json(
      { fiscalYear: label, startDate: `${year - 1}-10-01`, endDate: `${year}-09-30` },
      { status: 201 }
    );
  }),

  http.post('/api/v1/milestones/:milestoneId/cancel', () => HttpResponse.json({}, { status: 200 })),

  http.get('/api/v1/admin/reference-data/:type', ({ params }) => {
    const type = params.type as string;
    if (type === 'FUNDING_SOURCE') return HttpResponse.json([
      { code: 'OPEX', displayName: 'Operating Expense', description: '', active: true, sortOrder: 1 },
      { code: 'CAPEX', displayName: 'Capital Expense', description: '', active: true, sortOrder: 2 },
    ]);
    if (type === 'CONTRACT_STATUS') return HttpResponse.json([
      { code: 'ACTIVE', displayName: 'Active', description: '', active: true, sortOrder: 1 },
      { code: 'CLOSED', displayName: 'Closed', description: '', active: true, sortOrder: 2 },
    ]);
    if (type === 'PROJECT_STATUS') return HttpResponse.json([
      { code: 'ACTIVE', displayName: 'Active', description: '', active: true, sortOrder: 1 },
      { code: 'CLOSED', displayName: 'Closed', description: '', active: true, sortOrder: 2 },
    ]);
    return HttpResponse.json([
      { code: 'INVOICE', displayName: 'Invoice', description: '', active: true, sortOrder: 1, affectsAccrualLifecycle: false },
    ]);
  }),
  http.post('/api/v1/admin/reference-data/:type', async ({ request }) => {
    const body = await request.json() as { code?: string; displayName?: string };
    return HttpResponse.json({ code: body.code, displayName: body.displayName, active: true, sortOrder: 0 }, { status: 201 });
  }),
  http.post('/api/v1/admin/reference-data/:type/:code/toggle-active', ({ params }) => {
    return HttpResponse.json({ code: params.code, displayName: String(params.code), active: false, sortOrder: 0 });
  }),

  http.get('/api/v1/audit', () => HttpResponse.json([
    { auditId: 'a1', entityType: 'CONTRACT', entityId: 'c-001', action: 'CREATE', createdBy: 'alice', reason: 'Contract created', createdAt: '2026-01-15T10:00:00Z', changes: null },
    { auditId: 'a2', entityType: 'PROJECT', entityId: 'PR13752', action: 'UPDATE', createdBy: 'bob', reason: 'Project closed', createdAt: '2026-02-01T10:00:00Z', changes: { status: { before: 'ACTIVE', after: 'CLOSED' } } },
  ])),

  http.get('/api/v1/admin/users', () => HttpResponse.json(usersFixture)),
  http.post('/api/v1/admin/users', () => HttpResponse.json(
    { userId: 'new-user-id', username: 'newuser', displayName: 'New User', email: 'new@ledger.local', role: 'ANALYST', active: true },
    { status: 201 }
  )),
  http.post('/api/v1/admin/users/:userId/deactivate', () => HttpResponse.json(
    { ...usersFixture[1], active: false }
  )),
  http.post('/api/v1/admin/users/:userId/reactivate', () => HttpResponse.json(
    { ...usersFixture[1], active: true }
  )),
  http.patch('/api/v1/admin/users/:userId/role', () => HttpResponse.json(usersFixture[0])),
];
