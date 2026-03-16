/**
 * Tier 2 — P1-S1: Create new contract
 * Validates that a contract created via the UI is persisted and appears in:
 *   1. The contract detail page (immediately after creation)
 *   2. The dashboard CONTRACT SUMMARY table (navigating back to /)
 * Spec: 20-e2e-scenario-matrix.md §4 P1-S1
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

test.describe('P1-S1: Create Contract (Tier 2 — real backend)', () => {
  let contractName: string;
  let contractVendor: string;

  test.beforeEach(async ({ page, request }) => {
    contractName = `${runId()} Vendor Contract`;
    contractVendor = `${runId()} Corp`;
    await loginAs(page, request);
    await page.goto('/');
  });

  test('created contract navigates to detail page showing correct data', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Contract' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    const textInputs = drawerBody.locator('input:not([type="date"])');
    await textInputs.nth(0).fill(contractName);
    await textInputs.nth(1).fill(contractVendor);
    await textInputs.nth(2).fill('Finance Manager');
    await drawerBody.locator('input[type="date"]').fill('2026-01-01');
    await page.getByRole('button', { name: 'Create Contract' }).click();

    // Contract detail page — name and vendor must be visible
    await expect(page).toHaveURL(/\/contracts\/.+/);
    await expect(page.getByRole('heading', { name: contractName })).toBeVisible();
    await expect(page.getByText(contractVendor)).toBeVisible();
    await expect(page.getByText('ACTIVE')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s1-detail.png', fullPage: true });
  });

  test('created contract appears in dashboard CONTRACT SUMMARY', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Contract' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    const textInputs = drawerBody.locator('input:not([type="date"])');
    await textInputs.nth(0).fill(contractName);
    await textInputs.nth(1).fill(contractVendor);
    await textInputs.nth(2).fill('Finance Manager');
    await drawerBody.locator('input[type="date"]').fill('2026-01-01');
    await page.getByRole('button', { name: 'Create Contract' }).click();
    await expect(page).toHaveURL(/\/contracts\/.+/);

    // Navigate back to dashboard and confirm contract is listed
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('cell', { name: contractName })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s1-dashboard.png', fullPage: true });
  });

  test('created contract detail page is accessible on fresh load', async ({ page }) => {
    // Create via API to get the contractId, then verify the UI renders it correctly
    const resp = await page.request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await resp.json() as { token: string };

    const createResp = await page.request.post('http://localhost:8080/api/v1/contracts', {
      data: { name: contractName, vendor: contractVendor, ownerUser: 'Finance Manager', startDate: '2026-01-01' },
      headers: { Authorization: `Bearer ${token}` },
    });
    const { contractId } = await createResp.json() as { contractId: string };

    await page.goto(`/contracts/${contractId}`);
    await expect(page.getByRole('heading', { name: contractName })).toBeVisible();
    await expect(page.getByText(contractVendor)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s1-fresh-load.png', fullPage: true });
  });
});
