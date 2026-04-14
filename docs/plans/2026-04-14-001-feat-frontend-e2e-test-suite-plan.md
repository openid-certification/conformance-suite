---
title: "feat: Playwright E2E test suite for legacy frontend pages"
type: feat
status: active
date: 2026-04-14
origin: docs/brainstorms/2026-04-14-frontend-e2e-test-suite-requirements.md
---

# feat: Playwright E2E test suite for legacy frontend pages

## Overview

Add a Playwright E2E test suite that validates the 7 legacy static HTML pages work correctly with mocked API responses. The tests serve as a regression safety net for the ongoing migration from vanilla JS + lodash templates to Lit web components, running entirely without the Spring Boot backend, MongoDB, or HTTPS proxy.

## Problem Frame

The conformance suite frontend is being incrementally migrated to Lit web components. While 26 components have ~163 Storybook play-function tests, no tests exercise full page-level flows. Without a regression safety net covering the legacy pages, refactoring risks silently breaking user-facing behavior. (see origin: `docs/brainstorms/2026-04-14-frontend-e2e-test-suite-requirements.md`)

## Requirements Trace

**Test Infrastructure**
- R1. Playwright drives a real browser against legacy pages from `src/main/resources/static/`
- R2. API mocking via `page.route()` with fail-fast for unmocked `/api/*` routes; minimum mocks on every page: `/api/currentuser`, `/api/server`, `api/ui/spec_links`
- R3. Fixture data structured for reuse by both Playwright and Storybook MSW handlers
- R4. Static file server for `src/main/resources/static/`; CDN libraries load from real CDNs
- R5. Single `npm run test:e2e` script from `frontend/` directory
- R6. No Java backend, MongoDB, or TLS proxy required; CI runners need internet for CDNs

**Scheduling Flow** — R7 (cascade smoke), R8 (form interaction), R9 (submission), R10 (validation), R11 (error state)

**Execution Flow** — R12 (running-test smoke), R13 (manual refresh), R14 (exposed values), R15 (empty state — no completion detection per actual page behavior)

**Log Detail** — R16 (smoke), R17 (entry rendering), R18 (entry expansion), R19 (failed test), R20 (warning display)

**Cross-Page Journeys** — R21 (schedule-to-results), R22 (plan-detail-to-test)

**Home/Plans/Logs** — R25 (home smoke), R26 (plans DataTable), R27 (logs DataTable), R28 (plan-detail smoke)

**Authentication** — R23 (all tests mock authenticated user), R24 (unauthenticated state test on home page; other pages assume authenticated user via `setupCommonRoutes()`)

## Scope Boundaries

- **In scope**: 7 legacy static pages: index.html, schedule-test.html, running-test.html, log-detail.html, plan-detail.html, plans.html, logs.html
- **Out of scope**: Thymeleaf-rendered pages (require server-side rendering)
- **Out of scope**: Individual Lit web component behavior (covered by Storybook)
- **Out of scope**: Visual regression testing (focus is behavioral correctness)
- **Out of scope**: Real OAuth login flow (mock authenticated state via `/api/currentuser`; tests cover both authenticated and unauthenticated states per R23-R24)
- **Not replacing**: Existing Storybook component tests — E2E tests complement them

### Corrections from Origin Document

Flow analysis against the actual codebase revealed two requirements that describe non-existent behavior:

- **R15** ("when all tests finish, the UI shows final results with links"): running-test.html has no completion detection or auto-polling. It shows a manual-refresh snapshot of whatever `/api/runner/running` returns. This plan tests: when `/api/runner/running` returns an empty array, the page shows no running tests.
- **R22** ("navigating from plan-detail.html to running-test.html"): plan-detail.html's `runTest()` at line 659 navigates to `log-detail.html?log=:testId`, not running-test.html. This plan tests the actual navigation path: plan-detail → POST `/api/runner` → redirect to log-detail.html.

### Deferred to Separate Tasks

- `?public=true` view variants for plans.html, logs.html, plan-detail.html, log-detail.html — a meaningful behavioral branch (hides columns and buttons) but excluded from initial scope
- `schedule-test.html?edit-plan=ID` and `?edit-test=ID` flows — real user flows for editing existing configs, but not in the origin requirements

