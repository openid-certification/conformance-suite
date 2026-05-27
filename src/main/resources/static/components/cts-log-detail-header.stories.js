import { html } from "lit";
import { expect, within, waitFor, fn, userEvent } from "storybook/test";
import { MOCK_TEST_STATUS, MOCK_TEST_RUNNING, MOCK_TEST_FAILED } from "@fixtures/mock-test-data.js";
import "./cts-log-detail-header.js";
import { renderErrorIntoSlot } from "../js/log-detail-error-slot.js";

export default {
  title: "Components/cts-log-detail-header",
  component: "cts-log-detail-header",
};

// Build test data with result entries for summary counts
const MOCK_RESULTS = [
  { _id: "r1", result: "SUCCESS", src: "CheckConfig", msg: "Config valid" },
  { _id: "r2", result: "SUCCESS", src: "CheckJwks", msg: "JWKS valid" },
  { _id: "r3", result: "INFO", src: "LogInfo", msg: "Server contacted" },
  {
    _id: "r4",
    result: "WARNING",
    src: "CheckScope",
    msg: "Extra scope",
    requirements: ["OIDCC-3.1"],
  },
];

const MOCK_RESULTS_WITH_FAILURES = [
  ...MOCK_RESULTS,
  {
    _id: "r5",
    result: "FAILURE",
    src: "ValidateIdToken",
    msg: "Signature invalid",
    requirements: ["OIDCC-3.1.3.7-6"],
  },
  {
    _id: "r6",
    result: "FAILURE",
    src: "CheckClaims",
    msg: "Missing sub claim",
    requirements: ["OIDCC-5.1"],
  },
  {
    _id: "r7",
    result: "SKIPPED",
    src: "OptionalCheck",
    msg: "Skipped optional",
  },
];

const MOCK_RESULTS_WARNING_ONLY = [
  { _id: "p1", result: "SUCCESS", src: "A", msg: "OK" },
  { _id: "p2", result: "SUCCESS", src: "B", msg: "OK" },
  {
    _id: "p3",
    result: "WARNING",
    src: "EnsureRecommendedScope",
    msg: "Recommended scope missing",
    requirements: ["OIDCC-5.4"],
  },
];

const COMPLETED_TEST = {
  ...MOCK_TEST_STATUS,
  results: MOCK_RESULTS,
};

const FAILED_TEST = {
  ...MOCK_TEST_FAILED,
  results: MOCK_RESULTS_WITH_FAILURES,
};

const WARNING_TEST = {
  ...MOCK_TEST_STATUS,
  result: "WARNING",
  results: MOCK_RESULTS_WARNING_ONLY,
};

const RUNNING_TEST = {
  ...MOCK_TEST_RUNNING,
  results: [],
};

const ALL_PASSED_TEST = {
  ...MOCK_TEST_STATUS,
  results: [
    { _id: "s1", result: "SUCCESS", src: "A", msg: "OK" },
    { _id: "s2", result: "SUCCESS", src: "B", msg: "OK" },
    { _id: "s3", result: "INFO", src: "C", msg: "Note" },
  ],
};

/**
 * Resolve the inner `<button>` rendered inside a cts-button host with the
 * given data-testid. Required because cts-button renders to its own light
 * DOM and Lit binds `@click` on the inner `<button>` (see cts-button
 * HostClickDoesNotDispatch story for the rationale).
 *
 * @param {HTMLElement} canvasElement
 * @param {string} testId
 * @returns {HTMLButtonElement}
 */
function innerButton(canvasElement, testId) {
  const host = canvasElement.querySelector(`[data-testid="${testId}"]`);
  if (!host) throw new Error(`No element with data-testid="${testId}"`);
  const btn = host.querySelector("button");
  if (!btn) throw new Error(`No <button> inside [data-testid="${testId}"]`);
  return /** @type {HTMLButtonElement} */ (btn);
}

/**
 * Open the kebab popover and click the action item with the given id.
 * Most "secondary" actions (Edit configuration, Share link, Publish, etc.)
 * now live exclusively in the status bar's overflow popover after the
 * vertical action stack was removed in the hierarchy redesign.
 *
 * @param {HTMLElement} canvasElement
 * @param {string} actionId - The `id` field on the actions array entry
 *   (e.g., "edit-config", "share-link", "publish-summary").
 */
async function clickOverflowAction(canvasElement, actionId) {
  const overflow = /** @type {any} */ (
    canvasElement.querySelector('[data-testid="status-bar-overflow"]')
  );
  if (!overflow) throw new Error("No status-bar-overflow on this story");
  const trigger = overflow.querySelector('[data-testid="overflow-trigger"]');
  await userEvent.click(trigger);
  await waitFor(() => {
    const popover = overflow.querySelector('[data-testid="overflow-popover"]');
    if (!popover || !popover.matches(":popover-open")) {
      throw new Error("popover not yet open");
    }
    return popover;
  });
  const item = overflow.querySelector(`.overflowItem[data-action-id="${actionId}"]`);
  if (!item) throw new Error(`No overflow item with action id "${actionId}"`);
  await userEvent.click(item);
}

// --- Stories ---

export const CompletedTest = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Result badge shows PASSED on the canonical `pass` palette (sticky bar).
    const resultBadge = canvasElement.querySelector('cts-badge[variant="pass"][label="PASSED"]');
    expect(resultBadge).toBeTruthy();

    // PASSED hero renders the R24 "About this test" section.
    const summaryHero = canvasElement.querySelector('[data-testid="hero-summary"]');
    expect(summaryHero).toBeTruthy();
    expect(summaryHero.textContent).toContain("About this test");

    // Drawer renders both disclosures, both closed by default.
    const drawer = canvasElement.querySelector('[data-testid="drawer"]');
    expect(drawer).toBeTruthy();
    const detailsBlocks = drawer.querySelectorAll("details");
    expect(detailsBlocks.length).toBe(2);
    expect(detailsBlocks[0].open).toBe(false);
    expect(detailsBlocks[1].open).toBe(false);

    // The metadata table is rendered inside the closed Test details
    // disclosure — the labels exist in the DOM so `getByText` finds them
    // even when the parent details element is collapsed.
    expect(canvas.getByText("Test Name:")).toBeInTheDocument();
    expect(canvas.getByText("Test ID:")).toBeInTheDocument();
    expect(canvas.getByText("Created:")).toBeInTheDocument();
  },
};

