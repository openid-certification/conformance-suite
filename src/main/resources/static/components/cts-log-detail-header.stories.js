import { html } from "lit";
import { expect, within, waitFor, fn, userEvent } from "storybook/test";
import { MOCK_TEST_STATUS, MOCK_TEST_RUNNING, MOCK_TEST_FAILED } from "@fixtures/mock-test-data.js";
import "./cts-log-detail-header.js";

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

const COMPLETED_TEST = {
  ...MOCK_TEST_STATUS,
  results: MOCK_RESULTS,
};

const FAILED_TEST = {
  ...MOCK_TEST_FAILED,
  results: MOCK_RESULTS_WITH_FAILURES,
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

// --- Stories ---

export const CompletedTest = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Test info fields (token-styled "Label: value" rows; labels are plain
    // text inside .logMetaLabel divs).
    expect(canvas.getByText("Test Name:")).toBeInTheDocument();
    expect(canvas.getByText(COMPLETED_TEST.testId)).toBeInTheDocument();
    expect(canvas.getByText("Test ID:")).toBeInTheDocument();
    expect(canvas.getByText("Created:")).toBeInTheDocument();
    expect(canvas.getByText("Description:")).toBeInTheDocument();
    expect(canvas.getByText("Test Version:")).toBeInTheDocument();

    // Result badge shows PASSED on the canonical `pass` palette.
    const resultBadge = canvasElement.querySelector('cts-badge[variant="pass"][label="PASSED"]');
    expect(resultBadge).toBeTruthy();

    // Result summary row present
    const resultSummary = canvasElement.querySelector('[data-testid="result-summary"]');
    expect(resultSummary).toBeTruthy();
  },
};

export const FailedTest = {
  render: () => html`<cts-log-detail-header .testInfo=${FAILED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // FAILED result badge visible
    const failedBadge = canvasElement.querySelector('cts-badge[variant="fail"][label="FAILED"]');
    expect(failedBadge).toBeTruthy();

    // Failure summary section is visible
    const failureSummary = canvasElement.querySelector('[data-testid="failure-summary"]');
    expect(failureSummary).toBeTruthy();

    expect(canvas.getByText("Failure summary:")).toBeInTheDocument();

    // Failure entries are clickable
    const failureList = canvasElement.querySelector('[data-testid="failure-list"]');
    expect(failureList).toBeTruthy();

    // Check that failure text includes source and message
    expect(canvas.getByText("ValidateIdToken: Signature invalid")).toBeInTheDocument();
    expect(canvas.getByText("CheckClaims: Missing sub claim")).toBeInTheDocument();

    // Click a failure entry and verify event fires
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

export const RunningTest = {
  render: () => html`<cts-log-detail-header .testInfo=${RUNNING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Status shows RUNNING
    const statusBadge = canvasElement.querySelector('cts-badge[label="RUNNING"]');
    expect(statusBadge).toBeTruthy();

    // Running test info section is visible
    const runningInfo = canvasElement.querySelector('[data-testid="running-test-info"]');
    expect(runningInfo).toBeTruthy();

    expect(canvas.getByText(/This test is currently running/)).toBeInTheDocument();

    // Start and Stop buttons present in the running-test card. The
    // sticky status bar also surfaces a Stop primary action when
    // RUNNING, so /Start/ and /Stop/ each match in two places —
    // assert per-host instead of via the global text query.
    const startBtn = canvasElement.querySelector('[data-testid="start-btn"]');
    expect(startBtn).toBeTruthy();
    expect(within(startBtn).getByText(/Start/)).toBeInTheDocument();

    const stopBtn = canvasElement.querySelector('[data-testid="stop-btn"]');
    expect(stopBtn).toBeTruthy();
    expect(within(stopBtn).getByText(/Stop/)).toBeInTheDocument();
  },
};

