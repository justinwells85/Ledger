/**
 * Tier 1 — P2-S1: Upload SAP CSV
 * SAP Data Administrator uploads a SAP CSV export file.
 * Spec: 20-e2e-scenario-matrix.md §4 P2-S1
 */
import { test, expect } from '@playwright/test';
import { mockApis, IMPORT_ID } from './support/api-mock';

const NEW_IMPORT_ID = 'new-staged-import-id';

test.describe('P2-S1: Upload SAP CSV', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);

    // Override upload endpoint to return a new staged import
    await page.route('**/api/v1/imports/upload', r =>
      r.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          importId: NEW_IMPORT_ID,
          filename: 'SAP_FY26_Mar.csv',
          status: 'STAGED',
          totalLines: 12,
          newLines: 10,
          duplicateLines: 2,
          errorLines: 0,
          importedAt: '2026-03-15T10:00:00Z',
        }),
      })
    );

    await page.goto('/import');
  });

  test('drop zone is visible with upload instructions', async ({ page }) => {
    await expect(page.getByText('Drag and drop SAP export file here')).toBeVisible();
    await expect(page.getByText('or click to browse')).toBeVisible();
    await expect(page.getByText('Accepted: .csv, .xlsx, .xls')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/sap-upload-dropzone.png', fullPage: true });
  });

  test('file input accepts CSV files', async ({ page }) => {
    const fileInput = page.locator('input[type="file"]');
    await expect(fileInput).toHaveAttribute('accept', /\.csv/);
  });

  test('uploading a CSV navigates to import review page', async ({ page }) => {
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles({
      name: 'SAP_FY26_Mar.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from('DOCUMENT_NUMBER,POSTING_DATE,AMOUNT,VENDOR_NAME,WBSE,DESCRIPTION\nDOC001,2026-03-01,5000.00,Globant S.A.,1174905.SU.ES,Invoice Q1\n'),
    });

    await expect(page).toHaveURL(`/import/${NEW_IMPORT_ID}`);
    await page.screenshot({ path: 'e2e/screenshots/sap-upload-navigated.png', fullPage: true });
  });

  test('import history shows existing imports', async ({ page }) => {
    await expect(page.getByText('IMPORT HISTORY')).toBeVisible();
    await expect(page.getByText('SAP_FY26_Jan.csv')).toBeVisible();
    await expect(page.getByText('COMMITTED')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/sap-upload-history.png', fullPage: true });
  });

  void IMPORT_ID;
});
