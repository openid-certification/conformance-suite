/**
 * Behavioral wrapper that paints a one-shot "your selection landed here"
 * highlight wash over all of its children. The host page calls
 * `flashHighlight()` right after scrolling the group into view following a
 * jump-to action (e.g. picking a plan from the search list on
 * `schedule-test.html`, which then scrolls the spec cascade + variant
 * selectors into view).
 *
 * Vanilla HTMLElement following the cts-action-bar / cts-card / cts-tooltip
 * convention (light DOM, scoped style injected once into <head>). Unlike
 * cts-action-bar it does NOT reparent its children — it leaves the light-DOM
 * subtree untouched so `document.getElementById(...)` lookups into wrapped
 * content (e.g. `#planSelect`, `#variantSelectors`) keep working — and just
 * decorates the host with a `position: relative` box plus an absolutely
 * positioned `::after` wash. The wash overhangs the host by `--space-4` (the
 * "padding around it") and sits at `z-index: -1` behind the host's content, so
 * the wrapped controls stay legible and clickable during the flash.
 *
 * Flash state lives in the component-owned `data-flashing` attribute (not a
 * class — classes on the host belong to the consumer; see eslint
 * `wc/no-self-class`). Style hooks: `cts-flash-highlight` for the base box,
 * `cts-flash-highlight[data-flashing]::after` for the wash.
 *
 * @property {boolean} data-flashing - Reflected, component-managed state
 *   attribute. Present (empty value) while the scroll-in wash is animating,
 *   absent at rest. Set and cleared only by `flashHighlight()` and its
 *   cleanup; consumers observe it but should not toggle it directly.
 * @method flashHighlight - Paint a one-shot scroll-in highlight wash over the
 *   wrapped content; call after scrolling it into view. Honors
 *   prefers-reduced-motion. Re-calling while a flash is in flight restarts it
 *   from zero so rapid re-triggers always read as a fresh arrival.
 */

const STYLE_ID = "cts-flash-highlight-styles";

// Single source of truth for the scroll-in flash duration. Interpolated into
// the @keyframes animation below and reused by flashHighlight()'s cleanup
// timer so the CSS and JS can never drift out of sync.
const FLASH_DURATION_MS = 1600;

// Component-owned state attribute toggled on the host; the keyframe name is
// asserted against in the animationend guard.
const FLASHING_ATTR = "data-flashing";
const FLASH_KEYFRAME = "oidf-flash-highlight-flash";

const STYLE_TEXT = `
cts-flash-highlight {
  position: relative;
  display: block;
}
/* Basecamp-style scroll-in highlight (driven by flashHighlight(); wired from
   the cts-plan-select handler in schedule-test.html). A decorative ::after
   wash overhangs the host box by --space-4 — the "padding around it" — and
   sits behind the wrapped controls (z-index:-1 against the transparent host)
   so they stay legible and clickable during the flash. */
cts-flash-highlight[${FLASHING_ATTR}]::after {
  content: "";
  position: absolute;
  inset: calc(-1 * var(--space-4));
  z-index: -1;
  border-radius: var(--radius-4);
  background: var(--orange-100);
  pointer-events: none;
  opacity: 0;
}
@media (prefers-reduced-motion: no-preference) {
  /* Snap in (peak at 12%), then a slow dissolve over the long tail. */
  cts-flash-highlight[${FLASHING_ATTR}]::after {
    animation: ${FLASH_KEYFRAME} ${FLASH_DURATION_MS}ms ease-out;
  }
}
@media (prefers-reduced-motion: reduce) {
  /* No motion: show a brief static wash so the arrival is still perceptible.
     flashHighlight() clears the attribute via setTimeout, since animationend
     never fires when there is no animation. */
  cts-flash-highlight[${FLASHING_ATTR}]::after {
    opacity: 1;
  }
}
@keyframes ${FLASH_KEYFRAME} {
  0% { opacity: 0; }
  12% { opacity: 1; }
  100% { opacity: 0; }
}
`;

/**
 * Inject the cts-flash-highlight scoped stylesheet into `<head>` exactly once.
 * Idempotent: subsequent calls find the existing `<style>` tag by id and bail.
 * @returns {void}
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

class CtsFlashHighlight extends HTMLElement {
  constructor() {
    super();
    // Pending cleanup timer for the flash; cleared/reset on re-trigger and on
    // disconnect so a detached element never fires a stray attribute write.
    this._flashTimer = null;
    // Bound once so add/removeEventListener pair correctly across connect cycles.
    this._onFlashEnd = this._onFlashEnd.bind(this);
  }

  connectedCallback() {
    injectStyles();
    // animationend from the ::after pseudo-element bubbles to the host. Listen
    // here so the motion path clears the attribute as soon as the wash ends.
    this.addEventListener("animationend", this._onFlashEnd);
  }

  disconnectedCallback() {
    this.removeEventListener("animationend", this._onFlashEnd);
    if (this._flashTimer) {
      clearTimeout(this._flashTimer);
      this._flashTimer = null;
    }
  }

  /**
   * Flash a one-shot "your selection landed here" highlight over the wrapped
   * content. Honors `prefers-reduced-motion`: motion-OK users get the animated
   * flash cleared on `animationend` (with a timer safety net); reduced-motion
   * users get a brief *static* wash cleared purely by the timer, because
   * `animationend` never fires when the animation is suppressed — relying on it
   * alone would strand the wash on screen.
   *
   * @returns {void}
   */
  flashHighlight() {
    const reduce =
      typeof window !== "undefined" &&
      typeof window.matchMedia === "function" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    if (this._flashTimer) {
      clearTimeout(this._flashTimer);
      this._flashTimer = null;
    }

    // Drop the attribute and force a synchronous reflow before re-adding it, so
    // a re-trigger mid-flash replays the keyframes from 0% rather than
    // continuing mid-curve. On the first call the remove is a harmless no-op.
    this.removeAttribute(FLASHING_ATTR);
    void this.offsetWidth;
    this.setAttribute(FLASHING_ATTR, "");

    // Motion path: safety net in case `animationend` is missed. Reduced-motion
    // path: the sole cleanup, since no animation (and thus no `animationend`)
    // runs. A touch longer than the animation so `animationend` wins when it
    // does fire.
    this._flashTimer = setTimeout(
      () => {
        this.removeAttribute(FLASHING_ATTR);
        this._flashTimer = null;
      },
      reduce ? FLASH_DURATION_MS : FLASH_DURATION_MS + 200,
    );
  }

  /**
   * Clear the wash when its keyframe animation ends (motion path).
   * @param {AnimationEvent} e - The bubbled animationend event.
   * @returns {void}
   */
  _onFlashEnd(e) {
    if (e.animationName !== FLASH_KEYFRAME) return;
    if (this._flashTimer) {
      clearTimeout(this._flashTimer);
      this._flashTimer = null;
    }
    this.removeAttribute(FLASHING_ATTR);
  }
}

customElements.define("cts-flash-highlight", CtsFlashHighlight);

export {};
