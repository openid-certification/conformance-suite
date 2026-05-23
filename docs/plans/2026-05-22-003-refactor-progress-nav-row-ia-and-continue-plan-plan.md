---
title: "refactor: lift progress nav row above sticky bar (IA) + verify Continue Plan"
type: refactor
status: active
created: 2026-05-22
related:
  - docs/plans/2026-05-22-002-fix-mr1998-maintainer-feedback-plan.md
  - src/main/resources/static/components/cts-log-detail-header.js
  - src/main/resources/static/components/cts-test-nav-controls.js
---

# refactor: lift progress nav row above sticky bar (IA) + verify Continue Plan

## Summary

Two changes on `feat/redesign`'s log-detail page:

1. **IA reorder.** The page renders breadcrumb → sticky status bar → nav row (progress + Continue) → terminal banner → hero → drawer. The nav row carries plan-level orientation ("Plan progress: Module 1 of 38"), which sits one level UP the hierarchy from the sticky bar's per-test verdict/action affordances. Pull the nav row above the sticky bar inside `cts-log-detail-header` so the visual order matches the IA: breadcrumb → nav row (plan progress + Continue) → sticky bar (this test's verdict + actions) → banner → hero → drawer. The sticky bar still sticks; the nav row scrolls off with the breadcrumb on long pages.

2. **Continue Plan — verify or stub.** U4 (commit `0bd815594`) already wired the Continue button to `POST /api/runner` with the next module's `testName`. Expectation is that it works. The user asked: confirm in-situ on a real plan, and **if** any failure path still requires backend work, fall back to the SUS-style placeholder pattern that already exists for the Validate Configuration button (`cts-config-form.js`, commit `c5bf0bf74`) — `cts-tooltip` plus an explanatory `cts-modal` that explains the gap.

## Problem Frame

### IA reorder

The current zone order inside `cts-log-detail-header._renderContent` (see `cts-log-detail-header.js:1583`) renders the sticky status bar first, then the nav row, then the hero, then the drawer. The sticky bar carries verdict + action affordances for the *current* test; the nav row carries the *plan* context ("Plan progress: Module 1 of 38" + Continue Plan). Putting the leaf (test-level) before the branch (plan-level) inverts the IA hierarchy that the page-level breadcrumb (`cts-crumb` in `log-detail.html:223`) already establishes.

Closing this gap with a minimal change: swap the order of `_renderTestNavControlsRow` and `_renderStickyBar` inside the component's `_renderContent` template so the nav row precedes the sticky bar visually. The sticky bar's `position: sticky; top: 0` behaviour is unaffected — the element above a sticky one in DOM order scrolls normally and the sticky element pins to the viewport edge when its top hits 0.

### Continue Plan

U4 fixed the original HTTP 400 by routing through `buildRunnerUrl(next.testModule, info.planId, next.variant)` and resolving "next module" from the cached `/api/plan` modules list (`log-detail.js:807-845`). The flow:

1. `cts-test-nav-controls` fires `cts-continue` with `{ testId, planId }`.
2. `log-detail.js:1002` listens via `header.addEventListener("cts-continue", handleContinue)`.
3. `handleContinue` finds the *next* entry in `cachedPlanModules` (subset-match on variant), POSTs to `/api/runner?test=<next.testModule>&plan=<planId>&variant=<json>`, then navigates to `/log-detail.html?log=<new-id>` on success.

In-situ verification of this end-to-end flow has not been recorded since the U4 commit. The user's request asks for either confirmation that the button works or, if any case still fails, a `cts-tooltip` + `cts-modal` "feature not implemented yet" stub mirroring `cts-config-form.js:90-100` / commit `c5bf0bf74`. Investigation-first: only the modal path materializes if verification surfaces a failure.

---

## Scope Boundaries

### In scope

