import { LitElement, html, nothing, css } from "lit";
import "./cts-badge.js";
import "./cts-icon.js";
import {
  classifyRuns,
  IN_PROGRESS_LOGS_QUERY,
  FAILING_LOGS_QUERY,
} from "../js/run-classification.js";

const STYLE_ID = "cts-run-status-strip-styles";

const STYLE_TEXT = css`
  .runStrip {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-2) var(--space-5);
    min-height: 20px;
    margin-bottom: var(--space-4);
    font-family: var(--font-sans);
    font-size: var(--fs-14);
    line-height: var(--lh-snug);
    color: var(--fg);
  }
  .runStrip--clear,
  .runStrip--error {
    gap: var(--space-2);
    color: var(--fg-soft);
  }
  .runStrip--clear cts-icon,
  .runStrip--error cts-icon {
    color: var(--fg-soft);
  }
  .runStrip-link {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    text-decoration: none;
    color: var(--fg);
    border-radius: var(--radius-2);
  }
  /* The cts-badge carries the affordance ring (interactive), so the link
 * itself stays underline-free — the pill silhouette is what reads as
 * clickable (CLAUDE.md Badges affordance rule). Keyboard focus still needs a
 * visible ring, so surface the standard focus token on focus-visible. */
  .runStrip-link:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  .runStrip-skeleton {
    display: inline-block;
    width: 240px;
    height: 24px;
    border-radius: var(--radius-pill);
    background: var(--bg-elev);
    position: relative;
    overflow: hidden;
  }
  .runStrip-skeleton::after {
    content: "";
    position: absolute;
    inset: 0;
    transform: translateX(-100%);
    background: linear-gradient(90deg, transparent, var(--bg) 50%, transparent);
    animation: runStripShimmer 1.2s ease-in-out infinite;
  }
  @media (prefers-reduced-motion: reduce) {
    .runStrip-skeleton::after {
      animation: none;
    }
  }
  @keyframes runStripShimmer {
    100% {
      transform: translateX(100%);
    }
  }
`;

/**
 * Inject the scoped stylesheet for `cts-run-status-strip` into `<head>` once.
 * The `STYLE_ID` flag makes this a no-op on subsequent mounts so multiple
 * instances on the same page do not duplicate the rules. Mirrors the
 * head-style injection pattern used by `cts-footer` / `cts-card`.
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Always-on run-status strip for the plans home. Surfaces the one dashboard
 * signal worth keeping when the launchpad is retired: the authenticated
 * user's actionable runs (in-progress + failing counts), each linking into
 * the matching filtered logs view. It is the deliberate exception to the
 * "no decorative counts" rule — its actionable state earns its count
 * ("act on me"); its all-clear state carries no count, just confirmation.
 *
 * Four render states:
 * - **loading** — a shimmer skeleton on first paint (R8), so the toolbar
 *   below does not jump when the counts resolve. Paired with the
 *   `:not(:defined)` ~44px reservation in `css/layout.css` (KTD6).
 * - **actionable** — one link per NON-zero count (AE2: never a fabricated
 *   "0 failing" element). In-progress → `logs.html?status=running,waiting`;
 *   failing → `logs.html?result=failed,unknown` (the run-classification
 *   helper's exported query constants).
 * - **all caught up** — the user has runs but none are in-progress or
 *   failing: a check + confirmation, no count (R8/AE1).
 * - **error** — `/api/log` failed: a calm "couldn't load run status" line
 *   (R20) that is neither hidden nor implies all-clear.
 *
 * Renders `nothing` (collapses to zero height) when hidden — the anonymous
 * path, the Published view, or a zero-runs account (AE1b/R9).
 *
 * **Deferred fetch (KTD3).** The strip never fetches on its own at connect.
 * The owning page resolves auth once (`/api/currentuser`) and then drives the
 * strip: `fetchRuns()` on the authenticated My view, `hide()` for anon or the
 * Published view. This keeps a single auth probe on the home critical path and
 * guarantees the strip never requests `/api/log` for an anonymous visitor (R9).
 * A `start-hidden` attribute (set synchronously by the page for a
 * `?public=true` deep link) suppresses the first-paint skeleton entirely on the
 * public path, mirroring `cts-plan-list`'s `defer-initial-fetch` idiom.
 *
 * Light DOM (`createRenderRoot` returns `this`) like the other `cts-*`
 * components, so the strip inherits the page's typographic tokens. The host
 * carries `aria-live="polite"` so the resolved summary is announced once
 * without a search input on the host to make every keystroke chatty (R19).
 *
 * @property {string} _status - Internal reactive state: one of `"loading"`,
 *   `"ready"`, `"error"`, or `"hidden"`. Drives which state the strip renders.
 * @property {number} _inProgressCount - Internal reactive state: count of
 *   RUNNING/WAITING runs from the most-recent `/api/log` window. Valid only
 *   when `_status === "ready"`.
 * @property {number} _failingCount - Internal reactive state: count of
 *   FAILED/UNKNOWN runs from the most-recent `/api/log` window. Valid only
 *   when `_status === "ready"`.
 * @property {number} _totalRuns - Internal reactive state: total run count in
 *   the fetched window, used to distinguish "all caught up" (has runs) from
 *   "hidden" (zero runs, AE1b).
 * @fires nothing - The component emits no custom events; navigation is plain
 *   `<a href>` so cmd/middle-click and screen-reader destinations all work.
 */
