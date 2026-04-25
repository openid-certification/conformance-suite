import { html } from "lit";
import { expect, within, userEvent } from "storybook/test";
import "./cts-batch-runner.js";

export default {
  title: "Components/cts-batch-runner",
  component: "cts-batch-runner",
};

const MOCK_MODULES_MIXED = [
  { testModule: "oidcc-server", variant: {}, instances: [{ result: "PASSED" }] },
  { testModule: "oidcc-server-rotate-keys", variant: {}, instances: [{ result: "FAILED" }] },
  { testModule: "oidcc-ensure-redirect-uri", variant: {}, instances: [{ result: "WARNING" }] },
  { testModule: "oidcc-codereuse", variant: {}, instances: [] },
  { testModule: "oidcc-ensure-request-object", variant: {}, instances: [] },
];

const MOCK_MODULES_ALL_DONE = [
  { testModule: "oidcc-server", variant: {}, instances: [{ result: "PASSED" }] },
  { testModule: "oidcc-server-rotate-keys", variant: {}, instances: [{ result: "PASSED" }] },
];

const MOCK_MODULES_NONE_RUN = [
  { testModule: "oidcc-server", variant: {}, instances: [] },
  { testModule: "oidcc-server-rotate-keys", variant: {}, instances: [] },
  { testModule: "oidcc-codereuse", variant: {}, instances: [] },
];

export const MixedResults = {
  render: () =>
    html`<cts-batch-runner plan-id="plan-123" .modules=${MOCK_MODULES_MIXED}></cts-batch-runner>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    expect(canvas.getByText("Run All")).toBeTruthy();
    expect(canvas.getByText("Run Remaining")).toBeTruthy();
    const tiles = canvasElement.querySelectorAll(".oidf-batch-runner-tile");
    expect(tiles.length).toBe(MOCK_MODULES_MIXED.length);
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
    // Sanity check: the canonical b-pass / b-fail / b-warn / b-skip classes
    // from cts-badge are present on the rendered inner spans.
    const passSpan = passedBadges[0].querySelector(".badge");
    expect(passSpan?.classList.contains("b-pass")).toBe(true);
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

export const EmptyModules = {
  render: () => html`<cts-batch-runner plan-id="plan-000" .modules=${[]}></cts-batch-runner>`,
  async play({ canvasElement }) {
    const tiles = canvasElement.querySelectorAll(".oidf-batch-runner-tile");
    expect(tiles.length).toBe(0);
  },
};
