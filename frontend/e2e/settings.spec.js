import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { watchPageErrors, expectNoPageErrors } from "./helpers/page-errors.js";
import { MOCK_ADMIN_USER, MOCK_USER } from "./fixtures/mock-users.js";
import {
  MOCK_SETTINGS_CONFIGURED,
  MOCK_SETTINGS_EMPTY,
  MOCK_SETTINGS_KEY_MISMATCH,
} from "./fixtures/mock-settings.js";

/**
 * settings.html — the admin-only server settings page.
 *
 * One endpoint backs it: GET /api/admin/settings/cmf-chile returns the
 * redacted Chile CMF section (403 for a non-admin), and PUT saves it (400 with
 * per-field errors when refused). The server never sends stored secrets, so
 * these specs assert on the PUT body the page SENT: a secret the admin did not
 * type must be left out, since that is how the server knows to keep it.
 */

const ENDPOINT = "**/api/admin/settings/cmf-chile";

/**
 * @typedef {object} RouteReply
 * @property {number} status - HTTP status.
 * @property {unknown} [body] - JSON body; omitted for an empty response.
 */

/**
 * @param {import('@playwright/test').Route} route - The intercepted request.
 * @param {RouteReply} reply - What to answer.
 * @returns {Promise<void>} Resolves once fulfilled.
 */
function fulfil(route, reply) {
  return route.fulfill({
    status: reply.status,
    contentType: "application/json",
    body: reply.body === undefined ? "" : JSON.stringify(reply.body),
  });
}

/**
 * Serve the settings endpoint and record every PUT body.
 * @param {import('@playwright/test').Page} page - The page under test.
 * @param {{get?: RouteReply, put?: RouteReply}} [replies] - Answers; a
 *   successful PUT defaults to echoing the configured fixture.
 * @returns {Promise<Array<Record<string, any>>>} PUT bodies, in order.
 */
async function setupSettingsRoute(page, replies = {}) {
  const get = replies.get ?? { status: 200, body: MOCK_SETTINGS_CONFIGURED };
  const put = replies.put ?? { status: 200, body: MOCK_SETTINGS_CONFIGURED };
  /** @type {Array<Record<string, any>>} */
  const puts = [];
  await page.route(ENDPOINT, (route) => {
    if (route.request().method() === "PUT") {
      puts.push(route.request().postDataJSON());
      return fulfil(route, put);
    }
    return fulfil(route, get);
  });
  return puts;
}

