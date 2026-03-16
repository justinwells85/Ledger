/**
 * Tier 2 — P5-S9: Audit Trail from Contract
 * Validates that operations on a contract create real audit log entries visible via the UI.
 * Spec: 20-e2e-scenario-matrix.md §4 P5-S9
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

async function createContract(request: import('@playwright/test').APIRequestContext, rid: string) {
  const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
    data: { username: 'admin', password: 'admin' },
  });
  const { token } = await loginResp.json() as { token: string };

  const contractResp = await request.post('http://localhost:8080/api/v1/contracts', {
    data: { name: `${rid} Contract`, vendor: `${rid} Vendor`, ownerUser: 'PM', startDate: '2026-01-01' },
    headers: { Authorization: `Bearer ${token}` },
  });
  const { contractId } = await contractResp.json() as { contractId: string };
  return { contractId, token };
}

test.describe('P5-S9: Contract Audit Trail (Tier 2 — real backend)', () => {
  test('Audit Trail button navigates to audit log filtered for contract', async ({ page, request }) => {
    const rid = runId();
    const { contractId } = await createContract(request, rid);

    await loginAs(page, request);
    await page.goto(`/contracts/${contractId}`);
    await expect(page.getByRole('button', { name: 'Audit Trail' })).toBeVisible();

    await page.getByRole('button', { name: 'Audit Trail' }).click();

    await expect(page).toHaveURL(/\/admin\/audit/);
    await expect(page).toHaveURL(new RegExp(contractId));
    await page.screenshot({ path: 'e2e/screenshots/t2-p5-s9-audit-nav.png', fullPage: true });
  });

  test('audit log shows CREATE entry for newly created contract', async ({ page, request }) => {
    const rid = runId();
    const { contractId } = await createContract(request, rid);

    await loginAs(page, request);
    await page.goto(`/contracts/${contractId}`);
    await page.getByRole('button', { name: 'Audit Trail' }). click();

    // Audit log pre-filtered for this contract should show CREATE action
    await expect(page.getByText('CREATE')).toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: 'e2e/screenshots/t2-p5-s9-create-entry.png', fullPage: true });
  });

  test('audit log shows UPDATE entry after contract status change', async ({ page, request }) => {
    const rid = runId();
    const { contractId, token } = await createContract(request, rid);

    // Update contract status via API to generate an audit entry
    await request.put(`http://localhost:8080/api/v1/contracts/${contractId}`, {
      data: { status: 'INACTIVE', reason: 'Audit trail test update' },
      headers: { Authorization: `Bearer ${token}` },
    });

    await loginAs(page, request);
    await page.goto(`/contracts/${contractId}`);
    await page.getByRole('button', { name: 'Audit Trail' }).click();

    await expect(page.getByText('UPDATE')).toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: 'e2e/screenshots/t2-p5-s9-update-entry.png', fullPage: true });
  });

  test('audit log entity type filter is pre-populated as CONTRACT', async ({ page, request }) => {
    const rid = runId();
    const { contractId } = await createContract(request, rid);

    await loginAs(page, request);
    await page.goto(`/contracts/${contractId}`);
    await page.getByRole('button', { name: 'Audit Trail' }).click();

    // Entity type select should be pre-set to CONTRACT
    const entityTypeSelect = page.locator('select').first();
    await expect(entityTypeSelect).toHaveValue('CONTRACT');
    await page.screenshot({ path: 'e2e/screenshots/t2-p5-s9-filter-prefilled.png', fullPage: true });
  });
});
