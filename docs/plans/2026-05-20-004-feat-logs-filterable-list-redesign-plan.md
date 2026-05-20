---
title: "feat: Redesign logs.html as a filterable list (Mobbin-style)"
type: feat
status: active
created: 2026-05-20
branch: feat/redesign
target_files:
  - src/main/resources/static/logs.html
  - src/main/resources/static/components/cts-log-list.js
  - src/main/resources/static/components/cts-log-list.stories.js
  - frontend/e2e/logs.spec.js
---

# feat: Redesign logs.html as a filterable list (Mobbin-style)

## Problem Frame

`logs.html` today is a 10-column `cts-data-table` (Instance ID, Config, Name, Variant, Description, Started, Plan ID, Status, Result, Owner) with up to three CTAs per row (View Log link, View Plan link, View Config button, two owner pills when admin). The cells squeeze the high-value content — test name, variant, description, started date — into narrow columns; horizontal scanning is dominated by visual chrome (table grid, repeated CTAs, ID monospace strings) rather than the question the user is trying to answer: "which run am I looking at, and is it the one I want to open?"

User feedback (`/lfg`, 2026-05-20): the multi-column table is the wrong primitive. Inspired by Mobbin's pattern listing, we want each log to read as a single content card with a clear visual hierarchy (headline → context → metadata) and one obvious primary action (open the log). Faceted filters should be visible on the page rather than only reachable via URL params.

## Scope

In scope:

- Replace the 10-column DataTable on `logs.html` with a single-column filterable list of content cards.
- Each card shows: test name (headline link), Instance ID (slug line), description (secondary text), status + result chips, and a metadata footer carrying Variant, Started (relative + absolute), Plan ID chip, and Owner (admin-only, collapsed icon-pill).
- Visible faceted filters above the list: status multi-select chips, result multi-select chips, free-text search, sort selector. Sync chip selection with `?status=` / `?result=` URL params (preserving the existing dashboard deep-link contract).
- A new Lit component `cts-log-list` owns the data fetch, filter+sort state, URL reflection, item rendering, and the config modal trigger.
- Whole-card click opens the log (`log-detail.html?log=<id>`); the View Config affordance becomes a small ghost icon button anchored bottom-right of the card, not a column CTA.

Out of scope (deferred to follow-up):

- Server-side text search. The list operates client-side over the same `MAX_FILTERED_LOGS = 1000` envelope today's URL-filter codepath already uses. Cross-cap discoverability stays at "refine the filter to narrow further" via a footer hint.
- Bulk actions (multi-select cards, batch retry). Listed as a future possibility, not built now.
- Backward-compatible URL params beyond `?status=`, `?result=`, `?public=true` (already supported).
- A separate route for the public listing. `?public=true` continues to live on the same page.

### Deferred to Follow-Up Work

- Saved filter views ("My failed runs this week"). Tracked as a separate plan once the new list primitive ships.
- Server-side fuzzy search across `>1000` matches. Requires a backend search endpoint not present today.

## Requirements

**R1.** Each log row renders as a single card with: test name as the headline (link to `log-detail.html?log=<id>`), Instance ID as a slug below the headline, description in muted body text, status + result `cts-badge`s in the top-right of the card, metadata footer (Variant, Started, Plan ID, Owner-when-admin), and a discreet "View configuration" icon button anchored to the card's bottom-right.

**R2.** Faceted filter row above the list: status multi-select chip group (RUNNING, WAITING, FINISHED, INTERRUPTED, plus CREATED/CONFIGURED/NOT_YET_CREATED collapsed under an "Other" toggle if they appear in the dataset), result multi-select chip group (PASSED, FAILED, WARNING, REVIEW, SKIPPED, UNKNOWN). Selecting one or more chips filters the list immediately (client-side) and updates `?status=` / `?result=` URL params.

**R3.** Free-text search input filters by case-insensitive substring match across `testName`, `testId`, `description`, `planId`, and the formatted variant string. Live-filter on each keystroke; no Enter required. Empty input shows the full filtered list.

**R4.** Sort selector with at least these options: "Started (newest)" (default), "Started (oldest)", "Test name (A–Z)", "Status". Sort applies client-side on the currently-filtered set.

**R5.** Active-filter summary: when one or more facets or search text are active, render a compact chip strip showing what is applied with a "Clear all" reset. Replaces the legacy `#activeFilterChip` URL-driven pill from the current page. Match count rendered ("3 matches", "1,000+ matches" when truncated).

**R6.** Empty state: when zero items match the active filter, render a `cts-empty-state` with copy that names the active facet ("No logs match the active status filter — Clear filters to see all runs"). When the dataset itself is empty, render the existing "No logs to show" copy.

**R7.** Loading state: while the initial fetch is in flight, render a centred `cts-spinner` inside the list area. Subsequent client-side filter/sort/search are synchronous and do not show a spinner.

