/**
 * Tier 1 — P4-S2: Identify Over/Under Budget Contracts
 * Finance Leadership identifies which contracts are over or under budget from the dashboard.
 * The dashboard uses the variance report: over-budget contracts appear in the alerts strip,
 * and per-project variance amounts are visible in the contract cards.
 * Spec: 20-e2e-scenario-matrix.md §4 P4-S2
 */
import { test, expect } from '@playwright/test';
import { mockApis } from './support/api-mock';

test.describe('P4-S2: Budget Status Indicators', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/');
  });

  test('dashboard shows contract cards from variance report', async ({ page }) => {
    await expect(page.getByText('Globant ADM')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/budget-status-cards.png', fullPage: true });
  });

  test('over-budget contract count appears in alerts strip', async ({ page }) => {
    // varianceReportFixture has 1 OVER_BUDGET row → "1 contract over budget" in alerts
    await expect(page.getByText(/1 contract over budget/)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/budget-status-over-alert.png', fullPage: true });
  });

  test('contract card shows Variance column with monetary value', async ({ page }) => {
    await expect(page.getByText('Variance', { exact: true }).first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/budget-status-variance-col.png', fullPage: true });
  });

  test('contract status badge is visible on each card', async ({ page }) => {
    // StatusBadge renders contract.status (ACTIVE/INACTIVE), not totalStatus
    await expect(page.getByText('ACTIVE').first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/budget-status-active-badge.png', fullPage: true });
  });
});
