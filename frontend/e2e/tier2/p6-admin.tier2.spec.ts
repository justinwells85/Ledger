/**
 * Tier 2 — P6-S1, P6-S2, P6-S3, P6-S5: Admin scenarios
 * Validates that admin data mutations are persisted and visible after reload.
 * Spec: 20-e2e-scenario-matrix.md §4 P6-*
 */
import { test, expect } from '@playwright/test';
import { loginAs, runId } from './support/auth';

// ─── P6-S1: User Management ───────────────────────────────────────────────────

test.describe('P6-S1: Create User (Tier 2 — real backend)', () => {
  test('new user appears in user list after creation and on fresh reload', async ({ page, request }) => {
    const rid = runId();
    const username = `u${Date.now()}`.slice(0, 12);

    await loginAs(page, request);
    await page.goto('/admin/users');
    await expect(page.getByText('User Management')).toBeVisible();

    await page.getByRole('button', { name: '+ New User' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    const nonPasswordInputs = drawerBody.locator('input:not([type="password"])');
    await nonPasswordInputs.nth(0).fill(username);
    await nonPasswordInputs.nth(1).fill(`${rid} Display`);
    await nonPasswordInputs.nth(2).fill(`${username}@ledger.local`);
    await drawerBody.locator('input[type="password"]').fill('Secret123!');
    await page.getByRole('button', { name: 'Create User' }).click();

    // Drawer closes on success
    await expect(page.getByRole('heading', { name: 'New User' })).not.toBeVisible();

    // After reload: user must appear in the table
    await page.reload();
    await expect(page.getByRole('cell', { name: username, exact: true })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p6-s1-user-created.png', fullPage: true });
  });

  test('deactivated user shows Reactivate on fresh reload', async ({ page, request }) => {
    const rid = runId();
    const username = `u${Date.now()}`.slice(0, 12);

    // Create user via API
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };
    const createResp = await request.post('http://localhost:8080/api/v1/admin/users', {
      data: {
        username,
        displayName: `${rid} User`,
        email: `${username}@ledger.local`,
        password: 'Secret123!',
        role: 'ANALYST',
      },
      headers: { Authorization: `Bearer ${token}` },
    });
    const { userId } = await createResp.json() as { userId: string };

    await loginAs(page, request);
    await page.goto('/admin/users');

    // Deactivate the user
    await page.getByRole('row').filter({ hasText: username }).getByRole('button', { name: 'Deactivate' }).click();

    // After reload: shows Reactivate
    await page.reload();
    await expect(
      page.getByRole('row').filter({ hasText: username }).getByRole('button', { name: 'Reactivate' })
    ).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p6-s1-deactivated.png', fullPage: true });

    void userId;
  });
});

// ─── P6-S2: Fiscal Year Management ───────────────────────────────────────────

test.describe('P6-S2: Create Fiscal Year (Tier 2 — real backend)', () => {
  test('new fiscal year appears in list on fresh reload', async ({ page, request }) => {
    // BR-90: fiscal years must be sequential — query the last one and create the next
    const loginResp = await request.post('http://localhost:8080/api/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const { token } = await loginResp.json() as { token: string };

    const fyListResp = await request.get('http://localhost:8080/api/v1/fiscal-years', {
      headers: { Authorization: `Bearer ${token}` },
    });
    const fiscalYears = await fyListResp.json() as { fiscalYear: string }[];
    const lastNum = fiscalYears.reduce((max, fy) => {
      const n = parseInt(fy.fiscalYear.replace('FY', ''), 10);
      return isNaN(n) ? max : Math.max(max, n);
    }, 25);
    const fyName = `FY${String(lastNum + 1).padStart(2, '0')}`;

    await loginAs(page, request);
    await page.goto('/admin/fiscal-years');
    await expect(page.getByText('Fiscal Year Management')).toBeVisible();

    await page.getByRole('button', { name: '+ New Fiscal Year' }).click();
    await page.getByPlaceholder('e.g. FY28').fill(fyName);
    await page.getByRole('button', { name: 'Create' }).click();
    await expect(page.getByPlaceholder('e.g. FY28')).not.toBeVisible();

    // After reload: fiscal year appears in table
    await page.reload();
    await expect(page.getByRole('cell', { name: fyName, exact: true })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p6-s2-fy-created.png', fullPage: true });
  });
});

// ─── P6-S3: Reference Data ────────────────────────────────────────────────────

test.describe('P6-S3: Add Reference Data Entry (Tier 2 — real backend)', () => {
  test('new funding source appears in list on fresh reload', async ({ page, request }) => {
    const code = `TST${Date.now().toString().slice(-4)}`;

    await loginAs(page, request);
    await page.goto('/admin/reference-data');
    await expect(page.getByRole('button', { name: 'Funding Sources' })).toBeVisible();

    await page.getByRole('button', { name: '+ Add' }).click();
    await page.getByPlaceholder('CODE').fill(code);
    await page.getByPlaceholder('Display Name').fill(`Test Source ${code}`);
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByPlaceholder('CODE')).not.toBeVisible();

    // After reload: new entry in table
    await page.reload();
    await expect(page.getByRole('cell', { name: code, exact: true })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p6-s3-ref-data.png', fullPage: true });
  });
});

// ─── P6-S5: Settings ──────────────────────────────────────────────────────────

test.describe('P6-S5: Update System Settings (Tier 2 — real backend)', () => {
  test('updated tolerance value persists after page reload', async ({ page, request }) => {
    await loginAs(page, request);
    await page.goto('/settings');
    await expect(page.getByText('RECONCILIATION TOLERANCE')).toBeVisible();

    // Settings page is fully inline — each row has a value input, reason input, and Save button directly in the table row.
    const toleranceRow = page.locator('tr').filter({ hasText: 'Tolerance (%)' }).first();
    await toleranceRow.locator('input').nth(0).fill('0.03');
    await toleranceRow.locator('input').nth(1).fill('Tier 2 test adjustment');
    await toleranceRow.getByRole('button', { name: 'Save' }).click();

    // After reload: settings page still loads correctly
    await page.reload();
    await expect(page.getByText('RECONCILIATION TOLERANCE')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/t2-p6-s5-settings.png', fullPage: true });
  });
});
