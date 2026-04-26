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

    // Status badge shows RUNNING with the design-system `running` variant
    const badge = canvasElement.querySelector("cts-badge");
    expect(badge).toBeTruthy();
    expect(badge.getAttribute("label")).toBe("RUNNING");
    expect(badge.getAttribute("variant")).toBe("running");

    // The running variant renders the design-system spinning circular SVG
    // (an inline <svg> wrapped in .cts-badge-spin).
    const spinner = canvasElement.querySelector("cts-badge .cts-badge-spin svg");
    expect(spinner).toBeTruthy();

    // Created date is rendered
    expect(canvas.getByText("Created:")).toBeInTheDocument();

    // Buttons present
    expect(canvas.getByText(/Download Logs/)).toBeInTheDocument();
    expect(canvas.getByText(/View Test Details/)).toBeInTheDocument();

    // View Test Details link has correct href
    const detailLink = /** @type {HTMLAnchorElement} */ (
      canvas.getByText(/View Test Details/).closest("a")
    );
    expect(detailLink.getAttribute("href")).toContain("log-detail.html?log=");
    expect(detailLink.getAttribute("href")).toContain(encodeURIComponent(RUNNING_TEST._id));

    // Owner row NOT visible (isAdmin is false by default)
    const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
    expect(ownerRow).toBeNull();

    // No progress bar when `progress` is unset
    const progressBar = canvasElement.querySelector('[role="progressbar"]');
    expect(progressBar).toBeNull();
  },
};

export const Waiting = {
  render: () => html`<cts-running-test-card .test=${WAITING_TEST}></cts-running-test-card>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Test name displayed
    expect(canvas.getByText("oidcc-server-rotate-keys")).toBeInTheDocument();

    // Status badge shows WAITING with the design-system `warn` variant
    const badge = canvasElement.querySelector("cts-badge");
    expect(badge).toBeTruthy();
    expect(badge.getAttribute("label")).toBe("WAITING");
    expect(badge.getAttribute("variant")).toBe("warn");

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

    // INTERRUPTED status maps to the canonical `fail` variant — see
    // STATUS_BADGE_VARIANTS in cts-running-test-card.js.
    const badge = canvasElement.querySelector("cts-badge");
    expect(badge).toBeTruthy();
    expect(badge.getAttribute("label")).toBe("INTERRUPTED");
    expect(badge.getAttribute("variant")).toBe("fail");

    // Buttons present
    expect(canvas.getByText(/Download Logs/)).toBeInTheDocument();
    expect(canvas.getByText(/View Test Details/)).toBeInTheDocument();
  },
};

export const AdminView = {
  render: () =>
    html`<cts-running-test-card .test=${RUNNING_TEST} is-admin></cts-running-test-card>`,
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

/**
 * Asserts the orange-400 progress bar advances on prop change. Starts at
 * 35%, then bumps to 80% and verifies the inline `width` style follows.
 */
export const WithProgressBar = {
  render: () =>
    html`<cts-running-test-card .test=${RUNNING_TEST} .progress=${35}></cts-running-test-card>`,
  async play({ canvasElement }) {
    const card =
      /** @type {HTMLElement & { progress: number, updateComplete: Promise<unknown> }} */ (
        canvasElement.querySelector("cts-running-test-card")
      );
    expect(card).toBeTruthy();

    // Initial 35% fill rendered with the inline width style and
    // accessible role="progressbar" semantics.
    const progressBar = canvasElement.querySelector('[role="progressbar"]');
    expect(progressBar).toBeTruthy();
    expect(progressBar.getAttribute("aria-valuenow")).toBe("35");
    expect(progressBar.getAttribute("aria-valuemin")).toBe("0");
    expect(progressBar.getAttribute("aria-valuemax")).toBe("100");

    const fill = /** @type {HTMLElement} */ (canvasElement.querySelector(".cts-rtc-progress-fill"));
    expect(fill).toBeTruthy();
    expect(fill.style.width).toBe("35%");

    // Advance the prop; the bar should follow on the next render.
    card.progress = 80;
    await card.updateComplete;

    const updatedBar = canvasElement.querySelector('[role="progressbar"]');
    expect(updatedBar.getAttribute("aria-valuenow")).toBe("80");

    const updatedFill = /** @type {HTMLElement} */ (
      canvasElement.querySelector(".cts-rtc-progress-fill")
    );
    expect(updatedFill.style.width).toBe("80%");
  },
};

export const DownloadClick = {
  render: () => html`<cts-running-test-card .test=${RUNNING_TEST}></cts-running-test-card>`,
  async play({ canvasElement }) {
    let eventFired = false;
    /** @type {any} */
    let eventDetail = null;
    canvasElement.addEventListener("cts-download-log", (e) => {
      eventFired = true;
      eventDetail = /** @type {CustomEvent} */ (e).detail;
    });

    // The Download Logs control is now a cts-button — click its inner
    // <button>, which is what bubbles native + cts-click events.
    const downloadHost = /** @type {HTMLElement} */ (
      canvasElement.querySelector("cts-button.downloadBtn")
    );
    expect(downloadHost).toBeTruthy();
    const innerBtn = /** @type {HTMLButtonElement} */ (downloadHost.querySelector("button"));
    expect(innerBtn).toBeTruthy();
    await userEvent.click(innerBtn);

    expect(eventFired).toBe(true);
    expect(eventDetail.testId).toBe(RUNNING_TEST._id);
  },
};
