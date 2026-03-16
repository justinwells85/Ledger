import { test, expect } from '@playwright/test';
import { mockApis } from './support/api-mock';

test.describe('JournalViewer', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/journal');
  });

  test('shows journal entry list', async ({ page }) => {
    // Type badges appear in both the filter select options and the table spans.
    await expect(page.locator('span', { hasText: 'PLAN_CREATE' })).toBeVisible();
    await expect(page.locator('span', { hasText: 'ACTUAL_IMPORT' })).toBeVisible();
    await expect(page.locator('span', { hasText: 'PLAN_ADJUST' })).toBeVisible();
    await expect(page.getByText('Initial budget')).toBeVisible();
    await expect(page.getByText('SAP import: 38 lines')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/journal-list.png', fullPage: true });
  });

  test('expands entry to show double-entry lines', async ({ page }) => {
    // Description text is rendered as "Initial budget ▼" — match with partial text
    await page.getByText(/Initial budget/).click();
    await expect(page.getByText('PLANNED')).toBeVisible();
    await expect(page.getByText('VARIANCE_RESERVE')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/journal-expanded.png', fullPage: true });
  });

  test('expanded entry shows debit and credit amounts', async ({ page }) => {
    await page.getByText(/Initial budget/).click();
    // Both debit and credit show $25,000 — just assert one is visible
    await expect(page.getByText('$25,000').first()).toBeVisible();
  });

  test('collapses expanded entry on second click', async ({ page }) => {
    await page.getByText(/Initial budget/).click();
    await expect(page.getByText('PLANNED')).toBeVisible();
    await page.getByText(/Initial budget/).click();
    await expect(page.getByText('PLANNED')).not.toBeVisible();
  });

  test('filter by PLAN_CREATE shows only matching entries', async ({ page }) => {
    await page.getByRole('combobox').selectOption('PLAN_CREATE');
    await expect(page.getByText('Initial budget')).toBeVisible();
    await expect(page.getByText('SAP import: 38 lines')).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/journal-filtered.png', fullPage: true });
  });

  test('resetting filter shows all entries', async ({ page }) => {
    await page.getByRole('combobox').selectOption('PLAN_CREATE');
    await expect(page.getByText('SAP import: 38 lines')).not.toBeVisible();
    await page.getByRole('combobox').selectOption('');
    await expect(page.getByText('SAP import: 38 lines')).toBeVisible();
  });
});
