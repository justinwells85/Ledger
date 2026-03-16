/**
 * Tier 2 — P2-S2 / P2-S3: Review Import Summary and Inspect Lines
 * Validates that the import review page shows real counts and actual line data.
 * Spec: 20-e2e-scenario-matrix.md §4 P2-S2, P2-S3
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

const SAP_CSV = [
  'DOCUMENT_NUMBER,POSTING_DATE,AMOUNT,VENDOR_NAME,WBSE,DESCRIPTION',
  'RVW001,2026-03-01,8000.00,Acme Corp,9999.SU.ES,Invoice March',
  'RVW002,2026-03-15,-2000.00,Acme Corp,9999.SU.ES,Reversal',
  'RVW001,2026-03-01,8000.00,Acme Corp,9999.SU.ES,Invoice March',  // duplicate
].join('\n');

async function uploadCsv(page: import('@playwright/test').Page, rid: string) {
  await page.goto('/import');
  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles({
    name: `SAP_RVW_${rid}.csv`,
    mimeType: 'text/csv',
    buffer: Buffer.from(SAP_CSV),
  });
  await expect(page).toHaveURL(/\/import\/.+/, { timeout: 15_000 });
  return page.url().split('/import/')[1];
}

test.describe('P2-S2: Review Import Summary (Tier 2 — real backend)', () => {
  test('import review shows correct total line count', async ({ page, request }) => {
    const rid = runId();
    await loginAs(page, request);
    await uploadCsv(page, rid);

    // Summary card shows total lines = 3
    await expect(page.getByText('3', { exact: true })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s2-summary.png', fullPage: true });
  });

  test('import review shows new vs duplicate line counts', async ({ page, request }) => {
    const rid = runId();
    await loginAs(page, request);
    await uploadCsv(page, rid);

    // 2 new + 1 duplicate
    await expect(page.getByText('2', { exact: true })).toBeVisible();
    await expect(page.getByText('1', { exact: true })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s2-counts.png', fullPage: true });
  });
});

test.describe('P2-S3: Inspect Import Lines (Tier 2 — real backend)', () => {
  test('import lines table shows actual uploaded data', async ({ page, request }) => {
    const rid = runId();
    await loginAs(page, request);
    await uploadCsv(page, rid);

    // Line descriptions from CSV appear in table
    await expect(page.getByText('Invoice March')).toBeVisible();
    await expect(page.getByText('Reversal')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s3-lines.png', fullPage: true });
  });

  test('NEW filter shows only non-duplicate lines', async ({ page, request }) => {
    const rid = runId();
    await loginAs(page, request);
    await uploadCsv(page, rid);

    await page.getByRole('button', { name: 'NEW' }).click();
    // 2 unique lines should be visible; deduplication hides 1
    const rows = page.locator('tbody tr');
    await expect(rows).toHaveCount(2);
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s3-filter-new.png', fullPage: true });
  });

  test('DUPLICATES filter shows only duplicate lines', async ({ page, request }) => {
    const rid = runId();
    await loginAs(page, request);
    await uploadCsv(page, rid);

    await page.getByRole('button', { name: 'DUPLICATES' }).click();
    const rows = page.locator('tbody tr');
    await expect(rows).toHaveCount(1);
    await page.screenshot({ path: 'e2e/screenshots/t2-p2-s3-filter-dup.png', fullPage: true });
  });
});