**R8.** Pagination: render up to 25 cards initially with a "Show more" trigger that appends the next 25. When the upstream `MAX_FILTERED_LOGS = 1000` cap is hit, show a footer hint reading "Showing the first 1000 matches. Refine the filter to narrow further." (matches today's truncation hint copy).

**R9.** URL-param compatibility: existing `?status=foo,bar` and `?result=baz` deep links from `cts-dashboard` stat tiles continue to work. The page boots with those facets pre-selected in the filter chip group. Selecting/clearing chips writes back to the URL via `history.replaceState` so reload/share preserves state. `?public=true` continues to drive the public-mode fetch and admin-only column hiding (owner chip).

**R10.** Public listing (`?public=true`): owner footer chip is omitted; the "View configuration" affordance is omitted (legacy parity — `PublicTestInfo` does not expose `config`).

**R11.** Admin listing (`isAdmin && !isPublic`): owner footer chip renders the two-tone `user-01` / `globe` icon pill with `cts-tooltip` carrying `owner.sub` and `owner.iss`, mirroring the current `templates/owner.html` shape.

**R12.** Whole-card activation: clicking anywhere on a card (except a nested control such as the config button, plan chip, or owner chip) opens `log-detail.html?log=<id>` (preserving `?public=true` when active). The card is a single `<a>` wrapping the full content; nested interactive elements use `e.stopPropagation()` on click so they keep their own activation.

**R13.** Keyboard accessibility: each card is focusable as a single link in tab order; nested controls (config button, plan chip, owner chip) are reachable via subsequent Tab presses inside the card's focus path; visible focus ring is the standard `--focus-ring` token; the filter chips operate via Space/Enter activation and arrow-key navigation within a chip group; the search input is reachable via Tab.

**R14.** Stable selectors for e2e: the host element keeps `id="logsListing"` so existing pages and links don't break. Items use `data-testid="log-list-item"` and carry `data-test-id="<testId>"`. The active-filter chip strip uses `data-testid="active-filter-summary"`. Status filter chips carry `data-status="<value>"`; result filter chips carry `data-result="<value>"`.

**R15.** Storybook coverage: a `cts-log-list.stories.js` covers Default, EmptyDataset, FilterActiveZeroMatches, AdminListing, PublicListing, Loading, Truncated (>=1000 match cap), URLParamPreselected. Each story has a play function that asserts the rendered shape.

## Key Technical Decisions

**KD1. New Lit component `cts-log-list`, not in-page script.** Mirrors the `cts-plan-list` migration pattern; gives the redesign a single, testable surface, lets us add Storybook stories, and matches the project memory entry "Storybook interaction tests" requiring every CTS web component to have play tests. The page becomes a thin mount that passes `is-public` / `is-admin` attributes.

**KD2. Client-side filtering and sorting over a fetched dataset.** The current `logs.html` already has a client-side codepath (`fetchLogsForFiltering`) that pulls up to `MAX_FILTERED_LOGS = 1000` rows when a URL filter is active. The redesign promotes that codepath to the default, so all filtering/sorting/search/pagination runs on a single fetched dataset — no double-rendering, no mode-flip. Past 1000 matches, surface the truncation hint and direct the user to refine.

**KD3. Reuse existing primitives, no new chip widget.** Status and result filter chip groups are rendered as styled `<button>` toggles using `cts-icon` for state. The "Clear all" affordance reuses the same visual silhouette as the existing `oidf-page-filter-chip` (filter icon + label + close-md trailing glyph). No new shared chip-group component — if a second list page needs the same widget later, extract it then.

**KD4. Sort and search live in component state, not URL params.** URL reflects filter facets (`?status=`, `?result=`) and the public flag (`?public=true`) only. Sort defaults to "Started (newest)" and search resets on reload. Rationale: facets are the durable, shareable axis (dashboard deep-links and bookmarks); sort and search are session-local. Keeps URL params bounded and matches the existing dashboard-deep-link contract.

**KD5. The card itself is the primary link.** Wrapping a card in `<a href>` is the simplest path to: keyboard focus by Tab, "open in new tab" via middle-click / Ctrl+click, predictable assistive-tech announcement ("link, [test name]"). Nested interactive controls (config button, plan chip, owner chip) stop propagation. The `<a>` carries `data-test-id` so e2e selectors stay stable.

**KD6. Variant displayed inline in the metadata footer, not as a column header.** Today the variant column shows `client_auth_type=client_secret_basic, response_type=code` — a long horizontal string. In the card layout it sits next to other metadata chips, wraps freely, and a longer variant string no longer compresses neighbouring columns.

**KD7. Description is the card body, not a column cell.** Description currently renders as ~30 characters of column text in a narrow cell. As card body it's free to take the full card width, wraps to ~2 lines (clamped with `-webkit-line-clamp: 2` and a tooltip on overflow if needed), and finally functions as the human label the field name promises.

**KD8. Backwards-compatible `id="logsListing"` host.** Legacy e2e tests (`frontend/e2e/logs.spec.js`) and any internal references key off `#logsListing`. Keep the host id; the inner DOM changes from `<cts-data-table>` to `<cts-log-list>` rendering an item list. E2E tests will be re-authored against the new selectors; the id stays for any external references.

---

## High-Level Technical Design

This sketch illustrates the intended layout and is directional guidance — not implementation specification.

```text
┌─ logs.html  /  cts-log-list (light-DOM Lit) ──────────────────────────┐
│ ┌─ filter row ────────────────────────────────────────────────────── │
│ │  🔍 [ Search logs                                  ]   Sort: ▾    │
│ │  Status:  [ Running ] [ Waiting ] [ Finished ]   [ Interrupted ]  │
│ │  Result:  [ Passed ] [ Failed ] [ Warning ] [ Review ] [ Skipped ]│
│ └───────────────────────────────────────────────────────────────────│
│                                                                      │
│ ┌─ active-filter summary (conditional) ──────────────────────────── │
│ │  ⌕ Status: running or waiting · Result: failed   (3 matches)  ✕  │
│ └───────────────────────────────────────────────────────────────────│
│                                                                      │
│ ┌─ card ───────────────────────────────────────────────────────────│
│ │  oidcc-server-rotate-keys                  [ RUNNING ] [ ―  ]    │
│ │  test-log-002                                                     │
│ │                                                                   │
│ │  Tests key rotation behaviour across a refresh of the JWKS doc   │
│ │  with overlap window honoured.                                    │
│ │                                                                   │
│ │  client_auth_type=client_secret_basic · response_type=code        │
│ │  Started 2 hours ago · Plan plan-001 · 👤 owner       [ ⚙ Config]│
│ └───────────────────────────────────────────────────────────────────│
│ ┌─ card ───────────────────────────────────────────────────────────│
│ │  vci-failed                                  [ FINISHED ] [ FAIL ]│
│ │  ...                                                              │
└──────────────────────────────────────────────────────────────────────┘

Show more (25 of 142)   ·   data fetched once: GET /api/log?length=1000
```

Notes:

- The whole card is an `<a href="log-detail.html?log=…">`. The `[ ⚙ Config ]` icon button, plan chip, and owner chip are nested `<button>` / `<a>` elements that stop propagation.
- The filter row stays in the normal page flow (no sticky), since the list scrolls within the page and there's no upper navigation chrome on `logs.html` competing for space.
- "Started 2 hours ago" uses `cts-tooltip` to expose the full timestamp on hover/focus, matching the relative-time pattern from `cts-running-test-card`.

## Output Structure

```text
src/main/resources/static/
├── logs.html                          # (modified)  thin mount, drops <cts-data-table>
└── components/
    ├── cts-log-list.js                # (new)      Lit component owning the redesign
    └── cts-log-list.stories.js        # (new)      Storybook play tests
frontend/e2e/
└── logs.spec.js                       # (modified) re-authored against new selectors
```

---

## Implementation Units

### U1. Scaffold `cts-log-list` component shell

- **Goal:** Create the new Lit component as a thin shell that mirrors `cts-plan-list`'s structural conventions (light DOM via `createRenderRoot()`, `isAdmin` / `isPublic` boolean attributes, `_logs` / `_loading` / `_error` reactive state) and renders a loading placeholder by default. No filter/sort logic yet — just the fetch + render scaffold.
- **Requirements:** Sets up the surface that R1, R7, R9, R10, R11, R14 will hang off.
- **Dependencies:** None.
- **Files:**
  - `src/main/resources/static/components/cts-log-list.js` (new)
- **Approach:**
  - JSDoc `@property` block declaring `isAdmin`, `isPublic` and the public events the component will fire (`cts-log-filter-change`).
  - `connectedCallback()` calls `_fetchLogs()`. Fetch hits `/api/log?length=1000` (or `&public=true`), accepts both `PaginationResponse` envelope and raw-array shape (mirror `cts-plan-list._fetchPlans` and `cts-dashboard._fetchListEndpoint`).
  - `render()` returns: loading spinner, error alert, or a list region with placeholder text. Filter chip group, sort selector, and item cards arrive in subsequent units.
  - Inject scoped CSS via the `STYLE_ID` / `ensureStylesInjected()` pattern used by `cts-plan-list`. CSS variables only (`--space-*`, `--bg-elev`, `--border`, `--radius-*`, `--fs-*`, `--lh-*`) — no Bootstrap utilities (project memory: Bootstrap is being removed).
- **Patterns to follow:**
  - `src/main/resources/static/components/cts-plan-list.js` — class shape, attribute names, state names, fetch shape, error rendering.
  - `src/main/resources/static/components/AGENTS.md` §1–§3 — LitElement choice, light-DOM rationale, property setters.
- **Test scenarios:**
  - Component registers as `cts-log-list` and mounts inside a host fixture.
  - `is-public` attribute reflects to `this.isPublic` (boolean).
  - `is-admin` attribute reflects to `this.isAdmin` (boolean).
  - Initial render shows the loading spinner with role="status".
  - Mocked fetch returning `PaginationResponse` envelope populates `_logs` from `payload.data`.
  - Mocked fetch returning a raw array populates `_logs` directly.
  - Mocked fetch returning HTTP 500 sets `_error` and renders a `cts-alert variant="danger"`.
- **Verification:** A Storybook story `Default` mounts the component with MSW returning a small fixture; the list region appears with the placeholder copy after the spinner clears.

### U2. Render log item cards (R1, R10, R11, R12)

- **Goal:** Replace the placeholder list region with the real per-log card markup: headline link, slug ID, description, status/result badges, metadata footer (variant, started, plan chip, owner-when-admin), config icon button anchored bottom-right.
- **Requirements:** R1, R10, R11, R12.
- **Dependencies:** U1.
- **Files:**
  - `src/main/resources/static/components/cts-log-list.js` (modify)
- **Approach:**
  - Add `RESULT_BADGE_VARIANTS` and `STATUS_BADGE_VARIANTS` lookups copied from `cts-log-detail-header.js`. Keep them top-level consts.
  - `_renderCard(log)` returns a Lit `<a class="cts-log-card" href="log-detail.html?log=<id>{public?}" data-testid="log-list-item" data-test-id=<id>>`. Inside: a header row (`.cts-log-card-headline`, `.cts-log-card-badges`), a description paragraph (`.cts-log-card-description`), a metadata footer (`.cts-log-card-meta`), and a footer-right config button.
  - Status badge: `<cts-badge variant="${STATUS_BADGE_VARIANTS[log.status]||'skip'}" label="${log.status}">`. Result badge: same with `RESULT_BADGE_VARIANTS[log.result]||'skip'`. Both read-only (no `interactive`).
  - Metadata footer renders inline chips/spans for: formatted variant (using a `_formatVariant` helper duplicated locally — same shape as `cts-plan-list._formatVariant`), started date (relative + `cts-tooltip` carrying the absolute ISO string), Plan ID link if `log.planId` (a small `<a>` with `data-plan-id` and a stopPropagation click handler), Owner pill via `_renderOwner(log.owner)` (admin only, omitted when `isPublic`).
  - Config button (`<cts-button class="showConfigBtn" variant="ghost" size="sm" icon="settings">`) at the card's bottom-right. Click is wired to `_handleConfigClick(log)` (handler stubbed in U2; modal mounted in U5). Visible only when `!isPublic`.
  - Card focus styling: `:focus-visible` on the `<a>` renders `box-shadow: var(--focus-ring)`. Hover: subtle elevation change (`border-color: var(--border-strong)`).
  - Nested control handlers all call `event.stopPropagation()` so card-level navigation doesn't fire when a chip / button is activated.
- **Patterns to follow:**
  - `src/main/resources/static/components/cts-running-test-card.js` for the card hero + body + footer block stacking.
  - `src/main/resources/static/templates/owner.html` (legacy Mustache) for the two-tone owner pill shape — port to Lit template literally; keep `.log-owner`, `.ownerSub`, `.ownerIss`, `cts-tooltip` wrappers, and `aria-label` strings exactly.
  - `src/main/resources/static/components/cts-log-detail-header.js` for `STATUS_BADGE_VARIANTS` / `RESULT_BADGE_VARIANTS` constants — keep them in sync.
- **Test scenarios:**
  - With a fixture row (`testName`, `testId`, `description`, `status: FINISHED`, `result: PASSED`, `started`, `planId`, `owner`), the card renders: headline link, slug, description text, two badges, plan chip, started timestamp.
  - Status badge variant maps `FINISHED → skip`, `RUNNING → running`, `WAITING → warn`, `INTERRUPTED → fail`.
  - Result badge variant maps `PASSED → pass`, `FAILED → fail`, `WARNING → warn`, `REVIEW → review`, `SKIPPED → skip`, `INTERRUPTED → fail`.
  - With `is-public` and `is-admin` both unset, the card renders without the owner pill and without the config button.
  - With `is-public` set, the card renders without the owner pill and without the config button (mirrors legacy public-listing parity).
  - With `is-admin` set (and `is-public` unset), the card renders the owner pill with `cts-tooltip content="${owner.sub}"` wrapping `.ownerSub` and `cts-tooltip content="${owner.iss}"` wrapping `.ownerIss`. The pill matches the legacy `templates/owner.html` shape.
  - Clicking the card navigates to `log-detail.html?log=<testId>` (covered as a unit test by reading `card.href`; navigation itself is asserted in e2e at U7).
  - Clicking the config button does NOT navigate (verified by capturing `click` on the card host and asserting it was not the default action; this is the `stopPropagation` regression guard).
  - Card's `<a>` element has `data-testid="log-list-item"` and `data-test-id="${testId}"`.
- **Verification:** Storybook stories `Default`, `AdminListing`, `PublicListing` each render the expected card shape per fixture; the play function asserts badges, owner pill presence/absence, and the href value.

### U3. Filter chip groups + URL sync (R2, R9, R14)

- **Goal:** Render the status and result filter chip groups above the list. Chips toggle on click, update `_statusFilter` / `_resultFilter` Set state, reflect to `?status=` / `?result=` URL params, and apply to the displayed cards.
- **Requirements:** R2, R5 (partial — strip + count rendered in U6), R9, R14.
- **Dependencies:** U2.
- **Files:**
  - `src/main/resources/static/components/cts-log-list.js` (modify)
- **Approach:**
  - Add reactive state: `_statusFilter: Set<string>`, `_resultFilter: Set<string>`. Default empty.
  - `connectedCallback()` reads `new URLSearchParams(window.location.search)` once at boot and populates the two sets via `_parseFilterSet(rawParam, validUppercaseSet)` (port the helper currently inline in logs.html — same enum allowlists `VALID_STATUSES`, `VALID_RESULTS`).
  - `_renderFilterRow()` renders two `<div class="cts-log-filter-group" role="group" aria-label="Filter by status">` blocks. Each block contains one toggle `<button type="button" class="cts-log-filter-chip" data-status="${value}" aria-pressed="${active}">${displayLabel}</button>` per enum value. Click toggles membership in the relevant Set, calls `_writeUrl()`, and triggers `requestUpdate()`.
  - `_writeUrl()` uses `history.replaceState(null, '', newSearch)` — no full reload, no scroll jump. Empty Sets drop the URL param entirely; `?public=true` is preserved.
  - `_filteredLogs()` computed getter returns `_logs.filter(row => (statusFilter.size===0 || statusFilter.has(row.status)) && (resultFilter.size===0 || resultFilter.has(row.result)))`. The card render loop iterates `_filteredLogs()`.
  - Chip styling: pressed state uses `--bg-elev` fill + `--border-strong` ring; unpressed uses transparent fill + `--border` ring. Focus ring uses `var(--focus-ring)`.
  - Keyboard: chips are real `<button>`s, so Space/Enter activation comes for free. Arrow-key navigation within a chip group is wired by a single `keydown` listener on each group root (`ArrowRight`/`ArrowLeft` rotate focus across siblings).
- **Patterns to follow:**
  - Existing `parseFilterSet` and the enum allowlists in `src/main/resources/static/logs.html` lines 156–179 — port verbatim.
  - `src/main/resources/static/components/cts-batch-runner.js` for any keyboard handling on a button group (if the pattern already exists there, mirror it; otherwise implement minimally).
- **Test scenarios:**
  - With no URL params, all chips render in their unpressed state and `_filteredLogs()` returns the full dataset.
  - With `?status=running,waiting`, the RUNNING and WAITING chips boot with `aria-pressed="true"` and the list shows only matching rows.
  - With `?result=failed`, the FAILED chip boots with `aria-pressed="true"`.
  - With `?status=bogus`, no chip is pressed and `_statusFilter` is empty (unknown tokens silently dropped — matches legacy behaviour).
  - With `?status=running,bogus`, only RUNNING is pressed (bogus dropped).
  - With `?public=true&status=finished`, the FINISHED chip is pressed AND `?public=true` is preserved when the filter is cleared.
  - Clicking an unpressed status chip toggles it to pressed, updates the list, and writes `?status=<value>` via `history.replaceState`.
  - Clicking a pressed chip toggles it to unpressed, removes its value from the URL, and re-shows previously-filtered rows.
  - With both `?status=` and `?result=` populated, the AND-of-facets is applied (matches today's behaviour at logs.html:459–462).
  - Arrow-right on a focused status chip moves focus to the next chip in the group (wraps at end).
  - The chip groups have `data-status="<value>"` and `data-result="<value>"` for e2e selection.
  - Selecting two chips writes `?status=running,waiting` (comma-joined, lowercase per legacy convention).
- **Verification:** Storybook story `URLParamPreselected` boots with `?status=running` and asserts the RUNNING chip is pressed and only RUNNING rows are visible. Interactive story `FilterToggle` clicks two status chips and asserts the URL reflection + filtered list.

### U4. Free-text search + sort selector (R3, R4)

- **Goal:** Add the search input and sort selector above the chip groups. Both apply client-side over `_filteredLogs()`.
- **Requirements:** R3, R4.
- **Dependencies:** U3.
- **Files:**
  - `src/main/resources/static/components/cts-log-list.js` (modify)
- **Approach:**
  - Add reactive state: `_searchText: string` (default ""), `_sortKey: string` (default "started-desc").
  - `_renderSearchAndSort()` renders a row with `<input type="search" class="cts-log-search-input" placeholder="Search logs" aria-label="Search logs" .value=${this._searchText} @input=${this._handleSearchInput}>` and a `<select class="cts-log-sort-select" aria-label="Sort logs" .value=${this._sortKey} @change=${this._handleSortChange}>` with options `started-desc`, `started-asc`, `name-asc`, `status-asc`.
  - `_handleSearchInput(e)` sets `this._searchText = e.target.value`. No debounce — the dataset is bounded at 1000, filter is O(n) over `_filteredLogs()`.
  - `_searchedLogs()` computed: lowercases search text once, returns logs where the test name, testId, description, planId, or formatted variant contains the substring. Empty search returns the input unchanged.
  - `_sortedLogs()` computed: applies the sort comparator. `started-desc`: ISO string compare reversed. `name-asc`: `localeCompare` on `testName`. `status-asc`: rank by a status-order enum (RUNNING < WAITING < FINISHED < INTERRUPTED < other).
  - Render pipeline: `_logs → _filteredLogs() → _searchedLogs() → _sortedLogs()`. Each step is a pure function over the previous output.
- **Patterns to follow:**
  - Existing `cts-data-table` search-input styling tokens at `src/main/resources/static/components/cts-data-table.js:225+` for visual parity with other pages.
- **Test scenarios:**
  - Empty search returns the full filtered list.
  - Typing "rotate" filters to rows whose `testName`, `testId`, `description`, `planId`, or formatted variant contains "rotate" (case-insensitive).
  - Typing then deleting all characters restores the full filtered list.
  - Sort "Started (newest)" places the most recent `started` first.
  - Sort "Started (oldest)" places the oldest `started` first.
  - Sort "Test name (A–Z)" places `axxx` before `bxxx` regardless of `started` order.
  - Sort "Status" places RUNNING / WAITING rows before FINISHED rows.
  - Sort and search compose: typing "fapi" with sort "Test name (A–Z)" returns only fapi-prefixed rows in alphabetical order.
  - Sort selector and search input do NOT update the URL (KD4).
  - `_searchText` and `_sortKey` reset on page reload (state-local, not URL-persisted).
- **Verification:** Storybook stories `SortByName` and `SearchActive` boot the component with fixtures and assert the rendered card order / count.

### U5. Config modal (R1, R10 negative)

- **Goal:** Wire the per-card "View configuration" icon button to a modal that fetches the plan config and renders it in a `cts-json-editor`. Mirrors the current logs.html behaviour at lines 397–427.
- **Requirements:** R1 (config affordance), R10 (omitted in public mode).
- **Dependencies:** U2 (config button rendered).
- **Files:**
  - `src/main/resources/static/components/cts-log-list.js` (modify)
- **Approach:**
  - Mount a single `cts-modal` instance inside the component's render tree (id `cts-log-list-config-modal`). Modal contains: a toolbar with the test ID label + a copy button (using `flashCopyConfirmed` from `cts-copy-flash.js`), and a `cts-json-editor` readonly.
  - Reactive state: `_selectedConfig: object|null`, `_selectedTestId: string`.
  - `_handleConfigClick(log)` (`event.stopPropagation()` first) fetches `/api/plan/${planId}${public?'?public=true':''}`, parses the body, sets `_selectedConfig = jsonData.config`, `_selectedTestId = log.testId`, then shows the modal once the next update completes.
  - Copy uses `navigator.clipboard.writeText(JSON.stringify(_selectedConfig, null, 4))` — same as `cts-plan-list._handleCopyConfig`. Drop the `ClipboardJS` dependency for this surface (the legacy `logs.html` does use ClipboardJS for the config modal copy button, but `cts-plan-list` has already moved to `navigator.clipboard.writeText` and the same approach works here).
  - The toolbar uses the same `cts-tooltip` wrapper around the copy button used in the legacy modal (test `frontend/e2e/logs.spec.js:147–149` asserts the tooltip is present — keep the contract).
- **Patterns to follow:**
  - `src/main/resources/static/components/cts-plan-list.js:345–369` — `_renderConfigModal` shape, copy flow, tooltip wrapping.
  - `src/main/resources/static/logs.html:397–427` for the fetch/HTTP error handling shape (route 4xx/5xx to the existing error-modal pattern using `FAPI_UI.showError`).
- **Test scenarios:**
  - With `is-public` unset, clicking the config button fetches `/api/plan/<planId>` and opens the modal.
  - With `is-public` set, the config button isn't rendered (covered by U2 — re-asserted here negatively).
  - The modal toolbar shows the testId as a `<code>` element.
  - The copy button has `icon="copy"` and visible label "Copy configuration" (matches legacy contract at `frontend/e2e/logs.spec.js:140–142`).
  - The copy button is wrapped in `<cts-tooltip content="Copy configuration JSON to clipboard">`.
  - HTTP 500 on the plan fetch shows the error modal via `FAPI_UI.showError`.
  - Closing the modal via its close button hides it (covered structurally — `cts-modal` owns its own close path).
- **Verification:** Storybook story `ConfigModal` opens the modal via play function and asserts the toolbar + editor render. The e2e test `config button in log row opens config modal` is re-pointed at the new card-config button (U7).

### U6. Active-filter summary + empty state + truncation hint (R5, R6, R8)

- **Goal:** Render the active-filter summary strip when any facet/search is active, the empty state when zero items match, and the truncation hint when the upstream cap is hit.
- **Requirements:** R5, R6, R8.
- **Dependencies:** U3, U4.
- **Files:**
  - `src/main/resources/static/components/cts-log-list.js` (modify)
- **Approach:**
  - Track `_truncated: boolean` — set in `_fetchLogs()` when the response envelope's `recordsTotal` exceeds the returned `data.length` OR `data.length >= MAX_FILTERED_LOGS`. Mirrors the existing `fetchLogsForFiltering` logic at logs.html:498–504.
  - `_renderActiveFilterSummary()` renders a `<div data-testid="active-filter-summary">` when any of `_statusFilter`, `_resultFilter`, or `_searchText` is non-empty. The summary is a `cts-icon name="filter"` + descriptive label + match count + `cts-icon name="close-md"` clear-all button. Match count: `${filteredCount}${truncated && filteredCount >= MAX_FILTERED_LOGS ? "+" : ""} ${filteredCount === 1 ? "match" : "matches"}`.
  - Clear-all clears `_statusFilter`, `_resultFilter`, `_searchText`, calls `_writeUrl()`, and resets focus to the search input.
  - Empty state: when `_searchedLogs()` length is zero, render `<cts-empty-state>` with title "No logs match the active filter" (or "No logs to show" when `_logs.length === 0`). Copy includes a "Clear filters" affordance when a filter is the cause.
  - Truncation hint: when `_truncated`, render a small `<p class="cts-log-list-truncation">` below the card list with copy "Showing the first 1000 matches. Refine the filter to narrow further." Hide when `_truncated` is false.
- **Patterns to follow:**
  - `src/main/resources/static/logs.html:198–247` for the current chip/truncation styling tokens — port the CSS to the component scope.
  - `src/main/resources/static/components/cts-empty-state.js` for the empty-state API.
- **Test scenarios:**
  - With no filters and a non-empty dataset, the active-filter summary is NOT rendered.
  - Activating the RUNNING status chip renders the summary with text containing "Status: running" and "(1 matches)".
  - Activating two status chips renders "Status: running or waiting".
  - Activating one status and one result renders "Status: ... · Result: ...".
  - Typing search text "rotate" renders "Search: rotate".
  - Combined facet + search renders all three labels joined by " · ".
  - Clicking the clear-all button clears every filter and the summary disappears.
  - When no rows match the filter, the empty state renders the active-filter copy.
  - When `_logs` is empty (fetched zero rows), the empty state renders the no-data copy.
  - When the response envelope reports `recordsTotal > data.length`, `_truncated` is true and the truncation hint renders.
  - When `data.length >= MAX_FILTERED_LOGS`, `_truncated` is true (envelope-less raw-array path).
- **Verification:** Storybook stories `EmptyDataset`, `FilterActiveZeroMatches`, `Truncated` boot with appropriate fixtures and assert the rendered surfaces.

### U7. Pagination via "Show more" (R8)

- **Goal:** Render up to 25 cards initially and a "Show more" button that appends the next 25 in-place.
- **Requirements:** R8.
- **Dependencies:** U2, U3, U4, U6.
- **Files:**
  - `src/main/resources/static/components/cts-log-list.js` (modify)
- **Approach:**
  - Reactive state `_visibleCount: number`, default 25.
  - `render()` slices the sorted+searched+filtered list to `_visibleCount`; "Show more" button appears below the card list when `_visibleCount < searchedSortedLength`.
  - Clicking "Show more" sets `_visibleCount += 25` (no fetch — the dataset is already in memory).
  - Changing any filter / search / sort resets `_visibleCount` to 25 (otherwise a 25-cap could obscure the first matches of a fresh filter).
  - "Show more" button label includes the running count: `Show more (25 of 142)`.
- **Patterns to follow:**
  - No existing exact match in the codebase. The simplest stateful counter shape — bounded, in-memory — is correct here.
- **Test scenarios:**
  - With 30 logs and no filter, 25 cards render and the "Show more" button shows "Show more (25 of 30)".
  - Clicking "Show more" once renders all 30 and hides the button.
  - With 60 logs and no filter, two clicks of "Show more" reveal all 60.
  - Activating a filter that narrows to 10 rows hides the "Show more" button entirely.
  - Changing the sort selector after clicking "Show more" once resets `_visibleCount` to 25 (so the user re-evaluates which rows are top-of-list under the new sort).
- **Verification:** Storybook story `Paginated60Items` asserts the initial 25-card render and the "Show more" advance.

### U8. Replace `logs.html` page mount + drop legacy in-page script

- **Goal:** Strip the existing in-page `<script type="module">` that builds the `cts-data-table` imperatively, replace the `#logsListing` div with a `<cts-log-list>` mount, and drop the now-unused imports (the legacy `LOG_DETAIL` / `PLAN_DETAIL` / `OWNER` / `CONFIG` / `DATE` Mustache templates and the `cellRenderer` wiring).
- **Requirements:** R14 (host id preserved), R7 (loading state), R9 (URL param boot), R10 / R11 (mode flags).
- **Dependencies:** U1–U7.
- **Files:**
  - `src/main/resources/static/logs.html` (modify)
- **Approach:**
  - Add `<script type="module" src="/components/cts-log-list.js"></script>` to the head section.
  - Remove the script imports for `cts-data-table`, `cts-tooltip` (now imported by the component), and the `lit` / `unsafeHTML` import (no longer needed at the page level).
  - Remove the inline `<style>` block for `.oidf-page-filter` / `.oidf-page-filter-chip` / `.oidf-page-filter-truncation` (now component-scoped).
  - Replace `<main id="main-content" class="listing-page">…</main>` body with: a single `<cts-log-list id="logsListing" is-admin?="…" is-public?="…">` mount. The id stays for backward compat (R14, KD8).
  - The DOMContentLoaded script that wires `FAPI_UI.showBusy()` / `getUserInfo()` / `loadAvailableLogs(...)` collapses to a small script that: reads `?public=true`, calls `FAPI_UI.getUserInfo()`, sets the `is-public` / `is-admin` attributes on the mount before insertion (the component reads them in `connectedCallback`).
  - Remove `loadLogListTemplates()` — no longer needed; verify no other page calls it. If it's only called by `logs.html`, delete the function from `fapi.ui.js` (audit before deletion per the legacy `loadImageUploadTemplates` precedent).
  - Remove `templates/logDetailButton.html`, `templates/planDetailButton.html`, `templates/configButton.html`, `templates/date.html` IF nothing else references them. `templates/owner.html` may be used by other places — grep first; if it's logs-only, delete it; otherwise leave it.
  - The page's `<cts-modal id="configModal">` element is now redundant (the component mounts its own modal). Delete it from `logs.html`. The `errorModal` and `loadingModal` stay (used by `FAPI_UI.showError` / `showBusy` system-wide).
- **Patterns to follow:**
  - `src/main/resources/static/plans.html` — analogous post-migration page shape with a single `<cts-plan-list>` mount and a thin DOMContentLoaded boot.
- **Test scenarios:**
  - None at the unit level (page-level surface change; covered by the e2e re-author in U9).
- **Verification:** Manual: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`, visit `https://localhost.emobix.co.uk:8443/logs.html`, confirm the card list renders, the filter chips toggle, the search filters, the config modal opens. Then visit `?status=running,waiting`, `?result=failed`, `?public=true`, `?status=running&result=failed`. Each should boot pre-filtered.

### U9. Re-author `frontend/e2e/logs.spec.js`

- **Goal:** Update the existing e2e tests to target the new card / chip / search structure while preserving every contract today's tests enforce (URL filter chips, search, config modal, owner pill shape, dashboard deep-link, public-mode parity).
- **Requirements:** R14 + regression protection for R1–R13.
- **Dependencies:** U1–U8.
- **Files:**
  - `frontend/e2e/logs.spec.js` (modify)
  - `frontend/e2e/fixtures/mock-log-list.js` (likely unchanged — confirm during execution)
- **Approach:**
  - Replace the `#logsListing .oidf-dt-table tbody tr[data-row-index]` selectors with `#logsListing [data-testid="log-list-item"]` selectors.
  - Replace the "search triggers re-fetch with Enter key" test with "typing in search live-filters the rendered cards". (The new behaviour is client-side live filtering — no `/api/log?search=` request is fired.)
  - Replace "Started header is not sortable" with "sort selector defaults to Started (newest) and reorders on change".
  - Re-point the config-button test at `[data-testid="log-list-item"] .showConfigBtn button`.
  - Re-point the URL-filter tests at the new active-filter summary (`[data-testid="active-filter-summary"]`) and confirm `?status=`, `?result=`, combined, public-mode, unknown-tokens, all behave per R9 / R2.
  - Re-point the owner-pill test at `[data-testid="log-list-item"] .log-owner` and re-assert the same pill shape (the template was ported verbatim in U2, so the inner DOM is identical).
  - Add new tests:
    - Whole-card click navigates to `log-detail.html?log=<testId>` (preserving `?public=true` when active).
    - Clicking the config button inside a card opens the modal but does NOT navigate (verifies KD5 stopPropagation).
    - Activating a status filter chip writes `?status=<value>` to the URL via `history.replaceState`.
    - Toggling the same chip twice removes the URL param.
    - "Show more" button reveals the next 25 cards (using a 60-row fixture).
    - Sort by Test name (A–Z) reorders the rendered cards.
  - Keep the `expectNoUnmockedCalls(page)` afterEach hook (no new external calls — the dataset still comes from `/api/log?length=1000`).
- **Patterns to follow:**
  - `frontend/e2e/plans.spec.js` for the analogous list-page e2e shape.
  - `frontend/CLAUDE.md` Frontend E2E section for the route ordering / `setupFailFast` requirement.
- **Test scenarios:**
  - Re-authored tests pass against the new component.
  - The new whole-card-click test fails if `<a href>` is missing or `stopPropagation` is missing on a nested control.
  - The new chip-URL-sync test fails if the URL stops reflecting the chip state.
  - The new "Show more" test fails if pagination state is missing or fails to advance.
- **Verification:** `cd frontend && ./node_modules/.bin/playwright test e2e/logs.spec.js` exits 0.

### U10. Storybook play tests + register stories

- **Goal:** Add `cts-log-list.stories.js` with the eight stories named in R15, each with a play function asserting the rendered surface.
- **Requirements:** R15.
- **Dependencies:** U1–U7.
- **Files:**
  - `src/main/resources/static/components/cts-log-list.stories.js` (new)
- **Approach:**
  - Stories: `Default`, `EmptyDataset`, `FilterActiveZeroMatches`, `AdminListing`, `PublicListing`, `Loading`, `Truncated`, `URLParamPreselected`, `SortByName`, `SearchActive`, `ConfigModal`, `Paginated60Items`.
  - Use MSW handlers (see `cts-plan-list.stories.js` for the established pattern) to mock `/api/log?length=1000` per story. For `Loading`, return a never-resolving promise (or a long-delayed response).
  - Each play function asserts the rendered counts, badge variants, and any per-story-specific contract (e.g. `URLParamPreselected` asserts the RUNNING chip is `aria-pressed="true"`).
  - Register the new stories file in any Storybook composition index if the project keeps one (the existing pattern is per-component colocation — `cts-plan-list.stories.js` already lives alongside its component).
- **Patterns to follow:**
  - `src/main/resources/static/components/cts-plan-list.stories.js` — MSW handler shape, play function structure, assertion style.
  - `src/main/resources/static/components/cts-batch-runner.stories.js` for sample-data-only stories.
  - Project memory: "Storybook interaction tests" → every CTS Web Component must have play tests.
- **Test scenarios:**
  - Each story renders without console errors.
  - Each play function passes when run by `run-story-tests`.
- **Verification:** `npm run test:ci` (frontend) passes the new stories; `mcp__storybook-mcp__preview-stories` for `cts-log-list` returns previews for all 12 stories.

---

## System-Wide Impact

- **Frontend pages affected:** only `src/main/resources/static/logs.html`. No other page mounts `cts-data-table` against `/api/log`, but `cts-dashboard` stat tiles deep-link to `logs.html?status=…` / `?result=…` — those continue to work because U3 preserves the URL contract.
- **Templates potentially deleted:** `templates/logDetailButton.html`, `templates/planDetailButton.html`, `templates/configButton.html`, `templates/date.html`. Audit other consumers (`grep -rn 'logDetailButton\\|planDetailButton\\|configButton\\|date\\.html' src/main/resources/static`) before deleting; only delete if logs.html was the sole consumer. `templates/owner.html` is more likely shared — keep unless confirmed otherwise.
- **`js/fapi.ui.js`:** `loadLogListTemplates()` is now dead code if logs.html was the only caller. Delete only after the audit confirms.
- **API contract:** unchanged. `/api/log?length=1000` and `/api/log?public=true&length=1000` both already exist; the page now uses the single-fetch path always rather than only when a URL filter is present.
- **Backwards compatibility:** `id="logsListing"` host preserved; `?status=` / `?result=` / `?public=true` URL contracts preserved; dashboard deep-links preserved; config modal trigger and copy contract preserved.
- **Performance:** A single 1000-row fetch on page load (vs. today's 25-row paged fetch in unfiltered mode) — this is a small bandwidth increase per page load. At rough estimate (~600 bytes/log JSON) the worst-case payload is ~600 KB uncompressed. Most users have <100 logs. Acceptable per the project's "internal tooling default" feedback.
- **Accessibility:** Card-as-link gives a clean tab order, real `<button>` chips support Space/Enter, search input is a real `<input>` with `aria-label`, sort is a real `<select>`. No custom roles or `tabindex` games.

## Risks and Mitigations

- **Risk:** Single 1000-row fetch on page load is a meaningful change from today's paged fetch on the unfiltered path. **Mitigation:** Today's URL-filter codepath (`fetchLogsForFiltering`) already does exactly this, and `cts-dashboard` already pre-loads 1000 logs at boot for stat-tile counts. We're consolidating two codepaths into one.
- **Risk:** Long card descriptions could blow up card height. **Mitigation:** Clamp the description to ~2 lines with `-webkit-line-clamp: 2` and a hover tooltip exposing the full text.
- **Risk:** Live-filter on every keystroke is O(n) over up to 1000 rows × 5 fields. **Mitigation:** Each pass is a single lowercase substring match per field — well under a frame budget at n=1000. If we observe lag in practice, add a 50ms debounce; do not pre-optimise.
- **Risk:** The card-as-`<a>` pattern can confuse assistive tech when nested interactive controls are present. **Mitigation:** Nested controls are real `<button>` / `<a>` elements with their own focus path; the outer `<a>` is the headline link and stops propagation are placed only on the nested controls' click handlers. This is the same pattern Mobbin / Linear / Vercel use.
- **Risk:** The new active-filter summary copy diverges from today's chip copy ("Showing the first 1000 matches" etc.). **Mitigation:** Reuse the literal strings from `logs.html:204–246` verbatim; only the host selector changes.
- **Risk:** Deletion of unused templates affects an unaudited consumer. **Mitigation:** Hard requirement — `grep -rn` before any delete; if any other file references the template, leave it.

## Verification Strategy

- Unit-equivalent coverage: each Storybook story's play function (U10) asserts the visible surface for one variant.
- Integration: `frontend/e2e/logs.spec.js` (U9) covers the full page through Playwright + MSW: filter chip URL sync, search, sort, pagination, config modal, owner pill, whole-card click, public-mode parity, admin-only owner pill.
- Manual smoke (U8): `mvn spring-boot:run -Dspring-boot.run.profiles=dev`, visit `logs.html` and each URL-param variant, confirm the redesign renders and the modal opens.
- Frontend quality gates: `cd frontend && npm run test:ci` (format / lint / type-check / lint:jsdoc / lint:icons / lint:lit-analyzer) passes against the new files.
- Snapshot guard: `lit-analyzer` catches any Lit binding errors or unknown elements; `lint:icons` catches any `cts-icon name="…"` referencing an unvendored SVG.

## Deferred / Open Questions

- **Q1.** Should the "Started" timestamp render as relative time ("2 hours ago") or absolute ISO? **Default:** relative with `cts-tooltip` exposing the absolute. Implementer may swap for absolute-only if the relative-time helper is more weight than warranted.
- **Q2.** Should sort persist across reloads? **Default per KD4:** no. If users complain, promote sort to a URL param later.
- **Q3.** "Show more" vs. infinite scroll: shipped as "Show more" (R8) — predictable Tab order, no IntersectionObserver, no scroll-jacking. Infinite scroll can be added later if requested.
- **Q4.** Should the filter chips collapse into a "More filters" dropdown when the chip count exceeds the row width? **Default:** no — let them wrap naturally on narrow viewports; the chip group is short enough (~10 items total) that wrap-to-two-rows is acceptable.

## References

- `src/main/resources/static/logs.html` — page being redesigned.
- `src/main/resources/static/components/cts-plan-list.js` — closest structural analogue for the new component.
- `src/main/resources/static/components/cts-log-detail-header.js` — source of `STATUS_BADGE_VARIANTS` / `RESULT_BADGE_VARIANTS` constants.
- `src/main/resources/static/components/cts-dashboard.js` — sample of the `MAX_FILTERED_LOGS = 1000` single-fetch pattern.
- `src/main/resources/static/components/AGENTS.md` — component authoring rules.
- `frontend/e2e/logs.spec.js` — e2e tests being re-authored.
- `frontend/e2e/fixtures/mock-log-list.js` — fixture used by stories and e2e.
- Project memory: "Log-viewer redesign" (`docs/brainstorms/2026-04-26-cts-log-viewer-redesign-requirements.md`) — the parent log-page redesign brainstorm; this plan addresses the *listing* page only, complementing the *detail* page redesign that shipped earlier.
