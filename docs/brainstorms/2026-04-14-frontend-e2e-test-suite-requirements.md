---
date: 2026-04-14
topic: frontend-e2e-test-suite
---

# Frontend E2E Test Suite

## Problem Frame

The conformance suite frontend is being incrementally migrated from legacy vanilla JS + lodash templates to Lit web components. Today, 26 components have Storybook stories with ~163 play-function tests, but no tests exercise the full page-level flows (schedule-test.html, running-test.html, log-detail.html, etc.). Without a regression safety net covering the legacy pages, refactoring risks silently breaking user-facing behavior.

The test suite must run without the Spring Boot backend, MongoDB, or HTTPS proxy — using mocked API responses so tests are fast, portable, and CI-friendly.

## Requirements

**Test Infrastructure**

- R1. Tests use Playwright to drive a real browser against the legacy static pages served from `src/main/resources/static/`.
- R2. API calls (`/api/*`) are mocked via Playwright's `page.route()` — no service worker injection into legacy pages. Every page load requires mocking at minimum: `/api/currentuser`, `/api/server`, and `api/ui/spec_links`. Any unlisted `/api/*` call encountered during test execution should fail the test with a clear error indicating the unmocked route. The `/templates/` directory containing lodash partials must be served by the static file server (not mocked).
- R3. Fixture data files (mock response shapes) should be structured for reuse by both Playwright E2E tests and the existing Storybook MSW handlers. Existing data files in `frontend/stories/fixtures/` (e.g., `mock-plans.js`, `mock-test-data.js`) are the starting point; import compatibility must be verified during planning. The MSW handler wiring (`msw-handlers.js`) is MSW-specific — Playwright tests will need their own `page.route()` setup that imports the shared data.
- R4. A static file server serves the contents of `src/main/resources/static/` (HTML pages, JS, CSS, lodash templates in `templates/`). CDN-loaded libraries (Bootstrap, jQuery, DataTables) load from their real CDNs.
- R5. Tests are runnable via a single `npm` script (e.g., `npm run test:e2e`) from the `frontend/` directory.
- R6. Tests run in CI without requiring the Java backend, MongoDB, or TLS proxy. CI runners must have internet access for CDN-loaded libraries.

**Test Scope — Test Plan Scheduling Flow**

- R7. Smoke: schedule-test.html loads, the Specification dropdown populates from `/api/plan/available`, and the user can navigate the 4-step cascade (Specification -> Entity Under Test -> Version -> Test Plan) to select a test plan.
- R8. Form interaction: navigating the cascade to select a test plan renders the appropriate variant selectors and configuration fields; filling required fields enables the "Create Test Plan" button.
- R9. Submission: submitting a valid configuration calls `POST /api/plan` and the page navigates to plan-detail.html with the created plan ID.
- R10. Validation: submitting with missing required fields shows validation feedback without calling the API.
- R11. Error state: when `/api/plan/available` returns a server error, the page shows a meaningful error state.

**Test Scope — Test Execution Flow**

- R12. Smoke: running-test.html loads, fetches the list of running tests from `/api/runner/running`, then fetches `/api/runner/:testId` and `/api/info/:testId` for each test, rendering test names, statuses, and variants in a table.
- R13. Manual refresh: clicking the Refresh button re-fetches `/api/runner/running` and updates the displayed test statuses. When mocked responses change between refreshes, the UI reflects the new statuses.
- R14. Exposed values: when a running test exposes URLs (e.g., authorization endpoint), they render as actionable links in the test row.
- R15. Completion: when all tests finish, the UI shows final results with links to each test's detailed log.

**Test Scope — Results / Log Detail Flow**

- R16. Smoke: log-detail.html loads with a log ID parameter, fetches log entries from `/api/log/:testId`, and renders the log header with test name, status, and result.
- R17. Log entry rendering: individual log entries display source, message, and result badges (success, failure, warning, info).
- R18. Log entry expansion: clicking a log entry expands it to show detailed content (HTTP request/response, requirements mapping).
- R19. Failed test: a test with FAILED result shows the failure summary section with the failing condition details.
- R20. Warning display: a test with WARNING results highlights warnings distinctly from failures and passes.

**Test Scope — Cross-Page Journeys**

- R21. Schedule-to-results journey: a full user journey from loading schedule-test.html, configuring and creating a plan, viewing the running test, and arriving at the log detail page. Each transition is driven by mocked API responses that chain the test/plan IDs.
- R22. Plan detail to test execution: navigating from plan-detail.html (module list) to a specific test's running-test.html, verifying the correct test ID propagates.

