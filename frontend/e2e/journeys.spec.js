import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS } from "./fixtures/mock-plans.js";
import { MOCK_PLAN_DETAIL, MOCK_TEST_STATUS } from "./fixtures/mock-test-data.js";
import { MOCK_LOG_ENTRIES } from "./fixtures/mock-log-entries.js";

const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

test.describe("Cross-page journeys", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("schedule → plan-detail → log-detail journey (R21)", async ({ page }) => {
    // Register ALL routes before first navigation — they persist across page loads
    await setupFailFast(page);

    // --- Schedule-test routes ---
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

    // POST /api/plan — create a new plan, return plan ID
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "plan-journey-001" }),
        });
      }
      return route.fallback();
    });

    // --- Plan-detail routes ---
    await page.route("**/api/plan/plan-journey-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...MOCK_PLAN_DETAIL,
          _id: "plan-journey-001",
          planName: "oidcc-client-basic-certification-test-plan",
          modules: [
            {
              testModule: "oidcc-client-test",
              testSummary: "Client-side test",
              variant: {},
              instances: ["test-journey-001"],
            },
          ],
          config: { "server.issuer": "https://op.example.com" },
        }),
      }),
    );

    // POST /api/runner — run a test, return test ID
    await page.route("**/api/runner?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "test-journey-001", name: "oidcc-client-test" }),
        });
      }
      return route.fallback();
    });

    // --- Log-detail routes ---
    await page.route("**/api/log/test-journey-001**", (route) => {
      const url = new URL(route.request().url());
      const since = url.searchParams.get("since");
      if (since && Number(since) > 0) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([]),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_LOG_ENTRIES),
      });
    });

    await page.route("**/api/runner/test-journey-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: "test-journey-001",
          name: "oidcc-client-test",
          status: "FINISHED",
          created: new Date().toISOString(),
          updated: new Date().toISOString(),
          owner: { sub: "12345", iss: "https://accounts.google.com" },
        }),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-journey-001": {
        ...MOCK_TEST_STATUS,
        _id: "test-journey-001",
        testId: "test-journey-001",
        testName: "oidcc-client-test",
        planId: "plan-journey-001",
      },
    });
    // log-detail.html calls /api/uploaded-images on load; mock empty so
    // fail-fast doesn't trip when the journey lands there.
    await page.route("**/api/uploaded-images*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );
    await setupCommonRoutes(page);

    // === Step 1: schedule-test.html — navigate cascade and create plan ===
    await page.goto("/schedule-test.html");

    await page.locator("#specFamilySelect").selectOption("OIDCC");
    await page.locator("#entitySelect").selectOption("client-basic");
    await expect(page.locator("#planSelect")).toBeVisible();

    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });
    await createBtn.click();

    // === Step 2: Redirected to plan-detail.html ===
    await page.waitForURL("**/plan-detail.html?plan=plan-journey-001");
    await expect(page.locator("#planDetailHeader")).toContainText(
      "oidcc-client-basic-certification-test-plan",
    );

    // Verify module list rendered (cts-plan-modules .module-row, post-redesign).
    const moduleRows = page.locator("cts-plan-modules .module-row");
    await expect(moduleRows).toHaveCount(1);
    await expect(moduleRows.first()).toContainText("oidcc-client-test");

    // === Step 3: Click Run Test → redirected to log-detail.html ===
    const runBtn = page.locator(".startBtn").first();
    await expect(runBtn).toBeVisible();
    await runBtn.click();

    await page.waitForURL("**/log-detail.html?log=test-journey-001");

    // Verify log-detail loaded with correct test
    await expect(page.locator("#logHeader")).toContainText("oidcc-client-test");
    await expect(page.locator("#logHeader")).toContainText("test-journey-001");

    // Verify log entries rendered
    const logEntries = page.locator(".logItem");
    await expect(logEntries.first()).toBeVisible();
  });

  test("plan-detail → log-detail journey (R22)", async ({ page }) => {
    await setupFailFast(page);

    // Plan-detail routes
    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    // POST /api/runner — run a test from plan-detail
    let runnerPostCalled = false;
    await page.route("**/api/runner?*", (route) => {
      if (route.request().method() === "POST") {
        runnerPostCalled = true;
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "test-run-001", name: "oidcc-server" }),
        });
      }
      return route.fallback();
    });

    // Log-detail routes
    await page.route("**/api/log/test-run-001**", (route) => {
      const url = new URL(route.request().url());
      const since = url.searchParams.get("since");
      if (since && Number(since) > 0) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([]),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_LOG_ENTRIES),
      });
    });

    await page.route("**/api/runner/test-run-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: "test-run-001",
          name: "oidcc-server",
          status: "FINISHED",
          created: new Date().toISOString(),
          updated: new Date().toISOString(),
          owner: { sub: "12345", iss: "https://accounts.google.com" },
        }),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-run-001": {
        ...MOCK_TEST_STATUS,
        _id: "test-run-001",
        testId: "test-run-001",
        planId: "plan-abc-123",
      },
    });
    // log-detail.html calls /api/uploaded-images on load; mock empty so
    // fail-fast doesn't trip when the journey lands there.
    await page.route("**/api/uploaded-images*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );
    await setupCommonRoutes(page);

    // === Step 1: plan-detail.html ===
    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Verify plan loaded
    await expect(page.locator("#planDetailHeader")).toContainText(
      "oidcc-basic-certification-test-plan",
    );

    // Click "Run Test" on the first module
    const runBtn = page.locator(".startBtn").first();
    await expect(runBtn).toBeVisible();
    await runBtn.click();

    // === Step 2: Redirected to log-detail.html ===
    await page.waitForURL("**/log-detail.html?log=test-run-001");
    expect(runnerPostCalled).toBe(true);

    // Verify log-detail loaded with correct test
    await expect(page.locator("#logHeader")).toContainText("oidcc-server");
    await expect(page.locator("#logHeader")).toContainText("test-run-001");
  });

  /**
   * Cross-page journey for the error path: schedule-test → POST /api/plan 400
   * → user stays on schedule-test, #errorModal shown, cascade state intact.
   * Complements the single-page error-branch spec in error-paths.spec.js
   * with the full user-journey framing.
   */
  test("plan creation fails → user stays on schedule-test with error (R22)", async ({ page }) => {
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

    // POST /api/plan fails with a 400.
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({ error: "invalid variant selection" }),
        });
      }
      return route.fallback();
    });

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Fill the cascade to a submittable state using the no-variants plan.
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    const entitySelect = page.locator("#entitySelect");
    await expect(entitySelect).toBeVisible();
    await entitySelect.selectOption("client-basic");

    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });
    await createBtn.click();

    // Error modal appears; URL has NOT changed to /plan-detail.html.
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    await expect(page).toHaveURL(/\/schedule-test\.html/);

    // Cascade state is preserved so the user can fix + retry.
    await expect(page.locator("#specFamilySelect")).toHaveValue("OIDCC");
    await expect(entitySelect).toHaveValue("client-basic");

    // Dismissing the modal leaves the page functional.
    await errorModal.locator(".oidf-modal-close").first().click();
    await expect(errorModal).toBeHidden();
    await expect(createBtn).toBeEnabled();
  });

  test("sign out → pending state → login banner", async ({ page }) => {
    await setupFailFast(page);

    // plans.html list + status-box probes (plain JSON array, not DataTables).
    await page.route("**/api/plan*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([]),
      }),
    );

    // Spring's logout endpoint: a 302 to the banner URL. /logout is not
    // under /api/, so route it explicitly; fall back for non-POST methods
    // rather than narrowing the glob (route-helper convention — a broad
    // glob with fallback() is safe).
    await page.route("**/logout", (route) => {
      if (route.request().method() !== "POST") {
        return route.fallback();
      }
      return route.fulfill({
        status: 302,
        headers: { location: "/login.html?logout=true" },
      });
    });

    await setupCommonRoutes(page); // authenticated MOCK_USER + /api/server for the login.html footer

    await page.goto("/plans.html");

    // Scope to cts-navbar — nav links duplicate page-body links and trip
    // strict mode otherwise.
    const navbar = page.locator("cts-navbar");
    await navbar.locator(".cts-account-trigger").click();
    const account = navbar.locator(".cts-account");
    await expect(account).toHaveAttribute("data-open", "true");

    const signOut = navbar.locator(".cts-account-item--danger");
    await expect(signOut).toHaveText(/Sign out/);

    // Playwright locator polls cannot observe a frame once its navigation
    // is pending ("waiting for navigation to finish"), so the pending state
    // is captured from INSIDE the page instead: a probe listener stashes
    // the rendered state in sessionStorage, which survives the same-origin
    // navigation.
    //
    // Ordering invariant the probe depends on: the component's @submit
    // listener (registered at render time) runs BEFORE this probe listener
    // (registered later), and Lit queues its reactive-update microtask
    // synchronously inside that first listener. Microtasks are FIFO, so
    // Lit's re-render commits before the probe's queueMicrotask callback
    // reads the DOM. Verified against the vendored Lit bundle (single
    // microtask hop). If a Lit upgrade ever moves update scheduling off
    // the microtask queue, replace the probe with a polling check of the
    // button's disabled state instead.
    await page.evaluate(() => {
      const form = /** @type {HTMLFormElement} */ (
        document.querySelector("cts-navbar .cts-account-form")
      );
      form.addEventListener("submit", () => {
        queueMicrotask(() => {
          const navbarEl = /** @type {HTMLElement} */ (document.querySelector("cts-navbar"));
          const btn = /** @type {HTMLButtonElement} */ (
            navbarEl.querySelector(".cts-account-item--danger")
          );
          const accountEl = /** @type {HTMLElement} */ (navbarEl.querySelector(".cts-account"));
          sessionStorage.setItem(
            "cts-e2e:sign-out-pending-probe",
            JSON.stringify({
              disabled: btn.disabled,
              ariaBusy: btn.getAttribute("aria-busy"),
              label: btn.textContent,
              spinner: Boolean(navbarEl.querySelector(".cts-account-spinner")),
              menuOpen: accountEl.getAttribute("data-open"),
            }),
          );
        });
      });
    });

    await signOut.click();

    // The 302 lands on login.html with the banner trigger param, and the
    // "You have been logged out." confirmation renders.
    await page.waitForURL("**/login.html?logout=true");
    const banner = page.locator("cts-login-page .oidf-alert-info");
    await expect(banner).toBeVisible();
    await expect(banner).toContainText("You have been logged out.");

    // Pending state as the user saw it while the POST was in flight:
    // disabled button, busy semantics, swapped label, spinner, and the
    // dropdown still open.
    const probe = JSON.parse(
      await page.evaluate(() => sessionStorage.getItem("cts-e2e:sign-out-pending-probe") ?? "null"),
    );
    expect(probe).not.toBeNull();
    expect(probe.disabled).toBe(true);
    expect(probe.ariaBusy).toBe("true");
    expect(probe.label).toContain("Signing out…");
    expect(probe.spinner).toBe(true);
    expect(probe.menuOpen).toBe("true");
  });
});
