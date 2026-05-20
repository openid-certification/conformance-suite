---
name: clickable-stat-tiles-with-url-filters
description: Make the four dashboard cts-stat tiles clickable navigation affordances and add URL query-param filtering to logs.html with a clearable filter chip
status: active
created: 2026-05-19
type: feat
---

# feat: Clickable Stat Tiles with URL Filters

## Summary

The four `<cts-stat>` tiles on the home dashboard ("Your test plans", "Your test logs", "Logs in progress", "Logs with failures") are passive numerals today. Make them clickable so each tile navigates to the corresponding list view, with the two "subset" tiles ("in progress", "with failures") routing through new URL query-param filters on `logs.html`. Add a clear-filter affordance so users can return to the full unfiltered list with one click.

URL surface: `logs.html?status=running,waiting` and `logs.html?result=failed,unknown` — lowercase comma-separated enum values, layered on top of the existing `?public=true` convention.

## Problem Frame

The home dashboard summarizes four counts but offers no way to drill into the underlying lists. Users who see "3 logs in progress" or "5 logs with failures" have to navigate manually to `logs.html` and then scan or search visually to find the rows the tile was counting. The tile is providing information without an affordance to act on it.

A clickable tile turns the count into a navigation entry point. The "your plans" and "your logs" tiles can link directly to the existing default views. The "in progress" and "with failures" tiles need filtered views, which requires adding URL-driven filtering to `logs.html`.

URL-driven filters are the right shape because:
- They are shareable, bookmarkable, and survive back/forward navigation.
- They compose with the existing `?public=true` convention without overloading it.
- They give an obvious place for the "clear filter" affordance to point (the unfiltered URL).

## Requirements

| ID | Requirement |
|----|-------------|
| R1 | Each of the four `<cts-stat>` tiles on `cts-dashboard` is a clickable navigation target with visible hover, focus-visible, and keyboard activation affordances |
| R2 | "Your test plans" navigates to `plans.html`; "Your test logs" navigates to `logs.html`; "Logs in progress" navigates to `logs.html?status=running,waiting`; "Logs with failures" navigates to `logs.html?result=failed,unknown` |
| R3 | `logs.html` reads `?status=` and `?result=` query parameters, parses comma-separated lowercase enum values, and renders only rows whose `status` (case-insensitive) matches any value in `?status=` AND whose `result` matches any value in `?result=` |
| R4 | When any URL filter (`?status=` or `?result=`) is active on `logs.html`, a clearable chip is rendered above the table summarizing the active filter; clicking it navigates to `logs.html` (preserving `?public=true` if present, dropping the filter params) |
| R5 | URL filters compose with the existing `?public=true` parameter — e.g., `logs.html?public=true&result=failed,unknown` filters the published-logs view |
| R6 | The dashboard tiles preserve their existing visual layout (4-column grid, stat numeral on top, label below); the click affordance is added via wrapper styling, not by changing the `<cts-stat>` component's internal markup |
| R7 | Unknown enum tokens in `?status=` or `?result=` are silently ignored (e.g., `?status=running,bogus` filters to RUNNING and discards `bogus`); if all tokens for a key are unknown, that key is treated as inactive |

## Scope Boundaries

**In scope:**
- Wrapping the four dashboard stat tiles in anchors with hrefs.
- Adding URL-param parsing to `logs.html` for `status` and `result`.
- Switching `cts-data-table` to client-side mode when a filter is active so the filter applies to the full dataset (subject to the existing 1000-row cap).
- Rendering a clear-filter chip above the logs table when filters are active.
- Updating Storybook stories and play tests for the dashboard.
- Adding e2e coverage for tile hrefs and filter behavior.

**Out of scope:**
- Backend filtering. The `/api/log` endpoint stays as-is (only `?public=` and pagination params). The "minimal backend touching" project convention (`feedback_minimal_backend_touching.md`) plus the existing dashboard's client-side filter convention (capped at 1000 rows via `STATS_PAGE_SIZE`) both point to a frontend-only solution.
- Adding URL filters to `plans.html`. The "Your test plans" tile links to the default plans view; no filtered plans view is needed.
- General-purpose filter UI (multi-status picker, free-form filter builder). Only the two filters required by the dashboard tiles.

