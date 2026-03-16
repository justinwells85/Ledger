/**
 * Tier 2 — P1-S12: Close a contract
 * Validates that status change to CLOSED persists after page reload.
 * Spec: 20-e2e-scenario-matrix.md §4 P1-S12
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

test.describe('P1-S12: Close Contract (Tier 2 — real backend)', () => {
  test('contract status CLOSED persists after page reload', async ({ page, request }) => {
    const rid = runId();

    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };

    const contractResp = await request.post('http://localhost:8080/api/v1/contracts', {
      data: { name: `${rid} Contract`, vendor: `${rid} Vendor`, ownerUser: 'PM', startDate: '2026-01-01' },
      headers: { Authorization: `Bearer ${token}` },
    });
    const { contractId } = await contractResp.json() as { contractId: string };

    await loginAs(page, request);
    await page.goto(`/contracts/${contractId}`);
    await expect(page.getByText('ACTIVE')).toBeVisible();

    // Close the contract
    await page.getByRole('button', { name: 'Edit' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    await drawerBody.locator('select').selectOption('CLOSED');
    await drawerBody.locator('input:not([type="date"])').last().fill('Contract period ended');
    await page.getByRole('button', { name: 'Save Changes' }).click();
    await expect(page.getByRole('heading', { name: 'Edit Contract' })).not.toBeVisible();

    // Immediate: status shows CLOSED
    await expect(page.getByText('CLOSED')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s12-closed.png', fullPage: true });

    // After reload: CLOSED persists
    await page.reload();
    await expect(page.getByText('CLOSED')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s12-after-reload.png', fullPage: true });
  });
});
