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
    label: { control: "text" },
    icon: { control: "text" },
    disabled: { control: "boolean" },
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

export const AllVariants = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem;">
      <cts-link-button href="tokens.html" variant="light" label="Light"></cts-link-button>
      <cts-link-button href="logs.html" variant="info" label="Info" icon="file-earmark"></cts-link-button>
      <cts-link-button href="schedule-test.html" variant="primary" label="Primary" icon="plus-circle"></cts-link-button>
      <cts-link-button href="#" variant="danger" label="Danger" icon="trash"></cts-link-button>
    </div>
  `,
};
