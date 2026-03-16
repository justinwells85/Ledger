/**
 * E2E tests for contract, project, and milestone write flows.
 * Spec: 17-personas-and-use-cases.md
 * Covers: P1-S1 (create contract), P1-S2 (add project), P1-S3/P5-S2 (create milestone),
 *         P1-S4/P5-S3 (adjust milestone), P5-S4 (cancel milestone),
 *         plus read scenarios P1-S7, P5-S1.
 */
import { test, expect } from '@playwright/test';
import { mockApis, CONTRACT_ID, PROJECT_ID, MILESTONE_ID, NEW_CONTRACT_ID } from './support/api-mock';

// ─── P1-S7, P5-S1: Read flows (existing coverage) ────────────────────────────

test.describe('Contract Detail', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto(`/contracts/${CONTRACT_ID}`);
  });

  test('shows contract information', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Globant ADM' })).toBeVisible();
    await expect(page.getByText('Globant').first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/contract-detail.png', fullPage: true });
  });

  test('shows projects list', async ({ page }) => {
    await expect(page.getByText('DPI Photopass').first()).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/contract-projects.png', fullPage: true });
  });
});

test.describe('Project Detail', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto(`/projects/${PROJECT_ID}`);
  });

  test('shows project information and milestones', async ({ page }) => {
    await expect(page.getByText('DPI Photopass').first()).toBeVisible();
    await expect(page.getByText('January Sustainment')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/project-detail.png', fullPage: true });
  });
});

// ─── P1-S1: Create contract ───────────────────────────────────────────────────

test.describe('Create Contract (P1-S1)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/');
  });

  test('+ New Contract button opens drawer', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Contract' }).click();
    await expect(page.getByRole('heading', { name: 'New Contract' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create Contract' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/create-contract-drawer.png', fullPage: true });
  });

  test('shows validation error when name is empty', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Contract' }).click();
    await page.getByRole('button', { name: 'Create Contract' }).click();
    await expect(page.getByText('Name is required')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/create-contract-validation.png', fullPage: true });
  });

  test('shows validation error when vendor is empty', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Contract' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    await drawerBody.locator('input:not([type="date"])').nth(0).fill('My Contract');
    await page.getByRole('button', { name: 'Create Contract' }).click();
    await expect(page.getByText('Vendor is required')).toBeVisible();
  });

  test('shows validation error when start date is missing', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Contract' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    const textInputs = drawerBody.locator('input:not([type="date"])');
    await textInputs.nth(0).fill('My Contract');
    await textInputs.nth(1).fill('My Vendor');
    await textInputs.nth(2).fill('Owner');
    // no start date filled
    await page.getByRole('button', { name: 'Create Contract' }).click();
    await expect(page.getByText('Start date is required')).toBeVisible();
  });

  test('creates contract and navigates to contract detail (P1-S1 fix)', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Contract' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    const textInputs = drawerBody.locator('input:not([type="date"])');
    await textInputs.nth(0).fill('New Corp Contract');   // Name
    await textInputs.nth(1).fill('New Corp Vendor');     // Vendor
    await textInputs.nth(2).fill('Finance Manager');     // Owner
    await drawerBody.locator('input[type="date"]').fill('2026-01-01');
    await page.getByRole('button', { name: 'Create Contract' }).click();
    // After creation, app navigates to the new contract's detail page
    await expect(page).toHaveURL(`/contracts/${NEW_CONTRACT_ID}`);
    await page.screenshot({ path: 'e2e/screenshots/create-contract-navigated.png', fullPage: true });
  });
});

// ─── P1-S2: Add project to contract ──────────────────────────────────────────

