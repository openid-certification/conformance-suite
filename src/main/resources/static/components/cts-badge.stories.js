import { html } from "lit";
import { expect, userEvent } from "storybook/test";
import "./cts-badge.js";

export default {
  title: "Primitives/cts-badge",
  component: "cts-badge",
  argTypes: {
    variant: { control: "text" },
    label: { control: "text" },
    count: { control: "number" },
    pill: { control: "boolean" },
    clickable: { control: "boolean" },
  },
};

// --- Stories ---

export const Success = {
  args: { variant: "success", label: "SUCCESS" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge.result-success");
    expect(badge).toBeTruthy();
    expect(badge.textContent.trim()).toBe("SUCCESS");
  },
};

export const Failure = {
  args: { variant: "failure", label: "FAILURE" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge.result-failure");
    expect(badge).toBeTruthy();
  },
};

export const Warning = {
  args: { variant: "warning", label: "WARNING" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge.result-warning");
    expect(badge).toBeTruthy();
  },
};

export const BootstrapVariant = {
  args: { variant: "danger", label: "ADMIN" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge.bg-danger");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("result-danger")).toBe(false);
  },
};

export const WithCount = {
  args: { variant: "secondary", count: 5, pill: true },
  render: ({ variant, count, pill }) =>
    html`<cts-badge
      variant="${variant}"
      count="${count}"
      ?pill="${pill}"
    ></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.textContent.trim()).toBe("5");
    expect(badge.classList.contains("rounded-pill")).toBe(true);
    expect(badge.classList.contains("bg-secondary")).toBe(true);
  },
};

export const Clickable = {
  args: { variant: "info", label: "Click me", clickable: true },
  render: ({ variant, label, clickable }) =>
    html`<cts-badge
      variant="${variant}"
      label="${label}"
      ?clickable="${clickable}"
    ></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.getAttribute("role")).toBe("button");
    expect(badge.getAttribute("tabindex")).toBe("0");

    let clicked = false;
    canvasElement.addEventListener("cts-badge-click", () => {
      clicked = true;
    });

    await userEvent.click(badge);
    expect(clicked).toBe(true);
  },
};

export const NotClickable = {
  args: { variant: "info", label: "Not clickable" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.getAttribute("role")).toBeNull();
    expect(badge.getAttribute("tabindex")).toBeNull();

    let clicked = false;
    canvasElement.addEventListener("cts-badge-click", () => {
      clicked = true;
    });

    await userEvent.click(badge);
    expect(clicked).toBe(false);
  },
};

export const AllResultVariants = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem;">
      ${[
        "success",
        "failure",
        "warning",
        "review",
        "skipped",
        "interrupted",
        "info",
        "finished",
      ].map(
        (variant) =>
          html`<cts-badge
            variant="${variant}"
            label="${variant.toUpperCase()}"
          ></cts-badge>`,
      )}
    </div>
  `,
};

export const CountPrefersOverLabel = {
  args: { variant: "success", count: 42, label: "Ignored" },
  render: ({ variant, count, label }) =>
    html`<cts-badge
      variant="${variant}"
      count="${count}"
      label="${label}"
    ></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.textContent.trim()).toBe("42");
  },
};
