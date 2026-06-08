import { LitElement, html, css, nothing, unsafeCSS } from "lit";
import { classMap } from "lit/directives/class-map.js";
import { styleMap } from "lit/directives/style-map.js";

// One-shot decorative celebration overlay: a confetti burst plus a stream of
// falling emoji glyphs, played once when the element connects and then torn
// down. It is purely presentational — non-interactive (`pointer-events: none`),
// hidden from assistive tech (`aria-hidden`), and fully suppressed for users
// who prefer reduced motion.
//
// Light DOM + a single injected <style id> block (the dominant component
// convention, mirroring cts-login-page / cts-flash-highlight). The host element
// itself is `display: contents` so it never perturbs the page layout; the
// pieces live in a `position: fixed` full-viewport overlay layered at
// `z-index: 900` — above page content and the layout overflow popover
// (z-index 750), deliberately BELOW the navbar and toast host (both 1000) so
// confetti appears to fall out from under the header and never covers a toast.
//
// Animation is `transform`/`opacity`-only (compositor thread) driven by one
// shared @keyframes; per-piece randomisation rides in inline CSS custom
// properties. The render model is computed ONCE on connect (no pieces under
// reduced-motion / disabled, satisfying the accessibility gate), and a single
// cleanup timeout — sized to the actual longest piece animation — re-renders
// the overlay away. `disconnectedCallback` clears the timer so a detached
// element (Storybook story swap, DevTools restart) never leaks nodes or timers.

const STYLE_ID = "cts-confetti-styles";

// Committed defaults (see plan 2026-06-08-001 U1 / design-lens review). Named
// constants keep the CSS budget and the JS cleanup math from drifting apart.
const CONFETTI_PIECES = 60; // rectangular confetti count
const EMOJI_PIECES = 15; // falling emoji glyph count
const BASE_DURATION = 2400; // ms — base fall time
const MAX_DELAY = 1200; // ms — max per-piece start delay (staggers the burst)
const BUFFER = 200; // ms — cleanup safety margin past the last animation

// Curated celebratory + thematic (key / shield / check) glyph set. Whitespace
// separated so the `emojis` attribute stays trivial to override from markup.
const DEFAULT_EMOJIS = "🎉 🎊 ✨ 🥳 🔑 🛡️ ✅ 🚀";

// Warm-palette fill tokens only — no status-palette greens/blues (those read as
// data indicators, not decoration) and no hardcoded hex.
const CONFETTI_COLORS = [
  "--orange-300",
  "--orange-400",
  "--orange-500",
  "--sand-300",
  "--sand-200",
  "--ink-300",
];

const FALL_KEYFRAME = "oidf-confetti-fall";

// Scoped styles. The fall `animation` is applied only inside a
// `prefers-reduced-motion: no-preference` guard as a second line of defence —
// the JS gate already produces zero pieces under reduced motion, but if a piece
// ever reaches the DOM without the animation it stays parked above the viewport
// (top: -8vh), i.e. invisible, rather than frozen mid-screen. The @keyframes
// definition itself is inert until referenced, so it lives outside the guard.
const STYLE_TEXT = css`
  cts-confetti {
    display: contents;
  }
  .oidf-confetti-overlay {
    position: fixed;
    inset: 0;
    z-index: 900;
    overflow: hidden;
    pointer-events: none;
    contain: layout style paint;
  }
  .oidf-confetti-piece {
    position: absolute;
    top: -8vh;
    left: var(--x);
    inline-size: 8px;
    block-size: 14px;
    border-radius: 2px;
    scale: var(--scale, 1);
  }
  .oidf-confetti-piece--confetti {
    background: var(--piece-color, var(--orange-400));
  }
  .oidf-confetti-piece--emoji {
    inline-size: auto;
    block-size: auto;
    font-size: 1.6rem;
    line-height: 1;
    background: none;
  }
  @media (prefers-reduced-motion: no-preference) {
    .oidf-confetti-piece {
      animation: ${unsafeCSS(FALL_KEYFRAME)} var(--fall-dur) linear var(--delay) both;
    }
  }
  @keyframes ${unsafeCSS(FALL_KEYFRAME)} {
    0% {
      transform: translate3d(0, 0, 0) rotate(0deg);
      opacity: 0.85;
    }
    70% {
      opacity: 0.85;
    }
    100% {
      transform: translate3d(var(--drift), 118vh, 0) rotate(var(--spin));
      opacity: 0;
    }
  }
`;

/**
 * Inject the cts-confetti scoped stylesheet into `<head>` exactly once.
 * Idempotent: a second call finds the existing `<style>` by id and bails.
 * @returns {void}
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Pick a random element from a non-empty array.
 * @template T
 * @param {T[]} list - The non-empty array to sample from.
 * @returns {T} A uniformly random element of `list`.
 */
function pick(list) {
  return list[Math.floor(Math.random() * list.length)];
}

/**
 * @typedef {object} ConfettiPiece
 * @property {boolean} isEmoji - True for an emoji glyph, false for a confetti rectangle.
 * @property {string} glyph - The emoji character (empty for confetti pieces).
 * @property {string} color - `var(--token)` fill (empty for emoji pieces).
 * @property {string} x - Horizontal start position, e.g. `"42vw"`.
 * @property {string} drift - Horizontal end offset, e.g. `"-8vw"`.
 * @property {string} fallDur - Fall duration, e.g. `"2300ms"`.
 * @property {string} delay - Start delay, e.g. `"700ms"`.
 * @property {string} spin - End rotation, e.g. `"540deg"`.
 * @property {string} scale - Constant scale factor, e.g. `"1.05"`.
 */

