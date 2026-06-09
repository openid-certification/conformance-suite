import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import { withMockFetch } from "@fixtures/helpers.js";
import { MOCK_TEST_STATUS, MOCK_TEST_FAILED, MOCK_TEST_RUNNING } from "@fixtures/mock-test-data.js";
import { MOCK_LOG_ENTRIES, MOCK_EMPTY_LOG } from "@fixtures/mock-log-entries.js";

import "../../../src/main/resources/static/components/cts-log-detail-header.js";
import "../../../src/main/resources/static/components/cts-log-viewer.js";

export default {
  title: "Pages/LogDetail",
};

// Copied from log-detail.html's inline <style> — keep in sync with the page.
// Storybook's preview-head.html already loads the shared stylesheets; the
// page's inline rules are the only CSS a page story must supply itself. Only
// the rules a story asserts on are duplicated here (the .log-page wrapper and
// the cts-plan-status FOUC reservation, both added/relevant in U6).
const PAGE_STYLES = html`
  <style>
    .log-page {
      max-width: var(--maxw-wide);
      margin: 0 auto;
      padding: var(--space-4) var(--space-5);
      font-family: var(--font-sans);
    }
    /* FOUC reservation for the nav row's plan-status progress bar
       (cts-plan-status, log mode) — a custom element is display:inline until
       upgraded, so it reserves no height before its definition loads. */
    cts-plan-status:not(:defined) {
      display: block;
      min-height: 38px;
    }
  </style>
`;

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

    // Header region: test name renders. Since U2 the test name appears
    // in two places (the sticky bar's truncated row + the metadata
    // table's value row), so use getAllByText to allow both.
    await waitFor(() => {
      expect(canvas.getAllByText("oidcc-server").length).toBeGreaterThan(0);
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

// U6: the nav row's plan-progress bar (cts-plan-status, log mode) on the
// real page wrapper. The viewed instance is the most-recent of the 2nd
// module, so the "you are here" marker + label track it (R14/R17). Carries
// PAGE_STYLES + a layout assertion (the bar segments share one row) so this
// story fails if the page wrapper CSS rots (page-story-rot solution doc).
const PLAN_MODULES_FOR_PAGE = [
  {
    testModule: "oidcc-server-1",
    instances: ["page-1"],
    status: "FINISHED",
    result: "PASSED",
    _statusResolved: true,
  },
  {
    testModule: "oidcc-server",
    instances: [MOCK_TEST_STATUS.testId],
    status: "FINISHED",
    result: "PASSED",
    _statusResolved: true,
  },
  {
    testModule: "oidcc-server-3",
    instances: ["page-3"],
    status: "FINISHED",
    result: "FAILED",
    _statusResolved: true,
  },
];

export const PlanProgressBarInNavRow = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`
    ${PAGE_STYLES}
    <div class="log-page">
      <cts-log-detail-header
        .testInfo=${{ ...MOCK_TEST_STATUS, results: [] }}
        .planModules=${PLAN_MODULES_FOR_PAGE}
        current-instance-id="${MOCK_TEST_STATUS.testId}"
      ></cts-log-detail-header>
    </div>
  `,
  async play({ canvasElement, step }) {
    const bar = await waitFor(() => {
      const el = canvasElement.querySelector('cts-plan-status[data-testid="progress"]');
      if (!el) throw new Error("progress bar not yet rendered");
      return el;
    });

    await step("the bar marks the viewed module and reads Module 2 of 3", () => {
      const segments = bar.querySelectorAll('[data-testid="plan-status-segment"]');
      expect(segments.length).toBe(3);
      expect(segments[1].classList.contains("is-current")).toBe(true);
      // The "Module N of M" label sits on its own row in cts-test-nav-controls
      // (the bar's hide-label is set in the slim layout), not inside the bar.
      const position = canvasElement.querySelector('[data-testid="progress-position"]');
      expect(position.textContent.trim()).toBe("Module 2 of 3");
    });

    await step("layout: the bar segments sit on a single row (wide host)", () => {
      // The .log-page wrapper at the story viewport is wide enough that the
      // cts-plan-status @container branch does NOT trip, so all segments
      // share one row. A rotted page wrapper (zero width / collapsed) would
      // wrap them and fail this — the page-story-rot guard.
      const segments = bar.querySelectorAll('[data-testid="plan-status-segment"]');
      const top = segments[0].offsetTop;
      segments.forEach((seg) => expect(seg.offsetTop).toBe(top));
    });
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

    // Header region: running-state hero present.
    // The hero's canonical testid is `hero-running`; the older
    // `running-test-info` predates the sequel hierarchy and never reappeared.
    const runningHero = canvasElement.querySelector('[data-testid="hero-running"]');
    expect(runningHero).toBeTruthy();

    // Viewer region: empty log message present
    await waitForViewerLoad(canvasElement);
    expect(canvas.getByText("No log entries")).toBeInTheDocument();

    // Both regions present
    expect(canvasElement.querySelector("cts-log-detail-header")).toBeTruthy();
    expect(canvasElement.querySelector("cts-log-viewer")).toBeTruthy();
  },
};
