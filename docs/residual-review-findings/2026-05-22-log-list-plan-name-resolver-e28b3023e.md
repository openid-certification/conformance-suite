# Residual Review Findings — log-list planName resolver

**Commits:** `f3d2463f2` (fix) + `e28b3023e` (autofix)
**Branch:** `feat/redesign`
**MR:** [!1998](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998)
**Plan:** `docs/plans/2026-05-22-005-fix-log-card-plan-link-shows-id-plan.md`
**Run artifact:** `/tmp/compound-engineering/ce-code-review/20260522-231330-4a57e1f3/` (local; not durable across machines)
**Review team:** correctness, testing, maintainability, project-standards, performance, reliability, adversarial, julik-frontend-races, agent-native, learnings (10 personas)
**Verdict:** Ready with fixes — 3 `safe_auto` applied in `e28b3023e`; 8 residual + 3 advisory below.

## Already applied in `e28b3023e`

- **F1 (P2)** — Render fallback uses `??` instead of `||` so an empty-string `planName` from the server doesn't silently downgrade to the raw `planId`. Flagged by 5 reviewers.
- **F2 (P1)** — Removed `@state` JSDoc on `_planNames`; rationale moved to an inline constructor comment per AGENTS.md §5 (underscore-prefixed state is private and not part of the external API).
- **F3 (P2)** — Documented the always-fulfilled contract of the `Promise.allSettled` callback. The `status === "fulfilled"` guard stays for TypeScript narrowing; the comment explains why removing the per-fetch `.catch` would leak in-flight entries.

## Residual actionable work (8) → owner: downstream-resolver

### P1

- **R1** — Add `AbortController` + `disconnectedCallback` so in-flight `/api/plan/<id>` fetches are cancelled on unmount (and address the missing 5s timeout). `src/main/resources/static/components/cts-log-list.js:660` · `gated_auto` · reviewers: julik-frontend-races, reliability, adversarial.

### P2

- **R2** — Invalidate `_planNames` cache when `isPublic` toggles. Latent in single-mount `logs.html` context but closes the door for future reuse where a private-fetched name could leak into a public render. `cts-log-list.js:645` · `gated_auto` · reviewers: adversarial, julik-frontend-races.
- **R3** — Evict null-cached entries on `_fetchLogs` retry so transient `/api/plan` failures don't permanently downgrade the chip. Keeps the no-retry-on-every-render guarantee while honoring user-initiated refresh as a recovery path. `cts-log-list.js:612` · `gated_auto` · reviewer: adversarial.
- **R4** — Cap parallel fan-out (chunked `Promise.allSettled`, or the `/api/plan?ids=…` batch endpoint already deferred in the plan). Worst case today is 1000 simultaneous requests if every log row references a distinct plan. `cts-log-list.js:653` · `gated_auto` · reviewer: performance.

### P3

- **R5** — Strengthen the 404-fallback e2e assertion via `page.waitForResponse('**/api/plan/**')` before asserting chip text. Currently the assertion passes whether the fallback fires from the unresolved window OR from the resolved-null branch. `frontend/e2e/logs.spec.js:294` · `manual` · reviewer: testing.
- **R6** — Add e2e coverage for `?public=true` propagation on plan-name fetches (R4 from plan). The Storybook `PublicListing` story already covers this; only the e2e gap remains. `frontend/e2e/logs.spec.js:420` · `manual` · reviewers: testing, project-standards.
- **R7** — Add a planName-substring search test (R5 from plan). Existing `SearchActive` story only types `rotate` which matches `testName`, bypassing the new haystack entry. `frontend/e2e/logs.spec.js` · `manual` · reviewer: testing.
- **R8** — Share plan-fetch cache with `_handleConfigButtonClick` to avoid double-fetching when the user clicks Config on a card whose `planId` is already resolved. Already listed as deferred follow-up in the plan. `cts-log-list.js:1070` · `gated_auto` · reviewer: adversarial.

## Advisory (report-only)

- **A1 (P2)** — Config-button concurrent-click overwrite: rapid clicks on different cards produce two concurrent `/api/plan/<id>` requests and whichever lands last writes `_selectedConfig`. Pre-existing handler, not introduced by this commit.
- **A2 (P3)** — `_searchedLogs` re-allocates haystack arrays per row per keystroke. Pre-existing perf issue; debouncing `_handleSearchInput` is the right fix.
- **A3 (P3, suppressed below confidence gate)** — `planId` of `..` would bypass `encodeURIComponent` and URL-collapse `/api/plan/..` to `/api/`. Defensible because `planId` is system-generated; the resulting 404 is also fine.

## Agent-native observations

- Consider adding `data-plan-id="${log.planId}"` to the chip `<a>` so an agent can read the machine id without URL-parsing the `href`.
- Consider `aria-label="Plan: ${planName}"` on the chip link so accessibility-tree crawlers distinguish it from the headline link by stable name.

## Learnings & past solutions

- `docs/solutions/best-practices/playwright-route-pattern-with-fallback-is-safe-2026-04-17.md` — relevant pattern when a future spec stubs both `/api/plan/:planId` and `/api/plan/available` in the same file.

## Verification status (post-autofix)

- Prettier ✓ — TypeScript ✓ — JSDoc lint ✓ — lit-analyzer 0 errors ✓
- Playwright `frontend/e2e/logs.spec.js`: **17/17 pass** (including the 2 new tests)
- Storybook `cts-log-list.stories.js`: **12/12 pass** (including the new `WithResolvedPlanNames` story)

## CI Failures Unresolved (pipeline `2547834125`, all pre-existing baseline)

The post-push CI run on `f475e198b` had 3 failing jobs; all three pre-date this fix and are not attributable to its commits.

- **`frontend_lint`** — pre-existing Prettier finding in `src/main/resources/static/components/cts-config-form.js`. Verified pre-existing at the start of this session via `git stash && prettier --check` on a clean tree. The job is `allow_failure: true` per `.gitlab-ci.yml` until the 2026-06-12 promotion date noted in CLAUDE.md and not introduced by this fix. https://gitlab.com/openid/conformance-suite/-/jobs/14514210652
- **`frontend_e2e_test`** — 3 HTML-snapshot failures in `frontend/e2e/schedule-test-baselines.spec.js` (State A / State B / State C). Diff shows new `cts-form-field` SUS-notice markup introduced by commit `d3eb66198` ("fix(cts-form-field): route PEM/JWKS/key config fields to `<textarea>`") landed by an unrelated session during this run; snapshots in `schedule-test-baselines.spec.js-snapshots/` were not updated alongside that commit. `158 passed`, `3 failed` — `frontend/e2e/logs.spec.js` (the file touched by this fix) contributed `17/17 pass` to the green set. The snapshot update belongs in a follow-up commit owned by the cts-form-field change. https://gitlab.com/openid/conformance-suite/-/jobs/14514210658
- **`deploy-review`** — review-app deployment failure (infrastructure, not source). https://gitlab.com/openid/conformance-suite/-/jobs/14514210655

No autofix loop entered: all failures originate from earlier commits on this long-lived branch; LFG step 8's repair loop only applies to failures attributable to the current change.