export const FailedTest = {
  render: () => html`<cts-log-detail-header .testInfo=${FAILED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // FAILED result badge visible in the sticky bar.
    const failedBadge = canvasElement.querySelector('cts-badge[variant="fail"][label="FAILED"]');
    expect(failedBadge).toBeTruthy();

    // FAILED hero is the failure list. Eyebrow + count headline + the
    // embedded cts-failure-summary.
    const failureHero = canvasElement.querySelector('[data-testid="hero-failures"]');
    expect(failureHero).toBeTruthy();
    expect(failureHero.textContent).toContain("Findings");
    // Two FAILUREs + one WARNING + one SKIPPED in MOCK_RESULTS_WITH_FAILURES.
    expect(failureHero.textContent).toContain("2 failures");
    expect(failureHero.textContent).toContain("1 warning");

    const failureSummary = failureHero.querySelector('[data-testid="failure-summary"]');
    expect(failureSummary).toBeTruthy();

    // Failure entries are rendered with src + msg.
    expect(canvas.getByText("ValidateIdToken: Signature invalid")).toBeInTheDocument();
    expect(canvas.getByText("CheckClaims: Missing sub claim")).toBeInTheDocument();

    // Click a failure entry and verify the bubbled cts-scroll-to-entry event.
    let scrollEventFired = false;
    /** @type {any} */
    let scrollDetail = null;
    canvasElement.addEventListener("cts-scroll-to-entry", (e) => {
      scrollEventFired = true;
      scrollDetail = /** @type {CustomEvent} */ (e).detail;
    });

    const failureText = canvas.getByText("ValidateIdToken: Signature invalid");
    await userEvent.click(failureText);

    expect(scrollEventFired).toBe(true);
    expect(scrollDetail.entryId).toBe("r5");
  },
};

export const WarningOnlyTest = {
  render: () => html`<cts-log-detail-header .testInfo=${WARNING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-failures"]');
      if (!el) throw new Error("hero-failures not yet rendered");
      return el;
    });

    // WARNING result uses the same hero pattern as FAILED — the warnings
    // list is the page's primary affordance. Verdict shrinks to bar chrome.
    const warningBadge = canvasElement.querySelector('cts-badge[variant="warn"][label="WARNING"]');
    expect(warningBadge).toBeTruthy();

    const failureHero = canvasElement.querySelector('[data-testid="hero-failures"]');
    expect(failureHero).toBeTruthy();
    expect(failureHero.textContent).toContain("1 warning");
    expect(failureHero.textContent).not.toContain("failures");
  },
};

export const RunningTest = {
  render: () => html`<cts-log-detail-header .testInfo=${RUNNING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Status shows RUNNING in the sticky bar.
    const statusBadge = canvasElement.querySelector('cts-badge[label="RUNNING"]');
    expect(statusBadge).toBeTruthy();

    // RUNNING hero renders inside the host (was the secondary card pre-redesign).
    const runningHero = canvasElement.querySelector('[data-testid="hero-running"]');
    expect(runningHero).toBeTruthy();
    expect(canvas.getByText(/Live values from the running test/)).toBeInTheDocument();

    // Stop action is surfaced exclusively in the sticky bar's primary slot.
    const stickyStop = canvasElement.querySelector('[data-testid="status-bar-primary"]');
    expect(stickyStop).toBeTruthy();
  },
};

export const RepeatViaStatusBar = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar-primary"]');
      if (!el) throw new Error("status-bar-primary not yet rendered");
      return el;
    });

    // Repeat lives ONLY in the sticky bar's primary slot now (the legacy
    // vertical action stack was removed in the hierarchy redesign).
    const repeatHandler = fn();
    canvasElement.addEventListener("cts-repeat-test", repeatHandler);

    await userEvent.click(innerButton(canvasElement, "status-bar-primary"));

    expect(repeatHandler).toHaveBeenCalledOnce();
    expect(repeatHandler.mock.calls[0][0].detail.testId).toBe(COMPLETED_TEST.testId);
  },
};

export const ViewConfigViaKebab = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="drawer-config"]');
      if (!el) throw new Error("drawer-config not yet rendered");
      return el;
    });

    // The Configuration disclosure lives in the drawer; closed by default.
    const configDetails = /** @type {any} */ (
      canvasElement.querySelector('[data-testid="drawer-config"]')
    );
    expect(configDetails.open).toBe(false);

    // Click the kebab → "View configuration" — the redesigned action
    // routes to the drawer disclosure (was a standalone secondary card).
    await clickOverflowAction(canvasElement, "view-config");

    await waitFor(() => {
      expect(configDetails.open).toBe(true);
    });

    // Config JSON renders inside the read-only Monaco editor.
    const configJson = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector('cts-json-editor[data-testid="config-json"]');
        if (!el) throw new Error("cts-json-editor[data-testid='config-json'] not yet attached");
        return el;
      })
    );
    await configJson.whenReady();
    expect(configJson.getAttribute("readonly")).not.toBeNull();
    expect(configJson.value).toContain("server.issuer");
    expect(configJson.value).toContain("https://op.example.com");

    // U2 floor assertion: a small config (3 keys here) must still
    // render the editor at exactly 336px — the `min-height` half of
    // the min/max-height pair on `.ctsConfigJson`. Pairs with the
    // ceiling assertion in ConfigDrawerHeightLockedAtFixedValue so a
    // future change that drops `min-height` and keeps only
    // `max-height` is caught here. See
    // docs/plans/2026-05-21-002-fix-log-detail-layout-reflows-plan.md U2.
    const smallConfigRect = configJson.getBoundingClientRect();
    expect(Math.abs(smallConfigRect.height - 336)).toBeLessThanOrEqual(1);
  },
};

/**
 * Build a large config payload whose serialised JSON (4-space indent)
 * is comfortably longer than the configuration editor's fixed
 * `calc(var(--space-6) * 14)` = 336 px height. If the editor were free
 * to grow, opening the drawer would inflate the host beyond 336 px
 * (Monaco's default auto-grow ceiling) and the second-jump regression
 * this story guards against would resurface. 60 keys is well past the
 * 14-line ceiling that fits inside 336 px at the editor's line height.
 */
function makeOversizedConfig() {
  /** @type {Record<string, string>} */
  const config = {};
  for (let i = 0; i < 60; i += 1) {
    config[`config.key${String(i).padStart(2, "0")}`] = `value-${i}-${"x".repeat(40)}`;
  }
  return config;
}

const OVERSIZED_CONFIG_TEST = {
  ...COMPLETED_TEST,
  config: makeOversizedConfig(),
};

/**
 * Regression guard for
 * docs/plans/2026-05-21-002-fix-log-detail-layout-reflows-plan.md U2.
 *
 * Before the fix, `.ctsConfigJson` declared only `min-height`. Monaco's
 * auto-grow then expanded the host past 336 px whenever the config
 * payload was longer than ~14 lines, producing a second layout jump
 * after the drawer's disclosure had already settled. The fix sets both
 * `min-height` and `max-height` to the same value so the host stays
 * exactly 336 px tall and long content scrolls inside Monaco.
 *
 * This story opens the drawer with an oversized config, waits for
 * Monaco to mount, and asserts:
 *   1. The `.ctsConfigJson` host's outer height is within ±1 px of
 *      336 px (the calc(var(--space-6) * 14) value).
 *   2. A Monaco scrollable surface is present inside the host, so the
 *      bounded height does not silently clip the configuration — the
 *      user can scroll through the JSON within the editor.
 */
export const ConfigDrawerHeightLockedAtFixedValue = {
  render: () =>
    html`<cts-log-detail-header .testInfo=${OVERSIZED_CONFIG_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="drawer-config"]');
      if (!el) throw new Error("drawer-config not yet rendered");
      return el;
    });

    const configDetails = /** @type {any} */ (
      canvasElement.querySelector('[data-testid="drawer-config"]')
    );
    expect(configDetails.open).toBe(false);

    await clickOverflowAction(canvasElement, "view-config");

    await waitFor(() => {
      expect(configDetails.open).toBe(true);
    });

    const configJson = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector('cts-json-editor[data-testid="config-json"]');
        if (!el) throw new Error("cts-json-editor[data-testid='config-json'] not yet attached");
        return el;
      })
    );
    await configJson.whenReady();

    // The host's outer height must match the fixed CSS value within a
    // 1 px tolerance (sub-pixel rounding). 336 = calc(24px * 14) where
    // 24px is --space-6. If this assertion fails after Monaco mounts,
    // the editor is auto-growing again — re-check the min-height /
    // max-height pair in cts-log-detail-header.js.
    const hostRect = configJson.getBoundingClientRect();
    expect(Math.abs(hostRect.height - 336)).toBeLessThanOrEqual(1);

    // A Monaco scroll surface lives inside the host so the bounded
    // height does not silently clip the configuration JSON. Monaco
    // renders `.monaco-scrollable-element` as its scroll container;
    // its scrollHeight must exceed the bounded host height — `> 0`
    // would pass even for an empty editor (Monaco's scroll container
    // reports a non-zero baseline). Comparing against the host
    // height proves the oversized payload actually requires scroll.
    const scrollable = configJson.querySelector(".monaco-scrollable-element");
    expect(scrollable).toBeTruthy();
    expect(scrollable.scrollHeight).toBeGreaterThan(hostRect.height);
  },
};

