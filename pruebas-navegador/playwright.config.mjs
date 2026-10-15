import { pathToFileURL } from 'node:url';
const { defineConfig } = await import(process.env.PLAYWRIGHT_TEST_MODULE
  ? pathToFileURL(process.env.PLAYWRIGHT_TEST_MODULE).href : 'playwright/test');
export default defineConfig({
  testDir: '.', testMatch: 'fase-1.spec.mjs', workers: 1, fullyParallel: false,
  retries: 0, timeout: 60_000, expect: { timeout: 8_000 },
  outputDir: process.env.B2_OUTPUT_DIR || '../.tmp/b2-playwright',
  reporter: [['list'], ['json', { outputFile: process.env.B2_REPORT || '../.tmp/b2-playwright.json' }]],
  use: { baseURL: process.env.B2_BASE_URL || 'http://localhost:3000',
    launchOptions: { executablePath: process.env.B2_CHROME_PATH },
    screenshot: 'only-on-failure', trace: 'off', viewport: { width: 1440, height: 1000 } },
});
