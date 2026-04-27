import { html } from "lit";
import { expect, waitFor, userEvent } from "storybook/test";
import "./cts-log-toc.js";

export default {
  title: "Components/cts-log-toc",
  component: "cts-log-toc",
};

const BLOCKS = [
  {
    blockId: "block-1",
    label: "Authorization request",
    counts: { success: 4, failure: 0, warning: 0, review: 0, info: 1, total: 5 },
  },
  {
    blockId: "block-2",
    label: "Token response",
    counts: { success: 2, failure: 1, warning: 0, review: 0, info: 0, total: 3 },
  },
  {
    blockId: "block-3",
    label: "Userinfo request",
    counts: { success: 1, failure: 0, warning: 1, review: 0, info: 0, total: 2 },
  },
  {
    blockId: "block-4",
    label: "ID token validation",
    counts: { success: 0, failure: 0, warning: 0, review: 1, info: 0, total: 1 },
  },
  {
    blockId: "block-5",
    label: "Cleanup",
    counts: { success: 3, failure: 0, warning: 0, review: 0, info: 0, total: 3 },
  },
];

const FAILURES = [
  {
    _id: "f1",
    result: "FAILURE",
    src: "ValidateIdToken",
    msg: "Signature invalid",
    blockId: "block-2",
  },
  {
    _id: "f2",
    result: "WARNING",
    src: "CheckScope",
    msg: "Extra scope present",
    blockId: "block-3",
  },
];

// Wide host so the rail's natural sticky positioning has room to breathe
// when rendered inside the storybook iframe.
const WIDE_HOST = (rail) =>
  html`<div style="display: grid; grid-template-columns: 1fr 320px; gap: 16px; min-height: 600px;">
    <div style="border: 1px dashed #c7c2b8; padding: 12px;">Entries stream placeholder</div>
    ${rail}
  </div>`;

export const Default = {
  render: () =>
    WIDE_HOST(html`<cts-log-toc id="ctsLogToc" .blocks=${BLOCKS} test-id="test-1"></cts-log-toc>`),
  async play({ canvasElement }) {
    const list = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="toc-list"]');
      if (!el) throw new Error("toc-list not yet rendered");
      return el;
    });

    const rows = list.querySelectorAll("li.ctsLogTocItem");
    expect(rows).toHaveLength(BLOCKS.length);
    expect(rows[0].textContent).toContain("Authorization request");
    expect(rows[1].textContent).toContain("Token response");
    // Block 2 surfaces a failure pill; block 1 surfaces a single ✓ pill.
    expect(rows[1].querySelector('cts-badge[label="✗1"]')).toBeTruthy();
    expect(rows[0].querySelector('cts-badge[label="✓4"]')).toBeTruthy();
    // INFO is intentionally omitted from block badges.
    expect(rows[0].querySelector('cts-badge[label="ⓘ1"]')).toBeNull();
  },
};

export const ClickDispatchesScrollEvent = {
  render: () => WIDE_HOST(html`<cts-log-toc .blocks=${BLOCKS}></cts-log-toc>`),
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="toc-list"]');
      if (!el) throw new Error("toc-list not yet rendered");
      return el;
    });

    /** @type {Array<string>} */
    const fired = [];
    canvasElement.addEventListener("cts-scroll-to-block", (evt) => {
      fired.push(/** @type {CustomEvent} */ (evt).detail.blockId);
    });

    const row = canvasElement.querySelector('[data-testid="toc-row-block-3"] button');
    await userEvent.click(row);

    expect(fired).toEqual(["block-3"]);
  },
};

export const EmptyDuringWaiting = {
  render: () => WIDE_HOST(html`<cts-log-toc .blocks=${[]} .failures=${[]}></cts-log-toc>`),
  async play({ canvasElement }) {
    const rail = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector("cts-log-toc");
        if (!el) throw new Error("rail not yet rendered");
        return el;
      })
    );
    await rail.updateComplete;
    // Empty rail self-hides via the `hidden` attribute so the page-level
    // grid (`.log-page-v2--with-toc:has(#ctsLogToc:not([hidden]))`)
    // collapses to single column and the main content reclaims the
    // 320px slot. The aside is still in the DOM (so populating
    // `.blocks` later un-hides without a re-mount) but `display: none`
    // means it takes zero layout space.
    expect(rail.hasAttribute("hidden")).toBe(true);
  },
};

