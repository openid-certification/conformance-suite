import { test, expect } from "@playwright/test";
import { setupCommonRoutes } from "./helpers/routes.js";

/**
 * Drift-detection for the Lit importmap.
 *
 * The importmap that resolves `lit` and every `lit/directives/*.js` specifier
 * is repeated verbatim across every static HTML page under
 * `src/main/resources/static/`. A typo or missing entry on a single page leaves
 * components that import a directive broken on that page only — easy to miss
 * without hitting every page at runtime.
 *
 * The probes assert the specific named export exists (not just "any function
 * export"), so a misconfigured alias that returns a Lit module *without* the
 * expected directive name would fail here instead of silently at runtime.
 *
 * API hygiene is intentionally NOT enforced here — pages fire many different
 * `/api/**` endpoints at parse time (logs, plans, tokens, runner status, …),
 * and this test doesn't care about any of them. Individual page specs own
 * their own endpoint mocks and `setupFailFast`/`expectNoUnmockedCalls`
 * coverage. A permissive catch-all returns empty JSON so page JS can render
 * without uncaught fetch errors while the importmap probes run. Uncaught
 * page errors still fail the test via a `pageerror` listener attached by
 * `setupPermissiveApiMocks` and asserted in `afterEach` — so a page that
 * crashes during parse (because `{}` broke its hydration) cannot silently
 * satisfy the directive probe.
 */

const PAGES = [
  "/index.html",
  "/log-detail.html",
  "/login.html",
  "/logs.html",
  "/plan-detail.html",
  "/plans.html",
  "/running-test.html",
  "/schedule-test.html",
  "/tokens.html",
  "/upload.html",
];

const DIRECTIVE_PROBES = [
  { specifier: "lit/directives/repeat.js", exportName: "repeat" },
  { specifier: "lit/directives/class-map.js", exportName: "classMap" },
  { specifier: "lit/directives/when.js", exportName: "when" },
  { specifier: "lit/directives/if-defined.js", exportName: "ifDefined" },
  { specifier: "lit/directives/ref.js", exportName: "ref" },
];

/**
 * Register a permissive catch-all for `/api/**` first, then layer the
 * specific `setupCommonRoutes` mocks on top. Playwright matches the
 * most-recently-registered handler first, so the common routes (currentuser,
 * server, spec_links) still win; anything else falls through to `{}`.
 *
 * Also attaches a `pageerror` listener so crashes during parse (e.g., a page
 * hitting `TypeError` because `{}` isn't the shape it expected) surface as a
 * real test failure via `expectNoPageErrors` in `afterEach`.
 *
 * @param {import('@playwright/test').Page & { __pageErrors?: Error[] }} page
 */
async function setupPermissiveApiMocks(page) {
  /** @type {Error[]} */
  const pageErrors = [];
  page.__pageErrors = pageErrors;
  page.on("pageerror", (err) => pageErrors.push(err));

  await page.route("**/api/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    }),
  );
  await setupCommonRoutes(page);
}

/**
 * Assert that no uncaught page errors were recorded by the `pageerror`
 * listener that `setupPermissiveApiMocks` attached. Fails the test with a
 * joined list of error messages when any were captured.
 *
 * @param {import('@playwright/test').Page & { __pageErrors?: Error[] }} page
 */
function expectNoPageErrors(page) {
  const errors = page.__pageErrors ?? [];
  if (errors.length > 0) {
    const messages = errors.map((e) => e.message).join("\n  ");
    throw new Error(`Unexpected page errors:\n  ${messages}`);
  }
}

const BUNDLE_WARNING_TEXT = "Lit has been loaded from a bundle";

/**
 * Collect Lit-bundle warnings emitted on `page` for the duration of a single
 * navigation. Extracted to module scope so the filter conditional lives
 * outside the `test` body (eslint `playwright/no-conditional-in-test`).
 *
 * @param {import('@playwright/test').Page} page
 * @returns {{ warnings: string[], stop: () => void }}
 */
function captureBundleWarnings(page) {
  /** @type {string[]} */
  const warnings = [];
  /** @param {import('@playwright/test').ConsoleMessage} msg */
  const listener = (msg) => {
    if (msg.type() === "warning" && msg.text().includes(BUNDLE_WARNING_TEXT)) {
      warnings.push(msg.text());
    }
  };
  page.on("console", listener);
  return { warnings, stop: () => page.off("console", listener) };
}

test.describe("Lit importmap", () => {
  test.afterEach(async ({ page }) => {
    expectNoPageErrors(page);
  });

  for (const pagePath of PAGES) {
    test(`${pagePath} resolves every probed directive to its named export`, async ({ page }) => {
      await setupPermissiveApiMocks(page);

      await page.goto(pagePath);

      for (const { specifier, exportName } of DIRECTIVE_PROBES) {
        const exportType = await page.evaluate(
          async ({ spec, name }) => {
            const mod = await import(/* @vite-ignore */ spec);
            return typeof mod[name];
          },
          { spec: specifier, name: exportName },
        );

        expect(
          exportType,
          `${specifier} should export \`${exportName}\` as a function from ${pagePath}`,
        ).toBe("function");
      }
    });
  }

  test("every probed directive resolves to the same module instance (single bundle)", async ({
    page,
  }) => {
    await setupPermissiveApiMocks(page);
    await page.goto("/index.html");

    const sameAsLitModule = await page.evaluate(async (probes) => {
      const litModule = await import("lit");
      return Promise.all(
        probes.map(async ({ specifier, exportName }) => {
          const mod = await import(/* @vite-ignore */ specifier);
          return {
            specifier,
            exportName,
            sameModule: mod === litModule,
            sameExport: mod[exportName] === litModule[exportName],
          };
        }),
      );
    }, DIRECTIVE_PROBES);

    for (const { specifier, exportName, sameModule, sameExport } of sameAsLitModule) {
      expect(sameModule, `${specifier} should resolve to the same module as 'lit'`).toBe(true);
      expect(
        sameExport,
        `\`${exportName}\` from ${specifier} should be the same reference as \`lit\`.${exportName}`,
      ).toBe(true);
    }
  });

  test("litDisableBundleWarning is set on every page", async ({ page }) => {
    await setupPermissiveApiMocks(page);

    for (const pagePath of PAGES) {
      const { warnings, stop } = captureBundleWarnings(page);

      await page.goto(pagePath);

      stop();
      expect(warnings, `${pagePath} should not emit the Lit bundle warning`).toHaveLength(0);
    }
  });
});
