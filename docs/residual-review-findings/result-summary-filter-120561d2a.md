# Residual Review Findings — result-summary filter

Source: `ce-code-review mode:autofix` over `80325cfa8..HEAD`
(feat: log-result-summary-filter; commits 759b255af, 2f1db66e5).
8 persona reviewers. The `safe_auto` / low-risk fixes were applied in
commit `fix(review): apply autofix feedback`. The items below were left
for a maintainer decision (not auto-applied).

## Residual Review Findings

- **[P2] downstream-resolver — `src/main/resources/static/components/cts-badge.js` (`_render` → `replaceChildren`) / `cts-log-viewer.js` (`_toggleFilter` focus restore).**
  Poll-driven label update drops keyboard focus. When a count badge's `label`
  changes on the 3s poll (a running test), `cts-badge` rebuilds its inner
  `role="button"` span via `replaceChildren`, detaching the focused element →
  focus falls to `<body>`. The toggle/clear paths restore focus, but a
  subsequent poll re-render has no recovery. Flagged by julik-frontend-races
  (P1) and adversarial (P2) — cross-reviewer corroborated. Fix touches the
  shared `cts-badge` render model (update label text in place when only
  non-structural attrs change, OR re-focus the active `[data-result]` badge in
  `cts-log-viewer.updated()` after any `_entries` re-render). Deferred because
  it changes a shared primitive's render strategy — maintainer call.

- **[P1] human — `src/main/resources/static/components/cts-log-viewer.js`.**
  File crossed the 1000-line maintainability threshold (now ~1130). The filter
  concern (state + toggle/clear/announce/predicate/empty-state + the 5 CSS
  blocks) is self-contained and could be extracted to a `cts-log-viewer-filter`
  module. Out of this feature's scope; record for a future refactor.

- **[P3] review-fixer — `src/main/resources/static/components/cts-log-viewer.stories.js`.**
  Pre-block flat-entry filtering path (the `currentBlockId === null` branch of
  `flushBlock` under an active filter) has no test — no fixture has flat
  entries before the first `startBlock`. The correctness reviewer traced the
  path and confirmed it is correct; only the regression test is missing.

- **[advisory] human — render purity.** `_filterAnnouncement()` consumes the
  one-shot `_announceFilterChange` flag inside the render path. Sound under
  Lit's single-render-per-cycle cadence (and tested), but a pure-state design
  (set `_announcementText` in the handlers, read-only in render — identical
  text across polls does not re-announce) would remove the render side-effect.
  Noted by maintainability, project-standards, correctness, julik, adversarial
  as a low-priority residual risk.

## Acknowledged (no action — already covered)

- Deep-link / failure-summary / TOC jump into a filtered-out target silently
  no-ops (adversarial P2). This is the plan's explicitly-deferred **R9**
  (reveal-on-navigate); `clearFilters()` is the documented mitigation.

## Dropped (false positive)

- cts-badge "listener leak" on attribute change (julik P1, conf 100). Detached
  spans are garbage-collected — nothing retains them (`cts-badge` stores no
  span reference; captured children are moved to the new span). The adversarial
  reviewer examined the same `replaceChildren` and flagged only the focus drop,
  not a leak.
