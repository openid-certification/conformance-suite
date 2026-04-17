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
    const alert = canvasElement.querySelector(".alert");
    expect(alert).toBeTruthy();
    expect(alert.classList.contains("alert-info")).toBe(true);
    expect(alert.getAttribute("role")).toBe("alert");
    expect(alert.textContent).toContain("test is currently running");
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
    const alert = canvasElement.querySelector(".alert");
    expect(alert.classList.contains("alert-success")).toBe(true);
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
    const alert = canvasElement.querySelector(".alert");
    expect(alert.classList.contains("alert-warning")).toBe(true);
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
    const alert = canvasElement.querySelector(".alert");
    expect(alert.classList.contains("alert-danger")).toBe(true);
  },
};

/**
 * Setting `dismissible` renders a Bootstrap close button. Clicking it removes
 * the alert from the DOM and dispatches a `cts-alert-dismissed` event that
 * bubbles for parent listeners.
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
    const alert = canvasElement.querySelector(".alert");
    expect(alert.classList.contains("alert-dismissible")).toBe(true);

    const closeBtn = alert.querySelector("button.btn-close");
    expect(closeBtn).toBeTruthy();
    expect(closeBtn.getAttribute("aria-label")).toBe("Close");

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
    const alert = canvasElement.querySelector(".alert");
    expect(alert.classList.contains("alert-info")).toBe(true);
  },
};

/**
 * The default slot accepts any HTML, including other custom elements. This
 * is essential for the `templates/logHeader.html` migration where alerts wrap
 * `<strong>` headings, links, and inline icons.
 */
export const NestedContent = {
  render: () => html`
    <cts-alert variant="info">
      <strong>Heading</strong>
      <p>Paragraph below.</p>
      <a href="#">A link inside the alert.</a>
    </cts-alert>
  `,

  async play({ canvasElement }) {
    const alert = canvasElement.querySelector(".alert");
    expect(alert.querySelector("strong")).toBeTruthy();
    expect(alert.querySelector("p").textContent).toBe("Paragraph below.");
    expect(alert.querySelector("a").getAttribute("href")).toBe("#");
  },
};
