/**
 * Tier 1 — P4-S7: See Active Alerts
 * Finance Leadership views active alerts on the dashboard.
 * Spec: 20-e2e-scenario-matrix.md §4 P4-S7
 */
import { test, expect, Route } from '@playwright/test';
import { mockApis, varianceReportFixture } from './support/api-mock';

test.describe('P4-S7: Active Alerts', () => {
  test.describe('with alert data', () => {
    test.beforeEach(async ({ page }) => {
      await mockApis(page);
      await page.goto('/');
    });

    test('alerts strip is visible when there are alerts', async ({ page }) => {
      // varianceReportFixture has 1 OVER_BUDGET row and openAccrualsFixture has 1 row
      // The alerts strip only renders when there are open accruals OR over-budget contracts
      await expect(page.getByText(/1 contract over budget/)).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/alerts-strip.png', fullPage: true });
    });

    test('over budget chip shows correct count', async ({ page }) => {
      await expect(page.getByText(/1 contract over budget/)).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/alerts-over-budget.png', fullPage: true });
    });

    test('open accruals aging alert chip shows count', async ({ page }) => {
      // openAccrualsFixture has 1 row → "1 open accrual aging"
      await expect(page.getByText('⚠ 1 open accrual aging')).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/alerts-open-accruals.png', fullPage: true });
    });

    test('clicking over budget alert navigates to reports', async ({ page }) => {
      await page.getByText(/1 contract over budget/).click();
      await expect(page).toHaveURL('/reports');
    });
  });

  test.describe('with no alerts', () => {
    test('alerts strip is absent when no over-budget contracts or open accruals', async ({ page }) => {
      await mockApis(page);

      // Override to produce zero alerts
      await page.route('**/api/v1/reports/variance**', (r: Route) =>
        r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            ...varianceReportFixture,
            rows: varianceReportFixture.rows.map(row => ({ ...row, totalStatus: 'UNDER_BUDGET' })),
          }),
        })
      );
      await page.route('**/api/v1/reports/open-accruals**', (r: Route) =>
        r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fiscalYear: 'FY26', rows: [] }),
        })
      );

      await page.goto('/');
      // With no alerts, the alertsStrip div is not rendered at all
      await expect(page.getByText(/open accrual aging/)).not.toBeVisible();
      await expect(page.getByText(/contract over budget/)).not.toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/alerts-none.png', fullPage: true });
    });
  });
});
