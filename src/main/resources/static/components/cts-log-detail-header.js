import { LitElement, html, nothing } from "lit";
import "./cts-icon.js";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-link-button.js";
import "./cts-alert.js";
import "./cts-json-editor.js";
import "./cts-test-nav-controls.js";
import "./cts-failure-summary.js";
import "./cts-action-overflow.js";
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

const STYLE_ID = "cts-log-detail-header-styles";

// Scoped CSS for the log-detail header. All values flow from oidf-tokens.css.
//
// Visual structure (top-to-bottom inside the host element):
//
//   ┌───────────────────────────────────────────────────────────┐
//   │ Sticky status bar (Region A — U2; unchanged)              │  shadow-1
//   ├───────────────────────────────────────────────────────────┤
//   │ Test-nav-controls row (Previous / Test N/M / Next)        │
//   ├───────────────────────────────────────────────────────────┤
//   │ Hero (lifecycle-driven dominant zone)                     │  no chrome
//   │   FAILED/WARNING/REVIEW       → count headline + failure   │  fs-20 head
//   │   INTERRUPTED                 → error slot + failure list  │
//   │   PASSED/SKIPPED              → R24 description prose      │  fs-15 body
//   │   WAITING                     → R24 instructions + Start   │
//   │   RUNNING                     → exposed values + Stop      │
//   ├───────────────────────────────────────────────────────────┤
//   │ Drawer (Region C — two <details> disclosures)             │
//   │   ▸ Test details (metadata table; closed by default)      │
//   │   ▸ Configuration (JSON viewer; closed by default)        │
//   └───────────────────────────────────────────────────────────┘
//
// Each section is divided by a 1px border (no card-within-card chrome).
// The sticky bar carries the only shadow; the rest reads as a flat
// document under the bar.
const STYLE_TEXT = `
  cts-log-detail-header {
    display: block;
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
      "left   middle  primary"
      "name   name    name";
    column-gap: var(--space-3);
    row-gap: var(--space-1);
    align-items: center;
    /* Horizontal padding is owned by the page wrapper now (via the
       .log-page container's padding-inline), so the bar, nav row,
       hero, and drawer all use vertical-only padding here. The leading
       edge of every text block — verdict pill, "ABOUT THIS TEST"
       eyebrow, "Test details" summary — still aligns on a single
       vertical axis because the page wrapper sets the inset for the
       whole stack. */
    padding-block: var(--space-3);
    padding-inline: 0;
    margin-bottom: var(--space-4);
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
  /* Bar row 2 — test name + created datetime side-by-side. The name
     truncates with ellipsis; the date sits to its right at fixed
     width, separated by a middle-dot. The date is promoted out of
     the drawer (where it used to be) because operators glance at
     "when did this run?" constantly — hiding it behind a disclosure
     was a hierarchy regression. */
  cts-log-detail-header .ctsStatusBarTestName {
    grid-area: name;
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
    color: var(--fg-soft);
    font-size: var(--fs-13);
    min-width: 0;
  }
  cts-log-detail-header .ctsStatusBarTestNameText {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    min-width: 0;
    flex: 0 1 auto;
  }
  cts-log-detail-header .ctsStatusBarSeparator {
    color: var(--fg-faint);
    flex: 0 0 auto;
  }
  cts-log-detail-header .ctsStatusBarCreated {
    color: var(--fg-muted);
    white-space: nowrap;
    flex: 0 0 auto;
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

  /* Nav row — sits directly under the bar with the plan navigation
     cluster (#testNavControls). Always visible at every viewport so
     the user can step Previous / Next without opening a drawer.
     The cts-test-nav-controls component was originally designed for
     the legacy vertical action stack (column layout, card chrome,
     full-width buttons). Overriding its inner layout here makes it
     read as a horizontal control row inside the new structure
     without touching the component itself — the override is scoped
     to the .ctsNavRow descendant context only. */
  cts-log-detail-header .ctsNavRow {
    padding: var(--space-2) var(--space-5);
    border-bottom: 1px solid var(--border);
  }
  /* Hide the nav row when the embedded cts-test-nav-controls renders
     nothing — for example, an ad-hoc test (no planId) or the brief
     window before /api/plan resolves (totalCount=0, slim mode, no
     Continue). Without this, the row's padding + border-bottom would
     paint as an empty divider directly under the sticky status bar,
     reading as a broken section break. */
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

  /* Hero — the lifecycle-driven dominant zone. Flat section on the
     page background; no card chrome. Generous padding gives the
     hero its weight. The eyebrow / headline / body type-scale ramp
     replaces the legacy card's internal grid. */
  cts-log-detail-header .ctsHero {
    /* Horizontal padding is owned by the page wrapper (.log-page
       padding-inline); the hero keeps its generous vertical inset so
       it earns its weight via space, not edge chrome. */
    padding-block: var(--space-5);
    padding-inline: 0;
    border-bottom: 1px solid var(--border);
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }
  cts-log-detail-header .ctsHeroEyebrow {
    font-size: var(--fs-12);
    font-weight: var(--fw-bold);
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--fg-soft);
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
    font-size: var(--fs-15);
    line-height: 1.6;
    color: var(--fg);
  }
  cts-log-detail-header .ctsHeroBody p {
    margin: 0 0 var(--space-3);
  }
  cts-log-detail-header .ctsHeroBody p:last-child {
    margin-bottom: 0;
  }
  cts-log-detail-header .ctsHeroPlaceholder {
    color: var(--fg-faint);
    font-style: italic;
    font-size: var(--fs-14);
  }
  cts-log-detail-header .ctsHeroFooter {
    margin-top: var(--space-2);
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
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
    min-height: calc(var(--space-6) * 10);
  }

  /* Drawer (Region C) — two <details> disclosures. Native semantics +
     keyboard a11y; the chevron rotates 90° when [open]. No card chrome;
     borders between disclosures are 1px dividers continuing the
     section rhythm. */
  cts-log-detail-header .ctsDrawer {
    padding: 0;
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
    word-break: break-word;
  }

  /* Configuration JSON inside the Configuration disclosure. Same
     min-height as the legacy stand-alone config panel so Monaco
     virtualisation has room to render. */
  cts-log-detail-header .ctsConfigJson {
    display: block;
    min-height: calc(var(--space-6) * 14);
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
 * Header for the log-detail page. Three vertical zones inside the host:
 *
 *   1. Sticky status bar (Region A; unchanged from U2). Verdict + status
 *      pills, count pills, primary action, kebab popover.
 *   2. Hero — the lifecycle-driven dominant zone. Per
 *      `docs/brainstorms/2026-04-26-cts-log-detail-header-hierarchy-requirements.md`:
 *      FAILED / WARNING / REVIEW render the failure list as the hero;
 *      PASSED / SKIPPED render the R24 "About this test" description;
 *      WAITING renders R24 instructions + Start; RUNNING renders the
 *      running-test card content (info alert + exposed values +
 *      browser slot + Stop); INTERRUPTED renders the failure list with
 *      the FINAL_ERROR alert pinned at the top of the hero.
 *   3. Region C drawer — two `<details>` disclosures stacked
 *      (Test details, Configuration), both closed by default.
 *
 * Light DOM. Scoped CSS is injected once on first render. All visual
 * styling routes through the OIDF tokens vendored in `oidf-tokens.css`;
 * no Bootstrap classes are emitted.
 *
 * Page-integration contracts preserved verbatim from U1–U8:
 *   - `<cts-test-nav-controls id="testNavControls">` lives in the nav
 *     row directly under the sticky bar (was inside the legacy
 *     vertical action stack; promoted so it stays visible at every
 *     viewport width).
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
 * @property {boolean} archived - When `true`, renders the archived-test
 *   info banner above the hero. Set by the page when the runner no longer
 *   holds state for this test (e.g. `/api/runner/{testId}` returns 404).
 *   Reflects the `archived` attribute.
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
    archived: { type: Boolean, attribute: "archived" },
  };

  constructor() {
    super();
    this.testInfo = null;
    this.isAdmin = false;
    this.isPublic = false;
    this.archived = false;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  // ──────────────────────────── formatters ────────────────────────────

  _formatDate(dateStr) {
    if (!dateStr) return "";
    return new Date(dateStr).toString();
  }

  /**
   * Compact "{Mon DD, YYYY}, {h:mm AM}" formatter for the sticky bar
   * row 2 — the long form returned by `_formatDate()` does not fit
   * alongside the truncated test name. Falls back to the empty string
   * for missing input so the caller can short-circuit rendering.
   */
  _formatDateCompact(dateStr) {
    if (!dateStr) return "";
    const d = new Date(dateStr);
    if (Number.isNaN(d.getTime())) return "";
    return d.toLocaleString(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    });
  }

  _formatVariant(variant) {
    if (!variant || typeof variant !== "object") return "";
    return Object.entries(variant)
      .map(([key, value]) => `${key}: ${value}`)
      .join(", ");
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
   */
  _countFailureSeverities(failures) {
    const counts = { failure: 0, warning: 0, review: 0, skipped: 0, interrupted: 0 };
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

  // ──────────────────────────── status bar (Region A) ────────────────────────────

  _renderStatusBar(test) {
    const status = (test.status || "").toUpperCase();
    if (status === "WAITING") return this._renderWaitingBar(test);
    if (status === "RUNNING") return this._renderRunningBar(test);
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
   * Bar row 2 — truncated test name + created datetime. The created
   * timestamp is promoted out of the drawer because it's a "when did
   * this run?" anchor operators glance at constantly; hiding it
   * behind a click would force a disclosure for a fact that should
   * read at a glance.
   */
  _renderStatusBarTestName(test) {
    const name = test.testName || "";
    const created = this._formatDateCompact(test.created);
    if (!name && !created) return nothing;
    return html`<div class="ctsStatusBarTestName">
      ${name
        ? html`<span class="ctsStatusBarTestNameText" title="${name}">${name}</span>`
        : nothing}
      ${name && created
        ? html`<span class="ctsStatusBarSeparator" aria-hidden="true">·</span>`
        : nothing}
      ${created
        ? html`<span class="ctsStatusBarCreated tabular-nums" title="Created ${created}"
            >${created}</span
          >`
        : nothing}
    </div>`;
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
        icon: "bookmark",
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
    return html`
      <div class="ctsStatusBar" id="ctsLogStatusBar" data-testid="status-bar">
        <div class="ctsStatusBarLeft">
          ${this._renderStatusPill("WAITING")}
          <span class="ctsStatusBarSupport">Waiting for user input</span>
        </div>
        <div class="ctsStatusBarMiddle"></div>
        <div class="ctsStatusBarPrimary">
          <cts-button
            variant="primary"
            size="sm"
            icon="play"
            label="Start"
            data-testid="status-bar-primary"
            @cts-click=${this._handleStartTest}
          ></cts-button>
          ${this._renderStatusBarOverflowSlot()}
        </div>
        ${this._renderStatusBarTestName(test)}
      </div>
    `;
  }

  _renderRunningBar(test) {
    const counts = this._getResultCounts();
    return html`
      <div class="ctsStatusBar" id="ctsLogStatusBar" data-testid="status-bar">
        <div class="ctsStatusBarLeft">
          ${this._renderStatusPill("RUNNING")}
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
        ${this._renderStatusBarTestName(test)}
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
                label="Repeat"
                data-testid="status-bar-primary"
                @cts-click=${this._handleRepeatTest}
              ></cts-button>`
            : nothing}
          ${this._renderStatusBarOverflowSlot()}
        </div>
        ${this._renderStatusBarTestName(test)}
      </div>
    `;
  }

  // ──────────────────────────── nav row ────────────────────────────

  _renderTestNavControlsRow(test) {
    // `slim` removes the cluster's Return-to-Plan and Repeat-Test
    // buttons. The page-level breadcrumb (cts-crumb in
    // log-detail.html) already links back to the plan, and the
    // sticky status bar's primary action already carries Repeat — so
    // emitting them again here would duplicate two prominent
    // affordances inside one viewport. The legacy log-detail.html mounts
    // cts-test-nav-controls without `slim`, so that page keeps the full
    // cluster (it has no breadcrumb-driven back nav and no
    // status-bar-primary Repeat to inherit from).
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
   * Lifecycle dispatcher for the hero zone. Routes purely by
   * `status` then by `result` — NOT by whether the results array
   * contains any non-SUCCESS entries. A PASSED test that produced
   * informational WARNING entries still reads as "passed" at the
   * top of the page; the warning count surfaces in the sticky bar's
   * pill cluster and in the log entries below. The hero is the
   * verdict; the warning is annotation.
   */
  _renderHero(test) {
    const status = (test.status || "").toUpperCase();
    const result = (test.result || "").toUpperCase();

    if (status === "WAITING") return this._renderWaitingHero(test);
    if (status === "RUNNING") return this._renderRunningHero(test);
    if (status === "INTERRUPTED") return this._renderInterruptedHero();

    const mode = HERO_MODES[result] || "summary";
    if (mode === "failures") {
      return this._renderFailureHero(this._getFailures());
    }
    return this._renderSummaryHero(test);
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
   * the headline reads "Test was interrupted before any check ran".
   * Reads testInfo via `this._getFailures()`, so no test arg is needed.
   */
  _renderInterruptedHero() {
    const failures = this._getFailures();
    const counts = this._countFailureSeverities(failures);
    const headline =
      failures.length > 0
        ? this._formatFailureCountHeadline(counts)
        : "Test was interrupted before any check ran";
    return html`
      <div class="ctsHero ctsHero--failures" data-testid="hero-interrupted">
        <div id="runningTestError" data-slot="error" data-testid="running-error-slot"></div>
        <cts-alert variant="danger">
          <b>This test was interrupted.</b> See the error details above.
        </cts-alert>
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
   */
  _renderSummaryHero(test) {
    const summarySplit = splitTestSummary(test.summary || "");
    const description = summarySplit.description || test.description || "";

    if (!description) {
      return html`
        <div class="ctsHero ctsHero--summary" data-testid="hero-summary">
          <div class="ctsHeroEyebrow">About this test</div>
          <div class="ctsHeroPlaceholder">No description available for this test.</div>
        </div>
      `;
    }
    return html`
      <div class="ctsHero ctsHero--summary" data-testid="hero-summary">
        <div class="ctsHeroEyebrow" data-testid="about-test-zone">About this test</div>
        <div class="ctsHeroBody">${description}</div>
      </div>
    `;
  }

  /**
   * WAITING hero — R24 instructions ("What you need to do") + the
   * browser-URL slot + Start CTA. The slot remains so page-level JS
   * can inject browser-URL prompts during the WAITING window.
   */
  _renderWaitingHero(test) {
    const summarySplit = splitTestSummary(test.summary || "");
    const instructions = summarySplit.instructions || "Click Start when you're ready.";
    return html`
      <div class="ctsHero ctsHero--waiting" data-testid="hero-waiting">
        <div class="ctsHeroEyebrow" data-testid="user-instructions-zone">Action required</div>
        <div class="ctsHeroBody">${instructions}</div>
        ${this._renderExposedValues(test)}
        <div id="runningTestBrowser" data-slot="browser" data-testid="running-browser-slot"></div>
        <div class="ctsHeroFooter">
          <cts-button
            variant="primary"
            size="sm"
            icon="play"
            label="Start"
            data-testid="start-btn"
            @cts-click=${this._handleStartTest}
          ></cts-button>
        </div>
      </div>
    `;
  }

  /**
   * RUNNING hero — info alert + exposed values + browser slot + Stop.
   * The Stop button is intentionally redundant with the sticky bar's
   * primary action; for live runs the action *is* the job.
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
        <div class="ctsHeroFooter">
          <cts-button
            variant="secondary"
            size="sm"
            icon="stop"
            label="Stop"
            data-testid="stop-btn"
            @cts-click=${this._handleStopTest}
          ></cts-button>
        </div>
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
          <div class="ctsDrawerBody">${this._renderMetadataTable(test)}</div>
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
    const variantStr = this._formatVariant(test.variant);
    return html`
      <div class="logMetaTable" data-instance-id="${test.testId}" id="logHeader">
        <div class="logMetaLabel">Test Name:</div>
        <div class="logMetaValue">${test.testName}</div>
        ${variantStr
          ? html`
              <div class="logMetaLabel">Variant:</div>
              <div class="logMetaValue">${variantStr}</div>
            `
          : nothing}
        <div class="logMetaLabel">Test ID:</div>
        <div class="logMetaValue">${test.testId}</div>
        <div class="logMetaLabel">Created:</div>
        <div class="logMetaValue tabular-nums">${this._formatDate(test.created)}</div>
        ${test.description
          ? html`
              <div class="logMetaLabel">Description:</div>
              <div class="logMetaValue">${test.description}</div>
            `
          : nothing}
        ${test.version
          ? html`
              <div class="logMetaLabel">Test Version:</div>
              <div class="logMetaValue">${test.version}</div>
            `
          : nothing}
        ${this.isAdmin && test.owner
          ? html`
              <div class="logMetaLabel" data-testid="owner-row">Test Owner:</div>
              <div class="logMetaValue">
                ${test.owner.sub}${test.owner.iss ? ` (${test.owner.iss})` : ""}
              </div>
            `
          : nothing}
        ${test.planId
          ? html`
              <div class="logMetaLabel">Plan ID:</div>
              <div class="logMetaValue">${test.planId}</div>
            `
          : nothing}
      </div>
    `;
  }

  // ──────────────────────────── render + lifecycle ────────────────────────────

  render() {
    if (!this.testInfo) return nothing;
    return html`
      ${this._renderStatusBar(this.testInfo)} ${this._renderTestNavControlsRow(this.testInfo)}
      ${this._renderArchivedBanner()} ${this._renderHero(this.testInfo)}
      ${this._renderDrawer(this.testInfo)}
    `;
  }

  _renderArchivedBanner() {
    if (!this.archived) return nothing;
    return html`
      <cts-alert variant="info" dismissible id="runningTestArchived" data-testid="archived-banner">
        <b>This test is no longer running.</b> This log has been archived and can be viewed or
        downloaded.
      </cts-alert>
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
