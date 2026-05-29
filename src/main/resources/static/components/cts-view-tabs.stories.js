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

// Opt-in "Create test" CTA (R11): when create-test-href is set and the active
// view is My, the CTA renders at the END of the tabs row as a real anchor to
// the given destination.
export const CreateTestCta = {
  render: () =>
    html`<cts-view-tabs authenticated create-test-href="schedule-test.html"></cts-view-tabs>`,

  async play({ canvasElement }) {
    const cta = canvasElement.querySelector('[data-testid="create-test-cta"]');
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
    expect(link.textContent?.trim()).toContain("Create test");

    // The CTA keeps its oidf-btn styling — the tab-anchor rule must NOT leak
    // onto it. The tab rule sets a 2px bottom border; oidf-btn uses 1px. A
    // regression to a bare ".cts-view-tabs a" selector would make this 2px.
    expect(link.classList.contains("oidf-btn")).toBe(true);
    expect(getComputedStyle(link).borderBottomWidth).toBe("1px");
  },
};

// The CTA is gated on the My view: an authenticated user on the Published view
// (?public=true) does not see it — consistent with the runs strip.
export const CreateTestCtaHiddenOnPublished = {
  beforeEach() {
    history.replaceState(null, "", "/iframe.html?public=true");
  },
  render: () =>
    html`<cts-view-tabs authenticated create-test-href="schedule-test.html"></cts-view-tabs>`,

  async play({ canvasElement }) {
    expect(canvasElement.querySelector('[data-testid="create-test-cta"]')).toBeNull();
    // The Published anchor is the last child when the CTA is suppressed.
    const nav = canvasElement.querySelector("nav.cts-view-tabs");
    expect(nav.lastElementChild.getAttribute("data-view")).toBe("published");
  },
};

// Anonymous visitors never see the CTA (no My view to attach it to), even when
// create-test-href is set.
export const CreateTestCtaHiddenForAnon = {
  render: () => html`<cts-view-tabs create-test-href="schedule-test.html"></cts-view-tabs>`,

  async play({ canvasElement }) {
    expect(canvasElement.querySelector('[data-testid="create-test-cta"]')).toBeNull();
  },
};

// Without the opt-in (the logs.html usage), no CTA renders even for an
// authenticated user on the My view — the shared control stays page-neutral.
export const NoCreateTestWithoutOptIn = {
  render: () => html`<cts-view-tabs authenticated></cts-view-tabs>`,

  async play({ canvasElement }) {
    expect(canvasElement.querySelector("a[data-view='my']")).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="create-test-cta"]')).toBeNull();
  },
};
