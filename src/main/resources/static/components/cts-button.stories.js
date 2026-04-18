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
    size: {
      control: "select",
      options: ["sm", "md", "lg"],
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
        <cts-button
          variant="light"
          icon="wrench-adjustable"
          label="View Config"
          full-width
        ></cts-button>
        <cts-button variant="light" icon="save2" label="Download all Logs" full-width></cts-button>
        <cts-button
          variant="light"
          icon="bookmarks"
          label="Publish everything"
          full-width
        ></cts-button>
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

/**
 * The `size` attribute selects between Bootstrap's `btn-sm` (default), the
 * native size (`md` = no class), and `btn-lg`. Use `lg` for prominent CTAs
 * (login provider buttons, "Create Plan" on schedule-test, the action
 * buttons on the tokens page). Default `sm` keeps every existing usage
 * visually identical.
 */
export const Sizes = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem; align-items: center;">
      <cts-button variant="primary" label="Small (default)" size="sm"></cts-button>
      <cts-button variant="primary" label="Medium" size="md"></cts-button>
      <cts-button variant="primary" label="Large" size="lg"></cts-button>
    </div>
  `,

  async play({ canvasElement }) {
    const buttons = canvasElement.querySelectorAll("cts-button button");
    expect(buttons.length).toBe(3);
    expect(buttons[0].classList.contains("btn-sm")).toBe(true);
    expect(buttons[1].classList.contains("btn-sm")).toBe(false);
    expect(buttons[1].classList.contains("btn-lg")).toBe(false);
    expect(buttons[2].classList.contains("btn-lg")).toBe(true);
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
    expect(btn.classList.contains("btn-sm")).toBe(true);
    expect(btn.classList.contains("btn-lg")).toBe(false);
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
  render: () => html` <cts-button variant="light" label="Change me"></cts-button> `,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-button");
    await host.updateComplete;

    // Verify initial state — inner button has btn-light
    const btn = host.querySelector("button");
    expect(btn.classList.contains("btn-light")).toBe(true);
    expect(btn.classList.contains("btn-success")).toBe(false);

    // Use the property setter (the correct integration pattern)
    host.variant = "success";
    await host.updateComplete;

    // Inner button now reflects the new variant
    expect(btn.classList.contains("btn-success")).toBe(true);
    expect(btn.classList.contains("btn-light")).toBe(false);
  },
};
