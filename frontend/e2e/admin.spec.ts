/**
 * E2E tests for System Administrator persona (P6) scenarios.
 * Spec: 17-personas-and-use-cases.md (P6-S1 through P6-S7)
 * Covers: User Management, Fiscal Year Management, Reference Data,
 *         Settings, and Audit Log pages.
 */
import { test, expect } from '@playwright/test';
import { mockApis, usersFixture, ANALYST_ID } from './support/api-mock';

// ─── P6-S1: User Management ───────────────────────────────────────────────────

test.describe('Admin — User Management (P6-S1)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/admin/users');
  });

  test('lists all users with role and status', async ({ page }) => {
    await expect(page.getByText('User Management')).toBeVisible();
    // Use cell-scoped locators to avoid ambiguity with nav/badge elements
    await expect(page.getByRole('cell', { name: 'admin', exact: true })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'alice', exact: true })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Active' }).first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-users-list.png', fullPage: true });
  });

  test('opens new user drawer with form fields', async ({ page }) => {
    await page.getByRole('button', { name: '+ New User' }).click();
    // Drawer heading (h3) not the button text
    await expect(page.getByRole('heading', { name: 'New User' })).toBeVisible();
    // FormField renders label+input as siblings — check that inputs are visible
    await expect(page.locator('input').nth(0)).toBeVisible(); // Username
    await expect(page.locator('input').nth(1)).toBeVisible(); // Display Name
    await page.screenshot({ path: 'e2e/screenshots/admin-users-drawer.png', fullPage: true });
  });

  test('shows validation errors when creating user without required fields', async ({ page }) => {
    await page.getByRole('button', { name: '+ New User' }).click();
    await page.getByRole('button', { name: 'Create User' }).click();
    await expect(page.getByText('Username is required')).toBeVisible();
    await expect(page.getByText('Password is required')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-users-validation.png', fullPage: true });
  });

  test('creates new user and closes drawer', async ({ page }) => {
    await page.getByRole('button', { name: '+ New User' }).click();
    // Scope inputs to the drawer body to avoid the time machine date input in the layout
    const drawerBody = page.locator('[class*="body"]').last();
    const inputs = drawerBody.locator('input:not([type="password"])');
    await inputs.nth(0).fill('newuser');          // Username
    await inputs.nth(1).fill('New User');          // Display Name
    await inputs.nth(2).fill('new@ledger.local');  // Email
    await drawerBody.locator('input[type="password"]').fill('secret123');
    await page.getByRole('button', { name: 'Create User' }).click();
    // Drawer should close on success (heading disappears)
    await expect(page.getByRole('heading', { name: 'New User' })).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-users-created.png', fullPage: true });
  });

  test('shows deactivate button for active users', async ({ page }) => {
    const deactivateButtons = page.getByRole('button', { name: 'Deactivate' });
    await expect(deactivateButtons.first()).toBeVisible();
  });

  test('deactivating a user shows Reactivate button for that user (P6-S1)', async ({ page }) => {
    // After deactivation the page re-fetches; override GET to return alice as inactive
    let deactivated = false;
    await page.route('**/api/v1/admin/users', r => {
      if (deactivated) {
        return r.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([usersFixture[0], { ...usersFixture[1], active: false }]),
        });
      }
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(usersFixture) });
    });

    // Click Deactivate for alice (second row)
    await page.getByRole('row').filter({ hasText: 'alice' }).getByRole('button', { name: 'Deactivate' }).click();
    deactivated = true;

    // After re-fetch alice shows Reactivate
    await expect(page.getByRole('button', { name: 'Reactivate' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-user-deactivated.png', fullPage: true });
  });

  test('reactivating a user shows Deactivate button for that user (P6-S1)', async ({ page }) => {
    // Start with alice already inactive
    await page.route('**/api/v1/admin/users', r =>
      r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([usersFixture[0], { ...usersFixture[1], active: false }]),
      })
    );
    await page.reload();

    let reactivated = false;
    await page.route('**/api/v1/admin/users', r => {
      if (reactivated) {
        return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(usersFixture) });
      }
      return r.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([usersFixture[0], { ...usersFixture[1], active: false }]),
      });
    });

    await page.getByRole('row').filter({ hasText: 'alice' }).getByRole('button', { name: 'Reactivate' }).click();
    reactivated = true;

    await expect(page.getByRole('button', { name: 'Deactivate' }).nth(1)).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-user-reactivated.png', fullPage: true });
  });
});

// ─── P6-S2: Fiscal Year Management ───────────────────────────────────────────

test.describe('Admin — Fiscal Year Management (P6-S2)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/admin/fiscal-years');
  });

  test('lists existing fiscal years', async ({ page }) => {
    await expect(page.getByText('Fiscal Year Management')).toBeVisible();
    await expect(page.getByRole('cell', { name: 'FY25' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'FY26' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'FY27' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-fiscal-years-list.png', fullPage: true });
  });

  test('shows new fiscal year form on button click', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Fiscal Year' }).click();
    await expect(page.getByPlaceholder('e.g. FY28')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cancel' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-fiscal-years-form.png', fullPage: true });
  });

  test('creates new fiscal year and dismisses form', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Fiscal Year' }).click();
    await page.getByPlaceholder('e.g. FY28').fill('FY28');
    await page.getByRole('button', { name: 'Create' }).click();
    // Form should close after successful creation
    await expect(page.getByPlaceholder('e.g. FY28')).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-fiscal-years-created.png', fullPage: true });
  });

  test('can cancel creating fiscal year', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Fiscal Year' }).click();
    await page.getByPlaceholder('e.g. FY28').fill('FY28');
    await page.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.getByPlaceholder('e.g. FY28')).not.toBeVisible();
  });
});

