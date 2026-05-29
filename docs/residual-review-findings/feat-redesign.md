# Residual Review Findings — feat/redesign

Durable record of non-blocking review findings that were not auto-fixed during
`/lfg` autofix passes on the `feat/redesign` branch (ships via GitLab MR !1998,
so there is no GitHub PR body to host these). Append new findings as later units
land; remove an entry when it is resolved.

## Residual Review Findings

### From U8 + U9 (plans-page-as-home) — review run `20260529-080529-1e428340`, 2026-05-29

- **[P3] Stale `current-page` attributes after the U9 nav collapse** —
  `src/main/resources/static/index.html` sets `current-page="home"` and
  `src/main/resources/static/schedule-test.html` sets `current-page="create-test"`.
  Both page keys were removed from `cts-navbar`'s `NAV_LINKS` in U9, so they now
  match no nav link and highlight nothing (benign dead config, not a bug). Out of
  the U8/U9 changed-file scope. **U11 has since deleted `index.html`**, so only
  `schedule-test.html` needs a one-attribute cleanup (`current-page=""`).
  Owner: downstream-resolver.

#### Pre-existing / advisory (not introduced by U8/U9 — recorded for context, no action required here)

- **[P2] `cts-plan-list.js` exceeds 1k lines** (1025 → 1112). Already over the
  threshold before this diff; U8 added 87 coherent lines. The plan's "no
  abstraction without a second consumer" rule defers an empty-state render-helper
  extraction.
- **[P3] `cts-plan-list._fetchPlans` has no `_fetchSeq` fetch-generation guard.**
  Already a tracked U6 deferred follow-up (port the U7 `_fetchSeq` pattern). U8
  does not add a new re-fetch path — the `authenticated` attribute gates render
  only. See `docs/solutions/web-components/fetch-generation-guard-for-page-driven-components.md`.
- **[P3] 36px button height hard-coded** in `cts-plan-list.js` (and 5 other files);
  introduce a shared `--oidf-btn-height` token. Pre-dates this diff.
- **[P3] `cts-link-button` `icon` JSDoc says "Bootstrap Icons"** but the component
  resolves coolicons. Misleading for future callers; `add-plus` resolves correctly.
  Out of this diff's scope.

### From U10 + U11 (plans-home backend routing + dashboard retirement) — review run `20260529-115433-23592b07`, 2026-05-29

Atomic commit `30a64c00e` (feature) + `fbc6d2a42` (autofix). 7-reviewer panel;
security + adversarial + correctness confirmed the security boundary clean
(private-link lockout preserved by placing the new `permitAll` after the
`denyAll` matcher; `/api/*` data boundary untouched; OTT auth handler untouched;
no open redirect; no matcher bypass). Autofix applied a CI-blocker fix
(`lit-importmap.spec.js` still probed the deleted `/index.html`) plus four
comment/doc-rot fixes. Residuals not auto-fixed:

- **[P2] No automated server-level test for the routing/security change.** The
  302 redirects (`/`, `/index.html` → `/plans.html`), the anonymous `permitAll`,
  the private-link-denied-on-listing ordering invariant, and the OTT
  token-generation redirect target have no JUnit/MockMvc coverage. **Intentional
  per plan KTD2** — booting a Spring context needs a live MongoDB (`Application`
  connects to Mongo on `ApplicationReadyEvent`; no embedded-Mongo dependency),
  OAuth2 client-registration test config, and `spring-security-test`, none of
  which exist in the repo. Follow-up: build Spring integration-test
  infrastructure, then add `HomeRoutingTest.java` (assert the redirects + anon
  resolves + a private-link session gets 401/403 on `/plans.html`//`logs.html`,
  which locks the denyAll-before-permitAll ordering that is currently
  test-unguarded). This slice is verified by live-browser smoke instead. Owner:
  downstream-resolver. Also recorded in the plan's *Deferred to Follow-Up Work*.
- **[P3] Anonymous `/logs.html` data visibility now leans on `cts-logs-view`
  never auto-setting `is-public`.** Composition risk (adversarial, confidence
  50): today it fails closed — anonymous `/api/log` without `?public=true` → 401,
  so anonymous `/logs.html` renders an empty shell. But `PublicRequestMatcher`
  checks only the `?public` query param, never auth state, so a *future* edit
  that derives public-mode from anonymous auth state would expose the full
  published-log listing with no server-side gate. Guard: assert (story/e2e) that
  anonymous `cts-logs-view` never sets `is-public`, or move the
  published-vs-private decision server-side. Not triggered by this diff. Owner: human.

