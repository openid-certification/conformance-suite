import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-action-bar.js";
import "./cts-button.js";

export default {
  title: "Primitives/cts-action-bar",
  component: "cts-action-bar",
  argTypes: {
    position: {
      control: "select",
      options: ["bottom", "static"],
    },
    "align-to": {
      control: "select",
      options: ["schedule-test-page", "full-bleed"],
    },
  },
};

// --- Stories ---

/**
 * Static positioning is the dev-preview mode: the bar renders at its source
 * position so a story can lay it out next to other content without taking
 * over the viewport.
 */
export const Static = {
  args: { position: "static", "align-to": "schedule-test-page" },
  render: ({ position, "align-to": alignTo }) => html`
    <cts-action-bar position="${position}" align-to="${alignTo}" aria-label="Test plan actions">
      <cts-button variant="primary" size="lg" icon="flag" label="Create Test Plan"></cts-button>
      <cts-button
        variant="secondary"
        size="lg"
        icon="copy"
        label="Share Test Plan Configuration"
      ></cts-button>
    </cts-action-bar>
  `,

  async play({ canvasElement }) {
    const bar = canvasElement.querySelector(".oidf-action-bar");
    expect(bar).toBeTruthy();
    expect(bar.getAttribute("data-position")).toBe("static");
    expect(bar.getAttribute("role")).toBe("region");
    expect(bar.getAttribute("aria-label")).toBe("Test plan actions");

    const inner = bar.querySelector(".oidf-action-bar__inner");
    expect(inner).toBeTruthy();
    // schedule-test-page preset routes max-width through --maxw-page so the
    // inner wrapper tracks the page column token.
    expect(inner.style.maxWidth).toBe("var(--maxw-page)");

    // Children are moved into the inner wrapper.
    const buttons = inner.querySelectorAll("cts-button");
    expect(buttons.length).toBe(2);
    expect(buttons[0].getAttribute("label")).toBe("Create Test Plan");
    expect(buttons[1].getAttribute("label")).toBe("Share Test Plan Configuration");

    // Computed position must be static so the story canvas isn't covered.
    expect(getComputedStyle(bar).position).toBe("static");
  },
};

/**
 * Bottom-positioned bars publish their measured height as
 * `--cts-action-bar-height` on the document root. This story asserts the
 * property is set; the cleanup-on-disconnect behavior is exercised in
 * the Cleanup story below.
 */
export const Bottom = {
  args: { position: "bottom", "align-to": "schedule-test-page" },
  render: ({ position, "align-to": alignTo }) => html`
    <div style="height: 240px; padding: 16px; background: var(--bg-muted);">
      Scrollable content placeholder. The bar should pin to the bottom of the story canvas.
    </div>
    <cts-action-bar position="${position}" align-to="${alignTo}">
      <cts-button variant="primary" size="lg" label="Primary"></cts-button>
      <cts-button variant="secondary" size="lg" label="Secondary"></cts-button>
    </cts-action-bar>
  `,

  async play({ canvasElement }) {
    const bar = canvasElement.querySelector(".oidf-action-bar");
    expect(bar).toBeTruthy();
    expect(bar.getAttribute("data-position")).toBe("bottom");
    expect(getComputedStyle(bar).position).toBe("fixed");

    // Default aria-label kicks in when the host has none.
    expect(bar.getAttribute("aria-label")).toBe("Actions");

    // Allow the ResizeObserver publish callback to run.
    await new Promise((resolve) => setTimeout(resolve, 0));
    const published = document.documentElement.style.getPropertyValue("--cts-action-bar-height");
    // Must be a positive integer + "px". A leading-zero regex would silently
    // pass on "0px" — which would mean the bar was never measured.
    expect(published).toMatch(/^[1-9]\d*px$/);
  },
};

/**
 * Unknown `align-to` preset values fall back to the default and warn once
 * to the console. Matches the defensive fallback used by cts-alert,
 * cts-button, and cts-badge.
 */
export const UnknownPresetFallsBack = {
  render: () => html`
    <cts-action-bar position="static" align-to="periwinkle">
      <cts-button variant="primary" size="lg" label="Action"></cts-button>
    </cts-action-bar>
  `,

  async play({ canvasElement }) {
    const bar = canvasElement.querySelector(".oidf-action-bar");
    expect(bar).toBeTruthy();
    const inner = bar.querySelector(".oidf-action-bar__inner");
    // Falls back to the schedule-test-page default preset.
    expect(inner.style.maxWidth).toBe("var(--maxw-page)");
  },
};

/**
 * FOUC: the `:not(:defined)` fallback in css/layout.css must pin a
 * placeholder bar to the bottom of the viewport so the action bar exists
 * from first paint, not only after /components/cts-action-bar.js executes.
 * The post-upgrade bar is `position: fixed; bottom: 0`, so the fallback
 * must mirror that geometry — an in-flow height reservation (the regime
 * used by cts-navbar / cts-footer) would paint at the host's mid-page
 * source position instead. Mirrors cts-navbar's FoucFallbackReservesHeight.
 */
export const FoucFallbackPinsToViewportBottom = {
  render: () => html`
    <p style="max-width: 60ch; font-size: var(--fs-14); color: var(--fg-soft);">
      Nothing visual to inspect here: this story is a CSS regression guard. The play test
      (Interactions panel) asserts that css/layout.css pins a
      <code>cts-action-bar:not(:defined)</code> placeholder to the bottom of the viewport, so the
      bar exists from first paint — before cts-action-bar.js executes. The fallback can't render in
      Storybook because importing this story defines the element. To see it live, open
      schedule-test.html with JavaScript disabled (DevTools ⌘⇧P → "Disable JavaScript") and look at
      the bottom of the viewport.
    </p>
    <cts-action-bar position="static">
      <cts-button variant="primary" size="lg" label="Action"></cts-button>
    </cts-action-bar>
  `,

  async play() {
    const sheet = Array.from(document.styleSheets).find((s) =>
      (s.href || "").includes("/css/layout.css"),
    );
    expect(sheet).toBeTruthy();
    const cssRules = /** @type {CSSStyleRule[]} */ (
      Array.from(/** @type {CSSStyleSheet} */ (sheet).cssRules)
    );
    const fallback = cssRules.find(
      (r) => r.selectorText && r.selectorText.includes("cts-action-bar:not([position="),
    );
    expect(fallback).toBeTruthy();
    const fb = /** @type {CSSStyleRule} */ (fallback);
    // The static (in-flow) variant must stay excluded so dev previews are
    // not covered by a viewport-pinned placeholder.
    expect(fb.selectorText).toBe('cts-action-bar:not([position="static"]):not(:defined)');
    expect(fb.style.position).toBe("fixed");
    expect(fb.style.bottom).toBe("0px");
    expect(fb.style.left).toBe("0px");
    expect(fb.style.right).toBe("0px");
    expect(fb.style.display).toBe("block");
    // Matches the `var(--cts-action-bar-height, 80px)` pre-measure fallback
    // .schedule-test-page uses for its bottom-padding reservation.
    expect(fb.style.minHeight).toBe("80px");
  },
};

/**
 * The full-bleed preset stretches the inner wrapper to the viewport width
 * (with small inline padding), for pages that don't have a centered
 * content column.
 */
export const FullBleed = {
  render: () => html`
    <cts-action-bar position="static" align-to="full-bleed">
      <cts-button variant="primary" size="lg" label="Wide layout"></cts-button>
    </cts-action-bar>
  `,

  async play({ canvasElement }) {
    const inner = canvasElement.querySelector(".oidf-action-bar__inner");
    expect(inner.style.maxWidth).toBe("100%");
  },
};
