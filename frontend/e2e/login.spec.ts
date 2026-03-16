/**
 * E2E tests for authentication flows.
 * Covers: login form rendering, successful login (P1/P5/P6 entry point),
 *         invalid credentials error, and logout.
 */
import { test, expect } from '@playwright/test';
import { mockApis } from './support/api-mock';

// ─── Unauthenticated: login page renders ──────────────────────────────────────

test.describe('Login Page (unauthenticated)', () => {
  test.beforeEach(async ({ page }) => {
    // Set up API routes but do NOT inject a token — app must show LoginPage.
    await mockApis(page, { injectAuth: false });
    await page.goto('/');
  });

  test('shows Ledger branding and sign-in form', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Ledger' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/login-page.png', fullPage: true });
  });

  test('shows username and password inputs', async ({ page }) => {
    await expect(page.locator('input[autocomplete="username"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
  });

  test('shows error for invalid credentials', async ({ page }) => {
    // Override the login mock to reject
    await page.route('**/api/v1/auth/login', r =>
      r.fulfill({ status: 401, contentType: 'application/json', body: '{}' })
    );
    await page.locator('input[autocomplete="username"]').fill('baduser');
    await page.locator('input[type="password"]').fill('wrongpass');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByText('Invalid username or password')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/login-error.png', fullPage: true });
  });

  test('successful login navigates to dashboard', async ({ page }) => {
    await page.locator('input[autocomplete="username"]').fill('admin');
    await page.locator('input[type="password"]').fill('secret');
    await page.getByRole('button', { name: 'Sign in' }).click();
    // After login, app renders the authenticated layout with dashboard
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/login-success.png', fullPage: true });
  });
});

// ─── Authenticated: logout ─────────────────────────────────────────────────────

test.describe('Logout', () => {
  test.beforeEach(async ({ page }) => {
    await mockApis(page);
    await page.goto('/');
  });

  test('Sign Out button returns to login page', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await page.getByRole('button', { name: 'Sign Out' }).click();
    await expect(page.getByRole('heading', { name: 'Ledger' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/logout.png', fullPage: true });
  });

});
