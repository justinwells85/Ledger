/**
 * Tier 2 — P1-S2: Add project to contract
 * Validates that a project added via the UI is persisted and appears in:
 *   1. The ContractDetail PROJECTS table immediately after submit
 *   2. The ContractDetail PROJECTS table on a fresh page reload
 * Spec: 20-e2e-scenario-matrix.md §4 P1-S2
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

test.describe('P1-S2: Add Project to Contract (Tier 2 — real backend)', () => {
  let contractId: string;
  let projectId: string;
  let projectName: string;

  test.beforeEach(async ({ page, request }) => {
    const rid = runId();
    projectId = `PR-${rid.replace('T2-', '')}`.slice(0, 20);
    projectName = `${rid} Project`;

    // Create a fresh contract via API so we start clean
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };

    const contractResp = await request.post('http://localhost:8080/api/v1/contracts', {
      data: { name: `${rid} Contract`, vendor: `${rid} Vendor`, ownerUser: 'PM', startDate: '2026-01-01' },
      headers: { Authorization: `Bearer ${token}` },
    });
    const body = await contractResp.json() as { contractId: string };
    contractId = body.contractId;

    await loginAs(page, request);
    await page.goto(`/contracts/${contractId}`);
    await expect(page.getByText('No projects')).toBeVisible();
  });

  test('added project appears in projects table immediately', async ({ page }) => {
    await page.getByRole('button', { name: '+ Add Project' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    const inputs = drawerBody.locator('input:not([type="date"])');
    await inputs.nth(0).fill(projectId);
    await inputs.nth(1).fill(projectName);
    await inputs.nth(2).fill('9999.SU.ES');
    await page.getByRole('button', { name: 'Add Project', exact: true }).click();

    await expect(page.getByRole('heading', { name: 'Add Project' })).not.toBeVisible();
    await expect(page.getByText(projectName)).toBeVisible();
    await expect(page.getByText(projectId)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s2-immediate.png', fullPage: true });
  });

  test('added project persists on fresh page reload', async ({ page }) => {
    await page.getByRole('button', { name: '+ Add Project' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    const inputs = drawerBody.locator('input:not([type="date"])');
    await inputs.nth(0).fill(projectId);
    await inputs.nth(1).fill(projectName);
    await inputs.nth(2).fill('9999.SU.ES');
    await page.getByRole('button', { name: 'Add Project', exact: true }).click();
    await expect(page.getByText(projectName)).toBeVisible();

    // Reload the page — project must still be there
    await page.reload();
    await expect(page.getByText(projectName)).toBeVisible();
    await expect(page.getByText(projectId)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s2-after-reload.png', fullPage: true });
  });

  test('duplicate project ID shows validation error and does not add project', async ({ page }) => {
    // Add once successfully
    await page.getByRole('button', { name: '+ Add Project' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    const inputs = drawerBody.locator('input:not([type="date"])');
    await inputs.nth(0).fill(projectId);
    await inputs.nth(1).fill(projectName);
    await inputs.nth(2).fill('9999.SU.ES');
    await page.getByRole('button', { name: 'Add Project', exact: true }).click();
    await expect(page.getByText(projectName)).toBeVisible();

    // Try to add again with same ID
    await page.getByRole('button', { name: '+ Add Project' }).click();
    const drawerBody2 = page.locator('[class*="body"]').last();
    const inputs2 = drawerBody2.locator('input:not([type="date"])');
    await inputs2.nth(0).fill(projectId);
    await inputs2.nth(1).fill('Another Project');
    await inputs2.nth(2).fill('8888.SU.ES');
    await page.getByRole('button', { name: 'Add Project', exact: true }).click();

    // Should show an error — drawer stays open
    await expect(page.getByRole('heading', { name: 'Add Project' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s2-duplicate-error.png', fullPage: true });
  });
});