### Deferred to Follow-Up Work
- Extract the clear-filter chip into a reusable `<cts-filter-chip>` component if a second consumer appears (e.g., plans.html gains a filter).
- Backend `?status=` / `?result=` query params if the 1000-row cap becomes a practical limitation for users with very large log histories.
- Synchronizing the filter back into the URL when the user types in the cts-data-table search box (filter + search interaction).

## Key Technical Decisions

### Filter location: client-side, not backend

The backend `/api/log` accepts only `?public=` today (see `src/main/java/net/openid/conformance/logging/LogApi.java:113-136`). The dashboard already computes the in-progress and failures counts by fetching up to 1000 logs and filtering in memory (`src/main/resources/static/components/cts-dashboard.js:309-404`, with `STATS_PAGE_SIZE = 1000`). Reusing that exact convention on `logs.html` keeps the change frontend-only, mirrors the precedent the user already sees on the dashboard, and avoids touching Mongo query criteria — consistent with `feedback_minimal_backend_touching.md`. The "+" overflow indicator the dashboard already uses serves as the natural truncation signal.

### URL shape: lowercase comma-separated enum values

Two reasonable shapes existed: semantic aliases (`?status=in-progress`) or raw enum values (`?status=running,waiting`). Raw enum values won because:
- They map 1:1 to `TestModule.Status` / `TestModule.Result` enums (`src/main/java/net/openid/conformance/testmodule/TestModule.java`), so future filter additions (e.g., interrupted, skipped) don't require a new alias vocabulary.
- They compose naturally if the user edits the URL by hand: `?status=running` filters more narrowly than `?status=running,waiting`.
- The user-facing label belongs in the clear-filter chip, where it can be descriptive prose without polluting the URL.

Lowercase chosen over uppercase enum names for URL aesthetics; parsing is case-insensitive.

### Wrapper anchor, not modified `<cts-stat>` component

The CLAUDE.md "Badges" affordance decision tree (CLAUDE.md:302-342, generalized) says: when a wrapper element provides visible affordance (hover state, focus ring, transform), the inner component stays read-only. The existing `.oidf-dashboard-tile` anchor (`src/main/resources/static/components/cts-dashboard.js:139-149`) already encodes that affordance — hover border, shadow, -1px transform, focus-visible outline. Reuse that exact chrome for the stat tile, and `<cts-stat>` itself needs no `interactive` / `clickable` attribute.

### Reset chip: inline in `logs.html`, not a new component

A new `<cts-filter-chip>` component would be a clean abstraction but has exactly one consumer right now. Keeping the chip as ~30 lines of inline HTML + JS in `logs.html` matches the team's "no abstraction without two consumers" preference and defers the extraction question. Visual styling reuses tokens; the markup approximates the existing `.oidf-dt-search-filter-reset` pattern (`src/main/resources/static/components/cts-data-table.js:254-277`).

### Filter mode switching on `cts-data-table`

`cts-data-table` already supports both `server-side` and client-side modes (`src/main/resources/static/components/cts-data-table.js:430-460`). When `?status=` or `?result=` is present on `logs.html`, the page script flips to client-side mode, fetches up to 1000 rows once via `/api/log?length=1000` (preserving `?public=true` if present), filters the array client-side, and passes the filtered rows to `cts-data-table.rows`. When no URL filter is active, the page stays in server-side mode exactly as today.

This dual-mode approach means the unfiltered logs.html behavior is byte-identical to the pre-change behavior — only the filtered code path is new.

## High-Level Technical Design

```text
URL: /logs.html?result=failed,unknown
  │
  ▼
logs.html DOMContentLoaded
  │
  ├── URLSearchParams reads `public`, `status`, `result`
  │
  ├── if (status || result) {
  │     mode = "client-side"
  │     fetch("/api/log?length=1000" + publicSuffix)
  │       → filter rows where status ∈ statusSet AND result ∈ resultSet
  │       → cts-data-table.rows = filteredRows
  │     render clear-filter chip above the table
  │   } else {
  │     mode = "server-side"   // current behavior, unchanged
  │     cts-data-table.ajaxUrl = "/api/log" + publicSuffix
  │   }
  │
  ▼
clear-chip click → navigate to /logs.html + publicSuffix (filter params dropped)
```

