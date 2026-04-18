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

    // The button is hidden (display:none on parent) when no plan is selected.
    // Use evaluate to invoke the onclick handler directly — it checks
    // planSelect.value and shows an error modal.
    await page.evaluate(() => {
      const btn = document.getElementById("createPlanBtn");
      btn.removeAttribute("disabled");
      btn.style.display = "";
      btn.closest("#launchButtons").style.display = "";
      btn.click();
    });

    // Error modal should appear with "select a test plan" message
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    await expect(page.locator("#errorMessage")).toContainText("select a test plan");

    // Close the error modal
    await errorModal.locator('[data-bs-dismiss="modal"]').first().click();
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

    // When /api/plan/available returns a 500, the page does not show an error
    // message — response.json() parses the body and _.keyBy produces an empty
    // plan index, so the family select stays unpopulated. This test verifies
    // the page degrades gracefully (no JS errors, no crash) rather than
    // explicit error handling.
    const familySelect = page.locator("#specFamilySelect");
    const optionCount = familySelect.locator("option");
    await expect(optionCount).toHaveCount(1); // Only the "--- Select ---" default

    // Create button should remain disabled (no plan can be selected).
    // Targets the inner native button — see note in the R10 test.
    await expect(page.locator("#createPlanBtn button")).toBeDisabled();
  });
});
