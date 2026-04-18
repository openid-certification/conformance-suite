import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-link-button.js";

export default {
  title: "Primitives/cts-link-button",
  component: "cts-link-button",
  argTypes: {
    href: { control: "text" },
    variant: {
      control: "select",
      options: ["light", "info", "primary", "danger"],
    },
    size: {
      control: "select",
      options: ["sm", "md", "lg"],
    },
    label: { control: "text" },
    icon: { control: "text" },
    disabled: { control: "boolean" },
    fullWidth: { control: "boolean" },
  },
};

// --- Stories ---

export const Default = {
  args: { href: "tokens.html", variant: "light", label: "Tokens" },
  render: ({ href, variant, label, disabled }) =>
    html`<cts-link-button
      href="${href}"
      variant="${variant}"
      label="${label}"
      ?disabled="${disabled}"
    ></cts-link-button>`,

  async play({ canvasElement }) {
    const anchor = canvasElement.querySelector("a");
    expect(anchor).toBeTruthy();
    expect(anchor.getAttribute("href")).toBe("tokens.html");
    expect(anchor.classList.contains("btn")).toBe(true);
    expect(anchor.classList.contains("btn-sm")).toBe(true);
    expect(anchor.classList.contains("btn-light")).toBe(true);
    expect(anchor.classList.contains("bg-gradient")).toBe(true);
    expect(anchor.classList.contains("border")).toBe(true);
    expect(anchor.classList.contains("border-secondary")).toBe(true);
    expect(anchor.getAttribute("role")).toBe("button");
    expect(anchor.textContent.trim()).toBe("Tokens");
  },
};

export const WithIcon = {
  args: { variant: "info", icon: "file-earmark", label: "View Log" },
  render: ({ variant, label, icon, disabled }) =>
    html`<cts-link-button
      variant="${variant}"
      label="${label}"
      icon="${icon}"
      ?disabled="${disabled}"
    ></cts-link-button>`,

  async play({ canvasElement }) {
    const anchor = canvasElement.querySelector("a");
    expect(anchor).toBeTruthy();
    expect(anchor.classList.contains("btn-info")).toBe(true);

    const iconEl = anchor.querySelector("span.bi");
    expect(iconEl).toBeTruthy();
    expect(iconEl.classList.contains("bi-file-earmark")).toBe(true);
    expect(iconEl.getAttribute("aria-hidden")).toBe("true");
  },
};

export const Disabled = {
  args: { href: "tokens.html", variant: "light", label: "Tokens", disabled: true },
  render: ({ href, variant, label, disabled }) =>
    html`<cts-link-button
      href="${href}"
      variant="${variant}"
      label="${label}"
      ?disabled="${disabled}"
    ></cts-link-button>`,

  async play({ canvasElement }) {
    const anchor = canvasElement.querySelector("a");
    expect(anchor).toBeTruthy();
    expect(anchor.classList.contains("disabled")).toBe(true);
    expect(anchor.getAttribute("aria-disabled")).toBe("true");
    expect(anchor.getAttribute("tabindex")).toBe("-1");
    expect(anchor.hasAttribute("href")).toBe(false);
  },
};

/**
 * Stretches the button to fill its parent's width via the `full-width`
 * boolean attribute. The component sets `display: block` on its host and adds
 * `w-100` to the inner anchor — equivalent behavior to `cts-button` with the
 * same attribute. Use inside `.d-grid` containers for action stacks.
 */
export const FullWidth = {
  args: {
    href: "schedule-test.html",
    variant: "info",
    icon: "files",
    label: "Create a new test plan",
  },
  render: ({ href, variant, label, icon, disabled }) => html`
    <div style="width: 400px; padding: 1rem; border: 1px dashed #ccc;">
      <cts-link-button
        href="${href}"
        variant="${variant}"
        label="${label}"
        icon="${icon}"
        ?disabled="${disabled}"
        full-width
      ></cts-link-button>
    </div>
  `,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-link-button");
    await host.updateComplete;
    const anchor = host.querySelector("a");
    expect(anchor).toBeTruthy();
    expect(host.fullWidth).toBe(true);
    expect(anchor.classList.contains("w-100")).toBe(true);
    expect(host.style.display).toBe("block");
  },
};

export const AllVariants = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem;">
      <cts-link-button href="tokens.html" variant="light" label="Light"></cts-link-button>
      <cts-link-button
        href="logs.html"
        variant="info"
        label="Info"
        icon="file-earmark"
      ></cts-link-button>
      <cts-link-button
        href="schedule-test.html"
        variant="primary"
        label="Primary"
        icon="plus-circle"
      ></cts-link-button>
      <cts-link-button href="#" variant="danger" label="Danger" icon="trash"></cts-link-button>
    </div>
  `,
};

/**
 * The `size` attribute mirrors `cts-button`. Login provider buttons on
 * `login.html` and the action buttons in `tokenTable.html` use `lg` for
 * prominence; everything else stays on the `sm` default.
 */
export const Sizes = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem; align-items: center;">
      <cts-link-button
        href="#"
        variant="primary"
        label="Small (default)"
        size="sm"
      ></cts-link-button>
      <cts-link-button href="#" variant="primary" label="Medium" size="md"></cts-link-button>
      <cts-link-button href="#" variant="primary" label="Large" size="lg"></cts-link-button>
    </div>
  `,

  async play({ canvasElement }) {
    const anchors = canvasElement.querySelectorAll("cts-link-button a");
    expect(anchors.length).toBe(3);
    expect(anchors[0].classList.contains("btn-sm")).toBe(true);
    expect(anchors[1].classList.contains("btn-sm")).toBe(false);
    expect(anchors[1].classList.contains("btn-lg")).toBe(false);
    expect(anchors[2].classList.contains("btn-lg")).toBe(true);
  },
};

/**
 * Unknown `size` values fall back to `sm` (matches `variant`'s defensive
 * fallback).
 */
export const SizeFallback = {
  render: () => html`
    <cts-link-button href="#" variant="primary" label="Bogus" size="huge"></cts-link-button>
  `,

  async play({ canvasElement }) {
    const anchor = canvasElement.querySelector("cts-link-button a");
    expect(anchor.classList.contains("btn-sm")).toBe(true);
    expect(anchor.classList.contains("btn-lg")).toBe(false);
  },
};
