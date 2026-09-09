# Frontend toolchain conventions

Conventions for the tooling under `frontend/`: the Playwright E2E specs in `frontend/e2e/`, the lint, format, unit-test and Storybook gates, and the CI jobs that run them. They also apply when a change under `src/main/resources/static/` needs those checks. Styling rules for the static UI (icons, badges) are in `src/main/resources/static/AGENTS.md`; `cts-*` component-authoring rules are in `src/main/resources/static/components/AGENTS.md`; the command reference is `README.md` in this directory.

## Frontend E2E Tests

Playwright E2E tests in `frontend/e2e/` validate the legacy static HTML pages (`src/main/resources/static/*.html`) with mocked API responses. No backend required.

```bash
# Run E2E tests (from frontend/ directory)
cd frontend && npm run test:e2e

# Run a single spec file
cd frontend && npx playwright test e2e/home.spec.js
```

**When to run:** After modifying any file in `src/main/resources/static/` — HTML pages, `js/fapi.ui.js`, `templates/`, or `css/`. These tests catch regressions in page-level behavior.

**When to update tests:** If you change an API response shape consumed by the frontend, update the corresponding fixture in `frontend/e2e/fixtures/`. If you change page structure (DOM IDs, CSS classes used by JS), update the affected spec files.

**Key conventions:**

- Each spec file covers a single page; `journeys.spec.js` covers cross-page flows
- Route helpers in `frontend/e2e/helpers/routes.js` — `setupFailFast()` must be called FIRST (Playwright matches routes in reverse registration order), then specific routes
- All `page.route()` calls must happen before `page.goto()` because `fapi.ui.js` fires an API call at script parse time
- Fixture data lives in `frontend/e2e/fixtures/` as ES modules
- The `wrapDataTablesResponse()` helper wraps plain arrays in the `{draw, recordsTotal, recordsFiltered, data}` envelope — the DataTables-style pagination contract still served by `/api/plan` and `/api/log` — for pages using server-side pagination (plans.html, logs.html)

## Frontend quality gates

Lint, format, unit tests, and type-check for the frontend are covered by the `frontend_lint` GitLab job, mirrored locally by `npm run test:ci` from `frontend/` (format:check → lint → test:unit → type-check → lint:jsdoc → lint:icons → lint:lit-analyzer → codegen:check). `test:unit` runs the Vitest `unit` project (`vitest run --project=unit`) — plain-JS unit tests under `components/`, `lib/`, and `js/`. This is the only place that project runs; it is not part of `test-storybook` or the e2e job. See `frontend/README.md` for the command reference and failure-mode decoder. `lit-analyzer` provides Lit-aware template diagnostics (unknown elements, wrong binding sigils, unclosed tags); `ts-lit-plugin` exposes the same diagnostics inside TypeScript-language-service IDEs. `lint:icons` validates that every literal `cts-icon name="<value>"` resolves to a vendored SVG under `vendor/coolicons/icons/` — see the Icons section above.

Severity ladder: default is `error`; R8 light-DOM preset warnings from `eslint-plugin-lit` / `eslint-plugin-wc` stay at `warn`; a named Legacy Overrides block in `frontend/eslint.config.js` tracks per-file exceptions to zero — never blanket `"off"`.

The CI job is blocking (promoted 2026-08-13 once the R22 criteria were met — see the comment above the `frontend_lint` job in `.gitlab-ci.yml`). The Storybook suite (play functions + axe a11y) is also blocking, inside `frontend_e2e_test`; the deferred page-level axe half is tracked in `frontend/README.md`'s "Accessibility testing" section.
