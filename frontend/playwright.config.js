import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? "dot" : "list",

  use: {
    baseURL: "http://localhost:9876",
    trace: "on-first-retry",
  },

  // Project- and platform-agnostic snapshot file names so locally generated
  // snapshots match what CI produces. Single project (chromium), single
  // tested rendering surface — the disambiguator suffixes add noise without
  // value and would split locally-generated baselines from CI's.
  snapshotPathTemplate: "{snapshotDir}/{testFileDir}/{testFileName}-snapshots/{arg}{ext}",

  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],

  webServer: {
    command: "./node_modules/.bin/http-server ../src/main/resources/static -a 127.0.0.1 -p 9876 -s",
    port: 9876,
    reuseExistingServer: !process.env.CI,
  },
});