test.describe('Add Project to Contract (P1-S2)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto(`/contracts/${CONTRACT_ID}`);
  });

  test('+ Add Project button opens drawer', async ({ page }) => {
    await page.getByRole('button', { name: '+ Add Project' }).click();
    await expect(page.getByRole('heading', { name: 'Add Project' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/add-project-drawer.png', fullPage: true });
  });

  test('shows validation error when required fields missing', async ({ page }) => {
    await page.getByRole('button', { name: '+ Add Project' }).click();
    await page.getByRole('button', { name: 'Add Project', exact: true }).click();
    await expect(page.getByText('Project ID, name and WBSE are required')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/add-project-validation.png', fullPage: true });
  });

  test('adds project and new project appears in list', async ({ page }) => {
    // Stateful mock: after POST, GET returns the new project too
    const updatedProjects = [
      { projectId: PROJECT_ID, name: 'DPI Photopass', wbse: '1174905.SU.ES', fundingSource: 'OPEX', status: 'ACTIVE' },
      { projectId: 'PR99001', name: 'New Deliverable', wbse: '1174906.SU.ES', fundingSource: 'OPEX', status: 'ACTIVE' },
    ];
    let posted = false;
    await page.route('**/api/v1/contracts/*/projects', r => {
      if (r.request().method() === 'POST') {
        posted = true;
        return r.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({}) });
      }
      const list = posted ? updatedProjects : [updatedProjects[0]];
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(list) });
    });

    await page.getByRole('button', { name: '+ Add Project' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    const inputs = drawerBody.locator('input:not([type="date"])');
    await inputs.nth(0).fill('PR99001');             // Project ID
    await inputs.nth(1).fill('New Deliverable');     // Name
    await inputs.nth(2).fill('1174906.SU.ES');       // WBSE
    await page.getByRole('button', { name: 'Add Project', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Add Project' })).not.toBeVisible();
    await expect(page.getByText('New Deliverable')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/add-project-success.png', fullPage: true });
  });
});

// ─── P1-S2 continued: Edit contract ──────────────────────────────────────────

test.describe('Edit Contract', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto(`/contracts/${CONTRACT_ID}`);
  });

  test('Edit button opens edit drawer', async ({ page }) => {
    await page.getByRole('button', { name: 'Edit' }).click();
    await expect(page.getByRole('heading', { name: 'Edit Contract' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/edit-contract-drawer.png', fullPage: true });
  });

  test('requires reason to save', async ({ page }) => {
    await page.getByRole('button', { name: 'Edit' }).click();
    await page.getByRole('button', { name: 'Save Changes' }).click();
    await expect(page.getByText('Reason is required')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/edit-contract-validation.png', fullPage: true });
  });

  test('saves edit with reason and closes drawer', async ({ page }) => {
    await page.getByRole('button', { name: 'Edit' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    // Reason is the last text input in the edit form
    await drawerBody.locator('input:not([type="date"])').last().fill('Correcting owner name');
    await page.getByRole('button', { name: 'Save Changes' }).click();
    await expect(page.getByRole('heading', { name: 'Edit Contract' })).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/edit-contract-success.png', fullPage: true });
  });

  test('can close a contract by setting status to CLOSED (P1-S12)', async ({ page }) => {
    await page.getByRole('button', { name: 'Edit' }).click();
    const drawerBody = page.locator('[class*="body"]').last();
    await drawerBody.locator('select').selectOption('CLOSED');
    await drawerBody.locator('input:not([type="date"])').last().fill('Contract period ended');
    await page.getByRole('button', { name: 'Save Changes' }).click();
    await expect(page.getByRole('heading', { name: 'Edit Contract' })).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/contract-closed.png', fullPage: true });
  });
});

// ─── P1-S3 / P5-S2: Create milestone ─────────────────────────────────────────

test.describe('Create Milestone (P1-S3, P5-S2)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto(`/projects/${PROJECT_ID}`);
  });

  test('+ Add Milestone button opens drawer', async ({ page }) => {
    await page.getByRole('button', { name: '+ Add Milestone' }).click();
    await expect(page.getByRole('heading', { name: 'Add Milestone' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/add-milestone-drawer.png', fullPage: true });
  });

  test('shows fiscal period options in dropdown', async ({ page }) => {
    await page.getByRole('button', { name: '+ Add Milestone' }).click();
    const drawer = page.locator('[class*="body"]').last();
    const periodSelect = drawer.locator('select');
    await expect(periodSelect).toBeVisible();
    await expect(periodSelect.locator('option', { hasText: 'FY26-04-JAN' })).toBeAttached();
    await page.screenshot({ path: 'e2e/screenshots/add-milestone-periods.png', fullPage: true });
  });

  test('validates required fields', async ({ page }) => {
    await page.getByRole('button', { name: '+ Add Milestone' }).click();
    await page.getByRole('button', { name: 'Add Milestone', exact: true }).click();
    await expect(page.getByText('Name, amount and fiscal period are required')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/add-milestone-validation.png', fullPage: true });
  });

  test('creates milestone and new milestone appears in list', async ({ page }) => {
    // Stateful mock: after POST, GET returns the new milestone too
    const newMilestone = {
      milestoneId: 'ms-new',
      name: 'Q3 Sustainment',
      currentVersion: { versionNumber: 1, plannedAmount: 30000, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2026-03-15' },
    };
    let posted = false;
    await page.route('**/api/v1/projects/*/milestones', r => {
      if (r.request().method() === 'POST') {
        posted = true;
        return r.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({}) });
      }
      const list = posted
        ? [{ milestoneId: MILESTONE_ID, name: 'January Sustainment', currentVersion: { versionNumber: 1, plannedAmount: 25000, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2025-11-01' } }, newMilestone]
        : [{ milestoneId: MILESTONE_ID, name: 'January Sustainment', currentVersion: { versionNumber: 1, plannedAmount: 25000, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2025-11-01' } }];
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(list) });
    });

    await page.getByRole('button', { name: '+ Add Milestone' }).click();
    const drawer = page.locator('[class*="body"]').last();
    await drawer.locator('input:not([type="number"]):not([type="date"])').nth(0).fill('Q3 Sustainment');
    await drawer.locator('input[type="number"]').fill('30000');
    await drawer.locator('select').selectOption('fp1');
    await page.getByRole('button', { name: 'Add Milestone', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Add Milestone' })).not.toBeVisible();
    await expect(page.getByText('Q3 Sustainment')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/add-milestone-success.png', fullPage: true });
  });
});

// ─── P1-S4 / P5-S3: Adjust milestone planned amount ──────────────────────────

test.describe('Adjust Milestone Amount (P1-S4, P5-S3)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto(`/projects/${PROJECT_ID}`);
    // Expand the first milestone to reveal MilestonePanel
    await page.getByRole('row', { name: /January Sustainment/ }).click();
  });

  test('expands milestone to show version history and new version button', async ({ page }) => {
    await expect(page.getByText('VERSION HISTORY')).toBeVisible();
    await expect(page.getByRole('button', { name: '+ New Version' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-panel.png', fullPage: true });
  });

  test('shows reconciliation summary when expanded', async ({ page }) => {
    await expect(page.getByText('RECONCILIATION SUMMARY')).toBeVisible();
    await expect(page.getByText('PARTIALLY_MATCHED')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-reconciliation-summary.png', fullPage: true });
  });

  test('+ New Version opens version form', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Version' }).click();
    await expect(page.getByRole('button', { name: 'Save Version' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-new-version-form.png', fullPage: true });
  });

  test('validates amount and reason required for new version', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Version' }).click();
    await page.getByRole('button', { name: 'Save Version' }).click();
    await expect(page.getByText('Amount and reason are required')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-version-validation.png', fullPage: true });
  });

  test('saves new version and form collapses', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Version' }).click();
    // Fill amount and reason (both required)
    await page.locator('[class*="expandSection"]').last()
      .locator('input[type="number"]').fill('28000');
    await page.locator('[class*="expandSection"]').last()
      .locator('input:not([type="number"]):not([type="date"])').last().fill('Scope reduction approved');
    await page.getByRole('button', { name: 'Save Version' }).click();
    await expect(page.getByRole('button', { name: 'Save Version' })).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-version-saved.png', fullPage: true });
  });

  test('shows remaining budget in reconciliation summary (P5-S7)', async ({ page }) => {
    await expect(page.getByText('Remaining')).toBeVisible();
    // reconciliationStatusFixture.remaining = 10000
    await expect(page.getByText('$10,000.00')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-remaining-budget.png', fullPage: true });
  });
});

// ─── P5-S4: Cancel milestone ──────────────────────────────────────────────────

test.describe('Cancel Milestone (P5-S4)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto(`/projects/${PROJECT_ID}`);
    await page.getByRole('row', { name: /January Sustainment/ }).click();
  });

  test('shows Cancel Milestone button when expanded', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Cancel Milestone' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-cancel-button.png', fullPage: true });
  });

  test('Cancel Milestone shows confirmation form', async ({ page }) => {
    await page.getByRole('button', { name: 'Cancel Milestone' }).click();
    await expect(page.getByRole('button', { name: 'Confirm Cancel' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-cancel-form.png', fullPage: true });
  });

  test('validates reason required to cancel', async ({ page }) => {
    await page.getByRole('button', { name: 'Cancel Milestone' }).click();
    await page.getByRole('button', { name: 'Confirm Cancel' }).click();
    await expect(page.getByText('Reason is required')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-cancel-validation.png', fullPage: true });
  });

  test('cancels milestone with reason and collapses form', async ({ page }) => {
    await page.getByRole('button', { name: 'Cancel Milestone' }).click();
    // Fill reason (effective date is pre-populated with today)
    await page.locator('[class*="expandSection"]').first()
      .locator('input:not([type="date"])').last().fill('Project cancelled by PM');
    await page.getByRole('button', { name: 'Confirm Cancel' }).click();
    await expect(page.getByRole('button', { name: 'Confirm Cancel' })).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/milestone-cancel-success.png', fullPage: true });
  });
});

// ─── P5-S9: Audit trail from contract ────────────────────────────────────────

test.describe('Contract Audit Trail (P5-S9)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto(`/contracts/${CONTRACT_ID}`);
  });

  test('Audit Trail button is visible on contract detail', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Audit Trail' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/contract-audit-trail-button.png', fullPage: true });
  });

  test('clicking Audit Trail navigates to audit log filtered for this contract', async ({ page }) => {
    await page.getByRole('button', { name: 'Audit Trail' }).click();
    await expect(page).toHaveURL(/\/admin\/audit/);
    await expect(page).toHaveURL(new RegExp(`entityType=CONTRACT`));
    await expect(page).toHaveURL(new RegExp(`entityId=${CONTRACT_ID}`));
    await page.screenshot({ path: 'e2e/screenshots/contract-audit-trail-filtered.png', fullPage: true });
  });

  test('audit log pre-populates entity type filter as CONTRACT', async ({ page }) => {
    await page.getByRole('button', { name: 'Audit Trail' }).click();
    await expect(page.locator('select').first()).toHaveValue('CONTRACT');
    await page.screenshot({ path: 'e2e/screenshots/contract-audit-trail-prefilled.png', fullPage: true });
  });
});