*This sketch communicates the runtime decision tree at page load. It is directional guidance for review, not implementation specification.*

## Implementation Units

### U1. Wrap dashboard stat tiles in clickable anchors

**Goal:** Each of the four stat tiles becomes an `<a>` element with a target href, hover/focus affordance, and keyboard activation. The visual layout (numeral, label, 4-column grid) stays identical.

**Requirements:** R1, R2, R6

**Dependencies:** None

**Files:**
- `src/main/resources/static/components/cts-dashboard.js` (modify `_renderStats` and the `StatTile` typedef to add `href` and optionally `tone`/`delta` carried through)
- `src/main/resources/static/components/cts-dashboard.stories.js` (extend play tests)
- `src/main/resources/static/components/cts-stat.js` (no behavioral change; verify the existing markup composes inside an `<a>` without breaking layout)

**Approach:**
- Extend the `StatTile` typedef to include `href: string`.
- In `_placeholderStats()` and `_buildStats()`, set each tile's `href`:
  - `plans` tile → `"plans.html"`
  - `logs` tile → `"logs.html"`
  - `in-progress` tile → `"logs.html?status=running,waiting"`
  - `failed` tile → `"logs.html?result=failed,unknown"`
- Change the `_renderStats()` markup so the existing `<div class="oidf-dashboard-stat-tile">` becomes `<a class="oidf-dashboard-stat-tile" href="${tile.href}">` and the same `<cts-stat>` is rendered inside. Add anchor styling on `.oidf-dashboard-stat-tile` mirroring `.oidf-dashboard-tile` (text-decoration: none, hover border, focus-visible ring, -1px transform on hover).
- The placeholder tiles (while stats are loading) should also be anchors so layout is stable — they navigate to the same destinations.
- Add an `aria-label` per tile that combines label + value (e.g., `aria-label="Your test logs: 12 — view all"`) so screen readers don't repeat the visual layout twice.

**Patterns to follow:**
- `.oidf-dashboard-tile` anchor styling at `cts-dashboard.js:123-149` is the prior art for the affordance chrome.
- The existing per-tile `data-stat-key` attribute should be preserved on the new `<a>` for e2e selectors.

**Test scenarios:**
- Each tile renders as `<a>` with the correct `href` value (4 cases).
- Each tile has hover, focus-visible, and active states (visual; covered by play function `userEvent.hover` + screenshot or by querying computed style).
- Placeholder state (em-dash) still renders as a clickable anchor with the correct href.
- Unauthenticated user does not see the stats row at all (existing behavior preserved).
- `aria-label` is present and includes both the label and the numeric value.
- Keyboard: Tab moves focus through all four tiles in order; Enter on a focused tile triggers navigation (asserted via a click handler or `keyDown` simulation).

**Verification:** All Storybook play tests pass; the dashboard visually matches the pre-change layout at 4-column, 2-column, and 1-column breakpoints; tile hover/focus states are visible.

---

### U2. Read URL filters in `logs.html` and apply client-side filtering

**Goal:** When `?status=` or `?result=` is present in the URL, `logs.html` fetches up to 1000 logs once, filters them client-side, and renders the filtered rows in `cts-data-table` in client-side mode. When neither param is present, current behavior is unchanged (server-side mode, page-by-page fetch).

**Requirements:** R3, R5, R7

**Dependencies:** U1 is independent — this unit can land before U1 if desired, but the user-visible feature requires both.

**Files:**
- `src/main/resources/static/logs.html` (the inline `<script type="module">` block at lines 95–280)

