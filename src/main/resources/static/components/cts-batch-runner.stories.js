import { html } from "lit";
import { expect, within, userEvent } from "storybook/test";
import "./cts-batch-runner.js";

export default {
  title: "Components/cts-batch-runner",
  component: "cts-batch-runner",
};

// Fixture shape mirrors what `plan-detail.html` writes to
// `batchRunner.modules` after the per-module `/api/info/<instance>` merge:
// `instances` is an array of instance-id strings (the backend returns
// strings); the merged status/result land on the top-level module entry
// (`status: "FINISHED"` / `result: "PASSED"`, etc.). Modules that have
// never run carry an empty `instances` and no `status` (treated as
// PENDING by `_moduleResult`).
const MOCK_MODULES_MIXED = [
  {
    testModule: "oidcc-server",
    variant: {},
    status: "FINISHED",
    result: "PASSED",
    instances: ["mock-instance-1"],
  },
  {
    testModule: "oidcc-server-rotate-keys",
    variant: {},
    status: "FINISHED",
    result: "FAILED",
    instances: ["mock-instance-2"],
  },
  {
    testModule: "oidcc-ensure-redirect-uri",
    variant: {},
    status: "FINISHED",
    result: "WARNING",
    instances: ["mock-instance-3"],
  },
  { testModule: "oidcc-codereuse", variant: {}, instances: [] },
  { testModule: "oidcc-ensure-request-object", variant: {}, instances: [] },
];

const MOCK_MODULES_ALL_DONE = [
  {
    testModule: "oidcc-server",
    variant: {},
    status: "FINISHED",
    result: "PASSED",
    instances: ["mock-instance-1"],
  },
  {
    testModule: "oidcc-server-rotate-keys",
    variant: {},
    status: "FINISHED",
    result: "PASSED",
    instances: ["mock-instance-2"],
  },
];

const MOCK_MODULES_NONE_RUN = [
  { testModule: "oidcc-server", variant: {}, instances: [] },
  { testModule: "oidcc-server-rotate-keys", variant: {}, instances: [] },
  { testModule: "oidcc-codereuse", variant: {}, instances: [] },
];

// In-flight mix: one terminal, one mid-run, one never-started. Exercises
// the three branches of `_moduleResult` (PENDING / FINISHED + result /
// transient status).
const MOCK_MODULES_IN_PROGRESS = [
  {
    testModule: "oidcc-server",
    variant: {},
    status: "FINISHED",
    result: "PASSED",
    instances: ["mock-instance-1"],
  },
  {
    testModule: "oidcc-server-rotate-keys",
    variant: {},
    status: "RUNNING",
    instances: ["mock-instance-2"],
  },
  { testModule: "oidcc-codereuse", variant: {}, instances: [] },
];

// Edge cases for the non-canonical branches of `_moduleResult`:
//   - FINISHED without a result field falls back to "REVIEW" (and emits a
//     console.warn so the backend anomaly is observable, not silent).
//   - WAITING is a known TestModule.Status value that has no entry in
//     RESULT_BADGE_VARIANTS, so the label passes through as "WAITING" and
//     the variant falls back to "skip" via the `|| "skip"` chain in
//     `_moduleVariant`.
const MOCK_MODULES_EDGE_CASES = [
  {
    testModule: "oidcc-finished-no-result",
    variant: {},
    status: "FINISHED",
    instances: ["mock-instance-1"],
  },
  {
    testModule: "oidcc-waiting",
    variant: {},
    status: "WAITING",
    instances: ["mock-instance-2"],
  },
];