test.describe("settings.html — admin server settings", () => {
  test.beforeEach(async ({ page }) => {
    watchPageErrors(page);
  });

  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
    expectNoPageErrors(page);
  });

  test("an admin's navbar links to Settings and the form shows the stored section", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupSettingsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/settings.html");

    const link = page.locator(".cts-navlink[href='settings.html']");
    await expect(link).toBeVisible();
    await expect(link).toHaveText("Settings");
    await expect(link).toHaveClass(/active/);

    await expect(page.getByLabel("Directory token endpoint URL")).toHaveValue(
      "https://directory.sandbox.example.cl/token",
    );
    await expect(page.getByLabel("Directory client ID")).toHaveValue("oidf-conformance");
    const secret = page.getByLabel("Directory client secret");
    await expect(secret).toHaveValue("");
    await expect(secret).toHaveAttribute("placeholder", "set — leave blank to keep");
    await expect(page.getByText("CN=oidf-conformance,O=OpenID Foundation")).toBeVisible();
    await expect(
      page.locator('[data-testid="settings-negativeCertificates"] cts-badge'),
    ).toHaveAttribute("label", "Expired");
  });

  test("a non-admin gets the admin-only alert, no form, and no Settings nav link", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupSettingsRoute(page, { get: { status: 403 } });
    await setupCommonRoutes(page, { user: MOCK_USER });

    await page.goto("/settings.html");

    const forbidden = page.locator('[data-testid="settings-forbidden"]');
    await expect(forbidden).toBeVisible();
    await expect(forbidden).toContainText("Server settings are only available to administrators.");
    await expect(page.locator("cts-settings-page form")).toHaveCount(0);
    await expect(page.locator(".cts-navlink[href='plans.html']")).toBeVisible();
    await expect(page.locator(".cts-navlink[href='settings.html']")).toHaveCount(0);
  });

  test("an expired session is told to sign in again, not that it is a non-admin", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupSettingsRoute(page, { get: { status: 401 } });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/settings.html");

    const forbidden = page.locator('[data-testid="settings-forbidden"]');
    await expect(forbidden).toContainText("Your session has expired. Sign in again");
    await expect(forbidden).not.toContainText("only available to administrators");
  });

  test("saving without touching a secret sends no secret, so the stored ones are kept", async ({
    page,
  }) => {
    await setupFailFast(page);
    const puts = await setupSettingsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/settings.html");
    await page.getByLabel("Directory client ID").fill("renamed-client");
    await page.getByRole("button", { name: "Save" }).click();

    await expect.poll(() => puts.length).toBe(1);
    const body = puts[0];
    expect(body.clientId).toBe("renamed-client");
    expect(body).not.toHaveProperty("clientSecret");
    expect(body).not.toHaveProperty("clearClientSecret");
    expect(body).not.toHaveProperty("clientJwks");
    expect(body.positiveCertificates).toEqual([
      {
        id: "pos-1",
        label: "primary",
        certificateChainPem: MOCK_SETTINGS_CONFIGURED.positiveCertificates[0].certificateChainPem,
      },
    ]);
    expect(body.negativeCertificates[0]).not.toHaveProperty("privateKeyPem");
  });

  test("a typed secret and a replaced key are sent, then cleared from the page", async ({
    page,
  }) => {
    await setupFailFast(page);
    const puts = await setupSettingsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/settings.html");
    await page.getByLabel("Directory client secret").fill("typed-secret-value");
    await page.getByRole("button", { name: "Replace the private key of primary" }).click();
    await page.getByLabel("Private key (PEM, unencrypted)").fill("TYPED-PRIVATE-KEY");
    await page.getByRole("button", { name: "Save" }).click();

    await expect.poll(() => puts.length).toBe(1);
    expect(puts[0].clientSecret).toBe("typed-secret-value");
    expect(puts[0].positiveCertificates[0].privateKeyPem).toBe("TYPED-PRIVATE-KEY");

    await expect(page.getByLabel("Directory client secret")).toHaveValue("");
    await expect(page.getByLabel("Private key (PEM, unencrypted)")).toHaveCount(0);
    const html = await page.locator("cts-settings-page").evaluate((element) => element.outerHTML);
    expect(html).not.toContain("TYPED-PRIVATE-KEY");
  });

  test("a refused save puts the server's message on the field it concerns", async ({ page }) => {
    await setupFailFast(page);
    await setupSettingsRoute(page, { put: { status: 400, body: MOCK_SETTINGS_KEY_MISMATCH } });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/settings.html");
    await page.getByRole("button", { name: "Replace the private key of primary" }).click();
    await page.getByRole("button", { name: "Save" }).click();

    const fieldError = page.locator('[data-error-for="positiveCertificates[0].privateKeyPem"]');
    await expect(fieldError).toContainText("does not match the first certificate in its chain");
    const keyInput = page.getByLabel("Private key (PEM, unencrypted)");
    await expect(keyInput).toHaveAttribute("aria-invalid", "true");
    await expect(keyInput).toHaveAccessibleDescription(
      /does not match the first certificate in its chain/,
    );
    await expect(page.locator('[data-testid="settings-errors"]')).toContainText(
      "The settings were not saved",
    );
  });

  test("a JWKS that is not a JSON object is refused before anything is sent", async ({ page }) => {
    await setupFailFast(page);
    const puts = await setupSettingsRoute(page, {
      get: { status: 200, body: MOCK_SETTINGS_EMPTY },
    });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/settings.html");
    await page.getByRole("button", { name: "Set JWKS" }).click();
    const editor = page.locator("cts-settings-page cts-json-editor");
    await expect(editor).toBeVisible();
    await editor.evaluate((element) => {
      const jsonEditor = /** @type {HTMLElement & {value: string}} */ (element);
      jsonEditor.value = "[1, 2]";
      jsonEditor.dispatchEvent(new Event("input", { bubbles: true }));
    });
    await page.getByRole("button", { name: "Save" }).click();

    await expect(page.locator('[data-error-for="clientJwks"]')).toContainText(
      "'Client JWKS' must be a JSON object",
    );
    expect(puts).toHaveLength(0);
  });

  test("certificates can be added to and removed from a list", async ({ page }) => {
    await setupFailFast(page);
    const puts = await setupSettingsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/settings.html");
    const negative = page.locator('[data-testid="settings-negativeCertificates"]');
    await page
      .getByRole("button", { name: "Add a certificate to Negative DCR client certificates" })
      .click();
    await expect(negative.locator(".cts-settings-entry")).toHaveCount(2);
    await negative.getByLabel("Label").nth(1).fill("untrusted CA");
    await negative.getByLabel("Certificate chain (PEM, leaf first)").nth(1).fill("CERT-U");
    await negative.getByLabel("Private key (PEM, unencrypted)").fill("KEY-U");

    await page
      .getByRole("button", { name: "Remove primary from Positive DCR client certificates" })
      .click();
    await expect(page.locator('[data-testid="settings-positiveCertificates"]')).toContainText(
      "No certificates.",
    );

    await page.getByRole("button", { name: "Save" }).click();
    await expect.poll(() => puts.length).toBe(1);
    expect(puts[0].positiveCertificates).toEqual([]);
    expect(puts[0].negativeCertificates[1]).toEqual({
      label: "untrusted CA",
      certificateChainPem: "CERT-U",
      privateKeyPem: "KEY-U",
    });
  });
});