- Reorder `_renderTestNavControlsRow` and `_renderStickyBar` inside `cts-log-detail-header.js` so the nav row precedes the sticky bar.
- Adjust the scoped CSS in `cts-log-detail-header.js:309-351` (`.ctsNavRow`) to match the new vertical adjacency — currently the rule reads as "the row that sits directly under the sticky bar"; comments and border/padding choices may need flipping (e.g., `border-bottom` vs `border-top`).
- Adjust the empty-nav-row hide rule (`.ctsNavRow:has(cts-test-nav-controls:empty) { display: none; }`) to still suppress the row when there is nothing to render (transient state before `/api/plan` resolves).
- Update e2e and storybook tests that assert on the relative DOM order of `.ctsNavRow` versus the sticky bar.
- Update JSDoc in `cts-log-detail-header.js` (the layout sketch around line 80 / 1583) so the documented zone order matches the new template.
- In-situ verification of Continue Plan on a real multi-module plan via Playwright MCP per `[[feedback_agent_browser_cts_cert_bypass]]`.
- If verification fails: introduce the SUS-style placeholder (`cts-tooltip` + `cts-modal`) on the Continue button inside `cts-test-nav-controls.js`, gated behind a feature flag that defaults off (so the placeholder ships behind the existing wiring; future maintainers can flip the flag if/when they confirm a real backend gap).

### Out of scope

- Re-architecting `cts-log-detail-header` into smaller subcomponents (kept atomic per `[[project_phase_d_complete]]`).
- Moving the nav row OUT of `cts-log-detail-header` into the page-level layout next to `cts-crumb` (Option B in research notes; rejected — see Key Technical Decisions).
- Changes to the breadcrumb component (`cts-crumb`) itself.
- Adding a *new* "plan progress" surface anywhere else on the page (the nav row is the canonical home; U5 of MR 1998 plan already locked the label copy).
- Per-module status grid (E1 in MR 1998 plan, gated on G1).
- Continue Plan affordance changes besides verification or the conditional stub modal — the button's label, icon, and event payload stay identical.

### Deferred to Follow-Up Work

- Sticky-stack treatment where both the nav row and the sticky bar pin during scroll (would require a two-line sticky header and a CSS-only stack offset). Not requested; not needed for IA disambiguation.
- An audit of every page that mounts `cts-test-nav-controls` to confirm the IA reorder doesn't surface a different ordering there. Per `grep -rn cts-test-nav-controls src/main/resources/static/*.html`, log-detail.html is the only live consumer; Storybook is the only other surface.

---

## Key Technical Decisions

### KTD1. Reorder in-component, not page-level extraction

The nav row could have been pulled OUT of `cts-log-detail-header` into `log-detail.html` as a sibling of `<cts-crumb>` (the most IA-honest reading: "plan-level context belongs at the page level, leaf-level details belong in the test header"). Rejected because:

- `cts-log-detail-header` already imports `cts-test-nav-controls` and renders it via `_renderTestNavControlsRow(test)`, threading `test.testId` / `test.planId` / `readonly` / `publicView` from `this.testInfo`. Page-level extraction would require duplicating that state plumbing in `log-detail.js`, adding a new prop on the page, and re-wiring the `cts-continue` event listener.
- The page-level `cts-test-summary` and `cts-failure-summary` hoisting precedents (`log-detail.html:225-238`) exist because those components need to span the breakpoint boundary — narrow-width vs desktop placement is different. The nav row has no such boundary requirement; its placement is invariant across widths.
- In-component reorder is a single render-template change. It satisfies the user's "closer to breadcrumb" reading because the nav row will sit immediately under the breadcrumb visually (the sticky bar moves below it, banner/hero/drawer below that).

### KTD2. Sticky bar stays sticky; nav row does not

The sticky bar's role is "always-visible action and verdict affordance while the user is scrolled into the log entries". The nav row's role is "page-top orientation that tells you which test in the plan you're looking at". Once the user has scrolled into the entries stream, the orientation answer has been absorbed — the plan-progress label does not need to follow the viewport. Keeping the nav row non-sticky preserves the current sticky-bar height budget (one row) and avoids the visual complexity of a two-line sticky stack. The sticky bar's `position: sticky; top: 0` selector is unchanged.

### KTD3. Conditional Continue Plan stub — only if verification fails

The user's instruction explicitly conditions the stub modal on "if it requires a backend change". U4 (commit `0bd815594`) wired Continue Plan to existing endpoints and reported in-situ success on an OIDCC plan. The plan therefore treats Continue Plan as already-wired and uses U2 to **verify** rather than to **rewire**. The stub modal pattern is documented as a fallback if the in-situ verification surfaces a failure case U4 didn't catch — for example, a plan whose next module needs configuration the runner endpoint doesn't accept via the cached payload shape. Building the stub speculatively before that signal would add dead code and weaken the existing wiring.

