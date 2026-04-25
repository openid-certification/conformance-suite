import { html } from "lit";
import { expect, userEvent } from "storybook/test";
import "./cts-alert.js";

export default {
  title: "Primitives/cts-alert",
  component: "cts-alert",
  argTypes: {
    variant: {
      control: "select",
      options: ["info", "success", "warning", "danger"],
    },
    dismissible: { control: "boolean" },
  },
};

// --- Stories ---

export const Info = {
  args: { variant: "info" },
  render: ({ variant, dismissible }) => html`
    <cts-alert variant="${variant}" ?dismissible="${dismissible}">
      <strong>Info:</strong> the test is currently running.
    </cts-alert>
  `,

  async play({ canvasElement }) {
    const alert = canvasElement.querySelector(".oidf-alert");
    expect(alert).toBeTruthy();
    expect(alert.classList.contains("oidf-alert-info")).toBe(true);
    expect(alert.getAttribute("role")).toBe("alert");
    expect(alert.textContent).toContain("test is currently running");
    // Bootstrap classes must NOT leak through.
    expect(alert.classList.contains("alert")).toBe(false);
    expect(alert.classList.contains("alert-info")).toBe(false);
  },
};

export const Success = {
  args: { variant: "success" },
  render: ({ variant }) => html`
    <cts-alert variant="${variant}">
      <strong>The test has completed successfully!</strong>
    </cts-alert>
  `,

  async play({ canvasElement }) {
    const alert = canvasElement.querySelector(".oidf-alert");
    expect(alert.classList.contains("oidf-alert-success")).toBe(true);
  },
};

export const Warning = {
  args: { variant: "warning" },
  render: ({ variant }) => html`
    <cts-alert variant="${variant}">
      <strong>This test is no longer running.</strong>
      Values exported from the test are available for review.
    </cts-alert>
  `,

  async play({ canvasElement }) {
    const alert = canvasElement.querySelector(".oidf-alert");
    expect(alert.classList.contains("oidf-alert-warning")).toBe(true);
  },
};

export const Danger = {
  args: { variant: "danger" },
  render: ({ variant }) => html`
    <cts-alert variant="${variant}">
      <strong>Error:</strong> the request failed with status 500.
    </cts-alert>
  `,

  async play({ canvasElement }) {
    const alert = canvasElement.querySelector(".oidf-alert");
    expect(alert.classList.contains("oidf-alert-danger")).toBe(true);
  },
};

/**
 * Setting `dismissible` renders a close button (icon-only ghost button with
 * `aria-label="Close"`). Clicking it removes the alert from the DOM and
 * dispatches a `cts-alert-dismissed` event that bubbles for parent listeners.
 */
export const Dismissible = {
  args: { variant: "info", dismissible: true },
  render: ({ variant, dismissible }) => html`
    <cts-alert variant="${variant}" ?dismissible="${dismissible}">
      <strong>This alert dismisses on close.</strong>
      Click the × to remove it.
    </cts-alert>
  `,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-alert");
    const alert = canvasElement.querySelector(".oidf-alert");
    expect(alert).toBeTruthy();

    const closeBtn = alert.querySelector("button.oidf-alert-close");
    expect(closeBtn).toBeTruthy();
    expect(closeBtn.getAttribute("aria-label")).toBe("Close");
    // Bootstrap's btn-close class must NOT be emitted.
    expect(closeBtn.classList.contains("btn-close")).toBe(false);

    let dismissed = false;
    host.addEventListener("cts-alert-dismissed", () => {
      dismissed = true;
    });

    await userEvent.click(closeBtn);
    expect(dismissed).toBe(true);
    expect(canvasElement.querySelector("cts-alert")).toBeNull();
  },
};

/**
 * Unknown `variant` values fall back to `info` (matches the defensive
 * fallback used by `cts-button` and `cts-badge`).
 */
export const VariantFallback = {
  render: () => html`
    <cts-alert variant="bogus">
      <strong>Unknown variant.</strong> Falls back to info styling.
    </cts-alert>
  `,

  async play({ canvasElement }) {
    const alert = canvasElement.querySelector(".oidf-alert");
    expect(alert.classList.contains("oidf-alert-info")).toBe(true);
  },
};

/**
 * The default slot accepts any HTML, including links, inline emphasis, and
 * other custom elements. This is essential for the `templates/logHeader.html`
 * migration where alerts wrap `<strong>` headings, `<em>` emphasis, and links.
 */
export const NestedContent = {
  render: () => html`
    <cts-alert variant="info">
      <strong>Heading</strong>
      <p>Paragraph below with <em>emphasis</em>.</p>
      <a href="#">A link inside the alert.</a>
    </cts-alert>
  `,

  async play({ canvasElement }) {
    const alert = canvasElement.querySelector(".oidf-alert");
    const body = alert.querySelector(".oidf-alert-body");
    expect(body).toBeTruthy();
    expect(body.querySelector("strong")).toBeTruthy();
    expect(body.querySelector("em").textContent).toBe("emphasis");
    expect(body.querySelector("p").textContent).toContain("Paragraph below");
    expect(body.querySelector("a").getAttribute("href")).toBe("#");
  },
};
