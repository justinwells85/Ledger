/**
 * Tier 1 — P5-S1: View Milestones with Status and Remaining
 * Project Manager views all milestones for a project with status and remaining amounts.
 * ProjectDetail uses CashflowTable: fiscal periods are column headers (shortened, e.g. "Jan"),
 * milestones are rows. Clicking the period cell for a milestone opens a BottomSheet.
 * Spec: 20-e2e-scenario-matrix.md §4 P5-S1
 */
import { test, expect } from '@playwright/test';
import { mockApis, PROJECT_ID } from './support/api-mock';

test.describe('P5-S1: View Milestones with Status and Remaining', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto(`/projects/${PROJECT_ID}`);
  });

  test('milestones table is visible with at least one milestone row', async ({ page }) => {
    await expect(page.getByText('January Sustainment')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestones-table.png', fullPage: true });
  });

  test('cashflow table shows fiscal period as shortened column header', async ({ page }) => {
    // CashflowTable shortens "FY26-04-JAN" → "Jan" for column headers
    await expect(page.getByRole('columnheader', { name: 'Jan' })).toBeVisible();
  });

  test('cashflow table shows planned amount in milestone row', async ({ page }) => {
    // $25,000 may appear multiple times; use first visible instance
    await expect(page.getByText('$25,000').first()).toBeVisible();
  });

  test('clicking the period cell opens bottom sheet', async ({ page }) => {
    // The milestone is in FY26-04-JAN (Jan = column index 1 after the name cell at index 0)
    const milestoneRow = page.locator('tr', { hasText: 'January Sustainment' });
    await milestoneRow.locator('td').nth(1).click();
    await expect(page.getByText('RECONCILIATION', { exact: true })).toBeVisible({ timeout: 5_000 });
    await page.screenshot({ path: 'e2e/screenshots/milestones-bottom-sheet.png', fullPage: true });
  });

  test('reconciliation summary shows Remaining amount', async ({ page }) => {
    const milestoneRow = page.locator('tr', { hasText: 'January Sustainment' });
    await milestoneRow.locator('td').nth(1).click();
    await expect(page.getByText('Remaining')).toBeVisible({ timeout: 5_000 });
    // reconciliationStatusFixture.remaining = 10000
    await expect(page.getByText('$10,000').first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestones-remaining.png', fullPage: true });
  });

  test('bottom sheet shows version history section', async ({ page }) => {
    const milestoneRow = page.locator('tr', { hasText: 'January Sustainment' });
    await milestoneRow.locator('td').nth(1).click();
    await expect(page.getByText('VERSION HISTORY')).toBeVisible({ timeout: 5_000 });
    await page.screenshot({ path: 'e2e/screenshots/milestones-versions.png', fullPage: true });
  });

  test('bottom sheet can be closed with Escape', async ({ page }) => {
    const milestoneRow = page.locator('tr', { hasText: 'January Sustainment' });
    await milestoneRow.locator('td').nth(1).click();
    await expect(page.getByText('RECONCILIATION', { exact: true })).toBeVisible({ timeout: 5_000 });
    await page.keyboard.press('Escape');
    await expect(page.getByText('VERSION HISTORY')).not.toBeVisible();
  });
});
