import { html } from "lit";
import { expect, within } from "storybook/test";
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
