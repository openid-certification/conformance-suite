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
  { _id: "r7", result: "SKIPPED", src: "OptionalCheck", msg: "Skipped optional" },
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

    // Click View Config button (target the inner <button> rendered by
    // cts-button — clicking the host bypasses Lit's @click handler).
    const viewConfigBtn = innerButton(canvasElement, "view-config-btn");
    await userEvent.click(viewConfigBtn);

    // Config panel is now visible
    await waitFor(() => {
      configPanel = canvasElement.querySelector('[data-testid="config-panel"]');
      expect(configPanel).toBeTruthy();
    });

    // Config JSON is shown
    const configJson = canvasElement.querySelector('[data-testid="config-json"]');
    expect(configJson).toBeTruthy();
    expect(configJson.textContent).toContain("server.issuer");
    expect(configJson.textContent).toContain("https://op.example.com");

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

    // View Config is visible (always visible)
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
// Plan: docs/plans/2026-04-25-007-feat-r24-test-description-vs-instructions-plan.md
// The component splits `test.summary` on the marker `\n\n---\n\n` into an
// "About this test" descriptive zone and a "What you need to do" warning
// zone. These stories cover the three rendering paths: no marker, with
// marker, and no summary at all.

const SUMMARY_DESCRIPTION_ONLY =
  "This test calls the authorization endpoint and verifies the OP returns a normal login page.";

const SUMMARY_WITH_INSTRUCTIONS =
  "This test calls the authorization endpoint with a login_hint, which must not result in errors.\n\n---\n\nPlease remove any cookies you may have received from the OpenID Provider before proceeding. A fresh login page is needed.";

export const WithDescriptionOnly = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, summary: SUMMARY_DESCRIPTION_ONLY }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    });

    // About-this-test zone renders with eyebrow + body
    const aboutZone = canvasElement.querySelector('[data-testid="about-test-zone"]');
    expect(aboutZone).toBeTruthy();
    expect(aboutZone.textContent).toContain("About this test");
    expect(aboutZone.textContent).toContain("normal login page");

    // No instructions zone when the marker is absent
    const instructionsZone = canvasElement.querySelector('[data-testid="user-instructions-zone"]');
    expect(instructionsZone).toBeNull();

    // Description body lives inside an info-variant cts-alert (the blue box)
    const aboutAlert = aboutZone.closest("cts-alert");
    expect(aboutAlert).toBeTruthy();
    expect(aboutAlert.getAttribute("variant")).toBe("info");
  },
};

export const WithUserInstructions = {
  render: () =>
    html`<cts-log-detail-header
      .testInfo=${{ ...COMPLETED_TEST, summary: SUMMARY_WITH_INSTRUCTIONS }}
    ></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    });

    // Both zones render
    const aboutZone = canvasElement.querySelector('[data-testid="about-test-zone"]');
    const instructionsZone = canvasElement.querySelector('[data-testid="user-instructions-zone"]');
    expect(aboutZone).toBeTruthy();
    expect(instructionsZone).toBeTruthy();

    // Description half lives in the about zone; instructions half in the
    // instructions zone. Marker text is not visible to the operator.
    expect(aboutZone.textContent).toContain("must not result in errors");
    expect(aboutZone.textContent).not.toContain("Please remove any cookies");
    expect(instructionsZone.textContent).toContain("Please remove any cookies");
    expect(instructionsZone.textContent).not.toContain("must not result in errors");

    // Instructions zone is wrapped in a warning-variant cts-alert
    const instructionsAlert = instructionsZone.closest("cts-alert");
    expect(instructionsAlert).toBeTruthy();
    expect(instructionsAlert.getAttribute("variant")).toBe("warning");

    // Eyebrow captions are present on both zones
    expect(aboutZone.textContent).toContain("About this test");
    expect(instructionsZone.textContent).toContain("What you need to do");

    // Visual order: about zone precedes instructions zone in the DOM
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
