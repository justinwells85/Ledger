/**
 * Tier 2 — P3-S4: Undo Reconciliation
 * Validates that undoing a reconciliation restores the actual to the unreconciled list.
 * Spec: 20-e2e-scenario-matrix.md §4 P3-S4
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

const SAP_CSV = (rid: string) => [
  'DOCUMENT_NUMBER,POSTING_DATE,AMOUNT,VENDOR_NAME,WBSE,DESCRIPTION',
  `${rid}U01,2026-03-01,8000.00,Undo Corp,9999.SU.ES,Invoice for Undo Test`,
].join('\n');

async function fullSetup(request: import('@playwright/test').APIRequestContext, rid: string) {
  const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
    data: { username: 'admin', password: 'admin' },
  });
  const { token } = await loginResp.json() as { token: string };

  const contractResp = await request.post('http://localhost:8080/api/v1/contracts', {
    data: { name: `${rid} Contract`, vendor: `${rid} Vendor`, ownerUser: 'PM', startDate: '2026-01-01' },
    headers: { Authorization: `Bearer ${token}` },
  });
  const { contractId } = await contractResp.json() as { contractId: string };

  const projectId = `PR${rid.replace(/\D/g, '').slice(-6)}`;
  await request.post(`http://localhost:8080/api/v1/contracts/${contractId}/projects`, {
    data: { projectId, name: `${rid} Project`, wbse: '9999.SU.ES', fundingSource: 'OPEX' },
    headers: { Authorization: `Bearer ${token}` },
  });

  const periodsResp = await request.get('http://localhost:8080/api/v1/fiscal-years/FY26/periods', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const periods = await periodsResp.json() as { periodId: string }[];
  const period = periods[3];

  await request.post(`http://localhost:8080/api/v1/projects/${projectId}/milestones`, {
    data: {
      name: `${rid} Milestone`,
      plannedAmount: 20000,
      fiscalPeriodId: period.periodId,
      effectiveDate: '2026-01-01',
      reason: 'Initial',
      createdBy: 'admin',
    },
    headers: { Authorization: `Bearer ${token}` },
  });

  const importResp = await request.post('http://localhost:8080/api/v1/imports/upload', {
    multipart: {
      file: {
        name: `SAP_UNDO_${rid}.csv`,
        mimeType: 'text/csv',
        buffer: Buffer.from(SAP_CSV(rid)),
      },
    },
    headers: { Authorization: `Bearer ${token}` },
  });
  const { importId } = await importResp.json() as { importId: string };
  await request.post(`http://localhost:8080/api/v1/imports/${importId}/commit`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  return { contractId, projectId, token };
}

test.describe('P3-S4: Undo Reconciliation (Tier 2 — real backend)', () => {
  test('undoing reconciliation restores actual to unreconciled list after reload', async ({ page, request }) => {
    const rid = runId();
    const milestoneName = `${rid} Milestone`;
    await fullSetup(request, rid);

    await loginAs(page, request);
    await page.goto('/reconcile');

    // Reconcile first
    await expect(page.getByText('Invoice for Undo Test')).toBeVisible({ timeout: 10_000 });
    await page.getByText('Invoice for Undo Test').click();
    await expect(page.getByText(milestoneName)).toBeVisible({ timeout: 10_000 });
    await page.getByText(milestoneName).click();
    await page.getByRole('button', { name: 'Reconcile' }).click();

    // Undo
    await expect(page.getByText('MATCHED THIS SESSION')).toBeVisible();
    await page.getByRole('button', { name: 'Undo' }).click();
    await page.getByPlaceholder('Reason for undo (required)').fill('Wrong milestone selected');
    await page.getByRole('button', { name: 'Confirm Undo' }).click();
    await page.screenshot({ path: 'e2e/screenshots/t2-p3-s4-undo-confirmed.png', fullPage: true });

    // After reload: actual is back in unreconciled list
    await page.reload();
    await expect(page.getByText('Invoice for Undo Test')).toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: 'e2e/screenshots/t2-p3-s4-after-reload.png', fullPage: true });
  });
});