## Context & Research

### Relevant Code and Patterns

**Two AJAX patterns across the 7 pages:**
1. **`fetch()`-based**: index.html, schedule-test.html, running-test.html, log-detail.html, plan-detail.html
2. **jQuery + DataTables `serverSide: true`**: plans.html, logs.html — DataTables sends `{draw, start, length, search, order}` query parameters and expects `{draw, recordsTotal, recordsFiltered, data}` response envelope

**Template loading system** (`src/main/resources/static/js/fapi.ui.js`):
- `FAPI_UI` global object with `load*Templates()` methods per page
- Templates fetched via relative URLs from `templates/` directory (35 files)
- Compiled via `_.template()` (lodash 4.17.21) and stored on `FAPI_UI.logTemplates`
- Critical: IIFE at line 741 fires `fetch('api/ui/spec_links?public=true')` at script parse time — before `DOMContentLoaded`

**Existing fixture infrastructure** (`frontend/stories/fixtures/`):
- ES modules with named exports, compatible with `"type": "module"` in `frontend/package.json`
- `msw-handlers.js` — grouped handlers: auth, server, plan, log, runner, token
- `mock-test-data.js` — `MOCK_PLAN_DETAIL`, `MOCK_TEST_STATUS`, `MOCK_RUNNING_TESTS`, `MOCK_SERVER_INFO`
- `mock-plans.js` — `MOCK_PLANS` (available plans), `MOCK_PLAN_LIST` (plan listing)
- `mock-log-entries.js` — `MOCK_LOG_ENTRIES` plus variants
- `mock-users.js` — `MOCK_USER`, `MOCK_ADMIN_USER`, `MOCK_GUEST_USER`

**Known fixture gaps for E2E:**
- No `api/ui/spec_links` response shape
- No DataTables-shaped responses for `/api/plan` and `/api/log` list endpoints
- No `/api/lastconfig` response (used by schedule-test.html)
- `MOCK_RUNNING_TESTS` is an array of objects, but running-test.html:145 expects `/api/runner/running` to return string test IDs

**Known fixture bug:**
- `msw-handlers.js` line 51 mocks `/api/runner/available` but schedule-test.html calls `/api/plan/available`

**Storybook config pattern** (`frontend/.storybook/main.js`):
- `staticDirs: [{ from: "../../src/main/resources/static", to: "/" }]` — validates the path mapping

**Existing frontend tooling** (`frontend/package.json`):
- `playwright` ^1.59.1 (peer dep of `@vitest/browser-playwright` — does NOT include test runner)
- `@playwright/test` is **not installed** — must be added as devDependency
- `vite` ^6.3.3, `storybook` ^10.3.5, `vitest` ^4.1.4, `msw` ^2.13.3

**URL parameter patterns** driving page state:
- `plan-detail.html?plan=PLAN_ID`
- `log-detail.html?log=TEST_ID`
- `plans.html?public=true`, `logs.html?public=true`
- `schedule-test.html?test_plan=PLAN_NAME` or `?edit-plan=PLAN_ID`

### Institutional Learnings

No `docs/solutions/` directory exists. This is greenfield E2E infrastructure.

## Key Technical Decisions

- **Static file server: `serve` via Playwright `webServer` config** — Zero-config static HTTP server. Storybook already validates this path mapping works (`staticDirs` in `main.js`). Vite would add unnecessary complexity for serving unchanged static files. `serve` should be added as a devDependency for CI reliability rather than relying on `npx`.

- **Fixture sharing: Direct ES module imports, shared directory** — Existing fixtures in `frontend/stories/fixtures/` are ES modules. Playwright tests run in Node.js with ESM support (`"type": "module"` in package.json). New fixture shapes go in the same directory. MSW handler wiring is MSW-specific; Playwright tests use their own `page.route()` helpers that import the shared data.

- **Route registration timing: All routes before `page.goto()`** — The IIFE in `fapi.ui.js` fires `fetch('api/ui/spec_links')` at parse time. All `page.route()` calls must be registered before any navigation. This is standard Playwright practice but critical here because a late registration silently misses this fetch.

