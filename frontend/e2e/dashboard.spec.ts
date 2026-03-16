import { test, expect } from '@playwright/test';
import { mockApis, CONTRACT_ID } from './support/api-mock';

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/');
  });

  test('shows KPI cards with aggregated totals', async ({ page }) => {
    await expect(page.getByText('TOTAL BUDGET')).toBeVisible();
    await expect(page.getByText('TOTAL ACTUALS')).toBeVisible();
    await expect(page.getByText('OVERALL VARIANCE')).toBeVisible();
    // variances fixture: 25000+30000=55000 planned → $55K (appears in KPI + table + footer)
    await expect(page.getByText('$55K').first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/dashboard-kpis.png', fullPage: true });
  });

  test('shows contract summary table', async ({ page }) => {
    await expect(page.getByText('CONTRACT SUMMARY')).toBeVisible();
    await expect(page.getByText('Globant ADM').first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/dashboard-contract-summary.png', fullPage: true });
  });

  test('shows over-budget alert', async ({ page }) => {
    await expect(page.getByText(/1 milestone over budget/)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/dashboard-alerts.png', fullPage: true });
  });

  test('shows open accruals alert', async ({ page }) => {
    await expect(page.getByText(/1 open accrual/)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/dashboard-accruals-alert.png', fullPage: true });
  });

  test('navigates to contract detail on row click', async ({ page }) => {
    await page.getByText('Globant ADM').first().click();
    await expect(page).toHaveURL(`/contracts/${CONTRACT_ID}`);
    await page.screenshot({ path: 'e2e/screenshots/dashboard-nav-to-contract.png', fullPage: true });
  });
});
