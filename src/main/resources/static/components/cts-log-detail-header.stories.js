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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    });

    // Status shows RUNNING
    const statusBadge = canvasElement.querySelector('cts-badge[label="RUNNING"]');
    expect(statusBadge).toBeTruthy();

    // Running test info section is visible
    const runningInfo = canvasElement.querySelector('[data-testid="running-test-info"]');
    expect(runningInfo).toBeTruthy();

    expect(canvas.getByText(/This test is currently running/)).toBeInTheDocument();

    // Start and Stop buttons present
    const startBtn = canvasElement.querySelector('[data-testid="start-btn"]');
    expect(startBtn).toBeTruthy();
    expect(canvas.getByText(/Start/)).toBeInTheDocument();

    const stopBtn = canvasElement.querySelector('[data-testid="stop-btn"]');
    expect(stopBtn).toBeTruthy();
    expect(canvas.getByText(/Stop/)).toBeInTheDocument();
  },
};

export const ViewConfig = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    });

    // Plan link is present with correct href (rendered as cts-link-button → <a>).
    const planLinkHost = canvasElement.querySelector('[data-testid="return-to-plan-link"]');
    expect(planLinkHost).toBeTruthy();
    const planLink = planLinkHost.querySelector("a");
    expect(planLink).toBeTruthy();
    expect(planLink.getAttribute("href")).toContain("plan-detail.html?plan=");
    expect(planLink.getAttribute("href")).toContain(encodeURIComponent(COMPLETED_TEST.planId));
    expect(canvas.getByText(/Return to Plan/)).toBeInTheDocument();
  },
};

export const AllPassed = {
  render: () => html`<cts-log-detail-header .testInfo=${ALL_PASSED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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

    // Publish button visible for admin
    const publishBtnHost = canvasElement.querySelector('[data-testid="publish-btn"]');
    expect(publishBtnHost).toBeTruthy();
    expect(canvas.getByText(/Publish/)).toBeInTheDocument();

    // Click publish and verify event
    const publishHandler = fn();
    canvasElement.addEventListener("cts-publish", publishHandler);
    const publishBtn = innerButton(canvasElement, "publish-btn");
    await userEvent.click(publishBtn);

    expect(publishHandler).toHaveBeenCalledOnce();
    expect(publishHandler.mock.calls[0][0].detail.action).toBe("publish");
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
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
