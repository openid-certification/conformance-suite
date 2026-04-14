import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { defineMain } from "@storybook/web-components-vite/node";

const here = dirname(fileURLToPath(import.meta.url));

export default defineMain({
  stories: ["../../src/main/resources/static/components/**/*.stories.js"],
  framework: "@storybook/web-components-vite",
  addons: ["@storybook/addon-vitest", "@storybook/addon-mcp"],
  staticDirs: [
    { from: "../../src/main/resources/static", to: "/" },
    { from: "../public", to: "/" },
  ],

  // Component source and stories live outside frontend/ (in src/main/resources/static/).
  // Vite can't resolve bare imports from there, so we alias them to frontend/node_modules.
  viteFinal(viteConfig) {
    viteConfig.resolve = viteConfig.resolve || {};
    viteConfig.resolve.dedupe = [
      ...(viteConfig.resolve.dedupe || []),
      "lit",
    ];
    viteConfig.resolve.alias = {
      ...(viteConfig.resolve.alias || {}),
      // Resolve bare "lit" imports from components outside the Vite root
      lit: join(here, "..", "node_modules", "lit"),
      // Fixture helpers/mocks used by stories that live in src/main/resources/static/components/
      "@fixtures": join(here, "..", "stories", "fixtures"),
      // Vite can't follow storybook/test subpath exports from outside the root
      "storybook/test": join(here, "..", "node_modules", "storybook", "dist", "test", "index.js"),
    };
    // Allow Vite dev server to serve files from the repo root (components live
    // in src/main/resources/static/, outside the frontend/ Vite root)
    viteConfig.server = viteConfig.server || {};
    viteConfig.server.fs = viteConfig.server.fs || {};
    viteConfig.server.fs.allow = [
      ...(viteConfig.server.fs.allow || []),
      join(here, "..", ".."),
    ];
    // Pre-bundle storybook/test so Vite resolves the CJS/ESM boundary correctly
    viteConfig.optimizeDeps = viteConfig.optimizeDeps || {};
    viteConfig.optimizeDeps.include = [
      ...(viteConfig.optimizeDeps.include || []),
      "storybook/test",
      "msw/browser",
    ];
    return viteConfig;
  },
});
