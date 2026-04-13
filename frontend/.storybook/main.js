import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));

/** @type {import('@storybook/web-components-vite').StorybookConfig} */
const config = {
  stories: ["../stories/**/*.stories.js"],
  addons: ["@storybook/addon-essentials"],
  framework: {
    name: "@storybook/web-components-vite",
    options: {},
  },
  staticDirs: [{ from: "../../src/main/resources/static", to: "/" }],

  // The component source lives outside frontend/ (in src/main/resources/static/),
  // so Vite can't resolve bare "lit" imports from there. This tells Vite to
  // always resolve lit from frontend/node_modules.
  viteFinal(viteConfig) {
    viteConfig.resolve = viteConfig.resolve || {};
    viteConfig.resolve.dedupe = [
      ...(viteConfig.resolve.dedupe || []),
      "lit",
    ];
    viteConfig.resolve.alias = {
      ...(viteConfig.resolve.alias || {}),
      lit: join(here, "..", "node_modules", "lit"),
    };
    return viteConfig;
  },
};

export default config;
