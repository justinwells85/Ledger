/**
 * Tier 2 Playwright configuration — runs against the real Docker Compose stack.
 * Requires: docker compose up (frontend at http://localhost:3000, backend at http://localhost:8080)
 * No webServer directive — expects the stack to already be running.
 */
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e/tier2',
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: [
    ['html', { outputFolder: 'e2e/report-tier2', open: 'never' }],
    ['line'],
  ],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',
    screenshot: 'on',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
  },
  outputDir: 'e2e/test-results-tier2',
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
