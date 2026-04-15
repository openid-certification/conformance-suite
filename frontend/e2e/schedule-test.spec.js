import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast } from "./helpers/routes.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS } from "./fixtures/mock-plans.js";

/** All available plans including the no-variants plan */
const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

test.describe("schedule-test.html — Test Plan Scheduling", () => {
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

  test("submission POSTs to /api/plan and redirects (R9)", async ({
    page,
  }) => {
    let postCalled = false;

    await setupFailFast(page);

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    // Mock POST /api/plan — return a new plan ID.
    // Registered BEFORE the more specific /api/plan/* routes below so
    // that Playwright (which checks last-registered first) tries the
    // specific routes before this glob — **/api/plan?* would otherwise
    // intercept /api/plan/available because ? is a single-char wildcard.
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        postCalled = true;
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "plan-new-001", name: "oidcc-client-basic-certification-test-plan" }),
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

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
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

  test("create button disabled until plan selected, shows error modal on click (R10)", async ({
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

    // Wait for the page init chain to complete — the onclick handler on
    // createPlanBtn is registered in loadScheduleTestPage(), which runs
    // after loadAvailablePlans() populates the specFamilySelect options.
    await expect(page.locator("#specFamilySelect option")).not.toHaveCount(1);

    // Create button should be disabled initially (no plan selected)
    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeDisabled();

    // Wait for the onclick handler to be registered by loadScheduleTestPage()
    await page.waitForFunction(
      () => document.getElementById("createPlanBtn")?.onclick !== null,
    );

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
    await expect(page.locator("#errorMessage")).toContainText(
      "select a test plan",
    );

    // Close the error modal
    await errorModal.locator('[data-bs-dismiss="modal"]').first().click();
    await expect(errorModal).not.toBeVisible();
  });

  test("error state when /api/plan/available returns 500 (R11)", async ({
    page,
  }) => {
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

    // The spec family select should have no populated options (only the blank default)
    const familySelect = page.locator("#specFamilySelect");
    const optionCount = await familySelect.locator("option").count();
    expect(optionCount).toBe(1); // Only the "--- Select ---" default
  });
});