- **Fail-fast catch-all: `page.route('**/api/**', ...)` registered last** — Playwright matches routes in registration order. Specific routes registered first, then a catch-all that aborts the request and stores the URL for assertion. This surfaces unmocked endpoints as clear test failures rather than silent undefined behavior.

- **DataTables mock shape: Wrapper utility in route helper** — Shared fixtures export plain arrays (what Storybook components expect). The Playwright route helper wraps them in `{draw, recordsTotal, recordsFiltered, data}` based on request query parameters. Avoids duplicating data in two shapes.

- **running-test.html mock: String IDs, not objects** — The actual page iterates `/api/runner/running` response with `_.each(data, function(testId))` and calls `GET /api/runner/:testId` for each ID. The route helper extracts `._id` strings from `MOCK_RUNNING_TESTS` objects for the list endpoint, and returns individual objects for the detail endpoint.

- **Fix MSW handler endpoint as part of this work** — `msw-handlers.js` mocks `/api/runner/available` but schedule-test.html calls `/api/plan/available`. Fixing this benefits both Storybook stories and Playwright tests.

- **Test organization: `frontend/e2e/` directory, one `.spec.js` per page** — Keeps E2E tests adjacent to Storybook infrastructure. Shared route helpers in `frontend/e2e/helpers/`. A separate `journeys.spec.js` for cross-page tests.

## Open Questions

### Resolved During Planning

- **Which static file server?** `serve` via Playwright `webServer`. Simple, no build step, handles all standard MIME types.
- **Can existing fixtures be imported by Playwright?** Yes. Both are ES modules in a `"type": "module"` package. Playwright runs in Node with native ESM support.
- **How to chain cross-page navigation?** Single `page` object with all routes registered before first `page.goto()`. Playwright's `page.route()` interceptors persist across page navigations.
- **How to simulate state changes across refreshes (R13)?** Use a counter variable in the `page.route()` handler closure. Return different responses on successive calls to the same endpoint.
- **Does `page.route()` intercept jQuery `$.ajax()`?** Yes. `page.route()` intercepts all HTTP requests regardless of the client-side API (fetch, XMLHttpRequest, etc.).
- **What does `/api/runner/running` return?** Array of string test IDs (confirmed at running-test.html:145). Each ID is passed to `GET /api/runner/:testId` for the full object.

### Deferred to Implementation

- **Exact wait selectors for loading modal**: Each page shows/hides `#loadingModal`. The right wait strategy (modal hidden vs. specific content visible) depends on timing in practice.
- **DataTables initialization timing**: jQuery DataTables may need specific `waitForSelector` or `waitForResponse` patterns when combined with `page.route()` mocking.
- **schedule-test.html cascade completion timing**: The 4-step cascade involves multiple sequential DOM updates. The exact `waitForSelector` sequence depends on template rendering speed.
- **`serve` MIME type handling**: If `serve` handles certain MIME types differently than Spring Boot (unlikely but possible), may need to switch to Vite preview mode.

## Output Structure

```
frontend/
  e2e/
    helpers/
      routes.js             -- page.route() setup, fail-fast, common and page-specific routes
    home.spec.js            -- index.html tests (R25, R23, R24)
    schedule-test.spec.js   -- schedule-test.html tests (R7-R11)
    running-test.spec.js    -- running-test.html tests (R12-R15)
    log-detail.spec.js      -- log-detail.html tests (R16-R20)
    plan-detail.spec.js     -- plan-detail.html tests (R28)
    plans.spec.js           -- plans.html tests (R26)
    logs.spec.js            -- logs.html tests (R27)
    journeys.spec.js        -- cross-page journey tests (R21, R22)
  stories/
    fixtures/
      mock-spec-links.js    -- NEW: api/ui/spec_links response shape
      mock-log-list.js      -- NEW: log list data for /api/log DataTables endpoint
      mock-plans.js         -- EXISTING (may need extending)
      mock-test-data.js     -- EXISTING (may need extending)
      msw-handlers.js       -- FIX: /api/runner/available → /api/plan/available
  playwright.config.js      -- NEW: Playwright E2E config with webServer
  package.json              -- MODIFY: add @playwright/test + serve, add test:e2e script
```