export const ViewConfig = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Config panel is initially hidden
    let configPanel = canvasElement.querySelector('[data-testid="config-panel"]');
    expect(configPanel).toBeNull();

    // Click View configuration button (target the inner <button> rendered by
    // cts-button — clicking the host bypasses Lit's @click handler).
    const viewConfigBtn = innerButton(canvasElement, "view-config-btn");
    await userEvent.click(viewConfigBtn);

    // Config panel is now visible
    await waitFor(() => {
      configPanel = canvasElement.querySelector('[data-testid="config-panel"]');
      expect(configPanel).toBeTruthy();
    });

    // Config JSON renders inside the read-only Monaco editor. Monaco
    // virtualises long content, so we read `.value` rather than
    // `textContent` — the property is the wrapper's documented contract.
    // `whenReady()` resolves on either the Monaco or fallback path, so
    // the test stays agnostic to which surface mounted.
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

    // Click close to hide
    const closeBtn = configPanel.querySelector("cts-button button");
    await userEvent.click(closeBtn);

    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="config-panel"]')).toBeNull();
    });
  },
};

export const RepeatTest = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    const repeatHandler = fn();
    canvasElement.addEventListener("cts-repeat-test", repeatHandler);

    const repeatBtn = innerButton(canvasElement, "repeat-test-btn");
    await userEvent.click(repeatBtn);

    expect(repeatHandler).toHaveBeenCalledOnce();
    expect(repeatHandler.mock.calls[0][0].detail.testId).toBe(COMPLETED_TEST.testId);
  },
};

export const ReturnToPlan = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Plan link is present with correct href (rendered as cts-link-button → <a>).
    const planLinkHost = canvasElement.querySelector('[data-testid="return-to-plan-link"]');
    expect(planLinkHost).toBeTruthy();
    const planLink = planLinkHost.querySelector("a");
    expect(planLink).toBeTruthy();
    expect(planLink.getAttribute("href")).toContain("plan-detail.html?plan=");
    expect(planLink.getAttribute("href")).toContain(encodeURIComponent(COMPLETED_TEST.planId));
    // The action-stack's Return-to-Plan link AND cts-test-nav-controls's
    // Back-link both render with "Return to Plan" text when the test
    // belongs to a plan, so query within the action-stack host only.
    expect(within(planLinkHost).getByText(/Return to Plan/)).toBeInTheDocument();
  },
};

