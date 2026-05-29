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
 * Fails the test when the `pageerror` listener from `setupPermissiveApiMocks`
 * recorded any uncaught page errors. Throws loudly if the setup helper was
 * never called, matching the `expectNoUnmockedCalls` guard in `helpers/routes.js`.
 *
 * @param {import('@playwright/test').Page & { __pageErrors?: Error[] }} page
 */
function expectNoPageErrors(page) {
  if (!page.__pageErrors) {
    throw new Error(
      "setupPermissiveApiMocks() was not called for this test — page errors would not be detected",
    );
  }
  if (page.__pageErrors.length > 0) {
    const messages = page.__pageErrors.map((e) => e.message).join("\n  ");
    throw new Error(`Unexpected page errors:\n  ${messages}`);
  }
}

/**
 * Collects Lit-bundle console warnings for a single navigation. Extracted
 * to module scope so the filter `if` lives outside any `test` body (eslint
 * `playwright/no-conditional-in-test`).
 *
 * @param {import('@playwright/test').Page} page
 * @returns {{ warnings: string[], stop: () => void }}
 */
function captureBundleWarnings(page) {
  /** @type {string[]} */
  const warnings = [];
  /** @param {import('@playwright/test').ConsoleMessage} msg */
  const listener = (msg) => {
    if (msg.type() === "warning" && msg.text().includes("Lit has been loaded from a bundle")) {
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
    await page.goto("/plans.html");

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

  // marked + DOMPurify are vendored the same way as Lit: a per-page importmap
  // entry resolving the bare specifier to /vendor/. format-description.js
  // imports them as bare specifiers, so a missing or typo'd entry on a page
  // that renders test prose breaks rendering silently at import time. Probe
  // that both resolve to their working exports on every page (the importmap is
  // uniform across all pages, like the Lit entries).
  for (const pagePath of PAGES) {
    test(`${pagePath} resolves marked and dompurify to working exports`, async ({ page }) => {
      await setupPermissiveApiMocks(page);

      await page.goto(pagePath);

      const resolved = await page.evaluate(async () => {
        const markedMod = await import(/* @vite-ignore */ "marked");
        const purifyMod = await import(/* @vite-ignore */ "dompurify");
        return {
          markedParse: typeof markedMod.marked?.parse,
          purifySanitize: typeof purifyMod.default?.sanitize,
        };
      });

      expect(resolved.markedParse, `marked.parse should resolve from ${pagePath}`).toBe("function");
      expect(resolved.purifySanitize, `DOMPurify.sanitize should resolve from ${pagePath}`).toBe(
        "function",
      );
    });
  }
});