export const AllPassed = {
  render: () => html`<cts-log-detail-header .testInfo=${ALL_PASSED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-summary"]');
      if (!el) throw new Error("hero-summary not yet rendered");
      return el;
    });

    // PASSED + no failures → summary hero, NOT failure hero.
    const resultBadge = canvasElement.querySelector('cts-badge[variant="pass"][label="PASSED"]');
    expect(resultBadge).toBeTruthy();

    expect(canvasElement.querySelector('[data-testid="hero-failures"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="hero-summary"]')).toBeTruthy();
  },
};

export const AdminActions = {
  render: () =>
    html`<cts-log-detail-header .testInfo=${COMPLETED_TEST} is-admin></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Owner row lives inside the (closed) Test details drawer disclosure.
    // The label is in the DOM so getByText finds it; the row is just not
    // visually rendered until the disclosure opens.
    const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
    expect(ownerRow).toBeTruthy();
    expect(canvas.getByText("Test Owner:")).toBeInTheDocument();
    expect(canvas.getByText(/12345/)).toBeInTheDocument();

    // Admin pre-publish: kebab carries Publish summary + Publish everything.
    const overflow = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
    expect(overflow).toBeTruthy();
    const items = overflow.querySelectorAll(".overflowItem");
    const labels = Array.from(items, (el) => el.textContent.trim());
    expect(labels.some((s) => s.includes("Publish summary"))).toBe(true);
    expect(labels.some((s) => s.includes("Publish everything"))).toBe(true);
  },
};

export const PublicView = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, publish: "everything" }}
      is-public
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Public view: kebab is present but Repeat / Edit / Publish / Share
    // are filtered out. View configuration + Download Logs (because
    // publish === "everything") remain.
    const overflow = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
    expect(overflow).toBeTruthy();
    const items = overflow.querySelectorAll(".overflowItem");
    const labels = Array.from(items, (el) => el.textContent.trim());
    expect(labels.some((s) => s.includes("View configuration"))).toBe(true);
    expect(labels.some((s) => s.includes("Download Logs"))).toBe(true);
    expect(labels.some((s) => s.includes("Edit configuration"))).toBe(false);
    expect(labels.some((s) => s.includes("Private link"))).toBe(false);
    expect(labels.some((s) => s.includes("Publish"))).toBe(false);

    // No Repeat in the sticky bar primary slot (readonly).
    expect(canvasElement.querySelector('[data-testid="status-bar-primary"]')).toBeNull();

    // Owner row is not in the DOM (not admin).
    expect(canvasElement.querySelector('[data-testid="owner-row"]')).toBeNull();
  },
};

// --- R24: summary zone splitting (PASSED hero context) ---
// Plan: docs/plans/2026-04-25-008-feat-r24-test-description-vs-instructions-plan.md
// In the new hierarchy, the description half of the summary ("About this
// test") is the dominant element of the PASSED hero. The instructions
// half ("What you need to do") only renders in the WAITING hero — see
// the WaitingHeroWithInstructions story below for that path.

const SUMMARY_DESCRIPTION_ONLY = "This is a plain test summary, no marker present.";

const SUMMARY_WITH_INSTRUCTIONS =
  "Descriptive part of the summary.\n\n---\n\nImperative part of the summary.";

export const PassedHeroDescriptionOnly = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{
        ...COMPLETED_TEST,
        summary: SUMMARY_DESCRIPTION_ONLY,
      }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-summary"]');
      if (!el) throw new Error("hero-summary not yet rendered");
      return el;
    });

    const summaryHero = canvasElement.querySelector('[data-testid="hero-summary"]');
    expect(summaryHero.textContent).toContain("About this test");
    expect(summaryHero.textContent).toContain("This is a plain test summary");

    // Instructions zone is intentionally NOT rendered in PASSED state.
    expect(canvasElement.querySelector('[data-testid="user-instructions-zone"]')).toBeNull();
  },
};

export const PassedHeroDescriptionAndMarkerSplit = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{
        ...COMPLETED_TEST,
        summary: SUMMARY_WITH_INSTRUCTIONS,
      }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-summary"]');
      if (!el) throw new Error("hero-summary not yet rendered");
      return el;
    });

    const summaryHero = canvasElement.querySelector('[data-testid="hero-summary"]');
    expect(summaryHero.textContent).toContain("Descriptive part of the summary.");

    // The instructions half of the marker-split summary stays out of the
    // PASSED hero. It only renders during the WAITING lifecycle.
    expect(summaryHero.textContent).not.toContain("Imperative part of the summary.");
  },
};

export const PassedHeroFallbackPlaceholder = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, summary: undefined, description: undefined }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-summary"]');
      if (!el) throw new Error("hero-summary not yet rendered");
      return el;
    });

    // When neither summary nor description are available, the hero shows
    // a quiet placeholder rather than collapsing — the hero's structural
    // weight in the page stays consistent across tests.
    const summaryHero = canvasElement.querySelector('[data-testid="hero-summary"]');
    expect(summaryHero.textContent).toContain("About this test");
    expect(summaryHero.textContent).toContain("No description available");
  },
};

// Plan: docs/plans/2026-05-27-001-feat-autolink-and-format-test-prose-plan.md
// (U1). Bare URLs in the hero description render as clickable new-tab links.
const SUMMARY_WITH_BARE_URL =
  "This test follows https://datatracker.ietf.org/doc/html/rfc9126 to validate the request_uri parameter.";

export const PassedHeroWithAutolinkedUrl = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, summary: SUMMARY_WITH_BARE_URL }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-summary"]');
      if (!el) throw new Error("hero-summary not yet rendered");
      return el;
    });

    const summaryHero = canvasElement.querySelector('[data-testid="hero-summary"]');
    const link = summaryHero.querySelector(".ctsHeroBody a");
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("https://datatracker.ietf.org/doc/html/rfc9126");
    expect(link.getAttribute("target")).toBe("_blank");
    expect(link.getAttribute("rel")).toBe("noopener noreferrer");
    expect(summaryHero.textContent).toContain("request_uri");
  },
};

// --- WAITING hero ---

const WAITING_TEST = {
  ...MOCK_TEST_RUNNING,
  status: "WAITING",
  result: null,
};

const WAITING_TEST_WITH_INSTRUCTIONS = {
  ...WAITING_TEST,
  summary: SUMMARY_WITH_INSTRUCTIONS,
};

