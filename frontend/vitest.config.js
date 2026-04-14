import { defineConfig } from "vitest/config";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { storybookTest } from "@storybook/addon-vitest/vitest-plugin";
import { playwright } from "@vitest/browser-playwright";

const here = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  test: {
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
            provider: playwright(),
            headless: true,
            instances: [{ browser: "chromium" }],
          },
        },
      },
    ],
  },
});