export const ReappearsWhenBlocksArrive = {
  // Mirrors the streaming-poll case: the rail starts empty (so it's
  // hidden), then a polling cycle delivers a non-empty blocks array.
  // The rail must drop the hidden attribute so the page grid re-expands.
  render: () =>
    WIDE_HOST(html`<cts-log-toc id="reappearRail" .blocks=${[]} .failures=${[]}></cts-log-toc>`),
  async play({ canvasElement }) {
    const rail = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector("cts-log-toc");
        if (!el) throw new Error("rail not yet rendered");
        return el;
      })
    );
    await rail.updateComplete;
    expect(rail.hasAttribute("hidden")).toBe(true);

    rail.blocks = BLOCKS;
    await rail.updateComplete;
    expect(rail.hasAttribute("hidden")).toBe(false);
    expect(canvasElement.querySelector('[data-testid="toc-list"]')).toBeTruthy();
  },
};

export const WithFailures = {
  render: () =>
    WIDE_HOST(
      html`<cts-log-toc
        .blocks=${BLOCKS}
        .failures=${FAILURES}
        test-id="test-with-failures"
      ></cts-log-toc>`,
    ),
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector(".ctsLogTocFailures");
      if (!el) throw new Error("failure section not yet rendered");
      return el;
    });
    // The compact failure-summary inherits all of cts-failure-summary's
    // contracts (group-by-block, references, etc.). Just verify the host
    // mounted with compact=true and rendered the two failure rows.
    const summary = canvasElement.querySelector(".ctsLogTocFailures cts-failure-summary");
    expect(summary).toBeTruthy();
    expect(summary.hasAttribute("compact")).toBe(true);
    const list = summary.querySelector('[data-testid="failure-list"]');
    expect(list.querySelectorAll(".failureItem")).toHaveLength(2);
  },
};

export const ActiveBlockHighlight = {
  // Synthetically promote a block to "active" by setting the internal
  // _activeBlockId after the rail mounts. This avoids depending on
  // IntersectionObserver behaviour inside the storybook canvas.
  render: () => WIDE_HOST(html`<cts-log-toc id="activeRail" .blocks=${BLOCKS}></cts-log-toc>`),
  async play({ canvasElement }) {
    const rail = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector("cts-log-toc");
        if (!el) throw new Error("rail not yet rendered");
        return el;
      })
    );
    rail._activeBlockId = "block-3";
    rail.requestUpdate();
    await rail.updateComplete;

    const activeRow = canvasElement.querySelector('[data-testid="toc-row-block-3"]');
    expect(activeRow.classList.contains("is-active")).toBe(true);
    const button = activeRow.querySelector("button");
    expect(button.getAttribute("aria-current")).toBe("location");

    // Other rows are not active.
    const inactiveRow = canvasElement.querySelector('[data-testid="toc-row-block-1"]');
    expect(inactiveRow.classList.contains("is-active")).toBe(false);
    expect(inactiveRow.querySelector("button").getAttribute("aria-current")).toBe("false");
  },
};

export const PreferenceTogglesVisibility = {
  // Verifies the localStorage-backed preference + setEnabled() pair the
  // future U7 overflow toggle will call into. Run last so the persisted
  // pref is left in the default-on state for the next story run.
  render: () => WIDE_HOST(html`<cts-log-toc id="prefRail" .blocks=${BLOCKS}></cts-log-toc>`),
  async play({ canvasElement }) {
    const rail = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector("cts-log-toc");
        if (!el) throw new Error("rail not yet rendered");
        return el;
      })
    );
    await rail.updateComplete;
    // Visibility is now expressed via the `hidden` attribute (rather
    // than inline style.display) so the same signal also lets the
    // page-level grid collapse via `:has(#ctsLogToc:not([hidden]))`.
    expect(rail.hasAttribute("hidden")).toBe(false);

    rail.setEnabled(false);
    expect(rail.hasAttribute("hidden")).toBe(true);
    expect(localStorage.getItem("cts-log-toc-rail-enabled")).toBe("false");

    rail.setEnabled(true);
    expect(rail.hasAttribute("hidden")).toBe(false);
    expect(localStorage.getItem("cts-log-toc-rail-enabled")).toBe("true");
  },
};