### KTD4. Stub pattern, if needed: `cts-tooltip` + `cts-modal` per `c5bf0bf74`

The cts-config-form Validate Configuration precedent is the established stub pattern: wrap the button in a `cts-tooltip` carrying a brief "🚧 not yet implemented" caption, route the click to a `cts-modal` that explains what the feature is supposed to do, why it matters, and where to find the spec or tracking ticket. The same shape (tooltip caption + modal body with what / why / where) maps cleanly onto Continue Plan if the verification path lands on "the backend doesn't accept what the frontend is sending and the gap is real". The modal would point at a tracker entry — likely a follow-up note on MR 1998 — so maintainers can pick it up.

---

## High-Level Technical Design

Directional sketch — not implementation specification. The implementing agent should treat it as scaffolding for review, not template code.

```
Before (current order inside cts-log-detail-header._renderContent):

  <cts-crumb>                              ← page-level (in log-detail.html)
  <cts-log-detail-header>
    Region A:  sticky status bar           ← position: sticky; top: 0
    Nav row:   .ctsNavRow                  ← plan progress + Continue
    Banner:    terminal-state               (optional, per phase)
    Region B:  hero                        ← lifecycle-driven
    Region C:  drawer                      ← test details + config


After (this plan):

  <cts-crumb>                              ← unchanged
  <cts-log-detail-header>
    Nav row:   .ctsNavRow                  ← MOVED — plan progress + Continue
    Region A:  sticky status bar           ← STILL position: sticky; top: 0
    Banner:    terminal-state               (optional, per phase)
    Region B:  hero
    Region C:  drawer

Reading the page top-to-bottom matches the IA hierarchy:
  plan-link (breadcrumb) → plan progress (nav row) → this test's verdict
  (sticky bar) → this test's banner / hero / drawer.

Sticky behaviour: the nav row scrolls off the top of the viewport when
the user scrolls into the entries. The sticky bar pins at top:0 once
the page has scrolled enough that the bar's natural top would have
exited the viewport. No change to the sticky-bar selector or its
overflow-audit (the comment block in log-detail.html:5-17 still
applies — none of the bar's ancestors carry overflow/transform that
would break sticky).
```

---

## Implementation Units

### U1. Reorder nav row above sticky bar inside `cts-log-detail-header`

**Goal:** Render the `.ctsNavRow` before the sticky status bar inside `cts-log-detail-header._renderContent`, with scoped CSS / borders / spacing adjusted to read as "nav row sits above the sticky bar".

**Requirements:** User request part 1 — bring the progress nav bar closer to the breadcrumbs.

**Dependencies:** None.

**Files:**

- `src/main/resources/static/components/cts-log-detail-header.js` — `_renderContent` render template (around line 1583); the `.ctsNavRow` CSS block (~lines 309-351); JSDoc / sketch comments referencing zone order (~line 80, ~line 650).
- `src/main/resources/static/components/cts-log-detail-header.stories.js` — any story that asserts on `.ctsNavRow` position via `nth-child`, DOM-walk, or screenshot regression.
- `frontend/e2e/log-detail.spec.js` — any spec that asserts on the relative order of `[data-testid="nav-row"]` and the sticky bar.

