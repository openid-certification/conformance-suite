// ESLint 10 flat config for the CTS frontend.
//
// See docs/plans/2026-04-17-002-feat-frontend-lint-format-typecheck-ci-plan.md
// for the rationale behind plugin choices, severity ladder, and R8 overrides.
//
// WHY THIS FILE LIVES AT THE REPO ROOT (not in `frontend/`, as the plan's
// literal wording suggested): ESLint 10 flat config uses the config file's
// own directory as a `basePath`. Files outside that directory cannot be
// linted — even when passed as explicit CLI arguments. Since the frontend
// codebase spans two sibling trees (`frontend/` and
// `src/main/resources/static/components/`), the config has to live at their
// common ancestor. The lint script is still invoked from `frontend/` via
// `npm run lint` and explicitly scopes the targets it walks; nothing else
// in the repo (Java, Python, shell, etc.) is lintable because the global
// `ignores` block and the `files:` globs keep the scope frontend-only.

// Resolve plugins from frontend/node_modules. Using createRequire against a
// path *inside* `frontend/` makes Node walk its own module-resolution tree
// starting there, so `require("eslint-plugin-lit")` finds the plugin at
// `frontend/node_modules/eslint-plugin-lit/`. This keeps the package.json
// at `frontend/package.json` (the natural home for frontend tooling) while
// satisfying ESLint's base-path constraint that the config file live at a
// common ancestor of every lintable file.
import { createRequire } from "node:module";
const requireFromFrontend = createRequire(
  new URL("./frontend/package.json", import.meta.url),
);

const js = requireFromFrontend("@eslint/js");
const globals = requireFromFrontend("globals");
const litPlugin = requireFromFrontend("eslint-plugin-lit");
const wcPlugin = requireFromFrontend("eslint-plugin-wc");
const jsdocPlugin = requireFromFrontend("eslint-plugin-jsdoc");
const storybookPlugin = requireFromFrontend("eslint-plugin-storybook");
const playwrightPlugin = requireFromFrontend("eslint-plugin-playwright");
const prettierConfig = requireFromFrontend("eslint-config-prettier/flat");

const litRuleNames = Object.keys(litPlugin.rules || {});
const wcRuleNames = Object.keys(wcPlugin.rules || {});

const disableAllLitRules = Object.fromEntries(
  litRuleNames.map((r) => [`lit/${r}`, "off"]),
);
const disableAllWcRules = Object.fromEntries(
  wcRuleNames.map((r) => [`wc/${r}`, "off"]),
);