export const WaitingHeroWithInstructions = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${WAITING_TEST_WITH_INSTRUCTIONS}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-waiting"]');
      if (!el) throw new Error("hero-waiting not yet rendered");
      return el;
    });

    const waitingHero = canvasElement.querySelector('[data-testid="hero-waiting"]');
    expect(waitingHero.textContent).toContain("Action required");
    expect(waitingHero.textContent).toContain("Imperative part of the summary.");

    // Description half intentionally NOT rendered here — that's the PASSED
    // hero's job. WAITING focuses the operator on what to do, not what
    // the test is about.
    expect(waitingHero.textContent).not.toContain("Descriptive part of the summary.");

    // The hero must NOT carry its own Start button — Start is the
    // sticky status bar's primary action for WAITING tests, and a
    // duplicate inside the hero would split the operator's attention
    // between two identical CTAs in the same viewport.
    expect(waitingHero.querySelector('[data-testid="start-btn"]')).toBeNull();

    // Start is exercised through the status-bar primary instead.
    const startHandler = fn();
    canvasElement.addEventListener("cts-start-test", startHandler);
    await userEvent.click(innerButton(canvasElement, "status-bar-primary"));
    expect(startHandler).toHaveBeenCalledOnce();
  },
};

export const WaitingHeroFallbackInstructions = {
  render: () => html`<cts-log-detail-header .testInfo=${WAITING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-waiting"]');
      if (!el) throw new Error("hero-waiting not yet rendered");
      return el;
    });

    // No summary marker, no results yet — fall back to the generic
    // "Click Start Test when you're ready." copy. C1 (MR 1998): the
    // button label and the fallback prompt both name the action the
    // same way ("Start Test") for consistency with Repeat Test /
    // Continue Plan.
    const waitingHero = canvasElement.querySelector('[data-testid="hero-waiting"]');
    expect(waitingHero.textContent).toContain("Click Start Test when you're ready.");
    expect(waitingHero.getAttribute("data-waiting-mode")).toBe("user-action");
  },
};

// --- U1 parity: the four affordances + the two slots + nav-controls ---
// Plan: docs/plans/2026-04-26-002-refactor-log-detail-page-to-lit-triad-plan.md
// Edit-config / Share-link / Publish all moved into the kebab popover
// after the vertical action stack was removed in the hierarchy redesign.

export const EditConfigViaKebab = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
      if (!el) throw new Error("status-bar-overflow not yet rendered");
      return el;
    });

    const editHandler = fn();
    canvasElement.addEventListener("cts-edit-config", editHandler);

    await clickOverflowAction(canvasElement, "edit-config");

    expect(editHandler).toHaveBeenCalledOnce();
    const detail = editHandler.mock.calls[0][0].detail;
    expect(detail.testId).toBe(COMPLETED_TEST.testId);
    expect(detail.planId).toBe(COMPLETED_TEST.planId);
    expect(detail.config).toEqual(COMPLETED_TEST.config);
  },
};

export const ShareLinkViaKebab = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
      if (!el) throw new Error("status-bar-overflow not yet rendered");
      return el;
    });

    const shareHandler = fn();
    canvasElement.addEventListener("cts-share-link", shareHandler);

    await clickOverflowAction(canvasElement, "share-link");

    expect(shareHandler).toHaveBeenCalledOnce();
    expect(shareHandler.mock.calls[0][0].detail.testId).toBe(COMPLETED_TEST.testId);
  },
};

export const AdminUnpublished = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, publish: null }}
      is-admin
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const overflow = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
      if (!el) throw new Error("status-bar-overflow not yet rendered");
      return el;
    });

    // Admin pre-publish: both Publish-summary and Publish-everything
    // appear in the kebab, no Unpublish.
    const labels = Array.from(overflow.querySelectorAll(".overflowItem"), (el) =>
      el.textContent.trim(),
    );
    expect(labels.some((s) => s.includes("Publish summary"))).toBe(true);
    expect(labels.some((s) => s.includes("Publish everything"))).toBe(true);
    expect(labels.some((s) => s.includes("Unpublish"))).toBe(false);

    const publishHandler = fn();
    canvasElement.addEventListener("cts-publish", publishHandler);

    await clickOverflowAction(canvasElement, "publish-summary");
    expect(publishHandler).toHaveBeenCalledOnce();
    expect(publishHandler.mock.calls[0][0].detail.action).toBe("publish");
    expect(publishHandler.mock.calls[0][0].detail.mode).toBe("summary");
  },
};

export const AdminPublished = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, publish: "everything" }}
      is-admin
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const overflow = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
      if (!el) throw new Error("status-bar-overflow not yet rendered");
      return el;
    });

    // Admin post-publish: Unpublish appears, Publish-summary / -everything do not.
    const labels = Array.from(overflow.querySelectorAll(".overflowItem"), (el) =>
      el.textContent.trim(),
    );
    expect(labels.some((s) => s.includes("Unpublish"))).toBe(true);
    expect(labels.some((s) => s.includes("Publish summary"))).toBe(false);
    expect(labels.some((s) => s.includes("Publish everything"))).toBe(false);

    const publishHandler = fn();
    canvasElement.addEventListener("cts-publish", publishHandler);

    await clickOverflowAction(canvasElement, "unpublish");

    expect(publishHandler).toHaveBeenCalledOnce();
    expect(publishHandler.mock.calls[0][0].detail.action).toBe("unpublish");
    expect(publishHandler.mock.calls[0][0].detail.mode).toBeUndefined();
  },
};

export const WithRunningBrowserSlot = {
  render: () => html`<cts-log-detail-header .testInfo=${RUNNING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-running"]');
      if (!el) throw new Error("hero-running not yet rendered");
      return el;
    });

    // The browser slot moved from the secondary card into the running
    // hero. The legacy id="runningTestBrowser" + data-slot="browser"
    // selectors are preserved so log-detail.js's slot-injection
    // pattern keeps working without modification.
    const slotById = canvasElement.querySelector("#runningTestBrowser");
    const slotByAttr = canvasElement.querySelector('[data-slot="browser"]');
    expect(slotById).toBeTruthy();
    expect(slotByAttr).toBeTruthy();
    expect(slotById).toBe(slotByAttr);

    // Page-level JS injects content via DOM methods; the slot accepts
    // the injection without Lit's reactive re-render wiping it. Use a
    // cts-button here (not a plain <button>) so the story matches what
    // js/log-detail.js's renderBrowserSlot actually appends in
    // production — otherwise the rendered button looks unstyled and
    // misrepresents the live behaviour.
    const injected = document.createElement("cts-button");
    injected.setAttribute("data-testid", "injected-browser-btn");
    injected.setAttribute("variant", "primary");
    injected.setAttribute("size", "sm");
    injected.setAttribute("icon", "external-link");
    injected.setAttribute("label", "Open in browser");
    slotById.appendChild(injected);

    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="injected-browser-btn"]')).toBeTruthy();
    });
  },
};

export const WithFinalErrorSlot = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...RUNNING_TEST, status: "INTERRUPTED" }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-interrupted"]');
      if (!el) throw new Error("hero-interrupted not yet rendered");
      return el;
    });

    // INTERRUPTED renders the failure-hero pattern with the FINAL_ERROR
    // slot pinned at the top via the existing `[data-slot="error"]` —
    // population is driven by log-detail.js calling renderErrorIntoSlot.
    const slotById = canvasElement.querySelector("#runningTestError");
    const slotByAttr = canvasElement.querySelector('[data-slot="error"]');
    expect(slotById).toBeTruthy();
    expect(slotByAttr).toBeTruthy();
    expect(slotById).toBe(slotByAttr);
    // With no FINAL_ERROR injected, the slot is empty — the redundant
    // "This test was interrupted. See the error details above." cts-alert
    // that used to sit here was removed once the U3 terminal banner
    // took over the verdict (it duplicated the banner and pointed at an
    // empty slot in this exact case).
    expect(slotById.children.length).toBe(0);
  },
};

