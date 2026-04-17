import { html } from "lit";
import { expect, userEvent } from "storybook/test";
import "./cts-button.js";

export default {
  title: "Primitives/cts-button",
  component: "cts-button",
  argTypes: {
    variant: {
      control: "select",
      options: ["light", "info", "primary", "danger"],
    },
    label: { control: "text" },
    icon: { control: "text" },
    loading: { control: "boolean" },
    disabled: { control: "boolean" },
    fullWidth: { control: "boolean" },
    type: {
      control: "select",
      options: ["button", "submit"],
    },
  },
};

// --- Stories ---

export const Default = {
  args: { variant: "light", label: "Cancel" },
  render: ({ variant, label, loading, disabled }) =>
    html`<cts-button
      variant="${variant}"
      label="${label}"
      ?loading="${loading}"
      ?disabled="${disabled}"
    ></cts-button>`,

  async play({ canvasElement }) {
    const btn = canvasElement.querySelector("button");
    expect(btn).toBeTruthy();
    expect(btn.classList.contains("btn")).toBe(true);
    expect(btn.classList.contains("btn-sm")).toBe(true);
    expect(btn.classList.contains("btn-light")).toBe(true);
    expect(btn.classList.contains("bg-gradient")).toBe(true);
    expect(btn.classList.contains("border")).toBe(true);
    expect(btn.classList.contains("border-secondary")).toBe(true);
    expect(btn.disabled).toBe(false);
    expect(btn.textContent.trim()).toBe("Cancel");
  },
};

export const InfoWithIcon = {
  args: { variant: "info", label: "Create Test Plan", icon: "wrench-adjustable" },
  render: ({ variant, label, icon, loading, disabled }) =>
    html`<cts-button
      variant="${variant}"
      label="${label}"
      icon="${icon}"
      ?loading="${loading}"
      ?disabled="${disabled}"
    ></cts-button>`,

  async play({ canvasElement }) {
    const btn = canvasElement.querySelector("button");
    expect(btn).toBeTruthy();
    expect(btn.classList.contains("btn-info")).toBe(true);

    const icon = btn.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-wrench-adjustable")).toBe(true);
    expect(icon.getAttribute("aria-hidden")).toBe("true");
  },
};

export const Primary = {
  args: { variant: "primary", label: "Search", icon: "search" },
  render: ({ variant, label, icon, loading, disabled }) =>
    html`<cts-button
      variant="${variant}"
      label="${label}"
      icon="${icon}"
      ?loading="${loading}"
      ?disabled="${disabled}"
    ></cts-button>`,

  async play({ canvasElement }) {
    const btn = canvasElement.querySelector("button");
    expect(btn).toBeTruthy();
    expect(btn.classList.contains("btn-primary")).toBe(true);

    const icon = btn.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-search")).toBe(true);
  },
};

export const Danger = {
  args: { variant: "danger", label: "Delete", icon: "trash" },
  render: ({ variant, label, icon, loading, disabled }) =>
    html`<cts-button
      variant="${variant}"
      label="${label}"
      icon="${icon}"
      ?loading="${loading}"
      ?disabled="${disabled}"
    ></cts-button>`,

  async play({ canvasElement }) {
    const btn = canvasElement.querySelector("button");
    expect(btn).toBeTruthy();
    expect(btn.classList.contains("btn-danger")).toBe(true);

    const icon = btn.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-trash")).toBe(true);
  },
};

export const Loading = {
  args: { variant: "primary", label: "Saving...", loading: true },
  render: ({ variant, label, loading, disabled }) =>
    html`<cts-button
      variant="${variant}"
      label="${label}"
      ?loading="${loading}"
      ?disabled="${disabled}"
    ></cts-button>`,

  async play({ canvasElement }) {
    const btn = canvasElement.querySelector("button");
    expect(btn).toBeTruthy();
    expect(btn.disabled).toBe(true);

    const spinner = btn.querySelector("span.spinner-border");
    expect(spinner).toBeTruthy();
    expect(spinner.classList.contains("spinner-border-sm")).toBe(true);
    expect(spinner.getAttribute("role")).toBe("status");
    expect(spinner.getAttribute("aria-hidden")).toBe("true");

    // No icon when loading
    const icon = btn.querySelector("span.bi");
    expect(icon).toBeNull();
  },
};

