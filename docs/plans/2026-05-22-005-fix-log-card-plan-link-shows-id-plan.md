---
status: active
type: fix
depth: lightweight
created: 2026-05-22
---

# fix: log-card plan chip shows planId instead of planName

## Problem Frame

On `https://localhost.emobix.co.uk:8443/logs.html`, every log card's "Plan" meta chip renders the raw MongoDB plan document id (e.g. `uvwxyz123abc`) instead of the spec-identifier plan name (e.g. `fapi2-security-profile-final-test-plan`). The opaque id is unhelpful — users cannot tell which spec/profile a test ran under without clicking through to plan-detail.

Root cause: `/api/log` returns `TestInfo` records whose only plan reference is `planId`. The `_renderCard` method in `cts-log-list.js` (line 959) interpolates `${log.planId}` directly as the link text. The `planName` field lives on the `Plan` document and is only fetched separately via `/api/plan/<id>` — log-detail.js already does this for its breadcrumb.

## Scope

**In scope**
- `cts-log-card-plan-link` displays a meaningful plan name on logs.html for every card whose plan can be resolved.
- Fallback to `planId` when the plan fetch fails (404, network error, deleted plan) so we never render an empty chip.
- Search haystack (`_searchedLogs`) includes the resolved planName so users can text-search by plan identifier.
- Storybook story coverage (mock plan resolution) and Playwright e2e coverage on `logs.spec.js`.

**Out of scope (deferred to follow-up work)**
- Server-side enrichment of `/api/log` with `planName` (would avoid the planId→planName flash but requires Java DTO + serialization changes).
- Showing the human `displayName` from `@PublishTestPlan(displayName=...)`. The plan document only stores `planName` (kebab-case identifier); `displayName` is not persisted on the `Plan` model. Showing it would need a backend lookup against `VariantService`.
- Caching the `/api/plan/<id>` response so the "Config" button click reuses it instead of re-fetching.

---

## Requirements

- **R1.** The "Plan" chip on each log card on `logs.html` MUST display the plan's `planName` (e.g. `fapi2-security-profile-final-test-plan`) once resolved.
- **R2.** Until the plan is resolved (and when resolution fails), the chip MUST fall back to `planId` so it is never blank.
- **R3.** The link `href` (`plan-detail.html?plan=<planId>`) MUST remain unchanged — only the link text changes.
- **R4.** Resolution MUST honor the `?public=true` query when `isPublic` is set on `cts-log-list`.
- **R5.** When the user types into the logs search box, matches MUST also fire against the resolved `planName` (in addition to the existing `planId` match).

---

## Approach

Use the existing `/api/plan/<id>` endpoint to resolve plan names client-side. After `cts-log-list` receives its `logs` array, dispatch a parallel batch of plan fetches, one per unique `planId`, and store the results in a reactive `Map<planId, planName | null>` on the component. The render path consults the map and degrades to `planId` when the entry is missing or null.

Why client-side, not server-side:
- Minimal blast radius — no Java/DTO/migration changes, no risk to public/private permission boundaries.
- Mirrors the established pattern in `js/log-detail.js#fetchAndApplyPlanState`, which already does the same lookup for the breadcrumb middle crumb.
- Plans per page are bounded: a 50-row page typically references far fewer unique plans, so the request fan-out is small.
- The Storybook mock harness and the e2e mocks already stub `/api/plan/**`; the new behavior fits the existing test surface.

The initial paint will briefly show `planId`, then swap to `planName` once `/api/plan/<id>` resolves. This is acceptable for a Lightweight fix; the optimistic-then-resolve pattern matches what log-detail.js already does. The flash can be eliminated later by a server-side enrichment in `/api/log` (deferred).

---

## Files

- `src/main/resources/static/components/cts-log-list.js` — render path, plan-name resolver, search haystack
- `src/main/resources/static/components/cts-log-list.stories.js` — Storybook story showing the resolved-name state
- `frontend/e2e/logs.spec.js` — Playwright assertions on the chip text (resolved + fallback)
- `frontend/e2e/fixtures/mock-log-list.js` — no changes expected; existing `planId: "plan-001"` etc. are enough
- (no Java changes)

---

## Implementation Units

### U1. Add a planName resolver to `cts-log-list`

**Goal:** Component fetches a `planName` for every unique `planId` referenced in `this._logs` and stores the results in a reactive map; the render path uses the map with a `planId` fallback.

**Requirements:** R1, R2, R3, R4

**Dependencies:** none

**Files**
- `src/main/resources/static/components/cts-log-list.js` — add reactive `_planNames` state (`@state()`-style), a `_resolvePlanNames(logs)` private method, hook into the `logs` setter / `willUpdate` lifecycle, update `_renderCard` line 959 to consume the map, JSDoc updates per project convention.