/**
 * Demonstrates the FINAL_ERROR alert actually rendered into the
 * `[data-slot="error"]` placeholder. The empty slot lives inside
 * cts-log-detail-header; population is normally driven by the
 * `/api/runner/{testId}` polling loop in `js/log-detail.js`. This
 * story bypasses the polling loop and calls `renderErrorIntoSlot`
 * directly with a sample error payload, so the same construction
 * code that runs in production paints the alert in Storybook.
 *
 * Use this story to visually verify the alert layout, the
 * stacktrace toggle, and the cause-section reveal.
 */
const SAMPLE_RUNNER_ERROR = {
  error: "Connection refused while contacting authorization server",
  error_class: "ConnectException",
  stacktrace: [
    "java.net.ConnectException: Connection refused",
    "  at java.base/sun.nio.ch.Net.pollConnect(Native Method)",
    "  at java.base/sun.nio.ch.Net.pollConnectNow(Net.java:672)",
    "  at net.openid.conformance.runner.TestRunner.runTest(TestRunner.java:412)",
    "  at net.openid.conformance.runner.TestExecutionManager.runInBackground(TestExecutionManager.java:188)",
  ],
  cause_stacktrace: [
    "java.nio.channels.UnresolvedAddressException",
    "  at java.base/sun.nio.ch.Net.checkAddress(Net.java:149)",
    "  at java.base/sun.nio.ch.SocketChannelImpl.checkRemote(SocketChannelImpl.java:863)",
  ],
};

export const WithFinalErrorSlotPopulated = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...RUNNING_TEST, status: "INTERRUPTED" }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    // Wait for the header to render so the slot exists in the DOM.
    const slot = await waitFor(() => {
      const el = canvasElement.querySelector('[data-slot="error"]');
      if (!el) throw new Error("error slot not yet rendered");
      return el;
    });

    // Drive the slot through the same code path js/log-detail.js
    // uses on each /api/runner poll. Story stays faithful to the
    // production render path as long as renderErrorIntoSlot evolves.
    renderErrorIntoSlot(slot, SAMPLE_RUNNER_ERROR);

    // The injected alert is the danger-variant FINAL_ERROR — distinct
    // from the hero region's "interrupted" alert which is also danger.
    // We anchor on the heading text the helper writes, not on variant.
    const finalErrorAlert = Array.from(slot.querySelectorAll("cts-alert")).find((a) =>
      (a.textContent || "").includes("There was an error while running the test"),
    );
    expect(finalErrorAlert).toBeTruthy();
    expect(finalErrorAlert.getAttribute("variant")).toBe("danger");
    expect(finalErrorAlert.textContent).toContain("ConnectException");

    // Stacktrace + cause are present but hidden until the toggle is
    // clicked — mirrors the legacy reveal-on-click contract.
    const stack = slot.querySelector("#stacktrace");
    const cause = slot.querySelector("#causeStacktrace");
    expect(stack).toBeTruthy();
    expect(cause).toBeTruthy();
    expect(stack.style.display).toBe("none");
    expect(cause.style.display).toBe("none");

    const toggle = slot.querySelector("#stacktraceBtn");
    expect(toggle).toBeTruthy();
    // cts-button binds @click on the inner <button> (light DOM); a
    // userEvent.click on the host element does not bubble through to
    // the inner handler that emits cts-click. Click the inner button
    // directly — same shape as the innerButton() helper used elsewhere
    // in this file. cts-button renders its inner <button> via Lit on the
    // first microtask after connectedCallback, so we wait for it before
    // querying — without this, the lookup races the slot population.
    const toggleInner = await waitFor(() => {
      const inner = toggle.querySelector("button");
      if (!inner) throw new Error("cts-button inner <button> not yet rendered");
      return inner;
    });
    await userEvent.click(toggleInner);

    // After the toggle fires, both blocks reveal together and the
    // toggle button hides itself.
    expect(stack.style.display).toBe("block");
    expect(cause.style.display).toBe("block");
    expect(stack.classList.contains("show")).toBe(true);
    expect(cause.classList.contains("show")).toBe(true);
    expect(toggle.style.display).toBe("none");
  },
};

export const WithTestNavControls = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector("cts-test-nav-controls");
      if (!el) throw new Error("cts-test-nav-controls not yet rendered");
      return el;
    });

    // The nav cluster was promoted out of the legacy vertical action
    // stack and now sits in the .ctsNavRow directly above the sticky
    // bar — always visible at every viewport (the legacy stack hid at
    // <1024px, taking the cluster with it). Lifting it above the bar
    // (rather than below, as the initial four-zone redesign did)
    // tightens the IA proximity between the page-level breadcrumb
    // and the plan-progress orientation it carries.
    const navRow = canvasElement.querySelector('[data-testid="nav-row"]');
    expect(navRow).toBeTruthy();

    const navHost = navRow.querySelector("cts-test-nav-controls");
    expect(navHost).toBeTruthy();
    expect(navHost.getAttribute("id")).toBe("testNavControls");
    expect(navHost.getAttribute("data-testid")).toBe("test-nav-controls");
    expect(navHost.getAttribute("plan-id")).toBe(COMPLETED_TEST.planId);
    expect(navHost.getAttribute("test-id")).toBe(COMPLETED_TEST.testId);
    // The header always opts the cluster into `slim`. Without slim the
    // cluster would render Return-to-Plan + Repeat alongside the
    // status bar's primary "Repeat" and the page-level breadcrumb's
    // "Plan" item — duplicating two prominent affordances inside one
    // viewport.
    expect(navHost.hasAttribute("slim")).toBe(true);
    // The status bar primary already provides Repeat for FINISHED
    // tests; the slim cluster must NOT render its own Repeat copy.
    expect(navRow.querySelector('[data-testid="repeat-btn"]')).toBeNull();
    expect(navRow.querySelector('[data-testid="back-btn"]')).toBeNull();

    // IA regression: nav row must precede the sticky status bar in
    // DOM order. Reading top-to-bottom, the page chrome is
    // breadcrumb → plan progress → this test's verdict — matching
    // the page-level breadcrumb's own scope (plan → this test).
    const statusBar = canvasElement.querySelector('[data-testid="status-bar"]');
    expect(statusBar).toBeTruthy();
    // Node.DOCUMENT_POSITION_FOLLOWING (4) means statusBar follows navRow.
    expect(
      navRow.compareDocumentPosition(statusBar) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  },
};

// --- U2 sticky status bar (unchanged from before this redesign) ---
// Plan: docs/plans/2026-04-26-003-feat-status-bar-sticky-and-mode-aware-plan.md
// These verify the bar's lifecycle behaviour is unaffected by the
// hierarchy redesign — the bar's mechanics are stable across the
// brief-driven changes to the hero + drawer below.

const INTERRUPTED_TEST = {
  ...MOCK_TEST_FAILED,
  status: "INTERRUPTED",
  result: "INTERRUPTED",
  results: MOCK_RESULTS_WITH_FAILURES,
};

const RUNNING_TEST_WITH_RESULTS = {
  ...MOCK_TEST_RUNNING,
  results: MOCK_RESULTS,
};

