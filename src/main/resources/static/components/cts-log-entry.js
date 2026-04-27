import { LitElement, html, nothing } from "lit";
import "./cts-icon.js";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-log-entry-id.js";

/**
 * Maps a log entry's `result` value (case-insensitive) to a `cts-badge`
 * variant name. Lookup table per components/AGENTS.md §7 (no dynamic class
 * concatenation).
 * @type {Object.<string, string>}
 */
const RESULT_BADGE_VARIANTS = {
  success: "pass",
  failure: "fail",
  warning: "warn",
  info: "running",
  review: "review",
  skipped: "skip",
};

/**
 * Maps a log entry's `http` value (case-insensitive) to the icon + label
 * pair shown alongside the timestamp/severity. Bootstrap Icons class names
 * remain in use because the icon font is loaded globally.
 * @type {Object.<string, {icon: string, label: string}>}
 */
const HTTP_BADGES = {
  request: { icon: "arrow-right-md", label: "REQUEST" },
  response: { icon: "arrow-left-md", label: "RESPONSE" },
  incoming: { icon: "arrow-down-md", label: "INCOMING" },
  outgoing: { icon: "arrow-up-md", label: "OUTGOING" },
  redirect: { icon: "paper-plane", label: "REDIRECT" },
  "redirect-in": { icon: "arrow-circle-down", label: "REDIRECT-IN" },
};

/**
 * Canonical labels for the two semantic kinds R30 surfaces in the More panel.
 * Keys match the `kind` field on `classifyMoreEntries` rows.
 * @type {Object.<string, string>}
 */
const MORE_KIND_LABELS = {
  expected: "Expected (per spec)",
  actual: "Actual (received)",
};

/**
 * Modifier class for the `<dt>` of each kind. Lookup table per components/AGENTS.md §7
 * (no dynamic class concatenation in templates — even for closed-set unions).
 * @type {Object.<string, string>}
 */
const MORE_KIND_KEY_CLASSES = {
  expected: "moreInfo-key--expected",
  actual: "moreInfo-key--actual",
  other: "moreInfo-key--other",
};

/**
 * Modifier class for the `<dd>` of each kind. Same lookup-table policy as
 * MORE_KIND_KEY_CLASSES.
 * @type {Object.<string, string>}
 */
const MORE_KIND_VALUE_CLASSES = {
  expected: "moreInfo-value--expected",
  actual: "moreInfo-value--actual",
  other: "moreInfo-value--other",
};

/**
 * Humanize a snake_case identifier for display: replace underscores with
 * spaces and capitalize the first character only ("sentence case"). Domain
 * abbreviations stay lower-case in their own segment ("Http method", not
 * "HTTP method"); the codebase favors that softer rendering over per-word
 * title-casing, which would read awkwardly for keys like `expires_in`.
 * @param {string} key Raw key (e.g. `access_token`).
 * @returns {string} Humanized label (e.g. `"Access token"`); empty string if `key` is falsy.
 */
