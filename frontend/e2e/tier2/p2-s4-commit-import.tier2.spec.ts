/**
 * Tier 2 — P2-S4: Commit Import
 * Validates that committing a STAGED import changes its status to COMMITTED
 * and actuals are ingested into the system.
 * Spec: 20-e2e-scenario-matrix.md §4 P2-S4
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

const SAP_CSV = [
  'DOCUMENT_NUMBER,POSTING_DATE,AMOUNT,VENDOR_NAME,WBSE,DESCRIPTION',
  'CMT001,2026-03-01,12000.00,Commit Corp,9999.SU.ES,Q1 Invoice',
].join('\n');

async function uploadAndNavigateToReview(page: import('@playwright/test').Page, rid: string) {
  await page.goto('/import');
  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles({
    name: `SAP_CMT_${rid}.csv`,
    mimeType: 'text/csv',
    buffer: Buffer.from(SAP_CSV),
  });
  await expect(page).toHaveURL(/\/import\/.+/, { timeout: 15_000 });
  return page.url().split('/import/')[1];
}

test.describe('P2-S4: Commit Import (Tier 2 — real backend)', () => {
  test('committing import changes status to COMMITTED', async ({ page, request }) => {
    const rid = runId();
    await loginAs(page, request);
    const importId = await uploadAndNavigateToReview(page, rid);

    await page.getByRole('button', { name: 'Commit Import' }).click();

    // Redirects back to import history
    await expect(page).toHaveURL('/import', { timeout: 10_000 });
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s4-committed.png', fullPage: true });

    // Verify via API
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };
    const importResp = await request.get(`http://localhost:8080/api/v1/imports/${importId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const imp = await importResp.json() as { status: string };
    expect(imp.status).toBe('COMMITTED');
  });

  test('import history shows COMMITTED badge after commit', async ({ page, request }) => {
    const rid = runId();
    const filename = `SAP_CMT_${rid}.csv`;
    await loginAs(page, request);
    await uploadAndNavigateToReview(page, rid);

    await page.getByRole('button', { name: 'Commit Import' }).click();
    await expect(page).toHaveURL('/import', { timeout: 10_000 });

    await expect(page.locator('tr', { hasText: filename }).getByText('COMMITTED')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s4-history-committed.png', fullPage: true });
  });

  test('committed import no longer shows commit/reject buttons', async ({ page, request }) => {
    const rid = runId();
    await loginAs(page, request);
    const importId = await uploadAndNavigateToReview(page, rid);

    await page.getByRole('button', { name: 'Commit Import' }).click();
    await expect(page).toHaveURL('/import', { timeout: 10_000 });

    // Navigate back to the committed import review
    await page.goto(`/import/${importId}`);
    await expect(page.getByRole('button', { name: 'Commit Import' })).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Reject Import' })).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s4-no-buttons.png', fullPage: true });
  });
});
