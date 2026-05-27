import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS } from "./fixtures/mock-plans.js";

/** All available plans including the no-variants plan */
const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

test.describe("schedule-test.html — Test Plan Scheduling", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("cascade populates specification families (R7)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Cross-page contract: every wired page mounts a single <cts-toast-host>
    // for window.ctsToast(...). A silent removal of the mount from schedule-test.html
    // would otherwise pass all tests in this file. (Mirrors upload.spec.js:210.)
    await expect(page.locator("cts-toast-host")).toHaveCount(1);

    // Spec family dropdown populates with families from the mock data
    const familySelect = page.locator("#specFamilySelect");
    await expect(familySelect).toBeVisible();

    // Should have FAPI and OIDCC as options (from MOCK_PLANS)
    await expect(familySelect.locator("option")).toHaveCount(3); // blank + FAPI + OIDCC

    // Select OIDCC → entity selector appears
    await familySelect.selectOption("OIDCC");

    // Entity select should become visible (OIDCC has multiple profiles)
    const entitySelect = page.locator("#entitySelect");
    await expect(entitySelect).toBeVisible();

    // Select "basic" profile → version and plan cascade
    await entitySelect.selectOption("basic");

    // Plan select should become visible
    const planSelect = page.locator("#planSelect");
    await expect(planSelect).toBeVisible();

    // Should contain the basic OIDCC plan
    await expect(planSelect).toContainText("OpenID Connect Core: Basic Certification Profile");
  });

  test("submission POSTs to /api/plan and redirects (R9)", async ({ page }) => {
    let postCalled = false;

    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    // Mock POST /api/plan — return a new plan ID
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        postCalled = true;
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            id: "plan-new-001",
            name: "oidcc-client-basic-certification-test-plan",
          }),
        });
      }
      return route.fallback();
    });

    // Mock the plan-detail page that we'll be redirected to
    await page.route("**/api/plan/plan-new-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          _id: "plan-new-001",
          planName: "oidcc-client-basic-certification-test-plan",
          modules: [],
          variant: {},
          config: {},
        }),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Navigate the cascade: OIDCC → client-basic (no variants) → Final → plan auto-selects
    await page.locator("#specFamilySelect").selectOption("OIDCC");

    // Entity select appears — choose client-basic (has no variants)
    const entitySelect = page.locator("#entitySelect");
    await expect(entitySelect).toBeVisible();
    await entitySelect.selectOption("client-basic");

    // Plan should auto-select (single plan for client-basic/Final)
    const planSelect = page.locator("#planSelect");
    await expect(planSelect).toBeVisible();

    // Wait for the Create button to become enabled (no variants = immediately enabled)
    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });

    // Click Create Test Plan
    await createBtn.click();

    // Should navigate to plan-detail with the new plan ID
    await page.waitForURL("**/plan-detail.html?plan=plan-new-001");
    expect(postCalled).toBe(true);
  });

  test("create button disabled when no plan selected, shows error modal on forced click (R10)", async ({
    page,
  }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Create button should be disabled initially (no plan selected).
    // Query the inner native <button> rendered by cts-button — Playwright's
    // toBeDisabled() reads the `:disabled` pseudo-class which applies to
    // native form controls, not custom-element hosts.
    const createBtn = page.locator("#createPlanBtn button");
    await expect(createBtn).toBeDisabled();

    // Wait until the inline init chain has installed createPlanBtn.onclick.
    // Post-Phase-2 the chain awaits a config-field-catalog fetch before
    // loadScheduleTestPage() runs, so the assertion above (which only
    // checks the cts-button render output) can resolve before the click
    // handler is wired. Same pattern as schedule-test-baselines.spec.js.
    await page.waitForFunction(() => document.getElementById("createPlanBtn")?.onclick !== null);

    // The button is hidden (display:none on parent) when no plan is selected.
    // Use evaluate to invoke the onclick handler directly — it checks
    // planSelect.value and shows an error modal.
    await page.evaluate(() => {
      const btn = document.getElementById("createPlanBtn");
      if (!btn) throw new Error("createPlanBtn not found");
      btn.removeAttribute("disabled");
      btn.style.display = "";
      const launchButtons = /** @type {HTMLElement | null} */ (btn.closest("#launchButtons"));
      if (launchButtons) launchButtons.style.display = "";
      btn.click();
    });

    // Error modal should appear with "select a test plan" message
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    await expect(page.locator("#errorMessage")).toContainText("select a test plan");

    // Close the error modal
    await errorModal.locator(".oidf-modal-close").first().click();
    await expect(errorModal).toBeHidden();
  });

  test("variant selectors render when plan has variants (R5)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Navigate cascade to OIDCC basic plan (has client_auth_type, response_type, server_metadata variants)
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    const entitySelect = page.locator("#entitySelect");
    await expect(entitySelect).toBeVisible();
    await entitySelect.selectOption("basic");

    // Variant selectors should appear
    const variantSelectors = page.locator("#variantSelectors");
    await expect(variantSelectors).toBeVisible();

    // Should have 3 variant dropdowns (client_auth_type, response_type, server_metadata)
    const selects = variantSelectors.locator("select.variant-selector");
    await expect(selects).toHaveCount(3);

    // Each dropdown has the correct options from the fixture's variantValues
    const authSelect = page.locator("#vp_client_auth_type");
    await expect(authSelect).toBeVisible();
    await expect(authSelect.locator("option")).toHaveCount(4); // "--- Select ---" + 3 values
    await expect(authSelect).toContainText("client_secret_basic");
    await expect(authSelect).toContainText("client_secret_post");
    await expect(authSelect).toContainText("private_key_jwt");

    const responseSelect = page.locator("#vp_response_type");
    await expect(responseSelect).toBeVisible();
    await expect(responseSelect.locator("option")).toHaveCount(2); // "--- Select ---" + 1 value

    const metadataSelect = page.locator("#vp_server_metadata");
    await expect(metadataSelect).toBeVisible();
    await expect(metadataSelect.locator("option")).toHaveCount(3); // "--- Select ---" + 2 values
  });

  test("submitting with variants includes variant JSON in POST URL (R6)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    // Mock POST /api/plan — capture the request URL to verify variant params
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "plan-new-002", name: "oidcc-basic-certification-test-plan" }),
        });
      }
      return route.fallback();
    });

    await page.route("**/api/plan/plan-new-002", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          _id: "plan-new-002",
          planName: "oidcc-basic-certification-test-plan",
          modules: [],
          variant: {
            client_auth_type: "client_secret_basic",
            response_type: "code",
            server_metadata: "discovery",
          },
          config: {},
        }),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Navigate cascade to OIDCC basic plan
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    await page.locator("#entitySelect").selectOption("basic");

    // Select variant values
    await page.locator("#vp_client_auth_type").selectOption("client_secret_basic");
    await page.locator("#vp_response_type").selectOption("code");
    await page.locator("#vp_server_metadata").selectOption("discovery");

    // Wait for Create button to enable
    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });

    // Set up request interception BEFORE clicking
    const planRequest = page.waitForRequest(
      (req) => req.url().includes("/api/plan?") && req.method() === "POST",
    );

    // Click Create
    await createBtn.click();

    // Verify the POST URL contains variant JSON
    const req = await planRequest;
    const url = new URL(req.url());
    const variantParams = url.searchParams.getAll("variant");
    expect(variantParams.length).toBeGreaterThan(0);

    // The variant JSON should contain the selected values
    const variantJson = JSON.parse(decodeURIComponent(variantParams[0]));
    expect(variantJson.client_auth_type).toBe("client_secret_basic");
    expect(variantJson.response_type).toBe("code");
    expect(variantJson.server_metadata).toBe("discovery");

    // Should navigate to plan-detail
    await page.waitForURL("**/plan-detail.html?plan=plan-new-002");
  });

  test("variant selectors hidden for plan with no variants", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Navigate to the no-variants plan (client-basic)
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    await page.locator("#entitySelect").selectOption("client-basic");

    // Variant selectors should be hidden (display: none)
    await expect(page.locator("#variantSelectors")).toBeHidden();
  });

  test("degrades gracefully when /api/plan/available returns 500 (R11)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ error: "Internal Server Error" }),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // When /api/plan/available returns 5xx, cts-spec-cascade renders an
    // explicit error banner instead of an empty cascade. The cascade selects
    // are not rendered in this state — the user sees a clear message rather
    // than a silently broken UI.
    const errorBanner = page.locator('[data-testid="spec-cascade-error"]');
    await expect(errorBanner).toBeVisible();
    await expect(errorBanner).toContainText("Unable to load plans");

    // The cascade selects are absent in the error state — there is no plan
    // the user could pick, by design.
    await expect(page.locator("#specFamilySelect")).toHaveCount(0);

    // Create button should remain disabled (no plan can be selected).
    // Targets the inner native button — see note in the R10 test.
    await expect(page.locator("#createPlanBtn button")).toBeDisabled();
  });

  // --- R13: opt-in load-last-config (the page no longer auto-prefills) ---
  //
  // Phase 2 swapped <cts-json-editor id="config"> for <cts-config-form
  // id="ctsConfigForm">. The component owns its own JSON tab and exposes
  // the working config object via the `.config` property. These helpers
  // read/write the underlying config object directly so the assertions
  // stay independent of which tab is currently active.

  /**
   * @param {import('@playwright/test').Page} page
   * @returns {Promise<string>} JSON.stringify of the current config object.
   */
  async function readConfigValue(page) {
    return page.evaluate(() => {
      const host = /** @type {any} */ (document.getElementById("ctsConfigForm"));
      if (!host) return "";
      return JSON.stringify(host.config || {});
    });
  }

  /**
   * @param {import('@playwright/test').Page} page
   * @param {string} value JSON string parsed and assigned to .config.
   */
  async function setConfigValue(page, value) {
    await page.evaluate((next) => {
      const host = /** @type {any} */ (document.getElementById("ctsConfigForm"));
      if (!host) throw new Error("cts-config-form#ctsConfigForm not found");
      host.config = JSON.parse(next);
    }, value);
  }

  test("R13: selecting a new plan type clears the config form", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    // Even though we never click "Load last configuration", mock the
    // endpoint so an accidental fetch (regression to auto-prefill) shows
    // up as content in the editor, not an unmocked-call assertion failure.
    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    // Pick OIDCC basic, populate the config editor, then switch to FAPI.
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    await page.locator("#entitySelect").selectOption("basic");
    await setConfigValue(page, '{"alias":"about-to-be-cleared","server.issuer":"https://x.test"}');

    await page.locator("#specFamilySelect").selectOption("FAPI");

    // After clear, currentConfig is an empty object — stringified shape is "{}".
    expect(await readConfigValue(page)).toBe("{}");
  });

  test("R13: clicking 'Load last configuration' restores the previous config", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    // The endpoint shape is { config, planName, variant } — only `config`
    // is required for this assertion. No planName means selectPlanByName
    // is a no-op (returns false on `!plan`), which keeps the test focused
    // on "click → editor populated" without coupling to the cascade.
    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          config: {
            alias: "from-last-config",
            "server.issuer": "https://restored.example.com",
          },
        }),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    // Land on a plan with a config form; the editor must start empty
    // (per R13's new behavior — no auto-load on init).
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    await page.locator("#entitySelect").selectOption("basic");
    // After clear, currentConfig is an empty object — stringified shape is "{}".
    expect(await readConfigValue(page)).toBe("{}");

    await page.getByTestId("load-last-config").click();

    // The editor should now contain the /api/lastconfig payload.
    await expect.poll(() => readConfigValue(page)).toContain("from-last-config");
  });

  test("R13/U11: page load probes /api/lastconfig but does not apply it", async ({ page }) => {
    await setupFailFast(page);

    let lastconfigCalled = false;

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    // U11 (B3): bootstrap probes /api/lastconfig once to gate the
    // "Load last configuration" button's disabled state. R13's no-auto-
    // apply contract still holds — the probe MUST NOT write the payload
    // to the editor. The mock returns a populated config so an accidental
    // regression to auto-apply would land "should-not-load" in the form.
    await page.route("**/api/lastconfig", (route) => {
      lastconfigCalled = true;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ config: { alias: "should-not-load" } }),
      });
    });

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    await page.locator("#specFamilySelect").selectOption("OIDCC");
    await page.locator("#entitySelect").selectOption("basic");
    // Form starts empty — the probe must not apply the persisted config.
    expect(await readConfigValue(page)).toBe("{}");

    // The probe runs as part of init; wait for the button to settle out
    // of its initial loading state so the assertion isn't racy.
    await expect(page.locator("#loadLastConfigBtn")).not.toHaveAttribute("loading", /.*/);
    expect(lastconfigCalled).toBe(true);
  });

  test("U11: Load button starts disabled when no saved config exists", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    // Empty payload — backend always returns 200 with {} when nothing
    // is saved for the current user (SavedConfigurationApi.java:32).
    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    // cts-button forwards the host's `disabled` to the inner native
    // <button>; that's what :disabled tracks.
    const innerBtn = page.locator("#loadLastConfigBtn button");
    await expect(innerBtn).toBeDisabled();
    // The probe completes — loading attribute should be cleared.
    await expect(page.locator("#loadLastConfigBtn")).not.toHaveAttribute("loading", /.*/);
  });

  test("U11: Load button enables once probe reports a saved config", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ config: { alias: "saved" } }),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    const innerBtn = page.locator("#loadLastConfigBtn button");
    await expect(innerBtn).toBeEnabled();
    await expect(page.locator("#loadLastConfigBtn")).not.toHaveAttribute("loading", /.*/);
  });

  test("U11: clicking shows pending state and re-enables after success", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    let lastconfigCalls = 0;
    /** @type {(value?: void) => void} */
    let releaseClickFetch = () => {};
    const clickFetchInFlight = new Promise((resolve) => {
      releaseClickFetch = resolve;
    });

    // First call (bootstrap probe) resolves immediately. Second call
    // (click) is held open until the test asserts the pending state.
    await page.route("**/api/lastconfig", async (route) => {
      lastconfigCalls += 1;
      if (lastconfigCalls === 1) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ config: { alias: "saved" } }),
        });
      }
      await clickFetchInFlight;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ config: { alias: "from-last-config" } }),
      });
    });

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    const innerBtn = page.locator("#loadLastConfigBtn button");
    await expect(innerBtn).toBeEnabled();

    await page.getByTestId("load-last-config").click();

    // While the click fetch is in flight, the button shows the spinner
    // and is disabled (cts-button's loading state).
    await expect(page.locator("#loadLastConfigBtn")).toHaveAttribute("loading", /.*/);
    await expect(innerBtn).toBeDisabled();

    // Release the fetch and verify the button settles back to enabled.
    releaseClickFetch();
    await expect(page.locator("#loadLastConfigBtn")).not.toHaveAttribute("loading", /.*/);
    await expect(innerBtn).toBeEnabled();
  });

  test("U11: click error shows toast and re-enables the button", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    let lastconfigCalls = 0;
    await page.route("**/api/lastconfig", (route) => {
      lastconfigCalls += 1;
      if (lastconfigCalls === 1) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ config: { alias: "saved" } }),
        });
      }
      return route.fulfill({ status: 500, body: "Server error" });
    });

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    const innerBtn = page.locator("#loadLastConfigBtn button");
    await expect(innerBtn).toBeEnabled();

    await page.getByTestId("load-last-config").click();

    // FAPI_UI.showError opens the legacy #errorModal.
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();

    // After the failed click, the button re-enables so the user can retry.
    await expect(page.locator("#loadLastConfigBtn")).not.toHaveAttribute("loading", /.*/);
    await expect(innerBtn).toBeEnabled();
  });

  // --- cts-test-selector search flow (search-mode shortcut over the cascade) ---
  //
  // The search selector mounts above cts-spec-cascade and shares the same
  // /api/plan/available payload. Clicking a search result routes through
  // cascade.selectPlanByName so the existing downstream listeners
  // (clearConfigForNewPlan, updateVariants, updateConfigFieldVisibility) fire
  // exactly as if the user used the cascade dropdown. Cascade selections
  // bridge back to the search selector via the `selected` attribute, keeping
  // both entry points in sync regardless of which one the user touched.

  test("search-then-click drives the cascade and reveals the config form", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    // The selector renders one row per plan once `planSearch.plans` is set
    // by loadAvailablePlans(). Wait for at least one row before driving the
    // search — otherwise the type+click can race the init chain.
    const searchRows = page.locator("#planSearch .oidf-test-selector__row");
    await expect(searchRows.first()).toBeVisible();

    // Narrow the list with the search box, then click the no-variants plan
    // (its config form renders immediately — plans with variants are gated
    // on every variant being picked first).
    await page.locator("#planSearch .oidf-test-selector__search").fill("Client: Basic");
    const targetRow = page.locator(
      '#planSearch [data-plan-name="oidcc-client-basic-certification-test-plan"]',
    );
    await expect(targetRow).toBeVisible();
    await targetRow.click();

    // The cascade should reflect the selection across all four tiers.
    await expect(page.locator("#specFamilySelect")).toHaveValue("OIDCC");
    await expect(page.locator("#entitySelect")).toHaveValue("client-basic");
    await expect(page.locator("#planSelect")).toHaveValue(
      "oidcc-client-basic-certification-test-plan",
    );

    // The selector row stays highlighted via the cascade -> search bridge.
    await expect(targetRow).toHaveClass(/is-active/);

    // No variants, so the create button becomes enabled and the config form
    // is shown. The button is a cts-button — target the inner native button
    // for :disabled, matching the existing test patterns above.
    await expect(page.locator("#createPlanBtn button")).toBeEnabled({ timeout: 5000 });
  });

  test("click on a plan row smooth-scrolls #specCascade into view without stealing focus", async ({
    page,
  }) => {
    // The dead-click fix: with a long plan list, the cascade auto-fills
    // below the fold. Selecting a plan must scroll the cascade into view
    // so the user can see what just happened and continue. Mouse clicks
    // do NOT move focus into the cascade — that would surprise users who
    // clicked the plan but want to scroll down to adjust variants.
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    // Shrink the viewport so #specCascade is reliably off-screen at the
    // moment of the click — otherwise the page is short enough that the
    // cascade is already visible and scrolling is a no-op (the test
    // would tautologically pass).
    await page.setViewportSize({ width: 1024, height: 360 });
    await page.goto("/schedule-test.html");

    const searchRows = page.locator("#planSearch .oidf-test-selector__row");
    await expect(searchRows.first()).toBeVisible();

    await page.locator("#planSearch .oidf-test-selector__search").fill("Client: Basic");
    const targetRow = page.locator(
      '#planSearch [data-plan-name="oidcc-client-basic-certification-test-plan"]',
    );
    await expect(targetRow).toBeVisible();

    // Precondition: capture the cascade's top BEFORE the click so we can
    // assert below that the click actually moved the page (vs the
    // cascade happening to already be near viewport-top, which would
    // make the post-click assertion tautological).
    const cascadeTopBeforeClick = await page.locator("#specCascade").evaluate((el) => {
      return Math.round(el.getBoundingClientRect().top);
    });
    expect(cascadeTopBeforeClick).toBeGreaterThan(200);

    await targetRow.click();

    // After the rAF-deferred scroll, #specCascade's top edge should sit
    // near the viewport top. Give it a generous threshold — the scroll
    // is smooth and Playwright may sample mid-animation.
    await expect
      .poll(
        async () => {
          const top = await page.locator("#specCascade").evaluate((el) => {
            return Math.round(el.getBoundingClientRect().top);
          });
          return top;
        },
        { timeout: 3000 },
      )
      .toBeLessThan(100);

    // The scroll-in arrival is punctuated by a one-shot highlight on the
    // cascade. It's applied just after the scroll's rAF and clears itself
    // ~1.6s later, so poll for the modifier in-window (no timing assertion on
    // the animation itself, which would be flaky). The keyboard path runs the
    // identical flashHighlight() call, so asserting it here covers both.
    await expect
      .poll(
        async () =>
          page
            .locator("#specCascade .oidf-spec-cascade")
            .evaluate((el) => el.classList.contains("oidf-spec-cascade--highlight")),
        { timeout: 2000 },
      )
      .toBe(true);

    // Mouse-path: focus stays put. activeElement should not be a cascade
    // <select> (which would be the keyboard-path landing zone).
    const activeIsCascadeSelect = await page.evaluate(() => {
      const active = document.activeElement;
      const firstCascadeSelect = document.getElementById("specCascade")?.querySelector("select");
      return active === firstCascadeSelect;
    });
    expect(activeIsCascadeSelect).toBe(false);
  });

  test("keyboard selection (search → ArrowDown → Enter) scrolls AND focuses the first cascade select", async ({
    page,
  }) => {
    // Mirror of the click path, but on the keyboard side: Enter on a
    // focused row should both scroll the cascade into view AND land
    // focus on the first <select> in the cascade so the user can keep
    // keyboarding into the variants/config form without reaching for
    // the mouse.
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    await page.setViewportSize({ width: 1024, height: 360 });
    await page.goto("/schedule-test.html");

    const searchRows = page.locator("#planSearch .oidf-test-selector__row");
    await expect(searchRows.first()).toBeVisible();

    // Narrow the list, focus the search input, then ArrowDown into the
    // first result and Enter to commit.
    const searchInput = page.locator("#planSearch .oidf-test-selector__search");
    await searchInput.fill("Client: Basic");
    const targetRow = page.locator(
      '#planSearch [data-plan-name="oidcc-client-basic-certification-test-plan"]',
    );
    await expect(targetRow).toBeVisible();

    // Precondition: cascade starts well below viewport-top so the
    // post-selection scroll assertion is meaningful.
    const cascadeTopBeforeEnter = await page.locator("#specCascade").evaluate((el) => {
      return Math.round(el.getBoundingClientRect().top);
    });
    expect(cascadeTopBeforeEnter).toBeGreaterThan(200);

    await searchInput.focus();
    await page.keyboard.press("ArrowDown");
    // The focused row receives the keyboard event; pressing Enter on it
    // dispatches cts-plan-select with via:'keyboard'.
    await page.keyboard.press("Enter");

    // Cascade scrolls into view (same assertion as the click test).
    await expect
      .poll(
        async () => {
          const top = await page.locator("#specCascade").evaluate((el) => {
            return Math.round(el.getBoundingClientRect().top);
          });
          return top;
        },
        { timeout: 3000 },
      )
      .toBeLessThan(100);

    // And focus lands on the first <select> inside #specCascade — the
    // spec-family select — so Tab continues forward into variants/config.
    await expect
      .poll(
        async () => {
          return page.evaluate(() => {
            const active = document.activeElement;
            const firstCascadeSelect = document
              .getElementById("specCascade")
              ?.querySelector("select");
            return active === firstCascadeSelect;
          });
        },
        { timeout: 3000 },
      )
      .toBe(true);
  });

  test("cascade selection highlights the matching row in the search selector", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    // Drive the cascade — no variants means the plan auto-selects when the
    // entity tier picks "client-basic".
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    await page.locator("#entitySelect").selectOption("client-basic");

    // The corresponding search-row carries the is-active class via the
    // document-level `cts-plan-selected` listener that writes
    // `planSearch.selected = e.detail.plan.planName`.
    const targetRow = page.locator(
      '#planSearch [data-plan-name="oidcc-client-basic-certification-test-plan"]',
    );
    await expect(targetRow).toHaveClass(/is-active/, { timeout: 5000 });

    // The bridge sets one `selected` planName at a time. A regression that
    // marked every family-matching row active (e.g. by reading the spec
    // family instead of the plan name) would still pass the assertion
    // above; pin the count to exactly one to catch that shape.
    await expect(page.locator("#planSearch .oidf-test-selector__row.is-active")).toHaveCount(1);
  });

  test("?test_plan= deep-link resolves AND highlights the search row (regression)", async ({
    page,
  }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html?test_plan=oidcc-client-basic-certification-test-plan");

    // The cascade resolves the deep-link via selectPlanByName (the inline
    // wrapper that bumps isSystemSelectingPlan around the cascade call).
    await expect(page.locator("#planSelect")).toHaveValue(
      "oidcc-client-basic-certification-test-plan",
      { timeout: 5000 },
    );

    // Both entry points reflect the same selection.
    const targetRow = page.locator(
      '#planSearch [data-plan-name="oidcc-client-basic-certification-test-plan"]',
    );
    await expect(targetRow).toHaveClass(/is-active/);
  });

  test("R42: cts-form-field renders a label[for] / id pair for every visible field", async ({
    page,
  }) => {
    // After Phase 2, every rendered field is a <cts-form-field>, which owns
    // its own <label for="cts-ff-N"> + matching id on the inner control. The
    // legacy wireConfigFormLabels DOM-walk is gone. This test guards that
    // contract end-to-end on the live page (cts-form-field's own Storybook
    // play tests cover it at the unit level).
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    // cts-form-field elements only render after the cascade reaches a plan
    // AND every variant is selected (updateConfigFieldVisibility gates the
    // schema bind on allVariantsSelected). Pick the no-variants OIDCC plan
    // so the form renders immediately after the cascade resolves.
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    await page.locator("#entitySelect").selectOption("client-basic");
    await expect(page.locator('cts-form-field[name="alias"]').first()).toBeAttached({
      timeout: 5000,
    });

    // For every rendered field, the inner control must have an id AND a
    // matching label[for=…]. cts-form-field renders the label inside its
    // light DOM with the same uid that's pinned on the control.
    const samples = await page.evaluate(() => {
      const fields = Array.from(document.querySelectorAll("cts-form-field"));
      return fields.map((field) => {
        const control = field.querySelector("input, select, textarea");
        const id = control?.id || "";
        const labelEl = id ? field.querySelector(`label[for="${id}"]`) : null;
        return {
          name: field.getAttribute("name"),
          id,
          labelText: labelEl?.textContent?.trim() || "",
        };
      });
    });
    expect(samples.length).toBeGreaterThan(0);
    for (const s of samples) {
      expect(s.id, `expected id on cts-form-field[name=${s.name}]`).toBeTruthy();
    }
    // boolean fields render the description as the inline checkbox label
    // instead of a header label, so an empty labelText is permissible on
    // boolean controls but not on string/select/textarea ones. Sanity-check
    // that the schedule-test catalog (no boolean fields today) lands every
    // sample with non-empty label text.
    const blankLabels = samples.filter((s) => !s.labelText).map((s) => s.name);
    expect(blankLabels, `expected non-empty labels for ${blankLabels.join(", ")}`).toEqual([]);
    // Ids are unique even when the same dotted-path appears in multiple
    // sections (cts-form-field's uidCounter pins a monotonic counter).
    const ids = samples.map((s) => s.id).filter(Boolean);
    const dupes = ids.filter((id, i) => ids.indexOf(id) !== i);
    expect(dupes, "expected every cts-form-field control id to be unique").toEqual([]);
  });

  test.describe("unsaved changes guard", () => {
    /**
     * Bring the page up to a state where the config form is rendered and at
     * least one field is editable, but do not yet edit anything.
     *
     * The helper also pre-registers the `plans.html` mock route used by
     * link-click tests in this describe block. Per project convention all
     * `page.route()` calls must run before `page.goto()`; registering the
     * route per-test after `bootScheduleTestPage()` would fail that gate.
     * @param {import("@playwright/test").Page} page - Playwright page fixture
     */
    async function bootScheduleTestPage(page) {
      await setupFailFast(page);
      await page.route("**/api/plan/available", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(ALL_PLANS),
        }),
      );
      await page.route("**/api/lastconfig", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({}),
        }),
      );
      await page.route("**/plans.html*", (route) =>
        route.fulfill({
          status: 200,
          contentType: "text/html",
          body: "<!doctype html><html><body>ok</body></html>",
        }),
      );
      await setupCommonRoutes(page);
      await page.goto("/schedule-test.html");
      await page.locator("#specFamilySelect").selectOption("OIDCC");
      const entitySelect = page.locator("#entitySelect");
      await expect(entitySelect).toBeVisible();
      await entitySelect.selectOption("client-basic");
      await expect(page.locator("#createPlanBtn button")).toBeEnabled({ timeout: 5000 });
    }

    /**
     * Synthesize the cts-config-change event that cts-config-form would
     * dispatch on any field edit. Decouples the test from the field catalog
     * layout while exercising the exact code path the guard listens for.
     * @param {import("@playwright/test").Page} page
     */
    async function armGuardDirty(page) {
      await page.evaluate(() => {
        document.getElementById("ctsConfigForm")?.dispatchEvent(
          new CustomEvent("cts-config-change", {
            bubbles: true,
            detail: { config: { alias: "edited" } },
          }),
        );
      });
      await expect(page.locator("cts-unsaved-changes-guard")).toHaveAttribute("dirty", "");
    }

    test("pristine form: internal link click navigates without prompt", async ({ page }) => {
      await bootScheduleTestPage(page);
      await expect(page.locator("cts-unsaved-changes-guard")).not.toHaveAttribute("dirty", "");

      await page.locator('cts-navbar a[href="plans.html"]').first().click();
      await page.waitForURL(/plans\.html$/);
      await expect(
        page.locator("cts-unsaved-changes-guard cts-modal dialog.oidf-modal[open]"),
      ).toHaveCount(0);
    });

    test("dirty form: internal link click opens the unsaved-changes modal", async ({ page }) => {
      await bootScheduleTestPage(page);
      await armGuardDirty(page);

      await page.locator('cts-navbar a[href="plans.html"]').first().click();

      const dialog = page.locator("cts-unsaved-changes-guard cts-modal dialog.oidf-modal[open]");
      await expect(dialog).toBeVisible();
      await expect(dialog.locator(".oidf-modal-title")).toHaveText("You have unsaved changes");
    });

    test("Stay on page keeps the user and leaves the form dirty", async ({ page }) => {
      await bootScheduleTestPage(page);
      await armGuardDirty(page);

      const beforeUrl = page.url();
      await page.locator('cts-navbar a[href="plans.html"]').first().click();
      await expect(
        page.locator("cts-unsaved-changes-guard cts-modal dialog.oidf-modal[open]"),
      ).toBeVisible();

      await page.locator("#exitGuard-modal-stay").click();
      await expect(
        page.locator("cts-unsaved-changes-guard cts-modal dialog.oidf-modal[open]"),
      ).toHaveCount(0);
      expect(page.url()).toBe(beforeUrl);
      await expect(page.locator("cts-unsaved-changes-guard")).toHaveAttribute("dirty", "");
    });

    test("Leave page navigates to the link target", async ({ page }) => {
      await bootScheduleTestPage(page);
      await armGuardDirty(page);

      await page.locator('cts-navbar a[href="plans.html"]').first().click();
      await expect(
        page.locator("cts-unsaved-changes-guard cts-modal dialog.oidf-modal[open]"),
      ).toBeVisible();

      await page.locator("#exitGuard-modal-leave").click();
      await page.waitForURL(/plans\.html$/);
    });

    test("Create Test Plan does not trigger the unsaved-changes modal", async ({ page }) => {
      let postCalled = false;

      await setupFailFast(page);
      await page.route("**/api/plan/available", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(ALL_PLANS),
        }),
      );
      await page.route("**/api/lastconfig", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({}),
        }),
      );
      await page.route("**/api/plan?*", (route) => {
        if (route.request().method() === "POST") {
          postCalled = true;
          return route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              id: "plan-guard-001",
              name: "oidcc-client-basic-certification-test-plan",
            }),
          });
        }
        return route.fallback();
      });
      await page.route("**/plan-detail.html*", (route) =>
        route.fulfill({
          status: 200,
          contentType: "text/html",
          body: "<!doctype html><html><body>ok</body></html>",
        }),
      );
      await setupCommonRoutes(page);

      await page.goto("/schedule-test.html");
      await page.locator("#specFamilySelect").selectOption("OIDCC");
      await page.locator("#entitySelect").selectOption("client-basic");
      await expect(page.locator("#createPlanBtn button")).toBeEnabled({ timeout: 5000 });

      await armGuardDirty(page);

      await page.locator("#createPlanBtn").click();
      await page.waitForURL(/plan-detail\.html\?plan=plan-guard-001$/);
      expect(postCalled).toBe(true);
      await expect(
        page.locator("cts-unsaved-changes-guard cts-modal dialog.oidf-modal[open]"),
      ).toHaveCount(0);
    });

    test("modifier-key click on internal link is not intercepted", async ({ page }) => {
      await bootScheduleTestPage(page);
      await armGuardDirty(page);

      await page
        .locator('cts-navbar a[href="plans.html"]')
        .first()
        .click({ modifiers: ["Meta"] });
      await expect(
        page.locator("cts-unsaved-changes-guard cts-modal dialog.oidf-modal[open]"),
      ).toHaveCount(0);
    });

    test("dirty form: window beforeunload event has its default prevented", async ({ page }) => {
      // Closes the P1 testing gap from
      // docs/residual-review-findings/2026-05-18-dirty-form-exit-guard.md —
      // the _onBeforeUnload branch of cts-unsaved-changes-guard had no
      // integration coverage before this test.
      //
      // The residual file's fix recipe (page.reload() + page.on('dialog',
      // dismiss)) does not surface in headless Chromium — neither
      // page.reload() nor page.close({runBeforeUnload:true}) reliably emits
      // a 'dialog' event for beforeunload in CI. The residual explicitly
      // permitted test.skip in that case, but a real integration assertion
      // is more valuable: dispatch a real beforeunload event on window and
      // verify the guard's handler called preventDefault. This exercises:
      //  - the connectedCallback's window.addEventListener wiring
      //  - the live `dirty` state on the component
      //  - the _onBeforeUnload branch that runs preventDefault + returnValue
      // The browser-prompt UI side is a Chromium implementation detail that
      // headless mode does not expose; the handler contract is what matters.
      await bootScheduleTestPage(page);
      await armGuardDirty(page);

      const defaultPrevented = await page.evaluate(() => {
        const event = new Event("beforeunload", { cancelable: true });
        window.dispatchEvent(event);
        return event.defaultPrevented;
      });

      // `defaultPrevented === true` proves the handler ran inside the
      // dirty-state branch and called `event.preventDefault()` — the
      // signal modern browsers honour to fire the unsaved-changes prompt.
      expect(defaultPrevented).toBe(true);
    });

    test("pristine form: window beforeunload event is not prevented", async ({ page }) => {
      // Companion to the dirty-form beforeunload test — confirms the guard
      // does not preventDefault on the unload event when the form has not
      // been edited, so the browser would proceed with the unload normally.
      await bootScheduleTestPage(page);
      await expect(page.locator("cts-unsaved-changes-guard")).not.toHaveAttribute("dirty", "");

      const result = await page.evaluate(() => {
        const event = new Event("beforeunload", { cancelable: true });
        window.dispatchEvent(event);
        return { defaultPrevented: event.defaultPrevented };
      });

      expect(result.defaultPrevented).toBe(false);
    });
  });
});
