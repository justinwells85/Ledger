import { defineConfig, devices } from '@playwright/test';

const isCI = !!process.env.CI;
const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000';

export default defineConfig({
  testDir: './e2e',
  testIgnore: ['**/e2e/tier2/**'],
  fullyParallel: false,
  retries: isCI ? 1 : 0,
  workers: 1,
  reporter: [
    ['html', { outputFolder: 'e2e/report', open: 'never' }],
    ['line'],
  ],
  use: {
    baseURL,
    screenshot: 'on',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
  },
  outputDir: 'e2e/test-results',
  ...(!isCI && {
    webServer: {
      command: 'npm run dev',
      url: 'http://localhost:3000',
      reuseExistingServer: true,
      timeout: 30_000,
    },
  }),
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