export const AllPassed = {
  render: () => html`<cts-log-detail-header .testInfo=${ALL_PASSED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Result badge shows PASSED
    const resultBadge = canvasElement.querySelector('cts-badge[variant="pass"][label="PASSED"]');
    expect(resultBadge).toBeTruthy();

    // Failure summary section is NOT shown when there are no failures
    const failureSummary = canvasElement.querySelector('[data-testid="failure-summary"]');
    expect(failureSummary).toBeNull();

    // Result summary still shows
    const resultSummary = canvasElement.querySelector('[data-testid="result-summary"]');
    expect(resultSummary).toBeTruthy();
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

    // Owner row visible for admin
    const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
    expect(ownerRow).toBeTruthy();
    expect(canvas.getByText("Test Owner:")).toBeInTheDocument();
    expect(canvas.getByText(/12345/)).toBeInTheDocument();

    // All action buttons visible for admin (non-public, non-readonly)
    expect(canvasElement.querySelector('[data-testid="repeat-test-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="upload-images-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="view-config-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="download-log-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="return-to-plan-link"]')).toBeTruthy();

    // Publish-everything button visible for admin in pre-publish state.
    // The Publish split (summary + everything) renders both options when
    // !test.publish && admin; data-testid="publish-btn" is on the
    // "Publish everything" host so existing call sites keep working.
    const publishBtnHost = canvasElement.querySelector('[data-testid="publish-btn"]');
    expect(publishBtnHost).toBeTruthy();
    expect(within(publishBtnHost).getByText(/Publish everything/)).toBeInTheDocument();

    // Click "Publish everything" and verify event
    const publishHandler = fn();
    canvasElement.addEventListener("cts-publish", publishHandler);
    const publishBtn = innerButton(canvasElement, "publish-btn");
    await userEvent.click(publishBtn);

    expect(publishHandler).toHaveBeenCalledOnce();
    expect(publishHandler.mock.calls[0][0].detail.action).toBe("publish");
    expect(publishHandler.mock.calls[0][0].detail.mode).toBe("everything");
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

    // View configuration is visible (always visible)
    expect(canvasElement.querySelector('[data-testid="view-config-btn"]')).toBeTruthy();

    // Download is visible (publish === "everything")
    expect(canvasElement.querySelector('[data-testid="download-log-btn"]')).toBeTruthy();

    // Return to Plan is visible
    expect(canvasElement.querySelector('[data-testid="return-to-plan-link"]')).toBeTruthy();

    // Repeat Test, Upload Images, Publish are NOT visible in public view
    expect(canvasElement.querySelector('[data-testid="repeat-test-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="upload-images-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="publish-btn"]')).toBeNull();

    // Owner row is not visible (not admin)
    const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
    expect(ownerRow).toBeNull();
  },
};

// --- R24: summary zone splitting ---
// Plan: docs/plans/2026-04-25-008-feat-r24-test-description-vs-instructions-plan.md
// These stories assert the *rendered shape* the component emits for each
// summary path (no marker / with marker / no summary). The splitter's
// per-input-variation logic is covered exhaustively by the colocated
// `test-summary-split.test.js` unit tests, so play assertions here stay
// focused on DOM structure and not on input/output enumeration.

const SUMMARY_DESCRIPTION_ONLY = "This is a plain test summary, no marker present.";

const SUMMARY_WITH_INSTRUCTIONS =
  "Descriptive part of the summary.\n\n---\n\nImperative part of the summary.";

export const WithDescriptionOnly = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{
        ...COMPLETED_TEST,
        summary: SUMMARY_DESCRIPTION_ONLY,
      }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    const aboutZone = canvasElement.querySelector('[data-testid="about-test-zone"]');
    expect(aboutZone).toBeTruthy();
    expect(aboutZone.textContent).toContain("About this test");

    // No instructions zone when the marker is absent.
    expect(canvasElement.querySelector('[data-testid="user-instructions-zone"]')).toBeNull();

    // Description body lives inside an info-variant cts-alert (the blue box).
    const aboutAlert = aboutZone.closest("cts-alert");
    expect(aboutAlert.getAttribute("variant")).toBe("info");
  },
};

export const WithUserInstructions = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{
        ...COMPLETED_TEST,
        summary: SUMMARY_WITH_INSTRUCTIONS,
      }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    const aboutZone = canvasElement.querySelector('[data-testid="about-test-zone"]');
    const instructionsZone = canvasElement.querySelector('[data-testid="user-instructions-zone"]');
    expect(aboutZone).toBeTruthy();
    expect(instructionsZone).toBeTruthy();

    // Eyebrow captions identify each zone.
    expect(aboutZone.textContent).toContain("About this test");
    expect(instructionsZone.textContent).toContain("What you need to do");

    // Each zone is wrapped in the right cts-alert variant — info for
    // descriptive context, warning for action-required.
    expect(aboutZone.closest("cts-alert").getAttribute("variant")).toBe("info");
    expect(instructionsZone.closest("cts-alert").getAttribute("variant")).toBe("warning");

    // Visual order: description precedes instructions in the DOM so an
    // operator reads context before action.
    const order = aboutZone.compareDocumentPosition(instructionsZone);
    expect(order & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  },
};

export const WithoutSummary = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, summary: undefined }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Neither summary zone renders when there is no summary
    expect(canvasElement.querySelector('[data-testid="about-test-zone"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="user-instructions-zone"]')).toBeNull();

    // The rest of the metadata grid still renders
    expect(canvas.getByText("Test Name:")).toBeInTheDocument();
  },
};

// --- U1: parity gaps closed for the log-detail-v2 page ---
// Plan: docs/plans/2026-04-26-002-refactor-log-detail-page-to-lit-triad-plan.md
// These stories cover the four newly-added affordances (Edit config, Share
// link, Publish split, Public link), the two named-slot placeholders
// (`[data-slot="browser"]`, `[data-slot="error"]`), and the cts-test-nav-controls
// integration that the plan calls out as a hard parity requirement.

export const WithEditConfigAction = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Edit-configuration button is present in non-readonly state.
    const editBtnHost = canvasElement.querySelector('[data-testid="edit-config-btn"]');
    expect(editBtnHost).toBeTruthy();

    const editHandler = fn();
    canvasElement.addEventListener("cts-edit-config", editHandler);

    const editBtn = innerButton(canvasElement, "edit-config-btn");
    await userEvent.click(editBtn);

    expect(editHandler).toHaveBeenCalledOnce();
    const detail = editHandler.mock.calls[0][0].detail;
    expect(detail.testId).toBe(COMPLETED_TEST.testId);
    expect(detail.planId).toBe(COMPLETED_TEST.planId);
    expect(detail.config).toEqual(COMPLETED_TEST.config);
  },
};

export const WithShareLinkAction = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    const shareBtnHost = canvasElement.querySelector('[data-testid="share-link-btn"]');
    expect(shareBtnHost).toBeTruthy();

    const shareHandler = fn();
    canvasElement.addEventListener("cts-share-link", shareHandler);

    const shareBtn = innerButton(canvasElement, "share-link-btn");
    await userEvent.click(shareBtn);

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
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Pre-publish admin sees BOTH Publish-summary and Publish-everything.
    const summaryHost = canvasElement.querySelector('[data-testid="publish-summary-btn"]');
    const everythingHost = canvasElement.querySelector('[data-testid="publish-btn"]');
    expect(summaryHost).toBeTruthy();
    expect(everythingHost).toBeTruthy();

    // Neither Unpublish nor Public link renders when not yet published.
    expect(canvasElement.querySelector('[data-testid="unpublish-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="public-link"]')).toBeNull();

    const publishHandler = fn();
    canvasElement.addEventListener("cts-publish", publishHandler);

    // Click "Publish summary" → action: publish, mode: summary.
    const summaryBtn = innerButton(canvasElement, "publish-summary-btn");
    await userEvent.click(summaryBtn);

    expect(publishHandler).toHaveBeenCalledOnce();
    expect(publishHandler.mock.calls[0][0].detail.action).toBe("publish");
    expect(publishHandler.mock.calls[0][0].detail.mode).toBe("summary");

    // Click "Publish everything" → action: publish, mode: everything.
    const everythingBtn = innerButton(canvasElement, "publish-btn");
    await userEvent.click(everythingBtn);

    expect(publishHandler).toHaveBeenCalledTimes(2);
    expect(publishHandler.mock.calls[1][0].detail.action).toBe("publish");
    expect(publishHandler.mock.calls[1][0].detail.mode).toBe("everything");
  },
};

export const AdminPublished = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, publish: "everything" }}
      is-admin
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // Post-publish admin sees Unpublish + Public link.
    const unpublishHost = canvasElement.querySelector('[data-testid="unpublish-btn"]');
    const publicLinkHost = canvasElement.querySelector('[data-testid="public-link"]');
    expect(unpublishHost).toBeTruthy();
    expect(publicLinkHost).toBeTruthy();

    // Pre-publish split is hidden.
    expect(canvasElement.querySelector('[data-testid="publish-summary-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="publish-btn"]')).toBeNull();

    // Public link href points back to the public URL of this test.
    const publicLink = publicLinkHost.querySelector("a");
    expect(publicLink).toBeTruthy();
    expect(publicLink.getAttribute("href")).toContain(
      `log-detail.html?log=${encodeURIComponent(COMPLETED_TEST.testId)}&public=true`,
    );

    // Click Unpublish → action: unpublish, no mode.
    const publishHandler = fn();
    canvasElement.addEventListener("cts-publish", publishHandler);

    const unpublishBtn = innerButton(canvasElement, "unpublish-btn");
    await userEvent.click(unpublishBtn);

    expect(publishHandler).toHaveBeenCalledOnce();
    expect(publishHandler.mock.calls[0][0].detail.action).toBe("unpublish");
    expect(publishHandler.mock.calls[0][0].detail.mode).toBeUndefined();
  },
};

export const WithRunningBrowserSlot = {
  render: () => html`<cts-log-detail-header .testInfo=${RUNNING_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // The browser slot placeholder is present and addressable by the
    // legacy `runningTestBrowser` ID + the `data-slot="browser"` test seam.
    const slotById = canvasElement.querySelector("#runningTestBrowser");
    const slotByAttr = canvasElement.querySelector('[data-slot="browser"]');
    expect(slotById).toBeTruthy();
    expect(slotByAttr).toBeTruthy();
    expect(slotById).toBe(slotByAttr);

    // Page-level JS injects content via DOM methods; the slot accepts it
    // without the Lit re-render wiping it on the next reactive update.
    const injected = document.createElement("button");
    injected.setAttribute("data-testid", "injected-browser-btn");
    injected.textContent = "Open in browser";
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
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // INTERRUPTED keeps the running-test card visible so the page can
    // inject the FINAL_ERROR alert into the error slot.
    const card = canvasElement.querySelector('[data-testid="running-test-info"]');
    expect(card).toBeTruthy();

    const slotById = canvasElement.querySelector("#runningTestError");
    const slotByAttr = canvasElement.querySelector('[data-slot="error"]');
    expect(slotById).toBeTruthy();
    expect(slotByAttr).toBeTruthy();
    expect(slotById).toBe(slotByAttr);

    // Start/Stop are hidden when INTERRUPTED — restarting an interrupted
    // run goes through the Repeat Test flow instead.
    expect(canvasElement.querySelector('[data-testid="start-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="stop-btn"]')).toBeNull();

    // The interrupted-state alert variant is danger.
    const alerts = canvasElement.querySelectorAll("cts-alert");
    const interruptedAlert = Array.from(alerts).find((a) =>
      (a.textContent || "").includes("interrupted"),
    );
    expect(interruptedAlert).toBeTruthy();
    expect(interruptedAlert.getAttribute("variant")).toBe("danger");
  },
};

