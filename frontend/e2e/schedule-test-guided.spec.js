import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS, MOCK_GUIDED_PLANS } from "./fixtures/mock-plans.js";

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
