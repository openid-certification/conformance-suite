// Shared icon-flash helper for "copy to clipboard" affordances.
//
// Call `flashCopyConfirmed(host)` after a successful clipboard write to
// briefly swap the icon inside `host` to a checkmark with a small
// zoom-out / zoom-in crossfade, then revert with the same effect in
// reverse. This is the canonical local-success affordance for any copy
// button in the suite — use it instead of bespoke "--copied" classes
// or one-off toasts.
//
// Recognised hosts:
//   - <cts-button>      — drives the `.icon` reactive property
//   - <cts-link-button> — drives the `.icon` reactive property
//   - <cts-badge>       — drives the `icon` attribute (badge re-renders
//                         and replaces the inner cts-icon element)
//   - any element containing a <cts-icon> — swaps `name` on the inner icon
//
// The helper is dual-format: ES-module callers `import { flashCopyConfirmed }
// from ".../cts-copy-flash.js"`. Legacy HTML scripts include this file
// via `<script type="module" src="js/cts-copy-flash.js"></script>` and call
// `window.ctsCopyFlash(host)` from regular non-module handlers (e.g. the
// ClipboardJS success callbacks in plans.html / logs.html).
//
// Animation note. The crossfade runs in two phases on the inner
// `<cts-icon>` element via the Web Animations API. Phase 1 (out) shrinks
// the OLD icon to scale 0.6 + opacity 0. The icon name is then swapped
// (which for cts-badge replaces the cts-icon element entirely; for Lit
// hosts it merely updates the `name` attribute on the same element). We
// re-query after the swap so the next animation always targets the
// element actually in the DOM. Inline `transform` / `opacity` are set
// on the new element before phase 2 starts so it doesn't briefly render
// at full size before the animation reads its first keyframe.

const DEFAULT_HOLD_MS = 900;
const DEFAULT_CONFIRM_ICON = "check";
// One frame at 60Hz; the phase durations are tuned in whole-frame units
// so the animation lands cleanly on vsync boundaries.
const FRAME_MS = 1000 / 60;
const PHASE_OUT_MS = 100 - FRAME_MS;
const PHASE_IN_MS = 100 - FRAME_MS;
const EASING_OUT = "cubic-bezier(0.4, 0, 1, 1)";
const EASING_IN = "cubic-bezier(0, 0, 0.2, 1)";
// Back-out with pronounced overshoot. P1.y is well above 1, so the
// eased progress peaks around 1.45 partway through the curve before
// settling at 1. Applied to the keyframe pair scale(0.6) → scale(1),
// that extrapolates to a peak around scale(1.18) — visibly bouncier
// than the standard easeOutBack (which tops out near scale(1.04) with
// the same range). Used for the confirmation icon's entrance so the
// swap reads as a clear "pop", not a flat zoom-in.
const EASING_IN_OVERSHOOT = "cubic-bezier(0.34, 2.5, 0.64, 1)";

/**
 * @typedef {object} FlashOptions
 * @property {string} [confirmIcon="check"] - Icon name to swap in.
 * @property {number} [holdMs=900] - Time the confirmation icon stays
 *   visible between phase-in and phase-out of the revert.
 */

/**
 * @param {Element} host
 * @returns {{ get: () => string, set: (n: string) => void } | null}
 */
function resolveIconAccessor(host) {
  const tag = (host.tagName || "").toLowerCase();
  if (tag === "cts-button" || tag === "cts-link-button") {
    return {
      get: () => /** @type {any} */ (host).icon || "",
      set: (n) => {
        /** @type {any} */ (host).icon = n;
      },
    };
  }
  if (tag === "cts-badge") {
    return {
      get: () => host.getAttribute("icon") || "",
      set: (n) => host.setAttribute("icon", n),
    };
  }
  if (host.querySelector("cts-icon")) {
    return {
      get: () => {
        const el = host.querySelector("cts-icon");
        return el ? el.getAttribute("name") || "" : "";
      },
      set: (n) => {
        const el = host.querySelector("cts-icon");
        if (el) el.setAttribute("name", n);
      },
    };
  }
  return null;
}

/**
 * Wait for a host's reactive update to flush. cts-button and
 * cts-link-button expose `updateComplete` (Lit); cts-badge does not
 * (vanilla custom element with synchronous attributeChangedCallback).
 * @param {Element} host
 * @returns {Promise<void>}
 */
async function waitForUpdate(host) {
  const updateComplete = /** @type {any} */ (host).updateComplete;
  if (updateComplete && typeof updateComplete.then === "function") {
    try {
      await updateComplete;
    } catch {
      /* ignore — Lit update errors are not our concern here */
    }
  }
}

