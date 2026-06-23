import { test, expect } from "@playwright/test";
import {
  setupScheduleTestRoutes,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS, MOCK_GUIDED_PLANS } from "./fixtures/mock-plans.js";
import { MOCK_USER } from "./fixtures/mock-users.js";
import { MOCK_PLAN_DETAIL } from "./fixtures/mock-test-data.js";

const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS, ...MOCK_GUIDED_PLANS];

/**
 * Guided-mode coverage for schedule-test.html: the persistent
 * Guided | Advanced toggle, the mode-resolution ladder's user-visible
 * behavior, and the guided journey itself.
 *
 * The advanced surface keeps its own coverage in schedule-test.spec.js,
 * which forces `oidf-guided-mode=advanced` up front; this file owns the
 * guided default and the switching behavior. Route setup lives in
 * helpers/routes.js (setupScheduleTestRoutes), shared with the Monaco spec.
 */

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

  test("review step actions render inside the sticky action bar", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await walkKsaOpToReview(page);

    // Regression: renderActionBar must render into the persistent
    // display:contents span that cts-action-bar adopted at first connect.
    // Replacing the HOST's children tears out the component's sticky
    // wrapper and leaves the buttons stacked in normal flow.
    const inner = page.locator("#guidedStageActions .oidf-action-bar__inner");
    await expect(inner.locator("cts-button")).toHaveCount(2);

    const bar = page.locator("#guidedStageActions .oidf-action-bar");
    await expect(bar).toHaveCSS("position", "fixed");
    // Let the 220ms slide-in animation settle before measuring geometry.
    await bar.evaluate((el) => Promise.all(el.getAnimations().map((a) => a.finished)));
    const barBox = await bar.boundingBox();
    const backBox = await page.locator("#guidedStageActions").getByText("Back").boundingBox();
    const configureBox = await page
      .locator("#guidedStageActions")
      .getByText("Configure this plan")
      .boundingBox();
    const viewport = page.viewportSize();
    if (!barBox || !backBox || !configureBox || !viewport) {
      throw new Error("action bar is missing a bounding box");
    }

    // Pinned to the viewport bottom.
    expect(Math.abs(barBox.y + barBox.height - viewport.height)).toBeLessThanOrEqual(1);
    // Back and Configure share a row (flex), not stacked blocks.
    expect(backBox.y).toBe(configureBox.y);
    expect(configureBox.x).toBeGreaterThan(backBox.x + backBox.width);
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

  test("ConnectID RP CIBA path resolves to the client CIBA plan", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");

    await pickChoice(page, "connectid_au");
    await pickChoice(page, "rp");
    await expect(page.locator("#guidedStage h1")).toContainText("Which certification plan");
    await pickChoice(page, "ciba");

    await expect(page.locator("#guidedStage h1")).toHaveText("Here's the plan we resolved");
    await expect(page.locator("#guidedStage .plan-name-display")).toHaveText(
      "FAPI-CIBA-ID1: Client test",
    );
    await expect(page.locator("#guidedStage .plan-name-code").first()).toHaveText(
      "fapi-ciba-id1-client-test-plan",
    );

    const table = page.locator("#guidedStage table.variant-table");
    await expect(table.locator("tbody tr")).toHaveCount(3);
    await expect(table).toContainText("Client Authentication Type");
    await expect(table).toContainText("Private Key JWT");
    await expect(table).toContainText("CIBA Mode");
    await expect(table).toContainText("Poll");
    await expect(table).toContainText("FAPI-CIBA Profile");
    await expect(table).toContainText("ConnectID Australia");
  });

  test("ConnectID OP CIBA path resolves to the server CIBA plan", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");

    await pickChoice(page, "connectid_au");
    await pickChoice(page, "op");
    await expect(page.locator("#guidedStage h1")).toContainText("Which certification plan");
    await pickChoice(page, "ciba");

    await expect(page.locator("#guidedStage h1")).toHaveText("Here's the plan we resolved");
    await expect(page.locator("#guidedStage .plan-name-display")).toHaveText(
      "FAPI-CIBA-ID1: Authorization server test",
    );
    await expect(page.locator("#guidedStage .plan-name-code").first()).toHaveText(
      "fapi-ciba-id1-test-plan",
    );

    const table = page.locator("#guidedStage table.variant-table");
    await expect(table.locator("tbody tr")).toHaveCount(4);
    await expect(table).toContainText("Client Authentication Type");
    await expect(table).toContainText("Private Key JWT");
    await expect(table).toContainText("FAPI-CIBA Profile");
    await expect(table).toContainText("ConnectID Australia");
    await expect(table).toContainText("CIBA Mode");
    await expect(table).toContainText("Poll");
    await expect(table).toContainText("Client Registration");
    await expect(table).toContainText("Static (pre-registered) client");
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
    await expect(page.locator("#guidedConfigForm")).toHaveCount(0);
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

    // Accepting runs the same arrival cue as picking the plan from the
    // search list (revealPlanSelection): the cascade scrolls into view and,
    // once the scroll settles, the selection group flashes...
    await expect
      .poll(
        async () =>
          page.locator("#selectionFlash").evaluate((el) => el.hasAttribute("data-flashing")),
        { timeout: 2000 },
      )
      .toBe(true);
    await expect
      .poll(
        async () =>
          page.locator("#specCascade").evaluate((el) => Math.round(el.getBoundingClientRect().top)),
        { timeout: 3000 },
      )
      .toBeLessThan(100);
    // ...and focus drops into the first variant <select> so the user can
    // carry straight on to configuring.
    await expect
      .poll(
        async () =>
          page.evaluate(() => {
            const active = document.activeElement;
            const firstVariantSelect = document.querySelector("#variantSelectors select");
            return !!firstVariantSelect && active === firstVariantSelect;
          }),
        { timeout: 3000 },
      )
      .toBe(true);
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
    // The toBeFocused() check guards against key presses landing before
    // focus settles under parallel-worker CPU contention.
    const firstRadio = page.locator('#guidedStage input[name="guidedChoiceGroup"]').first();
    await firstRadio.focus();
    await expect(firstRadio).toBeFocused();
    for (let i = 0; i < 6; i++) {
      await page.keyboard.press("ArrowDown");
    }
    await page.keyboard.press("Enter");
    await expect(page.locator("#guidedStage h1")).toHaveText("What is your role?");

    // Arrow to OP and commit with Space.
    const roleRadio = page.locator('#guidedStage input[name="guidedChoiceGroup"]').first();
    await roleRadio.focus();
    await expect(roleRadio).toBeFocused();
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

test.describe("schedule-test.html — wizard_preset replay + sibling loop", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  const BRAZIL_PRESET = {
    ecosystemId: "open_finance_brazil",
    answers: ["op"],
    completedPlanNames: ["fapi1-advanced-final-test-plan"],
  };

  test("replay: the preset lands in guided with ecosystem + role pre-answered, param stripped", async ({
    page,
  }) => {
    // Even with a stored ADVANCED preference, wizard_preset forces guided.
    await page.addInitScript(() => {
      localStorage.setItem("oidf-guided-mode", "advanced");
    });
    await setupScheduleTestRoutes(page);
    await page.goto(
      "/schedule-test.html?wizard_preset=" + encodeURIComponent(JSON.stringify(BRAZIL_PRESET)),
    );

    await expect(page.locator("#guidedIsland")).toBeVisible();
    // Ecosystem + role replayed; the user lands on the plan question.
    await expect(page.locator("#guidedStage h1")).toContainText("Which certification plan");
    await expect(page.locator("#guidedTrail .chip")).toHaveCount(2);
    await expect(page.locator("#guidedTrail")).toContainText("OpenFinance Brazil");

    // Consumed once: the param is stripped via replaceState.
    expect(new URL(page.url()).searchParams.get("wizard_preset")).toBeNull();
  });

  test("replay best-effort: an unresolvable hop drops the user at the last valid step", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    const preset = {
      ecosystemId: "ksa",
      answers: ["op", "vanished-choice"],
      completedPlanNames: [],
    };
    await page.goto(
      "/schedule-test.html?wizard_preset=" + encodeURIComponent(JSON.stringify(preset)),
    );

    // "op" replayed; the broken hop leaves the user at the client-auth step.
    await expect(page.locator("#guidedStage h1")).toContainText("Client authentication method");
    await expect(page.locator("#guidedTrail .chip")).toHaveCount(2);
  });

  test("malformed preset: guided opens at the ecosystem screen with a console warning only", async ({
    page,
  }) => {
    /** @type {string[]} */
    const errors = [];
    /** @type {string[]} */
    const warnings = [];
    page.on("console", (msg) => {
      if (msg.type() === "error") errors.push(msg.text());
      if (msg.type() === "warning") warnings.push(msg.text());
    });
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html?wizard_preset=garbage%7B%7B");

    await expect(page.locator("#guidedStage h1")).toHaveText(
      "Which ecosystem are you certifying for?",
    );
    expect(new URL(page.url()).searchParams.get("wizard_preset")).toBeNull();
    expect(errors).toEqual([]);
    expect(warnings.some((w) => w.includes("wizard_preset"))).toBe(true);
  });

  test("full Brazil OP loop: FAPI create → banner → DCR replay (no re-offer) → no further banner (R14)", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    // Create POSTs: first FAPI → plan-brazil-fapi, then DCR → plan-brazil-dcr.
    let createCount = 0;
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        createCount += 1;
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: createCount === 1 ? "plan-brazil-fapi" : "plan-brazil-dcr" }),
        });
      }
      return route.fallback();
    });
    await page.route("**/api/plan/plan-brazil-fapi*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...MOCK_PLAN_DETAIL,
          _id: "plan-brazil-fapi",
          planName: "fapi1-advanced-final-test-plan",
        }),
      }),
    );
    await page.route("**/api/plan/plan-brazil-dcr*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...MOCK_PLAN_DETAIL,
          _id: "plan-brazil-dcr",
          planName: "fapi1-advanced-final-brazil-dcr-test-plan",
        }),
      }),
    );
    await setupTestInfoRoute(page);

    // ── Leg 1: guided walk to the Brazil OP FAPI plan, create it. ──
    await page.goto("/schedule-test.html");
    await pickChoice(page, "open_finance_brazil");
    await pickChoice(page, "op");
    await pickChoice(page, "fapi1_brazil_op");
    // Bundle checklist names DCR before review.
    await expect(page.locator("#guidedStage h1")).toHaveText(
      "This certification needs 2 test plans",
    );
    await page.locator("#guidedStageActions").getByText("Continue with plan 1").click();
    await expect(page.locator("#guidedStage h1")).toHaveText("Here's the plan we resolved");
    await page.locator("#guidedStageActions").getByText("Configure this plan").click();
    await expect(page.locator("#guidedConfigForm")).toBeVisible();
    await page.locator("#guidedCreateBtn").click();
    await page.waitForURL("**/plan-detail.html?plan=plan-brazil-fapi");

    // ── Leg 2: the banner offers DCR and links back into guided. ──
    const banner = page.locator("#alsoRequiredBanner");
    await expect(banner).toBeVisible();
    await expect(banner).toContainText("Dynamic Client Registration");
    await banner.locator("a").click();
    await page.waitForURL("**/schedule-test.html*");

    // Replay: role pre-answered, plan question shown.
    await expect(page.locator("#guidedStage h1")).toContainText("Which certification plan");
    await pickChoice(page, "dcr_brazil_op");

    // R14: FAPI is already completed — DCR's back-reference must NOT
    // re-offer it, so there is no bundle step and no checklist section.
    await expect(page.locator("#guidedStage h1")).toHaveText("Here's the plan we resolved");
    await expect(page.locator("#guidedStage .bundle-list")).toHaveCount(0);

    // ── Leg 3: create DCR; the loop terminates with no further banner. ──
    await page.locator("#guidedStageActions").getByText("Configure this plan").click();
    await expect(page.locator("#guidedConfigForm")).toBeVisible();
    await page.locator("#guidedCreateBtn").click();
    await page.waitForURL("**/plan-detail.html?plan=plan-brazil-dcr");
    await expect(page.locator("#planDetailHeader")).toContainText("plan-brazil-dcr");
    await expect(page.locator("#alsoRequiredBanner")).toHaveCount(0);
    expect(await page.evaluate(() => sessionStorage.getItem("oidf-also-required"))).toBeNull();
  });
});

