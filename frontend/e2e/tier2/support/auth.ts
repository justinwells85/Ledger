/**
 * Tier 2 auth helper — obtains a real JWT from the live backend and injects
 * it into localStorage so the app's AuthProvider passes the auth guard.
 */
import { Page, APIRequestContext } from '@playwright/test';

const LOGIN_URL = 'http://localhost:8080/api/v1/auth/login';

export async function loginAs(page: Page, request: APIRequestContext, username = 'admin', password = 'admin') {
  const resp = await request.post(LOGIN_URL, {
    data: { username, password },
  });
  const body = await resp.json() as { token: string };
  const token = body.token;

  await page.addInitScript((t: string) => {
    localStorage.setItem('ledger_token', t);
  }, token);
}

/** Unique prefix for test data — prevents collisions across parallel/repeated runs */
export function runId(): string {
  return `T2-${Date.now()}`;
}
