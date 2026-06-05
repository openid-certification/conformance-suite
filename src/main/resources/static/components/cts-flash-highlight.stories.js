import { html } from "lit";
import { expect, waitFor } from "storybook/test";
import "./cts-flash-highlight.js";

export default {
  title: "Primitives/cts-flash-highlight",
  component: "cts-flash-highlight",
};

// Sample content for the wash to overhang. Two stacked blocks stand in for the
// real consumers (the spec cascade + the variant selectors form) so the
// highlight's "cover the whole group" behavior is visible.
const SAMPLE = html`
  <div style="display:grid;gap:var(--space-3)">
    <div
      style="padding:var(--space-4);border:1px solid var(--ink-300);border-radius:var(--radius-2);background:var(--bg-elev)"
    >
      First wrapped block (e.g. spec cascade)
    </div>
    <div
      style="padding:var(--space-4);border:1px solid var(--ink-300);border-radius:var(--radius-2);background:var(--bg-elev)"
    >
      Second wrapped block (e.g. variant selectors)
    </div>
  </div>
`;

// Run `play` once the host element has upgraded and exposes flashHighlight().
async function whenUpgraded(canvasElement) {
  await customElements.whenDefined("cts-flash-highlight");
  const element = /** @type {any} */ (canvasElement.querySelector("cts-flash-highlight"));
  await waitFor(() => {
    expect(typeof element.flashHighlight).toBe("function");
  });
  return element;
}

// --- Stories ---

/**
 * The resting state: no wash. `flashHighlight()` must be called explicitly to
 * paint the highlight, so on its own the wrapper is invisible — it only adds a
 * `position: relative` box around its children.
 */
export const Default = {
  render: () => html`<cts-flash-highlight>${SAMPLE}</cts-flash-highlight>`,
  async play({ canvasElement }) {
    const element = await whenUpgraded(canvasElement);
    expect(element.hasAttribute("data-flashing")).toBe(false);
  },
};

/**
 * `flashHighlight()` paints the Basecamp-style scroll-in wash over the wrapped
 * content. With motion allowed, the `data-flashing` attribute is set, the
 * decorative `::after` carries `pointer-events:none` / `z-index:-1` so the
 * wrapped controls stay legible and clickable, and cleanup runs on
 * `animationend`.
 */
export const ScrollInHighlightFlash = {
  render: () => html`<cts-flash-highlight>${SAMPLE}</cts-flash-highlight>`,
  async play({ canvasElement, step }) {
    const element = await whenUpgraded(canvasElement);

    // Force the motion-allowed branch so the animated path (animationend
    // cleanup) is exercised regardless of the runner's OS preference.
    const origMatchMedia = window.matchMedia;
    window.matchMedia = /** @type {any} */ (
      (q) => ({
        matches: false,
        media: q,
        addEventListener() {},
        removeEventListener() {},
      })
    );
    try {
      await step("starts at rest with no wash", async () => {
        expect(element.hasAttribute("data-flashing")).toBe(false);
      });

      await step("flashHighlight paints the wash", async () => {
        element.flashHighlight();

        // The attribute lands after the restart-from-zero reflow.
        await waitFor(() => {
          expect(element.hasAttribute("data-flashing")).toBe(true);
        });
      });

      await step("wash is non-interactive and sits behind the wrapped controls", async () => {
        const after = getComputedStyle(element, "::after");
        expect(after.pointerEvents).toBe("none");
        expect(after.zIndex).toBe("-1");
      });

      await step("animation completion clears the attribute", async () => {
        element.dispatchEvent(
          new AnimationEvent("animationend", {
            animationName: "oidf-flash-highlight-flash",
            bubbles: true,
          }),
        );
        await waitFor(() => {
          expect(element.hasAttribute("data-flashing")).toBe(false);
        });
      });
    } finally {
      window.matchMedia = origMatchMedia;
    }
  },
};

/**
 * Regression guard for the stranded-wash bug: under `prefers-reduced-motion`
 * the animation is suppressed, so `animationend` never fires. Cleanup must NOT
 * depend on it — `flashHighlight()` schedules a timer so the static wash still
 * clears on its own.
 */
export const ReducedMotionHighlightStillClears = {
  render: () => html`<cts-flash-highlight>${SAMPLE}</cts-flash-highlight>`,
  async play({ canvasElement, step }) {
    const element = await whenUpgraded(canvasElement);

    const origMatchMedia = window.matchMedia;
    window.matchMedia = /** @type {any} */ (
      (q) => ({
        matches: q.includes("reduce"),
        media: q,
        addEventListener() {},
        removeEventListener() {},
      })
    );
    try {
      await step("static wash appears (no animation in this branch)", async () => {
        element.flashHighlight();
        await waitFor(() => {
          expect(element.hasAttribute("data-flashing")).toBe(true);
        });
      });

      await step("wash clears via the timer, without any animationend dispatch", async () => {
        await waitFor(
          () => {
            expect(element.hasAttribute("data-flashing")).toBe(false);
          },
          { timeout: 2500 },
        );
      });
    } finally {
      window.matchMedia = origMatchMedia;
    }
  },
};

/**
 * Re-triggering while a flash is still in flight restarts it from zero (a fresh
 * re-flash), rather than no-opping on the second call.
 */
export const HighlightRestartsOnReselect = {
  render: () => html`<cts-flash-highlight>${SAMPLE}</cts-flash-highlight>`,
  async play({ canvasElement, step }) {
    const element = await whenUpgraded(canvasElement);

    const origMatchMedia = window.matchMedia;
    window.matchMedia = /** @type {any} */ (
      (q) => ({
        matches: false,
        media: q,
        addEventListener() {},
        removeEventListener() {},
      })
    );
    try {
      await step("first flash paints the wash", async () => {
        element.flashHighlight();
        await waitFor(() => {
          expect(element.hasAttribute("data-flashing")).toBe(true);
        });
      });

      await step("re-triggering while active restarts the flash", async () => {
        // Second call while active must leave the highlight present (restarted),
        // not stuck off from a false→true toggle.
        element.flashHighlight();
        await waitFor(() => {
          expect(element.hasAttribute("data-flashing")).toBe(true);
        });
      });

      await step("cleanup so the timer doesn't outlive the story", async () => {
        element.dispatchEvent(
          new AnimationEvent("animationend", {
            animationName: "oidf-flash-highlight-flash",
            bubbles: true,
          }),
        );
        await waitFor(() => {
          expect(element.hasAttribute("data-flashing")).toBe(false);
        });
      });
    } finally {
      window.matchMedia = origMatchMedia;
    }
  },
};
