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
    icon: { control: "text" },
    pill: { control: "boolean" },
    clickable: { control: "boolean" },
  },
};

// --- Stories ---

// Canonical design-system status variants. Each maps to a `b-*` class
// painted from the `--status-*` token group in `oidf-tokens.css`.

export const Pass = {
  args: { variant: "pass", label: "Passed" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-pass")).toBe(true);
    expect(badge.textContent.trim()).toBe("Passed");
  },
};

export const Fail = {
  args: { variant: "fail", label: "Failed" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-fail")).toBe(true);
    expect(badge.textContent.trim()).toBe("Failed");
  },
};

export const Warn = {
  args: { variant: "warn", label: "Warning" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-warn")).toBe(true);
    expect(badge.textContent.trim()).toBe("Warning");
    // Bootstrap legacy class must NOT leak through.
    expect(badge.classList.contains("bg-warning")).toBe(false);
  },
};

export const Running = {
  args: { variant: "running", label: "Running" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-run")).toBe(true);

    // Spinner is the inline SVG, not a `bi bi-arrow-clockwise` icon.
    const spin = badge.querySelector(".cts-badge-spin");
    expect(spin).toBeTruthy();
    const svg = spin.querySelector("svg");
    expect(svg).toBeTruthy();
    // namespace should be SVG, not the HTML default (otherwise children
    // render as inert HTMLUnknownElements and the spinner is invisible).
    expect(svg.namespaceURI).toBe("http://www.w3.org/2000/svg");
    expect(svg.querySelector("circle")).toBeTruthy();
    expect(svg.querySelector("path")).toBeTruthy();

    // No bootstrap arrow-clockwise icon.
    expect(badge.querySelector("i.bi-arrow-clockwise")).toBeFalsy();

    // Bootstrap legacy class must NOT leak through.
    expect(badge.classList.contains("bg-info")).toBe(false);
  },
};

export const Skip = {
  args: { variant: "skip", label: "Skipped" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-skip")).toBe(true);
    expect(badge.textContent.trim()).toBe("Skipped");
  },
};

export const Review = {
  args: { variant: "review", label: "Review" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-rev")).toBe(true);
    expect(badge.textContent.trim()).toBe("Review");
  },
};

// --- Running variant: spinner replaces any icon attribute ---

/**
 * The `running` variant renders the design-system inline SVG spinner.
 * Even when an `icon` attribute is provided, the spinner takes priority
 * (the design archive specifies a single circular spinner glyph, so we
 * never render both).
 */
export const RunningIgnoresIconAttribute = {
  args: { variant: "running", label: "Running", icon: "arrow-clockwise" },
  render: ({ variant, label, icon }) =>
    html`<cts-badge variant="${variant}" label="${label}" icon="${icon}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-run")).toBe(true);

    // Spinner present.
    expect(badge.querySelector(".cts-badge-spin")).toBeTruthy();

    // No `bi bi-arrow-clockwise` icon, even though icon="arrow-clockwise"
    // was passed.
    expect(badge.querySelector("i.bi-arrow-clockwise")).toBeFalsy();
    expect(badge.querySelector("i.bi")).toBeFalsy();
  },
};

// --- Utility variants (kept for non-status uses) ---

export const BootstrapVariant = {
  args: { variant: "danger", label: "ADMIN" },
  render: ({ variant, label }) =>
    html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("bg-danger")).toBe(true);
  },
};

export const InfoSubtle = {
  args: { variant: "info-subtle", label: "Section description", pill: true },
  render: ({ variant, label, pill }) =>
    html`<cts-badge variant="${variant}" label="${label}" ?pill="${pill}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    // Retokenized scoped class on the design-system status-info palette.
    expect(badge.classList.contains("b-info-subtle")).toBe(true);
    expect(badge.textContent.trim()).toBe("Section description");
    // Bootstrap classes must NOT leak through.
    expect(badge.classList.contains("bg-info-subtle")).toBe(false);
    expect(badge.classList.contains("border-info-subtle")).toBe(false);
    expect(badge.classList.contains("text-info-emphasis")).toBe(false);
  },
};

export const WithIcon = {
  args: {
    variant: "info-subtle",
    label: "This section relates to the entity under test",
    icon: "info-circle-fill",
    pill: true,
  },
  render: ({ variant, label, icon, pill }) =>
    html`<cts-badge
      variant="${variant}"
      label="${label}"
      icon="${icon}"
      ?pill="${pill}"
    ></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-info-subtle")).toBe(true);

    const iconEl = badge.querySelector("i.bi");
    expect(iconEl).toBeTruthy();
    expect(iconEl.classList.contains("bi-info-circle-fill")).toBe(true);
    expect(iconEl.getAttribute("aria-hidden")).toBe("true");

    expect(badge.textContent.trim()).toContain("This section relates to the entity under test");
  },
};

export const WithCount = {
  args: { variant: "secondary", count: 5, pill: true },
  render: ({ variant, count, pill }) =>
    html`<cts-badge variant="${variant}" count="${count}" ?pill="${pill}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.textContent.trim()).toBe("5");
    expect(badge.classList.contains("bg-secondary")).toBe(true);
  },
};