#### Resolved by this unit

- The U8/U9 *stale `current-page="home"` on `index.html`* residual (above) is
  resolved — U11 deleted `index.html`. Only the `schedule-test.html` half remains.

#### Advisory (kept, not changed)

- The new `permitAll` block comment in `WebSecurityOidcLoginConfig` is verbose
  (~12 lines) for a 4-line rule (maintainability M-3). Kept deliberately —
  explicit rationale on security-filter ordering is worth the length.
- `publicRequestMatcher`'s `/plans.html`//`logs.html` entries are now redundant
  (shadowed by the new unconditional permit). A clarifying comment was added; the
  matcher itself was left untouched per the plan's minimal-diff decision on
  maintainer-gated security code. A future cleanup may remove the two dead entries.

### From U12 (terminology consistency + Published descriptor) — review run `20260529-124332-4772b17f`, 2026-05-29

Atomic commit `dec0d6300` (feature) + `9112f7a04` (autofix). 7-reviewer panel
(correctness, julik-frontend-races, testing, maintainability, project-standards,
agent-native, learnings). Two `safe_auto` fixes applied in `9112f7a04`: FIX-1 —
plans.html resolved the Published descriptor against a stale `authenticated`
flag, so an authed user clicking Published *during* the `/api/currentuser` probe
window got it wrongly hidden on Published; now derives from the live URL like the
sibling run-strip (corroborated by correctness + testing + a documented
docs/solutions/ pattern). FIX-2 — the AE4 "first paint" e2e tests asserted only
eventual visibility; added a synchronous `page.evaluate` reveal-check. Residuals
not auto-fixed:

- **[P2] logs.html drops a back/forward popstate that arrives inside the
  `customElements.whenDefined('cts-log-list')` window.** (julik-frontend-races,
  confidence 75.) The `cts-view-tab-change` listener — which updates both
  `publishedDesc.hidden` and the list's `is-public` — is registered only after
  `await getUserInfo()` AND `await customElements.whenDefined(...)`. A popstate in
  that gap is dropped, leaving the descriptor and dataset stale for the page
  lifetime. **Pre-existing U6 async-wiring shape, not introduced by U12** — the
  new descriptor toggle merely rides on it. Practical likelihood is near-zero:
  `cts-log-list` is a `<head>` module script fetched during HTML parse, so
  `whenDefined` almost always resolves before `getUserInfo` (a network
  round-trip) settles. plans.html is immune (its listener is registered
  synchronously before the fetch). Correct fix: register the listener right after
  the `getUserInfo` await (guard list ops with `if (list)`) and re-read the live
  URL for the mount-time `is-public` (mount currently uses `explicitPublic`
  captured at DOMContentLoaded) — this restructures U6 lifecycle beyond U12's
  add-a-descriptor scope. Bundle with the `_fetchSeq` guard follow-up below.
  Owner: downstream-resolver. `requires_verification` (delayed-module +
  synthetic-popstate e2e).

#### Advisory (kept, not changed)

- **Testing gaps** (testing reviewer): plans.html `.catch()` descriptor branch
  (network failure of `/api/currentuser` → `hidden=true`) is code-covered but
  e2e-untested; popstate descriptor toggling is covered implicitly via the shared
  handler but not asserted; logs.html has no catch block so a `getUserInfo` throw
  leaves the descriptor at the cache-guess. Low priority.
- **[P2 advisory] Descriptor invariant scattered across 7 sites** (maintainability
  M1): "descriptor ⟺ Published" is expressed at 4 sites in plans.html + 3 in
  logs.html. Acceptable at 2 listing pages; becomes a P1 trap at 3+. If a third
  listing page appears, extract a `setDescriptor(el, isPublic)` local helper per
  page (NOT a Lit component — copy differs per page, KTD1).
- **[P2 advisory] `.listing-page-header` markup duplicated across plans.html and
  logs.html** (maintainability M2). Correct per KTD1 (copy differs; no shared
  component). Consider a `<!-- keep in sync with logs.html -->` coupling comment.

#### Known pattern (pre-existing debt surfaced by learnings researcher)

- `cts-plan-list._fetchPlans` and `cts-log-list._fetchLogs` are still un-guarded
  by the `_fetchSeq` fetch-generation counter
  (`docs/solutions/web-components/fetch-generation-guard-for-page-driven-components.md`).
  U12 does not introduce this but operates over the same `cts-view-tab-change` /
  popstate surface — a stale fetch resolving after a tab swap could make the list
  data disagree with the descriptor. Port the guard (and fix JFR-1's listener
  timing) in one future pass.