export default [
  // ---------------------------------------------------------------------------
  // 1. Global ignores — mirror `frontend/.prettierignore` (R1a), plus every
  //    non-frontend tree under the repo root (this config lives at root so
  //    we explicitly gate non-frontend files out).
  // ---------------------------------------------------------------------------
  {
    ignores: [
      // Non-frontend repo trees (Java, shell, Python, docs, etc.)
      "src/main/java/**",
      "src/main/python/**",
      "src/main/resources/**/*.html",
      "src/main/resources/**/*.css",
      "src/main/resources/static/vendor/**",
      "src/main/resources/static/js/**",
      "src/test/**",
      "target/**",
      "scripts/**",
      ".gitlab-ci/**",
      "docs/**",
      "images/**",

      // Frontend-local excludes (mirror .prettierignore)
      "frontend/node_modules/**",
      "frontend/test-results/**",
      "frontend/playwright-report/**",
      "frontend/storybook-static/**",
      "frontend/.vite/**",
      "frontend/public/mockServiceWorker.js",
      "**/*.min.js",
    ],
  },

  // ---------------------------------------------------------------------------
  // 2. Universal JS base
  // ---------------------------------------------------------------------------
  js.configs.recommended,
  {
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.es2024,
      },
    },
  },

  // ---------------------------------------------------------------------------
  // 3. Component files — cts-*.js under components/ (NOT stories, NOT helpers)
  //    gets lit + wc + jsdoc (R6). The plan's plugin matrix says stories get
  //    storybook only, not lit/wc; narrowing the files/ignores here achieves
  //    that without a follow-up "disable lit on stories" block.
  // ---------------------------------------------------------------------------
  {
    files: ["src/main/resources/static/components/**/cts-*.js"],
    ignores: [
      "src/main/resources/static/components/**/*.stories.js",
      "src/main/resources/static/components/**/_*.js",
    ],
    plugins: {
      lit: litPlugin,
      wc: wcPlugin,
      jsdoc: jsdocPlugin,
    },
    rules: {
      ...litPlugin.configs["flat/recommended"].rules,
      ...wcPlugin.configs["flat/recommended"].rules,
      ...jsdocPlugin.configs["flat/recommended"].rules,

      // R8 — Light-DOM overrides.
      // cts-* components render to their own light DOM via
      // `createRenderRoot() { return this; }`. Rules that assume shadow DOM
      // or penalize idiomatic light-DOM patterns are downgraded.
      "wc/attach-shadow-constructor": "off",
      "lit/no-template-map": "warn",
      "lit/no-template-arrow": "warn",
      "lit/no-useless-template-literals": "warn",

      // R9 — High-catch-rate rules kept explicitly at error, even when the
      // preset already sets them. This is documentation: these are the rules
      // most likely to catch agent-produced bugs (wrong binding syntax,
      // class-field shadowing, typoed element names).
      "lit/binding-positions": "error",
      "lit/no-duplicate-template-bindings": "error",
      "lit/no-invalid-html": "error",
      "lit/no-legacy-template-syntax": "error",
      "lit/no-classfield-shadowing": "error",
      "lit/lifecycle-super": "error",
      "wc/no-constructor-attributes": "error",
      "wc/no-invalid-element-name": "error",
      "wc/no-typos": "error",
    },
  },

  // ---------------------------------------------------------------------------
  // 4. Helper files (R8a) — structural exemption.
  //    `_*.js` files (e.g., `_button-classes.js`) are imported by components
  //    but are not custom-element definitions. Disable all wc/*, lit/*, and
  //    jsdoc-property rules. This is a convention, NOT a legacy override:
  //    new `_*.js` helpers are welcome and get the same treatment.
  // ---------------------------------------------------------------------------
  {
    files: ["**/_*.js"],
    rules: {
      ...disableAllLitRules,
      ...disableAllWcRules,
      "jsdoc/require-property": "off",
    },
  },

  // ---------------------------------------------------------------------------
  // 5. Storybook story files and `.storybook/` config — storybook/flat/recommended
  //    Disables jsdoc/require-property on stories (they describe demos, not
  //    components with @property blocks).
  // ---------------------------------------------------------------------------
  ...storybookPlugin.configs["flat/recommended"],
  {
    files: ["**/*.stories.js"],
    rules: {
      "jsdoc/require-property": "off",
    },
  },

  // ---------------------------------------------------------------------------
  // 6. Playwright e2e specs
  // ---------------------------------------------------------------------------
  {
    files: ["frontend/e2e/**/*.spec.js"],
    plugins: { playwright: playwrightPlugin },
    languageOptions: {
      ...playwrightPlugin.configs["flat/recommended"].languageOptions,
    },
    rules: {
      ...playwrightPlugin.configs["flat/recommended"].rules,

      // R9 — High-catch-rate Playwright rules explicit at error.
      "playwright/missing-playwright-await": "error",
      "playwright/no-focused-test": "error",
      "playwright/no-skipped-test": "error",
      "playwright/expect-expect": "error",
    },
  },

  // ---------------------------------------------------------------------------
  // 7. eslint-config-prettier/flat — MUST be last.
  //    Turns off every ESLint stylistic rule that would fight with Prettier's
  //    output. R4: Prettier never runs as an ESLint rule; ESLint only
  //    disables what Prettier already owns.
  // ---------------------------------------------------------------------------
  prettierConfig,
];