export const Clickable = {
  args: { variant: "running", label: "Click me", clickable: true },
  render: ({ variant, label, clickable }) =>
    html`<cts-badge variant="${variant}" label="${label}" ?clickable="${clickable}"></cts-badge>`,

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
  args: { variant: "running", label: "Not clickable" },
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

export const AllStatusVariants = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem;">
      ${["pass", "fail", "warn", "running", "skip", "review"].map(
        (variant) =>
          html`<cts-badge variant="${variant}" label="${variant.toUpperCase()}"></cts-badge>`,
      )}
    </div>
  `,

  async play({ canvasElement }) {
    const badges = canvasElement.querySelectorAll("cts-badge .badge");
    expect(badges.length).toBe(6);
    const expectedClasses = ["b-pass", "b-fail", "b-warn", "b-run", "b-skip", "b-rev"];
    badges.forEach((badge, i) => {
      expect(badge.classList.contains(expectedClasses[i])).toBe(true);
    });
  },
};

/**
 * When neither `label` nor `count` is set, the badge wraps whatever child
 * nodes are inside the host element. This is the only way to embed `<a>`
 * links, `<em>` emphasis, or other rich content inside a badge — used by
 * the four federation entity headers in `schedule-test.html` to keep the
 * link to the detailed instructions clickable.
 */
export const WithRichContent = {
  render: () => html`
    <cts-badge variant="info-subtle" pill icon="info-circle-fill">
      This section relates to the entity under test, i.e. <em>your</em>
      federation entity. See also the
      <a href="https://openid.net/certification/federation_testing">detailed instructions</a>.
    </cts-badge>
  `,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-info-subtle")).toBe(true);

    // Icon still renders before the slotted content
    const iconEl = badge.querySelector("i.bi");
    expect(iconEl).toBeTruthy();
    expect(iconEl.classList.contains("bi-info-circle-fill")).toBe(true);

    // The <em> emphasis and <a> link survive the migration
    expect(badge.querySelector("em")).toBeTruthy();
    const link = badge.querySelector("a");
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("https://openid.net/certification/federation_testing");
    expect(link.textContent).toBe("detailed instructions");
  },
};

/**
 * Multi-line edge case: a badge containing an explicit `<br>` collapses
 * its corner radius to 9px (per the design archive's badge-radius
 * decision) so the wrapped content does not look squashed inside a fully-
 * rounded pill. The `:has(br)` selector in the scoped stylesheet is what
 * triggers the override.
 */
export const MultiLineWraps = {
  render: () => html`
    <cts-badge variant="info-subtle">
      This is a deliberately long badge label<br />that wraps onto a second line so we can verify
      the corner radius collapses to 9px.
    </cts-badge>
  `,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.querySelector("br")).toBeTruthy();
    // The `:has(br)` override should resolve to the 9px radius. We assert
    // the computed style rather than re-checking the rule string.
    const computed = window.getComputedStyle(badge);
    expect(computed.borderTopLeftRadius).toBe("9px");
  },
};

/**
 * Regression guard for the slot-children capture in `_render()`.
 * cts-badge is a vanilla HTMLElement with `observedAttributes`, so
 * changing an attribute like `variant` triggers a full re-render. The
 * rich slotted content must survive re-render: children are captured
 * once and moved between wrappers on each render, so the `<em>` and
 * `<a>` references stay live across attribute changes.
 */
export const RichContentRerenderStability = {
  render: () => html`
    <cts-badge variant="info-subtle" icon="info-circle-fill">
      See the <a href="/docs">documentation</a> for <em>details</em>.
    </cts-badge>
  `,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-badge");
    expect(host).toBeTruthy();

    // Capture the original slotted nodes — identity must be preserved.
    const initialLink = host.querySelector("a");
    const initialEm = host.querySelector("em");
    expect(initialLink).toBeTruthy();
    expect(initialEm).toBeTruthy();
    expect(initialLink.getAttribute("href")).toBe("/docs");

    // Mutate each observed attribute in turn.
    host.setAttribute("variant", "fail");
    host.setAttribute("icon", "exclamation-triangle-fill");

    // Wrapper reflects new state.
    const badge = host.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("b-fail")).toBe(true);
    const icon = badge.querySelector("i.bi");
    expect(icon.classList.contains("bi-exclamation-triangle-fill")).toBe(true);

    // Same <a> and <em> nodes still present — they moved, they weren't recreated.
    const rerenderedLink = host.querySelector("a");
    const rerenderedEm = host.querySelector("em");
    expect(rerenderedLink).toBe(initialLink);
    expect(rerenderedEm).toBe(initialEm);
    expect(rerenderedLink.getAttribute("href")).toBe("/docs");
    expect(rerenderedEm.textContent).toBe("details");

    // No recursive span nesting from repeated re-renders (one .badge wrapper).
    expect(host.querySelectorAll(".badge").length).toBe(1);
  },
};

export const CountPrefersOverLabel = {
  args: { variant: "pass", count: 42, label: "Ignored" },
  render: ({ variant, count, label }) =>
    html`<cts-badge variant="${variant}" count="${count}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.textContent.trim()).toBe("42");
  },
};
