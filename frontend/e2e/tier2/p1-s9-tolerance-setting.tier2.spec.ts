/**
 * Tier 2 — P1-S9: Adjust Reconciliation Tolerance
 * Validates that a system config change is persisted and visible after reload.
 * Spec: 20-e2e-scenario-matrix.md §4 P1-S9
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

const ORIGINAL_TOLERANCE = '2';   // 0.02 stored as "0.02"; displayed as "2" or "0.02"
const NEW_TOLERANCE = '5';

test.describe('P1-S9: Adjust Reconciliation Tolerance (Tier 2 — real backend)', () => {
  test.afterEach(async ({ request }) => {
    // Restore original tolerance to avoid contaminating other tests
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };
    await request.put('http://localhost:8080/api/v1/config/tolerance_percent', {
      data: { configValue: '0.02', reason: 'Restore after test' },
      headers: { Authorization: `Bearer ${token}` },
    });
  });

  test('updated tolerance value persists after page reload', async ({ page, request }) => {
    await loginAs(page, request);
    await page.goto('/settings');

    // Find the Tolerance (%) row and its value input
    const toleranceRow = page.locator('tr', { hasText: 'Tolerance (%)' });
    await expect(toleranceRow).toBeVisible();

    const valueInput = toleranceRow.locator('input[type="number"], input[type="text"]').first();
    await valueInput.triple_click();
    await valueInput.fill(NEW_TOLERANCE);

    const reasonInput = toleranceRow.locator('input[type="text"]').last();
    await reasonInput.fill('Testing tolerance change');

    const saveBtn = toleranceRow.getByRole('button', { name: 'Save' });
    await saveBtn.click();
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s9-saved.png', fullPage: true });

    // After reload: new value is still shown
    await page.reload();
    const reloadedRow = page.locator('tr', { hasText: 'Tolerance (%)' });
    const reloadedInput = reloadedRow.locator('input[type="number"], input[type="text"]').first();
    const reloadedValue = await reloadedInput.inputValue();
    // Backend stores as decimal (0.05) but may display in various ways; either form is acceptable
    expect(['5', '0.05', '5.0', '0.050']).toContain(reloadedValue);
    await page.screenshot({ path: 'e2e/screenshots/t2-p1-s9-after-reload.png', fullPage: true });
  });

  test('tolerance change is reflected in GET /config API response', async ({ page, request }) => {
    await loginAs(page, request);
    await page.goto('/settings');

    const toleranceRow = page.locator('tr', { hasText: 'Tolerance (%)' });
    const valueInput = toleranceRow.locator('input[type="number"], input[type="text"]').first();
    await valueInput.triple_click();
    await valueInput.fill(NEW_TOLERANCE);

    const reasonInput = toleranceRow.locator('input[type="text"]').last();
    await reasonInput.fill('Tolerance API check');

    await toleranceRow.getByRole('button', { name: 'Save' }).click();

    // Verify via API
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };
    const configResp = await request.get('http://localhost:8080/api/v1/config', {
      headers: { Authorization: `Bearer ${token}` },
    });
    const config = await configResp.json() as { configKey: string; configValue: string }[];
    const toleranceSetting = config.find(c => c.configKey === 'tolerance_percent');
    expect(toleranceSetting).toBeDefined();
    // Value stored as decimal
    expect(parseFloat(toleranceSetting!.configValue)).toBeCloseTo(0.05, 3);

    void ORIGINAL_TOLERANCE;
  });
});
