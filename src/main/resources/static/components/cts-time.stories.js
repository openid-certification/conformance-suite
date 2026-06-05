import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-time.js";
import {
  formatRelative,
  formatAbsolute,
  formatTimeOfDay,
  formatCompact,
} from "../lib/time-format.js";

export default {
  title: "Components/cts-time",
  component: "cts-time",
};

// --- Helpers ---

/**
 * Returns the inner <time> element rendered by the cts-time under the
 * canvas, or null when the component rendered nothing.
 * @param {HTMLElement} canvasElement
 * @returns {HTMLTimeElement | null}
 */
function getTimeEl(canvasElement) {
  return /** @type {HTMLTimeElement | null} */ (canvasElement.querySelector("cts-time time"));
}

// A timestamp parked squarely mid-bucket (~5 min ago) so the relative label
// is stable across the few ms between render and assertion. Recomputed at
// module-eval so it stays "recent" no matter when the test runs.
const FIVE_MIN_AGO = new Date(Date.now() - (5 * 60 + 5) * 1000).toISOString();
const FIXED = "2026-01-15T13:30:45.000Z";

// --- Stories ---

export const Default = {
  render: () => html`<cts-time value=${FIVE_MIN_AGO}></cts-time>`,

  async play({ canvasElement }) {
    const t = getTimeEl(canvasElement);
    expect(t).toBeTruthy();
    // Visible text is the relative form...
    expect(t?.textContent?.trim()).toBe(formatRelative(FIVE_MIN_AGO));
    // ...and the full absolute form is always on the title for hover.
    expect(t?.getAttribute("title")).toBe(formatAbsolute(FIVE_MIN_AGO));
    // Native <time datetime> carries the machine-readable ISO value.
    expect(t?.getAttribute("datetime")).toBe(new Date(FIVE_MIN_AGO).toISOString());
  },
};

export const Absolute = {
  render: () => html`<cts-time value=${FIXED} mode="absolute"></cts-time>`,

  async play({ canvasElement }) {
    const t = getTimeEl(canvasElement);
    expect(t).toBeTruthy();
    // In absolute mode the visible text equals the title — both the full form.
    expect(t?.textContent?.trim()).toBe(formatAbsolute(FIXED));
    expect(t?.getAttribute("title")).toBe(formatAbsolute(FIXED));
  },
};

export const TimeOfDay = {
  render: () => html`<cts-time value=${FIXED} mode="time-of-day"></cts-time>`,

  async play({ canvasElement }) {
    const t = getTimeEl(canvasElement);
    expect(t).toBeTruthy();
    // Clock time only on screen, full form on hover.
    expect(t?.textContent?.trim()).toBe(formatTimeOfDay(FIXED));
    expect(t?.getAttribute("title")).toBe(formatAbsolute(FIXED));
  },
};

export const Compact = {
  render: () => html`<cts-time value=${FIXED} mode="compact"></cts-time>`,

  async play({ canvasElement }) {
    const t = getTimeEl(canvasElement);
    expect(t).toBeTruthy();
    // Compact visible form (medium date + short time), full form on hover.
    expect(t?.textContent?.trim()).toBe(formatCompact(FIXED));
    expect(t?.getAttribute("title")).toBe(formatAbsolute(FIXED));
  },
};

export const AutoAbsoluteFallback = {
  // A timestamp well past the 30-day relative crossover. In auto mode the
  // visible text falls back to the absolute locale string (the component-level
  // counterpart to formatRelative's unit-tested fallback branch).
  render: () => html`<cts-time value="2020-01-01T00:00:00.000Z" mode="auto"></cts-time>`,

  async play({ canvasElement }) {
    const t = getTimeEl(canvasElement);
    expect(t).toBeTruthy();
    expect(t?.textContent?.trim()).toBe(formatAbsolute("2020-01-01T00:00:00.000Z"));
    expect(t?.getAttribute("title")).toBe(formatAbsolute("2020-01-01T00:00:00.000Z"));
  },
};

export const EmptyValue = {
  render: () => html`<cts-time value=""></cts-time>`,

  async play({ canvasElement }) {
    // No value → component renders nothing, so there is no <time> element and
    // the cell collapses cleanly (callers decide empty-state rendering).
    expect(getTimeEl(canvasElement)).toBeNull();
  },
};

export const UnparseableValue = {
  render: () => html`<cts-time value="not-a-date"></cts-time>`,

  async play({ canvasElement }) {
    expect(getTimeEl(canvasElement)).toBeNull();
  },
};

export const LayoutNeutral = {
  render: () => html`
    <div style="display: flex; gap: 0; align-items: baseline;">
      <span id="before">before</span><cts-time value=${FIXED} mode="time-of-day"></cts-time
      ><span id="after">after</span>
    </div>
  `,

  async play({ canvasElement }) {
    const host = /** @type {HTMLElement} */ (canvasElement.querySelector("cts-time"));
    expect(host).toBeTruthy();
    // The load-bearing contract: the host contributes no box of its own, so
    // dropping it inline never adds an inline-block gap or shifts baselines.
    expect(getComputedStyle(host).display).toBe("contents");
    // The inner <time> is still a real, rendered element.
    expect(getTimeEl(canvasElement)).toBeTruthy();
  },
};

export const ReactiveUpdate = {
  render: () => html`<cts-time value=${FIXED} mode="time-of-day"></cts-time>`,

  async play({ canvasElement, step }) {
    await step("initial mode renders the time-of-day text", async () => {
      expect(getTimeEl(canvasElement)?.textContent?.trim()).toBe(formatTimeOfDay(FIXED));
    });

    await step("flipping mode re-renders the visible text without a full remount", async () => {
      const host =
        /** @type {HTMLElement & { mode: string; updateComplete: Promise<unknown> } } */ (
          canvasElement.querySelector("cts-time")
        );
      host.mode = "absolute";
      await host.updateComplete;
      expect(getTimeEl(canvasElement)?.textContent?.trim()).toBe(formatAbsolute(FIXED));
    });
  },
};
