/**
 * Tier 2 — P2-S1: Upload SAP CSV
 * Validates that a CSV file upload creates a STAGED import persisted in the backend.
 * Spec: 20-e2e-scenario-matrix.md §4 P2-S1
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

const SAP_CSV_CONTENT = [
  'DOCUMENT_NUMBER,POSTING_DATE,AMOUNT,VENDOR_NAME,WBSE,DESCRIPTION',
  'TDOC001,2026-03-01,5000.00,Test Vendor,9999.SU.ES,Invoice Q1',
  'TDOC002,2026-03-15,3000.00,Test Vendor,9999.SU.ES,Accrual Q1',
].join('\n');

test.describe('P2-S1: Upload SAP CSV (Tier 2 — real backend)', () => {
  test('uploading CSV creates a STAGED import visible in history', async ({ page, request }) => {
    const rid = runId();
    const filename = `SAP_T2_${rid}.csv`;

    await loginAs(page, request);
    await page.goto('/import');

    // Upload via hidden file input
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles({
      name: filename,
      mimeType: 'text/csv',
      buffer: Buffer.from(SAP_CSV_CONTENT),
    });

    // After upload, navigates to import review page
    await expect(page).toHaveURL(/\/import\/.+/, { timeout: 15_000 });
    await expect(page.getByText(filename)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s1-upload-review.png', fullPage: true });

    // Extract import ID from URL
    const url = page.url();
    const importId = url.split('/import/')[1];

    // Verify via API that the import exists with STAGED status
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };
    const importResp = await request.get(`http://localhost:8080/api/v1/imports/${importId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const imp = await importResp.json() as { status: string; filename: string };
    expect(imp.status).toBe('STAGED');
    expect(imp.filename).toBe(filename);
  });

  test('upload appears in GET /imports list with STAGED status', async ({ page, request }) => {
    const rid = runId();
    const filename = `SAP_T2_LIST_${rid}.csv`;

    await loginAs(page, request);
    await page.goto('/import');

    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles({
      name: filename,
      mimeType: 'text/csv',
      buffer: Buffer.from(SAP_CSV_CONTENT),
    });
    await expect(page).toHaveURL(/\/import\/.+/, { timeout: 15_000 });

    // Navigate back to history and confirm the new entry appears
    await page.goto('/import');
    await expect(page.getByText(filename)).toBeVisible();
    await expect(page.locator('tr', { hasText: filename }).getByText('STAGED')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s1-history-staged.png', fullPage: true });
  });
});
