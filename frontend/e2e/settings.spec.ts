import { test, expect } from '@playwright/test';
import { mockApis } from './support/api-mock';

test.describe('Settings', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/settings');
  });

  test('shows all configuration sections', async ({ page }) => {
    await expect(page.getByText('RECONCILIATION TOLERANCE')).toBeVisible();
    await expect(page.getByText('ACCRUAL AGING')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/settings-page.png', fullPage: true });
  });

  test('shows config rows with current values', async ({ page }) => {
    await expect(page.getByText('Tolerance (%)')).toBeVisible();
    await expect(page.locator('input[value="0.02"]')).toBeVisible();
    await expect(page.getByText('Tolerance ($)')).toBeVisible();
    await expect(page.locator('input[value="50.00"]')).toBeVisible();
  });

  test('shows all four config settings', async ({ page }) => {
    await expect(page.getByText('Warning Threshold (days)')).toBeVisible();
    await expect(page.getByText('Critical Threshold (days)')).toBeVisible();
  });

  test('shows validation error without reason', async ({ page }) => {
    await page.getByRole('button', { name: 'Save' }).first().click();
    await expect(page.getByText('Reason is required')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/settings-validation-error.png', fullPage: true });
  });

  test('can type a reason and save successfully', async ({ page }) => {
    await page.getByPlaceholder('Reason for change…').first().fill('Audit Q2 review');
    await page.getByRole('button', { name: 'Save' }).first().click();
    await expect(page.getByText('Reason is required')).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/settings-save-success.png', fullPage: true });
  });

  test('can edit a config value', async ({ page }) => {
    // Use a positional textbox locator — nth(0) is time machine date, nth(1) is first value input
    const firstValueInput = page.getByRole('textbox').nth(1);
    await firstValueInput.clear();
    await firstValueInput.fill('0.03');
    await page.getByPlaceholder('Reason for change…').first().fill('Updating tolerance');
    await page.getByRole('button', { name: 'Save' }).first().click();
    await expect(page.getByText('Reason is required')).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/settings-value-changed.png', fullPage: true });
  });
});