export const MixedResults = {
  render: () =>
    html`<cts-batch-runner plan-id="plan-123" .modules=${MOCK_MODULES_MIXED}></cts-batch-runner>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await step("toolbar exposes both run actions", async () => {
      expect(canvas.getByText("Run All")).toBeTruthy();
      expect(canvas.getByText("Run Remaining")).toBeTruthy();
    });
    await step("renders one tile per module", async () => {
      const tiles = canvasElement.querySelectorAll(".oidf-batch-runner-tile");
      expect(tiles.length).toBe(MOCK_MODULES_MIXED.length);
    });
    await step("each result maps to its badge label and variant", async () => {
      const badges = canvasElement.querySelectorAll("cts-badge");
      const passedBadges = Array.from(badges).filter((b) => b.getAttribute("label") === "PASSED");
      expect(passedBadges.length).toBe(1);
      expect(passedBadges[0].getAttribute("variant")).toBe("pass");
      const failedBadges = Array.from(badges).filter((b) => b.getAttribute("label") === "FAILED");
      expect(failedBadges.length).toBe(1);
      expect(failedBadges[0].getAttribute("variant")).toBe("fail");
      const warningBadges = Array.from(badges).filter((b) => b.getAttribute("label") === "WARNING");
      expect(warningBadges.length).toBe(1);
      expect(warningBadges[0].getAttribute("variant")).toBe("warn");
      const pendingBadges = Array.from(badges).filter((b) => b.getAttribute("label") === "PENDING");
      expect(pendingBadges.length).toBe(2);
      expect(pendingBadges[0].getAttribute("variant")).toBe("skip");
    });
  },
};

export const AllComplete = {
  render: () =>
    html`<cts-batch-runner
      plan-id="plan-456"
      .modules=${MOCK_MODULES_ALL_DONE}
    ></cts-batch-runner>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    expect(canvas.getByText("Run All")).toBeTruthy();
    expect(canvas.queryByText("Run Remaining")).toBeNull();
  },
};

export const NoneRun = {
  render: () =>
    html`<cts-batch-runner
      plan-id="plan-789"
      .modules=${MOCK_MODULES_NONE_RUN}
    ></cts-batch-runner>`,
  async play({ canvasElement }) {
    const badges = canvasElement.querySelectorAll("cts-badge");
    for (const badge of badges) {
      expect(badge.getAttribute("label")).toBe("PENDING");
      expect(badge.getAttribute("variant")).toBe("skip");
    }
  },
};

export const RunAllEvent = {
  render: () =>
    html`<cts-batch-runner plan-id="plan-123" .modules=${MOCK_MODULES_MIXED}></cts-batch-runner>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    let eventFired = false;
    canvasElement.addEventListener("cts-run-all", () => {
      eventFired = true;
    });
    await userEvent.click(canvas.getByText("Run All"));
    expect(eventFired).toBe(true);
  },
};

export const RunRemainingEvent = {
  render: () =>
    html`<cts-batch-runner plan-id="plan-123" .modules=${MOCK_MODULES_MIXED}></cts-batch-runner>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    let eventFired = false;
    canvasElement.addEventListener("cts-run-remaining", () => {
      eventFired = true;
    });
    await userEvent.click(canvas.getByText("Run Remaining"));
    expect(eventFired).toBe(true);
  },
};

export const InProgress = {
  render: () =>
    html`<cts-batch-runner
      plan-id="plan-running"
      .modules=${MOCK_MODULES_IN_PROGRESS}
    ></cts-batch-runner>`,
  async play({ canvasElement, step }) {
    const badges = canvasElement.querySelectorAll("cts-badge");
    await step("badge labels follow module order", async () => {
      const labels = Array.from(badges).map((b) => b.getAttribute("label"));
      expect(labels).toEqual(["PASSED", "RUNNING", "PENDING"]);
    });
    await step("the in-flight module renders the running variant", async () => {
      const runningBadge = Array.from(badges).find((b) => b.getAttribute("label") === "RUNNING");
      expect(runningBadge?.getAttribute("variant")).toBe("running");
    });
  },
};

export const DataShapeEdgeCases = {
  render: () =>
    html`<cts-batch-runner
      plan-id="plan-edge-cases"
      .modules=${MOCK_MODULES_EDGE_CASES}
    ></cts-batch-runner>`,
  async play({ canvasElement, step }) {
    const badges = canvasElement.querySelectorAll("cts-badge");
    const byLabel = (label) => Array.from(badges).find((b) => b.getAttribute("label") === label);

    await step("FINISHED without a result falls back to the REVIEW badge", async () => {
      const reviewBadge = byLabel("REVIEW");
      expect(reviewBadge?.getAttribute("variant")).toBe("review");
    });

    await step("WAITING passes through with the skip variant", async () => {
      const waitingBadge = byLabel("WAITING");
      expect(waitingBadge?.getAttribute("variant")).toBe("skip");
    });
  },
};

export const EmptyModules = {
  render: () => html`<cts-batch-runner plan-id="plan-000" .modules=${[]}></cts-batch-runner>`,
  async play({ canvasElement }) {
    const tiles = canvasElement.querySelectorAll(".oidf-batch-runner-tile");
    expect(tiles.length).toBe(0);
  },
};
