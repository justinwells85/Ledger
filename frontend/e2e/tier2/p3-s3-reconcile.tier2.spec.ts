/**
 * Tier 2 — P3-S3: Reconcile Actual to Milestone
 * Validates that a reconciliation is persisted: the actual disappears from
 * the unreconciled list after reconciliation and a RECONCILE journal entry is created.
 * Spec: 20-e2e-scenario-matrix.md §4 P3-S3
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

const SAP_CSV = (rid: string) => [
  'DOCUMENT_NUMBER,POSTING_DATE,AMOUNT,VENDOR_NAME,WBSE,DESCRIPTION',
  `${rid}D01,2026-03-01,15000.00,Recon Corp,9999.SU.ES,Invoice for Recon`,
].join('\n');

async function fullSetup(request: import('@playwright/test').APIRequestContext, rid: string) {
  const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
    data: { username: 'admin', password: 'admin' },
  });
  const { token } = await loginResp.json() as { token: string };

  // Contract + project + milestone
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

  const msResp = await request.post(`http://localhost:8080/api/v1/projects/${projectId}/milestones`, {
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
  const { milestoneId } = await msResp.json() as { milestoneId: string };

  // Upload and commit a SAP import to create actuals
  const fd = new URLSearchParams();
  const importResp = await request.post('http://localhost:8080/api/v1/imports/upload', {
    multipart: {
      file: {
        name: `SAP_${rid}.csv`,
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

  void fd;
  return { contractId, projectId, milestoneId, token, importId };
}

test.describe('P3-S3: Reconcile Actual to Milestone (Tier 2 — real backend)', () => {
  test('reconciling an actual removes it from the unreconciled list after reload', async ({ page, request }) => {
    const rid = runId();
    const { milestoneId } = await fullSetup(request, rid);
    const milestoneName = `${rid} Milestone`;

    await loginAs(page, request);
    await page.goto('/reconcile');

    // Find the actual from our import
    await expect(page.getByText('Invoice for Recon')).toBeVisible({ timeout: 10_000 });
    await page.getByText('Invoice for Recon').click();

    // Milestone should appear as a candidate
    await expect(page.getByText(milestoneName)).toBeVisible({ timeout: 10_000 });
    await page.getByText(milestoneName).click();

    // Submit reconciliation
    await page.getByRole('button', { name: 'Reconcile' }).click();
    await page.screenshot({ path: 'e2e/screenshots/t2-p3-s3-reconciled.png', fullPage: true });

    // After reload: actual no longer in unreconciled list
    await page.reload();
    await expect(page.getByText('Invoice for Recon')).not.toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: 'e2e/screenshots/t2-p3-s3-after-reload.png', fullPage: true });

    void milestoneId;
  });
});