**Approach**
- Add a private reactive state field `_planNames` of type `Map<string, string | null>`. The map key is `planId`; the value is the resolved `planName`, or `null` to mark "tried, no name" (so we don't retry on each render).
- On `logs` assignment (`updated` or a `willUpdate` hook keyed on the `logs` prop), compute the set of unique non-empty `planId` values, subtract any already keyed in `_planNames`, and `fetch('/api/plan/' + encodeURIComponent(planId) + (this.isPublic ? '?public=true' : ''))` for each in parallel via `Promise.allSettled`.
- For each settled response: on 2xx + JSON parse, set `_planNames.set(planId, body.planName ?? null)`; on any failure, set `_planNames.set(planId, null)`. After the batch resolves, call `this.requestUpdate()` once so the cards re-render together (a single repaint, not per-fetch flicker).
- In `_renderCard`, replace `${log.planId}` on the existing link element (line 959) with `${this._planNames?.get(log.planId) ?? log.planId}`. The link `href` (already computed via `planHref`) is untouched.
- Use a small in-flight set or a guard so concurrent `logs` reassignments don't double-fetch the same planId.

**Patterns to follow**
- `src/main/resources/static/js/log-detail.js#fetchAndApplyPlanState` — same `/api/plan/<id>` fetch shape, same `isPublic` suffix handling, same "swallow failures and fall back" posture.
- `src/main/resources/static/components/cts-log-list.js` (existing) — reactive state, `@property`/`@state` declarations, `requestUpdate()` usage, JSDoc `@property` and `@state` annotations (per `feedback_jsdoc_annotations`).

**Test scenarios**
- Happy path: `_logs` contains two rows with `planId: "plan-001"` and one row with `planId: "plan-002"`; component issues exactly two `/api/plan/...` requests; once both resolve with distinct `planName`s, all three cards render the correct name.
- Idempotence: reassigning `_logs` with the same set of `planId`s does NOT issue additional `/api/plan/...` requests.
- Public mode: with `isPublic = true`, every `/api/plan/...` request URL includes `?public=true`.
- Fallback — 404: a planId returning 404 leaves the chip text equal to that `planId`; no console error is raised.
- Fallback — network failure: a planId rejecting (simulated network reject) leaves the chip text equal to `planId`; subsequent re-renders don't re-fetch.
- Missing planId: rows without `planId` continue to omit the Plan chip entirely (existing `planHref` null guard at line 910 stays correct).

**Verification**
- Visually load `/logs.html` against a dev server, confirm each card shows the kebab-case `planName` (e.g. `oidcc-basic-certification-test-plan`) within ~1s of initial paint.
- Network panel shows one `/api/plan/<id>` request per unique planId on the page, not one per card.
- Toggling sort/filter does not re-issue plan-name fetches.

---

### U2. Include resolved planName in the search haystack

**Goal:** Users typing the kebab-case plan identifier (or any substring of it) into the search box match the corresponding cards.

**Requirements:** R5

**Dependencies:** U1

**Files**
- `src/main/resources/static/components/cts-log-list.js` — modify `_searchedLogs(rows)` (around line 724) to add `this._planNames?.get(row.planId)` to the haystack array.

**Approach**
- In the haystack `[row.testName, row.testId, row.description, row.planId, formatVariant(row.variant)]`, append `this._planNames && this._planNames.get(row.planId)`. The existing `.filter(Boolean)` chain naturally drops missing values.
- Keep `row.planId` in the haystack so users who paste a raw planId from a URL still find their card.

**Patterns to follow**
- Existing `_searchedLogs` in `src/main/resources/static/components/cts-log-list.js` line 724-740 — same `Array.filter(Boolean).join(' ').toLowerCase()` shape.

**Test scenarios**
- Typing a substring of `planName` (e.g. `fapi2-security`) filters down to the matching cards once the plan map has resolved.
- Typing a substring of `planId` (e.g. `plan-001`) still matches (regression guard).
- Typing during the brief pre-resolution window matches only `planId` (existing behavior); once names resolve, the search re-runs naturally via the reactive update.

**Verification**
- Manual: type a plan identifier into `#logsListing input[type="search"]` and confirm cards collapse to the matches.
- E2E (covered in U3).

---

### U3. Storybook + Playwright coverage

**Goal:** The new behavior is exercised by both Storybook (visual + interaction) and Playwright (e2e). Future regressions in either the resolved-name path or the fallback path get caught.

**Requirements:** R1, R2, R5

**Dependencies:** U1, U2

**Files**
- `src/main/resources/static/components/cts-log-list.stories.js` — add or extend the `Default` (or a new `WithResolvedPlanNames`) story to stub `fetch('/api/plan/...')` via the story's `loaders` / `beforeEach`, then assert in a `play` function (per `feedback_storybook_interaction_tests`) that the rendered chip text matches `planName`, not `planId`.
- `frontend/e2e/logs.spec.js` — add two tests:
  - "log card plan chip shows planName once `/api/plan/<id>` resolves" — register a `**/api/plan/**` route returning `{ _id: planId, planName: "oidcc-basic-certification-test-plan" }`, assert `.cts-log-card-plan-link` text equals the planName.
  - "log card plan chip falls back to planId on 404" — register the route to return 404, assert chip text equals the row's `planId`.

**Approach**
- The existing `config button in card opens config modal` test at `frontend/e2e/logs.spec.js:130` already stubs `**/api/plan/**` returning `{ _id: planId, config: {...} }`. Extend that fixture to include `planName: "<something>"` so it doubles as coverage for the resolved-name path, OR add a dedicated route helper.
- Use `setupFailFast(page)` first per the existing convention, then register the plan-route stub before `setupLogListRoute` (Playwright matches routes in reverse registration order — see `feedback_storybook_story_url_pollution` / CLAUDE.md frontend section).
- For the Storybook story, prefer `play` over snapshot assertions for the chip text since Lit comment markers will pollute DOM snapshots (see `feedback_lit_marker_snapshot_normalization`).
- For the 404 fallback test, mock `/api/plan/**` returning `route.fulfill({ status: 404, contentType: 'application/json', body: '{}' })` and assert `await expect(planLink).toHaveText(MOCK_LOG_LIST[0].planId)`.

**Patterns to follow**
- `frontend/e2e/logs.spec.js:130-149` — `**/api/plan/**` route stub pattern.
- `frontend/e2e/log-detail.spec.js:128-140` — exact `_id` + `planName` JSON shape returned by `/api/plan/<id>`.
- Existing `cts-log-list.stories.js` — story shape, `args` wiring.

**Test scenarios**
- Storybook `WithResolvedPlanNames` (play fn): mock plan fetch returns `planName: "test-plan-resolved"` → `.cts-log-card-plan-link` text equals `"test-plan-resolved"`.
- Storybook `WithPlanFetchFailure` (play fn, optional): mock plan fetch rejects → chip text equals the raw `planId`.
- Playwright "resolved planName" test: chip text on the first card equals the mocked `planName`.
- Playwright "404 fallback" test: chip text equals `MOCK_LOG_LIST[0].planId` (i.e. `"plan-001"`).

**Verification**
- `cd frontend && ./node_modules/.bin/playwright test e2e/logs.spec.js` — both new tests pass.
- `cd frontend && npm run test:ci` — lint / typecheck / lit-analyzer pass.
- Storybook play function passes via `run-story-tests`.

---

## Deferred to Follow-Up Work

- **Server-side enrichment of `/api/log`** — eliminate the planId→planName flash by joining plan data server-side. Requires `TestInfo` DTO/Jackson adjustments, pagination response wrapper changes, and Java unit tests. Worth a dedicated plan if the flash bothers users in practice.
- **Display the human `displayName`** — the `@PublishTestPlan(displayName=...)` value (e.g. "FAPI 2.0 Security Profile Final") would read better than the kebab-case `planName`. Requires either persisting `displayName` on the `Plan` model at create time, or a second `VariantService` lookup keyed by `planName`. Defer until product asks for it.
- **Plan-fetch cache shared with the Config button** — `_handleConfigButtonClick` at line 994 issues its own `/api/plan/<id>` fetch on click. After U1 resolves names, that response is already in flight or cached locally. A small shared `_planCache` could deduplicate, but the savings are minor for this fix.

---

## Risks

- **Risk: `/api/plan/<id>` access denied for some logs in the listing.** The `/api/log` listing can include logs whose plan was created by a different user, but the current authorization for `/api/plan/<id>` requires the requester to be admin or the plan owner. For non-admin users, some plan fetches may 403/404. **Mitigation:** the fallback in U1 (chip stays as `planId`) handles this gracefully without any error surface. The user is no worse off than today.
- **Risk: Many unique plans on a page → many parallel fetches.** Typical pages reference ≤20 unique plans; even 50 parallel small JSON fetches is well within browser limits. **Mitigation:** none needed at this scale. If it becomes a problem, batch through a hypothetical future `/api/plan?ids=...` endpoint.
- **Risk: Pre-existing e2e flakes on `logs.spec.js`.** The memory `feedback_e2e_pre_existing_failures_2026_05_20` notes baseline e2e failures on `feat/redesign`. **Mitigation:** before claiming the new tests pass, stash-verify the baseline so we know we are not blaming our new tests for pre-existing flakes.

---

## Verification

End-to-end:
1. `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` — build still green (no Java changes, this is a smoke check).
2. `cd frontend && ./node_modules/.bin/playwright test e2e/logs.spec.js` — all logs.spec tests pass including the two new assertions.
3. `cd frontend && npm run test:ci` — lint / format / typecheck / lit-analyzer / icon checks pass.
4. Manual: load `https://localhost.emobix.co.uk:8443/logs.html`; every card's "Plan" chip shows a kebab-case plan name (not a hash-looking id); the link target still points at `plan-detail.html?plan=<id>`.
5. Manual: type a planName substring into the search box; matching cards stay visible; non-matching cards drop out.