export const WithTestNavControls = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
    });

    // The header re-renders cts-test-nav-controls inside its own action
    // stack so legacy bootstraps using `getElementById('testNavControls')`
    // continue to find it. (See plan #007 + the U1 plan's Resolved decision
    // §4 for why the ID lives on the host element.)
    const navHost = canvasElement.querySelector("cts-test-nav-controls");
    expect(navHost).toBeTruthy();
    expect(navHost.getAttribute("id")).toBe("testNavControls");
    expect(navHost.getAttribute("data-testid")).toBe("test-nav-controls");

    // Plan-id and test-id flow through from testInfo so the cluster has
    // enough data to render its progress + Return-to-Plan link.
    expect(navHost.getAttribute("plan-id")).toBe(COMPLETED_TEST.planId);
    expect(navHost.getAttribute("test-id")).toBe(COMPLETED_TEST.testId);
  },
};

// --- U2: mode-aware sticky status bar (Region A) ---
// Plan: docs/plans/2026-04-26-003-feat-status-bar-sticky-and-mode-aware-plan.md
// Each lifecycle (WAITING / RUNNING / FINISHED-PASSED / FINISHED-FAILED /
// INTERRUPTED) renders a different combination of status pill, supporting
// text, and primary action inside the bar's three columns. The bar
// publishes its measured height as `--status-bar-height` on
// document.documentElement so downstream sticky descendants
// (connection-lost banner, R32 anchors, U7 overflow popover) coordinate
// without re-measuring.

