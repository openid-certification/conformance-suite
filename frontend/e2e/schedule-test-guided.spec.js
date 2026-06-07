import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS, MOCK_GUIDED_PLANS } from "./fixtures/mock-plans.js";
import { MOCK_PLAN_DETAIL } from "./fixtures/mock-test-data.js";

const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS, ...MOCK_GUIDED_PLANS];

/**
 * Guided-mode coverage for schedule-test.html: the persistent
 * Guided | Advanced toggle, the mode-resolution ladder's user-visible
 * behavior, and (in later units) the guided journey itself.
 *
 * The advanced surface keeps its own coverage in schedule-test.spec.js,
 * which forces `oidf-guided-mode=advanced` up front; this file owns the
 * guided default and the switching behavior.
 */

/**
 * Register the routes the schedule-test init chain always hits, regardless
 * of mode: plans catalog, lastconfig probe, and the common trio.
 *
 * @param {import('@playwright/test').Page} page
 * @param {object} [options]
 * @param {Array<object>} [options.plans]
 * @param {object|null} [options.user] - Forwarded to setupCommonRoutes (null → 401).
 */
async function setupScheduleTestRoutes(page, options = {}) {
  await setupFailFast(page);
  await page.route("**/api/plan/available", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(options.plans || ALL_PLANS),
    }),
  );
  await page.route("**/api/lastconfig", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({}) }),
  );
  await setupCommonRoutes(page, options.user !== undefined ? { user: options.user } : {});
}

test.describe("schedule-test.html — Guided | Advanced mode toggle", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("first visit (no stored preference) lands in guided mode", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");

    await expect(page.locator("#guidedIsland")).toBeVisible();
    await expect(page.locator("#scheduleTestPage")).toBeHidden();
    await expect(page.locator("#modeGuidedBtn")).toHaveAttribute("aria-pressed", "true");
    await expect(page.locator("#modeAdvancedBtn")).toHaveAttribute("aria-pressed", "false");
  });

  test("toggle to advanced persists across a reload; toggling back restores guided", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await expect(page.locator("#guidedIsland")).toBeVisible();

    await page.locator("#modeAdvancedBtn").click();
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    await expect(page.locator("#guidedIsland")).toBeHidden();
    await expect(page.locator("#modeAdvancedBtn")).toHaveAttribute("aria-pressed", "true");

    // The explicit switch persisted — a reload stays in advanced.
    await page.reload();
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    await expect(page.locator("#guidedIsland")).toBeHidden();

    // And the toggle is symmetric.
    await page.locator("#modeGuidedBtn").click();
    await expect(page.locator("#guidedIsland")).toBeVisible();
    await expect(page.locator("#scheduleTestPage")).toBeHidden();
    await page.reload();
    await expect(page.locator("#guidedIsland")).toBeVisible();
  });

  test("?test_plan= deep-link forces advanced for a stored-guided user and applies the preset", async ({
    page,
  }) => {
    await page.addInitScript(() => {
      try {
        localStorage.setItem("oidf-guided-mode", "guided");
      } catch {
        /* storage unavailable — the test will surface it */
      }
    });
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html?test_plan=oidcc-basic-certification-test-plan");

    // Advanced island shown, guided untouched (R9 forcing).
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    await expect(page.locator("#guidedIsland")).toBeHidden();
    await expect(page.locator("#modeAdvancedBtn")).toHaveAttribute("aria-pressed", "true");

    // The advanced hydration ran: the cascade resolved the deep-linked plan.
    await expect(page.locator("#planSelect")).toHaveValue("oidcc-basic-certification-test-plan");

    // Deep-link forcing is transient — it must NOT overwrite the stored
    // preference. A plain reload returns the user to guided.
    await page.goto("/schedule-test.html");
    await expect(page.locator("#guidedIsland")).toBeVisible();
    await expect(page.locator("#scheduleTestPage")).toBeHidden();
  });

  test("mode switch moves focus to the revealed island", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await expect(page.locator("#guidedIsland")).toBeVisible();

    await page.locator("#modeAdvancedBtn").click();
    await expect(page.locator("#scheduleTestPage")).toBeFocused();

    await page.locator("#modeGuidedBtn").click();
    // The guided island's stage heading is the focus target when present.
    await expect(page.locator("#guidedIsland h1")).toBeFocused();
  });
});