function humanizeKey(key) {
  if (!key) return "";
  const spaced = key.replace(/_/g, " ");
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

/**
 * Classify each entry in `more` as `"expected"`, `"actual"`, or `"other"`,
 * and reorder so expected rows render first, actual second, other third.
 * Insertion order is preserved within each bucket. Classification is by
 * exact match (`expected` / `actual`) or strict prefix (`expected_…` /
 * `actual_…`) — substrings like `unexpected_field` do NOT match.
 * @param {Object.<string, unknown>} more The raw `entry.more` payload.
 * @returns {Array<{kind: "expected"|"actual"|"other", key: string, displayLabel: string, value: unknown}>} Rows ready to render, ordered expected → actual → other.
 */
function classifyMoreEntries(more) {
  if (!more || typeof more !== "object") return [];
  const expectedRows = [];
  const actualRows = [];
  const otherRows = [];
  for (const [key, value] of Object.entries(more)) {
    /** @type {"expected"|"actual"|"other"} */
    let kind;
    let suffix = "";
    if (key === "expected") {
      kind = "expected";
    } else if (key === "actual") {
      kind = "actual";
    } else if (key.startsWith("expected_")) {
      kind = "expected";
      suffix = key.slice("expected_".length);
    } else if (key.startsWith("actual_")) {
      kind = "actual";
      suffix = key.slice("actual_".length);
    } else {
      kind = "other";
    }
    let displayLabel;
    if (kind === "other") {
      displayLabel = humanizeKey(key);
    } else if (suffix) {
      displayLabel = `${MORE_KIND_LABELS[kind]} — ${humanizeKey(suffix)}`;
    } else {
      displayLabel = MORE_KIND_LABELS[kind];
    }
    const row = { kind, key, displayLabel, value };
    if (kind === "expected") expectedRows.push(row);
    else if (kind === "actual") actualRows.push(row);
    else otherRows.push(row);
  }
  return [...expectedRows, ...actualRows, ...otherRows];
}

const STYLE_ID = "cts-log-entry-styles";

// Scoped CSS for the OIDF token-styled log row. Mirrors the design archive's
// `project/preview/components-log-entry.html` layout but with the 5-column
// grid called out in U16 (timestamp / severity / http / body / actions).
// Failure rows get a left-edge red gradient; warning rows the orange
// equivalent. Inline `<code>` inside the body uses the --ink-50 surface
// described in the design preview.
//
// U3 (2026-04-26): the host establishes a `container-type: inline-size`
// query so the row reflow tracks the entry's *container* width rather than
// the viewport. Below 640px (small layout) the grid drops to two visual
// rows — meta cluster on top, body+actions on the second row, footer/More
// panel on a third row when present. At >= 640px the existing five-column
// track resumes via the @container override below. This matches
// cts-plan-modules.js's container-query precedent.
//
// Positioning-context audit: `container-type: inline-size` establishes a
// containing block for absolute / fixed descendants per the CSS Containment
// spec. cts-log-entry has no positioned descendants today (badges are
// static, the More button is static, the cURL button is static). The
// constraint is forward-looking: any future tooltip or popover added inside
// an entry will position relative to the entry host, not the viewport.
const STYLE_TEXT = `
  cts-log-entry {
    display: block;
    border-bottom: 1px solid var(--ink-100);
    font-size: var(--fs-13);
    container-type: inline-size;
    container-name: ctsLogEntry;
    /* U6: when this entry is the target of a #LOG-NNNN deep link, leave
       breathing room above so the row lands below the sticky status bar
       (U2) and any persistent banner instead of being scrolled under
       them. --status-bar-height is published by cts-log-detail-header;
       --banner-height is reserved for any future persistent banner
       (e.g. flag-flip rollback notice); it falls back to 0px when not
       present. */
    scroll-margin-top: calc(var(--status-bar-height, 0px) + var(--banner-height, 0px) + var(--space-4));
  }
  cts-log-entry:last-child { border-bottom: 0; }

  /* Default = small layout (no @container required). Applies whenever the
     wide @container rule below does not match — including in browsers
     without @container support, which then see the small layout at every
     width. */
  cts-log-entry .logItem {
    display: grid;
    grid-template-columns: 1fr auto;
    grid-template-areas:
      "metaRow metaRow"
      "logId   logId"
      "body    actions"
      "footer  footer";
    gap: var(--space-2) var(--space-3);
    padding: var(--space-2) var(--space-3);
    align-items: start;
    /* Anchor for the .is-block ::before stripe so it lays over the row's
       left edge without pushing content inward. */
    position: relative;
  }
  cts-log-entry .logIdRow {
    grid-area: logId;
    min-width: 0;
  }
  cts-log-entry .logMetaRow {
    grid-area: metaRow;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--fs-12);
    min-width: 0;
  }
  cts-log-entry .logBody { grid-area: body; }
  cts-log-entry .logActions { grid-area: actions; }
  cts-log-entry .logFooter { grid-area: footer; }
  cts-log-entry .logItem.is-fail {
    background: linear-gradient(90deg, rgba(164,54,4,0.04), transparent 60%);
  }
  cts-log-entry .logItem.is-warn {
    background: linear-gradient(90deg, rgba(235,139,53,0.06), transparent 60%);
  }
  /* Block-membership cue. Rendered as an absolutely-positioned ::before
     stripe (rather than a real border-left) so blocked rows and
     non-blocked rows share the same content start position — a real
     border would push everything 3px to the right and break vertical
     alignment between siblings. */
  cts-log-entry .logItem.is-block::before {
    content: "";
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    width: 3px;
    background: var(--orange-400);
    pointer-events: none;
  }

  cts-log-entry .logTime {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    color: var(--fg-soft);
    padding-top: 2px;
  }
  cts-log-entry .logSeverity,
  cts-log-entry .logHttp {
    display: flex;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: var(--space-1);
  }
  cts-log-entry .logBody {
    line-height: var(--lh-base);
    color: var(--fg);
    min-width: 0;
    word-break: break-word;
  }
  cts-log-entry .logBody .logSrc {
    color: var(--fg-soft);
    font-size: var(--fs-12);
    margin-right: var(--space-2);
  }
  /* U6: chip placement.
     Default (small layout) shows the chip in its own grid row — see the
     "logId" area in grid-template-areas above — and hides the inline
     instance that lives inside .logBody. The wide @container rule
     below inverts both visibilities so the chip reads inline next to
     the source label on desktop. Two render positions, one visible at
     a time, so a tap target is always present in the right place
     without requiring container-query observation in JS. */
  cts-log-entry .logIdInline { display: none; }
  cts-log-entry .logIdRow cts-log-entry-id { display: inline-flex; }
  cts-log-entry .logBody code {
    background: var(--ink-50);
    padding: 1px 5px;
    border-radius: var(--radius-1);
    font-family: var(--font-mono);
    font-size: var(--fs-12);
  }
  cts-log-entry .logActions {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: var(--space-1);
  }

  cts-log-entry .curlBtn {
    appearance: none;
    background: transparent;
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    color: var(--fg-muted);
    font-family: var(--font-mono);
    font-size: 10px;
    font-weight: var(--fw-medium);
    padding: 1px 6px;
    cursor: pointer;
  }
  cts-log-entry .curlBtn:hover { background: var(--ink-50); color: var(--fg); }
  cts-log-entry .curlBtn:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  cts-log-entry .logFooter {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding-top: var(--space-2);
  }
  cts-log-entry .logRequirements {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-1);
  }
  cts-log-entry .logRequirement {
    display: inline-block;
    background: var(--ink-50);
    border: 1px solid var(--border);
    border-radius: var(--radius-pill);
    color: var(--fg-muted);
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    padding: 1px var(--space-2);
  }
  cts-log-entry .moreInfo {
    background: var(--ink-50);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    padding: var(--space-3);
    font-size: var(--fs-12);
  }
  cts-log-entry .moreInfo dl {
    display: grid;
    grid-template-columns: minmax(120px, 200px) 1fr;
    gap: var(--space-2) var(--space-3);
    margin: 0;
  }
  cts-log-entry .moreInfo dt {
    color: var(--fg-soft);
    font-weight: var(--fw-bold);
    text-align: right;
    word-break: break-word;
  }
  /* R30: semantic labels for "expected" (per spec) and "actual" (received).
     Color is paired with explicit text labels so the meaning never relies
     on color alone. The row-level .is-fail / .is-warn gradient on the entry
     itself carries the failure cue, so the per-row treatment here stays
     subtle to avoid double-signaling. */
  cts-log-entry .moreInfo-key--expected {
    color: var(--status-info);
    border-right: 2px solid var(--status-info);
    padding-right: var(--space-2);
  }
  cts-log-entry .moreInfo-key--actual {
    color: var(--ink-700);
    border-right: 2px solid var(--ink-400);
    padding-right: var(--space-2);
  }
  cts-log-entry .moreInfo dd {
    margin: 0;
    color: var(--fg);
    word-break: break-word;
  }
  cts-log-entry .moreInfo-value--expected {
    color: var(--status-info);
  }
  /* moreInfo-key--other / moreInfo-value--actual / moreInfo-value--other
     have no rules by design — those rows inherit the default <dt> / <dd>
     treatment so the labeled expected/actual rows pop visually. The hook
     classes are still emitted (and asserted by the cts-log-entry play
     tests) so future stylesheet work can target them without churning
     the render template. */
  cts-log-entry .moreInfo pre {
    margin: 0;
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    white-space: pre-wrap;
    word-break: break-word;
  }

  /* Wide layout. Restores the original five-column track (timestamp /
     severity / http / body / actions). The .logMetaRow flex wrapper
     vanishes via display: contents so its three children participate in
     the parent grid as if the wrapper didn't exist — preserving today's
     column order without a markup change. */
  @container ctsLogEntry (min-width: 640px) {
    cts-log-entry .logItem {
      grid-template-columns: 110px 70px 60px 1fr auto;
      grid-template-areas: none;
      gap: var(--space-3);
    }
    cts-log-entry .logMetaRow {
      display: contents;
    }
    cts-log-entry .logBody { grid-area: auto; }
    cts-log-entry .logActions { grid-area: auto; }
    cts-log-entry .logFooter {
      grid-area: auto;
      grid-column: 4 / -1;
    }
    /* U6: hide the small-layout row chip and reveal the inline one inside
       .logBody (rendered between the source label and the message text). */
    cts-log-entry .logIdRow { display: none; }
    cts-log-entry .logIdInline {
      display: inline-flex;
      margin-right: var(--space-2);
      vertical-align: middle;
    }
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Renders a single log entry row. Layout reflows via container queries on the
 * host: at >= 640px the row is a 5-column grid (timestamp / severity badge /
 * HTTP marker / body / More toggle); below 640px it collapses to two visual
 * rows (meta cluster on row 1, body+actions on row 2, More panel on row 3).
 * Container queries — not viewport media queries — so the entry reflows based
 * on its container's width, supporting future side-by-side compare views and
 * narrow rails without coupling to viewport size. Failure rows get a left-edge
 * red gradient; warning rows the orange equivalent. Block-start entries
 * (`entry.blockId` set) gain a 3px orange left border.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first render; all colors / spacing route through the OIDF
 * tokens vendored in `oidf-tokens.css`. No Bootstrap classes are emitted.
 *
 * Severity is rendered via `cts-badge` (canonical `pass`/`fail`/`warn`/etc.
 * variants). The "More" toggle is composed of a `cts-badge` (count chip)
 * and a `cts-button` (`size="xs"`, `variant="secondary"`, chevron via
 * `icon`); the `.moreBtn` class on the cts-button host preserves the
 * legacy DOM hook used by `log-detail.html`'s non-Lit code path and the
 * Playwright spec. Clicking the button reveals the `.moreInfo` panel
 * listing every key in `entry.more`. R30: keys named `expected` / `actual`
 * (or prefixed
 * `expected_…` / `actual_…`) are surfaced with semantic labels ("Expected
 * (per spec)" / "Actual (received)") and rendered in expected → actual →
 * other order so users don't have to translate raw JSON keys to compare
 * what the spec required vs what the implementation produced. HTTP request
 * entries also expose a "cURL" copy button that writes a curl command to
 * the clipboard.
 *
 * @property {object} entry - Log entry object from `/api/log/{testId}`; shape
 *   includes `_id`, `time`, `result`, `http`, `src`, `msg`, `upload`,
 *   `blockId`, `requirements`, and a nested `more` object.
 * @property {string} referenceId - Human-readable ordinal label such as
 *   `"LOG-0042"` (U6). When set, the entry renders a copyable chip and
 *   the host element receives `id="LOG-0042"` so URL fragments resolve.
 *   Empty string omits both. Reflects nothing — the host id is set
 *   imperatively in `willUpdate` so the attribute and the host id stay
 *   in sync without a string-template `class=` binding.
 * @property {string} testId - Test instance ID forwarded to the chip so
 *   the deep URL it copies always carries `?log={testId}#{referenceId}`,
 *   disambiguating the same `LOG-NNNN` across re-runs.
 */
class CtsLogEntry extends LitElement {
  static properties = {
    entry: { type: Object },
    referenceId: { type: String, attribute: "reference-id" },
    testId: { type: String, attribute: "test-id" },
    _expanded: { state: true },
  };

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  constructor() {
    super();
    this.entry = {};
    this.referenceId = "";
    this.testId = "";
    this._expanded = false;
  }

  /**
   * Mirror `referenceId` to the host element's `id` attribute so URL
   * fragment navigation (`#LOG-0042`) lands the user on this entry.
   * Doing this in `willUpdate` (rather than via `reflect: true`) keeps
   * the property name (`referenceId`) free of attribute-name semantics
   * — the host id is the *value* of referenceId, not the literal string
   * `referenceId`.
   * @param {Map<string, unknown>} changed
   */
  willUpdate(changed) {
    if (changed.has("referenceId")) {
      if (this.referenceId) this.id = this.referenceId;
      else this.removeAttribute("id");
    }
  }

  _toggleMore() {
    this._expanded = !this._expanded;
  }

  _formatCurl() {
    const { more } = this.entry;
    if (!more) return "";
    const method = (more.method || "GET").toUpperCase();
    const url = more.url || "";
    const parts = [`curl -X ${method}`];
    if (more.headers) {
      for (const [key, value] of Object.entries(more.headers)) {
        parts.push(`-H '${key}: ${value}'`);
      }
    }
    if (more.body && typeof more.body === "string") {
      parts.push(`-d '${more.body}'`);
    } else if (more.body && typeof more.body === "object") {
      parts.push(`-d '${JSON.stringify(more.body)}'`);
    }
    parts.push(`'${url}'`);
    return parts.join(" \\\n  ");
  }

  async _copyCurl() {
    const curl = this._formatCurl();
    await navigator.clipboard.writeText(curl);
  }

  _renderSeverityBadges() {
    const entry = this.entry;
    return html`
      ${entry.result
        ? html`<cts-badge
            variant="${RESULT_BADGE_VARIANTS[entry.result.toLowerCase()] || "review"}"
            label="${entry.result}"
          ></cts-badge>`
        : nothing}
      ${entry.upload
        ? html`<cts-badge variant="warn" icon="camera" label="IMAGE"></cts-badge>`
        : nothing}
    `;
  }

  _renderHttpBadge() {
    const httpType = this.entry.http?.toLowerCase();
    const badge = HTTP_BADGES[httpType];
    if (!badge) return nothing;
    return html`
      <cts-badge variant="running" label="${badge.label}"></cts-badge>
      ${httpType === "request"
        ? html`<button type="button" class="curlBtn" title="Copy as cURL" @click=${this._copyCurl}>
            <cts-icon name="copy" aria-hidden="true"></cts-icon> cURL
          </button>`
        : nothing}
    `;
  }

  _renderRequirements() {
    const { requirements } = this.entry;
    if (!requirements || requirements.length === 0) return nothing;
    return html`
      <div class="logRequirements">
        ${requirements.map((req) => html`<span class="logRequirement">${req}</span>`)}
      </div>
    `;
  }

  _renderMoreButton() {
    const { more } = this.entry;
    if (!more || Object.keys(more).length === 0) return nothing;
    const count = Object.keys(more).length;
    const chevron = this._expanded ? "chevron-up" : "chevron-down";
    return html`
      <cts-badge variant="secondary" count="${count}"></cts-badge>
      <cts-button
        class="moreBtn"
        variant="secondary"
        size="xs"
        icon="${chevron}"
        label="More"
        @cts-click=${this._toggleMore}
      ></cts-button>
    `;
  }

  _renderMorePanel() {
    const { more } = this.entry;
    if (!this._expanded || !more) return nothing;
    const rows = classifyMoreEntries(more);
    return html`
      <div class="moreInfo">
        <dl>
          ${rows.map(
            ({ kind, key, displayLabel, value }) => html`
              <dt class="moreInfo-key ${MORE_KIND_KEY_CLASSES[kind]}" data-key="${key}">
                ${displayLabel}
              </dt>
              <dd class="moreInfo-value ${MORE_KIND_VALUE_CLASSES[kind]}">
                <pre>${typeof value === "string" ? value : JSON.stringify(value, null, 2)}</pre>
              </dd>
            `,
          )}
        </dl>
      </div>
    `;
  }

  _hasFooter() {
    const { more, requirements } = this.entry;
    const hasReqs = Array.isArray(requirements) && requirements.length > 0;
    const hasMore = this._expanded && more && Object.keys(more).length > 0;
    return hasReqs || hasMore;
  }

  render() {
    const entry = this.entry;
    if (!entry || !entry._id) return nothing;

    const result = (entry.result || "").toLowerCase();
    const isFail = result === "failure";
    const isWarn = result === "warning";
    const itemClasses = ["logItem"];
    if (isFail) itemClasses.push("is-fail");
    if (isWarn) itemClasses.push("is-warn");
    if (entry.blockId) itemClasses.push("is-block");

    // U6: the chip is rendered in two positions; CSS hides whichever one
    // does not match the current container width. Both bind to the same
    // referenceId/testId so a deep-URL click yields identical clipboard
    // contents regardless of the visible layout.
    const idChip = this.referenceId
      ? html`<cts-log-entry-id
          reference-id=${this.referenceId}
          test-id=${this.testId}
        ></cts-log-entry-id>`
      : nothing;

    return html`
      <div class="${itemClasses.join(" ")}">
        <div class="logMetaRow">
          <div class="logTime">
            ${entry.time ? new Date(entry.time).toLocaleTimeString() : nothing}
          </div>
          <div class="logSeverity">${this._renderSeverityBadges()}</div>
          <div class="logHttp">${this._renderHttpBadge()}</div>
        </div>
        ${this.referenceId ? html`<div class="logIdRow">${idChip}</div>` : nothing}
        <div class="logBody">
          ${entry.src ? html`<span class="logSrc">${entry.src}</span>` : nothing}
          ${this.referenceId ? html`<span class="logIdInline">${idChip}</span>` : nothing}
          ${entry.msg ? html`<span>${entry.msg}</span>` : nothing}
        </div>
        <div class="logActions">${this._renderMoreButton()}</div>
        ${this._hasFooter()
          ? html`<div class="logFooter">
              ${this._renderRequirements()} ${this._renderMorePanel()}
            </div>`
          : nothing}
      </div>
    `;
  }
}

customElements.define("cts-log-entry", CtsLogEntry);

export {};
