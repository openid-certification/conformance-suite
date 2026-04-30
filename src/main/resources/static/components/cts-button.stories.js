import { html } from "lit";
import { expect, userEvent } from "storybook/test";
import "./cts-button.js";

export default {
  title: "Primitives/cts-button",
  component: "cts-button",
  argTypes: {
    variant: {
      control: "select",
      options: ["primary", "secondary", "ghost", "danger"],
    },
    size: {
      control: "select",
      options: ["xxs", "xs", "sm", "md", "lg"],
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
  args: { variant: "secondary", label: "Cancel" },
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
    expect(btn.classList.contains("oidf-btn")).toBe(true);
    expect(btn.classList.contains("oidf-btn-sm")).toBe(true);
    expect(btn.classList.contains("oidf-btn-secondary")).toBe(true);
    expect(btn.disabled).toBe(false);
    expect(btn.textContent.trim()).toBe("Cancel");
    // Ensure the legacy Bootstrap btn classes are no longer rendered.
    expect(btn.classList.contains("btn")).toBe(false);
    expect(btn.classList.contains("btn-light")).toBe(false);
  },
};

export const SecondaryWithIcon = {
  args: { variant: "secondary", label: "Create Test Plan", icon: "settings" },
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
    expect(btn.classList.contains("oidf-btn")).toBe(true);
    expect(btn.classList.contains("oidf-btn-secondary")).toBe(true);

    const iconEl = btn.querySelector("cts-icon");
    expect(iconEl).toBeTruthy();
    expect(iconEl.getAttribute("name")).toBe("settings");
    expect(iconEl.getAttribute("aria-hidden")).toBe("true");
  },
};

export const Primary = {
  args: { variant: "primary", label: "Search", icon: "search-magnifying-glass" },
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
    expect(btn.classList.contains("oidf-btn")).toBe(true);
    expect(btn.classList.contains("oidf-btn-primary")).toBe(true);

    const iconEl = btn.querySelector("cts-icon");
    expect(iconEl).toBeTruthy();
    expect(iconEl.getAttribute("name")).toBe("search-magnifying-glass");
  },
};

export const Ghost = {
  args: { variant: "ghost", label: "View log" },
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
    expect(btn.classList.contains("oidf-btn")).toBe(true);
    expect(btn.classList.contains("oidf-btn-ghost")).toBe(true);
  },
};

export const Danger = {
  args: { variant: "danger", label: "Delete", icon: "trash-empty" },
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
    expect(btn.classList.contains("oidf-btn")).toBe(true);
    expect(btn.classList.contains("oidf-btn-danger")).toBe(true);

    const iconEl = btn.querySelector("cts-icon");
    expect(iconEl).toBeTruthy();
    expect(iconEl.getAttribute("name")).toBe("trash-empty");
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

    // Loading indicator is now an inline SVG spinner — no more Bootstrap
    // spinner-border markup.
    const spinner = btn.querySelector("svg.oidf-btn-spinner");
    expect(spinner).toBeTruthy();
    expect(spinner.getAttribute("role")).toBe("status");
    expect(spinner.getAttribute("aria-hidden")).toBe("true");
    expect(btn.querySelector("span.spinner-border")).toBeNull();

    // No glyph icon renders while the spinner is showing.
    expect(btn.querySelector("cts-icon")).toBeNull();
  },
};

export const Disabled = {
  args: { variant: "secondary", label: "Cancel", disabled: true },
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

/**
 * Documents the `host.click()` vs inner `<button>.click()` divergence for
 * cts-button. Programmatic activation via the host is a no-op because Lit
 * registers `@click` on the inner button, which does not see synthetic clicks
 * on its parent. This story is the regression canary: if it ever starts
 * failing, either shadow DOM was enabled or `preventDefault` was introduced
 * in `_handleClick`, both of which silently break ClipboardJS, Bootstrap
 * `data-bs-*`, and jQuery-delegated handlers on cts-button hosts.
 */
export const HostClickDoesNotDispatch = {
  args: { variant: "primary", label: "Programmatic" },
  render: ({ variant, label }) =>
    html`<cts-button variant="${variant}" label="${label}"></cts-button>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-button");
    await host.updateComplete;
    const btn = host.querySelector("button");
    expect(btn).toBeTruthy();

    // Count cts-click events. Lit dispatches them from the host, so
    // `e.target === host` in both cases — we just count invocations.
    let ctsClickCount = 0;
    canvasElement.addEventListener("cts-click", () => {
      ctsClickCount += 1;
    });

    // Synthetic click on the host bypasses Lit's @click on the inner button.
    host.click();
    expect(ctsClickCount).toBe(0);

    // Clicking the inner button (what a real user hits) dispatches cts-click.
    btn.click();
    expect(ctsClickCount).toBe(1);
  },
};

export const DisabledNoEvent = {
  args: { variant: "secondary", label: "Cancel", disabled: true },
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
    variant: "primary",
    icon: "settings",
    label: "View configuration",
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
    // Block-level layout is driven by the `[full-width]` CSS rule injected
    // by the component, so we assert on the computed style rather than the
    // imperative inline `style.display` set by the previous Wave-1 build.
    expect(getComputedStyle(host).display).toBe("block");
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
        <cts-button
          variant="secondary"
          icon="settings"
          label="View configuration"
          full-width
        ></cts-button>
        <cts-button
          variant="secondary"
          icon="save"
          label="Download all Logs"
          full-width
        ></cts-button>
        <cts-button
          variant="secondary"
          icon="bookmark"
          label="Publish everything"
          full-width
        ></cts-button>
        <cts-button
          variant="secondary"
          icon="bookmark"
          label="Private link"
          full-width
        ></cts-button>
        <cts-button variant="danger" icon="trash-empty" label="Delete plan" full-width></cts-button>
      </div>
    </div>
  `,
};

export const AllVariants = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem;">
      <cts-button variant="primary" label="Run plan" icon="play"></cts-button>
      <cts-button variant="secondary" label="Cancel"></cts-button>
      <cts-button variant="ghost" label="View log"></cts-button>
      <cts-button variant="danger" label="Delete" icon="trash-empty"></cts-button>
    </div>
  `,
};

/**
 * The legacy Bootstrap-era variant names (`light`, `info`, `success`,
 * `warning`, `dark`, `outline-*`) remain accepted as aliases that map to
 * the closest new OIDF variant. This story keeps regressions visible if
 * the alias table in cts-button.js drifts.
 */
export const LegacyVariantAliases = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem;">
      <cts-button variant="light" label="light → secondary"></cts-button>
      <cts-button variant="info" label="info → primary"></cts-button>
      <cts-button variant="success" label="success → secondary"></cts-button>
      <cts-button variant="warning" label="warning → secondary"></cts-button>
      <cts-button variant="dark" label="dark → secondary"></cts-button>
      <cts-button variant="outline-primary" label="outline-primary → primary"></cts-button>
    </div>
  `,

  async play({ canvasElement }) {
    const buttons = canvasElement.querySelectorAll("cts-button button");
    expect(buttons.length).toBe(6);
    // light → secondary
    expect(buttons[0].classList.contains("oidf-btn-secondary")).toBe(true);
    // info → primary
    expect(buttons[1].classList.contains("oidf-btn-primary")).toBe(true);
    // success → secondary
    expect(buttons[2].classList.contains("oidf-btn-secondary")).toBe(true);
    // warning → secondary
    expect(buttons[3].classList.contains("oidf-btn-secondary")).toBe(true);
    // dark → secondary
    expect(buttons[4].classList.contains("oidf-btn-secondary")).toBe(true);
    // outline-primary → primary
    expect(buttons[5].classList.contains("oidf-btn-primary")).toBe(true);
  },
};