test.describe("schedule-test.html — guided hardening (review followup)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("guest viewer (isGuest) gets the sign-in prompt, not the create button (R6)", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page, { user: { ...MOCK_USER, isGuest: true } });
    await page.goto("/schedule-test.html");
    await walkKsaOpToReview(page);
    await page.locator("#guidedStageActions").getByText("Configure this plan").click();

    await expect(page.locator("#guidedSignInPrompt")).toBeVisible();
    await expect(page.locator("#guidedCreateBtn")).toHaveCount(0);
  });

  test("storage-unavailable: guided default still boots and the toggle still switches (R7)", async ({
    page,
  }) => {
    await page.addInitScript(() => {
      Object.defineProperty(window, "localStorage", {
        get() {
          throw new DOMException("denied", "SecurityError");
        },
      });
    });
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");

    // Falls back to the guided default without persisting.
    await expect(page.locator("#guidedIsland")).toBeVisible();
    await page.locator("#modeAdvancedBtn").click();
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    // No persistence — a reload lands back on the guided default.
    await page.reload();
    await expect(page.locator("#guidedIsland")).toBeVisible();
  });

  test("bridge accept skips variant values the plan does not declare", async ({ page }) => {
    // Serve a catalog where the KSA plan's sender_constrain lacks the
    // tree's "mtls" value — the overlay must leave that select alone.
    const plans = JSON.parse(
      JSON.stringify([...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS, ...MOCK_GUIDED_PLANS]),
    );
    const ksaPlan = plans.find((p) => p.planName === "fapi2-message-signing-final-test-plan");
    delete ksaPlan.variants.sender_constrain.variantValues.mtls;
    await setupScheduleTestRoutes(page, { plans });

    await page.goto("/schedule-test.html");
    await walkKsaOpToReview(page);
    await page.locator("#modeAdvancedBtn").click();
    await expect(page.locator("#bridgePrompt")).toBeVisible();
    await page.locator("#bridgeAcceptBtn").click();

    await expect(page.locator("#planSelect")).toHaveValue("fapi2-message-signing-final-test-plan");
    // Declared values overlaid; the undeclared one left on the placeholder.
    await expect(page.locator("#vp_client_auth_type")).toHaveValue("private_key_jwt");
    await expect(page.locator("#vp_sender_constrain")).toHaveValue("select");
  });

  test("recovery drift: record for a plan gone from the catalog starts fresh, record cleared (R5)", async ({
    page,
  }) => {
    await page.addInitScript(() => {
      sessionStorage.setItem(
        "oidf-guided-recovery",
        JSON.stringify({
          ecosystemId: "ksa",
          answers: ["op", "pkjwt", "ksav2"],
          planName: "fapi2-message-signing-final-test-plan",
          config: { alias: "drifted" },
          completedPlanNames: [],
        }),
      );
    });
    await setupScheduleTestRoutes(page, {
      plans: [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS, ...MOCK_GUIDED_PLANS].filter(
        (p) => p.planName !== "fapi2-message-signing-final-test-plan",
      ),
    });
    await page.goto("/schedule-test.html");

    await expect(page.locator("#guidedStage h1")).toHaveText(
      "Which ecosystem are you certifying for?",
    );
    expect(await page.evaluate(() => sessionStorage.getItem("oidf-guided-recovery"))).toBeNull();
  });

  test("wizard_preset full trail to a plan absent from the catalog dead-ends (R4+R13)", async ({
    page,
  }) => {
    const preset = {
      ecosystemId: "ksa",
      answers: ["op", "pkjwt", "ksav2"],
      completedPlanNames: [],
    };
    await setupScheduleTestRoutes(page, {
      plans: [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS, ...MOCK_GUIDED_PLANS].filter(
        (p) => p.planName !== "fapi2-message-signing-final-test-plan",
      ),
    });
    await page.goto(
      "/schedule-test.html?wizard_preset=" + encodeURIComponent(JSON.stringify(preset)),
    );

    await expect(page.locator("#guidedStage h1")).toHaveText(
      "This path isn't available on this server",
    );
    await expect(page.locator("#guidedDeadEndEscape")).toBeVisible();
    expect(new URL(page.url()).searchParams.get("wizard_preset")).toBeNull();
  });

  test("guided beforeunload fires when the config is dirty (positive case)", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await walkKsaOpToReview(page);
    await page.locator("#guidedStageActions").getByText("Configure this plan").click();
    await page.locator("#guidedConfigForm").getByLabel("alias", { exact: true }).fill("dirty");

    const dialogPromise = page.waitForEvent("dialog");
    await page.close({ runBeforeUnload: true });
    const dialog = await dialogPromise;
    expect(dialog.type()).toBe("beforeunload");
    await dialog.accept();
  });

  test("a fresh ecosystem pick resets the completedPlanNames ledger (R14)", async ({ page }) => {
    // Replay with DCR already completed, then backtrack to the ecosystem
    // screen and start over: the FAPI plan's bundle must re-offer DCR.
    const preset = {
      ecosystemId: "open_finance_brazil",
      answers: ["op"],
      completedPlanNames: ["fapi1-advanced-final-brazil-dcr-test-plan"],
    };
    await setupScheduleTestRoutes(page);
    await page.goto(
      "/schedule-test.html?wizard_preset=" + encodeURIComponent(JSON.stringify(preset)),
    );
    await expect(page.locator("#guidedStage h1")).toContainText("Which certification plan");

    // Backtrack to the ecosystem screen (chip idx -1), then walk fresh.
    await page.locator("#guidedTrail .chip").first().click();
    await expect(page.locator("#guidedStage h1")).toHaveText(
      "Which ecosystem are you certifying for?",
    );
    await pickChoice(page, "open_finance_brazil");
    await pickChoice(page, "op");
    await pickChoice(page, "fapi1_brazil_op");

    // The stale ledger is gone: DCR is offered again in the bundle.
    await expect(page.locator("#guidedStage h1")).toHaveText(
      "This certification needs 2 test plans",
    );
    await expect(page.locator("#guidedStage .bundle-list")).toContainText(
      "Dynamic Client Registration",
    );
  });
});
