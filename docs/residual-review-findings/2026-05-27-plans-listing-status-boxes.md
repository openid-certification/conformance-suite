# Residual Review Findings — plans listing card layout + module status boxes

Source: `ce-code-review mode:autofix` run `20260527-150713-bb0fac0c` over the
plans-listing change on `feat/redesign` (commits `dbcbe17de..2c2788f0c`).
Plan: `docs/plans/2026-05-27-002-feat-plans-listing-cards-status-dots-plan.md`.

Five findings were auto-applied (lookup-table class, aria-label status word,
click dead-zone pointer-events, cached view, gated `updated()` resolver — see
commit `fix(review): apply autofix feedback`). The items below were validated
as real but **deferred** (behavior-changing or cross-scope) rather than
auto-applied. None block the feature; several are parity gaps shared with the
sibling `cts-log-list`.

## Residual Review Findings

- **P2 · `src/main/resources/static/components/cts-plan-list.js` (~`_resolveVisibleModuleStatuses` fetch)** — No timeout / `AbortController` on the per-module `/api/info` fetch. A hung connection keeps the instance in `_infoFetchesInFlight` indefinitely, so its status box never settles (stays gray past the pulse cap). *Reviewers: reliability, adversarial. Parity gap shared with `cts-log-list._resolvePlanNames`.* Fix shape: wrap each fetch in `AbortSignal.timeout(...)`.

- **P2 · `src/main/resources/static/components/cts-plan-list.js` (lifecycle)** — No `disconnectedCallback` to abort in-flight fetches; a fetch resolving after the element is removed mutates a detached instance and reassigns `_plans`. *Reviewers: julik-frontend-races, reliability. Low impact on `plans.html` (plain static page, no Turbo remount); parity with `cts-log-list`.* Fix shape: one `AbortController` per `connectedCallback`, aborted on disconnect.

- **P2 · `src/main/resources/static/components/cts-plan-list.js` (fan-out)** — The visible-card fetch gate bounds the number of *cards* fetched, not the per-card *module* count. A page of 25 cards each with dozens of modules can still fire hundreds–thousands of simultaneous `/api/info` requests per "Show more". *Reviewers: adversarial, performance.* Fix shape: cap concurrency with a small worker pool / batched N-at-a-time.

- **P2 · multiple components** — `formatVariant` is duplicated across `cts-plan-list`, `cts-log-list`, `cts-plan-modules`, `cts-plan-header`, `cts-running-test-card` with subtle divergence (some lack the string-variant shortcut). *Reviewer: maintainability. Pre-existing duplication; this change added one more copy.* Fix shape: hoist into `js/module-status.js` (or a `js/variant-format.js`) and import in all five.

## Advisory (report-only, not tracked as actionable)

- Testing gaps: assert `?public=true` on the `/api/info` request; assert the off-screen fetch upper-bound stays at one batch; cover `RUNNING`/`REVIEW`/`SKIPPED` box variants; a 500 (vs 404) `/api/info` case; `started-asc` sort and show-more/empty-state e2e.
- Tokenize the `32px`/`18px` status-box dimensions and the `10`-iteration pulse cap; consider inlining the two-hop `_handlePlanLinkClick → _handlePlanClick` / `_handleConfigButtonClick → _handleConfigClick` delegations.

## Rejected (false positives, recorded for traceability)

- "File crosses 1000 lines" — the file is 939 lines (< 1000).
- "`finally` clears in-flight before `allSettled.then`, allowing a duplicate fetch" — the per-fetch `.then`/`.catch` sets `_statusResolved=true` before `.finally` runs, so the guard holds; verified by the correctness reviewer.
- "`connectedCallback` re-fetch on remount" — `plans.html` is a plain static page with no Turbo/morphing boundary.
- "`inst-004` / `ensure-signed-request` not in `MOCK_PLAN_LIST`" — they are present in the Storybook `@fixtures` copy; all 18 plan-list stories pass.
