import { LitElement, html, nothing, css } from "lit";
import "./cts-icon.js";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-link-button.js";
import "./cts-alert.js";
import "./cts-json-editor.js";
import "./cts-test-nav-controls.js";
import "./cts-failure-summary.js";
import "./cts-action-overflow.js";
import "./cts-time.js";
import { formatDescription } from "./format-description.js";
import { splitTestSummary } from "./test-summary-split.js";

/**
 * Top-level test result -> canonical cts-badge variant. INTERRUPTED maps to
 * `fail` because an interrupted run did not complete successfully and we
 * surface that alongside the FINISHED/INTERRUPTED status badge.
 * Lookup table per components/AGENTS.md §7 (no dynamic class concatenation).
 * @type {Object.<string, string>}
 */
const RESULT_BADGE_VARIANTS = {
  PASSED: "pass",
  FAILED: "fail",
  WARNING: "warn",
  REVIEW: "review",
  SKIPPED: "skip",
  INTERRUPTED: "fail",
};

/**
 * Test running-state -> canonical cts-badge variant. FINISHED is neutral
 * (`skip`) because the sibling result badge carries the outcome; WAITING
 * uses `warn` to signal user action; INTERRUPTED matches RESULT_BADGE_VARIANTS.
 * @type {Object.<string, string>}
 */
const STATUS_BADGE_VARIANTS = {
  RUNNING: "running",
  WAITING: "warn",
  FINISHED: "skip",
  INTERRUPTED: "fail",
};

/**
 * Per-condition result keys (lowercase, mirroring backend log entries) used
 * to aggregate counts in the sticky bar's pill cluster. These keys must
 * match `entry.result.toLowerCase()` from the backend (success/failure/
 * warning/review/info), so they are intentionally NOT the canonical badge
 * variant names — see RESULT_TYPE_BADGE_VARIANTS for the key -> variant
 * mapping.
 * @type {ReadonlyArray<string>}
 */
const RESULT_TYPES = ["success", "failure", "warning", "review", "info"];

/**
 * Per-condition result key -> canonical cts-badge variant.
 * @type {Object.<string, string>}
 */
const RESULT_TYPE_BADGE_VARIANTS = {
  success: "pass",
  failure: "fail",
  warning: "warn",
  review: "review",
  info: "info-subtle",
};

/**
 * Per-condition result key -> compact glyph used in the sticky status bar's
 * result-pill cluster (e.g. `✓ 47`, `✗ 3`). Lookup table per
 * components/AGENTS.md §7 (no dynamic class concatenation).
 * @type {Object.<string, string>}
 */
const RESULT_TYPE_PILL_GLYPHS = {
  success: "✓",
  failure: "✗",
  warning: "⚠",
  review: "?",
  info: "ⓘ",
};

/**
 * Lifecycle-driven hero region keys. Drives `_renderHero()`'s dispatch
 * and the visual eyebrow / divider treatment per state.
 * @type {Object.<string, string>}
 */
const HERO_MODES = {
  PASSED: "summary",
  SKIPPED: "summary",
  FAILED: "failures",
  WARNING: "failures",
  REVIEW: "failures",
  INTERRUPTED: "interrupted",
  WAITING: "waiting",
  RUNNING: "running",
};

/**
 * Terminal `test.result` values that pin the lifecycle into a finished
 * phase regardless of what `test.status` reports. The runner sets
 * `result` as soon as a verdict is assigned, but the WAITING→FINISHED
 * status flip can lag (the front-end polling cadence isn't synchronized
 * with the verdict write). When `result` is one of these values, the
 * UI must surface the verdict immediately — leaving Start visible on a
 * test that already passed/failed is what MR 1998 finding A1 reported.
 * @type {ReadonlySet<string>}
 */
const TERMINAL_RESULTS = new Set([
  "PASSED",
  "FAILED",
  "WARNING",
  "REVIEW",
  "SKIPPED",
  "INTERRUPTED",
]);

/**
 * Phase → terminal-banner palette + headline. Palette keys map 1:1 to
 * the `.ctsTerminalBanner--*` modifier classes defined in STYLE_TEXT;
 * headline strings are the "did my test pass?" answer in plain English
 * (MR 1998 findings A2 + A7).
 *
 * REVIEW result uses the warn palette: a reviewer needs to act, so the
 * banner reads as "needs attention", not as a verdict failure.
 * @type {Object.<string, { palette: string, headline: string, icon: string }>}
 */
const TERMINAL_BANNER_BY_PHASE = {
  "finished-pass": { palette: "pass", headline: "Test passed", icon: "circle-check" },
  "finished-fail": { palette: "fail", headline: "Test failed", icon: "close-circle" },
  "finished-warn": { palette: "warn", headline: "Test passed with warnings", icon: "warning" },
  "finished-review": { palette: "warn", headline: "Test needs review", icon: "warning" },
  "finished-skip": { palette: "skip", headline: "Test skipped", icon: "info" },
  interrupted: { palette: "fail", headline: "Test interrupted", icon: "close-circle" },
};

const STYLE_ID = "cts-log-detail-header-styles";

