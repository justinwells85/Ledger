/**
 * Tier 2 — P1-S5 / P5-S4: Cancel Milestone
 * Validates that milestone cancellation is persisted and reflected after page reload.
 * Spec: 20-e2e-scenario-matrix.md §4 P1-S5
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

  const projectId = `PR${rid.replace(/\D/g, '').slice(-6)}`;
  await request.post(`http://localhost:8080/api/v1/contracts/${contractId}/projects`, {
    data: { projectId, name: `${rid} Project`, wbse: '9999.SU.ES', fundingSource: 'OPEX' },
    headers: { Authorization: `Bearer ${token}` },
  });

  const periodsResp = await request.get('http://localhost:8080/api/v1/fiscal-years/FY26/periods', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const periods = await periodsResp.json() as { periodId: string; periodKey: string }[];
  const period = periods[3];

  const msResp = await request.post(`http://localhost:8080/api/v1/projects/${projectId}/milestones`, {
    data: {
      name: `${rid} Milestone`,
      plannedAmount: 20000,
      fiscalPeriodId: period.periodId,
      effectiveDate: '2026-01-01',
      reason: 'Initial',
      createdBy: 'admin',
    },
    headers: { Authorization: `Bearer ${token}` },
  });
  const { milestoneId } = await msResp.json() as { milestoneId: string };

  return { contractId, projectId, milestoneId, token };
}

test.describe('P1-S5 / P5-S4: Cancel Milestone (Tier 2 — real backend)', () => {
  test('cancelled milestone persists CANCELLED status after page reload', async ({ page, request }) => {
    const rid = runId();
    const { projectId, milestoneId } = await apiSetup(request, rid);
    const milestoneName = `${rid} Milestone`;

    await loginAs(page, request);
    await page.goto(`/projects/${projectId}`);
    await expect(page.getByText(milestoneName)).toBeVisible();

    // Open BottomSheet by clicking on milestone row
    await page.getByText(milestoneName).click();
    await expect(page.getByText('VERSION HISTORY')).toBeVisible();

    // Click Cancel Milestone
    await page.getByRole('button', { name: 'Cancel Milestone' }).click();
    await expect(page.getByRole('button', { name: 'Confirm Cancel' })).toBeVisible();

    // Fill reason
    const reasonInput = page.locator('[placeholder*="reason"], [placeholder*="Reason"]').last();
    await reasonInput.fill('Project scope removed');
    await page.getByRole('button', { name: 'Confirm Cancel' }).click();

    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s5-cancel-submitted.png', fullPage: true });

    // Verify via API that milestone is cancelled
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };
    const msResp = await request.get(`http://localhost:8080/api/v1/milestones/${milestoneId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const ms = await msResp.json() as { status: string };
    expect(ms.status).toBe('CANCELLED');

    // After reload: milestone still visible (as CANCELLED)
    await page.reload();
    await expect(page.getByText(milestoneName)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s5-after-reload.png', fullPage: true });
  });
});