**Approach:** Swap the `${this._renderTestNavControlsRow(this.testInfo)}` and `${this._renderStickyBar(this.testInfo)}` invocations in `_renderContent` so the nav row precedes the sticky bar. Audit the scoped CSS — the current `.ctsNavRow` block sets `padding: var(--space-4) 0; border-bottom: 1px solid var(--border);` and the override comment at line 313-321 ("Hide the nav row when... an empty divider directly under the sticky status bar") needs the comment updated to reflect the new adjacency (nav row sits ABOVE the bar, so the worry shifts to whether the bar's own top border still reads, or whether the row's `border-bottom` becomes redundant against the bar's own `border-top`). Keep the `:has(cts-test-nav-controls:empty) { display: none }` rule intact — the row should still vanish when empty.

The sticky bar itself does NOT need an order or z-index adjustment — `position: sticky` does not depend on DOM order to pin, only on the scrollable ancestor. Verify on a live page that the sticky bar still pins as the user scrolls into the entries.

**Patterns to follow:**

- `cts-log-detail-header.js`'s existing scoped-CSS block pattern (single `STYLE_TEXT` constant, `ensureStylesInjected()` on first render).
- The four-zone sketch comment around line 1255 ("nav row sits directly under the sticky bar") — update its prose to reflect the swap; this comment is the canonical in-source documentation of zone order.
- AGENTS.md §6 (Storybook play tests cover state branches) — every story that demonstrates a particular phase / variant of the header needs its DOM-order assertion (if any) refreshed.

**Test scenarios:**

- Play test on the existing `WithTestNavControls` story (or its closest equivalent): assert that the element with `[data-testid="nav-row"]` appears in the DOM *before* the sticky bar (`[data-testid="status-bar-primary"]` or the bar's wrapping div) — adjust whichever direction is currently asserted.
- Play test: when `cts-test-nav-controls` renders nothing (totalCount=0, slim mode), `.ctsNavRow` still has `display: none` (existing `:has()` rule continues to apply with the new ordering).
- Play test: the sticky bar's `position: sticky` styles are intact (assert computed `position: sticky` and `top: 0`) after the reorder.
- e2e (`frontend/e2e/log-detail.spec.js`): on a fixture with a 5-module plan, the `.ctsNavRow` is rendered immediately after the `cts-crumb` and immediately before the sticky bar.
- e2e: scrolling the page (`window.scrollTo`) confirms the nav row scrolls off-screen while the sticky bar pins at top.

**Verification:** Storybook play tests pass; e2e regression on log-detail does not surface a new failure beyond `[[feedback_e2e_pre_existing_failures_2026_05_20]]`. Visual smoke on `https://localhost.emobix.co.uk:8443/log-detail.html?log=<known-id>`: breadcrumb is immediately followed by "Plan progress: Module N of M" / Continue Plan; the sticky bar's verdict + Repeat Test cluster sits below the nav row at the page top, then pins on scroll.

---

### U2. Verify Continue Plan in-situ; fall back to stub modal only on confirmed failure

**Goal:** Confirm the Continue Plan button drives the runner forward end-to-end on a real multi-module plan. If verification succeeds, ship a Storybook + e2e regression that captures the wiring. If verification surfaces a path that requires backend work, introduce a SUS-style `cts-tooltip` + `cts-modal` stub on the Continue button.

**Requirements:** User request part 2 — make Continue Plan work, with a stub-modal fallback if backend wiring is needed.

**Dependencies:** None. (U1 does not block U2; the IA reorder doesn't change the Continue button's wiring.)

**Execution note:** Investigation-first. Drive the live page via Playwright MCP, click Continue Plan on a plan where `nextEnabled=true`, observe `POST /api/runner` response, and check whether navigation to the next module's `log-detail.html` page succeeds. Continue-Plan stories already exist (`ContinueFiresEvent`); the gap is real-API verification.

**Files (always touched):**

- `frontend/e2e/log-detail.spec.js` — new spec asserting Continue Plan POSTs to `/api/runner` with the expected `test=<nextTestName>&plan=<planId>` shape, expecting HTTP 201 (use the existing mock-fetch infrastructure if real API call is unstable in CI).
- `src/main/resources/static/components/cts-test-nav-controls.stories.js` — extend the existing `ContinueFiresEvent` story (or add a new story) that asserts the event detail carries the expected `{ testId, planId }` payload after the U1 reorder (defensive — verify the IA reorder doesn't break the event path through Lit's render tree).

**Files (only if verification surfaces a real failure):**

- `src/main/resources/static/components/cts-test-nav-controls.js` — wrap the Continue button in `cts-tooltip` (caption: `"Continue Plan is awaiting backend wiring 🚧"`) and route the click to a new `cts-modal` (`id="continuePlanStubModal"`) that explains the gap and links to the follow-up tracker entry. Mirror the structure used in `cts-config-form.js:90-100` and the scoped CSS used in `c5bf0bf74` for `.oidf-config-form-sus-notice`.
- `src/main/resources/static/log-detail.html` — mount the new `<cts-modal id="continuePlanStubModal">` (if the modal is page-level rather than component-scoped).
- `docs/plans/2026-05-22-002-fix-mr1998-maintainer-feedback-tracker.md` — open a new row under the tracker for the residual Continue Plan backend gap, marked `open` with a pointer to this plan's U2.

**Approach:**

1. Bring up the dev environment (`devenv up` already running per session) and identify a multi-module plan with `nextEnabled=true` for the current test (Aim for an OIDCC plan with ≥3 modules where the current module is in the middle).
2. Open `log-detail.html?log=<currentTestId>` via Playwright MCP. Confirm the nav row renders the Continue Plan button (`[data-testid="continue-btn"]`).
3. Click the button via `mcp__plugin_playwright_playwright__browser_click`. Capture the `POST /api/runner` request (`mcp__plugin_playwright_playwright__browser_network_request`) and the response.
4. If response is 201 with `{ id: "..." }` and the page navigates to the new test's log-detail URL → Continue Plan works. Land the e2e regression test asserting the request/response shape, and leave the wiring intact.
5. If the response is non-200 OR the navigation does not happen OR the next-module resolution surfaces an unhandled case → introduce the stub modal. The modal copy explains: "Continue Plan is awaiting backend wiring. The runner accepts module-name POSTs to `/api/runner`, but resolving 'next module' for plans whose variants don't subset-match the cached `/api/plan` modules list requires a backend change. Tracked at <follow-up note>." Wrap the button in `cts-tooltip` with the construction-emoji caption.

**Patterns to follow:**

- `cts-config-form.js:90-100` and commit `c5bf0bf74` for the cts-tooltip + cts-modal stub shape (scoped `.oidf-config-form-sus-notice` CSS block, modal body with `h4` + `p` + `blockquote` + `code` typography).
- `log-detail.js:807-845` — the canonical Continue Plan handler. Reading this is the cheapest way to spot the failure-mode the in-situ test should provoke.
- `frontend/e2e/log-detail.spec.js` existing fixtures (`fixtures/mock-runner.js` if it exists, or `fixtures/mock-plans.js`) for mocking the runner POST.

**Test scenarios (always shipped):**

- e2e: Click Continue Plan on a fixture with a 3-module plan, currently on module 1. Assert `POST /api/runner` is fired with `test=<module2.testModule>&plan=<planId>&variant=<json>`. Assert the spec stops short of navigation (mock-driven; real navigation is the in-situ smoke step, not the e2e regression).
- e2e: Click Continue Plan when `nextEnabled` is false. Assert the button is not in the DOM (existing behaviour; defensive regression).
- e2e: Click Continue Plan when the runner returns 400 with `{ error: "..." }`. Assert the toast surfaces the server error and the button is re-enabled.
- Storybook play test: `ContinueFiresEvent` continues to pass after U1's reorder (defensive — Lit render-tree reorder doesn't affect event bubbling, but verify).

**Test scenarios (only if the stub modal lands):**

- Play test: clicking Continue Plan opens `#continuePlanStubModal` (or its component-scoped equivalent), not a runner POST.
- Play test: the modal's heading reads "Continue Plan — coming soon" (or whatever copy the implementer chooses); the body contains the rationale and tracker link.
- Play test: a tooltip is wired to the button host (`cts-tooltip` carries the construction caption); hovering or focusing the button fires the tooltip open.
- Regression: the `cts-continue` event is NOT dispatched while the stub is active (otherwise the page-level handler would race with the modal).

**Verification:**

- If Continue Plan works in-situ: Playwright MCP screenshot at `tmp/screenshots/u2-continue-plan-success-in-situ.png` showing the new log-detail page for the next module after the click. e2e spec passes locally.
- If the stub modal lands: Playwright MCP screenshot at `tmp/screenshots/u2-continue-plan-stub-modal-in-situ.png` showing the modal opened over the log-detail header. The tracker entry is staged alongside the code so the maintainer-facing reply text is ready.

---

## Risks

### R1. Sticky bar's vertical position changes visually after the reorder

Moving the nav row above the sticky bar means the bar's natural-flow position shifts down by the height of the nav row (~`var(--space-4)` of padding + the row's content height). When the bar's natural-flow position is below the viewport top, the bar starts sticking. Functionally identical to today; visually the user sees the breadcrumb + nav row before the bar pins. Acceptable; this is exactly the IA effect the user asked for.

