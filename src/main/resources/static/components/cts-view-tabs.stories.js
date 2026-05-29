import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import "./cts-view-tabs.js";

export default {
  title: "Primitives/cts-view-tabs",
  component: "cts-view-tabs",
  // The component reads/writes location.search via pushState, so without a
  // reset the URL state leaks across stories (learning:
  // feedback_storybook_story_url_pollution). Reset to a clean path before each
  // story so each one derives its active view from a known URL.
  beforeEach() {
    history.replaceState(null, "", "/iframe.html");
  },
};

// --- Stories ---

// Authenticated, no public param: both anchors render and My is active.
export const Authenticated = {
  render: () => html`<cts-view-tabs authenticated></cts-view-tabs>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    const my = canvasElement.querySelector("a[data-view='my']");
    const published = canvasElement.querySelector("a[data-view='published']");
    expect(my).toBeTruthy();
    expect(published).toBeTruthy();

    // My is active by default (no ?public=true in the URL). Inactive anchors
    // carry aria-current="false" (a valid token), not the active "page".
    expect(my.getAttribute("aria-current")).toBe("page");
    expect(published.getAttribute("aria-current")).toBe("false");

    // It is a nav landmark, uniquely labelled (WCAG 2.4.1) — NOT a tablist.
    const nav = canvas.getByRole("navigation", { name: "Dataset view" });
    expect(nav).toBeTruthy();
    expect(canvasElement.querySelector("[role='tablist']")).toBeNull();
    expect(canvasElement.querySelector("[role='tab']")).toBeNull();
  },
};

// Anonymous: only Published renders (the My anchor is suppressed), and
// Published is active because anon visitors are always on Published.
export const Anonymous = {
  render: () => html`<cts-view-tabs></cts-view-tabs>`,

  async play({ canvasElement }) {
    const my = canvasElement.querySelector("a[data-view='my']");
    const published = canvasElement.querySelector("a[data-view='published']");

    // No My anchor for anonymous visitors.
    expect(my).toBeNull();
    expect(published).toBeTruthy();
    expect(published.getAttribute("aria-current")).toBe("page");
  },
};

// Published active for an authenticated user when ?public=true is in the URL.
export const PublishedActive = {
  beforeEach() {
    history.replaceState(null, "", "/iframe.html?public=true");
  },
  render: () => html`<cts-view-tabs authenticated></cts-view-tabs>`,

  async play({ canvasElement }) {
    const my = canvasElement.querySelector("a[data-view='my']");
    const published = canvasElement.querySelector("a[data-view='published']");

    // Both anchors render (authed), but Published carries aria-current because
    // ?public=true is present.
    expect(my).toBeTruthy();
    expect(published).toBeTruthy();
    expect(published.getAttribute("aria-current")).toBe("page");
    expect(my.getAttribute("aria-current")).toBe("false");
  },
};

// Opt-in "Schedule test" CTA (R11): when create-test-href is set, the CTA
// renders at the END of the tabs row as a real anchor to the given destination
// — in every state (here: authenticated, My view).
export const ScheduleTestCta = {
  render: () =>
    html`<cts-view-tabs authenticated create-test-href="schedule-test.html"></cts-view-tabs>`,

  async play({ canvasElement }) {
    const cta = canvasElement.querySelector('[data-testid="schedule-test-cta"]');
    expect(cta).toBeTruthy();

    // It is the last child of the tabs nav — i.e. at the end, after Published.
    const nav = canvasElement.querySelector("nav.cts-view-tabs");
    expect(nav.lastElementChild).toBe(cta);

    // Real anchor to the opt-in destination (cts-link-button renders its <a>
    // on its own update tick).
    const link = /** @type {HTMLAnchorElement} */ (
      await waitFor(() => {
        const a = cta.querySelector("a");
        expect(a).toBeTruthy();
        return a;
      })
    );
    expect(link.getAttribute("href")).toBe("schedule-test.html");
    expect(link.textContent?.trim()).toContain("Schedule test");

    // The CTA keeps its oidf-btn styling — the tab-anchor rule must NOT leak
    // onto it. The tab rule sets a 2px bottom border; oidf-btn uses 1px. A
    // regression to a bare ".cts-view-tabs a" selector would make this 2px.
    expect(link.classList.contains("oidf-btn")).toBe(true);
    expect(getComputedStyle(link).borderBottomWidth).toBe("1px");
  },
};

// The CTA is a persistent entry point: an authenticated user on the Published
// view (?public=true) still sees it at the end of the row.
export const ScheduleTestCtaOnPublished = {
  beforeEach() {
    history.replaceState(null, "", "/iframe.html?public=true");
  },
  render: () =>
    html`<cts-view-tabs authenticated create-test-href="schedule-test.html"></cts-view-tabs>`,

  async play({ canvasElement }) {
    const cta = canvasElement.querySelector('[data-testid="schedule-test-cta"]');
    expect(cta).toBeTruthy();
    // Still the last child, after the (active) Published anchor.
    const nav = canvasElement.querySelector("nav.cts-view-tabs");
    expect(nav.lastElementChild).toBe(cta);
  },
};

// Anonymous visitors also see the CTA (a click lands on the server-auth-gated
// schedule page). The My anchor is absent, but the CTA still renders.
export const ScheduleTestCtaForAnon = {
  render: () => html`<cts-view-tabs create-test-href="schedule-test.html"></cts-view-tabs>`,

  async play({ canvasElement }) {
    expect(canvasElement.querySelector("a[data-view='my']")).toBeNull();
    const cta = canvasElement.querySelector('[data-testid="schedule-test-cta"]');
    expect(cta).toBeTruthy();
    const nav = canvasElement.querySelector("nav.cts-view-tabs");
    expect(nav.lastElementChild).toBe(cta);
  },
};

// Without the opt-in (the logs.html usage), no CTA renders — the shared control
// stays page-neutral.
export const NoScheduleTestWithoutOptIn = {
  render: () => html`<cts-view-tabs authenticated></cts-view-tabs>`,

  async play({ canvasElement }) {
    expect(canvasElement.querySelector("a[data-view='my']")).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="schedule-test-cta"]')).toBeNull();
  },
};

// Opt-in Published help affordance (R22): when published-help is set, a
// circled-question-mark icon renders immediately after the Published anchor,
// wrapped in a cts-tooltip carrying the descriptor. The icon is the focusable
// trigger (tabindex=0) and carries the full descriptor as its accessible name.
export const PublishedHelpTooltip = {
  render: () =>
    html`<cts-view-tabs
      authenticated
      published-help="Published test plans are conformance test configurations that implementers have chosen to make public."
    ></cts-view-tabs>`,

  async play({ canvasElement }) {
    const help = canvasElement.querySelector('[data-testid="published-help"]');
    expect(help).toBeTruthy();

    // The cts-icon itself is the focusable, screen-reader-labelled trigger.
    expect(help.getAttribute("name")).toBe("circle-help");
    expect(help.getAttribute("tabindex")).toBe("0");
    expect(help.getAttribute("aria-label")).toContain("Published test plans");

    // It sits immediately after the Published anchor, inside a cts-tooltip whose
    // content carries the same descriptor.
    const published = canvasElement.querySelector("a[data-view='published']");
    const tooltip = help.closest("cts-tooltip");
    expect(tooltip).toBeTruthy();
    expect(published.nextElementSibling).toBe(tooltip);
    expect(tooltip.getAttribute("content")).toContain("Published test plans");
  },
};

// The help affordance coexists with the Schedule-test CTA: help sits after
// Published, the CTA stays last in the row.
export const PublishedHelpWithCta = {
  render: () =>
    html`<cts-view-tabs
      authenticated
      create-test-href="schedule-test.html"
      published-help="Published test plans are conformance test configurations that implementers have chosen to make public."
    ></cts-view-tabs>`,

  async play({ canvasElement }) {
    const help = canvasElement.querySelector('[data-testid="published-help"]');
    const cta = canvasElement.querySelector('[data-testid="schedule-test-cta"]');
    expect(help).toBeTruthy();
    expect(cta).toBeTruthy();

    // Help comes before the CTA; the CTA remains the last element in the row.
    const nav = canvasElement.querySelector("nav.cts-view-tabs");
    expect(nav.lastElementChild).toBe(cta);
  },
};