export const Disabled = {
  args: { variant: "light", label: "Cancel", disabled: true },
  render: ({ variant, label, loading, disabled }) =>
    html`<cts-button
      variant="${variant}"
      label="${label}"
      ?loading="${loading}"
      ?disabled="${disabled}"
    ></cts-button>`,

  async play({ canvasElement }) {
    const btn = canvasElement.querySelector("button");
    expect(btn).toBeTruthy();
    expect(btn.disabled).toBe(true);
  },
};

export const ClickEvent = {
  args: { variant: "primary", label: "Click me" },
  render: ({ variant, label, loading, disabled }) =>
    html`<cts-button
      variant="${variant}"
      label="${label}"
      ?loading="${loading}"
      ?disabled="${disabled}"
    ></cts-button>`,

  async play({ canvasElement }) {
    const btn = canvasElement.querySelector("button");
    expect(btn).toBeTruthy();

    let eventFired = false;
    canvasElement.addEventListener("cts-click", () => {
      eventFired = true;
    });

    await userEvent.click(btn);
    expect(eventFired).toBe(true);
  },
};

export const DisabledNoEvent = {
  args: { variant: "light", label: "Cancel", disabled: true },
  render: ({ variant, label, loading, disabled }) =>
    html`<cts-button
      variant="${variant}"
      label="${label}"
      ?loading="${loading}"
      ?disabled="${disabled}"
    ></cts-button>`,

  async play({ canvasElement }) {
    const btn = canvasElement.querySelector("button");
    expect(btn).toBeTruthy();
    expect(btn.disabled).toBe(true);

    let eventFired = false;
    canvasElement.addEventListener("cts-click", () => {
      eventFired = true;
    });

    // Click a disabled button — userEvent should not trigger the click handler
    await userEvent.click(btn, { pointerEventsCheck: 0 });
    expect(eventFired).toBe(false);
  },
};

export const FullWidth = {
  args: {
    variant: "info",
    icon: "wrench-adjustable",
    label: "View Config",
  },
  render: ({ variant, label, icon, disabled }) => html`
    <div style="width: 400px; padding: 1rem; border: 1px dashed #ccc;">
      <cts-button
        variant="${variant}"
        label="${label}"
        icon="${icon}"
        ?disabled="${disabled}"
        full-width
      ></cts-button>
    </div>
  `,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-button");
    await host.updateComplete;
    const btn = host.querySelector("button");
    expect(btn).toBeTruthy();
    expect(btn.classList.contains("w-100")).toBe(true);
    expect(host.style.display).toBe("block");
  },
};

/**
 * Stack of full-width buttons, the canonical action-list pattern used in the
 * homepage, login page, and plan/log detail headers. The wrapping
 * `.d-grid.gap-1` + `full-width` per child gives uniform stretched buttons
 * with consistent vertical spacing.
 *
 * Pair `cts-button` (for click handlers) with `cts-link-button` (for
 * navigation) inside the same grid — both honour `full-width` identically.
 */
export const FullWidthStack = {
  render: () => html`
    <div style="width: 240px; padding: 1rem; border: 1px dashed #ccc;">
      <div class="d-grid gap-1">
        <cts-button variant="light" icon="wrench-adjustable" label="View Config" full-width></cts-button>
        <cts-button variant="light" icon="save2" label="Download all Logs" full-width></cts-button>
        <cts-button variant="light" icon="bookmarks" label="Publish everything" full-width></cts-button>
        <cts-button variant="light" icon="bookmarks" label="Private link" full-width></cts-button>
        <cts-button variant="danger" icon="trash" label="Delete plan" full-width></cts-button>
      </div>
    </div>
  `,
};

export const AllVariants = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem;">
      <cts-button variant="light" label="Light"></cts-button>
      <cts-button variant="info" label="Info" icon="info-circle-fill"></cts-button>
      <cts-button variant="primary" label="Primary" icon="search"></cts-button>
      <cts-button variant="danger" label="Danger" icon="trash"></cts-button>
    </div>
  `,
};
