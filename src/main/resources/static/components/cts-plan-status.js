import { LitElement, html, nothing, css } from "lit";
import { repeat } from "lit/directives/repeat.js";
import { classMap } from "lit/directives/class-map.js";
import { ifDefined } from "lit/directives/if-defined.js";
import "./cts-tooltip.js";
import "./cts-badge.js";
import {
  segmentVariant,
  moduleMatchesResultFilter,
  moduleKey,
  currentModuleIndex,
  moduleRowId,
  NOT_RUN_FILTER_VALUE,
} from "../js/module-status.js";

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

// Detail-mode count badges (R9 redesign): the read-only count summary and the
// "Filter by result" control are merged into one row of count pills, mirroring
// log-detail's logResultSummary. Each entry is one badge in fixed display
// order; only non-zero categories render. `filterable` entries are interactive
// multi-select toggles whose `key` is the result token fed into the page's
// activeResultFilter Set (SKIPPED and NOT_RUN are distinct tokens — see
// moduleMatchesResultFilter); `running`/`pending` are display-only because they
// are transient states with no filter token. Counts for filterable categories
// use moduleMatchesResultFilter so a badge's count always equals the rows the
// filter would show; the two transient ones count by segmentVariant.
const RESULT_BADGES = [
  { key: "PASSED", variant: "pass", label: "Passed", filterable: true },
  { key: "FAILED", variant: "fail", label: "Failed", filterable: true },
  { key: "WARNING", variant: "warn", label: "Warning", filterable: true },
  { key: "REVIEW", variant: "review", label: "Review", filterable: true },
  { key: "running", variant: "running", label: "Running", filterable: false },
  { key: "pending", variant: "skip", label: "Checking", filterable: false },
  { key: "SKIPPED", variant: "skip", label: "Skipped", filterable: true },
  { key: NOT_RUN_FILTER_VALUE, variant: "skip", label: "Not run", filterable: true },
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
    /* The fill is routed through --cts-seg-fill (set per variant below) so the
       "you are here" marker can paint its ring in the segment's own status
       colour without re-deriving it. */
    background: var(--cts-seg-fill, var(--ink-300));
    appearance: none;
    -webkit-appearance: none;
    font: inherit;
    color: inherit;
    /* detail segments render as <a> (deep-link to the module row); kill the
       default anchor underline — harmless on <span>/<button>. */
    text-decoration: none;
  }

  /* Status fills mirror cts-plan-list's moduleStatusBox--* tokens so the two
     surfaces stay in lockstep. Each variant sets --cts-seg-fill (consumed by
     the base background rule AND the is-current marker ring). Review is the
     saturated --status-review token added in U1. */
  .cts-pst-seg--pass {
    --cts-seg-fill: var(--status-pass);
  }
  .cts-pst-seg--fail {
    --cts-seg-fill: var(--status-fail);
  }
  .cts-pst-seg--warn {
    --cts-seg-fill: var(--status-warning);
  }
  .cts-pst-seg--running {
    --cts-seg-fill: var(--status-running);
  }
  .cts-pst-seg--review {
    --cts-seg-fill: var(--status-review);
  }
  /* Settled not-run / unresolved uses the lighter neutral so "nothing to
     report" recedes; pending uses the darker neutral and pulses. */
  .cts-pst-seg--skip {
    --cts-seg-fill: var(--ink-300);
  }
  .cts-pst-seg--pending {
    --cts-seg-fill: var(--status-skipped);
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

  /* "You are here" marker (R14/R17): a 2px inset ring in the segment's OWN
     status colour, then a 3px inset --bg ring so a thin background gap separates
     the ring from the fill (reads as a haloed/selected segment). Both are inset
     box-shadows, so the box model is unchanged and toggling causes no reflow. */
  .cts-pst-seg.is-current {
    box-shadow:
      inset 0 0 0 2px var(--cts-seg-fill, var(--fg)),
      inset 0 0 0 3px var(--bg);
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

  /* Interactive segments carry the pointer affordance and an OUTWARD focus
     outline (Open Question candidate) so it never collides with the inset
     is-current ring. Only the interactive element forms get the cue: detail
     anchors and log buttons. Read-only span segments (a hard readonly surface,
     or a public-view sibling that is not navigable) are never click targets, so
     keying off the element type keeps the pointer off them without a host-level
     :not([readonly]) guard. */
  cts-plan-status[mode="detail"] a.cts-pst-seg,
  cts-plan-status[mode="log"] button.cts-pst-seg {
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
      /* Equal row and column gaps so the wrapped tiles read as an even grid. */
      column-gap: var(--space-1);
      row-gap: var(--space-1);
    }
    cts-plan-status[mode="detail"] .cts-pst-seg,
    cts-plan-status[mode="log"] .cts-pst-seg {
      /* Don't grow past the 40px tap-target basis — keep tiles a consistent
         size rather than stretching the last row's items. */
      flex: 0 1 40px;
      min-width: 40px;
      min-height: 24px;
    }
  }

  /* The "Module N of M" position label (R14, log) — a supplementary line under
     the bar. (Detail mode's old text summary is now the count-badge row below.) */
  .cts-pst-meta {
    margin: var(--space-2) 0 0;
    font-size: var(--fs-12);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
    font-variant-numeric: tabular-nums;
  }

  /* Detail-mode count-badge row (R9): the merged summary + result filter. One
     wrapping row of count pills under the bar; the clear button trails them.
     When a filter is active (.is-filtering) the non-pressed filterable badges
     recede so the active selection stands out — mirroring logResultSummary. */
  .cts-pst-filter {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    margin: var(--space-3) 0 0;
  }
  /* De-emphasize the inactive (unpressed) filterable badges while a filter is
     active, so the active ones stand out — opacity matches logResultSummary. */
  .cts-pst-filter.is-filtering cts-badge[clickable]:not([pressed]) {
    opacity: 0.6;
  }
  /* "Clear filters" affordance — styled identically to cts-log-viewer's
     .logFilterClear so the two filter surfaces match exactly. */
  .cts-pst-filter-clear {
    align-self: center;
    font: inherit;
    font-size: var(--fs-12);
    line-height: 16px;
    color: var(--fg-muted);
    background: transparent;
    border: 0;
    padding: 2px var(--space-1);
    cursor: pointer;
    text-decoration: underline;
    text-underline-offset: 2px;
  }
  .cts-pst-filter-clear:hover {
    color: var(--fg);
  }
  .cts-pst-filter-clear:focus-visible {
    outline: 2px solid var(--rust-400, #c75a3f);
    outline-offset: 2px;
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
 *   - `detail` (plan-detail.html): the responsive bar; segments are in-page
 *     anchors (`href="#cts-module-N"`) that deep-link to the module's row — the
 *     URL hash updates and the row gets a `:target` highlight + flash. A dimmed
 *     (filtered-out) segment cancels the native jump and emits
 *     `cts-plan-status-activate` so the page clears the filter first, then
 *     navigates (R11). Below the bar a row of count badges merges the tally and
 *     the "Filter by result" control (R9) — clicking a filterable badge emits
 *     `cts-plan-status-filter` and `activeResultFilter` dims non-matching
 *     segments (R10).
 *   - `log` (log-detail.html): the responsive bar; segments are buttons that
 *     emit `cts-plan-status-activate` (the page opens that instance's log); the
 *     segment whose module ran `currentInstanceId` carries the "you are here"
 *     marker, and a "Module N of M" label is shown (R14/R15/R17).
 *
 * Light DOM (`createRenderRoot` returns `this`). Scoped CSS is injected once on
 * first connect. The host is a `container-type: inline-size` block so the
 * bar↔grid switch tracks the host's own width in any embedding context (KTD2).
 * @property {Array<object>} modules - Plan modules in plan order, each shaped
 *   `{ testModule, variant?, instances?, status?, result?, _statusResolved?,
 *   navigable? }`. The component reads resolved status via the shared
 *   `segmentVariant` helper and initiates no fetches of its own. An empty array
 *   renders nothing. In `log` mode on a public view (`publicView`), only modules
 *   the page has flagged `navigable: true` (its `/api/info` fan-out confirmed the
 *   target instance is publicly reachable) are click targets; off public the flag
 *   is ignored and every segment navigates.
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
 *   are dimmed (R10/R18) and the matching count badges render pressed; the badge
 *   counts themselves stay totals, not the filtered subset. Set via JS only —
 *   the page coordinator owns it and pushes it back after a badge toggle.
 * @property {boolean} readonly - A hard "force every segment non-interactive"
 *   override: in `log`/`detail` mode all segments render as the read-only
 *   `<span role="img">` form (no `<button>`/`<a>`, no activate emission, no
 *   pointer affordance) while the "you are here" marker, the "Module N of M"
 *   label, and dimming still render; when `readonly` and `publicView` are both
 *   set, `readonly` wins (every segment is a span). Public log views no longer
 *   use this to suppress navigation — that is decided per-segment via
 *   `publicView` plus each
 *   module's `navigable` flag (a UX affordance, not an access boundary; the
 *   backend `/api/info` gating is the real boundary). Reflects the `readonly`
 *   attribute. Default false.
 * @property {boolean} publicView - In `log` mode, marks the public view so
 *   navigation is gated per-segment: only modules flagged `navigable: true` (the
 *   page's `/api/info` fan-out confirmed the target instance is publicly
 *   reachable) render as click targets; the rest stay read-only spans. No effect
 *   off public (every segment navigates) or in `overview`/`detail`. Reflects the
 *   `public-view` attribute. Default false.
 * @property {boolean} hideLabel - In `log` mode, suppress the built-in
 *   "Module N of M" position label so the host can render and place it itself
 *   (cts-test-nav-controls sets this to lay the label on its own row, below the
 *   bar+button line, so the bar and the Continue button stay vertically centred
 *   as siblings rather than the button centring against the taller bar+label
 *   block). No effect in `overview`/`detail`. Reflects the `hide-label`
 *   attribute. Default false.
 * @fires cts-plan-status-activate - When an interactive (`detail`/`log`)
 *   segment is clicked or keyboard-activated, with
 *   `{ index, module, instanceId, dimmed }` where `instanceId` is the module's
 *   most-recent instance (R17) and `dimmed` is whether an active result filter
 *   currently dims the segment (so a coordinator can clear-then-scroll, R11).
 *   Bubbles and is composed.
 * @fires cts-plan-status-filter - In `detail` mode, when a count badge is
 *   toggled (`{ value }`, the result token) or the "Clear filters" button is
 *   pressed (`{ clear: true }`). The page coordinator updates its
 *   `activeResultFilter` Set and pushes it back down (pressed state + dimming)
 *   and across to cts-plan-modules (row narrowing). Bubbles and is composed.
 */
class CtsPlanStatus extends LitElement {
  static properties = {
    modules: { type: Array, attribute: false },
    mode: { type: String, reflect: true },
    currentInstanceId: { type: String, attribute: "current-instance-id" },
    activeResultFilter: { attribute: false },
    readonly: { type: Boolean, reflect: true },
    publicView: { type: Boolean, attribute: "public-view", reflect: true },
    hideLabel: { type: Boolean, attribute: "hide-label", reflect: true },
  };

  constructor() {
    super();
    this.modules = [];
    this.mode = "overview";
    this.currentInstanceId = "";
    this.activeResultFilter = null;
    this.readonly = false;
    this.publicView = false;
    this.hideLabel = false;
    this._onActivate = this._onActivate.bind(this);
    this._onFilterBadgeClick = this._onFilterBadgeClick.bind(this);
    this._onClearFilter = this._onClearFilter.bind(this);
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
    return currentModuleIndex(modules, this.currentInstanceId);
  }

  /**
   * Tally modules per count-badge category across the WHOLE plan (counts are
   * always totals, never the filtered subset). Filterable categories count via
   * `moduleMatchesResultFilter` so a badge's count equals the rows the filter
   * would show; the transient `running`/`pending` categories count by
   * `segmentVariant`.
   * @param {object[]} modules - The plan modules.
   * @returns {{ [key: string]: number }} Count per `RESULT_BADGES` key.
   */
  _filterCounts(modules) {
    /** @type {{ [key: string]: number }} */
    const counts = {};
    for (const badge of RESULT_BADGES) {
      counts[badge.key] = badge.filterable
        ? modules.filter((mod) => moduleMatchesResultFilter(mod, new Set([badge.key]))).length
        : modules.filter((mod) => segmentVariant(mod) === badge.key).length;
    }
    return counts;
  }

  /**
   * The detail-mode count-badge row (R9): the merged summary + "Filter by
   * result" control. Renders one `cts-badge` per non-zero category; filterable
   * categories are multi-select toggles (pressed = membership in
   * `activeResultFilter`) that emit `cts-plan-status-filter`, and a "Clear
   * filters" button appears while a filter is active. Readonly downgrades every
   * badge to a non-interactive label (public view shows the tally, not a
   * control).
   * @param {object[]} modules - The plan modules.
   * @returns {import('lit').TemplateResult} The badge row.
   */
  _renderFilterBadges(modules) {
    const counts = this._filterCounts(modules);
    const filter = this.activeResultFilter;
    const filtering = filter instanceof Set && filter.size > 0;
    const interactive = !this.readonly;
    const visible = RESULT_BADGES.filter((badge) => counts[badge.key] > 0);
    return html`<div
      class=${classMap({ "cts-pst-filter": true, "is-filtering": filtering && interactive })}
      role="group"
      aria-label="Filter modules by result"
      data-testid="plan-status-filter"
    >
      ${repeat(
        visible,
        (badge) => badge.key,
        (badge) => this._renderFilterBadge(badge, counts[badge.key], filter, interactive),
      )}
      ${filtering && interactive
        ? html`<button
            type="button"
            class="cts-pst-filter-clear"
            data-testid="plan-status-filter-clear"
            @click=${this._onClearFilter}
          >
            Clear filters
          </button>`
        : nothing}
    </div>`;
  }

  /**
   * Render one count badge. Filterable categories on an interactive surface are
   * `clickable` + `?pressed` multi-select toggles; everything else is a
   * read-only label.
   * @param {{key: string, variant: string, label: string, filterable: boolean}} badge
   *   - The `RESULT_BADGES` entry.
   * @param {number} count - The category's module count.
   * @param {Set<string>|null} filter - The active result filter.
   * @param {boolean} interactive - Whether badges are click targets (detail,
   *   not readonly).
   * @returns {import('lit').TemplateResult} The badge.
   */
  _renderFilterBadge(badge, count, filter, interactive) {
    const label = `${badge.label} ${count}`;
    if (!badge.filterable || !interactive) {
      return html`<cts-badge variant=${badge.variant} label=${label}></cts-badge>`;
    }
    const pressed = filter instanceof Set && filter.has(badge.key);
    const ariaLabel = pressed
      ? `Stop filtering by ${badge.label}`
      : `Show only ${badge.label} modules`;
    return html`<cts-badge
      variant=${badge.variant}
      label=${label}
      aria-label=${ariaLabel}
      data-result=${badge.key}
      clickable
      ?pressed=${pressed}
      @cts-badge-click=${this._onFilterBadgeClick}
    ></cts-badge>`;
  }

  _onFilterBadgeClick(event) {
    const value = event.currentTarget && event.currentTarget.dataset.result;
    if (!value) return;
    // The page coordinator owns the activeResultFilter Set; this just reports
    // which token was toggled (it pushes the new Set back down for pressed state
    // + segment dimming, and across to cts-plan-modules for row narrowing).
    this.dispatchEvent(
      new CustomEvent("cts-plan-status-filter", {
        bubbles: true,
        composed: true,
        detail: { value },
      }),
    );
  }

  _onClearFilter() {
    this.dispatchEvent(
      new CustomEvent("cts-plan-status-filter", {
        bubbles: true,
        composed: true,
        detail: { clear: true },
      }),
    );
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
    // In detail mode the segment is an in-page anchor to the module's row. When
    // the segment is dimmed the row is filtered OUT of the DOM, so the native
    // `#row` jump would land nowhere — cancel it and let the page coordinator
    // clear the filter first, then navigate once the row re-renders (R11). For a
    // visible (non-dimmed) detail segment the native anchor handles the jump;
    // for log mode (a <button>) there is no default to cancel.
    if (this.mode === "detail" && dimmed) {
      event.preventDefault();
    }
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
   * The element depends on mode/affordance:
   *   - `detail` interactive → an `<a href="#cts-module-N">` deep-link to the
   *     module's row (clicking sets the URL hash → the row's `:target` highlight
   *     + a flash; a dimmed segment cancels the jump so the page can clear the
   *     filter first — see `_onActivate`).
   *   - `log` interactive → a `<button>` (sibling navigation is a cross-page
   *     load coordinated by the page, which threads the public-view param).
   *   - non-interactive (overview, readonly) → a read-only `<span role="img">`.
   * All three carry the same accessible name (module + status).
   * @param {object} mod - The plan module entry.
   * @param {number} index - The module's index in plan order.
   * @param {number} currentIndex - The "you are here" index (log mode) or -1.
   * @param {boolean} interactive - Whether segments are click targets.
   * @param {string} mode - The resolved surface mode.
   * @returns {import('lit').TemplateResult} The wrapped segment.
   */
  _renderSegment(mod, index, currentIndex, interactive, mode) {
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

    // Per-segment interactivity. Detail mode uses the surface-level decision
    // (same-page anchors are always safe). Log mode additionally gates on public
    // reachability: on a public view only siblings the page flagged
    // `navigable: true` (fan-out confirmed their target instance returns 200) are
    // click targets, so a published-plan viewer navigates to reachable siblings
    // without dead-ending on an unpublished one. Off public, every log segment
    // navigates as before. A hard `readonly` (interactive=false) forces spans.
    const navigableOnPublic = !this.publicView || mod.navigable === true;
    const segInteractive = interactive && (mode !== "log" || navigableOnPublic);

    let segment;
    if (!segInteractive) {
      segment = html`<span
        class=${classMap(segClasses)}
        data-testid="plan-status-segment"
        role="img"
        aria-label=${ariaName}
      ></span>`;
    } else if (mode === "detail") {
      segment = html`<a
        class=${classMap(segClasses)}
        href="#${moduleRowId(index)}"
        data-index=${index}
        data-testid="plan-status-segment"
        aria-label=${ariaName}
        @click=${this._onActivate}
      ></a>`;
    } else {
      segment = html`<button
        type="button"
        class=${classMap(segClasses)}
        data-index=${index}
        data-testid="plan-status-segment"
        aria-label=${ariaName}
        aria-current=${ifDefined(isCurrent ? "step" : undefined)}
        @click=${this._onActivate}
      ></button>`;
    }

    return html`<cts-tooltip content="${name} — ${word}" placement="top">${segment}</cts-tooltip>`;
  }

  render() {
    const modules = Array.isArray(this.modules) ? this.modules : [];
    // Zero modules → render nothing (hides itself, matching the empty
    // moduleStatusGrid today).
    if (modules.length === 0) return nothing;

    const mode = VALID_MODES.has(this.mode) ? this.mode : "overview";
    // Surface-level interactivity for detail-mode anchors and the count-badge
    // filter row; `readonly` is the hard off-switch. In log mode this is only the
    // upper bound — `_renderSegment` further gates each segment on public
    // reachability (publicView + the module's `navigable` flag) so a public view
    // navigates to reachable siblings but not to unpublished ones. The backend
    // /api/info gating, not these flags, is the real access boundary.
    const interactive = (mode === "detail" || mode === "log") && !this.readonly;
    const currentIndex = mode === "log" ? this._currentIndex(modules) : -1;

    const track = html`<div
      class="cts-pst-track"
      data-testid="plan-status-track"
      role="group"
      aria-label="Module status, ${modules.length} modules"
    >
      ${repeat(
        modules,
        // Key by module identity AND plan-order index: a plan may list the same
        // testModule+variant twice, so moduleKey alone can collide and make the
        // keyed repeat() drop a segment and misalign the marker / activate
        // index. The index keeps the key unique; the list order is stable
        // (dimming never reorders), so DOM reuse stays positional and correct.
        (mod, index) => `${moduleKey(mod)}#${index}`,
        (mod, index) => this._renderSegment(mod, index, currentIndex, interactive, mode),
      )}
    </div>`;

    /** @type {import('lit').TemplateResult | typeof nothing} */
    let meta = nothing;
    if (mode === "detail") {
      // The merged count-summary + result filter (R9): one row of count badges,
      // the filterable ones interactive (emit cts-plan-status-filter).
      meta = this._renderFilterBadges(modules);
    } else if (mode === "log" && currentIndex >= 0 && !this.hideLabel) {
      // The "Module N of M" label is suppressed when the host opts to place it
      // itself (cts-test-nav-controls sets hide-label so it can lay the label on
      // its own row below the bar+button line, keeping the bar and button as
      // true centred siblings — see currentModuleIndex usage there).
      meta = html`<p class="cts-pst-meta" data-testid="plan-status-position">
        Module ${currentIndex + 1} of ${modules.length}
      </p>`;
    }

    return html`${track}${meta}`;
  }
}

customElements.define("cts-plan-status", CtsPlanStatus);

export {};
