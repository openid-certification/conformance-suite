import { html } from "lit";
import { expect, waitFor } from "storybook/test";
import "./cts-confetti.js";

export default {
  title: "Components/cts-confetti",
  component: "cts-confetti",
  argTypes: {
    pieces: { control: "number" },
    emojis: { control: "text" },
    duration: { control: "number" },
    disabled: { control: "boolean" },
  },
};

// Resolve the upgraded host element once the custom element is defined.
async function whenUpgraded(canvasElement) {
  await customElements.whenDefined("cts-confetti");
  const element = /** @type {any} */ (canvasElement.querySelector("cts-confetti"));
  await waitFor(() => {
    expect(element).toBeTruthy();
  });
  return element;
}

function pieceCount(canvasElement) {
  return canvasElement.querySelectorAll(".oidf-confetti-piece").length;
}

// --- Stories ---

/**
 * The effect playing: a fixed, decorative overlay spawns confetti + emoji
 * pieces. The overlay is `aria-hidden` and `pointer-events: none`, so it never
 * reaches assistive tech or intercepts clicks. Piece count is randomised-but-
 * bounded, so assertions check "non-zero" rather than an exact number.
 */
export const Default = {
  render: () => html`<cts-confetti></cts-confetti>`,
  async play({ canvasElement, step }) {
    await whenUpgraded(canvasElement);

    await step("a decorative overlay spawns with pieces", async () => {
      await waitFor(() => {
        const overlay = canvasElement.querySelector(".oidf-confetti-overlay");
        expect(overlay).toBeTruthy();
      });
      expect(pieceCount(canvasElement)).toBeGreaterThan(0);
    });

    await step("overlay is hidden from assistive tech (R4)", async () => {
      const overlay = /** @type {HTMLElement} */ (
        canvasElement.querySelector(".oidf-confetti-overlay")
      );
      expect(overlay.getAttribute("aria-hidden")).toBe("true");
      // Decorative-only: no interactive role.
      expect(overlay.hasAttribute("role")).toBe(false);
    });

    await step("overlay never intercepts clicks (R3)", async () => {
      const overlay = /** @type {HTMLElement} */ (
        canvasElement.querySelector(".oidf-confetti-overlay")
      );
      expect(getComputedStyle(overlay).pointerEvents).toBe("none");
    });

    await step("both confetti and emoji pieces are present", async () => {
      expect(
        canvasElement.querySelectorAll(".oidf-confetti-piece--confetti").length,
      ).toBeGreaterThan(0);
      expect(canvasElement.querySelectorAll(".oidf-confetti-piece--emoji").length).toBeGreaterThan(
        0,
      );
    });
  },
};

/**
 * The `disabled` opt-out: no overlay, no pieces. This is the hard off-switch
 * (distinct from the reduced-motion gate, which is covered separately below).
 */
export const Disabled = {
  render: () => html`<cts-confetti disabled></cts-confetti>`,
  async play({ canvasElement, step }) {
    await whenUpgraded(canvasElement);

    await step("no overlay and no pieces are produced", async () => {
      // Give the (suppressed) effect a frame to prove it stays empty.
      await new Promise((r) => requestAnimationFrame(() => r(undefined)));
      expect(canvasElement.querySelector(".oidf-confetti-overlay")).toBeNull();
      expect(pieceCount(canvasElement)).toBe(0);
    });
  },
};

/**
 * The reduced-motion accessibility gate (R2). `window.matchMedia` is mocked to
 * report `prefers-reduced-motion: reduce` BEFORE the element mounts (via
 * `beforeEach`, since the effect fires on connect — a `play`-time mock would be
 * too late), and restored afterwards. This exercises the real `matchMedia`
 * branch, which the `disabled` story does not reach.
 */
export const ReducedMotion = {
  beforeEach() {
    const original = window.matchMedia;
    window.matchMedia = /** @type {any} */ (
      (query) => ({
        matches: query.includes("prefers-reduced-motion: reduce"),
        media: query,
        onchange: null,
        addEventListener() {},
        removeEventListener() {},
        addListener() {},
        removeListener() {},
        dispatchEvent() {
          return false;
        },
      })
    );
    return () => {
      window.matchMedia = original;
    };
  },
  render: () => html`<cts-confetti></cts-confetti>`,
  async play({ canvasElement, step }) {
    await whenUpgraded(canvasElement);

    await step("reduced-motion users get no overlay and no pieces", async () => {
      await new Promise((r) => requestAnimationFrame(() => r(undefined)));
      expect(canvasElement.querySelector(".oidf-confetti-overlay")).toBeNull();
      expect(pieceCount(canvasElement)).toBe(0);
    });
  },
};

/**
 * Custom glyph set and a smaller piece count — proves the `emojis`/`pieces`
 * props flow through to the render model.
 */
export const DenseEmoji = {
  render: () => html`<cts-confetti pieces="20" emojis="🎈 🎁 🌟"></cts-confetti>`,
  async play({ canvasElement, step }) {
    await whenUpgraded(canvasElement);

    await step("custom props spawn confetti + emoji pieces", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector(".oidf-confetti-overlay")).toBeTruthy();
      });
      expect(canvasElement.querySelectorAll(".oidf-confetti-piece--confetti").length).toBe(20);
      expect(canvasElement.querySelectorAll(".oidf-confetti-piece--emoji").length).toBeGreaterThan(
        0,
      );
    });
  },
};

/**
 * Cleanup contract: when the host is removed from the DOM, its overlay and
 * pending cleanup timer go with it — guarding against cross-story timer/DOM
 * leaks and DevTools-restart residue.
 */
export const CleansUpOnDisconnect = {
  render: () => html`<cts-confetti></cts-confetti>`,
  async play({ canvasElement, step }) {
    const element = await whenUpgraded(canvasElement);

    await step("overlay is present while mounted", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector(".oidf-confetti-overlay")).toBeTruthy();
      });
    });

    await step("removing the host clears the overlay", async () => {
      element.remove();
      await waitFor(() => {
        expect(canvasElement.querySelector(".oidf-confetti-overlay")).toBeNull();
      });
    });
  },
};