**Mitigation:** U1's e2e step explicitly scrolls past the nav row's natural position and asserts the bar pins. Existing `scroll-margin-top` on log entries (set in MR 1998 / R32) accounts for the bar's height, not its position; no entry-anchor regression expected.

### R2. The `.ctsNavRow:has(cts-test-nav-controls:empty)` rule depends on the row not being a flex item of an unexpected parent

Currently `.ctsNavRow` sits inside `cts-log-detail-header` and the `:has()` selector relies on `cts-test-nav-controls` being a direct child. Moving the row above the sticky bar keeps that parentage intact (both are children of the same wrapper inside the host), so the rule continues to apply. Verify in U1's play test.

**Mitigation:** Play test asserts `display: none` on the nav row when `cts-test-nav-controls` is empty after the reorder.

### R3. Continue Plan verification surfaces an intermittent failure

The runner endpoint may behave differently across plan families (OIDCC, FAPI, VCI, VP). U4's verification was on an OIDCC plan; an FAPI plan with mTLS variants might surface a different failure. The plan handles this with the conditional stub fork — but if the failure is intermittent (works on the first try, fails on the second), the conditional logic gets messy.

**Mitigation:** If U2 verification finds inconsistent results, treat as a real failure and land the stub modal. Document the inconsistent cases in the tracker entry so a future plan can pick up the backend-side root cause.