**Approach:**
- Extend the existing URL-param block at `logs.html:104-106` to also parse `status` and `result`.
- Define a small helper inside the script: `parseFilterSet(raw, validValues)` that lowercases, splits on `,`, trims each token, and intersects with the set of known enum values (case-insensitive). Returns either a `Set<string>` of valid uppercase tokens or `null` if no valid tokens remain.
- After parsing, branch in `loadAvailableLogs`:
  - **No filter:** keep current server-side behavior exactly as today.
  - **Filter active:** set `table.setAttribute("server-side", "")` is *not* set; instead set `table.serverSide = false` (or omit the attribute). Fetch `/api/log?length=1000` (preserve `?public=true`), normalize the response (`{data, recordsTotal}` envelope OR raw array — same dual-shape handling cts-dashboard uses at lines 325-344), filter the array, and assign to `table.rows`.
- The filter predicate: a row passes if (statusSet === null OR statusSet.has(row.status)) AND (resultSet === null OR resultSet.has(row.result)). Comparison uses the uppercase enum values stored on the row (TestModule.Status / TestModule.Result emit uppercase strings).
- The `STATS_PAGE_SIZE = 1000` constant from `cts-dashboard.js` is the prior art for the cap. Define a local `MAX_FILTERED_LOGS = 1000` in `logs.html` (do not import from a component) and surface truncation in U3's chip ("Showing 1000+; refine filter to narrow further").

**Patterns to follow:**
- The dual-shape response normalization in `cts-dashboard.js:325-344`.
- The 1000-row cap convention from `cts-dashboard.js:19` (with the `+` overflow signal).
- `cts-data-table`'s client-side mode (see its JSDoc at `src/main/resources/static/components/cts-data-table.js:46-56`).

**Test scenarios:**
- `?status=running,waiting` filters the displayed rows to those with `status === "RUNNING" || status === "WAITING"` (e2e with mocked API).
- `?result=failed,unknown` filters to rows with `result === "FAILED" || result === "UNKNOWN"` (e2e).
- `?status=running&result=failed` (combined) filters to rows matching BOTH (e2e).
- `?status=running,bogus` filters to RUNNING only (unknown token silently dropped) — R7 verification.
- `?status=bogus` (no valid tokens) treats the param as inactive — i.e., status filter is not applied, but if `?result=` is also present, that still applies.
- `?public=true&result=failed,unknown` filters published-logs view to failures only — R5 verification.
- No filter params → server-side mode, no API change (regression check: existing logs.html behavior).
- 1000-row truncation: when the fetch returns exactly 1000 rows, the truncation signal renders in the chip (U3).
- Case-insensitive parsing: `?status=RUNNING` works identically to `?status=running`.

**Verification:** Manual smoke test of all four dashboard tiles → land on a correctly filtered logs page; e2e specs assert filtered row counts; the unfiltered logs.html page-load behavior is byte-identical to the pre-change behavior.

---

### U3. Render a clear-filter chip on `logs.html` when filters are active

**Goal:** When `?status=` or `?result=` is present in the URL, render a small filter chip above the cts-data-table summarizing what is filtered. Clicking the chip navigates to the unfiltered URL (preserving `?public=true`).

**Requirements:** R4

**Dependencies:** U2 (the chip needs the parsed filter state)

**Files:**
- `src/main/resources/static/logs.html` (markup + script for the chip)
- `src/main/resources/static/css/oidf-app.css` or `layout.css` (add `.oidf-page-filter-chip` rules) — OR keep styles inline in a `<style>` block in logs.html if scoped enough

**Approach:**
- Add a placeholder div above the `#logsListingMount` element: `<div id="activeFilterChip"></div>`.
- When the URL parser in U2 detects an active filter, populate that div with markup approximating the cts-data-table search-filter-reset pill:
  - Icon: `cts-icon name="filter"` (or "tag" — confirm via `ls src/main/resources/static/vendor/coolicons/icons/`)
  - Label: human-readable summary, e.g., "Status: running or waiting" or "Result: failed or unknown" — derived from the parsed sets via a small helper. Join with " · " when both filters are active.
  - Trailing `cts-icon name="x" size="16"` for the dismiss affordance.
  - Optional trailing count: "(N matches)" — populated after the fetch resolves.
  - `aria-label="Clear active filters"` on the wrapping button.