/**
 * Decorative one-shot confetti + falling-emoji overlay. Plays once on connect,
 * cleans itself up, and renders nothing when `disabled` or when the user
 * prefers reduced motion. Decorative only: the overlay is `aria-hidden` and
 * `pointer-events: none`, so it never reaches the accessibility tree or
 * intercepts clicks.
 *
 * @property {number} pieces - Confetti rectangle count. Defaults to 60.
 * @property {string} emojis - Whitespace-separated glyphs to rain;
 *   `EMOJI_PIECES` (15) are sampled from the set. Defaults to a celebratory set.
 * @property {number} duration - Base fall duration in ms. Defaults to 2400.
 * @property {boolean} disabled - Hard opt-out; reflected attribute. When set, no
 *   overlay and no pieces are produced (same no-spawn path as reduced motion).
 */
class CtsConfetti extends LitElement {
  static properties = {
    pieces: { type: Number },
    emojis: { type: String },
    duration: { type: Number },
    disabled: { type: Boolean, reflect: true },
    // Reactive render model — state drives render(). Empty array (the resting and
    // post-cleanup state) renders nothing; populating it on connect paints the
    // burst. Keeping this reactive (rather than a separate _done flag) means a
    // disconnect/reconnect of the same instance replays cleanly.
    _model: { state: true },
  };

  constructor() {
    super();
    this.pieces = CONFETTI_PIECES;
    this.emojis = DEFAULT_EMOJIS;
    this.duration = BASE_DURATION;
    this.disabled = false;
    /** @type {ConfettiPiece[]} Reactive render model. */
    this._model = [];
    /** @type {ReturnType<typeof setTimeout>|null} */
    this._cleanupTimer = null;
  }

  // Light DOM so the injected global keyframes apply and the fixed overlay
  // escapes any stacking context the host might otherwise create.
  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    this._spawn();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._cleanupTimer) {
      clearTimeout(this._cleanupTimer);
      this._cleanupTimer = null;
    }
    // Reset to the resting state so re-inserting the same instance replays a
    // fresh burst on the next connectedCallback rather than staying dark.
    this._model = [];
  }

  /** @returns {boolean} True when the user has requested reduced motion. */
  _prefersReducedMotion() {
    return (
      typeof window.matchMedia === "function" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches
    );
  }

  // Build the render model. The accessibility gate (R2) and the `disabled`
  // opt-out both short-circuit to an empty model so render() emits `nothing`.
  _spawn() {
    // Never leave a prior burst's timer armed (guards a connect without an
    // intervening disconnect).
    if (this._cleanupTimer) {
      clearTimeout(this._cleanupTimer);
      this._cleanupTimer = null;
    }

    if (this.disabled || this._prefersReducedMotion()) {
      this._model = [];
      return;
    }

    const emojiList = this.emojis.split(/\s+/).filter(Boolean);
    // Sanitize to a finite, bounded, non-negative integer so a stray
    // `pieces="Infinity"` / `pieces="abc"` attribute can never hang the tab in
    // an unbounded loop or silently suppress the whole burst via NaN.
    const confettiCount = Math.min(Math.max(0, Math.floor(Number(this.pieces) || 0)), 500);
    const emojiCount = emojiList.length ? EMOJI_PIECES : 0;
    const total = confettiCount + emojiCount;

    /** @type {ConfettiPiece[]} */
    const model = [];
    let maxEnd = 0;
    for (let i = 0; i < total; i++) {
      const isEmoji = i >= confettiCount;
      const delay = Math.random() * MAX_DELAY;
      // Clamp the randomised fall so delay + fall never exceeds
      // BASE_DURATION + MAX_DELAY; the cleanup timer is sized to the real max.
      const span = this.duration + MAX_DELAY - delay;
      const fall = Math.min(this.duration * (0.75 + Math.random() * 0.5), span);
      maxEnd = Math.max(maxEnd, delay + fall);

      model.push({
        isEmoji,
        glyph: isEmoji ? pick(emojiList) : "",
        color: isEmoji ? "" : `var(${pick(CONFETTI_COLORS)})`,
        x: `${Math.random() * 100}vw`,
        drift: `${(Math.random() * 2 - 1) * 18}vw`,
        fallDur: `${Math.round(fall)}ms`,
        delay: `${Math.round(delay)}ms`,
        spin: `${Math.round((Math.random() * 2 - 1) * 720)}deg`,
        scale: (0.7 + Math.random() * 0.6).toFixed(2),
      });
    }

    this._model = model;
    this._cleanupTimer = setTimeout(
      () => {
        this._cleanupTimer = null;
        // Empty the reactive model -> render() emits nothing -> overlay removed.
        this._model = [];
      },
      Math.ceil(maxEnd) + BUFFER,
    );
  }

  render() {
    if (this._model.length === 0) return nothing;
    return html`<div class="oidf-confetti-overlay" aria-hidden="true">
      ${this._model.map(
        (p) =>
          html`<span
            class=${classMap({
              "oidf-confetti-piece": true,
              "oidf-confetti-piece--emoji": Boolean(p.isEmoji),
              "oidf-confetti-piece--confetti": !p.isEmoji,
            })}
            style=${styleMap({
              "--x": p.x,
              "--drift": p.drift,
              "--fall-dur": p.fallDur,
              "--delay": p.delay,
              "--spin": p.spin,
              "--scale": p.scale,
              ...(p.isEmoji ? {} : { "--piece-color": p.color }),
            })}
            >${p.isEmoji ? p.glyph : nothing}</span
          >`,
      )}
    </div>`;
  }
}

customElements.define("cts-confetti", CtsConfetti);

export {};