const WAITING_TEST = {
  ...MOCK_TEST_RUNNING,
  status: "WAITING",
  result: null,
};

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

    // Status pill renders WAITING on the warn palette.
    const pill = bar.querySelector('cts-badge[label="WAITING"]');
    expect(pill).toBeTruthy();
    expect(pill.getAttribute("variant")).toBe("warn");

    // Supporting text matches the R19 phrasing.
    expect(bar.textContent).toContain("Waiting for user input");

    // Primary action is "Start" and fires cts-start-test on click.
    const primaryHost = bar.querySelector('[data-testid="status-bar-primary"]');
    expect(primaryHost).toBeTruthy();
    expect(within(primaryHost).getByText(/Start/)).toBeInTheDocument();

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

    // Status pill renders RUNNING on the running palette.
    const pill = bar.querySelector('cts-badge[label="RUNNING"]');
    expect(pill).toBeTruthy();
    expect(pill.getAttribute("variant")).toBe("running");

    // Result-pill cluster renders compact glyphs + counts. Two SUCCESS,
    // one INFO, one WARNING in MOCK_RESULTS — failure/review are zero so
    // they are filtered out.
    const pillCluster = bar.querySelector('[data-testid="status-bar-pills"]');
    expect(pillCluster).toBeTruthy();
    expect(pillCluster.querySelector('cts-badge[label="✓ 2"]')).toBeTruthy();
    expect(pillCluster.querySelector('cts-badge[label="⚠ 1"]')).toBeTruthy();
    expect(pillCluster.querySelector('cts-badge[label="ⓘ 1"]')).toBeTruthy();

    // Primary action is "Stop" and fires cts-stop-test.
    expect(within(bar).getByText(/Stop/)).toBeInTheDocument();
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

    // Result pill (PASSED on pass palette) renders alongside FINISHED on
    // skip palette. Both live inside the bar, both are addressable.
    const passedPill = bar.querySelector('cts-badge[variant="pass"][label="PASSED"]');
    const finishedPill = bar.querySelector('cts-badge[variant="skip"][label="FINISHED"]');
    expect(passedPill).toBeTruthy();
    expect(finishedPill).toBeTruthy();

    // Primary action is "Repeat" and fires cts-repeat-test.
    expect(within(bar).getByText(/Repeat/)).toBeInTheDocument();
    const repeatHandler = fn();
    canvasElement.addEventListener("cts-repeat-test", repeatHandler);
    await userEvent.click(innerButton(canvasElement, "status-bar-primary"));
    expect(repeatHandler).toHaveBeenCalledOnce();
    expect(repeatHandler.mock.calls[0][0].detail.testId).toBe(COMPLETED_TEST.testId);

    // Test name shows on row 2 (truncated container).
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

    // FAILED on fail palette + FINISHED on skip palette.
    expect(bar.querySelector('cts-badge[variant="fail"][label="FAILED"]')).toBeTruthy();
    expect(bar.querySelector('cts-badge[variant="skip"][label="FINISHED"]')).toBeTruthy();

    // Failure count flows into the result-pill cluster.
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

    // RESULT_BADGE_VARIANTS["INTERRUPTED"] === "fail" — verifies the
    // mapping flows through the bar's _renderFinishedBar path.
    const resultPill = bar.querySelector('cts-badge[label="INTERRUPTED"][variant="fail"]');
    expect(resultPill).toBeTruthy();

    // Status pill (separate badge) also renders INTERRUPTED on fail palette.
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

    // The bar is sticky at >= 640px. Storybook canvas in test mode is
    // typically wider than the breakpoint; assert the computed
    // `position` is `sticky`. (Avoid scroll-based assertions because
    // the test runner's viewport size and scroll container are
    // deterministic only at the computed-style level.)
    const computed = getComputedStyle(bar);
    expect(computed.position).toBe("sticky");
    expect(computed.top).toBe("0px");
    // z-index 10 stacks the bar above the connection-lost banner (9).
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

    // After firstUpdated() runs the ResizeObserver attach + initial
    // publish, document.documentElement carries an inline
    // --status-bar-height value matching the bar's measured height.
    await waitFor(() => {
      const value = getComputedStyle(document.documentElement)
        .getPropertyValue("--status-bar-height")
        .trim();
      // Non-zero, non-empty pixel value (e.g. "44px").
      expect(value).toMatch(/^\d+px$/);
      expect(value).not.toBe("0px");
    });
  },
};