/**
 * Click a guided choice card by its data-choice id.
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} choiceId
 */
async function pickChoice(page, choiceId) {
  await page.locator(`#guidedStage .choice[data-choice="${choiceId}"]`).click();
}

/**
 * Walk KSA → OP → private_key_jwt → SAMA v2 (resolves to FAPI2 MS final).
 * @param {import('@playwright/test').Page} page
 */
async function walkKsaOpToReview(page) {
  await pickChoice(page, "ksa");
  await expect(page.locator("#guidedStage h1")).toHaveText("What is your role?");
  await pickChoice(page, "op");
  await expect(page.locator("#guidedStage h1")).toContainText("Client authentication method");
  await pickChoice(page, "pkjwt");
  await expect(page.locator("#guidedStage h1")).toContainText("Which version");
  await pickChoice(page, "ksav2");
  await expect(page.locator("#guidedStage h1")).toHaveText("Here's the plan we resolved");
}

test.describe("schedule-test.html — guided journey", () => {
  /** @type {string[]} */
  let consoleErrors;

  test.beforeEach(async ({ page }) => {
    consoleErrors = [];
    page.on("console", (msg) => {
      if (msg.type() === "error") consoleErrors.push(msg.text());
    });
  });

  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
    // Zero console errors across every journey walk (U3 verification).
    // Fail-fast aborts surface as console errors in OTHER specs; here all
    // routes are mocked, so any error is a real regression.
    expect(consoleErrors).toEqual([]);
  });

  test("happy path: KSA → OP → Private Key JWT → SAMA v2 → review", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");

    await expect(page.locator("#guidedStage h1")).toHaveText(
      "Which ecosystem are you certifying for?",
    );
    await walkKsaOpToReview(page);

    // Resolved plan card shows the catalog display name + machine name.
    await expect(page.locator("#guidedStage .plan-name-display")).toHaveText(
      "FAPI2-Message-Signing-Final: Authorization server test",
    );
    await expect(page.locator("#guidedStage .plan-name-code").first()).toHaveText(
      "fapi2-message-signing-final-test-plan",
    );

    // Read-only variant table: plain-language labels, no form controls.
    const table = page.locator("#guidedStage table.variant-table");
    await expect(table).toBeVisible();
    await expect(table.locator("tbody tr")).toHaveCount(7);
    await expect(table).toContainText("Sender Constraining");
    await expect(table).toContainText("Mutual TLS (mTLS)");
    await expect(table).toContainText("Signed (non-repudiation)");
    await expect(page.locator("#guidedStage select")).toHaveCount(0);

    // The trail carries every answer as a backtrack chip.
    await expect(page.locator("#guidedTrail .chip")).toHaveCount(4);
  });

  test("bundle: Brazil OP FAPI path shows the upfront checklist before review", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");

    await pickChoice(page, "open_finance_brazil");
    await pickChoice(page, "op");
    await expect(page.locator("#guidedStage h1")).toContainText("Which certification plan");
    await pickChoice(page, "fapi1_brazil_op");

    // The bundle checklist names the sibling BEFORE review.
    await expect(page.locator("#guidedStage h1")).toHaveText(
      "This certification needs 2 test plans",
    );
    const bundle = page.locator("#guidedStage .bundle-list");
    await expect(bundle.locator("li")).toHaveCount(2);
    await expect(bundle).toContainText("FAPI1-Advanced-Final: Authorization server test");
    await expect(bundle).toContainText("Dynamic Client Registration");

    // Continue lands on review, which repeats the checklist.
    await page.locator("#guidedStageActions").getByText("Continue with plan 1").click();
    await expect(page.locator("#guidedStage h1")).toHaveText("Here's the plan we resolved");
    await expect(page.locator("#guidedStage .bundle-list")).toContainText(
      "Dynamic Client Registration",
    );
  });

  test("backtrack: the ecosystem chip resets the journey to the ecosystem screen", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");

    await pickChoice(page, "ksa");
    await pickChoice(page, "op");
    await pickChoice(page, "pkjwt");
    // Ecosystem chip + two answered-question chips.
    await expect(page.locator("#guidedTrail .chip")).toHaveCount(3);

    // The ecosystem chip is the first one (idx -1).
    await page.locator("#guidedTrail .chip").first().click();
    await expect(page.locator("#guidedStage h1")).toHaveText(
      "Which ecosystem are you certifying for?",
    );
    // Downstream answers are gone.
    await expect(page.locator("#guidedTrail .chip")).toHaveCount(0);
  });

  test("skew dead-end: tree plan absent from the catalog → escape hatch, no config step", async ({
    page,
  }) => {
    // Serve a catalog WITHOUT fapi2-message-signing-final-test-plan so the
    // KSA SAMA-v2 leaf cannot resolve (R4).
    await setupScheduleTestRoutes(page, {
      plans: ALL_PLANS.filter((p) => p.planName !== "fapi2-message-signing-final-test-plan"),
    });
    await page.goto("/schedule-test.html");

    await pickChoice(page, "ksa");
    await pickChoice(page, "op");
    await pickChoice(page, "pkjwt");
    await pickChoice(page, "ksav2");

    await expect(page.locator("#guidedStage h1")).toHaveText(
      "This path isn't available on this server",
    );
    await expect(page.locator("#guidedStage")).toContainText(
      "fapi2-message-signing-final-test-plan",
    );
    // No config step is reachable; the escape hatch routes to advanced.
    await expect(page.locator("#guidedConfigMount")).toHaveCount(0);
    await page.locator("#guidedDeadEndEscape").click();
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    await expect(page.locator("#guidedIsland")).toBeHidden();
  });

  test("bridge: a resolved journey offers prefill on entering advanced; accept applies it", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await walkKsaOpToReview(page);

    await page.locator("#modeAdvancedBtn").click();
    const prompt = page.locator("#bridgePrompt");
    await expect(prompt).toBeVisible();
    await expect(prompt).toContainText("FAPI2-Message-Signing-Final: Authorization server test");

    await page.locator("#bridgeAcceptBtn").click();
    await expect(prompt).toBeHidden();
    await expect(page.locator("#planSelect")).toHaveValue("fapi2-message-signing-final-test-plan");
    // The journey's variant choices are overlaid onto the advanced selects.
    await expect(page.locator("#vp_sender_constrain")).toHaveValue("mtls");
    await expect(page.locator("#vp_client_auth_type")).toHaveValue("private_key_jwt");
    await expect(page.locator("#vp_fapi_profile")).toHaveValue("ksa");
    // All variants resolved → the advanced create button lights up.
    await expect(page.locator("#createPlanBtn")).toBeEnabled();
  });

  test("bridge: decline leaves advanced untouched and is remembered per plan", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await walkKsaOpToReview(page);

    await page.locator("#modeAdvancedBtn").click();
    await expect(page.locator("#bridgePrompt")).toBeVisible();
    await page.locator("#bridgeDeclineBtn").click();
    await expect(page.locator("#bridgePrompt")).toBeHidden();
    // Advanced untouched: the cascade's plan select stays unselected (the
    // element exists at rest with an empty value).
    await expect(page.locator("#planSelect")).toHaveValue("");

    // The decline is remembered for this plan: round-trip the toggle and
    // the offer does not repeat.
    await page.locator("#modeGuidedBtn").click();
    await expect(page.locator("#guidedIsland")).toBeVisible();
    await page.locator("#modeAdvancedBtn").click();
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    await expect(page.locator("#bridgePrompt")).toBeHidden();
  });

  test("escape hatch: ecosystem screen routes to advanced; journey intact on switch back", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await expect(page.locator("#guidedStage h1")).toHaveText(
      "Which ecosystem are you certifying for?",
    );

    await page.locator("#guidedBrowseAll").click();
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    await expect(page.locator("#guidedIsland")).toBeHidden();

    // Switching back shows the journey exactly where it was left (R8).
    await page.locator("#modeGuidedBtn").click();
    await expect(page.locator("#guidedStage h1")).toHaveText(
      "Which ecosystem are you certifying for?",
    );
  });

  test("keyboard: arrows + Enter advance the journey (radiogroup model)", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await expect(page.locator("#guidedStage h1")).toHaveText(
      "Which ecosystem are you certifying for?",
    );

    // Focus the first radio, arrow to KSA (7th card → 6 presses), commit.
    await page.locator('#guidedStage input[name="guidedChoiceGroup"]').first().focus();
    for (let i = 0; i < 6; i++) {
      await page.keyboard.press("ArrowDown");
    }
    await page.keyboard.press("Enter");
    await expect(page.locator("#guidedStage h1")).toHaveText("What is your role?");

    // Arrow to OP and commit with Space.
    await page.locator('#guidedStage input[name="guidedChoiceGroup"]').first().focus();
    await page.keyboard.press("ArrowDown");
    await page.keyboard.press(" ");
    await expect(page.locator("#guidedStage h1")).toContainText("Client authentication method");
  });
});