## Implementation Units

- [ ] **Unit 1: Playwright infrastructure and npm scripts**

  **Goal:** Set up Playwright test runner, static file server, and npm script so that `npm run test:e2e` works (even with no tests yet).

  **Requirements:** R1, R4, R5, R6

  **Dependencies:** None

  **Files:**
  - Create: `frontend/playwright.config.js`
  - Modify: `frontend/package.json`

  **Approach:**
  - Add `@playwright/test` (pinned to `^1.59.1` to align with the existing `playwright` peer dependency and avoid browser binary conflicts) and `serve` as devDependencies
  - Run `npx playwright install chromium` after installation to ensure browser binaries are available
  - Configure `webServer` in `playwright.config.js` to run `serve` pointing at `../../src/main/resources/static` on a fixed port
  - Set `baseURL` to `http://localhost:<port>` so tests can use relative paths like `/index.html`
  - Configure Chromium-only browser (consistent with existing vitest browser setup, faster than multi-browser)
  - Set `testDir` to `./e2e`
  - Add `test:e2e` script to `package.json`
  - Create `frontend/.gitignore` (or append to root `.gitignore`) with `e2e-results/`, `playwright-report/`, and `test-results/` (Playwright's default trace output directory)

  **Patterns to follow:**
  - `frontend/.storybook/main.js` — path mapping `{ from: "../../src/main/resources/static", to: "/" }`
  - `frontend/vitest.config.js` — existing browser test config (Chromium, headless)

  **Test expectation:** none — infrastructure scaffolding with no behavioral changes

  **Verification:**
  - `npm run test:e2e` starts the static file server and Playwright exits cleanly (no tests found)
  - Manually navigating to `localhost:<port>/index.html` shows the page loading (CDN resources resolve, templates directory accessible)

- [ ] **Unit 2: Shared fixtures and route helpers**

  **Goal:** Create the route mocking infrastructure and extend shared fixtures with missing response shapes.

  **Requirements:** R2, R3

  **Dependencies:** Unit 1

  **Files:**
  - Create: `frontend/e2e/helpers/routes.js`
  - Create: `frontend/stories/fixtures/mock-spec-links.js`
  - Create: `frontend/stories/fixtures/mock-log-list.js`
  - Modify: `frontend/stories/fixtures/msw-handlers.js`

  **Approach:**
  - Route helper exports: `setupCommonRoutes(page)` for the 3 always-needed mocks (`/api/currentuser`, `/api/server`, `api/ui/spec_links`), plus page-specific setup functions for each page's additional routes
  - All setup functions import data from shared `stories/fixtures/` modules
  - `setupCommonRoutes` accepts optional overrides (e.g., to return 401 for unauthenticated tests)
  - Shared wildcard route for `/api/info/:testId` — returns a `MOCK_TEST_STATUS`-shaped object with the `testId` extracted from the URL path. This route is needed by plan-detail.html, plans.html, running-test.html, and log-detail.html, and must be registered before the fail-fast catch-all. Page-specific setup functions can override this with more specific data when needed
  - After all specific routes are registered, a `setupFailFast(page)` call registers the catch-all `**/api/**` route that calls `route.abort('failed')` (or `route.fulfill({ status: 599 })`) and records the unmocked URL for assertion. The handler must always complete the route — omitting `abort()`/`fulfill()` causes 30-second hangs instead of clear failures
  - DataTables wrapper utility: takes a raw data array and request URL, returns `{draw, recordsTotal, recordsFiltered, data}` shape by reading `draw`, `start`, `length` from query params
  - `/api/runner/running` route helper maps `MOCK_RUNNING_TESTS` objects to string IDs for the list endpoint
  - Fix `msw-handlers.js` line 51: change `/api/runner/available` to `/api/plan/available`
  - New `mock-spec-links.js`: empty object `{}` as default (sufficient for most pages; log-detail tests that verify spec link rendering may need richer data)
  - New `mock-log-list.js`: array of log summary objects matching the shape `/api/log` returns (fields: `testId`, `testName`, `result`, `status`, `started`, `planId`, `variant`, `owner`)

  **Patterns to follow:**
  - `frontend/stories/fixtures/msw-handlers.js` — handler grouping, named export pattern
  - `frontend/stories/fixtures/mock-test-data.js` — fixture module pattern (JSDoc header, named const exports)

  **Test scenarios:**
  - Happy path: Route helper intercepts `/api/currentuser` and returns `MOCK_USER` data
  - Happy path: Route helper intercepts `api/ui/spec_links` before page load (IIFE timing)
  - Edge case: Unmocked `/api/foo/bar` route triggers fail-fast — test receives a clear error identifying the URL
  - Integration: DataTables wrapper produces correct envelope shape from raw array and request params

  **Verification:**
  - A minimal smoke test using `setupCommonRoutes` navigates to `index.html` without route errors
  - The fail-fast handler catches an unmocked `/api/*` call and produces a descriptive assertion failure
  - Existing Storybook play-function tests continue to pass after the MSW handler fix — run the Storybook test command to confirm no regressions from the endpoint rename

- [ ] **Unit 3: Home page and authentication tests**

  **Goal:** First real test file — validates the entire infrastructure chain and tests index.html rendering plus authentication states.

  **Requirements:** R25, R23, R24

  **Dependencies:** Unit 1 (infrastructure verified working), Unit 2 (route helpers)

  **Files:**
  - Create: `frontend/e2e/home.spec.js`

  **Approach:**
  - Navigate to `/index.html` with common routes mocked
  - index.html calls: template fetch (userinfo.html), `GET /api/currentuser`, `GET /api/server`, and the `api/ui/spec_links` IIFE
  - For the unauthenticated test, override `/api/currentuser` to return 401 and verify the page gracefully degrades

  **Patterns to follow:**
  - `src/main/resources/static/index.html` — page structure, DOM element IDs

  **Test scenarios:**
  - Happy path (R25, R23): `index.html` loads → server version info rendered in `.serverInfo` footer element → user name shown in `#userInfoHolder` → navigation buttons visible
  - Edge case (R24): `/api/currentuser` returns 401 → `#userInfoHolder` contains no user info → navbar shows only public navigation links (Published Logs, Published Plans, API Docs)
  - Integration: Proves the full chain: `serve` → `page.route()` → template loading → lodash rendering → content visible

  **Verification:**
  - Both tests pass via `npm run test:e2e`
  - Removing the `templates/userinfo.html` file causes the smoke test to fail (regression detection sanity check — run once manually, not part of the suite)

- [ ] **Unit 4: Schedule test page tests**

  **Goal:** Test the test plan scheduling flow from specification cascade through plan submission.

  **Requirements:** R7, R8, R9, R10, R11

  **Dependencies:** Unit 2

  **Files:**
  - Create: `frontend/e2e/schedule-test.spec.js`

  **Approach:**
  - Mock `/api/plan/available` with `MOCK_PLANS` fixture — this drives the 4-step cascade (Specification → Entity Under Test → Version → Test Plan)
  - Mock `/api/lastconfig` to return empty object (no previous config)
  - The cascade uses chained `<select>` elements: `#specFamilySelect`, `#entityTypeSelect`, `#entityVersionSelect`, `#planSelect`
  - For R9, mock `POST /api/plan` to return `{id: "plan-new-001"}` and verify `window.location` changes to `plan-detail.html?plan=plan-new-001`
  - For R10, attempt submission without selecting a plan and verify the submit button is disabled or validation feedback appears
  - For R11, mock `/api/plan/available` to return HTTP 500 and verify error handling

  **Patterns to follow:**
  - `src/main/resources/static/schedule-test.html` — cascade logic around line 2173, submission around line 2513

  **Test scenarios:**
  - Happy path (R7): Page loads → `#specFamilySelect` populates with specification families → select a spec → `#entityTypeSelect` populates → select entity type → `#entityVersionSelect` populates → select version → plan options appear
  - Happy path (R8): After plan selection → variant selectors render (e.g., client_auth_type, response_type dropdowns) → configuration fields appear → fill required fields → "Create Test Plan" button becomes enabled
  - Happy path (R9): Click "Create Test Plan" → `POST /api/plan` called with planName and variant in query params, config JSON in body → page navigates to `plan-detail.html?plan=plan-new-001`
  - Edge case (R10): Without selecting a test plan → "Create Test Plan" button is disabled or shows validation feedback → no `POST /api/plan` request made
  - Error path (R11): `/api/plan/available` returns 500 → error modal or meaningful error state visible

  **Verification:**
  - All 5 tests pass
  - Removing the `#specFamilySelect` element from `schedule-test.html` causes R7 test to fail (regression detection)

- [ ] **Unit 5: Running test page tests**

  **Goal:** Test the running tests list page with manual refresh and state transitions.

  **Requirements:** R12, R13, R14, R15

  **Dependencies:** Unit 2

  **Files:**
  - Create: `frontend/e2e/running-test.spec.js`

  **Approach:**
  - Mock `/api/runner/running` to return an array of string test IDs (e.g., `["test-running-001", "test-running-002"]`) — NOT objects
  - Mock `/api/runner/:testId` for each test to return the full test object (from `MOCK_RUNNING_TESTS` fixture items)
  - Mock `/api/info/:testId` for each test to return status/result info
  - Sequential fetch pattern: page fetches list → iterates IDs → fetches runner detail → fetches info for each
  - For R13 (refresh), use a counter in the route handler to return different responses on the second `/api/runner/running` call (e.g., one test's status changes to FINISHED)
  - For R14, include `exposed.authorization_endpoint_request` URL in the test fixture
  - For R15, return empty array from `/api/runner/running`

  **Patterns to follow:**
  - `src/main/resources/static/running-test.html` — `updateRunningTable()` at line 136, `getRunningTest()` at line 158

  **Test scenarios:**
  - Happy path (R12): Page loads → calls `/api/runner/running` → for each test ID, calls `/api/runner/:id` then `/api/info/:id` → table renders rows with test names, statuses, and variants
  - Happy path (R13): Click `#refresh` button → `/api/runner/running` fetched again → second response has updated data (e.g., test-running-001 now shows FINISHED) → table reflects new statuses
  - Happy path (R14): Test fixture includes `exposed.authorization_endpoint_request` URL → URL renders as clickable `<a>` link in the test row
  - Edge case (R15, reframed): `/api/runner/running` returns empty array `[]` → `#running-tests` container is empty, no test rows rendered

  **Verification:**
  - All 4 tests pass
  - R12 test verifies the sequential API call pattern (running → runner/:id → info/:id for each test)

- [ ] **Unit 6: Log detail page tests**

  **Goal:** Test log detail rendering including entry expansion, failure summaries, and warning display.

  **Requirements:** R16, R17, R18, R19, R20

  **Dependencies:** Unit 2

  **Files:**
  - Create: `frontend/e2e/log-detail.spec.js`

  **Approach:**
  - Navigate to `log-detail.html?log=TEST_ID`
  - Mock `/api/info/:testId` with appropriate test status fixture (PASSED, FAILED, or WARNING variant)
  - Mock `/api/log/:testId` with `MOCK_LOG_ENTRIES` — supports `?since=` param for incremental polling (return empty array for `since > 0`)
  - Mock `/api/runner/:testId` to return the test object with `status: "FINISHED"` for finished test scenarios — this mirrors real behavior where `getActive()` succeeds, shows the runner info container, and calls `stopReloader()` at line 1031. Returning 404 instead would suppress DOM state (running test info panel, scroll-to-anchor) that tests may depend on. Reserve 404 only if testing behavior when the test is truly absent from the runner
  - Mock `/api/plan/:planId` for the plan button in the header (secondary call from `getHeader()`)
  - For R19, use `MOCK_TEST_FAILED` fixture and log entries that include failure condition details
  - For R20, create or use a WARNING result variant

  **Patterns to follow:**
  - `src/main/resources/static/log-detail.html` — `getHeader()`, `getLogs()`, `getActive()` sequence
  - `frontend/stories/fixtures/mock-log-entries.js` — existing log entry fixture shapes

  **Test scenarios:**
  - Happy path (R16): `log-detail.html?log=test-inst-001` loads → header shows test name "oidcc-server", status "FINISHED", result "PASSED"
  - Happy path (R17): Log entries render with source labels, message text, and result badges (success/failure/warning/info styling)
  - Happy path (R18): Click a collapsed log entry → entry expands to reveal detailed content (HTTP request/response block, requirements mapping section) → content was hidden before click
  - Edge case (R19): `/api/info/:testId` returns FAILED result → failure summary section is visible → failing condition name and details shown
  - Edge case (R20): Test has WARNING results → warning log entries styled distinctly from failures and passes (different badge color/class)

  **Verification:**
  - All 5 tests pass
  - The expansion test (R18) explicitly asserts content is hidden before click and visible after

- [ ] **Unit 7: Plan detail, plans list, and logs list tests**

  **Goal:** Test plan-detail rendering and the DataTables-powered list pages.

  **Requirements:** R26, R27, R28

  **Dependencies:** Unit 2

  **Files:**
  - Create: `frontend/e2e/plan-detail.spec.js`
  - Create: `frontend/e2e/plans.spec.js`
  - Create: `frontend/e2e/logs.spec.js`

  **Approach:**
  - **plan-detail.html**: Navigate to `?plan=plan-abc-123`, mock `/api/plan/:planId` with `MOCK_PLAN_DETAIL`, mock `/api/info/:testId` for each module that has instances (the page's `drawCallback` fetches test results per module)
  - **plans.html**: Mock `/api/plan` with DataTables-shaped response using the wrapper utility from Unit 2. DataTables sends `{draw, start, length, search, order}` — the wrapper reads these and returns `{draw, recordsTotal, recordsFiltered, data: MOCK_PLAN_LIST}`. Also mock `/api/info/:testId` for the `fetchTestResults` cascade (plans.html lines 314-346 fetch test status for visible rows)
  - **logs.html**: Mock `/api/log` with DataTables-shaped response using `mock-log-list.js` data. Same envelope pattern as plans.html

  **Patterns to follow:**
  - `src/main/resources/static/plan-detail.html` — `getPlan()` at line 248
  - `src/main/resources/static/plans.html` — DataTable config at line 172, `fetchTestResults` at line 314
  - `src/main/resources/static/logs.html` — DataTable config at line 169

  **Test scenarios:**
  - Happy path (R28): `plan-detail.html?plan=plan-abc-123` loads → plan name "oidcc-basic-certification-test-plan" rendered → variant info shown → module list shows 4 modules → action buttons visible
  - Happy path (R26): `plans.html` loads → DataTable initializes → rows show plan names, variants, started dates → action buttons (detail link, config button) in each row
  - Happy path (R27): `logs.html` loads → DataTable initializes → rows show test names, results (badges), timestamps, variants
  - Integration: DataTables mock correctly provides `{draw, recordsTotal, recordsFiltered, data}` envelope — table renders without JavaScript errors and shows correct row count

  **Verification:**
  - All 4 tests pass (1 plan-detail + 1 plans + 1 logs + 1 DataTables integration)
  - DataTables pages show the number of rows matching fixture data length

- [ ] **Unit 8: Cross-page journey tests**

  **Goal:** Test full user journeys that span multiple page navigations with coordinated mocks.

  **Requirements:** R21, R22

  **Dependencies:** Units 3-7

  **Files:**
  - Create: `frontend/e2e/journeys.spec.js`

  **Approach:**
  - Register all needed routes before the first `page.goto()` — Playwright's `page.route()` interceptors persist across page navigations on the same `page` object
  - **R21 journey**: schedule-test.html → fill cascade → submit → `POST /api/plan` returns `{id: "plan-journey-001"}` → verify navigation to `plan-detail.html?plan=plan-journey-001` → plan renders with modules → click run on a module → `POST /api/runner` returns `{id: "test-journey-001"}` → verify navigation to `log-detail.html?log=test-journey-001` → log entries render
  - **R22 (corrected)**: Start at `plan-detail.html?plan=plan-abc-123` → click run on a module → verify `POST /api/runner` called with correct `testName` and `planId` → redirect to `log-detail.html?log=:testId` → header shows correct test info
  - Each transition requires the next page's mocks to already be registered (hence pre-registering all routes)
  - The `POST /api/plan` and `POST /api/runner` mocks must return IDs that match what subsequent page mocks expect

  **Execution note:** These tests depend on all page-level tests passing first. If individual page tests are failing, journey tests will also fail at those pages.

  **Patterns to follow:**
  - `src/main/resources/static/schedule-test.html` — submission redirect at line 2585
  - `src/main/resources/static/plan-detail.html` — `runTest()` redirect at line 659

  **Test scenarios:**
  - Happy path (R21): Navigate to `schedule-test.html` → select spec family, entity, version, plan → fill required config → click "Create Test Plan" → URL changes to `plan-detail.html?plan=plan-journey-001` → plan name and modules visible → click run on first module → URL changes to `log-detail.html?log=test-journey-001` → log header shows test name and entries render
  - Happy path (R22, corrected): Navigate to `plan-detail.html?plan=plan-abc-123` → click run on module "oidcc-server" → `POST /api/runner?test=oidcc-server&plan=plan-abc-123` called → URL changes to `log-detail.html?log=test-run-001` → header shows test ID matches

  **Verification:**
  - Both journey tests pass end-to-end
  - Each navigation step verified: correct URL, correct API calls intercepted, correct content rendered on the destination page

## System-Wide Impact

- **Interaction graph:** E2E tests are read-only consumers of static files and mock API routes. No modifications to production code. The MSW handler fix (`/api/runner/available` → `/api/plan/available`) corrects an existing bug that also benefits Storybook stories.
- **Error propagation:** Test failures propagate via Playwright test runner → npm script exit code → CI pipeline.
- **API surface parity:** Fixture shapes must stay synchronized with actual backend API responses. If the backend changes an API response shape, both MSW handlers and Playwright route helpers need updating. This is an inherent trade-off of mocked testing.
- **Integration coverage:** Cross-page journey tests (Unit 8) verify that page transitions work correctly with coordinated mocks. `page.route()` persistence across navigations is the key integration seam.
- **Unchanged invariants:** Existing Storybook play-function tests continue to work unmodified. The Storybook test runner (`vitest.config.js`), MSW service worker setup, and component-level stories are not affected by the new E2E infrastructure.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| CDN unavailability in CI breaks all tests | Per R6, CI runners require internet access. CDN failures produce clear network errors. A local fallback is deferred to a future task. |
| `serve` MIME type handling differs from Spring Boot | `serve` uses standard MIME types. If issues arise, switch to Vite preview mode (Vite is already a dependency). |
| DataTables initialization timing with mocked responses | Use Playwright `waitForSelector` on DataTable rows or `waitForResponse` on the AJAX call. DataTables fires `drawCallback` after rendering. |
| schedule-test.html cascade complexity (3,359 lines of HTML) | Start with happy-path cascade only. Cascade edge cases deferred to future iterations. |
| `MOCK_RUNNING_TESTS` shape mismatch (objects vs string IDs) | Route helper extracts `._id` fields for the list endpoint. Existing MSW handlers continue using objects directly for Storybook. |
| `@playwright/test` and `playwright` version conflict | Pin `@playwright/test` to `^1.59.1` to match the existing `playwright` peer dependency. Future version bumps must keep both aligned. |
| `frontend/` directory only exists on branches with web component work | Plan and implementation target the branch that has `frontend/`. Verify the branch has the expected fixture files before starting. |

## Sources & References

- **Origin document:** [`docs/brainstorms/2026-04-14-frontend-e2e-test-suite-requirements.md`](docs/brainstorms/2026-04-14-frontend-e2e-test-suite-requirements.md)
- Static pages: `src/main/resources/static/*.html`
- Main frontend JS: `src/main/resources/static/js/fapi.ui.js`
- Existing fixtures: `frontend/stories/fixtures/`
- Storybook config: `frontend/.storybook/main.js`
- MSW handlers: `frontend/stories/fixtures/msw-handlers.js`
