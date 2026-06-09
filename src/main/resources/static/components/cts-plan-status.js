import { LitElement, html, nothing, css } from "lit";
import { repeat } from "lit/directives/repeat.js";
import { classMap } from "lit/directives/class-map.js";
import { ifDefined } from "lit/directives/if-defined.js";
import "./cts-tooltip.js";
import { segmentVariant, moduleMatchesResultFilter } from "../js/module-status.js";

const STYLE_ID = "cts-plan-status-styles";

const VALID_MODES = new Set(["overview", "detail", "log"]);

// segmentVariant() result -> the segment's status-fill modifier class.
// Explicit lookup table per components/AGENTS.md §7 (no dynamic class
// concatenation); an unknown variant falls back to the neutral skip box.
const SEGMENT_VARIANT_CLASS = {
  pass: "cts-pst-seg--pass",
  fail: "cts-pst-seg--fail",
  warn: "cts-pst-seg--warn",
  running: "cts-pst-seg--running",
  skip: "cts-pst-seg--skip",
  review: "cts-pst-seg--review",
  pending: "cts-pst-seg--pending",
};

// segmentVariant() result -> the accessible status word baked into each
// segment's accessible name (and tooltip), so a non-visual agent/AT gets the
// outcome the fill colour conveys. Mirrors STATUS_BOX_LABELS in cts-plan-list:
// `skip` is the settled "no result", `pending` the in-flight fetch.
const SEGMENT_STATUS_WORD = {
  pass: "passed",
  fail: "failed",
  warn: "warning",
  running: "running",
  review: "review",
  skip: "no result",
  pending: "checking status",
};

// Count-summary word per variant, in display order (R4). Only non-zero
// categories render; the order is fixed so the summary reads consistently.
const SUMMARY_PARTS = [
  ["pass", "passed"],
  ["fail", "failed"],
  ["warn", "warning"],
  ["review", "review"],
  ["running", "running"],
  ["pending", "checking"],
  ["skip", "not run"],
];