- The chip is a `<button type="button">` (not an anchor) because the action is a state change, not a navigation in the traditional REST sense — though the implementation `window.location` navigates. Use `<button>` for keyboard semantics; on click, set `window.location = "logs.html" + (isPublic ? "?public=true" : "")`.
- Style: inline-flex, border-radius var(--radius-pill), background var(--bg-elev), border 1px solid var(--border), padding var(--space-2) var(--space-3), gap var(--space-2). Hover state: border-color var(--border-strong); focus-visible: box-shadow var(--focus-ring).
- When truncation is detected (filtered count ≥ 1000), append a muted secondary line under the chip: "Showing the first 1000 matches. Refine the filter to narrow further."

**Patterns to follow:**
- `.oidf-dt-search-filter-reset` styling at `src/main/resources/static/components/cts-data-table.js:254-277` is the visual prior art. Match the pill silhouette but use page-level class names (`.oidf-page-filter-chip` not `.oidf-dt-search-filter-*`) so the rules don't accidentally inherit cts-data-table's scoping.
- Token-driven styling — no hardcoded colors, padding, or radii.

**Test scenarios:**
- Chip is absent when no filter params are present (regression).
- Chip is present and labeled correctly for `?status=running,waiting` (e.g., "Status: running or waiting").
- Chip is present and labeled correctly for `?result=failed,unknown`.
- Chip shows both filter facets when both are active, joined with " · ".
- Clicking the chip navigates to `logs.html` (filter params dropped, public preserved if set).
- Keyboard: Tab focuses the chip, Enter activates it.
- Truncation message is shown only when the fetched dataset hit the 1000-row cap.
- `aria-label` reads as "Clear active filters" for assistive tech.

**Verification:** E2E test asserts chip presence, label content, click behavior, and URL navigation; manual visual check that the chip integrates with the existing logs page layout.

---

### U4. Storybook play tests and e2e coverage

**Goal:** Lock the new behavior under test so regressions are caught. CLAUDE.md mandates Storybook play function tests for all components; the URL filter behavior is page-level and goes to e2e.

**Requirements:** R1–R7 (verification layer)

**Dependencies:** U1, U2, U3

**Files:**
- `src/main/resources/static/components/cts-dashboard.stories.js` (extend existing stat tile stories with play tests asserting hrefs)
- `frontend/e2e/home.spec.js` (assert stat tile hrefs)
- `frontend/e2e/logs.spec.js` (CREATE new file; if a logs spec exists, extend it instead — check `ls frontend/e2e/ | grep logs` before creating)
- `frontend/e2e/fixtures/logs.js` (CREATE if missing; provide mocked log rows covering RUNNING, WAITING, FINISHED + FAILED, PASSED, UNKNOWN states)
- `frontend/e2e/helpers/routes.js` (extend if needed to mock `/api/log?length=1000` for the filter tests)

**Approach:**
- **Storybook (cts-dashboard.stories.js):** Add a play test on the existing `AuthenticatedWithStats` variant (or whichever variant renders the stats row) that:
  - Queries each `[data-stat-key]` anchor by key.
  - Asserts the `href` attribute matches the expected target for each of the 4 tiles.
  - Verifies the anchor has an `aria-label` containing both the label and the value.
- **home.spec.js:** Extend an existing dashboard test (or add a new `test("stat tiles link to the right destinations")`) that asserts the four hrefs after the stats fetch resolves.
- **logs.spec.js (new):**
  - `test("renders all rows with no filter")` — baseline.
  - `test("?status=running,waiting filters to in-progress rows")` — load page with URL filter, assert filtered count, assert chip is visible.
  - `test("?result=failed,unknown filters to failure rows")` — same shape.
  - `test("combined ?status and ?result apply both filters")`.
  - `test("clicking the clear-filter chip navigates to the unfiltered URL")`.
  - `test("?public=true&result=failed,unknown filters the published-logs view")`.
  - `test("unknown filter tokens are silently dropped")` — covers R7.

**Patterns to follow:**
- Existing play function patterns in cts-dashboard.stories.js (already extensive — see stories 1-10).
- `frontend/e2e/home.spec.js` for fixture wiring and route mocking conventions.
- The `wrapDataTablesResponse()` helper in `frontend/e2e/helpers/` for the `/api/log` envelope shape (mentioned in CLAUDE.md "Frontend E2E Tests" section).
- The `setupFailFast()` ordering rule from CLAUDE.md — call before specific routes.

