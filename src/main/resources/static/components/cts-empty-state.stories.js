import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-icon.js";
import "./cts-link-button.js";
import "./cts-empty-state.js";

export default {
  title: "Patterns/cts-empty-state",
  component: "cts-empty-state",
  argTypes: {
    heading: { control: "text" },
    body: { control: "text" },
    icon: { control: "text" },
    ctaLabel: { control: "text" },
    ctaHref: { control: "text" },
  },
};

// --- Stories ---

/**
 * Happy path: icon + heading + body + built-in CTA all render.
 */
export const Default = {
  args: {
    heading: "No test plans yet",
    body: "Create your first plan to get started running conformance tests.",
    icon: "inbox",
    ctaLabel: "Create a plan",
    ctaHref: "schedule-test.html",
  },
  render: ({ heading, body, icon, ctaLabel, ctaHref }) =>
    html`<cts-empty-state
      heading=${heading}
      body=${body}
      icon=${icon}
      cta-label=${ctaLabel}
      cta-href=${ctaHref}
    ></cts-empty-state>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-empty-state");
    await host.updateComplete;

    const wrapper = host.querySelector(".oidf-empty-state");
    expect(wrapper).toBeTruthy();

    // Icon glyph rendered above the heading.
    const iconHost = host.querySelector("cts-icon");
    expect(iconHost).toBeTruthy();
    expect(iconHost.getAttribute("name")).toBe("inbox");
    expect(iconHost.getAttribute("size")).toBe("24");

    // Heading text matches.
    const heading = host.querySelector(".oidf-empty-state-heading");
    expect(heading).toBeTruthy();
    expect(heading.tagName).toBe("H2");
    expect(heading.textContent.trim()).toBe("No test plans yet");

    // Body paragraph rendered with the supplied copy.
    const body = host.querySelector(".oidf-empty-state-body");
    expect(body).toBeTruthy();
    expect(body.textContent.trim()).toBe(
      "Create your first plan to get started running conformance tests.",
    );

    // Built-in CTA renders a primary cts-link-button (its inner anchor
    // carries the OIDF button classes).
    const ctaHost = host.querySelector("cts-link-button");
    expect(ctaHost).toBeTruthy();
    await ctaHost.updateComplete;
    const ctaAnchor = ctaHost.querySelector("a");
    expect(ctaAnchor).toBeTruthy();
    expect(ctaAnchor.getAttribute("href")).toBe("schedule-test.html");
    expect(ctaAnchor.textContent.trim()).toBe("Create a plan");
    expect(ctaAnchor.classList.contains("oidf-btn-primary")).toBe(true);
    expect(ctaAnchor.classList.contains("oidf-btn-sm")).toBe(true);
  },
};

/**
 * Edge case: no `icon` and no `cta-label` / `cta-href`. The component
 * should drop the icon glyph cleanly and render no CTA (and no slot
 * fallback content because the slot is empty).
 */
export const HeadingAndBodyOnly = {
  args: {
    heading: "Nothing to show here",
    body: "Once data arrives, it will appear in this panel.",
  },
  render: ({ heading, body }) =>
    html`<cts-empty-state heading=${heading} body=${body}></cts-empty-state>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-empty-state");
    await host.updateComplete;

    // Heading still renders.
    const heading = host.querySelector(".oidf-empty-state-heading");
    expect(heading).toBeTruthy();
    expect(heading.textContent.trim()).toBe("Nothing to show here");

    // Body still renders.
    const body = host.querySelector(".oidf-empty-state-body");
    expect(body).toBeTruthy();

    // Icon and CTA are omitted cleanly.
    expect(host.querySelector("cts-icon")).toBeNull();
    expect(host.querySelector("cts-link-button")).toBeNull();
  },
};

/**
 * Edge case: `heading` only. No body paragraph or CTA either; verifies
 * that `body` omits cleanly the same way `icon` and the CTA do.
 */
export const HeadingOnly = {
  args: { heading: "All clear" },
  render: ({ heading }) => html`<cts-empty-state heading=${heading}></cts-empty-state>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-empty-state");
    await host.updateComplete;

    expect(host.querySelector(".oidf-empty-state-heading").textContent.trim()).toBe("All clear");
    expect(host.querySelector(".oidf-empty-state-body")).toBeNull();
    expect(host.querySelector("cts-icon")).toBeNull();
    expect(host.querySelector("cts-link-button")).toBeNull();
  },
};

/**
 * Slot escape hatch: when `cta-label` / `cta-href` aren't enough (for
 * example, the consumer needs a `<cts-button>` wired to a click handler,
 * or wants to stack two buttons), they can pass arbitrary CTA content
 * via the default slot. The built-in `<cts-link-button>` is NOT rendered
 * in this mode.
 */
export const SlottedCta = {
  render: () => html`
    <cts-empty-state
      heading="No tokens issued yet"
      body="Generate an API token to integrate with the conformance suite."
      icon="key"
    >
      <button type="button" id="slotted-cta" class="oidf-btn oidf-btn-sm oidf-btn-primary">
        Generate token
      </button>
    </cts-empty-state>
  `,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-empty-state");
    await host.updateComplete;

    // Built-in CTA is NOT rendered (no cta-label / cta-href).
    expect(host.querySelector("cts-link-button")).toBeNull();

    // Slotted button is captured into the CTA slot wrapper. Light DOM +
    // <slot> means the slotted child remains the same node — the slot
    // simply renders it in place.
    const slotted = host.querySelector("#slotted-cta");
    expect(slotted).toBeTruthy();
    expect(slotted.textContent.trim()).toBe("Generate token");
  },
};

/**
 * Demonstrates the "block-level fill" intent (Coherence F4) by placing
 * the empty state inside a sized container — the same shape it takes
 * when used as a dashboard tile or a table-empty row.
 */
export const FillsContainer = {
  render: () => html`
    <div
      style="width: 480px; min-height: 240px; border: 1px dashed var(--ink-300); border-radius: var(--radius-3);"
    >
      <cts-empty-state
        heading="No log entries"
        body="Run a test to populate the log viewer."
        icon="journal-text"
        cta-label="Run a test"
        cta-href="schedule-test.html"
      ></cts-empty-state>
    </div>
  `,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-empty-state");
    await host.updateComplete;
    // The component remains centered inside the container; we just verify
    // that the wrapper renders and the icon glyph is present.
    expect(host.querySelector(".oidf-empty-state")).toBeTruthy();
    expect(host.querySelector("cts-icon")).toBeTruthy();
  },
};