export const StatusBarWaiting = {
  render: () => html`<cts-log-detail-header .testInfo=${WAITING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const bar = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });

    const pill = bar.querySelector('cts-badge[label="WAITING"]');
    expect(pill).toBeTruthy();
    expect(pill.getAttribute("variant")).toBe("warn");
    expect(bar.textContent).toContain("Waiting for user input");

    const primaryHost = bar.querySelector('[data-testid="status-bar-primary"]');
    expect(primaryHost).toBeTruthy();
    expect(within(primaryHost).getByText(/Start/)).toBeInTheDocument();

    // Both the bar primary AND the hero footer carry Start during WAITING.
    // Asserting the bar fires the event keeps the U2 contract.
    const startHandler = fn();
    canvasElement.addEventListener("cts-start-test", startHandler);
    await userEvent.click(innerButton(canvasElement, "status-bar-primary"));
    expect(startHandler).toHaveBeenCalledOnce();
    expect(startHandler.mock.calls[0][0].detail.testId).toBe(WAITING_TEST.testId);

    // Test name leads .ctsStatusBarLeft on every bar variant — locks
    // the row-1 hierarchy so a future template refactor can't silently
    // demote the name behind the status pill on the WAITING path.
    const left = bar.querySelector(".ctsStatusBarLeft");
    const nameText = left.querySelector(".ctsStatusBarTestNameText");
    expect(nameText).toBeTruthy();
    expect(left.firstElementChild).toBe(nameText);
  },
};

export const StatusBarRunning = {
  render: () =>
    html`<cts-log-detail-header .testInfo=${RUNNING_TEST_WITH_RESULTS}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const bar = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });

    const pill = bar.querySelector('cts-badge[label="RUNNING"]');
    expect(pill).toBeTruthy();
    expect(pill.getAttribute("variant")).toBe("running");

    // Result-pill cluster: 2 SUCCESS, 1 INFO, 1 WARNING in MOCK_RESULTS.
    const pillCluster = bar.querySelector('[data-testid="status-bar-pills"]');
    expect(pillCluster).toBeTruthy();
    expect(pillCluster.querySelector('cts-badge[label="✓ 2"]')).toBeTruthy();
    expect(pillCluster.querySelector('cts-badge[label="⚠ 1"]')).toBeTruthy();
    expect(pillCluster.querySelector('cts-badge[label="ⓘ 1"]')).toBeTruthy();

    const stopHandler = fn();
    canvasElement.addEventListener("cts-stop-test", stopHandler);
    await userEvent.click(innerButton(canvasElement, "status-bar-primary"));
    expect(stopHandler).toHaveBeenCalledOnce();

    // Test name leads .ctsStatusBarLeft on every bar variant — locks
    // the row-1 hierarchy so a future template refactor can't silently
    // demote the name behind the running status pill or the result-pill
    // cluster.
    const left = bar.querySelector(".ctsStatusBarLeft");
    const nameText = left.querySelector(".ctsStatusBarTestNameText");
    expect(nameText).toBeTruthy();
    expect(left.firstElementChild).toBe(nameText);
  },
};

export const StatusBarFinishedPassed = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const bar = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });

    expect(bar.querySelector('cts-badge[variant="pass"][label="PASSED"]')).toBeTruthy();
    expect(bar.querySelector('cts-badge[variant="skip"][label="FINISHED"]')).toBeTruthy();
    expect(within(bar).getByText(/Repeat/)).toBeInTheDocument();

    const repeatHandler = fn();
    canvasElement.addEventListener("cts-repeat-test", repeatHandler);
    await userEvent.click(innerButton(canvasElement, "status-bar-primary"));
    expect(repeatHandler).toHaveBeenCalledOnce();
    expect(repeatHandler.mock.calls[0][0].detail.testId).toBe(COMPLETED_TEST.testId);

    // Test module name leads the bar's left cluster, before the
    // verdict badge — promotes "which test is this?" above the
    // badges that describe it.
    const left = bar.querySelector(".ctsStatusBarLeft");
    expect(left).toBeTruthy();
    const nameText = left.querySelector(".ctsStatusBarTestNameText");
    expect(nameText).toBeTruthy();
    expect(nameText.textContent).toContain(COMPLETED_TEST.testName);
    expect(left.firstElementChild).toBe(nameText);

    // Created timestamp owns row 2 by itself after the row-1 lift —
    // make sure no future template tweak silently drops the field.
    const created = bar.querySelector(".ctsStatusBarCreated");
    expect(created).toBeTruthy();
    expect(created.textContent.trim().length).toBeGreaterThan(0);

    // It renders through cts-time: a native <time> whose title carries the
    // full absolute date on hover (the compact visible form elides parts).
    const createdTime = created.querySelector("time");
    expect(createdTime).toBeTruthy();
    expect(createdTime.getAttribute("title")).toBeTruthy();
    expect(createdTime.getAttribute("datetime")).toBeTruthy();
  },
};

export const StatusBarFinishedFailed = {
  render: () => html`<cts-log-detail-header .testInfo=${FAILED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const bar = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });

    expect(bar.querySelector('cts-badge[variant="fail"][label="FAILED"]')).toBeTruthy();
    expect(bar.querySelector('cts-badge[variant="skip"][label="FINISHED"]')).toBeTruthy();

    const pillCluster = bar.querySelector('[data-testid="status-bar-pills"]');
    expect(pillCluster.querySelector('cts-badge[label="✗ 2"]')).toBeTruthy();
    expect(pillCluster.querySelector('[data-testid="status-bar-pill-failure"]')).toBeTruthy();
  },
};

export const StatusBarInterrupted = {
  render: () => html`<cts-log-detail-header .testInfo=${INTERRUPTED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const bar = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });

    const resultPill = bar.querySelector('cts-badge[label="INTERRUPTED"][variant="fail"]');
    expect(resultPill).toBeTruthy();

    const statusPills = bar.querySelectorAll('cts-badge[label="INTERRUPTED"]');
    expect(statusPills.length).toBeGreaterThanOrEqual(2);
  },
};

export const StatusBarStickyOnScroll = {
  render: () => html`
    <div style="height: 2000px;">
      <cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>
    </div>
  `,
  async play({ canvasElement }) {
    const bar = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });

    const computed = getComputedStyle(bar);
    expect(computed.position).toBe("sticky");
    expect(computed.top).toBe("0px");
    expect(computed.zIndex).toBe("10");
  },
};

export const StatusBarPublishesHeightCustomProperty = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });

    await waitFor(() => {
      const value = getComputedStyle(document.documentElement)
        .getPropertyValue("--status-bar-height")
        .trim();
      expect(value).toMatch(/^\d+px$/);
      expect(value).not.toBe("0px");
    });
  },
};

// --- U7 kebab popover (unchanged from before this redesign) ---
// Plan: docs/plans/2026-04-26-008-feat-action-overflow-and-cts-test-summary-extraction-plan.md
// The kebab item count + dispatch behaviour is unaffected by the hierarchy
// redesign. The vertical action stack that used to duplicate these items
// at desktop has been removed.