**Test scenarios (meta-level — this unit's "test scenarios" ARE tests):**
- See per-test list above; each is a concrete e2e or play function assertion.

**Verification:**
- `cd frontend && npm run test:e2e` passes locally (excluding the pre-existing baseline failures documented in `feedback_e2e_pre_existing_failures_2026_05_18.md`).
- `mcp__storybook-mcp__run-story-tests` on the dashboard stories passes (mind the pre-existing flakes documented in `feedback_storybook_pre_existing_flakes.md` — verify against HEAD baseline).
- The full frontend quality gate (`cd frontend && npm run test:ci`) is no worse than the pre-existing baseline.

---

## System-Wide Impact

| Surface | Impact |
|---------|--------|
| Backend API | None. `/api/log` and `/api/plan` unchanged. |
| `cts-dashboard` component | Stat tiles become anchors. Layout preserved. New `href` field on `StatTile` typedef. |
| `cts-stat` component | No changes. The component composes inside an anchor without modification. |
| `logs.html` | New URL-param parsing, new client-side filter branch, new clear-filter chip. Unfiltered path is byte-identical to pre-change behavior. |
| `plans.html` | No changes. |
| `cts-data-table` | No changes. Used in both server-side and client-side modes as it already supports. |
| Storybook | Updated play tests on cts-dashboard. |
| E2E | Extended home.spec.js, new (or extended) logs.spec.js, new logs fixture. |
| URL shape | New: `?status=<csv>` and `?result=<csv>` on logs.html. Composes with existing `?public=true`. |
| Browser history | New filtered URLs are bookmarkable, shareable, survive back/forward — desirable. |
| Accessibility | New `aria-label`s on stat tiles include the value; clear-filter chip has `aria-label="Clear active filters"`. |

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| 1000-row cap silently hides matching logs on large datasets | Truncation message in the clear-filter chip when the cap is hit; documented in the plan as a follow-up trigger for backend filtering |
| Client-side filter de-syncs from cts-data-table search box (user searches inside a URL-filtered view) | The cts-data-table search runs on the client-side `rows` array in client-side mode, so search+filter compose naturally. No additional code needed. Covered by an e2e test in U4. |
| URL bookmarks become stale when the underlying data shifts | Acceptable. The URL is a query, not a data identifier. Same semantics as `?public=true` today. |
| Unfiltered logs.html behavior regresses (the larger code change of mode-switching breaks the default path) | U2 explicitly keeps the unfiltered branch byte-identical to today. E2E baseline test asserts row count and server-side mode in the no-filter case. |
| Case sensitivity of enum comparisons (URL is lowercase, backend emits uppercase) | The filter helper in U2 normalizes both sides via `.toUpperCase()`. Covered by test scenario. |
| Storybook play tests flake when querying anchor hrefs before the stats fetch resolves | Use the existing `await waitFor(() => element.href.includes("logs.html"))` pattern from existing dashboard play tests; the stories already wait for stats to populate before assertions. |

## Dependencies / Prerequisites

- No new npm packages.
- No new vendored assets (icons already exist in coolicons: `x`, `filter` / `tag`).
- No backend changes.
- No new style tokens — all colors, radii, and spacing already exist in `oidf-tokens.css`.

## Verification (Plan-Level)

The feature is complete when:
1. All four dashboard tiles are clickable; clicking each lands on the correct view.
2. `logs.html?status=running,waiting` and `logs.html?result=failed,unknown` show only the matching rows and a clear-filter chip.
3. Clicking the clear-filter chip returns to the unfiltered view (preserving `?public=true` if set).
4. Unfiltered `logs.html` and all four dashboard tile behaviors regressing-equal the pre-change behavior.
5. E2E specs and Storybook play tests pass (relative to the documented baselines in memory).
6. `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` succeeds (no Java changes, sanity build).
7. `cd frontend && npm run test:ci` is no worse than the pre-change baseline.