test.describe("schedule-test.html — guided config + create", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  /**
   * Walk to the guided config step (KSA path — no siblings, so no handoff
   * record interferes) and assert the real config form rendered.
   *
   * @param {import('@playwright/test').Page} page
   */
  async function walkToConfigStep(page) {
    await walkKsaOpToReview(page);
    await page.locator("#guidedStageActions").getByText("Configure this plan").click();
    await expect(page.locator("#guidedStage h1")).toHaveText("Configure your test");
    await expect(page.locator("#guidedConfigForm")).toBeVisible();
  }

  test("create: POST /api/plan with variant + JSON body, then redirect to plan-detail", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    // POST /api/plan → created id; the wildcard must fall back for GETs.
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "plan-guided-001" }),
        });
      }
      return route.fallback();
    });
    // plan-detail.html loads after the redirect.
    await page.route("**/api/plan/plan-guided-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...MOCK_PLAN_DETAIL,
          _id: "plan-guided-001",
          planName: "fapi2-message-signing-final-test-plan",
        }),
      }),
    );
    await setupTestInfoRoute(page);

    await page.goto("/schedule-test.html");
    await walkToConfigStep(page);

    // Type into the real cts-config-form instance so cts-config-change
    // updates the guided config island (programmatic .config would not).
    await page.locator("#guidedConfigForm").getByLabel("alias", { exact: true }).fill("guided-e2e");

    const planRequest = page.waitForRequest(
      (req) => req.url().includes("/api/plan?") && req.method() === "POST",
    );
    await page.locator("#guidedCreateBtn").click();

    const req = await planRequest;
    const url = new URL(req.url());
    expect(url.searchParams.get("planName")).toBe("fapi2-message-signing-final-test-plan");
    const variantJson = JSON.parse(url.searchParams.get("variant") || "{}");
    expect(variantJson.client_auth_type).toBe("private_key_jwt");
    expect(variantJson.sender_constrain).toBe("mtls");
    expect(variantJson.fapi_profile).toBe("ksa");
    expect(req.postDataJSON()).toMatchObject({ alias: "guided-e2e" });

    await page.waitForURL("**/plan-detail.html?plan=plan-guided-001");
  });

  test("create failure: inline error on the config step, journey intact, recovery record present", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 500,
          contentType: "application/json",
          body: JSON.stringify({ message: "alias is already in use by another user" }),
        });
      }
      return route.fallback();
    });

    await page.goto("/schedule-test.html");
    await walkToConfigStep(page);
    await page.locator("#guidedConfigForm").getByLabel("alias", { exact: true }).fill("dupe");
    await page.locator("#guidedCreateBtn").click();

    // Inline error, normalized through the same path as the advanced modal.
    const errorBox = page.locator("#guidedConfigError cts-alert");
    await expect(errorBox).toBeVisible();
    await expect(errorBox).toContainText("HTTP 500");
    await expect(errorBox).toContainText("alias is already in use by another user");

    // Journey intact: still on the config step, answers preserved.
    await expect(page.locator("#guidedStage h1")).toHaveText("Configure your test");
    await expect(page.locator("#guidedTrail .chip")).toHaveCount(4);

    // Recovery record written before the POST (R5).
    const record = await page.evaluate(() =>
      JSON.parse(sessionStorage.getItem("oidf-guided-recovery") || "null"),
    );
    expect(record).toMatchObject({
      ecosystemId: "ksa",
      planName: "fapi2-message-signing-final-test-plan",
      config: { alias: "dupe" },
    });
    expect(record.answers).toEqual(["op", "pkjwt", "ksav2"]);
  });

  test("recovery: a reload after a failed create re-enters guided at the config step", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({ status: 500, contentType: "text/plain", body: "boom" });
      }
      return route.fallback();
    });

    await page.goto("/schedule-test.html");
    await walkToConfigStep(page);
    await page.locator("#guidedConfigForm").getByLabel("alias", { exact: true }).fill("recover-me");
    await page.locator("#guidedCreateBtn").click();
    await expect(page.locator("#guidedConfigError cts-alert")).toBeVisible();

    await page.reload();

    // Straight back to the config step with the journey + values restored.
    await expect(page.locator("#guidedStage h1")).toHaveText("Configure your test");
    await expect(page.locator("#guidedTrail .chip")).toHaveCount(4);
    await expect(
      page.locator("#guidedConfigForm").getByLabel("alias", { exact: true }),
    ).toHaveValue("recover-me");
  });

  test("anonymous: the config step shows a sign-in prompt instead of the create button (R6)", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page, { user: null });

    await page.goto("/schedule-test.html");
    await walkToConfigStep(page);

    await expect(page.locator("#guidedSignInPrompt")).toBeVisible();
    await expect(page.locator("#guidedSignInPrompt a")).toHaveAttribute("href", "/login.html");
    await expect(page.locator("#guidedCreateBtn")).toHaveCount(0);
    // The journey itself stayed browsable all the way here.
    await expect(page.locator("#guidedConfigForm")).toBeVisible();
  });

  test("guard isolation: dirty advanced form never prompts on guided clicks; guided create redirects unprompted", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "plan-guided-002" }),
        });
      }
      return route.fallback();
    });
    await page.route("**/api/plan/plan-guided-002", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...MOCK_PLAN_DETAIL,
          _id: "plan-guided-002",
          planName: "fapi2-message-signing-final-test-plan",
        }),
      }),
    );
    await setupTestInfoRoute(page);

    await page.goto("/schedule-test.html");

    // Dirty the ADVANCED island's form (arms cts-unsaved-changes-guard).
    await page.locator("#modeAdvancedBtn").click();
    await page.evaluate(() => {
      document.getElementById("ctsConfigForm")?.dispatchEvent(
        new CustomEvent("cts-config-change", {
          bubbles: true,
          detail: { config: { alias: "advanced-edit" } },
        }),
      );
    });
    await expect(page.locator("cts-unsaved-changes-guard")).toHaveAttribute("dirty", "");

    // Guided navigation is button-based — the guard's link interceptor
    // must never engage while clicking through the journey.
    await page.locator("#modeGuidedBtn").click();
    await walkToConfigStep(page);
    await expect(
      page.locator("cts-unsaved-changes-guard cts-modal dialog.oidf-modal[open]"),
    ).toHaveCount(0);

    // Dirty the GUIDED config, then create: the guided beforeunload check
    // disarms before the redirect, so navigation completes unprompted (an
    // armed beforeunload would abort the auto-dismissed dialog).
    await page.locator("#guidedConfigForm").getByLabel("alias", { exact: true }).fill("g");
    await page.locator("#guidedCreateBtn").click();
    await page.waitForURL("**/plan-detail.html?plan=plan-guided-002");
  });
});
