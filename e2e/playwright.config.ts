import { defineConfig, devices } from '@playwright/test';
import { env } from './utils/env';

/**
 * E2E suite for FocusFortress. Expects the full docker-compose stack to be running
 * (frontend, backend, MySQL, MailHog) – see README.md.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  // The backend (bcrypt + MySQL in Docker) is the bottleneck; more workers only cause timeouts.
  workers: process.env.CI ? 2 : 4,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  outputDir: 'test-results',
  use: {
    baseURL: env.baseUrl,
    // The backend serializes timestamps as zone-less UTC LocalDateTime; pin the browser to UTC so
    // results don't depend on the machine's time zone (see the counter time-zone BUG test).
    timezoneId: 'UTC',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10_000,
    navigationTimeout: 20_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
  ],
});