// --- U7: action overflow popover (status bar slot) ---
// Plan: docs/plans/2026-04-26-008-feat-action-overflow-and-cts-test-summary-extraction-plan.md
// The status bar's <slot name="action-overflow"> is filled by a
// <cts-action-overflow> with the secondary actions (Upload Images, View
// configuration, Edit configuration, Download Logs, Publish, Share Link).
// The desktop header card still renders the full vertical action stack
// for thoroughness; the overflow is the glance affordance the bar
// surfaces at every viewport width.

export const StatusBarOverflowSecondaryActions = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const overflow = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
      if (!el) throw new Error("status-bar-overflow not yet rendered");
      return /** @type {any} */ (el);
    });

    // The overflow lives inside the bar's reserved slot.
    const slot = canvasElement.querySelector('[data-slot="action-overflow"]');
    expect(slot).toBeTruthy();
    expect(slot.contains(overflow)).toBe(true);

    // Non-readonly, non-admin: visible actions = upload, view-config,
    // edit-config, download-log (no publish, no unpublish), plus
    // share-link. Five rows.
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
    // Admin pre-publish: secondary actions + Publish summary + Publish
    // everything; Unpublish hidden until after a publish call. Seven rows.
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
    // Pre-run there's nothing meaningful to overflow — Start is the only
    // action and lives in the bar's primary slot. The overflow trigger
    // is intentionally absent so the bar reads as a single Start CTA.
    expect(canvasElement.querySelector('[data-testid="status-bar-overflow"]')).toBeNull();
  },
};