export const StatusBarOverflowSecondaryActions = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const overflow = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
      if (!el) throw new Error("status-bar-overflow not yet rendered");
      return /** @type {any} */ (el);
    });

    const slot = canvasElement.querySelector('[data-slot="action-overflow"]');
    expect(slot).toBeTruthy();
    expect(slot.contains(overflow)).toBe(true);

    // Non-readonly, non-admin: upload, view-config, edit-config,
    // download-log, share-link → 5 rows.
    const items = overflow.querySelectorAll(".overflowItem");
    expect(items.length).toBe(5);
    const labels = Array.from(items, (el) => el.textContent.trim());
    expect(labels).toEqual(
      expect.arrayContaining([
        expect.stringContaining("Upload Images"),
        expect.stringContaining("View configuration"),
        expect.stringContaining("Edit configuration"),
        expect.stringContaining("Download Logs"),
        expect.stringContaining("Private link"),
      ]),
    );
  },
};

export const StatusBarOverflowAdminWithPublish = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, publish: null }}
      is-admin
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const overflow = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
      if (!el) throw new Error("status-bar-overflow not yet rendered");
      return /** @type {any} */ (el);
    });
    // Admin pre-publish: 5 base actions + Publish summary + Publish
    // everything = 7. Unpublish is hidden until after a publish call.
    const items = overflow.querySelectorAll(".overflowItem");
    expect(items.length).toBe(7);
    const labels = Array.from(items, (el) => el.textContent.trim());
    expect(labels.some((s) => s.includes("Publish summary"))).toBe(true);
    expect(labels.some((s) => s.includes("Publish everything"))).toBe(true);
    expect(labels.some((s) => s.includes("Unpublish"))).toBe(false);
  },
};

export const StatusBarOverflowSkippedForWaiting = {
  render: () => html`<cts-log-detail-header .testInfo=${WAITING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });
    expect(canvasElement.querySelector('[data-testid="status-bar-overflow"]')).toBeNull();
  },
};

export const StatusBarOverflowDispatchesEditConfig = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
      if (!el) throw new Error("status-bar-overflow not yet rendered");
      return el;
    });

    const editHandler = fn();
    canvasElement.addEventListener("cts-edit-config", editHandler);

    await clickOverflowAction(canvasElement, "edit-config");

    expect(editHandler).toHaveBeenCalledOnce();
    expect(editHandler.mock.calls[0][0].detail.testId).toBe(COMPLETED_TEST.testId);
  },
};

// --- Drawer (Region C) ---
// The Test details + Configuration disclosures replace the legacy
// _renderConfigPanel standalone secondary card and surface the metadata
// table that used to live inline inside the .logHeaderCard. Both
// disclosures are closed by default; native `<details>` semantics
// handle keyboard a11y.

export const DrawerExpandedRevealsMetadata = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const detailsHost = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector('[data-testid="drawer-test-details"]');
        if (!el) throw new Error("drawer-test-details not yet rendered");
        return el;
      })
    );

    expect(detailsHost.open).toBe(false);

    // Open the disclosure via summary click — the same mechanic a real
    // user would use.
    const summary = detailsHost.querySelector("summary");
    await userEvent.click(summary);
    await waitFor(() => expect(detailsHost.open).toBe(true));

    // Metadata table is now visible inside the open disclosure.
    const metaTable = detailsHost.querySelector(".logMetaTable");
    expect(metaTable).toBeTruthy();
    expect(metaTable.textContent).toContain("Test ID:");
    expect(metaTable.textContent).toContain(COMPLETED_TEST.testId);
    expect(metaTable.textContent).toContain("Plan ID:");
    expect(metaTable.textContent).toContain(COMPLETED_TEST.planId);
  },
};

export const DrawerCollapsedByDefault = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="drawer"]');
      if (!el) throw new Error("drawer not yet rendered");
      return el;
    });

    // Both disclosures default to closed. The `open` attribute is the
    // single source of truth for visibility; CSS handles the chevron
    // rotation.
    const testDetails = canvasElement.querySelector('[data-testid="drawer-test-details"]');
    const configDetails = canvasElement.querySelector('[data-testid="drawer-config"]');
    expect(testDetails.open).toBe(false);
    expect(configDetails.open).toBe(false);
  },
};

// --- U3 (MR 1998 findings A1, A2, A6, A7, C1) — terminal-state
// banner, action-bar visibility, WAITING copy, label consistency.
// Plan: docs/plans/2026-05-22-002-fix-mr1998-maintainer-feedback-plan.md

const WAITING_TEST_WITH_RESULTS = {
  ...WAITING_TEST,
  results: MOCK_RESULTS,
};

const STALE_STATUS_WAITING_RESULT_PASSED = {
  ...COMPLETED_TEST,
  status: "WAITING",
};

const STALE_STATUS_WAITING_RESULT_FAILED = {
  ...FAILED_TEST,
  status: "WAITING",
};

const WARNING_RESULT_TEST = {
  ...COMPLETED_TEST,
  result: "WARNING",
  results: MOCK_RESULTS_WARNING_ONLY,
};

const REVIEW_RESULT_TEST = {
  ...COMPLETED_TEST,
  result: "REVIEW",
};

const SKIPPED_RESULT_TEST = {
  ...COMPLETED_TEST,
  result: "SKIPPED",
};

export const TerminalBannerPassed = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const banner = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="terminal-banner"]');
      if (!el) throw new Error("terminal-banner not yet rendered");
      return el;
    });
    expect(banner.getAttribute("data-phase")).toBe("finished-pass");
    expect(banner.classList.contains("ctsTerminalBanner--pass")).toBe(true);
    expect(banner.textContent).toContain("Test passed");
    expect(banner.querySelector('cts-icon[name="circle-check"]')).toBeTruthy();
  },
};

export const TerminalBannerFailed = {
  render: () => html`<cts-log-detail-header .testInfo=${FAILED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const banner = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="terminal-banner"]');
      if (!el) throw new Error("terminal-banner not yet rendered");
      return el;
    });
    expect(banner.getAttribute("data-phase")).toBe("finished-fail");
    expect(banner.classList.contains("ctsTerminalBanner--fail")).toBe(true);
    expect(banner.textContent).toContain("Test failed");
    expect(banner.querySelector('cts-icon[name="close-circle"]')).toBeTruthy();
  },
};

export const TerminalBannerWarning = {
  render: () =>
    html`<cts-log-detail-header .testInfo=${WARNING_RESULT_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const banner = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="terminal-banner"]');
      if (!el) throw new Error("terminal-banner not yet rendered");
      return el;
    });
    expect(banner.getAttribute("data-phase")).toBe("finished-warn");
    expect(banner.classList.contains("ctsTerminalBanner--warn")).toBe(true);
    expect(banner.textContent).toContain("Test passed with warnings");
    expect(banner.querySelector('cts-icon[name="warning"]')).toBeTruthy();
  },
};

export const TerminalBannerReview = {
  render: () =>
    html`<cts-log-detail-header .testInfo=${REVIEW_RESULT_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const banner = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="terminal-banner"]');
      if (!el) throw new Error("terminal-banner not yet rendered");
      return el;
    });
    expect(banner.getAttribute("data-phase")).toBe("finished-review");
    // Review shares the warn palette: a reviewer needs to act, but it
    // isn't a failure — same urgency as a warning.
    expect(banner.classList.contains("ctsTerminalBanner--warn")).toBe(true);
    expect(banner.textContent).toContain("Test needs review");
  },
};

