/**
 * E2E tests for the Time Machine feature.
 * Spec: 08-time-machine.md
 * Covers: P1-S10, P4-S4 — viewing data as of a prior date.
 */
import { test, expect } from '@playwright/test';
import { mockApis } from './support/api-mock';

test.describe('Time Machine (P1-S10, P4-S4)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/');
  });

  test('time machine date input is present in the header', async ({ page }) => {
    await expect(page.getByLabel('Time Machine:')).toBeVisible();
  });

  test('setting a date shows the time machine banner', async ({ page }) => {
    await page.getByLabel('Time Machine:').fill('2026-01-01');
    await expect(page.getByText(/TIME MACHINE ACTIVE/)).toBeVisible();
    await expect(page.getByText(/2026-01-01/)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/time-machine-active.png', fullPage: true });
  });

  test('banner shows a Reset button when time machine is active', async ({ page }) => {
    await page.getByLabel('Time Machine:').fill('2026-01-01');
    // Two reset buttons: one in header, one in banner
    await expect(page.getByRole('button', { name: 'Reset' }).first()).toBeVisible();
  });

  test('clicking Reset clears the banner', async ({ page }) => {
    await page.getByLabel('Time Machine:').fill('2026-01-01');
    await expect(page.getByText(/TIME MACHINE ACTIVE/)).toBeVisible();
    // Click the banner's Reset button
    await page.getByText('Reset').first().click();
    await expect(page.getByText(/TIME MACHINE ACTIVE/)).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/time-machine-reset.png', fullPage: true });
  });

  test('clearing the date input hides the banner', async ({ page }) => {
    await page.getByLabel('Time Machine:').fill('2026-01-01');
    await expect(page.getByText(/TIME MACHINE ACTIVE/)).toBeVisible();
    await page.getByLabel('Time Machine:').fill('');
    await expect(page.getByText(/TIME MACHINE ACTIVE/)).not.toBeVisible();
  });

  test('time machine date is passed as asOfDate to variance report', async ({ page }) => {
    let capturedUrl = '';
    await page.route('**/api/v1/reports/variance**', async r => {
      capturedUrl = r.request().url();
      await r.continue();
    });
    await page.getByLabel('Time Machine:').fill('2026-01-01');
    // Wait for the re-fetch triggered by the date change
    await page.waitForTimeout(300);
    expect(capturedUrl).toContain('asOfDate=2026-01-01');
    await page.screenshot({ path: 'e2e/screenshots/time-machine-api-param.png', fullPage: true });
  });
});