export const StatusBarOverflowDispatchesEditConfig = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const overflow = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector('[data-testid="status-bar-overflow"]');
        if (!el) throw new Error("status-bar-overflow not yet rendered");
        return el;
      })
    );

    const editHandler = fn();
    canvasElement.addEventListener("cts-edit-config", editHandler);

    // Open the popover via the trigger and click the edit-config row.
    const trigger = overflow.querySelector('[data-testid="overflow-trigger"]');
    await userEvent.click(trigger);
    await waitFor(() => {
      const popover = overflow.querySelector('[data-testid="overflow-popover"]');
      if (!popover || !popover.matches(":popover-open")) {
        throw new Error("popover not yet open");
      }
      return popover;
    });
    const editItem = overflow.querySelector('.overflowItem[data-action-id="edit-config"]');
    await userEvent.click(editItem);

    expect(editHandler).toHaveBeenCalledOnce();
    expect(editHandler.mock.calls[0][0].detail.testId).toBe(COMPLETED_TEST.testId);
  },
};

// --- U7: cts-test-summary extraction ---
// The summary alerts moved out of cts-log-detail-header into the
// cts-test-summary sibling component. They still render inside the
// header card at desktop (the [data-testid] selectors are preserved
// on the new component's DOM, so existing R24 stories above keep
// passing). The page-level instance is asserted via log-detail-v2 e2e.

export const TestSummaryRendersAsSiblingComponent = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, summary: SUMMARY_WITH_INSTRUCTIONS }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="header-test-summary"]');
      if (!el) throw new Error("header-test-summary not yet rendered");
      return el;
    });
    // The header now hosts a cts-test-summary sibling rather than
    // rendering the alerts inline; the about/instructions zones live
    // inside that component.
    const summaryHost = canvasElement.querySelector('[data-testid="header-test-summary"]');
    expect(summaryHost.tagName.toLowerCase()).toBe("cts-test-summary");
    expect(summaryHost.querySelector('[data-testid="about-test-zone"]')).toBeTruthy();
    expect(summaryHost.querySelector('[data-testid="user-instructions-zone"]')).toBeTruthy();
  },
};
