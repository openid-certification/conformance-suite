import { LitElement, html, nothing } from "lit";
import "./cts-icon.js";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-tooltip.js";
import "./cts-time.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";
import { loadSpecLinks, resolveSpecLink } from "../lib/spec-links.js";

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
  info: "info",
  review: "review",
  skipped: "skip",
};

/**
 * Maps a log entry's `http` value (case-insensitive) to the badge shown
 * alongside the timestamp/severity. Two shapes:
 *   - `{ icon, ariaLabel }` renders an icon-only badge (visible glyph,
 *     accessible name via aria-label). Used where the direction IS the
 *     whole signal — REQUEST (↑ sent out by the suite) and RESPONSE
 *     (↓ received) — to keep the long entry stream visually quiet.
 *   - `{ label }` renders a text badge. Used where the direction alone
 *     isn't self-explanatory enough (INCOMING/OUTGOING/REDIRECT/etc.).
 * Icon names match files under `/vendor/coolicons/icons/`.
 * @type {Object.<string, {icon?: string, ariaLabel?: string, label?: string}>}
 */
const HTTP_BADGES = {
  request: { icon: "arrow-up-md", ariaLabel: "Request" },
  response: { icon: "arrow-down-md", ariaLabel: "Response" },
  incoming: { label: "INCOMING" },
  outgoing: { label: "OUTGOING" },
  redirect: { label: "REDIRECT" },
  "redirect-in": { label: "REDIRECT-IN" },
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
 * Envelope fields the More panel must hide. Mirror of
 * `LogEntryHelper.visibleFields` in `src/main/java/.../export/LogEntryHelper.java`
 * (the legacy Thymeleaf export's strip list), plus the per-test metadata fields
 * that `/api/log/{testId}` adds to every entry on the wire (the export helper
 * never saw these because Thymeleaf rendered from the raw DB document).
 *
 * Anything in this set is either rendered elsewhere in the row (timestamp,
 * severity badge, source label, message, HTTP marker, requirements chips,
 * upload pill) or is per-test metadata identical across every entry of the
 * test and therefore noise inside the per-entry disclosure. Anything NOT in
 * the set is application payload — request bodies, response headers, the
 * `expected` / `actual` R30 keys, JWT claims, etc. — that belongs in the
 * disclosure.
 * @type {Set<string>}
 */
const ENVELOPE_FIELDS = new Set([
  "_id",
  "_class",
  "msg",
  "src",
  "time",
  "result",
  "requirements",
  "upload",
  "testOwner",
  "testId",
  "http",
  "blockId",
  "startBlock",
  "baseUrl",
  "baseMtlsUrl",
  "variant",
  "alias",
  "description",
  "planId",
  "config",
  "testName",
]);

/**
 * Resolve the disclosure payload for an entry. Two shapes are accepted:
 *
 * 1. **Fixture shape** — `entry.more` is a non-empty object. Returned as-is
 *    so existing stories (and the `_formatCurl` flow that reads
 *    `more.method` / `more.url` / `more.headers` / `more.body`) keep
 *    working without churn.
 * 2. **API shape** — the live `/api/log/{testId}` endpoint serializes
 *    application payload at the entry's *top level* (`request_uri`,
 *    `response_body`, `expected`, `actual`, …). When `entry.more` is absent
 *    or empty we synthesise the view by stripping `ENVELOPE_FIELDS` and
 *    returning what's left.
 *
 * Without this fallback the More button would never render against real
 * backend responses because `entry.more` is undefined on every wire entry
 * — the regression Almgren flagged in MR 1998 review pass (D1).
 * @param {Object.<string, unknown>} entry The full log entry.
 * @returns {Object.<string, unknown>} A flat key/value map for the disclosure panel; empty object when the entry has nothing to disclose.
 */
function extractMoreFields(entry) {
  if (!entry || typeof entry !== "object") return {};
  if (entry.more && typeof entry.more === "object" && Object.keys(entry.more).length > 0) {
    return /** @type {Object.<string, unknown>} */ (entry.more);
  }
  /** @type {Object.<string, unknown>} */
  const extras = {};
  for (const [key, value] of Object.entries(entry)) {
    if (key === "more") continue;
    if (ENVELOPE_FIELDS.has(key)) continue;
    extras[key] = value;
  }
  return extras;
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
  /* …but a block's final entry keeps its bottom border so the block has a
     closing edge below its last row (it is the :last-child of .logBlock,
     which the rule above would otherwise strip). Small layout: the border
     lives on the host here; the wide-layout equivalent restores it on the
     nested .logItem inside the @container block below. */
  cts-log-viewer .logEntries > .logBlock cts-log-entry:last-child {
    border-bottom: 1px solid var(--ink-100);
  }

  /* Default = small layout (no @container required). Applies whenever the
     wide @container rule below does not match — including in browsers
     without @container support, which then see the small layout at every
     width. */
  cts-log-entry .logItem {
    display: grid;
    grid-template-columns: 1fr auto;
    grid-template-areas:
      "metaRow metaRow"
      "body    actions"
      "footer  footer";
    gap: var(--space-2) var(--space-3);
    padding: var(--space-2) var(--space-3);
    align-items: start;
    /* Anchor for the .is-block ::before stripe so it lays over the row's
       left edge without pushing content inward. */
    position: relative;
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
     border would push everything 5px to the right and break vertical
     alignment between siblings. The stripe matches the .startBlock band
     colour (var(--ink-100)) so the block reads as one continuous surface
     from its header down its left edge. */
  cts-log-entry .logItem.is-block::before {
    content: "";
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    width: 5px;
    background: var(--ink-100);
    pointer-events: none;
  }
  /* Deep-link landing highlight. When the URL fragment targets this entry
     (#LOG-NNNN matches the host id mirrored in willUpdate), wash the row
     with the lightest brand tint so the user sees where they landed.
     Persists while the fragment matches and clears automatically when the
     user navigates to another entry — no JS. This rule's (0,2,1)
     specificity beats the global .logItem:hover repaint (0,2,0), but TIES
     the cts-log-entry .logItem.is-fail / .is-warn gradients (also (0,2,1)),
     so on failed/warned rows the highlight wins by SOURCE ORDER — this rule
     must stay defined after them. Reordering it above is-fail / is-warn
     would let a failed row's gradient override the landing wash. */
  cts-log-entry:target .logItem {
    background: var(--orange-50);
    transition: background var(--dur-1) var(--ease-standard);
  }

  cts-log-entry .logTime {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    color: var(--fg-soft);
    padding-top: 2px;
    /* Inter and the mono fall-back stack render proportional digits otherwise,
       which makes the colon column drift row-to-row in stacked log entries. */
    font-variant-numeric: tabular-nums;
  }
  /* The timestamp is the entry's deep-link handle. Keep it visually quiet —
     inherits the muted .logTime colour, no resting underline — so it reads
     as a timestamp first and a link on hover/focus. */
  cts-log-entry .logTimeLink {
    color: inherit;
    text-decoration: none;
    border-radius: var(--radius-1);
  }
  cts-log-entry .logTimeLink:hover {
    text-decoration: underline;
  }
  cts-log-entry .logTimeLink:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-log-entry .logSeverity,
  cts-log-entry .logHttp {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-1);
    min-width: 0;
  }
  /* cts-tooltip is a behaviour-only wrapper; collapse its inline box so
     the wrapped cts-button aligns directly within the .logHttp flex row
     instead of inheriting cts-tooltip's ghost line-box. */
  cts-log-entry .logHttp cts-tooltip {
    display: contents;
  }
  cts-log-entry .logBody {
    line-height: var(--lh-base);
    color: var(--fg);
    min-width: 0;
    /* R31: long URLs in log messages must not push content off-screen.
       Combined with min-width: 0 on the Grid 1fr track, overflow-wrap
       anywhere lets a single unbreakable URL wrap at any character when
       the column is narrower than the URL, while still preferring word
       boundaries for prose. Standard, modern equivalent of the legacy
       non-standard word-break:break-word. */
    overflow-wrap: anywhere;
  }
  cts-log-entry .logBody .logSrc {
    color: var(--fg-soft);
    font-size: var(--fs-12);
    margin-right: var(--space-2);
  }
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

  cts-log-entry .logFooter {
    display: flex;
    flex-direction: column;
    /* Inter-element gap inside the footer (requirements ↔ moreInfo panel).
       No padding-top: the grid row-gap on .logItem already spaces the
       footer below the body. Adding padding here would double-space along
       the same edge and diverge between the small (8px) and wide (12px)
       container-query layouts. */
    gap: var(--space-2);
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
    text-decoration-line: none;
  }
  cts-log-entry a.logRequirement {
    color: var(--fg-link, var(--fg-muted));
    cursor: pointer;
    /* Anchor variant opts back into the underline the base pill suppresses,
       kept transparent at rest so the global \`a\` transition fades it in. */
    text-decoration-line: underline;
    text-underline-offset: 2px;
    text-decoration-color: transparent;
  }
  cts-log-entry a.logRequirement:hover,
  cts-log-entry a.logRequirement:focus-visible {
    background: var(--bg-muted);
    color: var(--fg);
    text-decoration-color: var(--link-decoration-color);
  }
  cts-log-entry .moreInfo {
    background: var(--ink-50);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    padding: var(--space-3);
    font-size: var(--fs-12);
  }
  /* Stacked layout: each label sits above its value at every container
     width. The earlier two-column grid (dt | dd) read well only when
     labels were short and values fit on one line — for HTTP debug data
     (multi-line JSON, long URLs) the narrow 1fr value column forced
     character-level word-break that was painful to scan. Stacking gives
     each value the full panel width, so URLs wrap on word boundaries
     and JSON lines break at expected indentation. Spacing rhythm: tight
     between a label and its value (dt→dd ≈ 4px), looser between groups
     (dd→next dt ≈ 16px) so each label/value pair reads as one unit. */
  cts-log-entry .moreInfo dl {
    margin: 0;
  }
  cts-log-entry .moreInfo dt {
    font-family: var(--font-sans);
    font-size: var(--fs-12);
    font-weight: var(--fw-medium);
    color: var(--fg-soft);
    letter-spacing: 0.02em;
    margin: 0 0 var(--space-1) 0;
    text-align: left;
  }
  cts-log-entry .moreInfo dd {
    margin: 0 0 var(--space-4) 0;
    padding-left: var(--space-3);
    border-left: 2px solid var(--ink-200);
    color: var(--fg);
    overflow-wrap: anywhere;
  }
  cts-log-entry .moreInfo dd:last-child {
    margin-bottom: 0;
  }
  /* R30: semantic cue for "expected" (per spec) and "actual" (received).
     The cue rides on (a) the dt label color and (b) the dd's coloured
     left border + value tint. Pairing color with explicit text labels
     means the meaning never relies on color alone. The row-level
     .is-fail / .is-warn gradient on the entry itself carries the failure
     cue, so the per-row treatment here stays subtle to avoid
     double-signaling. */
  cts-log-entry .moreInfo-key--expected {
    color: var(--status-info);
  }
  cts-log-entry .moreInfo-key--actual {
    color: var(--ink-700);
  }
  cts-log-entry .moreInfo-value--expected {
    border-left-color: var(--status-info);
    color: var(--status-info);
  }
  cts-log-entry .moreInfo-value--actual {
    border-left-color: var(--ink-400);
  }
  /* moreInfo-key--other / moreInfo-value--other have no rules by design —
     those rows inherit the neutral dt/dd treatment so the labeled
     expected/actual rows pop visually. The hook classes are still
     emitted (and asserted by the cts-log-entry play tests) so future
     stylesheet work can target them without churning the render
     template. */
  cts-log-entry .moreInfo pre {
    margin: 0;
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }

  /* Wide layout. Restores the original five-column track (timestamp /
     severity / http / body / actions). The .logMetaRow flex wrapper
     vanishes via display: contents so its three children participate in
     the parent grid as if the wrapper didn't exist — preserving today's
     column order without a markup change.

     Severity and HTTP columns are auto-sized (max-content) so badges
     never overflow into the body. Earlier fixed widths (70px/60px) caused
     "REQUEST" + cURL to spill across the body text on rows where the
     status pill was wider than its column. The body column (1fr) absorbs
     whatever is left after the meta cluster and the actions tray sit at
     their natural widths. */
  @container ctsLogViewer (min-width: 640px) {
    /* Default wide layout: each .logItem owns its own 5-column
       grid. This is the fallback; both overrides below replace it
       with subgrid so columns align across rows — the top-level
       rule for direct .logEntries children, and the .logBlock rule
       for entries nested inside a block. A .logItem only keeps this
       standalone 5-column grid when used outside the viewer's
       master grid entirely (e.g. the cts-log-entry story in
       isolation). */
    cts-log-entry .logItem {
      grid-template-columns: 92px max-content max-content 1fr auto;
      grid-template-areas: none;
      /* Split row/column gap: 8px between the body row and the footer row
         keeps requirement chips visually grouped with the message above,
         while 12px between the timestamp / severity / http / body /
         actions columns preserves the horizontal rhythm badges rely on.
         Same row-then-column pattern as the narrow layout above. */
      gap: var(--space-2) var(--space-3);
      border-bottom: 1px solid var(--ink-100);
    }
    cts-log-entry:last-child .logItem {
      border-bottom: 0;
    }
    /* …but a block's final entry keeps its bottom border (closing edge for
       the block). Wide layout: the host is display:contents, so the border
       lives on the nested .logItem. Mirrors the small-layout override above. */
    cts-log-viewer .logEntries > .logBlock cts-log-entry:last-child .logItem {
      border-bottom: 1px solid var(--ink-100);
    }
    /* Top-level entries (direct children of .logEntries) subgrid
       into the master grid in cts-log-viewer so timestamp /
       severity / http / body / actions columns are sized once
       across ALL top-level rows by the widest content in each
       track. cts-log-entry hosts are display: contents at this
       width (see cts-log-viewer.js), so .logItem is a direct
       child of the parent grid and one level of subgrid is enough.

       Block entries (nested inside <div class="logBlock">) subgrid
       the same way, one level deeper: .logBlock is itself a subgrid
       that relays the master tracks, and its nested cts-log-entry
       hosts are display: contents too (see cts-log-viewer.js), so this
       .logItem is a direct grid item of .logBlock and inherits the
       master columns through it. (The block was once a <details>,
       whose UA ::details-content wrapper had to be dissolved with
       display: contents for the relay to propagate; de-collapsing it
       to a <div> removed that wrapper, so no such hack is needed now.)
       This replaces the per-entry max-content grid that previously left
       block rows ragged and misaligned with top-level rows. */
    .logEntries > cts-log-entry .logItem,
    .logBlock cts-log-entry .logItem {
      grid-template-columns: subgrid;
      grid-column: 1 / -1;
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
 * entries also expose an icon-only copy button (accessible name "Copy as
 * cURL") that writes a curl command to the clipboard.
 *
 * @property {object} entry - Log entry object from `/api/log/{testId}`; shape
 *   includes `_id`, `time`, `result`, `http`, `src`, `msg`, `upload`,
 *   `blockId`, `requirements`, and a nested `more` object.
 * @property {string} referenceId - Human-readable ordinal label such as
 *   `"LOG-0042"`. When set, the timestamp renders as a deep-link anchor
 *   (`href="#LOG-0042"`) and the host element receives `id="LOG-0042"` so
 *   URL fragments resolve to this row. Empty string renders a plain,
 *   non-interactive timestamp and omits the id. The host id is set both in
 *   `willUpdate` (for standalone use) and by the viewer's entry template,
 *   so the fragment target exists the moment the viewer's render commits.
 * @property {string} testId - Test instance ID forwarded by the viewer. No
 *   longer read by this component since the `LOG-NNNN` copy chip was
 *   retired — the timestamp deep-link is a relative `#LOG-NNNN` fragment
 *   that resolves against the page's existing `?log=` query, so no testId
 *   is needed here. Retained pending the removal flagged in code review.
 */
class CtsLogEntry extends LitElement {
  static properties = {
    entry: { type: Object },
    referenceId: { type: String, attribute: "reference-id" },
    testId: { type: String, attribute: "test-id" },
    _expanded: { state: true },
    _specLinks: { state: true },
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
    this._specLinks = null;
  }

  connectedCallback() {
    super.connectedCallback();
    // Spec-link map is fetched once per page (KTD4) and cached at module
    // scope inside lib/spec-links.js; subsequent mounts reuse the in-flight
    // Promise rather than hitting the network again. The first render shows
    // requirements as static chips; once the map resolves, the state update
    // re-renders them as anchors when a prefix matches.
    loadSpecLinks().then((map) => {
      if (!this.isConnected) return;
      this._specLinks = map;
    });
  }

  /**
   * Mirror `referenceId` to the host element's `id` attribute so URL
   * fragment navigation (`#LOG-0042`) lands the user on this entry.
   * Doing this in `willUpdate` (rather than via `reflect: true`) keeps
   * the property name (`referenceId`) free of attribute-name semantics
   * — the host id is the *value* of referenceId, not the literal string
   * `referenceId`.
   * @param {Map<string, unknown>} changed - Lit's changed-properties map for this update cycle.
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

  async _copyCurl(event) {
    // Capture currentTarget synchronously: by the time the await
    // resolves the event has finished dispatching and currentTarget is
    // null (per the DOM spec), so reading it later loses the trigger.
    const trigger = event && event.currentTarget;
    const curl = this._formatCurl();
    try {
      await navigator.clipboard.writeText(curl);
    } catch (err) {
      console.warn("[cts-log-entry] clipboard.writeText failed:", err);
      return;
    }
    flashCopyConfirmed(trigger);
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
    // Icon-only badges (request/response) carry their meaning in the
    // glyph alone, so wrap them in cts-tooltip to surface the textual
    // label on hover/focus. Read-aloud users get the same name via the
    // badge's forwarded aria-label. The .logHttp cts-tooltip rule is
    // display:contents, so the wrapper doesn't disturb the flex row.
    const badgeEl = badge.icon
      ? html`<cts-tooltip content="${badge.ariaLabel}" placement="top"
          ><cts-badge
            variant="info"
            icon="${badge.icon}"
            aria-label="${badge.ariaLabel}"
          ></cts-badge
        ></cts-tooltip>`
      : html`<cts-badge variant="info" label="${badge.label}"></cts-badge>`;
    return html`
      ${badgeEl}
      ${httpType === "request"
        ? html`<cts-tooltip content="Copy as cURL" placement="top"
            ><cts-button
              class="curlBtn"
              variant="ghost"
              size="xxs"
              icon="copy"
              aria-label="Copy as cURL"
              @cts-click=${this._copyCurl}
            ></cts-button
          ></cts-tooltip>`
        : nothing}
    `;
  }

  _renderRequirements() {
    const { requirements } = this.entry;
    if (!requirements || requirements.length === 0) return nothing;
    const map = this._specLinks;
    return html`
      <div class="logRequirements">
        ${requirements.map((req) => {
          const url = map ? resolveSpecLink(req, map) : null;
          if (url) {
            return html`<a
              class="logRequirement"
              href="${url}"
              target="_blank"
              rel="noopener noreferrer"
              >${req}</a
            >`;
          }
          return html`<span class="logRequirement">${req}</span>`;
        })}
      </div>
    `;
  }

  _renderMoreButton() {
    const more = extractMoreFields(this.entry);
    if (Object.keys(more).length === 0) return nothing;
    // Subtle disclosure: chevron + "Details" in a ghost button.
    // No border, no count number — "Details" tells the user *what* will
    // appear (request payload, expected/actual values, headers …) rather
    // than just how many items, which on its own delivers no meaning.
    // Visually this recedes behind the FAILURE/SUCCESS pills that need
    // attention. The .moreBtn class hook is preserved for log-detail.html
    // and the e2e specs that rely on it.
    //
    // The chevron icon is the static `chevron-down`; cts-button rotates it
    // 180° via CSS keyed on the forwarded `aria-expanded` attribute (see
    // cts-button.js styles). One transition, one DOM node — no per-render
    // glyph swap.
    return html`
      <cts-button
        class="moreBtn"
        variant="ghost"
        size="xs"
        icon="chevron-down"
        label="Details"
        aria-expanded="${this._expanded ? "true" : "false"}"
        @cts-click=${this._toggleMore}
      ></cts-button>
    `;
  }

  _renderMorePanel() {
    if (!this._expanded) return nothing;
    const more = extractMoreFields(this.entry);
    if (Object.keys(more).length === 0) return nothing;
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
    const { requirements } = this.entry;
    const hasReqs = Array.isArray(requirements) && requirements.length > 0;
    const hasMore = this._expanded && Object.keys(extractMoreFields(this.entry)).length > 0;
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

    // The timestamp doubles as this entry's citation handle: when the
    // entry has a reference id it renders as a deep-link anchor
    // (`#LOG-NNNN`). Left-click jumps to the entry; "Copy Link Address"
    // resolves the fragment against the current `?log=<testId>` URL,
    // yielding the same canonical deep link the retired LOG-NNNN chip used
    // to build. The relative fragment keeps left-click a pure in-page
    // navigation (no reload). aria-label carries the reference id so the
    // accessible name stays unique across rows logged in the same second.
    // No tooltip — the audience is technical and the anchor affordance
    // (hover underline + right-click → Copy Link Address) is self-evident.
    const time = html`<cts-time mode="time-of-day" value=${entry.time || ""}></cts-time>`;
    const timeContent = this.referenceId
      ? html`<a
          class="logTimeLink"
          href="#${this.referenceId}"
          aria-label="Link to log entry ${this.referenceId}"
          >${time}</a
        >`
      : time;

    return html`
      <div class="${itemClasses.join(" ")}">
        <div class="logMetaRow">
          <div class="logTime">${timeContent}</div>
          <div class="logSeverity"> ${this._renderSeverityBadges()} </div>
          <div class="logHttp">${this._renderHttpBadge()}</div>
        </div>
        <div class="logBody">
          ${entry.src ? html`<span class="logSrc">${entry.src}</span>` : nothing}
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