export const TerminalBannerSkipped = {
  render: () =>
    html`<cts-log-detail-header .testInfo=${SKIPPED_RESULT_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const banner = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="terminal-banner"]');
      if (!el) throw new Error("terminal-banner not yet rendered");
      return el;
    });
    expect(banner.getAttribute("data-phase")).toBe("finished-skip");
    expect(banner.classList.contains("ctsTerminalBanner--skip")).toBe(true);
    expect(banner.textContent).toContain("Test skipped");
  },
};

export const TerminalBannerInterrupted = {
  render: () => html`<cts-log-detail-header .testInfo=${INTERRUPTED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const banner = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="terminal-banner"]');
      if (!el) throw new Error("terminal-banner not yet rendered");
      return el;
    });
    expect(banner.getAttribute("data-phase")).toBe("interrupted");
    expect(banner.classList.contains("ctsTerminalBanner--fail")).toBe(true);
    expect(banner.textContent).toContain("Test interrupted");
  },
};

export const NoTerminalBannerWhileRunning = {
  render: () =>
    html`<cts-log-detail-header .testInfo=${RUNNING_TEST_WITH_RESULTS}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });
    // RUNNING is a non-terminal phase — no verdict to announce.
    expect(canvasElement.querySelector('[data-testid="terminal-banner"]')).toBeNull();
  },
};

export const NoTerminalBannerWhileWaiting = {
  render: () => html`<cts-log-detail-header .testInfo=${WAITING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });
    // WAITING (with no result yet) is non-terminal.
    expect(canvasElement.querySelector('[data-testid="terminal-banner"]')).toBeNull();
  },
};

export const StaleStatusBarReadsTerminalWhenResultIsSet = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${STALE_STATUS_WAITING_RESULT_PASSED}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    // A1 (MR 1998): the previous behaviour branched on `status` alone,
    // so polling lag (status=WAITING, result=PASSED) kept Start visible
    // on a test that had already passed. The new phase derivation
    // routes (status=WAITING, result=PASSED) through the FINISHED bar
    // and renders the Repeat Test primary instead.
    const bar = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });

    expect(bar.querySelector('cts-badge[variant="pass"][label="PASSED"]')).toBeTruthy();
    expect(within(bar).getByText(/Repeat Test/)).toBeInTheDocument();
    // The Start button must not leak through: querying by label leaves
    // no room for the regression to come back.
    expect(within(bar).queryByText(/Start Test/)).toBeNull();

    // The terminal banner also picks up the verdict.
    const banner = canvasElement.querySelector('[data-testid="terminal-banner"]');
    expect(banner).toBeTruthy();
    expect(banner.getAttribute("data-phase")).toBe("finished-pass");
  },
};

export const StaleStatusRoutesHeroToFailureWhenResultFailed = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${STALE_STATUS_WAITING_RESULT_FAILED}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    // Hero side of A1: the failure list, not the WAITING hero, must
    // render when result=FAILED — even if status is still WAITING.
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="terminal-banner"]');
      if (!el) throw new Error("terminal-banner not yet rendered");
      return el;
    });
    expect(canvasElement.querySelector('[data-testid="hero-failures"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="hero-waiting"]')).toBeNull();
  },
};

export const WaitingHeroDistinguishesExternalVsUserAction = {
  render: () =>
    html`<cts-log-detail-header .testInfo=${WAITING_TEST_WITH_RESULTS}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    // A6 (MR 1998): a WAITING test that has already executed
    // conditions is waiting on an external party (HTTP callback,
    // browser-driven step), not on a user click. The previous copy
    // ("ACTION REQUIRED — Click Start when you're ready") was wrong
    // for that case. Switch to the "external request" branch and
    // hide the Start CTA the user does not need to act on.
    const waitingHero = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-waiting"]');
      if (!el) throw new Error("hero-waiting not yet rendered");
      return el;
    });

    expect(waitingHero.getAttribute("data-waiting-mode")).toBe("external");
    expect(waitingHero.textContent).toContain("Test running");
    expect(waitingHero.textContent).toContain(
      "Waiting for an external request — no action required from you.",
    );
    expect(waitingHero.textContent).not.toContain("Click Start");

    const bar = canvasElement.querySelector('[data-testid="status-bar"]');
    expect(bar.textContent).toContain("Waiting for external input — no action required");
    // Start button is suppressed in the external-wait branch — the
    // user does not need it.
    expect(bar.querySelector('[data-testid="status-bar-primary"]')).toBeNull();
  },
};

export const WaitingHeroKeepsStartWhenFreshTest = {
  render: () => html`<cts-log-detail-header .testInfo=${WAITING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    // A6 inverse: a fresh WAITING test (no conditions yet) genuinely
    // needs the user to click Start to kick off the run. Eyebrow,
    // copy, and the primary button all stay in their "needs user
    // action" form.
    const waitingHero = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="hero-waiting"]');
      if (!el) throw new Error("hero-waiting not yet rendered");
      return el;
    });

    expect(waitingHero.getAttribute("data-waiting-mode")).toBe("user-action");
    expect(waitingHero.textContent).toContain("Action required");

    const bar = canvasElement.querySelector('[data-testid="status-bar"]');
    expect(bar.textContent).toContain("Waiting for user input");

    // Start Test (C1 label) is present and dispatches cts-start-test.
    const startHandler = fn();
    canvasElement.addEventListener("cts-start-test", startHandler);
    await userEvent.click(innerButton(canvasElement, "status-bar-primary"));
    expect(startHandler).toHaveBeenCalledOnce();
  },
};

export const ConsistentActionLabels = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    // C1 (MR 1998): the bar's primary on a finished test reads
    // "Repeat Test" (not "Repeat"), matching cts-test-nav-controls'
    // "Repeat Test" / "Continue Plan" labels so all three actions
    // name themselves consistently as `<Verb> <Object>`.
    const bar = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar"]');
      if (!el) throw new Error("status bar not yet rendered");
      return el;
    });
    expect(within(bar).getByText("Repeat Test")).toBeInTheDocument();
    expect(within(bar).queryByText(/^Repeat$/)).toBeNull();
  },
};

export const VariantRendersAsDefinitionList = {
  // U6 (MR 1998 finding C2): the Variant row inside the Test details
  // drawer renders each key/value pair on its own <dt>/<dd> line
  // instead of the legacy "k: v, k: v" comma-soup. The drawer is
  // collapsed by default, so the play test opens it before asserting.
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const drawer = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="drawer-test-details"]');
      if (!el) throw new Error("drawer not yet rendered");
      return el;
    });
    drawer.open = true;
    await waitFor(() => {
      if (!canvasElement.querySelector('[data-testid="variant-list"]')) {
        throw new Error("variant list not yet rendered");
      }
    });

    const list = canvasElement.querySelector('[data-testid="variant-list"]');
    expect(list).toBeTruthy();
    expect(list.tagName).toBe("DL");

    const keys = Array.from(list.querySelectorAll("dt"));
    const values = Array.from(list.querySelectorAll("dd"));
    // COMPLETED_TEST inherits MOCK_TEST_STATUS.variant which has two
    // keys: client_auth_type + response_type.
    expect(keys.length).toBe(2);
    expect(values.length).toBe(2);
    expect(keys[0].textContent.trim()).toBe("client_auth_type");
    expect(values[0].textContent.trim()).toBe("client_secret_basic");
    expect(keys[1].textContent.trim()).toBe("response_type");
    expect(values[1].textContent.trim()).toBe("code");

    // Regression: the legacy comma-joined string must not appear inside
    // the variant cell.
    const cell = list.parentElement;
    expect(cell.textContent).not.toContain("client_auth_type: client_secret_basic, ");
  },
};