class CtsRunStatusStrip extends LitElement {
  static properties = {
    _status: { state: true },
    _inProgressCount: { state: true },
    _failingCount: { state: true },
    _totalRuns: { state: true },
  };

  constructor() {
    super();
    this._status = "loading";
    this._inProgressCount = 0;
    this._failingCount = 0;
    this._totalRuns = 0;
    /**
     * Monotonic fetch-generation token. Bumped at the top of every
     * `fetchRuns()` and inside `hide()`; each `fetchRuns()` snapshots it and
     * discards its own post-`await` writes if a newer call (another
     * `fetchRuns()` or a `hide()`) has since bumped it. This makes call order —
     * i.e. the latest user intent — authoritative over response order, so a
     * slow `/api/log` resolving after the user switched to Published cannot
     * resurrect the personal strip over the public browser (R9). Not a reactive
     * property; it never drives a render on its own.
     * @type {number}
     */
    this._fetchSeq = 0;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    // R19: announce the resolved summary politely. Set on the host (the stable
    // wrapper across loading→ready renders), mirroring cts-plan-list /
    // cts-log-list. The strip has no input children, so the host-level live
    // region announces only the run-status summary — no keystroke chatter.
    this.setAttribute("aria-live", "polite");
    // Deferred fetch (KTD3): the page drives fetchRuns()/hide() after auth
    // resolves. `start-hidden` (set synchronously for a ?public=true deep link)
    // skips the first-paint skeleton on the public path entirely.
    if (this.hasAttribute("start-hidden")) {
      this._status = "hidden";
    }
  }

