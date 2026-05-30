import { html } from "lit";
import { expect, waitFor } from "storybook/test";
import "./cts-spinner.js";

export default {
  title: "Primitives/cts-spinner",
  component: "cts-spinner",
};

// --- Stories ---

export const Default = {
  render: () => html`<cts-spinner></cts-spinner>`,

  async play({ canvasElement }) {
    const spinner = canvasElement.querySelector("cts-spinner");
    await waitFor(() => expect(spinner).toBeTruthy());
    if (!spinner) throw new Error("cts-spinner not rendered");

    // role=status is the durable signal that the host registered as a
    // live region — the loading-modal contract depends on this so screen
    // readers announce the activity state when the modal appears.
    expect(spinner.getAttribute("role")).toBe("status");

    // Default label is "Loading" — both as aria-label on the host and as a
    // visually-hidden span inside, mirroring the duplication discipline
    // that keeps DOM-preferring and ARIA-preferring AT in sync.
    expect(spinner.getAttribute("aria-label")).toBe("Loading");
    const srText = spinner.querySelector(".cts-spinner-sr-only");
    expect(srText).toBeTruthy();
    expect(srText.textContent).toBe("Loading");

    // The ring is inline SVG <circle>s sized by the size attribute, not an
    // <img>. This is the regression assertion for the GIF removal. Two-tone:
    // a full-circle track under a dash-clipped indicator arc, both coloured
    // via CSS classes (so neither carries an inline stroke attribute).
    const svg = spinner.querySelector("svg");
    expect(svg).toBeTruthy();
    expect(svg.getAttribute("aria-hidden")).toBe("true");
    const circles = svg.querySelectorAll("circle");
    expect(circles.length).toBe(2);
    expect(svg.querySelector(".cts-spinner-track")).toBeTruthy();
    expect(svg.querySelector(".cts-spinner-indicator")).toBeTruthy();
    expect(circles[0].getAttribute("stroke")).toBeNull(); // stroke set via CSS

    // Default size is "md" — the host carries data-size="md" so the
    // scoped stylesheet keys off it. This is the contract between the
    // attribute API and the CSS rule sheet.
    expect(spinner.getAttribute("data-size")).toBe("md");
  },
};

export const CustomLabel = {
  render: () => html`<cts-spinner label="Saving your changes"></cts-spinner>`,

  async play({ canvasElement }) {
    const spinner = canvasElement.querySelector("cts-spinner");
    if (!spinner) throw new Error("cts-spinner not rendered");

    expect(spinner.getAttribute("aria-label")).toBe("Saving your changes");
    const srText = spinner.querySelector(".cts-spinner-sr-only");
    expect(srText.textContent).toBe("Saving your changes");
  },
};

export const SmallSize = {
  render: () => html`<cts-spinner size="sm" label="Loading"></cts-spinner>`,

  async play({ canvasElement }) {
    const spinner = canvasElement.querySelector("cts-spinner");
    if (!spinner) throw new Error("cts-spinner not rendered");
    expect(spinner.getAttribute("data-size")).toBe("sm");

    // Computed width on the inner SVG tracks the size scale. 24px for sm
    // is the engineering contract — dense surfaces like inline-in-text
    // would otherwise overflow visually.
    const svg = /** @type {SVGElement} */ (spinner.querySelector("svg"));
    const rect = svg.getBoundingClientRect();
    expect(Math.round(rect.width)).toBe(24);
  },
};

export const LargeSize = {
  render: () => html`<cts-spinner size="lg" label="Loading"></cts-spinner>`,

  async play({ canvasElement }) {
    const spinner = canvasElement.querySelector("cts-spinner");
    if (!spinner) throw new Error("cts-spinner not rendered");
    expect(spinner.getAttribute("data-size")).toBe("lg");

    const svg = /** @type {SVGElement} */ (spinner.querySelector("svg"));
    const rect = svg.getBoundingClientRect();
    expect(Math.round(rect.width)).toBe(64);
  },
};

/**
 * Misspelled or otherwise unknown size values fall back to "md" rather than
 * leaving the host without a data-size — an unsized spinner would have no
 * dimensions from the scoped stylesheet and render invisibly. The fallback
 * keeps the spinner visible while making the typo obvious to a reviewer
 * (the rendered size will be wrong).
 */
export const UnknownSizeFallsBackToMd = {
  render: () => html`<cts-spinner size="bogus" label="Loading"></cts-spinner>`,

  async play({ canvasElement }) {
    const spinner = canvasElement.querySelector("cts-spinner");
    if (!spinner) throw new Error("cts-spinner not rendered");
    expect(spinner.getAttribute("data-size")).toBe("md");
  },
};

/**
 * AllSizes — visual reference rendering of every valid size, used by the
 * Storybook visual review pass when the spinner ships. The play function
 * is light because per-size assertions live in SmallSize / Default / LargeSize.
 */
export const AllSizes = {
  render: () => html`
    <div style="display: flex; gap: 32px; align-items: center;">
      <cts-spinner size="sm" label="Loading"></cts-spinner>
      <cts-spinner size="md" label="Loading"></cts-spinner>
      <cts-spinner size="lg" label="Loading"></cts-spinner>
    </div>
  `,

  async play({ canvasElement }) {
    const spinners = canvasElement.querySelectorAll("cts-spinner");
    expect(spinners.length).toBe(3);
    expect(spinners[0].getAttribute("data-size")).toBe("sm");
    expect(spinners[1].getAttribute("data-size")).toBe("md");
    expect(spinners[2].getAttribute("data-size")).toBe("lg");
  },
};
