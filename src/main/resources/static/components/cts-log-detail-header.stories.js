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

// --- Stories ---

export const CompletedTest = {
  render: () => html`<cts-log-detail-header .testInfo=${COMPLETED_TEST}></cts-log-detail-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    });

    // Test info fields
    expect(canvas.getByText("Test Name:")).toBeInTheDocument();
    expect(canvas.getByText(COMPLETED_TEST.testId)).toBeInTheDocument();
    expect(canvas.getByText("Test ID:")).toBeInTheDocument();
    expect(canvas.getByText("Created:")).toBeInTheDocument();
    expect(canvas.getByText("Description:")).toBeInTheDocument();
    expect(canvas.getByText("Test Version:")).toBeInTheDocument();

    // Result badge shows PASSED
    const resultBadge = canvasElement.querySelector('cts-badge[variant="success"][label="PASSED"]');
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
    const failedBadge = canvasElement.querySelector('cts-badge[variant="failure"][label="FAILED"]');
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
    let scrollDetail = null;
    canvasElement.addEventListener("cts-scroll-to-entry", (e) => {
      scrollEventFired = true;
      scrollDetail = e.detail;
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

    // Click View Config button
    const viewConfigBtn = canvasElement.querySelector('[data-testid="view-config-btn"]');
    expect(viewConfigBtn).toBeTruthy();
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
    const closeBtn = configPanel.querySelector("button");
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

    const repeatBtn = canvasElement.querySelector('[data-testid="repeat-test-btn"]');
    expect(repeatBtn).toBeTruthy();
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

    // Plan link is present with correct href
    const planLink = canvasElement.querySelector('[data-testid="return-to-plan-link"]');
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
    const resultBadge = canvasElement.querySelector('cts-badge[variant="success"][label="PASSED"]');
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
    const publishBtn = canvasElement.querySelector('[data-testid="publish-btn"]');
    expect(publishBtn).toBeTruthy();
    expect(canvas.getByText(/Publish/)).toBeInTheDocument();

    // Click publish and verify event
    const publishHandler = fn();
    canvasElement.addEventListener("cts-publish", publishHandler);
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