  /**
   * Fetch the most-recent run window, classify it, and render the resolved
   * state. Called by the page once auth resolves on the authenticated My view.
   * Accepts both the PaginationResponse envelope (`{ data, recordsTotal }`)
   * and a plain array (MSW stories), matching `cts-dashboard`'s dual-shape
   * handling. Fail-soft: any non-OK response or network error lands in the
   * degraded "error" state (R20) — it never throws and never implies all-clear.
   * @returns {Promise<void>} Resolves once the fetch settles (success or failure).
   */
  async fetchRuns() {
    // Snapshot this call's generation. A newer fetchRuns()/hide() bumps
    // _fetchSeq, after which every guarded write below becomes a no-op, so a
    // stale in-flight response cannot clobber a more recent intent.
    const seq = ++this._fetchSeq;
    this._status = "loading";
    try {
      const response = await fetch("/api/log?start=0&length=1000");
      if (seq !== this._fetchSeq) return;
      if (!response.ok) {
        console.warn(`[cts-run-status-strip] /api/log responded ${response.status}`);
        this._status = "error";
        return;
      }
      const payload = await response.json();
      if (seq !== this._fetchSeq) return;
      // Accept the PaginationResponse envelope or a plain array. A 200 whose
      // body is neither (a contract violation: error envelope, renamed key,
      // HTML-as-200) is a data failure, not "zero runs" — surface the degraded
      // state (R20) rather than silently hiding the strip, which would read as
      // "nothing to see here".
      const data = Array.isArray(payload)
        ? payload
        : Array.isArray(payload?.data)
          ? payload.data
          : null;
      if (data === null) {
        console.warn("[cts-run-status-strip] /api/log returned an unexpected body shape");
        this._status = "error";
        return;
      }
      const { inProgressCount, failingCount } = classifyRuns(data);
      this._inProgressCount = inProgressCount;
      this._failingCount = failingCount;
      this._totalRuns = data.length;
      this._status = "ready";
    } catch (err) {
      if (seq !== this._fetchSeq) return;
      console.warn("[cts-run-status-strip] /api/log fetch failed:", err);
      this._status = "error";
    }
  }

  /**
   * Collapse the strip to nothing. Called by the page for the anonymous path
   * and whenever the Published view is active — the strip is a personal-home
   * signal, out of place on the public results browser. Bumps the fetch
   * generation so an in-flight `fetchRuns()` cannot resurrect the strip after
   * the user has navigated away (R9).
   * @returns {void}
   */
  hide() {
    this._fetchSeq++;
    this._status = "hidden";
  }

  render() {
    switch (this._status) {
      case "hidden":
        return nothing;
      case "loading":
        return this._renderSkeleton();
      case "error":
        return this._renderError();
      case "ready":
      default:
        // AE1b: a zero-runs account never shows the strip.
        if (this._totalRuns === 0) {
          return nothing;
        }
        // AE1/R8: has runs, but none actionable → confirmation, no count.
        if (this._inProgressCount === 0 && this._failingCount === 0) {
          return this._renderAllCaughtUp();
        }
        // AE2: only the non-zero counts render, each as a link.
        return this._renderActionable();
    }
  }

  /** @returns {import('lit').TemplateResult} The first-paint shimmer skeleton. */
  _renderSkeleton() {
    return html`<div class="runStrip runStrip--loading">
      <span class="runStrip-skeleton" aria-hidden="true"></span>
    </div>`;
  }

  /** @returns {import('lit').TemplateResult} The degraded "couldn't load" line (R20). */
  _renderError() {
    return html`<div class="runStrip runStrip--error">
      <cts-icon name="circle-warning" size="20" aria-hidden="true"></cts-icon>
      <span>Couldn't load run status</span>
    </div>`;
  }

  /** @returns {import('lit').TemplateResult} The all-clear confirmation (no count). */
  _renderAllCaughtUp() {
    return html`<div class="runStrip runStrip--clear">
      <cts-icon name="circle-check" size="20" aria-hidden="true"></cts-icon>
      <span>You're all caught up</span>
    </div>`;
  }

  /** @returns {import('lit').TemplateResult} The actionable counts, non-zero only. */
  _renderActionable() {
    return html`<div class="runStrip runStrip--actionable">
      ${this._inProgressCount > 0
        ? html`<a class="runStrip-link" href="logs.html?${IN_PROGRESS_LOGS_QUERY}">
            <cts-badge variant="info" count="${this._inProgressCount}" interactive></cts-badge>
            <span>in progress</span>
          </a>`
        : nothing}
      ${this._failingCount > 0
        ? html`<a class="runStrip-link" href="logs.html?${FAILING_LOGS_QUERY}">
            <cts-badge variant="fail" count="${this._failingCount}" interactive></cts-badge>
            <span>failing</span>
          </a>`
        : nothing}
    </div>`;
  }
}

customElements.define("cts-run-status-strip", CtsRunStatusStrip);

export {};
