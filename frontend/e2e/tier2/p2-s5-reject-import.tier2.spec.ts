/**
 * Tier 2 — P2-S5: Reject Import
 * Validates that rejecting a STAGED import changes its status to REJECTED.
 * Spec: 20-e2e-scenario-matrix.md §4 P2-S5
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

const SAP_CSV = [
  'DOCUMENT_NUMBER,POSTING_DATE,AMOUNT,VENDOR_NAME,WBSE,DESCRIPTION',
  'REJ001,2026-03-01,9000.00,Reject Corp,9999.SU.ES,Erroneous entry',
].join('\n');

async function uploadAndNavigateToReview(page: import('@playwright/test').Page, rid: string) {
  await page.goto('/import');
  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles({
    name: `SAP_REJ_${rid}.csv`,
    mimeType: 'text/csv',
    buffer: Buffer.from(SAP_CSV),
  });
  await expect(page).toHaveURL(/\/import\/.+/, { timeout: 15_000 });
  return page.url().split('/import/')[1];
}

test.describe('P2-S5: Reject Import (Tier 2 — real backend)', () => {
  test('rejecting import changes status to REJECTED', async ({ page, request }) => {
    const rid = runId();
    await loginAs(page, request);
    const importId = await uploadAndNavigateToReview(page, rid);

    page.on('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Reject Import' }).click();

    await expect(page).toHaveURL('/import', { timeout: 10_000 });
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s5-rejected.png', fullPage: true });

    // Verify via API
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };
    const importResp = await request.get(`http://localhost:8080/api/v1/imports/${importId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const imp = await importResp.json() as { status: string };
    expect(imp.status).toBe('REJECTED');
  });

  test('import history shows REJECTED badge after rejection', async ({ page, request }) => {
    const rid = runId();
    const filename = `SAP_REJ_${rid}.csv`;
    await loginAs(page, request);
    await uploadAndNavigateToReview(page, rid);

    page.on('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Reject Import' }).click();
    await expect(page).toHaveURL('/import', { timeout: 10_000 });

    await expect(page.locator('tr', { hasText: filename }).getByText('REJECTED')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s5-history-rejected.png', fullPage: true });
  });

  test('canceling reject dialog keeps import STAGED', async ({ page, request }) => {
    const rid = runId();
    await loginAs(page, request);
    const importId = await uploadAndNavigateToReview(page, rid);

    page.on('dialog', dialog => dialog.dismiss());
    await page.getByRole('button', { name: 'Reject Import' }).click();

    // Still on review page
    await expect(page.getByRole('button', { name: 'Reject Import' })).toBeVisible();

    // Verify still STAGED via API
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };
    const importResp = await request.get(`http://localhost:8080/api/v1/imports/${importId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const imp = await importResp.json() as { status: string };
    expect(imp.status).toBe('STAGED');
  });
});
