import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import { withMockFetch } from "@fixtures/helpers.js";
import { MOCK_TEST_FAILED, MOCK_TEST_RUNNING } from "@fixtures/mock-test-data.js";
import { MOCK_LOG_ENTRIES, MOCK_EMPTY_LOG } from "@fixtures/mock-log-entries.js";

import "../../../src/main/resources/static/components/cts-log-detail-header.js";
import "../../../src/main/resources/static/components/cts-log-viewer.js";

export default {
  title: "Pages/LogDetail",
};

const TEST_FAILED_WITH_RESULTS = {
  ...MOCK_TEST_FAILED,
  results: [
    { _id: "r1", result: "SUCCESS", src: "CheckConfig", msg: "Config valid" },
    {
      _id: "r2",
      result: "FAILURE",
      src: "ValidateIdToken",
      msg: "Signature invalid",
      requirements: ["OIDCC-3.1.3.7-6"],
    },
  ],
};

const TEST_RUNNING_WITH_RESULTS = {
  ...MOCK_TEST_RUNNING,
  results: [],
};

async function waitForViewerLoad(canvasElement) {
  await waitFor(
    () => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeNull();
    },
    { timeout: 3000 },
  );
}

export const Default = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`
    <div class="container-fluid p-3">
      <cts-log-detail-header .testInfo=${TEST_FAILED_WITH_RESULTS}></cts-log-detail-header>
      <cts-log-viewer test-id="test-fail-001"></cts-log-viewer>
    </div>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Header region: test name renders
    await waitFor(() => {
      expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    });

    // Header region: FAILED badge present (FAILED → canonical `fail` variant)
    const failedBadge = canvasElement.querySelector('cts-badge[variant="fail"][label="FAILED"]');
    expect(failedBadge).toBeTruthy();

    // Header region: failure summary section visible
    const failureSummary = canvasElement.querySelector('[data-testid="failure-summary"]');
    expect(failureSummary).toBeTruthy();

    // Viewer region: log entries loaded
    await waitForViewerLoad(canvasElement);
    const entries = canvasElement.querySelectorAll(".logItem");
    expect(entries.length).toBeGreaterThan(0);

    // Both regions present in same canvas
    const header = canvasElement.querySelector("cts-log-detail-header");
    const viewer = canvasElement.querySelector("cts-log-viewer");
    expect(header).toBeTruthy();
    expect(viewer).toBeTruthy();
  },
};

export const RunningTestPage = {
  decorators: [withMockFetch("/api/log/", MOCK_EMPTY_LOG)],
  render: () => html`
    <div class="container-fluid p-3">
      <cts-log-detail-header .testInfo=${TEST_RUNNING_WITH_RESULTS}></cts-log-detail-header>
      <cts-log-viewer test-id="test-running-001"></cts-log-viewer>
    </div>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Header region: RUNNING status
    await waitFor(() => {
      const runningBadge = canvasElement.querySelector('cts-badge[label="RUNNING"]');
      expect(runningBadge).toBeTruthy();
    });

    // Header region: running-test info block present
    const runningInfo = canvasElement.querySelector('[data-testid="running-test-info"]');
    expect(runningInfo).toBeTruthy();

    // Viewer region: empty log message present
    await waitForViewerLoad(canvasElement);
    expect(canvas.getByText("No log entries")).toBeInTheDocument();

    // Both regions present
    expect(canvasElement.querySelector("cts-log-detail-header")).toBeTruthy();
    expect(canvasElement.querySelector("cts-log-viewer")).toBeTruthy();
  },
};