/**
 * Crossfade the cts-icon currently inside `host` from one icon name to
 * another. Phase 1: zoom-out + fade-out the visible icon. Swap the name.
 * Phase 2: zoom-in + fade-in the new icon. Returns when both phases have
 * settled (or immediately if the host has no cts-icon / no Web Animations
 * API support).
 *
 * @param {Element} host
 * @param {string} toIcon
 * @param {{ get: () => string, set: (n: string) => void }} accessor
 * @param {boolean} [overshoot=false] - When true, the phase-in uses a
 *   back-out easing so the new icon briefly passes its final size
 *   before settling. Use for the confirmation icon's entrance; leave
 *   false for the revert so the original icon settles cleanly.
 * @returns {Promise<void>}
 */
async function crossfadeIcon(host, toIcon, accessor, overshoot = false) {
  const oldIcon = host.querySelector("cts-icon");
  if (!oldIcon || typeof /** @type {any} */ (oldIcon).animate !== "function") {
    accessor.set(toIcon);
    return;
  }

  // Phase 1: shrink + fade the outgoing icon to invisible.
  const out = /** @type {any} */ (oldIcon).animate(
    [
      { transform: "scale(1)", opacity: 1 },
      { transform: "scale(0.6)", opacity: 0 },
    ],
    { duration: PHASE_OUT_MS, easing: EASING_OUT, fill: "forwards" },
  );
  try {
    await out.finished;
  } catch {
    /* canceled — proceed anyway so the icon doesn't get stuck invisible */
  }

  accessor.set(toIcon);
  await waitForUpdate(host);

  // Re-query: cts-badge replaces the cts-icon element on attribute
  // change (its _render calls this.replaceChildren), so the previous
  // reference is now detached.
  const newIcon = host.querySelector("cts-icon");
  if (!newIcon) return;

  // Lock the new element's initial state so it doesn't pop at full size
  // for one frame between insertion and animation start. Without this,
  // cts-badge's freshly-inserted cts-icon briefly shows at scale(1)
  // before the animation's first keyframe takes effect.
  /** @type {HTMLElement} */ (newIcon).style.transform = "scale(0.6)";
  /** @type {HTMLElement} */ (newIcon).style.opacity = "0";

  const inAnim = /** @type {any} */ (newIcon).animate(
    [
      { transform: "scale(0.6)", opacity: 0 },
      { transform: "scale(1)", opacity: 1 },
    ],
    {
      duration: PHASE_IN_MS,
      easing: overshoot ? EASING_IN_OVERSHOOT : EASING_IN,
      fill: "forwards",
    },
  );
  try {
    await inAnim.finished;
  } catch {
    /* canceled */
  }
  // Clear the inline overrides so subsequent style changes (theme,
  // hover, future flashes) compose normally. fill:"forwards" keeps the
  // animation's end state in effect even after we strip the inline
  // styles, so there's no visual jump.
  /** @type {HTMLElement} */ (newIcon).style.transform = "";
  /** @type {HTMLElement} */ (newIcon).style.opacity = "";
}

/**
 * Briefly swap the icon inside `host` to a confirmation icon with a
 * zoom-out / zoom-in crossfade, then revert with the same effect in
 * reverse. Idempotent: repeated calls during an active flash cancel
 * the pending revert and start a fresh flash without losing the
 * original icon name.
 *
 * @param {Element | null | undefined} host
 * @param {FlashOptions} [opts]
 * @returns {Promise<void>}
 */
export async function flashCopyConfirmed(host, opts) {
  if (!host) return;
  const confirmIcon = (opts && opts.confirmIcon) || DEFAULT_CONFIRM_ICON;
  const holdMs = (opts && opts.holdMs) || DEFAULT_HOLD_MS;
  const accessor = resolveIconAccessor(host);
  if (!accessor) return;

  // Cancel any in-flight revert from a prior flash. Preserve the very
  // first originalIcon across rapid repeats so we always restore the
  // user-facing icon, never a stale "check".
  const tracker = /** @type {any} */ (host)._ctsCopyFlash;
  if (tracker) {
    clearTimeout(tracker.timer);
  } else {
    /** @type {any} */ (host)._ctsCopyFlash = { originalIcon: accessor.get() };
  }
  const state = /** @type {any} */ (host)._ctsCopyFlash;

  // Overshoot on the way in so the confirmation reads as a tiny "pop";
  // settle cleanly on the way out so the revert is unobtrusive.
  await crossfadeIcon(host, confirmIcon, accessor, true);

  state.timer = setTimeout(async () => {
    await crossfadeIcon(host, state.originalIcon, accessor, false);
    /** @type {any} */ (host)._ctsCopyFlash = null;
  }, holdMs);
}

if (typeof window !== "undefined") {
  /** @type {any} */ (window).ctsCopyFlash = flashCopyConfirmed;
}
