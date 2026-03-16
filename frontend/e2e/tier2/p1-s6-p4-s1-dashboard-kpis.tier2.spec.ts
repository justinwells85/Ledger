/**
 * Tier 2 — P1-S6 / P4-S1: View Dashboard KPI Cards
 * Validates that contracts with milestones and actuals are visible on the dashboard.
 * Documents the known visibility behavior: dashboard reads from variance report,
 * which only includes contracts with milestones.
 * Spec: 20-e2e-scenario-matrix.md §4 P1-S6, P4-S1
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

async function apiSetup(request: import('@playwright/test').APIRequestContext, rid: string) {
  const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
    data: { username: 'admin', password: 'admin' },
  });
  const { token } = await loginResp.json() as { token: string };

  const contractResp = await request.post('http://localhost:8080/api/v1/contracts', {
    data: { name: `${rid} Contract`, vendor: `${rid} Vendor`, ownerUser: 'Admin', startDate: '2026-01-01' },
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

  await request.post(`http://localhost:8080/api/v1/projects/${projectId}/milestones`, {
    data: {
      name: `${rid} Milestone`,
      plannedAmount: 50000,
      fiscalPeriodId: period.periodId,
      effectiveDate: '2026-01-01',
      reason: 'Initial budget',
      createdBy: 'admin',
    },
    headers: { Authorization: `Bearer ${token}` },
  });

  return { contractId, projectId, token };
}

test.describe('P1-S6 / P4-S1: Dashboard KPI Cards (Tier 2 — real backend)', () => {
  test('contract with milestone appears in dashboard variance report', async ({ page, request }) => {
    const rid = runId();
    const { contractId } = await apiSetup(request, rid);
    const contractName = `${rid} Contract`;

    await loginAs(page, request);
    await page.goto('/');

    // Dashboard reads from variance report — contract with milestones appears
    await expect(page.getByText(contractName)).toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: 'e2e/screenshots/t2-dashboard-contract-visible.png', fullPage: true });

    void contractId;
  });

  test('KPI cards render total budget and variance values', async ({ page, request }) => {
    const rid = runId();
    await apiSetup(request, rid);

    await loginAs(page, request);
    await page.goto('/');

    // KPI cards: TOTAL BUDGET, TOTAL ACTUAL, TOTAL VARIANCE
    await expect(page.getByText('TOTAL BUDGET')).toBeVisible();
    await expect(page.getByText('TOTAL ACTUAL')).toBeVisible();
    await expect(page.getByText('TOTAL VARIANCE')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-dashboard-kpis.png', fullPage: true });
  });

  test('contract created without milestones does not appear in dashboard (known behavior)', async ({ page, request }) => {
    const rid = runId();
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };

    // Create contract WITHOUT a milestone
    const contractResp = await request.post('http://localhost:8080/api/v1/contracts', {
      data: { name: `${rid} NoMilestone`, vendor: `${rid} Vendor`, ownerUser: 'Admin', startDate: '2026-01-01' },
      headers: { Authorization: `Bearer ${token}` },
    });
    await contractResp.json();

    await loginAs(page, request);
    await page.goto('/');

    // Dashboard reads from variance report which excludes contracts without milestones
    await expect(page.getByText(`${rid} NoMilestone`)).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-dashboard-no-milestone-contract.png', fullPage: true });
  });
});
