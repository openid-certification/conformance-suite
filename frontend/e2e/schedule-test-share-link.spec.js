import { test, expect } from "@playwright/test";
import { setupScheduleTestRoutes, expectNoUnmockedCalls } from "./helpers/routes.js";
import { selectedPlanRow } from "./helpers/pick-plan.js";

/**
 * "Share Test Plan Configuration" links.
 *
 * The config rides in the query string. New links use the compressed
 * `?configJsonZ=` (base64url/deflate, see /js/config-url-codec.js); links
 * already in the wild use the legacy `?configJson=` (URL-encoded JSON). The
 * page must emit the former and hydrate from both.
 *
 * No backend: the server never parses either parameter.
 */

const PLAN = "oidcc-client-basic-certification-test-plan";

/**
 * URL of the codec module as served to the browser. Kept in a variable (not
 * an import() literal) because the import() calls below run inside
 * page.evaluate — in the browser — and tsc would otherwise try to resolve the
 * absolute URL against the Node project.
 */
const CODEC_MODULE_URL = "/js/config-url-codec.js";

/** Realistic-ish config: nested objects, a JWK, PEM-with-newlines, unicode. */
const SHARED_CONFIG = {
  alias: "shared-via-link",
  description: "VCI Issuer test config (dpop) — ü/€",
  "server.issuer": "https://restored.example.com",
  client: {
    client_id: "52480754053",
    jwks: {
      keys: [
        {
          kty: "EC",
          crv: "P-256",
          kid: "vci-example-key-1",
          x: "yHNp8QgNiVSxSxIH_n_nH23dpUDlNhbgvLKSrjK1hDs",
          y: "3_rlpW_FXqghp8dKPpkjfvbfACQQFLFZwJXxOr319Ac",
        },
      ],
    },
  },
  mtls: {
    cert: "-----BEGIN CERTIFICATE-----\nMIIDlTCCAn2gAwIBAgIJAKRJoaX7BlZb\n-----END CERTIFICATE-----\n",
  },
  browser: [
    {
      match: "https://*/authorize*",
      tasks: [{ task: "Verify", commands: [["wait", "id", "x", 10]] }],
    },
  ],
};

/**
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<any>} the cts-config-form's current config object
 */
async function readConfig(page) {
  return page.evaluate(() => {
    const host = /** @type {any} */ (document.getElementById("ctsConfigForm"));
    return host ? host.config || {} : null;
  });
}

/**
 * Compress a config the way the page does, inside the browser, using the real
 * codec module — keeps the test honest about the wire format.
 *
 * @param {import('@playwright/test').Page} page
 * @param {object} config
 * @returns {Promise<string>}
 */
async function compressInBrowser(page, config) {
  return page.evaluate(
    async ([url, cfg]) => {
      const codec = await import(/** @type {string} */ (url));
      return codec.compressConfigForUrl(cfg);
    },
    [CODEC_MODULE_URL, config],
  );
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} value
 * @returns {Promise<any>}
 */
async function decompressInBrowser(page, value) {
  return page.evaluate(
    async ([url, v]) => {
      const codec = await import(/** @type {string} */ (url));
      return codec.decompressConfigFromUrl(v);
    },
    [CODEC_MODULE_URL, value],
  );
}

/** @param {import('@playwright/test').Page} page */
async function installClipboardSpy(page) {
  await page.addInitScript(() => {
    /** @type {any} */ (window).__copiedText = null;
    if (navigator.clipboard) {
      navigator.clipboard.writeText = (text) => {
        /** @type {any} */ (window).__copiedText = text;
        return Promise.resolve();
      };
    }
  });
}

