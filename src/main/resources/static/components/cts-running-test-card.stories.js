import { html } from "lit";
import { expect, within, userEvent } from "storybook/test";
import { MOCK_RUNNING_TESTS } from "@fixtures/mock-test-data.js";
import "./cts-running-test-card.js";

export default {
  title: "Components/cts-running-test-card",
  component: "cts-running-test-card",
};

const RUNNING_TEST = MOCK_RUNNING_TESTS[0]; // status: RUNNING
const WAITING_TEST = MOCK_RUNNING_TESTS[1]; // status: WAITING

const INTERRUPTED_TEST = {
  ...RUNNING_TEST,
  _id: "test-interrupted-001",
  testName: "oidcc-ensure-redirect-uri",
  status: "INTERRUPTED",
};

// --- Stories ---

export const Running = {
  render: () => html`<cts-running-test-card .test=${RUNNING_TEST}></cts-running-test-card>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Test name displayed
    expect(canvas.getByText("oidcc-server")).toBeInTheDocument();

    // Test ID displayed
    expect(canvas.getByText(RUNNING_TEST._id)).toBeInTheDocument();

    // Status badge shows RUNNING with info variant
    const badge = canvasElement.querySelector("cts-badge");
    expect(badge).toBeTruthy();
    expect(badge.getAttribute("label")).toBe("RUNNING");
    expect(badge.getAttribute("variant")).toBe("info");

    // Created date is rendered
    expect(canvas.getByText("Created:")).toBeInTheDocument();

    // Buttons present
    expect(canvas.getByText(/Download Logs/)).toBeInTheDocument();
    expect(canvas.getByText(/View Test Details/)).toBeInTheDocument();

    // View Test Details link has correct href
    const detailLink = canvas.getByText(/View Test Details/).closest("a");
    expect(detailLink.getAttribute("href")).toContain("log-detail.html?log=");
    expect(detailLink.getAttribute("href")).toContain(encodeURIComponent(RUNNING_TEST._id));

    // Owner row NOT visible (isAdmin is false by default)
    const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
    expect(ownerRow).toBeNull();
  },
};

export const Waiting = {
  render: () => html`<cts-running-test-card .test=${WAITING_TEST}></cts-running-test-card>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Test name displayed
    expect(canvas.getByText("oidcc-server-rotate-keys")).toBeInTheDocument();

    // Status badge shows WAITING with warning variant
    const badge = canvasElement.querySelector("cts-badge");
    expect(badge).toBeTruthy();
    expect(badge.getAttribute("label")).toBe("WAITING");
    expect(badge.getAttribute("variant")).toBe("warning");

    // Variant info displayed
    expect(canvas.getByText("Variant:")).toBeInTheDocument();

    // Buttons present
    expect(canvas.getByText(/Download Logs/)).toBeInTheDocument();
    expect(canvas.getByText(/View Test Details/)).toBeInTheDocument();
  },
};

export const Interrupted = {
  render: () => html`<cts-running-test-card .test=${INTERRUPTED_TEST}></cts-running-test-card>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Test name displayed
    expect(canvas.getByText("oidcc-ensure-redirect-uri")).toBeInTheDocument();

    // Status badge shows INTERRUPTED with interrupted variant
    const badge = canvasElement.querySelector("cts-badge");
    expect(badge).toBeTruthy();
    expect(badge.getAttribute("label")).toBe("INTERRUPTED");
    expect(badge.getAttribute("variant")).toBe("interrupted");

    // Buttons present
    expect(canvas.getByText(/Download Logs/)).toBeInTheDocument();
    expect(canvas.getByText(/View Test Details/)).toBeInTheDocument();
  },
};

export const AdminView = {
  render: () => html`<cts-running-test-card .test=${RUNNING_TEST} is-admin></cts-running-test-card>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Owner row IS visible
    const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
    expect(ownerRow).toBeTruthy();
    expect(canvas.getByText("Test Owner:")).toBeInTheDocument();

    // Owner info is rendered
    expect(canvas.getByText(/12345/)).toBeInTheDocument();
    expect(canvas.getByText(/accounts\.google\.com/)).toBeInTheDocument();

    // All other fields still present
    expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    expect(canvas.getByText(RUNNING_TEST._id)).toBeInTheDocument();
    expect(canvas.getByText(/Download Logs/)).toBeInTheDocument();
    expect(canvas.getByText(/View Test Details/)).toBeInTheDocument();
  },
};

export const DownloadClick = {
  render: () => html`<cts-running-test-card .test=${RUNNING_TEST}></cts-running-test-card>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    let eventFired = false;
    let eventDetail = null;
    canvasElement.addEventListener("cts-download-log", (e) => {
      eventFired = true;
      eventDetail = e.detail;
    });

    const downloadBtn = canvas.getByText(/Download Logs/);
    await userEvent.click(downloadBtn);

    expect(eventFired).toBe(true);
    expect(eventDetail.testId).toBe(RUNNING_TEST._id);
  },
};