/**
 * Five sizes drive 20/24/30/36/44px button heights via the `oidf-btn-xxs` /
 * `oidf-btn-xs` / `oidf-btn-sm` / `oidf-btn-lg` modifiers (default `md`
 * carries no modifier — height comes from the base `.oidf-btn` rule).
 * `xxs` is a chip-scale used inline next to badges (e.g. the cURL copy
 * affordance in cts-log-entry); `xs` is intended for dense surfaces like
 * log entry "More" toggles where `sm` reads too tall.
 */
export const Sizes = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem; align-items: center;">
      <cts-button variant="primary" label="Chip" size="xxs"></cts-button>
      <cts-button variant="primary" label="Extra small" size="xs"></cts-button>
      <cts-button variant="primary" label="Small (default)" size="sm"></cts-button>
      <cts-button variant="primary" label="Medium" size="md"></cts-button>
      <cts-button variant="primary" label="Large" size="lg"></cts-button>
    </div>
  `,

  async play({ canvasElement }) {
    const buttons = canvasElement.querySelectorAll("cts-button button");
    expect(buttons.length).toBe(5);
    expect(buttons[0].classList.contains("oidf-btn-xxs")).toBe(true);
    expect(buttons[1].classList.contains("oidf-btn-xs")).toBe(true);
    expect(buttons[2].classList.contains("oidf-btn-sm")).toBe(true);
    expect(buttons[3].classList.contains("oidf-btn-sm")).toBe(false);
    expect(buttons[3].classList.contains("oidf-btn-lg")).toBe(false);
    expect(buttons[4].classList.contains("oidf-btn-lg")).toBe(true);
  },
};

/**
 * Unknown `size` values fall back to `sm` — same defensive behaviour as the
 * `variant` attribute. Prevents regressions if a typo or stale value reaches
 * the component.
 */
export const SizeFallback = {
  render: () => html` <cts-button variant="primary" label="Bogus size" size="huge"></cts-button> `,

  async play({ canvasElement }) {
    const btn = canvasElement.querySelector("cts-button button");
    expect(btn.classList.contains("oidf-btn-sm")).toBe(true);
    expect(btn.classList.contains("oidf-btn-lg")).toBe(false);
  },
};

/**
 * Demonstrates the correct integration pattern for updating a cts-button's
 * variant at runtime: set the `.variant` property on the host element and
 * await `updateComplete`. Class manipulation via `classList.add/remove` on
 * the host does NOT work — Lit renders the inner `<button>`'s classes, not
 * the host's.
 *
 * This pattern applies to all reactive properties: `.disabled`, `.loading`,
 * `.label`, `.icon`, `.size`, `.fullWidth`.
 */
export const VariantPropertySetter = {
  render: () => html` <cts-button variant="secondary" label="Change me"></cts-button> `,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-button");
    await host.updateComplete;

    // Verify initial state — inner button has oidf-btn-secondary
    const btn = host.querySelector("button");
    expect(btn.classList.contains("oidf-btn-secondary")).toBe(true);
    expect(btn.classList.contains("oidf-btn-primary")).toBe(false);

    // Use the property setter (the correct integration pattern)
    host.variant = "primary";
    await host.updateComplete;

    // Inner button now reflects the new variant
    expect(btn.classList.contains("oidf-btn-primary")).toBe(true);
    expect(btn.classList.contains("oidf-btn-secondary")).toBe(false);
  },
};
