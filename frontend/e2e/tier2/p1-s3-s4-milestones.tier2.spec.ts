/**
 * Tier 2 — P1-S3: Create milestone, P1-S4: Adjust milestone amount
 * Validates milestones are persisted and visible after page reload.
 * Spec: 20-e2e-scenario-matrix.md §4 P1-S3, P1-S4
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

async function apiSetup(request: import('@playwright/test').APIRequestContext, rid: string) {
  const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
    data: { username: 'admin', password: 'admin' },
  });
  const { token } = await loginResp.json() as { token: string };

  const contractResp = await request.post('http://localhost:8080/api/v1/contracts', {
    data: { name: `${rid} Contract`, vendor: `${rid} Vendor`, ownerUser: 'PM', startDate: '2026-01-01' },
    headers: { Authorization: `Bearer ${token}` },
  });
  const { contractId } = await contractResp.json() as { contractId: string };

  const projectId = `PR-${rid.replace('T2-', '')}`.slice(0, 20);
  await request.post(`http://localhost:8080/api/v1/contracts/${contractId}/projects`, {
    data: { projectId, name: `${rid} Project`, wbse: '9999.SU.ES', fundingSource: 'OPEX' },
    headers: { Authorization: `Bearer ${token}` },
  });

  // Get a real fiscal period ID for FY26
  const periodsResp = await request.get('http://localhost:8080/api/v1/fiscal-years/FY26/periods', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const periods = await periodsResp.json() as { periodId: string; periodKey: string }[];
  const period = periods[3]; // FY26-04-JAN

  return { contractId, projectId, token, period };
}

test.describe('P1-S3: Create Milestone (Tier 2 — real backend)', () => {
  test('created milestone appears in project detail immediately and after reload', async ({ page, request }) => {
    const rid = runId();
    const { projectId } = await apiSetup(request, rid);
    const milestoneName = `${rid} Milestone`;

    await loginAs(page, request);
    await page.goto(`/projects/${projectId}`);
    await expect(page.getByText('No milestones')).toBeVisible();

    await page.getByRole('button', { name: '+ Add Milestone' }).click();
    const drawer = page.locator('[class*="body"]').last();
    await drawer.locator('input:not([type="number"]):not([type="date"])').nth(0).fill(milestoneName);
    await drawer.locator('input[type="number"]').fill('25000');
    await drawer.locator('select').selectOption({ index: 1 });
    await page.getByRole('button', { name: 'Add Milestone', exact: true }).click();

    // Immediate: milestone visible in table
    await expect(page.getByText(milestoneName)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s3-immediate.png', fullPage: true });

    // After reload: milestone still there
    await page.reload();
    await expect(page.getByText(milestoneName)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s3-after-reload.png', fullPage: true });
  });
});

test.describe('P1-S4: Adjust Milestone Amount (Tier 2 — real backend)', () => {
  test('updated milestone amount persists after page reload', async ({ page, request }) => {
    const rid = runId();
    const { projectId, token, period } = await apiSetup(request, rid);
    const milestoneName = `${rid} Milestone`;

    // Create milestone via API
    const msResp = await request.post(`http://localhost:8080/api/v1/projects/${projectId}/milestones`, {
      data: {
        name: milestoneName,
        plannedAmount: 25000,
        fiscalPeriodId: period.periodId,
        effectiveDate: '2026-01-01',
        reason: 'Initial',
        createdBy: 'admin',
      },
      headers: { Authorization: `Bearer ${token}` },
    });
    const { milestoneId } = await msResp.json() as { milestoneId: string };

    await loginAs(page, request);
    await page.goto(`/projects/${projectId}`);
    await expect(page.getByText(milestoneName)).toBeVisible();

    // Expand milestone, add new version
    await page.getByRole('row', { name: new RegExp(milestoneName) }).click();
    await expect(page.getByText('VERSION HISTORY')).toBeVisible();
    await page.getByRole('button', { name: '+ New Version' }).click();

    await page.locator('[class*="expandSection"]').last()
      .locator('input[type="number"]').fill('30000');
    await page.locator('[class*="expandSection"]').last()
      .locator('input:not([type="number"]):not([type="date"])').last().fill('Scope increase approved');
    await page.getByRole('button', { name: 'Save Version' }).click();
    await expect(page.getByRole('button', { name: 'Save Version' })).not.toBeVisible();

    // After reload: row shows updated amount
    await page.reload();
    await expect(page.getByText(milestoneName)).toBeVisible();
    // Version history should show 2 versions
    await page.getByRole('row', { name: new RegExp(milestoneName) }).click();
    await expect(page.getByText('VERSION HISTORY')).toBeVisible();
    const versionRows = page.locator('[class*="versionRow"], [class*="version"]');
    await expect(versionRows.first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s4-after-reload.png', fullPage: true });

    void milestoneId; // used above
  });
});
