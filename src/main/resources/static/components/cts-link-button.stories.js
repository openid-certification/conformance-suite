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
      options: ["primary", "secondary", "ghost", "danger"],
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
  args: { href: "tokens.html", variant: "secondary", label: "Tokens" },
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
    expect(anchor.classList.contains("oidf-btn")).toBe(true);
    expect(anchor.classList.contains("oidf-btn-sm")).toBe(true);
    expect(anchor.classList.contains("oidf-btn-secondary")).toBe(true);
    expect(anchor.getAttribute("role")).toBe("button");
    expect(anchor.textContent.trim()).toBe("Tokens");
    // Ensure the legacy Bootstrap btn classes are no longer rendered.
    expect(anchor.classList.contains("btn")).toBe(false);
    expect(anchor.classList.contains("btn-light")).toBe(false);
  },
};

export const WithIcon = {
  args: { variant: "primary", icon: "file-blank", label: "View Log" },
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
    expect(anchor.classList.contains("oidf-btn")).toBe(true);
    expect(anchor.classList.contains("oidf-btn-primary")).toBe(true);

    const iconEl = anchor.querySelector("cts-icon");
    expect(iconEl).toBeTruthy();
    expect(iconEl.getAttribute("name")).toBe("file-blank");
    expect(iconEl.getAttribute("aria-hidden")).toBe("true");
  },
};

export const Ghost = {
  args: { href: "logs.html", variant: "ghost", label: "View log" },
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
    expect(anchor.classList.contains("oidf-btn-ghost")).toBe(true);
  },
};

export const Disabled = {
  args: { href: "tokens.html", variant: "secondary", label: "Tokens", disabled: true },
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
    expect(anchor.getAttribute("aria-disabled")).toBe("true");
    expect(anchor.getAttribute("tabindex")).toBe("-1");
    expect(anchor.hasAttribute("href")).toBe(false);
  },
};

/**
 * Stretches the button to fill its parent's width via the `full-width`
 * boolean attribute. The component is laid out as a block via the
 * `cts-link-button[full-width]` rule injected by the component (matches
 * `cts-button`). Use inside `.d-grid` containers for action stacks.
 */
export const FullWidth = {
  args: {
    href: "schedule-test.html",
    variant: "primary",
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
    expect(getComputedStyle(host).display).toBe("block");
  },
};

export const AllVariants = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem;">
      <cts-link-button href="tokens.html" variant="secondary" label="Secondary"></cts-link-button>
      <cts-link-button
        href="logs.html"
        variant="primary"
        label="Primary"
        icon="file-blank"
      ></cts-link-button>
      <cts-link-button href="logs.html" variant="ghost" label="Ghost"></cts-link-button>
      <cts-link-button
        href="#"
        variant="danger"
        label="Danger"
        icon="trash-empty"
      ></cts-link-button>
    </div>
  `,
};

/**
 * Legacy variant aliases mirror the `cts-button` table — they exist so that
 * call sites still passing `light`, `info`, `success`, `warning`, `dark`,
 * or `outline-*` continue to render the closest new OIDF variant.
 */
export const LegacyVariantAliases = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem;">
      <cts-link-button href="#" variant="light" label="light → secondary"></cts-link-button>
      <cts-link-button href="#" variant="info" label="info → primary"></cts-link-button>
      <cts-link-button
        href="#"
        variant="outline-primary"
        label="outline-primary → primary"
      ></cts-link-button>
    </div>
  `,

  async play({ canvasElement }) {
    const anchors = canvasElement.querySelectorAll("cts-link-button a");
    expect(anchors.length).toBe(3);
    expect(anchors[0].classList.contains("oidf-btn-secondary")).toBe(true);
    expect(anchors[1].classList.contains("oidf-btn-primary")).toBe(true);
    expect(anchors[2].classList.contains("oidf-btn-primary")).toBe(true);
  },
};

/**
 * The `size` attribute mirrors `cts-button` — including the new `xs` for
 * dense rows. Login provider buttons on `login.html` and the action buttons
 * in `cts-token-manager` use `lg` for prominence; everything else stays on
 * the `sm` default.
 */
export const Sizes = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem; align-items: center;">
      <cts-link-button href="#" variant="primary" label="Extra small" size="xs"></cts-link-button>
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
    expect(anchors.length).toBe(4);
    expect(anchors[0].classList.contains("oidf-btn-xs")).toBe(true);
    expect(anchors[1].classList.contains("oidf-btn-sm")).toBe(true);
    expect(anchors[2].classList.contains("oidf-btn-sm")).toBe(false);
    expect(anchors[2].classList.contains("oidf-btn-lg")).toBe(false);
    expect(anchors[3].classList.contains("oidf-btn-lg")).toBe(true);
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
    expect(anchor.classList.contains("oidf-btn-sm")).toBe(true);
    expect(anchor.classList.contains("oidf-btn-lg")).toBe(false);
  },
};
