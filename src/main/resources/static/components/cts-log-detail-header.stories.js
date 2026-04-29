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

/**
 * Archived state — `archived` reactive prop renders the legacy
 * `runningTestArchived` info banner above the hero. The page sets
 * `header.archived = true` when `/api/runner/{testId}` returns 404
 * (mirrors `log-detail.html:1662–1664`). Banner is dismissible.
 */
export const ArchivedTest = {
  render: () =>
    html`<cts-log-detail-header .testInfo=${COMPLETED_TEST} archived></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const banner = canvasElement.querySelector('[data-testid="archived-banner"]');
      if (!banner) throw new Error("archived-banner not yet rendered");
      return banner;
    });
    const banner = canvasElement.querySelector('[data-testid="archived-banner"]');
    expect(banner).toBeTruthy();
    expect(banner.getAttribute("variant")).toBe("info");
    expect(banner.textContent).toContain("This test is no longer running.");
    expect(banner.textContent).toContain("This log has been archived");
  },
};

/**
 * Default state has no archived banner — verifies `archived=false` keeps
 * the prior chrome unchanged.
 */
export const NotArchivedHasNoBanner = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const bar = canvasElement.querySelector('[data-testid="status-bar-primary"]');
      if (!bar) throw new Error("status bar not yet rendered");
      return bar;
    });
    const banner = canvasElement.querySelector('[data-testid="archived-banner"]');
    expect(banner).toBeFalsy();
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

    // No summary marker — fall back to the generic "Click Start" copy.
    const waitingHero = canvasElement.querySelector('[data-testid="hero-waiting"]');
    expect(waitingHero.textContent).toContain("Click Start when you're ready.");
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
    // alert pinned at the top via the existing `[data-slot="error"]`.
    const slotById = canvasElement.querySelector("#runningTestError");
    const slotByAttr = canvasElement.querySelector('[data-slot="error"]');
    expect(slotById).toBeTruthy();
    expect(slotByAttr).toBeTruthy();
    expect(slotById).toBe(slotByAttr);

    // The interrupted-state alert variant is danger.
    const interruptedAlert = Array.from(canvasElement.querySelectorAll("cts-alert")).find((a) =>
      (a.textContent || "").includes("interrupted"),
    );
    expect(interruptedAlert).toBeTruthy();
    expect(interruptedAlert.getAttribute("variant")).toBe("danger");
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
    // in this file.
    const toggleInner = toggle.querySelector("button");
    expect(toggleInner).toBeTruthy();
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
    // stack and now sits in the .ctsNavRow directly under the sticky
    // bar — always visible at every viewport (the legacy stack hid at
    // <1024px, taking the cluster with it).
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

    const testName = bar.querySelector(".ctsStatusBarTestName");
    expect(testName).toBeTruthy();
    expect(testName.textContent).toContain(COMPLETED_TEST.testName);
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
