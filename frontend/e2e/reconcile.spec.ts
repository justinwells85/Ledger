import { test, expect } from '@playwright/test';
import { mockApis, ACTUAL_ID, MILESTONE_ID } from './support/api-mock';

test.describe('Reconcile Workspace', () => {
  test.describe('Actuals panel', () => {
    test.beforeEach(async ({ page }) => {
      await mockApis(page);
      await page.goto('/reconcile');
    });

    test('shows unreconciled actuals in left panel', async ({ page }) => {
      await expect(page.getByText('UNRECONCILED ACTUALS')).toBeVisible();
      await expect(page.getByText('Invoice March')).toBeVisible();
      await expect(page.getByText('$25,000.00')).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/reconcile-actuals.png', fullPage: true });
    });

    test('shows both actuals with amounts', async ({ page }) => {
      await expect(page.getByText('Reversal')).toBeVisible();
      await expect(page.getByText('($18,000.00)')).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/reconcile-both-actuals.png', fullPage: true });
    });

    test('selecting actual loads candidate milestones', async ({ page }) => {
      await page.getByText('Invoice March').click();
      await expect(page.getByText('January Sustainment')).toBeVisible();
      await expect(page.getByText('Other Milestone')).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/reconcile-candidates.png', fullPage: true });
    });

    test('candidate shows relevance score', async ({ page }) => {
      await page.getByText('Invoice March').click();
      await expect(page.getByText('January Sustainment')).toBeVisible();
      await expect(page.getByText('★10')).toBeVisible();
      await page.screenshot({ path: 'e2e/screenshots/reconcile-score.png', fullPage: true });
    });
  });

  test.describe('Reconcile form', () => {
    test.beforeEach(async ({ page }) => {
      await mockApis(page);
      await page.goto('/reconcile');
      await page.getByText('Invoice March').click();
      await expect(page.getByText('January Sustainment')).toBeVisible();
    });

    test('selecting candidate shows reconcile form', async ({ page }) => {
      await page.getByText('January Sustainment').click();
      await expect(page.getByText(/Reconcile to:/)).toBeVisible();
      await expect(page.getByRole('radio', { name: 'INVOICE', exact: true })).toBeChecked();
      await page.screenshot({ path: 'e2e/screenshots/reconcile-form.png', fullPage: true });
    });

    test('reconcile form has category options', async ({ page }) => {
      await page.getByText('January Sustainment').click();
      await expect(page.getByRole('radio', { name: 'INVOICE', exact: true })).toBeVisible();
      await expect(page.getByRole('radio', { name: 'ACCRUAL', exact: true })).toBeVisible();
      await expect(page.getByRole('radio', { name: 'ACCRUAL REVERSAL' })).toBeVisible();
      await expect(page.getByRole('radio', { name: 'ALLOCATION', exact: true })).toBeVisible();
    });

    test('can change category to ACCRUAL', async ({ page }) => {
      await page.getByText('January Sustainment').click();
      await page.getByRole('radio', { name: 'ACCRUAL', exact: true }).click();
      await expect(page.getByRole('radio', { name: 'ACCRUAL', exact: true })).toBeChecked();
      await page.screenshot({ path: 'e2e/screenshots/reconcile-category.png', fullPage: true });
    });

    test('submit reconcile clears selection', async ({ page }) => {
      await page.getByText('January Sustainment').click();
      // Use placeholder to distinguish notes textarea from the time machine date input
      await page.getByPlaceholder('Notes (optional)').fill('Test notes');
      await page.getByRole('button', { name: 'Reconcile' }).click();
      // After window.location.reload(), the page reloads fresh — use first() to handle
      // any brief DOM overlap during the reload transition
      await expect(page.getByText('Invoice March').first()).toBeVisible();
    });
  });
});

// ─── P3-S5: Filter actuals ────────────────────────────────────────────────────

test.describe('Reconcile Workspace — Filter Actuals (P3-S5)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/reconcile');
  });

  test('vendor filter hides non-matching actuals', async ({ page }) => {
    await page.locator('input[placeholder*="vendor"]').fill('xyz-no-match');
    await expect(page.getByText('No actuals match filter')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/reconcile-filter-empty.png', fullPage: true });
  });

  test('vendor filter matching both actuals shows both', async ({ page }) => {
    await page.locator('input[placeholder*="vendor"]').fill('Globant');
    await expect(page.getByText('Invoice March')).toBeVisible();
    await expect(page.getByText('Reversal')).toBeVisible();
  });

  test('positive amount filter hides negative actuals', async ({ page }) => {
    await page.getByRole('combobox', { name: 'Amount filter' }).selectOption('positive');
    await expect(page.getByText('Invoice March')).toBeVisible();
    await expect(page.getByText('Reversal')).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/reconcile-filter-positive.png', fullPage: true });
  });

  test('negative amount filter hides positive actuals', async ({ page }) => {
    await page.getByRole('combobox', { name: 'Amount filter' }).selectOption('negative');
    await expect(page.getByText('Reversal')).toBeVisible();
    await expect(page.getByText('Invoice March')).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/reconcile-filter-negative.png', fullPage: true });
  });
});

// ─── P3-S4: Undo reconciliation ───────────────────────────────────────────────

test.describe('Reconcile Workspace — Undo Reconciliation (P3-S4)', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/reconcile');
    // Perform a reconciliation first so the undo section appears
    await page.getByText('Invoice March').click();
    await page.getByText('January Sustainment').click();
    await page.getByRole('button', { name: 'Reconcile' }).click();
  });

  test('recently reconciled section appears after submitting', async ({ page }) => {
    await expect(page.getByText('RECENTLY RECONCILED')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Undo' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/reconcile-recently-reconciled.png', fullPage: true });
  });

  test('Undo button opens reason form', async ({ page }) => {
    await page.getByRole('button', { name: 'Undo' }).click();
    await expect(page.getByPlaceholder('Reason for undo (required)')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Confirm Undo' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/reconcile-undo-form.png', fullPage: true });
  });

  test('Confirm Undo is disabled when reason is empty', async ({ page }) => {
    await page.getByRole('button', { name: 'Undo' }).click();
    // Button is disabled until a reason is typed
    await expect(page.getByRole('button', { name: 'Confirm Undo' })).toBeDisabled();
  });

  test('Confirm Undo with reason removes item from recently reconciled', async ({ page }) => {
    await page.getByRole('button', { name: 'Undo' }).click();
    await page.getByPlaceholder('Reason for undo (required)').fill('Matched to wrong milestone');
    await page.getByRole('button', { name: 'Confirm Undo' }).click();
    await expect(page.getByText('RECENTLY RECONCILED')).not.toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/reconcile-undo-success.png', fullPage: true });
  });

  test('Cancel undo closes the form', async ({ page }) => {
    await page.getByRole('button', { name: 'Undo' }).click();
    await expect(page.getByPlaceholder('Reason for undo (required)')).toBeVisible();
    await page.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.getByPlaceholder('Reason for undo (required)')).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Undo' })).toBeVisible();
  });
});