// ─── P6-S3/S4: Reference Data Management ─────────────────────────────────────

test.describe('Admin — Reference Data (P6-S3, P6-S4)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/admin/reference-data');
  });

  test('shows all four tabs', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Funding Sources' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Contract Statuses' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Project Statuses' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Reconciliation Categories' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-reference-data-tabs.png', fullPage: true });
  });

  test('shows funding sources list on default tab', async ({ page }) => {
    await expect(page.getByRole('cell', { name: 'OPEX' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Operating Expenditure' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'CAPEX' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-reference-data-list.png', fullPage: true });
  });

  test('switches tabs and loads different data', async ({ page }) => {
    await page.getByRole('button', { name: 'Contract Statuses' }).click();
    // Contract Status specific codes
    await expect(page.getByRole('cell', { name: 'ACTIVE', exact: true })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'INACTIVE', exact: true })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-reference-data-contract-status.png', fullPage: true });
  });

  test('opens add form on + Add click', async ({ page }) => {
    await page.getByRole('button', { name: '+ Add' }).click();
    await expect(page.getByPlaceholder('CODE')).toBeVisible();
    await expect(page.getByPlaceholder('Display Name')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-reference-data-add-form.png', fullPage: true });
  });

  test('adds new reference data entry', async ({ page }) => {
    await page.getByRole('button', { name: '+ Add' }).click();
    await page.getByPlaceholder('CODE').fill('GRANT');
    await page.getByPlaceholder('Display Name').fill('Grant Funding');
    await page.getByRole('button', { name: 'Save' }).click();
    // Form closes on success
    await expect(page.getByPlaceholder('CODE')).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-reference-data-added.png', fullPage: true });
  });

  test('shows deactivate button for active entries', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Deactivate' }).first()).toBeVisible();
  });

  test('shows reactivate button for inactive entries', async ({ page }) => {
    // CAPEX is inactive in fixture
    await expect(page.getByRole('button', { name: 'Reactivate' }).first()).toBeVisible();
  });
});

// ─── P6-S5: Settings (data-driven) ───────────────────────────────────────────
// Main coverage in settings.spec.ts; this confirms admin can see all groups

test.describe('Admin — Settings page admin view (P6-S5)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/settings');
  });

  test('shows all config groups and display names', async ({ page }) => {
    await expect(page.getByText('RECONCILIATION TOLERANCE')).toBeVisible();
    await expect(page.getByText('ACCRUAL AGING')).toBeVisible();
    await expect(page.getByText('Tolerance (%)')).toBeVisible();
    await expect(page.getByText('Tolerance ($)')).toBeVisible();
    await expect(page.getByText('Warning Threshold (days)')).toBeVisible();
    await expect(page.getByText('Critical Threshold (days)')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-settings-groups.png', fullPage: true });
  });
});

// ─── P6-S6/S7: Audit Log Viewer ───────────────────────────────────────────────

test.describe('Admin — Audit Log (P6-S6, P6-S7)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/admin/audit');
  });

  test('shows audit log table with entries', async ({ page }) => {
    // Use heading role to avoid matching nav link 'Audit Log'
    await expect(page.getByRole('heading', { name: 'Audit Log' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'CONTRACT' }).first()).toBeVisible();
    await expect(page.getByRole('cell', { name: 'admin' }).first()).toBeVisible();
    await expect(page.getByRole('cell', { name: 'alice' }).first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-audit-log.png', fullPage: true });
  });

  test('shows filter controls', async ({ page }) => {
    // Check select elements exist (option text inside select is not "visible" in CSS terms)
    await expect(page.locator('select').first()).toBeVisible();
    await expect(page.getByPlaceholder('Entity ID')).toBeVisible();
    await expect(page.getByPlaceholder('User')).toBeVisible();
  });

  test('can filter by entity type', async ({ page }) => {
    // Select the first dropdown (entity type filter)
    await page.locator('select').first().selectOption('CONTRACT');
    // After filtering, CONTRACT entries remain visible in the table
    await expect(page.getByRole('cell', { name: 'CONTRACT' }).first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-audit-log-filtered.png', fullPage: true });
  });

  test('can filter by user', async ({ page }) => {
    await page.getByPlaceholder('User').fill('alice');
    await expect(page.getByRole('cell', { name: 'alice' }).first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-audit-log-user-filter.png', fullPage: true });
  });

  test('shows Export CSV link', async ({ page }) => {
    const exportLink = page.getByRole('link', { name: 'Export CSV' });
    await expect(exportLink).toBeVisible();
    await expect(exportLink).toHaveAttribute('download', 'audit-log.csv');
    await page.screenshot({ path: 'e2e/screenshots/admin-audit-log-export.png', fullPage: true });
  });

  test('expand changes button shows change details', async ({ page }) => {
    // The second audit entry has changes (status update) — shows 'N field(s)' button
    const expandBtn = page.getByRole('button', { name: /field/ });
    await expect(expandBtn).toBeVisible();
    await expandBtn.click();
    await expect(page.getByRole('columnheader', { name: 'Before' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'After' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/admin-audit-log-changes.png', fullPage: true });
  });
});
