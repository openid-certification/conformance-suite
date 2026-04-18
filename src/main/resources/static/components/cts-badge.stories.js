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

export const InfoSubtle = {
  args: { variant: "info-subtle", label: "Section description", pill: true },
  render: ({ variant, label, pill }) =>
    html`<cts-badge variant="${variant}" label="${label}" ?pill="${pill}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("bg-info-subtle")).toBe(true);
    expect(badge.classList.contains("border")).toBe(true);
    expect(badge.classList.contains("border-info-subtle")).toBe(true);
    expect(badge.classList.contains("text-info-emphasis")).toBe(true);
    expect(badge.classList.contains("rounded-pill")).toBe(true);
    expect(badge.textContent.trim()).toBe("Section description");
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
    expect(badge.classList.contains("bg-info-subtle")).toBe(true);

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
    expect(badge.classList.contains("rounded-pill")).toBe(true);
    expect(badge.classList.contains("bg-secondary")).toBe(true);
  },
};

export const Clickable = {
  args: { variant: "info", label: "Click me", clickable: true },
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
          html`<cts-badge variant="${variant}" label="${variant.toUpperCase()}"></cts-badge>`,
      )}
    </div>
  `,
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
    expect(badge.classList.contains("bg-info-subtle")).toBe(true);

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
 * Regression guard for the slot-children capture in `_render()`. cts-badge is
 * a vanilla HTMLElement with `observedAttributes`, so changing an attribute
 * like `variant` or `pill` triggers a full re-render. The rich slotted
 * content must survive re-render: children are captured once and moved
 * between wrappers on each render, so the `<em>` and `<a>` references stay
 * live across attribute changes.
 */
export const RichContentRerenderStability = {
  render: () => html`
    <cts-badge variant="info-subtle" pill icon="info-circle-fill">
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
    host.setAttribute("variant", "danger");
    host.removeAttribute("pill");
    host.setAttribute("icon", "exclamation-triangle-fill");

    // Wrapper reflects new state.
    const badge = host.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.classList.contains("bg-danger")).toBe(true);
    expect(badge.classList.contains("rounded-pill")).toBe(false);
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
  args: { variant: "success", count: 42, label: "Ignored" },
  render: ({ variant, count, label }) =>
    html`<cts-badge variant="${variant}" count="${count}" label="${label}"></cts-badge>`,

  async play({ canvasElement }) {
    const badge = canvasElement.querySelector(".badge");
    expect(badge).toBeTruthy();
    expect(badge.textContent.trim()).toBe("42");
  },
};