test.describe("schedule-test.html — share link config encoding", () => {
  test.beforeEach(async ({ page }) => {
    // Advanced island throughout (guided is the stored-preference default).
    await page.addInitScript(() => {
      try {
        localStorage.setItem("oidf-guided-mode", "advanced");
      } catch {
        /* storage unavailable — the test will surface it */
      }
    });
  });

  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("hydrates the form from a legacy ?configJson= link", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    const legacy = encodeURIComponent(JSON.stringify(SHARED_CONFIG));
    await page.goto(`/schedule-test.html?test_plan=${PLAN}&configJson=${legacy}`);

    await expect(selectedPlanRow(page)).toHaveAttribute("data-plan-name", PLAN);
    await expect.poll(() => readConfig(page)).toEqual(SHARED_CONFIG);
  });

  test("hydrates the form from a compressed ?configJsonZ= link", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    // Compress on a throwaway navigation so the module is served by the same
    // static server the page uses.
    await page.goto("/schedule-test.html");
    const compressed = await compressInBrowser(page, SHARED_CONFIG);
    // base64url is URL-unreserved: the value goes into the link verbatim.
    expect(encodeURIComponent(compressed)).toBe(compressed);
    expect(compressed.length).toBeLessThan(
      encodeURIComponent(JSON.stringify(SHARED_CONFIG)).length,
    );

    await page.goto(`/schedule-test.html?test_plan=${PLAN}&configJsonZ=${compressed}`);

    await expect(selectedPlanRow(page)).toHaveAttribute("data-plan-name", PLAN);
    await expect.poll(() => readConfig(page)).toEqual(SHARED_CONFIG);
  });

  test("an undecodable ?configJsonZ= is ignored (plan still selected, form empty)", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    const warnings = [];
    page.on("console", (msg) => {
      if (msg.type() === "warning") warnings.push(msg.text());
    });

    await page.goto(`/schedule-test.html?test_plan=${PLAN}&configJsonZ=%40%40not-base64%40%40`);

    await expect(selectedPlanRow(page)).toHaveAttribute("data-plan-name", PLAN);
    await page.waitForFunction(() => document.getElementById("createPlanBtn")?.onclick !== null);
    expect(await readConfig(page)).toEqual({});
    expect(warnings.some((w) => w.includes("configJsonZ"))).toBe(true);
  });

  test("the share button copies a ?configJsonZ= link that round-trips the config", async ({
    page,
  }) => {
    await installClipboardSpy(page);
    await setupScheduleTestRoutes(page);

    // Arrive via a legacy link so the page has a plan AND a config without
    // driving the form field-by-field. A user editing the form and pressing
    // share ends in the same createTestPlanParameterUrl() path.
    const legacy = encodeURIComponent(JSON.stringify(SHARED_CONFIG));
    await page.goto(`/schedule-test.html?test_plan=${PLAN}&configJson=${legacy}`);
    await expect.poll(() => readConfig(page)).toEqual(SHARED_CONFIG);

    const shareBtn = page.locator("#copyPlanUrlBtn");
    await expect(shareBtn).toBeEnabled();
    await shareBtn.click();

    await expect
      .poll(() => page.evaluate(() => /** @type {any} */ (window).__copiedText))
      .toBeTruthy();
    const copied = new URL(await page.evaluate(() => /** @type {any} */ (window).__copiedText));

    expect(copied.pathname).toBe("/schedule-test.html");
    expect(copied.searchParams.get("test_plan")).toBe(PLAN);
    // New links are compressed — never the legacy parameter.
    expect(copied.searchParams.get("configJson")).toBeNull();
    const compressed = copied.searchParams.get("configJsonZ");
    expect(compressed).toBeTruthy();
    expect(await decompressInBrowser(page, /** @type {string} */ (compressed))).toEqual(
      SHARED_CONFIG,
    );
    // And it's shorter than the legacy link that brought us here.
    expect(copied.href.length).toBeLessThan(page.url().length);

    // The success modal opens (same UX as before the encoding change).
    await expect(page.locator("#copyTestPlanUrlModal")).toBeVisible();

    // Following our own link lands the same config again.
    await page.goto(copied.href);
    await expect(selectedPlanRow(page)).toHaveAttribute("data-plan-name", PLAN);
    await expect.poll(() => readConfig(page)).toEqual(SHARED_CONFIG);
  });
});
