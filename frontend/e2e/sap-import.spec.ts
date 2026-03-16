import { test, expect } from '@playwright/test';
import { mockApis, IMPORT_ID } from './support/api-mock';

test.describe('SAP Import', () => {
  test.describe('Import History', () => {
    test.beforeEach(async ({ page }) => {
      await mockApis(page);
      await page.goto('/import');
    });

    test('shows import history table', async ({ page }) => {
      await expect(page.getByText('SAP_FY26_Jan.csv')).toBeVisible();
      await expect(page.getByText('COMMITTED')).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/import-history.png', fullPage: true });
    });

    test('shows STAGED badge for pending import', async ({ page }) => {
      await expect(page.getByText('SAP_FY26_Feb.csv')).toBeVisible();
      await expect(page.getByText('STAGED')).toBeVisible();
    });

    test('navigates to import review on row click', async ({ page }) => {
      await page.getByText('SAP_FY26_Jan.csv').click();
      await expect(page).toHaveURL(`/import/${IMPORT_ID}`);
      await page.screenshot({ path: 'e2e/screenshots/import-nav.png', fullPage: true });
    });
  });

  test.describe('Import Review', () => {
    test.beforeEach(async ({ page }) => {
      await mockApis(page);
      await page.goto(`/import/${IMPORT_ID}`);
    });

    test('import review shows summary cards', async ({ page }) => {
      await expect(page.getByText('SAP_FY26_Jan.csv')).toBeVisible();
      // TOTAL LINES card shows '3' — use exact match to avoid substring matches
      await expect(page.getByText('3', { exact: true })).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/import-review-summary.png', fullPage: true });
    });

    test('import review shows all lines by default', async ({ page }) => {
      await expect(page.getByText('Invoice')).toBeVisible();
      await expect(page.getByText('Accrual')).toBeVisible();
      await expect(page.getByText('Old line')).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/import-review-lines.png', fullPage: true });
    });

    test('NEW filter hides duplicate lines', async ({ page }) => {
      await expect(page.getByText('Invoice')).toBeVisible();
      await page.getByRole('button', { name: 'NEW' }).click();
      await expect(page.getByText('Invoice')).toBeVisible();
      await expect(page.getByText('Old line')).not.toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/import-filter-new.png', fullPage: true });
    });

    test('DUPLICATES filter shows only duplicate lines', async ({ page }) => {
      await expect(page.getByText('Invoice')).toBeVisible();
      await page.getByRole('button', { name: 'DUPLICATES' }).click();
      await expect(page.getByText('Old line')).toBeVisible();
      await expect(page.getByText('Invoice')).not.toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/import-filter-duplicates.png', fullPage: true });
    });

    test('commit button is visible for STAGED import', async ({ page }) => {
      await expect(page.getByRole('button', { name: 'Commit Import' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Reject Import' })).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/import-review-actions.png', fullPage: true });
    });

    // ─── P2-S4: Commit import ────────────────────────────────────────────────

    test('committing import navigates back to import history (P2-S4)', async ({ page }) => {
      await page.getByRole('button', { name: 'Commit Import' }).click();
      await expect(page).toHaveURL('/import');
      await page.screenshot({ path: 'e2e/screenshots/import-committed.png', fullPage: true });
    });

    // ─── P2-S5: Reject import ────────────────────────────────────────────────

    test('rejecting import navigates back to import history (P2-S5)', async ({ page }) => {
      page.on('dialog', dialog => dialog.accept());
      await page.getByRole('button', { name: 'Reject Import' }).click();
      await expect(page).toHaveURL('/import');
      await page.screenshot({ path: 'e2e/screenshots/import-rejected.png', fullPage: true });
    });

    test('canceling reject dialog stays on review page', async ({ page }) => {
      page.on('dialog', dialog => dialog.dismiss());
      await page.getByRole('button', { name: 'Reject Import' }).click();
      // Dialog dismissed — should remain on the review page
      await expect(page.getByRole('button', { name: 'Reject Import' })).toBeVisible();
    });
  });
});