// Scoped CSS. KTD2: the host is the size container and a single per-mode
// property set (plus one @container branch for the bar↔grid switch) drives
// layout — the bar and grid are the same flex row, not two layouts.
const STYLE_TEXT = css`
  /* Custom elements default to display:inline, which cannot establish size
     containment, so @container would silently never match. Promote the host
     to a block-level inline-size container (KTD2). */
  cts-plan-status {
    display: block;
    container-type: inline-size;
    font-family: var(--font-sans);
  }

  .cts-pst-track {
    display: flex;
    align-items: center;
    margin: 0;
    padding: 0;
  }

  /* The segment is a colour-only rectangle. The base reset is harmless on a
     <span> (overview) and neutralises the default chrome on a <button>
     (detail/log) so both render identically. Never named .logItem — the
     global ".logItem:hover" repaint would bleed through (auto-memory
     feedback_layout_css_logitem_hover). */
  .cts-pst-seg {
    box-sizing: border-box;
    display: block;
    margin: 0;
    padding: 0;
    border: none;
    border-radius: var(--radius-1);
    background: var(--ink-300);
    appearance: none;
    -webkit-appearance: none;
    font: inherit;
    color: inherit;
  }

  /* Status fills mirror cts-plan-list's moduleStatusBox--* tokens so the two
     surfaces stay in lockstep. Review is now the saturated --status-review
     token added in U1. */
  .cts-pst-seg--pass {
    background: var(--status-pass);
  }
  .cts-pst-seg--fail {
    background: var(--status-fail);
  }
  .cts-pst-seg--warn {
    background: var(--status-warning);
  }
  .cts-pst-seg--running {
    background: var(--status-running);
  }
  .cts-pst-seg--review {
    background: var(--status-review);
  }
  /* Settled not-run / unresolved uses the lighter neutral so "nothing to
     report" recedes; pending uses the darker neutral and pulses. */
  .cts-pst-seg--skip {
    background: var(--ink-300);
  }
  .cts-pst-seg--pending {
    background: var(--status-skipped);
  }
  @media (prefers-reduced-motion: no-preference) {
    .cts-pst-seg--pending {
      animation: cts-pst-status-pulse 1.2s ease-in-out 10;
    }
  }
  @keyframes cts-pst-status-pulse {
    0%,
    100% {
      opacity: 1;
    }
    50% {
      opacity: 0.35;
    }
  }

  /* "You are here" marker (R14/R17): a 2px inset ring OVERLAID on the status
     fill (it does not replace the fill colour). Inset box-shadow keeps the box
     model identical so toggling the marker causes no reflow. */
  .cts-pst-seg.is-current {
    box-shadow: inset 0 0 0 2px var(--fg);
  }

  /* Dimming while a result filter is active (R10): non-matching (and
     still-pending, R18) segments recede; matching ones keep full colour. The
     dim also stops the pending pulse so a dimmed pending segment does not
     fight the dim opacity — this rule sits after the @media pulse rule so it
     wins on source order at equal specificity. */
  .cts-pst-seg.is-dimmed {
    opacity: 0.25;
    animation: none;
  }

  /* Interactive segments (detail/log) carry the pointer affordance and an
     OUTWARD focus outline (Open Question candidate) so it never collides with
     the inset is-current ring. A readonly log surface (Decision 1) renders
     <span> segments with no click action, so it is excluded from the pointer
     cue via :not([readonly]). */
  cts-plan-status[mode="detail"]:not([readonly]) .cts-pst-seg,
  cts-plan-status[mode="log"]:not([readonly]) .cts-pst-seg {
    cursor: pointer;
  }
  .cts-pst-seg:focus-visible {
    outline: 2px solid var(--orange-400);
    outline-offset: 2px;
  }

  /* --- overview: the legacy moduleStatusGrid look, unchanged (R12). Fixed
     32x18 non-growing boxes that wrap; the host rides above the card block-link
     ::after overlay (z-index:1) but stays pointer-events:none so only the
     tooltip-wrapped boxes catch hover and every other pixel falls through to
     the card link (KTD6). --- */
  cts-plan-status[mode="overview"] {
    position: relative;
    z-index: 1;
    pointer-events: none;
  }
  cts-plan-status[mode="overview"] cts-tooltip {
    pointer-events: auto;
  }
  cts-plan-status[mode="overview"] .cts-pst-track {
    flex-wrap: wrap;
    gap: var(--space-1);
  }
  cts-plan-status[mode="overview"] .cts-pst-seg {
    flex: 0 0 auto;
    width: 32px;
    height: 18px;
  }

  /* --- detail & log: the responsive bar. Wide containers get a single row of
     segments that shrink to hairlines (flex:1 1 0; min-width:0 — R7); the
     @container branch below flips to the wrapping tappable grid on narrow
     containers (R6/AE3). --- */
  cts-plan-status[mode="detail"] .cts-pst-track,
  cts-plan-status[mode="log"] .cts-pst-track {
    flex-wrap: nowrap;
    gap: 2px;
  }
  cts-plan-status[mode="detail"] .cts-pst-seg,
  cts-plan-status[mode="log"] .cts-pst-seg {
    flex: 1 1 0;
    min-width: 0;
    height: 14px;
  }
  @container (max-width: 520px) {
    cts-plan-status[mode="detail"] .cts-pst-track,
    cts-plan-status[mode="log"] .cts-pst-track {
      flex-wrap: wrap;
      column-gap: var(--space-1);
      /* Zero row-gap when wrapping (KTD2): wrapped rows sit flush so the grid
         reads as one mosaic rather than spaced bands. */
      row-gap: 0;
    }
    cts-plan-status[mode="detail"] .cts-pst-seg,
    cts-plan-status[mode="log"] .cts-pst-seg {
      flex: 1 1 40px;
      min-width: 40px;
      min-height: 24px;
    }
  }

  /* The count summary (R4, detail) / position label (R14, log) — supplementary
     line under the bar. The summary is the polite live region that announces
     the settled tally; per-segment name changes stay silent (they are not in a
     live region). */
  .cts-pst-meta {
    margin: var(--space-2) 0 0;
    font-size: var(--fs-12);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
    font-variant-numeric: tabular-nums;
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Stable identity key for a module entry, unique across lists so the keyed
 * `repeat()` never reuses a segment's DOM across a full module-set swap
 * (mirrors cts-plan-modules._moduleKey). Index keys would collide here.
 * @param {{testModule?: string, variant?: object}} mod - A plan module entry.
 * @returns {string} Content-derived key (testModule plus serialized variant).
 */
function moduleKey(mod) {
  return `${mod.testModule}|${JSON.stringify(mod.variant ?? null)}`;
}

/**
 * Format a module's variant object into a compact `key=value, …` string.
 * Mirrors the variant formatting in cts-plan-modules / cts-plan-list.
 * @param {object|null|undefined} variant - The module variant map.
 * @returns {string} Formatted variant, or "" when there is no variant.
 */
function formatVariant(variant) {
  if (!variant || typeof variant !== "object") return "";
  return Object.entries(variant)
    .map(([key, value]) => `${key}=${value}`)
    .join(", ");
}

/**
 * Presentational, data-fed plan-status component (KTD1): one colour-coded
 * segment per test module in the canonical status palette, rendered as a
 * compact completion bar (wide) or a wrapping grid of tappable rectangles
 * (narrow), shared across three surfaces. It NEVER self-fetches — each page
 * resolves per-module status and feeds the modules in (R5).
 *
 * A surface `mode` gates layout, affordance, and the supplementary label:
 *   - `overview` (plans.html): the legacy fixed-box grid, read-only. Segments
 *     are not click targets; the host uses the block-link pointer-events
 *     pattern so clicks fall through to the surrounding card link (R12/R16).
 *   - `detail` (plan-detail.html): the responsive bar; segments are buttons
 *     that emit `cts-plan-status-activate` (the page scrolls to the row); a
 *     polite count summary announces the tally (R4); `activeResultFilter`
 *     dims non-matching segments (R10).
 *   - `log` (log-detail.html): the responsive bar; segments are buttons that
 *     emit `cts-plan-status-activate` (the page opens that instance's log); the
 *     segment whose module ran `currentInstanceId` carries the "you are here"
 *     marker, and a "Module N of M" label is shown (R14/R15/R17).
 *
 * Light DOM (`createRenderRoot` returns `this`). Scoped CSS is injected once on
 * first connect. The host is a `container-type: inline-size` block so the
 * bar↔grid switch tracks the host's own width in any embedding context (KTD2).
 * @property {Array<object>} modules - Plan modules in plan order, each shaped
 *   `{ testModule, variant?, instances?, status?, result?, _statusResolved? }`.
 *   The component reads resolved status via the shared `segmentVariant` helper
 *   and initiates no fetches of its own. An empty array renders nothing.
 * @property {string} mode - Surface mode: `overview` (default), `detail`, or
 *   `log`. Reflected to the `mode` attribute so the scoped CSS can branch on
 *   it; an unknown value falls back to `overview`.
 * @property {string} currentInstanceId - In `log` mode, the instance currently
 *   being viewed. The segment whose module's `instances` array includes this id
 *   gets the "you are here" marker and drives the "Module N of M" label (R17).
 *   Reflects the `current-instance-id` attribute.
 * @property {Set<string>|null} activeResultFilter - In `detail` mode, the
 *   active "Filter by result" selection (result tokens plus the
 *   `NOT_RUN_FILTER_VALUE` sentinel). Non-matching and still-pending segments
 *   are dimmed; the count summary ignores it (R4/R10/R18). Set via JS only.
 * @property {boolean} readonly - In `log` mode, downgrade segments to the
 *   non-interactive read-only form (a `<span role="img">`, no `<button>`, no
 *   activate emission, no pointer affordance) for the public / readonly log
 *   view. The "you are here" marker, the "Module N of M" label, and dimming
 *   still render — only the click action is suppressed (a UX affordance, not
 *   an access boundary; the backend `/api/info` gating is the real boundary).
 *   Reflects the `readonly` attribute. Default false.
 * @fires cts-plan-status-activate - When an interactive (`detail`/`log`)
 *   segment is clicked or keyboard-activated, with
 *   `{ index, module, instanceId, dimmed }` where `instanceId` is the module's
 *   most-recent instance (R17) and `dimmed` is whether an active result filter
 *   currently dims the segment (so a coordinator can clear-then-scroll, R11).
 *   Bubbles and is composed.
 */
class CtsPlanStatus extends LitElement {
  static properties = {
    modules: { type: Array },
    mode: { type: String, reflect: true },
    currentInstanceId: { type: String, attribute: "current-instance-id" },
    activeResultFilter: { attribute: false },
    readonly: { type: Boolean, reflect: true },
  };

  constructor() {
    super();
    this.modules = [];
    this.mode = "overview";
    this.currentInstanceId = "";
    this.activeResultFilter = null;
    this.readonly = false;
    this._onActivate = this._onActivate.bind(this);
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  willUpdate(changed) {
    // Normalise an unknown mode so the attribute-keyed CSS always matches a
    // known layout rather than rendering an unstyled flex row.
    if (changed.has("mode") && !VALID_MODES.has(this.mode)) {
      this.mode = "overview";
    }
  }

  /**
   * Index of the segment whose module ran the currently-viewed instance, found
   * by matching `currentInstanceId` against each module's FULL instance list
   * (not just the last) so viewing an older re-run still highlights the right
   * segment (R17).
   * @param {object[]} modules - The plan modules.
   * @returns {number} The matching index, or -1 when there is no match.
   */
  _currentIndex(modules) {
    if (!this.currentInstanceId) return -1;
    return modules.findIndex(
      (m) => Array.isArray(m.instances) && m.instances.includes(this.currentInstanceId),
    );
  }

  /**
   * Tally modules by status variant across the WHOLE plan (the summary always
   * reflects total counts, never the filtered subset — R4).
   * @param {object[]} modules - The plan modules.
   * @returns {{ [variant: string]: number }} Count per `segmentVariant` result.
   */
  _counts(modules) {
    const counts = { pass: 0, fail: 0, warn: 0, running: 0, review: 0, skip: 0, pending: 0 };
    for (const mod of modules) counts[segmentVariant(mod)] += 1;
    return counts;
  }

  /**
   * The count-summary string (e.g. "18 passed · 3 failed · 7 not run"), built
   * from non-zero categories in fixed order.
   * @param {object[]} modules - The plan modules.
   * @returns {string} The summary text.
   */
  _summaryText(modules) {
    const counts = this._counts(modules);
    return SUMMARY_PARTS.filter(([key]) => counts[key] > 0)
      .map(([key, word]) => `${counts[key]} ${word}`)
      .join(" · ");
  }

  /**
   * Display name for a module, including its variant (R3).
   * @param {object} mod - A plan module entry.
   * @returns {string} `testModule` plus a parenthesised variant when present.
   */
  _moduleName(mod) {
    const variant = formatVariant(mod.variant);
    return variant ? `${mod.testModule} (${variant})` : mod.testModule;
  }

  _onActivate(event) {
    const idx = Number(event.currentTarget.dataset.index);
    const modules = Array.isArray(this.modules) ? this.modules : [];
    const mod = modules[idx];
    if (!mod) return;
    const instances = Array.isArray(mod.instances) ? mod.instances : [];
    const instanceId = instances.length ? instances[instances.length - 1] : null;
    // Whether this segment is currently dimmed by an active result filter, so a
    // page coordinator can implement "clear the filter, then scroll" (R11)
    // without re-deriving the match (the same `moduleMatchesResultFilter` source
    // of truth the dimming render uses).
    const dimmed = !moduleMatchesResultFilter(mod, this.activeResultFilter);
    this.dispatchEvent(
      new CustomEvent("cts-plan-status-activate", {
        bubbles: true,
        composed: true,
        detail: { index: idx, module: mod, instanceId, dimmed },
      }),
    );
  }

  /**
   * Render one segment, wrapped in a tooltip naming the module + status (R3).
   * Interactive modes render a `<button>` (native keyboard + click); overview
   * renders a read-only `<span role="img">` whose accessible name carries the
   * same module + status (matching the legacy boxes).
   * @param {object} mod - The plan module entry.
   * @param {number} index - The module's index in plan order.
   * @param {number} currentIndex - The "you are here" index (log mode) or -1.
   * @param {boolean} interactive - Whether segments are click targets.
   * @returns {import('lit').TemplateResult} The wrapped segment.
   */
  _renderSegment(mod, index, currentIndex, interactive) {
    const variant = segmentVariant(mod);
    const word = SEGMENT_STATUS_WORD[variant] || SEGMENT_STATUS_WORD.skip;
    const name = this._moduleName(mod);
    const isCurrent = index === currentIndex;
    const isDimmed = !moduleMatchesResultFilter(mod, this.activeResultFilter);
    const segClasses = {
      "cts-pst-seg": true,
      "is-current": isCurrent,
      "is-dimmed": isDimmed,
    };
    segClasses[SEGMENT_VARIANT_CLASS[variant] || SEGMENT_VARIANT_CLASS.skip] = true;
    const ariaName = `${name}: ${word}`;

    const segment = interactive
      ? html`<button
          type="button"
          class=${classMap(segClasses)}
          data-index=${index}
          data-testid="plan-status-segment"
          aria-label=${ariaName}
          aria-current=${ifDefined(isCurrent ? "true" : undefined)}
          @click=${this._onActivate}
        ></button>`
      : html`<span
          class=${classMap(segClasses)}
          data-testid="plan-status-segment"
          role="img"
          aria-label=${ariaName}
        ></span>`;

    return html`<cts-tooltip content="${name} — ${word}" placement="top">${segment}</cts-tooltip>`;
  }

  render() {
    const modules = Array.isArray(this.modules) ? this.modules : [];
    // Zero modules → render nothing (hides itself, matching the empty
    // moduleStatusGrid today).
    if (modules.length === 0) return nothing;

    const mode = VALID_MODES.has(this.mode) ? this.mode : "overview";
    // `readonly` downgrades the otherwise-interactive detail/log modes to the
    // non-clickable span form (Decision 1): the public/readonly log view shows
    // the marker, label, and dimming but suppresses sibling navigation. The
    // backend /api/info gating, not this flag, is the real access boundary.
    const interactive = (mode === "detail" || mode === "log") && !this.readonly;
    const currentIndex = mode === "log" ? this._currentIndex(modules) : -1;

    const track = html`<div class="cts-pst-track" data-testid="plan-status-track">
      ${repeat(
        modules,
        (mod) => moduleKey(mod),
        (mod, index) => this._renderSegment(mod, index, currentIndex, interactive),
      )}
    </div>`;

    /** @type {import('lit').TemplateResult | typeof nothing} */
    let meta = nothing;
    if (mode === "detail") {
      // Polite live region: announces the settled tally once segments resolve.
      meta = html`<p class="cts-pst-meta" aria-live="polite" data-testid="plan-status-summary">
        ${this._summaryText(modules)}
      </p>`;
    } else if (mode === "log" && currentIndex >= 0) {
      meta = html`<p class="cts-pst-meta" data-testid="plan-status-position">
        Module ${currentIndex + 1} of ${modules.length}
      </p>`;
    }

    return html`${track}${meta}`;
  }
}

customElements.define("cts-plan-status", CtsPlanStatus);

export {};