// Scoped CSS for the log-detail header. All values flow from oidf-tokens.css.
//
// Visual structure (top-to-bottom inside the host element):
//
//   ┌───────────────────────────────────────────────────────────┐
//   │ Nav row (.ctsNavRow — plan progress + Continue Plan)      │
//   ├───────────────────────────────────────────────────────────┤
//   │ Sticky status bar (Region A — U2; unchanged)              │  shadow-1
//   ├───────────────────────────────────────────────────────────┤
//   │ Terminal-state banner (PASSED/FAILED/WARN/REVIEW/SKIP/    │  status palette
//   │ INTERRUPTED only; absent during RUNNING / WAITING)        │
//   ├───────────────────────────────────────────────────────────┤
//   │ Hero (lifecycle-driven dominant zone)                     │  no chrome
//   │   FAILED/WARNING/REVIEW       → count headline + failure  │  fs-20 head
//   │   INTERRUPTED                 → error slot + failure list │
//   │   PASSED/SKIPPED              → R24 description prose     │  fs-15 body
//   │   WAITING                     → R24 instructions (Start in│
//   │                                 sticky bar, not duplicated)│
//   │   RUNNING                     → exposed values            │
//   ├───────────────────────────────────────────────────────────┤
//   │ Drawer (Region C — two <details> disclosures)             │
//   │   ▸ Test details (metadata table; closed by default)      │
//   │   ▸ Configuration (JSON viewer; closed by default)        │
//   └───────────────────────────────────────────────────────────┘
//
// The nav row leads so plan-level orientation sits immediately under
// the page-level breadcrumb (cts-crumb in log-detail.html). The
// terminal banner follows the sticky bar so the verdict is the first
// thing the eye lands on after the bar's pill cluster, with the bar
// still pinning at top: 0 on scroll. Each section is divided by a 1px
// border (no card-within-card chrome). The sticky bar carries the
// only shadow; the rest reads as a flat document under the bar.
const STYLE_TEXT = css`
  cts-log-detail-header {
    /* display: contents removes the host from the box tree so the
       sticky .ctsStatusBar below sticks within the page-level
       containing block (.log-page-main) instead of being clipped at
       the bottom of the header's intrinsic content height. The host
       paints no background / border / padding itself, so the swap is
       visually invisible. */
    display: contents;
  }

  /* Sticky status bar (Region A — unchanged from U2). Sticks at tablet
     and above; on mobile it scrolls away. Z-index 10 keeps the bar
     above the connection-lost banner (z-index 9 in cts-log-viewer's
     own styles).
     'position: relative' is set unconditionally (not just inside the
     >=640px sticky branch) so the ::after gradient — used as the
     bar's elevation effect in place of a 'box-shadow' — has a
     positioning ancestor at every viewport. The pseudo itself only
     paints when sticky kicks in (see the >=640px branch below); on
     mobile it stays display:none so no fake shadow streaks across the
     hero as the bar scrolls past with the page. */
  cts-log-detail-header .ctsStatusBar {
    position: relative;
    display: grid;
    grid-template-columns: auto 1fr auto;
    grid-template-areas:
      "left    middle  primary"
      "created created created";
    column-gap: var(--space-3);
    align-items: center;
    padding: 20px;
    margin-inline: -20px;
    background: var(--bg-elev);
    border-bottom: 1px solid var(--border);
    z-index: 10;
  }
  /* Faux drop-shadow for the sticky bar. A real 'box-shadow' bleeds
     past the bar's left/right edges (the rule's spread fades outward
     in every direction), which reads as two grey wings poking out of
     a page that has no other floating chrome. Substituting an
     absolutely-positioned ::after with a top-to-bottom gradient
     constrains the shadow to exactly the bar's width — 'left: 0;
     right: 0' matches the bar's content box, and the gradient fades
     from a low-opacity ink to transparent over a few pixels so the
     elevation cue still reads at a glance.
     'pointer-events: none' keeps the pseudo from blocking clicks on
     the section directly below the bar (failure summary chips,
     drawer summaries, log entry rows). The pseudo is hidden by
     default and only painted on the sticky branch below — no fake
     shadow on mobile, where the bar scrolls with the content. */
  cts-log-detail-header .ctsStatusBar::after {
    content: "";
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    height: var(--space-2);
    pointer-events: none;
    display: none;
    background: linear-gradient(to bottom, rgba(0, 0, 0, 0.07), rgba(0, 0, 0, 0));
  }
  cts-log-detail-header .ctsStatusBarLeft {
    grid-area: left;
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }
  cts-log-detail-header .ctsStatusBarMiddle {
    grid-area: middle;
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
    min-width: 0;
  }
  cts-log-detail-header .ctsStatusBarSupport {
    color: var(--fg-muted);
    font-size: var(--fs-13);
  }
  cts-log-detail-header .ctsStatusBarPrimary {
    grid-area: primary;
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
  cts-log-detail-header .ctsStatusBarOverflow {
    display: contents;
  }
  /* Test name leads the bar's left cluster (Row 1, ahead of the status
     pill and result-count badges) so the bar's title — "which test is
     this?" — reads before the badges that describe it. Slightly larger
     than the surrounding chrome (--fs-14 vs --fs-13) and weighted as
     the bar's title, truncating with ellipsis when long names would
     wrap the badges onto a new visual line. */
  cts-log-detail-header .ctsStatusBarTestNameText {
    color: var(--fg);
    font-size: var(--fs-14);
    font-weight: var(--fw-medium);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    min-width: 0;
    flex: 0 1 auto;
  }
  /* Row 2 carries the created timestamp alone. Promoted out of the
     drawer (where it used to be) because operators glance at "when
     did this run?" constantly — hiding it behind a disclosure was a
     hierarchy regression. */
  cts-log-detail-header .ctsStatusBarCreated {
    grid-area: created;
    color: var(--fg-muted);
    font-size: var(--fs-13);
    white-space: nowrap;
  }
  @media (min-width: 640px) {
    cts-log-detail-header .ctsStatusBar {
      position: sticky;
      top: 0;
    }
    /* Reveal the faux drop-shadow only when the bar is sticky. A
       static bar at mobile widths gets no shadow — it sits flush with
       the hero below it and the 1px border-bottom is the only
       elevation cue needed. */
    cts-log-detail-header .ctsStatusBar::after {
      display: block;
    }
  }

  /* Nav row — the first zone inside the header, carrying the plan
     navigation cluster (#testNavControls). Sits between the page-level
     breadcrumb (cts-crumb in log-detail.html) and the sticky status
     bar so the page reads "plan link → plan progress → this test"
     top-to-bottom. Always visible at every viewport so the user can
     step Previous / Next without opening a drawer.
     The cts-test-nav-controls component was originally designed for
     the legacy vertical action stack (column layout, card chrome,
     full-width buttons). Overriding its inner layout here makes it
     read as a horizontal control row inside the new structure
     without touching the component itself — the override is scoped
     to the .ctsNavRow descendant context only. */
  cts-log-detail-header .ctsNavRow {
    padding: var(--space-4) 0;
    border-bottom: 1px solid var(--border);
  }
  /* Hide the nav row when the embedded cts-test-nav-controls renders
     nothing — for example, an ad-hoc test (no planId) or the brief
     window before /api/plan resolves (totalCount=0, slim mode, no
     Continue). Without this, the row's padding + border-bottom would
     paint as an empty divider between the breadcrumb and the sticky
     status bar, reading as a broken section break. */
  cts-log-detail-header .ctsNavRow:has(cts-test-nav-controls:empty) {
    display: none;
  }
  cts-log-detail-header .ctsNavRow cts-test-nav-controls .cts-tnc-group {
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    padding: 0;
    background: transparent;
    border: none;
    border-radius: 0;
  }
  cts-log-detail-header .ctsNavRow cts-test-nav-controls .cts-tnc-progress {
    flex-direction: row;
    align-items: center;
    gap: var(--space-2);
    flex: 1 1 200px;
    min-width: 160px;
  }
  cts-log-detail-header .ctsNavRow cts-test-nav-controls .cts-tnc-progress-track {
    flex: 1 1 auto;
    min-width: 80px;
  }
  cts-log-detail-header .ctsNavRow cts-test-nav-controls .cts-tnc-buttons {
    flex-direction: row;
    flex-wrap: wrap;
    gap: var(--space-2);
  }
  cts-log-detail-header .ctsNavRow cts-test-nav-controls .cts-tnc-buttons cts-button,
  cts-log-detail-header .ctsNavRow cts-test-nav-controls .cts-tnc-buttons cts-link-button {
    width: auto;
  }

  /* Terminal-state banner — the verdict, shown as a full-width band
     between the sticky status bar and the hero whenever a test has
     reached a terminal phase (PASSED / FAILED / WARNING / REVIEW /
     SKIPPED / INTERRUPTED). Closes MR 1998 findings A2 + A7: without this,
     the only "did my test pass?" signal was a small chip among the
     log filters, which both reviewers flagged as too subtle.
     The bleed-out margins match the sticky bar's so the banner
     reads as the same horizontal section as the page chrome above. */
  cts-log-detail-header .ctsTerminalBanner {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-4) 20px;
    margin-inline: -20px;
    font-family: var(--font-sans);
    font-weight: var(--fw-bold);
    font-size: var(--fs-20);
    line-height: var(--lh-tight);
    border-bottom: 1px solid var(--border);
  }
  cts-log-detail-header .ctsTerminalBanner cts-icon {
    flex: 0 0 auto;
  }
  cts-log-detail-header .ctsTerminalBanner--pass {
    background: var(--status-pass-bg);
    color: var(--status-pass);
    border-bottom-color: var(--status-pass-border);
  }
  cts-log-detail-header .ctsTerminalBanner--fail {
    background: var(--status-fail-bg);
    color: var(--status-fail);
    border-bottom-color: var(--status-fail-border);
  }
  cts-log-detail-header .ctsTerminalBanner--warn {
    background: var(--status-warning-bg);
    color: var(--status-warning);
    border-bottom-color: var(--status-warning-border);
  }
  cts-log-detail-header .ctsTerminalBanner--skip {
    background: var(--status-skipped-bg);
    color: var(--status-skipped);
    border-bottom-color: var(--status-skipped-border);
  }

  /* Hero — the lifecycle-driven dominant zone. Flat section on the
     page background; no card chrome. Generous padding gives the
     hero its weight. The eyebrow / headline / body type-scale ramp
     replaces the legacy card's internal grid. */
  cts-log-detail-header .ctsHero {
    /* Horizontal padding is owned by the page wrapper (.log-page
       padding-inline). Vertical padding gives the hero its own
       breathing room as a section so it never sits flush against the
       nav row's border above or the drawer summaries below. */
    padding-inline: 0;
    padding-block: var(--space-5) var(--space-2);
    display: flex;
    flex-direction: column;
  }
  /* Page-injected slots ([data-slot="error"], [data-slot="browser"])
     are empty until log-detail.js injects an alert or browser-URL
     prompt. 'display: contents' makes the slot disappear from the
     flex flow while empty, so the parent's gap skips it. The moment
     log-detail.js appends a child element, :not(:has(*)) stops
     matching and the slot rejoins the flex flow with the standard
     gap above and below. */
  cts-log-detail-header .ctsHero > [data-slot]:not(:has(*)) {
    display: contents;
  }
  /* Once the error slot is populated by log-detail.js, separate the
     injected FINAL_ERROR alert from the sibling "interrupted" alert
     that follows it. Margin lives on the slot (not on the parent as a
     blanket 'gap') so the rest of the hero — running/waiting/summary
     variants — remains visually unchanged. */
  cts-log-detail-header .ctsHero > [data-slot="error"]:has(*) {
    margin-bottom: var(--space-3);
  }
  /* Eyebrow + headline are a single title group (Gestalt proximity),
     so they sit 4px apart instead of inheriting the 16px hero gap. */
  cts-log-detail-header .ctsHeroEyebrow {
    font-size: var(--fs-12);
    font-weight: var(--fw-bold);
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--fg-soft);
    margin-top: var(--space-4);
  }
  cts-log-detail-header .ctsHeroHeadline {
    font-size: var(--fs-20);
    font-weight: var(--fw-bold);
    color: var(--fg);
    line-height: 1.3;
    margin: 0;
    font-variant-numeric: tabular-nums;
  }
  cts-log-detail-header .ctsHeroBody {
    font-size: var(--fs-13);
    line-height: 1.6;
    color: var(--fg);
  }
  cts-log-detail-header .ctsHeroBody p {
    margin: 0 0 var(--space-3);
  }
  cts-log-detail-header .ctsHeroBody p:last-child {
    margin-bottom: 0;
  }
  cts-log-detail-header .ctsHeroBody code {
    font-family: var(--font-mono);
    font-size: 0.92em;
    background: var(--bg-muted);
    color: var(--fg);
    padding: 0 var(--space-1);
    border-radius: var(--radius-1);
  }
  cts-log-detail-header .ctsHeroPlaceholder {
    color: var(--fg-faint);
    font-style: italic;
    font-size: var(--fs-14);
  }
  /* Failure hero — wraps cts-failure-summary and elevates each row's
     type / spacing so the failure list reads as the page's primary
     affordance. Hide cts-failure-summary's own accordion title (the
     hero's count headline replaces it) and drop the section's
     border-top since the hero is already a bordered section. */
  cts-log-detail-header .ctsHero--failures cts-failure-summary .failureSummaryTitle {
    display: none;
  }
  cts-log-detail-header .ctsHero--failures cts-failure-summary .failureSummary {
    margin-top: 0;
    border-top: none;
    padding-top: 0;
  }
  cts-log-detail-header .ctsHero--failures cts-failure-summary .failureItem {
    font-size: var(--fs-14);
    padding: var(--space-2) 0;
  }
  cts-log-detail-header .ctsHero--failures cts-failure-summary .failureText {
    font-weight: var(--fw-medium);
  }

  /* Running / waiting hero — exposed values + browser slot rows.
     Mirrors the legacy .runningTestRow stack but inside the hero
     instead of a secondary card. */
  cts-log-detail-header .ctsExposedLabel {
    font-weight: var(--fw-bold);
    color: var(--fg);
    margin-bottom: var(--space-2);
  }
  cts-log-detail-header .ctsExposedJson {
    display: block;
  }

  /* Drawer (Region C) — two <details> disclosures. Native semantics +
     keyboard a11y; the chevron rotates 90° when [open]. No card chrome;
     borders between disclosures are 1px dividers continuing the
     section rhythm. */
  cts-log-detail-header .ctsDrawer {
    padding: 0;
    margin-bottom: 20px;
  }
  cts-log-detail-header .ctsDrawer details {
    border-bottom: 1px solid var(--border);
  }
  cts-log-detail-header .ctsDrawer details:last-child {
    border-bottom: none;
  }
  cts-log-detail-header .ctsDrawer summary {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-3) 0;
    cursor: pointer;
    list-style: none;
    color: var(--fg-soft);
    font-size: var(--fs-13);
    font-weight: var(--fw-medium);
    border-radius: var(--radius-2);
  }
  cts-log-detail-header .ctsDrawer summary::-webkit-details-marker {
    display: none;
  }
  cts-log-detail-header .ctsDrawer summary:hover {
    color: var(--fg);
  }
  cts-log-detail-header .ctsDrawer summary:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-log-detail-header .ctsDrawer summary cts-icon {
    transition: transform 150ms ease;
    color: var(--fg-faint);
  }
  cts-log-detail-header .ctsDrawer details[open] summary cts-icon {
    transform: rotate(90deg);
  }
  cts-log-detail-header .ctsDrawer .ctsDrawerBody {
    padding: 0 0 var(--space-4) var(--space-6);
  }
  @media (prefers-reduced-motion: reduce) {
    cts-log-detail-header .ctsDrawer summary cts-icon {
      transition: none;
    }
  }

  /* Metadata table inside the Test details disclosure. Mirrors the
     legacy header card's metadata grid so the data treatment stays
     familiar; only its position changed. */
  cts-log-detail-header .logMetaTable {
    display: grid;
    grid-template-columns: minmax(120px, 180px) 1fr;
    gap: var(--space-2) var(--space-4);
    font-size: var(--fs-13);
  }
  cts-log-detail-header .logMetaLabel {
    color: var(--fg-soft);
    font-weight: var(--fw-bold);
  }
  cts-log-detail-header .logMetaValue {
    color: var(--fg);
    overflow-wrap: anywhere;
  }
  /* Mirror cts-plan-header's .mono chip — small monospace pill for
     IDs, versions, and variant strings so a reader comparing the
     plan-detail header with the test-detail drawer sees the same
     visual treatment for the same kind of data. */
  cts-log-detail-header .logMetaValue .mono {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    color: var(--fg);
    background: var(--ink-50);
    padding: 1px 6px;
    border-radius: var(--radius-1);
  }
  /* Variant key/value pairs render as a nested definition list inside
     the value cell so each entry sits on its own row instead of the
     legacy comma-joined string the maintainers flagged as a
     "comma-soup" (MR 1998 review pass, finding C2). Row gap is tighter
     than the outer metadata gap so the inner list reads as one block. */
  cts-log-detail-header .logMetaValue .variantList {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: var(--space-1) var(--space-3);
    margin: 0;
  }
  cts-log-detail-header .logMetaValue .variantList dt {
    margin: 0;
  }
  cts-log-detail-header .logMetaValue .variantList dd {
    margin: 0;
  }

  /* Configuration JSON inside the Configuration disclosure. Fixed
     min-height AND max-height so Monaco bounds its inner editor at
     the same size and cannot auto-grow after async mount — opening
     the drawer is therefore a single predictable layout shift
     instead of a disclosure + Monaco-grow combo. cts-json-editor's
     resolveBounds() reads computed min-height and max-height (not
     height) to clamp the inner Monaco surface, so both bounds must
     be set; long configuration JSON then scrolls inside the editor.
     The disclosure was already scrolled into view by
     _openConfigDisclosure() so the scroll surface is immediately
     reachable. See
     docs/plans/2026-05-21-002-fix-log-detail-layout-reflows-plan.md
     U2. */
  cts-log-detail-header .ctsConfigJson {
    display: block;
    min-height: calc(var(--space-6) * 14);
    max-height: calc(var(--space-6) * 14);
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
 * @typedef {object} TestInfo
 * @property {string} testId - Test instance ID.
 * @property {string} testName - Module class name.
 * @property {string} status - One of: RUNNING, WAITING, FINISHED, INTERRUPTED.
 * @property {string} result - Final result (PASSED/FAILED/WARNING/REVIEW/SKIPPED).
 * @property {Array} results - Log entries used for the result/failure summary.
 * @property {string} created - ISO timestamp of test creation.
 * @property {string} description - Human-readable description.
 * @property {string} version - Test module version.
 * @property {object} variant - Variant parameters map.
 * @property {string} planId - Parent plan ID, if the test belongs to one.
 * @property {object} owner - `{ sub, iss }` owner identity (admin only).
 * @property {object} config - Test configuration JSON.
 * @property {object} exposed - Values exported by a running test.
 * @property {string|boolean} publish - Publish mode ("summary", "everything") or falsy.
 * @property {string} summary - Test-level summary. May contain the
 *   `\n\n---\n\n` marker exposed by `./test-summary-split.js` to split
 *   into a description (PASSED hero) and instructions (WAITING hero).
 *   R24 origin: `docs/brainstorms/2026-04-13-cts-ux-improvement-plan-requirements.md`.
 */

/**
 * Header for the log-detail page. Five vertical zones inside the host:
 *
 *   1. Nav row — plan navigation cluster (Plan progress label +
 *      Continue Plan button). Sits immediately below the page-level
 *      breadcrumb (cts-crumb in log-detail.html) so the IA reads
 *      breadcrumb → plan progress → this test's verdict top-to-bottom.
 *   2. Sticky status bar (Region A; unchanged from U2). Verdict + status
 *      pills, count pills, primary action, kebab popover.
 *   3. Terminal-state banner — full-width band immediately below the
 *      sticky bar carrying the verdict ("Test passed" / "Test failed" /
 *      "Test interrupted") on the matching status palette. Rendered
 *      only when the test has reached a terminal phase (PASSED /
 *      FAILED / WARNING / REVIEW / SKIPPED / INTERRUPTED).
 *   4. Hero — the lifecycle-driven dominant zone. Per
 *      `docs/brainstorms/2026-04-26-cts-log-detail-header-hierarchy-requirements.md`:
 *      FAILED / WARNING / REVIEW render the failure list as the hero;
 *      PASSED / SKIPPED render the R24 "About this test" description;
 *      WAITING renders R24 instructions (Start lives in the sticky
 *      status bar, not duplicated in the hero); RUNNING renders the
 *      running-test card content (info alert + exposed values +
 *      browser slot); INTERRUPTED renders the failure list with
 *      the FINAL_ERROR alert pinned at the top of the hero.
 *   5. Region C drawer — two `<details>` disclosures stacked
 *      (Test details, Configuration), both closed by default.
 *
 * Light DOM. Scoped CSS is injected once on first render. All visual
 * styling routes through the OIDF tokens vendored in `oidf-tokens.css`;
 * no Bootstrap classes are emitted.
 *
 * Page-integration contracts preserved verbatim from U1–U8:
 *   - `<cts-test-nav-controls id="testNavControls">` lives in the nav
 *     row directly above the sticky bar (was inside the legacy
 *     vertical action stack; promoted so it stays visible at every
 *     viewport width). The nav row is the first zone rendered by
 *     this component so the page reads "breadcrumb → plan progress
 *     → this test's verdict + actions" top-to-bottom.
 *   - `[data-slot="browser"]` and `[data-slot="error"]` placeholders
 *     remain inside the WAITING / RUNNING / INTERRUPTED hero so
 *     `js/log-detail.js` can inject the browser-URL prompt and the
 *     FINAL_ERROR alert.
 *   - `[data-slot="action-overflow"]` remains inside the sticky bar
 *     for the kebab-popover host.
 *   - `cts-failure-summary` still renders inside the host so
 *     `applyReferences()` in `log-detail.js` can set `.references`
 *     and `.testId` on it for the R32 chip rendering.
 *
 * @property {TestInfo} testInfo - The test info object fetched from
 *   `/api/info`. Reflects the `test-info` attribute when set as a string.
 * @property {boolean} isAdmin - Reveals admin-only rows and actions.
 *   Reflects the `is-admin` attribute.
 * @property {boolean} isPublic - Public (read-only) view hides repeat /
 *   upload / publish actions. Reflects the `is-public` attribute.
 * @fires cts-scroll-to-entry - Bubbled up from the embedded
 *   `cts-failure-summary` child when a failure row is activated, with
 *   `{ detail: { entryId } }`. Bubbles AND is composed.
 * @fires cts-repeat-test - When the Repeat Test button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 * @fires cts-upload-images - When the Upload Images button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 * @fires cts-edit-config - When the Edit configuration button is clicked,
 *   with `{ detail: { testId, planId, config } }`; bubbles. The page-level
 *   handler navigates to schedule-test.html seeded with the supplied
 *   `planId` (preferred) or `config`.
 * @fires cts-share-link - When the Private link button is clicked, with
 *   `{ detail: { testId } }`; bubbles. The page-level handler opens the
 *   private-link expiration modal and POSTs to `/api/info/{testId}/share`.
 * @fires cts-download-log - When the Download Logs button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 * @fires cts-publish - When a Publish (summary / everything) or Unpublish
 *   button is clicked, with `{ detail: { testId, action, mode? } }` where
 *   `action` is `publish` or `unpublish` and `mode` is `summary` or
 *   `everything` (omitted for unpublish); bubbles.
 * @fires cts-start-test - When the Start button is clicked on a running /
 *   waiting test, with `{ detail: { testId } }`; bubbles.
 * @fires cts-stop-test - When the Stop button is clicked on a running /
 *   waiting test, with `{ detail: { testId } }`; bubbles.
 */
class CtsLogDetailHeader extends LitElement {
  static properties = {
    testInfo: { type: Object, attribute: "test-info" },
    isAdmin: { type: Boolean, attribute: "is-admin" },
    isPublic: { type: Boolean, attribute: "is-public" },
  };

  constructor() {
    super();
    this.testInfo = null;
    this.isAdmin = false;
    this.isPublic = false;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  /**
   * Render the variant map as a nested definition list so each key/value
   * pair sits on its own row, replacing the legacy comma-joined string
   * the MR 1998 review pass flagged as a "comma-soup" (finding C2).
   * @param {Record<string, string> | null | undefined} variant - Variant
   *   selections from the runner payload, keyed by parameter name.
   * @returns {ReturnType<typeof html> | typeof nothing} A `<dl>` template
   *   with one `<dt>`/`<dd>` per variant entry, or `nothing` when the
   *   map is empty / not an object.
   */
  _renderVariantList(variant) {
    if (!variant || typeof variant !== "object") return nothing;
    const entries = Object.entries(variant);
    if (entries.length === 0) return nothing;
    return html`
      <dl class="variantList" data-testid="variant-list">
        ${entries.map(
          ([key, value]) => html`
            <dt><span class="mono">${key}</span></dt>
            <dd>${value}</dd>
          `,
        )}
      </dl>
    `;
  }

  _getResultCounts() {
    const counts = {};
    for (const type of RESULT_TYPES) {
      counts[type] = 0;
    }
    if (this.testInfo && Array.isArray(this.testInfo.results)) {
      for (const entry of this.testInfo.results) {
        const key = (entry.result || "").toLowerCase();
        if (key in counts) {
          counts[key]++;
        }
      }
    }
    return counts;
  }

  _getFailures() {
    if (!this.testInfo || !Array.isArray(this.testInfo.results)) return [];
    return this.testInfo.results.filter(
      (entry) =>
        entry.result === "FAILURE" ||
        entry.result === "WARNING" ||
        entry.result === "SKIPPED" ||
        entry.result === "INTERRUPTED",
    );
  }

  _getUploadCount() {
    if (!this.testInfo || !Array.isArray(this.testInfo.results)) return 0;
    return this.testInfo.results.filter((entry) => entry.upload).length;
  }

  _isReadonly() {
    return this.isPublic;
  }

  /**
   * Severity buckets for the failure-hero count headline (e.g.
   * "3 failures, 1 warning"). Lower-cased keys mirror the backend
   * `entry.result` values used everywhere else in the component.
   * @param {Array<{result?: string}>} failures - Failure log entries to bucket.
   * @returns {{failure: number, warning: number, review: number, skipped: number, interrupted: number}} Counts keyed by severity.
   */
  _countFailureSeverities(failures) {
    const counts = {
      failure: 0,
      warning: 0,
      review: 0,
      skipped: 0,
      interrupted: 0,
    };
    for (const entry of failures || []) {
      const key = (entry.result || "").toLowerCase();
      if (key in counts) counts[key]++;
    }
    return counts;
  }

  _formatFailureCountHeadline(counts) {
    const parts = [];
    if (counts.failure > 0) {
      parts.push(`${counts.failure} ${counts.failure === 1 ? "failure" : "failures"}`);
    }
    if (counts.warning > 0) {
      parts.push(`${counts.warning} ${counts.warning === 1 ? "warning" : "warnings"}`);
    }
    if (counts.review > 0) {
      parts.push(`${counts.review} ${counts.review === 1 ? "needs review" : "need review"}`);
    }
    if (counts.skipped > 0) {
      parts.push(`${counts.skipped} skipped`);
    }
    if (counts.interrupted > 0) {
      parts.push(`${counts.interrupted} interrupted`);
    }
    return parts.length > 0 ? parts.join(", ") : "Issues found";
  }

  // ──────────────────────────── event handlers ────────────────────────────

  _handleRepeatTest() {
    this.dispatchEvent(
      new CustomEvent("cts-repeat-test", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _handleUploadImages() {
    this.dispatchEvent(
      new CustomEvent("cts-upload-images", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _handleDownloadLog() {
    this.dispatchEvent(
      new CustomEvent("cts-download-log", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _handleEditConfig() {
    this.dispatchEvent(
      new CustomEvent("cts-edit-config", {
        bubbles: true,
        detail: {
          testId: this.testInfo.testId,
          planId: this.testInfo.planId || null,
          config: this.testInfo.config || null,
        },
      }),
    );
  }

  _handleShareLink() {
    this.dispatchEvent(
      new CustomEvent("cts-share-link", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _dispatchPublish(action, mode) {
    this.dispatchEvent(
      new CustomEvent("cts-publish", {
        bubbles: true,
        detail: {
          testId: this.testInfo.testId,
          action,
          ...(mode ? { mode } : {}),
        },
      }),
    );
  }

  _handlePublishSummary() {
    this._dispatchPublish("publish", "summary");
  }

  _handlePublishEverything() {
    this._dispatchPublish("publish", "everything");
  }

  _handleUnpublish() {
    this._dispatchPublish("unpublish", null);
  }

  _handleStartTest() {
    this.dispatchEvent(
      new CustomEvent("cts-start-test", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _handleStopTest() {
    this.dispatchEvent(
      new CustomEvent("cts-stop-test", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  /**
   * Open the Configuration disclosure in the drawer and scroll it
   * into view. Replaces the legacy `_toggleConfig()` standalone
   * panel — the kebab "View configuration" item now routes to the
   * drawer disclosure that lives at the bottom of the host. Smooth
   * scroll honours `prefers-reduced-motion: reduce` automatically
   * (modern browsers fall back to instant when the user opts out).
   */
  _openConfigDisclosure() {
    const details = /** @type {HTMLDetailsElement | null} */ (
      this.querySelector('[data-testid="drawer-config"]')
    );
    if (!details) return;
    details.open = true;
    details.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  /**
   * Derive a lifecycle phase from `(status, result)`. Used by the
   * status bar, hero, and terminal banner so all three agree on
   * "what kind of state is this?" even when the polling cadence in
   * log-detail.js has not yet flipped `status` from WAITING/RUNNING
   * to FINISHED. The runner sets `result` as soon as a verdict is
   * assigned, so `result` is authoritative for "is this terminal?";
   * `status` only owns the non-terminal branches.
   *
   * Closes MR 1998 finding A1: previously the bar branched on
   * `status` alone, so a test whose `result` had landed but whose
   * `status` still read WAITING kept showing the Start button.
   * @param {TestInfo} test - Test info with `status` and `result`.
   * @returns {string} One of: `waiting`, `running`, `interrupted`,
   *   `finished-pass`, `finished-fail`, `finished-warn`,
   *   `finished-review`, `finished-skip`, `unknown`.
   */
  _derivePhase(test) {
    const status = (test.status || "").toUpperCase();
    const result = (test.result || "").toUpperCase();
    if (status === "INTERRUPTED" || result === "INTERRUPTED") return "interrupted";
    if (TERMINAL_RESULTS.has(result)) {
      if (result === "PASSED") return "finished-pass";
      if (result === "FAILED") return "finished-fail";
      if (result === "WARNING") return "finished-warn";
      if (result === "REVIEW") return "finished-review";
      if (result === "SKIPPED") return "finished-skip";
    }
    if (status === "WAITING") return "waiting";
    if (status === "RUNNING") return "running";
    return "unknown";
  }

  /**
   * "Has this test already executed at least one condition?" Used to
   * choose WAITING-state copy (MR 1998 finding A6): a fresh test with
   * no entries genuinely needs the user to click Start; a test that
   * already has results is waiting on an external callback, so the
   * "Click Start" copy is misleading. The runner writes condition
   * results as it goes, so a non-empty `results` array is the simplest
   * signal that the test has started.
   * @param {TestInfo} test - Test info to probe.
   * @returns {boolean} True when the test has at least one result entry.
   */
  _hasStartedRunning(test) {
    return Boolean(test && Array.isArray(test.results) && test.results.length > 0);
  }

  // ──────────────────────────── status bar (Region A) ────────────────────────────

  _renderStatusBar(test) {
    const phase = this._derivePhase(test);
    if (phase === "waiting") return this._renderWaitingBar(test);
    if (phase === "running") return this._renderRunningBar(test);
    return this._renderFinishedBar(test);
  }

  _renderStatusPill(status) {
    if (!status) return nothing;
    return html`<cts-badge
      variant="${STATUS_BADGE_VARIANTS[status] || "skip"}"
      label="${status}"
    ></cts-badge>`;
  }

  _renderResultPills(counts) {
    return RESULT_TYPES.filter((type) => counts[type] > 0).map(
      (type) =>
        html`<cts-badge
          variant="${RESULT_TYPE_BADGE_VARIANTS[type]}"
          label="${RESULT_TYPE_PILL_GLYPHS[type]} ${counts[type]}"
          data-testid="status-bar-pill-${type}"
        ></cts-badge>`,
    );
  }

  /**
   * Test module name span — placed as the first child of the bar's
   * left cluster (Row 1) so it leads the badges that describe it.
   * Truncates with ellipsis when long names would push the badges
   * onto a new visual line.
   * @param {TestInfo} test - Test info used to source the name.
   * @returns {import('lit').TemplateResult|typeof nothing} The test-name span, or `nothing` when no name is set.
   */
  _renderStatusBarTestNameText(test) {
    const name = test.testName || "";
    if (!name) return nothing;
    return html`<span class="ctsStatusBarTestNameText" title="${name}">${name}</span>`;
  }

  /**
   * Bar row 2 — created datetime, alone. Promoted out of the drawer
   * because it's a "when did this run?" anchor operators glance at
   * constantly; hiding it behind a click would force a disclosure
   * for a fact that should read at a glance.
   * @param {TestInfo} test - Test info used to source the created timestamp.
   * @returns {import('lit').TemplateResult|typeof nothing} The row 2 template, or `nothing` when no created timestamp is set.
   */
  _renderStatusBarCreated(test) {
    if (!test.created) return nothing;
    // The span is the grid item carrying `grid-area: created`; cts-time is
    // display:contents, so it must sit *inside* a placed element rather than
    // being the grid item itself.
    return html`<span class="ctsStatusBarCreated tabular-nums">
      <cts-time mode="compact" value=${test.created}></cts-time>
    </span>`;
  }

  _renderStatusBarOverflowSlot() {
    if (!this.testInfo) return nothing;
    const actions = this._buildOverflowActions();
    if (actions.length === 0) return nothing;
    return html`<div class="ctsStatusBarOverflow" data-slot="action-overflow">
      <cts-action-overflow
        data-testid="status-bar-overflow"
        .actions=${actions}
        @cts-overflow-action=${this._handleOverflowAction}
      ></cts-action-overflow>
    </div>`;
  }

  _buildOverflowActions() {
    const test = this.testInfo;
    if (!test) return [];
    const readonly = this._isReadonly();
    const status = (test.status || "").toUpperCase();
    if (status === "WAITING") return [];

    const uploadCount = this._getUploadCount();
    /** @type {Array<{ id: string, label: string, icon?: string, hidden?: boolean, variant?: string }>} */
    const actions = [
      {
        id: "upload-images",
        label: uploadCount ? `Upload Images (${uploadCount})` : "Upload Images",
        icon: "image-01",
        hidden: readonly,
      },
      {
        id: "view-config",
        label: "View configuration",
        icon: "settings",
      },
      {
        id: "edit-config",
        label: "Edit configuration",
        icon: "edit-pencil-01",
        hidden: readonly,
      },
      {
        id: "download-log",
        label: "Download Logs",
        icon: "save",
        hidden: readonly && test.publish !== "everything",
      },
    ];
    if (!readonly && this.isAdmin && !test.publish) {
      actions.push({
        id: "publish-summary",
        label: "Publish summary",
        icon: "bookmark",
      });
      actions.push({
        id: "publish-everything",
        label: "Publish everything",
        icon: "bookmark",
      });
    }
    if (!readonly && this.isAdmin && test.publish) {
      actions.push({
        id: "unpublish",
        label: "Unpublish",
        icon: "close-circle",
      });
    }
    if (!readonly) {
      actions.push({
        id: "share-link",
        label: "Private link",
        icon: "lock",
      });
    }
    // TODO(U8 follow-up): wire a "Hide log structure rail" / "Show log
    // structure rail" toggle item that flips the cts-log-toc preference
    // (localStorage key "cts-log-toc-rail-enabled") and calls
    // ctsLogToc.setEnabled(...). Tracked in the U8 plan's "Open spec
    // gaps for this unit > [P1] User-preference toggle home" section.
    return actions;
  }

  _handleOverflowAction(event) {
    const id = event.detail && event.detail.actionId;
    switch (id) {
      case "upload-images":
        this._handleUploadImages();
        break;
      case "view-config":
        this._openConfigDisclosure();
        break;
      case "edit-config":
        this._handleEditConfig();
        break;
      case "download-log":
        this._handleDownloadLog();
        break;
      case "publish-summary":
        this._handlePublishSummary();
        break;
      case "publish-everything":
        this._handlePublishEverything();
        break;
      case "unpublish":
        this._handleUnpublish();
        break;
      case "share-link":
        this._handleShareLink();
        break;
      default:
        break;
    }
  }

  _renderWaitingBar(test) {
    // Support-text + primary-button copy switches on whether the test
    // has already executed any conditions. A fresh WAITING test needs
    // a user click on Start; a test that already has results is
    // waiting on an external party (HTTP callback, browser-driven
    // flow), so a "Start" prompt is misleading. The backend status
    // enum (RUNNING / WAITING / INTERRUPTED / FINISHED — see
    // src/main/java/net/openid/conformance/testmodule/TestModule.java)
    // is too coarse to distinguish those two cases on its own.
    // MR 1998 finding A6.
    const started = this._hasStartedRunning(test);
    const supportText = started
      ? "Waiting for external input — no action required"
      : "Waiting for user input";
    return html`
      <div class="ctsStatusBar" id="ctsLogStatusBar" data-testid="status-bar">
        <div class="ctsStatusBarLeft">
          ${this._renderStatusBarTestNameText(test)} ${this._renderStatusPill("WAITING")}
          <span class="ctsStatusBarSupport" data-testid="status-bar-support">${supportText}</span>
        </div>
        <div class="ctsStatusBarMiddle"></div>
        <div class="ctsStatusBarPrimary">
          ${started
            ? nothing
            : html`<cts-button
                variant="primary"
                size="sm"
                icon="play"
                label="Start Test"
                data-testid="status-bar-primary"
                @cts-click=${this._handleStartTest}
              ></cts-button>`}
          ${this._renderStatusBarOverflowSlot()}
        </div>
        ${this._renderStatusBarCreated(test)}
      </div>
    `;
  }

  _renderRunningBar(test) {
    const counts = this._getResultCounts();
    return html`
      <div class="ctsStatusBar" id="ctsLogStatusBar" data-testid="status-bar">
        <div class="ctsStatusBarLeft">
          ${this._renderStatusBarTestNameText(test)} ${this._renderStatusPill("RUNNING")}
          <span class="ctsStatusBarSupport">Test running</span>
        </div>
        <div class="ctsStatusBarMiddle" data-testid="status-bar-pills">
          ${this._renderResultPills(counts)}
        </div>
        <div class="ctsStatusBarPrimary">
          <cts-button
            variant="secondary"
            size="sm"
            icon="stop"
            label="Stop"
            data-testid="status-bar-primary"
            @cts-click=${this._handleStopTest}
          ></cts-button>
          ${this._renderStatusBarOverflowSlot()}
        </div>
        ${this._renderStatusBarCreated(test)}
      </div>
    `;
  }

  _renderFinishedBar(test) {
    const counts = this._getResultCounts();
    const resultVariant = RESULT_BADGE_VARIANTS[test.result] || "skip";
    const readonly = this._isReadonly();
    return html`
      <div class="ctsStatusBar" id="ctsLogStatusBar" data-testid="status-bar">
        <div class="ctsStatusBarLeft">
          ${this._renderStatusBarTestNameText(test)}
          ${test.result
            ? html`<cts-badge variant="${resultVariant}" label="${test.result}"></cts-badge>`
            : nothing}
          ${this._renderStatusPill(test.status)}
        </div>
        <div class="ctsStatusBarMiddle" data-testid="status-bar-pills">
          ${this._renderResultPills(counts)}
        </div>
        <div class="ctsStatusBarPrimary">
          ${!readonly
            ? html`<cts-button
                variant="primary"
                size="sm"
                icon="arrows-reload-01"
                label="Repeat Test"
                data-testid="status-bar-primary"
                @cts-click=${this._handleRepeatTest}
              ></cts-button>`
            : nothing}
          ${this._renderStatusBarOverflowSlot()}
        </div>
        ${this._renderStatusBarCreated(test)}
      </div>
    `;
  }

  // ──────────────────────────── nav row ────────────────────────────

  _renderTestNavControlsRow(test) {
    // `slim` removes the cluster's Return-to-Plan and Repeat-Test
    // buttons. The page-level breadcrumb (cts-crumb in
    // log-detail.html) — which sits immediately above this nav row
    // — already links back to the plan, and the sticky status
    // bar's primary action (rendered directly below this row) already
    // carries Repeat — so emitting them again here would duplicate
    // two prominent affordances inside one viewport.
    return html`
      <div class="ctsNavRow" data-testid="nav-row">
        <cts-test-nav-controls
          id="testNavControls"
          data-testid="test-nav-controls"
          test-id="${test.testId}"
          plan-id="${test.planId || ""}"
          ?readonly=${this._isReadonly()}
          ?public-view=${this.isPublic}
          slim
        ></cts-test-nav-controls>
      </div>
    `;
  }

  // ──────────────────────────── hero (lifecycle-driven) ────────────────────────────

  /**
   * Lifecycle dispatcher for the hero zone. Routes by the derived
   * phase (see `_derivePhase`), which prefers `test.result` over
   * `test.status` whenever a verdict is set — so a polling-lagged
   * test whose `status` still reads WAITING but whose `result` has
   * already landed renders the appropriate finished hero, not the
   * WAITING one. A PASSED test that produced informational WARNING
   * entries still reads as "passed" at the top of the page; the
   * warning count surfaces in the sticky bar's pill cluster and in
   * the log entries below. The hero is the verdict; the warning is
   * annotation.
   * @param {TestInfo} test - Test info that drives phase routing.
   * @returns {import('lit').TemplateResult} The hero template for the current lifecycle state.
   */
  _renderHero(test) {
    const phase = this._derivePhase(test);

    if (phase === "waiting") return this._renderWaitingHero(test);
    if (phase === "running") return this._renderRunningHero(test);
    if (phase === "interrupted") return this._renderInterruptedHero();

    const result = (test.result || "").toUpperCase();
    const mode = HERO_MODES[result] || "summary";
    if (mode === "failures") {
      return this._renderFailureHero(this._getFailures());
    }
    return this._renderSummaryHero(test);
  }

  /**
   * Terminal-state banner shown immediately above the hero whenever the
   * test has reached a terminal phase. Closes MR 1998 findings A2 + A7
   * (Thomas, Almgren): the previous design surfaced the verdict only
   * via a small chip among the log filters, which neither reviewer
   * spotted at a glance. The banner is a sibling of the hero — not
   * nested inside it — so the hero's own content (description for
   * PASSED, failure list for FAILED, etc.) keeps its full vertical
   * weight as the page's primary detail surface.
   * @param {TestInfo} test - Test info used to derive the phase.
   * @returns {import('lit').TemplateResult|typeof nothing} The banner template,
   *   or `nothing` for non-terminal phases (waiting / running / unknown).
   */
  _renderTerminalBanner(test) {
    const phase = this._derivePhase(test);
    const config = TERMINAL_BANNER_BY_PHASE[phase];
    if (!config) return nothing;
    return html`
      <div
        class="ctsTerminalBanner ctsTerminalBanner--${config.palette}"
        role="status"
        data-testid="terminal-banner"
        data-phase="${phase}"
      >
        <cts-icon name="${config.icon}" size="24"></cts-icon>
        <span>${config.headline}</span>
      </div>
    `;
  }

  _renderFailureHero(failures) {
    const counts = this._countFailureSeverities(failures);
    const headline = this._formatFailureCountHeadline(counts);
    return html`
      <div class="ctsHero ctsHero--failures" data-testid="hero-failures">
        <div class="ctsHeroEyebrow">Findings</div>
        <h2 class="ctsHeroHeadline">${headline}</h2>
        ${failures.length > 0
          ? html`<cts-failure-summary
              data-testid="header-failure-summary"
              .failures=${failures}
            ></cts-failure-summary>`
          : html`<div class="ctsHeroPlaceholder"> No conditions ran before the test ended. </div>`}
      </div>
    `;
  }

  /**
   * INTERRUPTED hero — failure-list pattern with the FINAL_ERROR alert
   * pinned at the top via the existing `[data-slot="error"]` placeholder.
   * If no conditions ran before the interruption (failures array empty),
   * the headline reads "No checks completed before the test stopped" —
   * scoped to the absence of findings, not to the interruption verdict
   * (which is already established by the terminal banner above the hero).
   * Reads testInfo via `this._getFailures()`, so no test arg is needed.
   * @returns {import('lit').TemplateResult} The INTERRUPTED hero template.
   */
  _renderInterruptedHero() {
    const failures = this._getFailures();
    const counts = this._countFailureSeverities(failures);
    const headline =
      failures.length > 0
        ? this._formatFailureCountHeadline(counts)
        : "No checks completed before the test stopped";
    return html`
      <div class="ctsHero ctsHero--failures" data-testid="hero-interrupted">
        <div id="runningTestError" data-slot="error" data-testid="running-error-slot"></div>
        <div class="ctsHeroEyebrow">Findings</div>
        <h2 class="ctsHeroHeadline">${headline}</h2>
        ${failures.length > 0
          ? html`<cts-failure-summary
              data-testid="header-failure-summary"
              .failures=${failures}
            ></cts-failure-summary>`
          : nothing}
      </div>
    `;
  }

  /**
   * PASSED / SKIPPED hero — R24 description ("About this test"). When
   * `summary` is empty falls back to `testInfo.description`; when that
   * is also empty renders a quiet "No description available"
   * placeholder so the hero zone never appears empty.
   * @param {TestInfo} test - Test info sourcing `summary` and `description`.
   * @returns {import('lit').TemplateResult} The summary hero template.
   */
  _renderSummaryHero(test) {
    const summarySplit = splitTestSummary(test.summary || "");
    const description = summarySplit.description || test.description || "";

    if (!description) {
      return html`
        <div class="ctsHero ctsHero--summary" data-testid="hero-summary">
          <div class="ctsHeroEyebrow">About this test</div>
          <div class="ctsHeroPlaceholder"> No description available for this test. </div>
        </div>
      `;
    }
    return html`
      <div class="ctsHero ctsHero--summary" data-testid="hero-summary">
        <div class="ctsHeroEyebrow" data-testid="about-test-zone"> About this test </div>
        <div class="ctsHeroBody">${formatDescription(description)}</div>
      </div>
    `;
  }

  /**
   * WAITING hero — R24 instructions ("What you need to do") + the
   * browser-URL slot. The Start CTA lives in the sticky status bar
   * (its primary action for WAITING tests), so the hero does not
   * duplicate it. The slot remains so page-level JS can inject
   * browser-URL prompts during the WAITING window.
   * @param {TestInfo} test - Test info sourcing `summary` and `exposed` values.
   * @returns {import('lit').TemplateResult} The WAITING hero template.
   */
  _renderWaitingHero(test) {
    // Mirror the bar's branch on _hasStartedRunning: a test that has
    // already executed conditions is waiting on an external party,
    // not on a user click. The "Click Start" prompt and the "Action
    // required" eyebrow would both be wrong in that case
    // (MR 1998 finding A6).
    const started = this._hasStartedRunning(test);
    const summarySplit = splitTestSummary(test.summary || "");
    const eyebrow = started ? "Test running" : "Action required";
    const fallbackInstructions = started
      ? "Waiting for an external request — no action required from you."
      : "Click Start Test when you're ready.";
    const instructions = summarySplit.instructions || fallbackInstructions;
    return html`
      <div
        class="ctsHero ctsHero--waiting"
        data-testid="hero-waiting"
        data-waiting-mode="${started ? "external" : "user-action"}"
      >
        <div class="ctsHeroEyebrow" data-testid="user-instructions-zone">${eyebrow}</div>
        <div class="ctsHeroBody">${formatDescription(instructions)}</div>
        ${this._renderExposedValues(test)}
        <div id="runningTestBrowser" data-slot="browser" data-testid="running-browser-slot"></div>
      </div>
    `;
  }

  /**
   * RUNNING hero — info alert + exposed values + browser slot.
   * @param {TestInfo} test - Test info sourcing `exposed` values for the running card.
   * @returns {import('lit').TemplateResult} The RUNNING hero template.
   */
  _renderRunningHero(test) {
    return html`
      <div class="ctsHero ctsHero--running" data-testid="hero-running">
        <div class="ctsHeroEyebrow">Test running</div>
        <cts-alert variant="info">
          Live values from the running test are shown below, along with any URLs that need to be
          visited interactively.
        </cts-alert>
        ${this._renderExposedValues(test)}
        <div id="runningTestBrowser" data-slot="browser" data-testid="running-browser-slot"></div>
      </div>
    `;
  }

  _renderExposedValues(test) {
    if (!test.exposed || Object.keys(test.exposed).length === 0) return nothing;
    return html`
      <div>
        <div class="ctsExposedLabel">Exported values:</div>
        <cts-json-editor
          class="ctsExposedJson"
          readonly
          aria-label="Exported test values"
          .value=${JSON.stringify(test.exposed, null, 2)}
        ></cts-json-editor>
      </div>
    `;
  }

  // ──────────────────────────── drawer (Region C) ────────────────────────────

  _renderDrawer(test) {
    return html`
      <div class="ctsDrawer" data-testid="drawer">
        <details data-testid="drawer-test-details">
          <summary>
            <cts-icon name="chevron-right" size="16"></cts-icon>
            Test details
          </summary>
          <div class="ctsDrawerBody"> ${this._renderMetadataTable(test)} </div>
        </details>
        <details data-testid="drawer-config">
          <summary>
            <cts-icon name="chevron-right" size="16"></cts-icon>
            Configuration
          </summary>
          <div class="ctsDrawerBody">
            <cts-json-editor
              class="ctsConfigJson"
              data-testid="config-json"
              readonly
              aria-label="Test configuration JSON"
              .value=${JSON.stringify(test.config || {}, null, 4)}
            ></cts-json-editor>
          </div>
        </details>
      </div>
    `;
  }

  _renderMetadataTable(test) {
    const variantList = this._renderVariantList(test.variant);
    return html`
      <div class="logMetaTable" data-instance-id="${test.testId}" id="logHeader">
        <div class="logMetaLabel">Test Name:</div>
        <div class="logMetaValue">${test.testName}</div>
        ${variantList !== nothing
          ? html`
              <div class="logMetaLabel">Variant:</div>
              <div class="logMetaValue">${variantList}</div>
            `
          : nothing}
        <div class="logMetaLabel">Test ID:</div>
        <div class="logMetaValue">
          <span class="mono">${test.testId}</span>
        </div>
        <div class="logMetaLabel">Created:</div>
        <div class="logMetaValue tabular-nums">
          <cts-time mode="absolute" value=${test.created}></cts-time>
        </div>
        ${test.description
          ? html`
              <div class="logMetaLabel">Description:</div>
              <div class="logMetaValue">${test.description}</div>
            `
          : nothing}
        ${test.version
          ? html`
              <div class="logMetaLabel">Test Version:</div>
              <div class="logMetaValue">
                <span class="mono">${test.version}</span>
              </div>
            `
          : nothing}
        ${this.isAdmin && test.owner
          ? html`
              <div class="logMetaLabel" data-testid="owner-row"> Test Owner: </div>
              <div class="logMetaValue">
                ${test.owner.sub}${test.owner.iss ? ` (${test.owner.iss})` : ""}
              </div>
            `
          : nothing}
        ${test.planId
          ? html`
              <div class="logMetaLabel">Plan ID:</div>
              <div class="logMetaValue">
                <span class="mono">${test.planId}</span>
              </div>
            `
          : nothing}
      </div>
    `;
  }

  // ──────────────────────────── render + lifecycle ────────────────────────────

  render() {
    if (!this.testInfo) return nothing;
    // Order: nav row (plan progress / Continue Plan) → sticky status
    // bar → verdict banner → hero → drawer. The nav row carries
    // plan-level orientation ("Plan progress: Module N of M"), which
    // sits one level UP the IA hierarchy from the sticky bar's
    // per-test verdict + actions; reading the page top-to-bottom
    // matches the page-level breadcrumb's own scope (plan link →
    // this test) and tightens the visual proximity between
    // breadcrumb and plan-progress orientation.
    return html`
      ${this._renderTestNavControlsRow(this.testInfo)} ${this._renderStatusBar(this.testInfo)}
      ${this._renderTerminalBanner(this.testInfo)} ${this._renderHero(this.testInfo)}
      ${this._renderDrawer(this.testInfo)}
    `;
  }

  firstUpdated() {
    this._observeStatusBar();
  }

  updated(changed) {
    super.updated?.(changed);
    // testInfo flips from null to non-null after the first /api/info fetch
    // resolves, so the bar may not exist on the first render. Re-attempt
    // the observer attach on every update until the bar is in the DOM.
    if (!this._resizeObserver) this._observeStatusBar();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._resizeObserver) {
      this._resizeObserver.disconnect();
      this._resizeObserver = null;
    }
    // Clear the published custom property so a different page mounted
    // afterwards does not inherit a stale measurement.
    document.documentElement.style.removeProperty("--status-bar-height");
  }

  _observeStatusBar() {
    const bar = this.querySelector(".ctsStatusBar");
    if (!bar) return;
    if (this._resizeObserver) return;
    this._publishStatusBarHeight();
    this._resizeObserver = new ResizeObserver(() => this._publishStatusBarHeight());
    this._resizeObserver.observe(bar);
  }

  _publishStatusBarHeight() {
    const bar = this.querySelector(".ctsStatusBar");
    if (!bar) return;
    const height = bar.getBoundingClientRect().height;
    document.documentElement.style.setProperty("--status-bar-height", `${Math.ceil(height)}px`);
  }
}

customElements.define("cts-log-detail-header", CtsLogDetailHeader);

export {};
