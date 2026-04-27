import { defineConfig } from "vitest/config";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { storybookTest } from "@storybook/addon-vitest/vitest-plugin";
import { playwright } from "@vitest/browser-playwright";

const here = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  test: {
    // Auto-restore vi.spyOn / fn() mocks after every test so stories don't
    // leak mock state into each other. Pairs with the `spyOn` migration of
    // every navigator.clipboard mock — see story files under
    // src/main/resources/static/components/cts-*.stories.js.
    restoreMocks: true,
    projects: [
      {
        plugins: [
          storybookTest({
            configDir: join(here, ".storybook"),
          }),
        ],
        test: {
          name: "storybook",
          browser: {
            enabled: true,
            // Grant clipboard permissions to the browser context so the
            // production code path (navigator.clipboard.writeText) actually
            // runs in headless Chromium. Without this the API throws
            // NotAllowedError and stories must redefine navigator.clipboard,
            // which is brittle and easy to typo. With the permission granted,
            // stories can use vi.spyOn to assert calls without touching the
            // global. See frontend/AGENTS.md (testing) for the reasoning.
            provider: playwright({
              contextOptions: {
                permissions: ["clipboard-read", "clipboard-write"],
              },
            }),
            headless: true,
            instances: [{ browser: "chromium" }],
          },
        },
      },
      {
        test: {
          name: "unit",
          environment: "node",
          include: [join(here, "../src/main/resources/static/components/**/*.test.js")],
        },
      },
    ],
  },
});