**Test Scope — Home, Plans, and Logs Pages**

- R25. Smoke: index.html loads, fetches `/api/server` and `/api/currentuser`, and renders server version info and user info with navigation buttons.
- R26. Smoke: plans.html loads, fetches the plan list from `/api/plan`, and renders a DataTable with plan names, statuses, and action buttons.
- R27. Smoke: logs.html loads, fetches the log list, and renders a DataTable with test names, results, and timestamps.
- R28. Smoke: plan-detail.html loads with a plan ID parameter, fetches `/api/plan/:planId`, and renders the plan name, variant, module list, and action buttons.

**Authentication Mocking**

- R23. All tests mock `/api/currentuser` to return an authenticated user. The user info (name, email) renders correctly in the page header.
- R24. At least one test verifies the unauthenticated state: when `/api/currentuser` returns 401, the page renders without user info in the header and the navbar shows only public navigation links (Published Logs, Published Plans, API Docs).

## Success Criteria

**Functional correctness**
- All tests pass against the current legacy frontend on `master` with zero backend dependencies.

**Regression detection**
- A deliberate breaking change to a legacy page (e.g., removing the Specification dropdown in schedule-test.html) causes at least one test to fail.

**Performance**
- Tests complete in under 60 seconds total (excluding browser download on first run).

**Fixture reusability**
- The fixture data files (mock response shapes) are importable by both Playwright tests and Storybook MSW handlers without duplication.

## Scope Boundaries

- **In scope**: Legacy static pages only (index.html, schedule-test.html, running-test.html, log-detail.html, plan-detail.html, plans.html, logs.html).
- **Out of scope**: Thymeleaf-rendered pages (OAuth callbacks, session iframes) — these require server-side rendering.
- **Out of scope**: Testing individual Lit web component behavior — those are covered by Storybook play-function tests. However, legacy pages embed components like cts-navbar, cts-modal, and cts-tabs; E2E tests implicitly verify these load and render within their host pages.
- **Out of scope**: Visual regression testing (screenshot comparison) — focus is behavioral correctness.
- **Out of scope**: Login flow with real OAuth — mock the authenticated state via `/api/currentuser`.
- **Not replacing**: The existing Storybook component tests. E2E tests complement them by covering page-level integration.

## Key Decisions

- **Playwright route() over MSW for mocking**: Legacy pages should not be modified to register a service worker. Playwright's network interception is transparent to the pages under test, which means the tests validate the same code that runs in production.
- **Hybrid fixture sharing**: Fixture data lives in a shared location importable by both Playwright and MSW. No duplication of mock API response shapes.
- **Static file server, not Spring Boot**: Tests serve files from `src/main/resources/static/` directly. This proves the frontend works independently and keeps test execution fast.
- **Full flow coverage from the start**: Given the goal is a safety net for refactoring, smoke tests alone wouldn't catch integration breakage between pages. Cross-page journeys (R21, R22) are essential.

## Dependencies / Assumptions

- `@playwright/test` must be added as a devDependency in `frontend/package.json` (the existing `playwright` package is only a peer dependency of `@vitest/browser-playwright` and does not include the test runner, `page.route()` fixtures, or `expect` assertions).
- CDN-loaded libraries (Bootstrap 5.3.3, jQuery 3.6.4, DataTables 1.13.4, etc.) remain accessible during test runs. If CI has no internet, tests will fail — a local fallback is deferred to planning.
- The `frontend/` directory's `package.json` is the home for E2E test configuration and scripts.
- Existing MSW fixtures in `frontend/stories/fixtures/` match the current API response shapes.

## Outstanding Questions

### Deferred to Planning

- [Affects R4][Technical] Which static file server to use — Playwright's built-in `webServer` config vs. `npx serve` vs. Vite? The choice affects how template partials and CDN references resolve.
- [Affects R3][Needs research] Can the existing Storybook MSW fixture modules be imported directly by Playwright tests, or do they need a build step / format change to work in both contexts?
- [Affects R21][Technical] How to chain cross-page navigation in Playwright when each page is a separate HTML file (no SPA routing) — direct URL navigation with query params, or intercepting link clicks?
- [Affects R13][Technical] How to simulate changing state across manual refreshes — Playwright route() can return different responses on successive calls, but the exact pattern for mocking evolving `/api/runner/running` responses needs design.

## Next Steps

-> `/ce:plan` for structured implementation planning
