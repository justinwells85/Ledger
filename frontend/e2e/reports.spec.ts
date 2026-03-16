import { test, expect } from '@playwright/test';
import { mockApis } from './support/api-mock';

test.describe('Budget Report', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/reports/budget');
  });

  test('renders period columns and row data', async ({ page }) => {
    await expect(page.getByText('Globant ADM').first()).toBeVisible();
    await expect(page.getByText('FY26-04-JAN')).toBeVisible();
    await expect(page.getByText('FY26-05-FEB')).toBeVisible();
    // $45,000.00 appears in both the row total and the grand total — just assert one
    await expect(page.getByText('$45,000.00').first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/report-budget.png', fullPage: true });
  });

  test('shows grand total row', async ({ page }) => {
    await expect(page.getByText('GRAND TOTAL')).toBeVisible();
  });

  test('can switch to By Quarter groupBy', async ({ page }) => {
    // First combobox is fiscal year; second is groupBy
    await page.getByRole('combobox').nth(1).selectOption('By Quarter');
    await page.screenshot({ path: 'e2e/screenshots/report-budget-quarter.png', fullPage: true });
  });

  test('export CSV button is visible', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Export CSV' })).toBeVisible();
  });

  test('contract/project filter narrows visible rows (P5-S8)', async ({ page }) => {
    await expect(page.getByText('DPI Photopass')).toBeVisible();
    await page.getByPlaceholder('Filter by contract or project…').fill('xyz-no-match');
    await expect(page.getByText('DPI Photopass')).not.toBeVisible();
    await page.getByPlaceholder('Filter by contract or project…').clear();
    await expect(page.getByText('DPI Photopass')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/budget-report-filter.png', fullPage: true });
  });
});

test.describe('Variance Report', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/reports/variance');
  });

  test('shows under and over budget rows', async ({ page }) => {
    await expect(page.getByText('UNDER_BUDGET')).toBeVisible();
    await expect(page.getByText('OVER_BUDGET')).toBeVisible();
    await expect(page.getByText('DPI Photopass')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/report-variance.png', fullPage: true });
  });

  test('variance amounts formatted correctly', async ({ page }) => {
    await expect(page.getByText('$25,000.00').first()).toBeVisible();
    await expect(page.getByText('$15,000.00').first()).toBeVisible();
  });

  test('can change fiscal year', async ({ page }) => {
    await page.getByRole('combobox').selectOption('FY25');
    await page.screenshot({ path: 'e2e/screenshots/report-variance-fy25.png', fullPage: true });
  });
});

test.describe('Reconciliation Report', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/reports/reconciliation');
  });

  test('shows milestone reconciliation rows', async ({ page }) => {
    await expect(page.getByText('January Sustainment')).toBeVisible();
    // Status values appear in filter options AND table badges — use first match
    // Target the badge span specifically, not the hidden select option
    await expect(page.locator('span', { hasText: 'PARTIALLY_MATCHED' })).toBeVisible();
    await expect(page.getByText('Feb Sustainment')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/report-reconciliation.png', fullPage: true });
  });

  test('shows fully reconciled milestone', async ({ page }) => {
    await expect(page.locator('span', { hasText: 'FULLY_RECONCILED' })).toBeVisible();
  });
});

test.describe('Open Accruals Report', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/reports/accruals');
  });

  test('shows open accruals table', async ({ page }) => {
    await expect(page.getByText('January Sustainment')).toBeVisible();
    await expect(page.getByText('WARNING')).toBeVisible();
    await expect(page.getByText('65')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/report-open-accruals.png', fullPage: true });
  });

  test('can change fiscal year', async ({ page }) => {
    await page.getByRole('combobox').selectOption('FY25');
    await page.screenshot({ path: 'e2e/screenshots/report-accruals-fy25.png', fullPage: true });
  });
});

// ─── P4-S3: Funding Source Report ─────────────────────────────────────────────

test.describe('Funding Source Report (P4-S3)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/reports/funding');
  });

  test('shows funding source summary heading', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Funding Source Summary' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/funding-report.png', fullPage: true });
  });

  test('shows OPEX row with planned amount', async ({ page }) => {
    await expect(page.getByRole('cell', { name: 'OPEX' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/funding-report-rows.png', fullPage: true });
  });

  test('shows grand total row', async ({ page }) => {
    await expect(page.getByText('GRAND TOTAL')).toBeVisible();
    await expect(page.getByText('100%')).toBeVisible();
  });

  test('export CSV button is visible', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Export CSV' })).toBeVisible();
  });

  test('can change fiscal year', async ({ page }) => {
    await page.getByRole('combobox').selectOption('FY27');
    await expect(page.getByRole('heading', { name: 'Funding Source Summary' })).toBeVisible();
  });
});

// ─── P4-S5: Forecast Report ───────────────────────────────────────────────────

test.describe('Forecast Report (P4-S5)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/reports/forecast');
  });

  test('shows forecast report heading', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Forecast Report' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/forecast-report.png', fullPage: true });
  });

  test('shows project rows with planned and actuals', async ({ page }) => {
    await expect(page.getByText('DPI Photopass')).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Planned' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Actuals YTD' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Remaining' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/forecast-report-rows.png', fullPage: true });
  });

  test('shows total remaining row', async ({ page }) => {
    await expect(page.getByText('TOTAL REMAINING')).toBeVisible();
  });

  test('shows status badge on rows', async ({ page }) => {
    await expect(page.getByText('UNDER_BUDGET').first()).toBeVisible();
  });

  test('export CSV button is visible', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Export CSV' })).toBeVisible();
  });
});