### R4. Pre-existing log-detail.spec.js failures get blamed on this plan

Per `[[feedback_e2e_pre_existing_failures_2026_05_20]]` the log-detail e2e has 6+ baseline failures. New tests added in U1/U2 may land on the wrong side of an unrelated flake.

**Mitigation:** Before adding new e2e assertions, stash this plan's changes and confirm the baseline failure count. New tests must pass on a clean checkout; existing baseline failures are documented and not in scope.

---

## Verification Strategy

- **Per-unit:** Storybook play tests + e2e where the unit touches DOM order or event wiring.
- **Per-plan:** Manual smoke on `https://localhost.emobix.co.uk:8443/log-detail.html?log=<known-id>` via Playwright MCP per `[[feedback_agent_browser_cts_cert_bypass]]`. Capture screenshots to `tmp/screenshots/u1-*.png` and `tmp/screenshots/u2-*.png`.
- **Plan-wide:** `npm run test:ci` from `frontend/` passes (format → lint → type-check → lint:jsdoc → lint:icons → lint:lit-analyzer → codegen:check); `mvn -B -Dmaven.test.skip clean package` succeeds.
- **Baseline regression:** Pre-existing e2e + Storybook flake count does not increase beyond the 2026-05-20 baseline.

---

## System-Wide Impact

- **Page-level mounters of `cts-test-nav-controls`:** Only `log-detail.html` mounts the widget today (via `cts-log-detail-header._renderTestNavControlsRow`). Storybook is the other consumer and is unaffected by the IA reorder because Storybook stories don't mount inside `cts-log-detail-header`.
- **Sticky-bar dependencies:** `log-detail.html:5-17` documents the sticky-bar overflow audit. None of the ancestors gain or lose overflow/transform from this change. `frontend/e2e/log-detail.spec.js`'s sticky-bar specs verify pinning behaviour; updates required only where they assert sibling order.
- **Continue Plan downstream consumers:** `log-detail.js:1002`'s `cts-continue` listener is the only consumer. Storybook's `ContinueFiresEvent` test verifies the event dispatch. No other component listens.
- **MR 1998 tracker:** if the stub modal lands, the tracker gets a new row pointing at the residual backend gap; if Continue Plan works, the tracker's A3 row (already closed) carries U4 as the closing reference and no new entry is needed.

---

## Execution Posture

Default per-unit posture: implement, add Storybook + e2e coverage, verify in-situ on the live page. U2 is investigation-first (Playwright MCP probe before any code change); U1 is implementation-first because the IA reorder is a contained edit with a clear before/after.

---

## Origin Document Trace

No upstream brainstorm doc — this plan is sourced directly from the user's `/lfg` invocation message on 2026-05-22:

> on this branch, bring this progress nav bar closer to the breadcrumbs, as this makes more sense from an information architecture hierarchical perspective.
>
> And make the "Continue plan" button work. If it requires a backend change, let's not wire it up just yet and have a stub modal like in some other spots where a modal says the feature isn't implemented yet.

Cross-references:

- `docs/plans/2026-05-22-002-fix-mr1998-maintainer-feedback-plan.md` — U4 (commit `0bd815594`) is the canonical Continue Plan wiring; U5 (commit `5db5f2acb`) added the "Plan progress:" eyebrow on the nav row.
- `src/main/resources/static/components/cts-config-form.js:83-98` + commit `c5bf0bf74` — the SUS-style stub pattern referenced as the fallback for U2.
