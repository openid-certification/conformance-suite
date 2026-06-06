---
title: "fix: Stack the plan-header metadata list on mobile"
type: fix
status: completed
date: 2026-06-05
---

# fix: Stack the plan-header metadata list on mobile

## Summary

On phone-width viewports, the plan-detail page's header metadata list (`.planMeta` in `cts-plan-header`) keeps a two-column label/value grid whose `max-content` label track eats 164px of a ~312px content box, squeezing every value (variant string, plan ID, version chips) into a ~132px column. Fix it with the codebase's established stack-up container-query pattern, mirroring the fix that shipped for `cts-log-detail-header.js` in `docs/plans/2026-06-05-002-fix-log-detail-mobile-responsive-plan.md` (commits `2b39190e6`, `32884a48f`, `d00037aef`): stacked single column by default, two-column layout restored at ≥640px container width.

## Problem Frame

Reproduced and measured at 360×844 / 375×800 / 1280×800 against `plan-detail.html` with long values (66-char plan name, five-entry variant map, two-entry certification profile array) via a throwaway Playwright spec using the existing mocked routes:

- **`.planMeta` never stacks.** `cts-plan-header .planMeta` (`src/main/resources/static/components/cts-plan-header.js`) is a `<dl>` grid with `grid-template-columns: max-content 1fr` and no responsive rule. The longest label — "CERTIFICATION PROFILE:" rendered uppercase fs-12 bold with 0.06em letter-spacing — sizes the `max-content` track to **164.14px at every viewport**. At 360px the page padding leaves a 312px content box, so values get ~132px; at 375px, ~147px. The comma-joined variant mono chip wraps heavily in that sliver.
- **No document-level horizontal overflow exists** — `document.documentElement.scrollWidth` = `clientWidth` at 360px and 375px even with long values (the host's flex column and `dd { word-break: break-word }` keep content breaking). Unlike log-detail, there is **no second root cause** here: no status-bar analogue, nothing inflating the page. The fix is confined to the `.planMeta` grid. The e2e overflow guard is added as a regression lock, not a bug fix.
- **Desktop is healthy.** At 1280px the label track is the same content-hugging 164.14px with 792px for values. The wide layout needs no visual change — only re-expression through the container-query branch.

Diagnosis note carried from the task brief: this track is `max-content`, NOT log-detail's always-maximizing `minmax(120px, 180px)` bug. The defect is purely the absence of a stacking rule, not track maximization.

## Requirements

**Readability**

- R1. At phone widths (≤375px viewport), every `.planMeta` value renders with the full content width available to the header — label above value, no two-column squeeze.
- R2. Within a stacked pair the value hugs its label (tight intra-pair gap); successive pairs are visibly separated so each label+value group reads as one block.

**Regression safety**

- R3. At ≥640px container width the two-column label/value layout is preserved, with the label track hugging its content (measured ~164px) and never maximizing toward a fixed cap — locked by a strict upper-bound assertion.
- R4. `plan-detail.html` has no document-level horizontal overflow at 375px with long values (`document.documentElement.scrollWidth <= clientWidth`) — currently true; lock it so a future header change cannot silently regress it.
- R5. Existing Storybook play tests and Playwright e2e suites stay green; new coverage locks the stacked and wide layouts.

## Key Technical Decisions

- **Stack-up container query, not a media query.** Default `.planMeta` to a stacked single column; restore the two-column grid only inside `@container ctsPlanHeader (min-width: 640px)`. 640px is the established "phone vs not" line (`cts-log-entry`/`ctsLogViewer` 640px, `cts-log-detail-header`/`ctsLogDrawer` 640px; `cts-running-test-card` 560px). A container query keys on the header's actual available width — correct under the `.oidf-plan-detail-grid` two-column desktop layout (where the header column can be narrow while the viewport is wide) and in Storybook isolation. Sibling-on-same-page precedent: `cts-plan-modules` already declares `planModulesCard / inline-size` for exactly this reason.
- **The host carries the container.** Unlike `cts-log-detail-header` (whose host is `display: contents` and cannot be a container), `cts-plan-header`'s host rule is `display: flex; flex-direction: column` — a real box. Declare `container: ctsPlanHeader / inline-size` on the existing host rule rather than introducing a wrapper. Name `ctsPlanHeader` collides with none of the registered names (`ctsLogViewer`, `ctsLogDrawer`, `planModulesCard`, `ctsRunningTestCard`). Inline-size containment makes the host the containing block for absolutely-positioned descendants — `cts-plan-header` renders none, so this is safe (note it in the CSS comment per house style).
- **`fit-content(180px)` for the wide-layout label track, `minmax(0, 1fr)` for the value track.** Labels are template-fixed strings (today's longest measures ~164px), so `max-content` is currently bounded — but `fit-content(180px)` future-proofs against longer labels and keeps the two sibling header components on one idiom. `minmax(0, 1fr)` (never bare `1fr`) drops the value track's implicit `min-width: auto` so long unbreakable values wrap — the documented house idiom (`cts-log-entry.js` R31 comment; `docs/solutions/web-components/subgrid-alignment-inside-details-blocks-2026-05-28.md`).
- **Strict desktop bound `< 176`, recalibrated from measurement — do not copy log-detail's `< 160`.** The d00037aef lesson is that the bound must sit strictly between the measured healthy value and the failure-mode value. Here the longest label genuinely measures 164.14px (vs log-detail's ~84px), so `< 160` would fail *with* the fix. The failure mode to exclude is a track maximizing to a 180px cap (e.g., a future revert to fixed-max `minmax()`): 164.14 < 176 < 180 gives ~12px rendering headroom while still failing on a maximizing regression. If the Storybook environment measures differently, re-pick the bound strictly between the measured width and 180.
- **Stacked-layout spacing mirrors the sibling:** `gap: var(--space-1)` as the stacked grid's gap (value hugs label) plus `margin-top: var(--space-3)` on `dt:not(:first-child)` to separate pairs; the wide branch restores the current `column-gap: var(--space-4) / row-gap: var(--space-2)` and zeroes the dt margin.
- **No page-level change.** Measurement showed no doc-level overflow at phone widths, so the second-root-cause hunt mandated by the brief came back clean — the fix stays inside `cts-plan-header.js`'s `STYLE_TEXT`.

## Assumptions

- The stacked default uses `grid-template-columns: 1fr` (mirroring `2b39190e6`'s stacked branch); the `dd` wrapping rule (today `word-break: break-word`, switched to `overflow-wrap: anywhere` by U1 for sibling parity) guarantees values wrap, so the single-column track needs no `minmax(0, 1fr)`.
- The e2e regression test belongs in the existing `frontend/e2e/plan-detail.spec.js` describe block and reuses its route conventions, stubbing the module-instance `/api/info/:testId` calls via `setupTestInfoRoute` like the first test does (note: `setupTestInfoRoute` registers a catch-all that serves a default for unmapped ids, so explicit per-instance entries are a determinism choice rather than a hard `expectNoUnmockedCalls` requirement).
- A shared long-values fixture (`MOCK_PLAN_DETAIL_LONG_VARIANT`) goes in `frontend/e2e/fixtures/mock-test-data.js`, mirroring `MOCK_TEST_STATUS_LONG_VARIANT` from `32884a48f`; the Storybook stories use a story-local fixture instead (stories don't import e2e fixtures — the existing stories define `PLAN` locally).
- No JSDoc `@property` changes are needed (no API change); the component's heavy CSS-comment house style is maintained on every touched rule.

## Implementation Units

### U1. Stack `.planMeta` at narrow container widths

**Goal:** Make every plan-header metadata value readable at phone widths by giving values the full header width, while re-expressing the healthy desktop layout through a container-query branch.

**Requirements:** R1, R2, R3

**Dependencies:** none

**Files:**
- `src/main/resources/static/components/cts-plan-header.js` (STYLE_TEXT: host rule, `.planMeta`, `.planMeta dt`)

**Approach:** Add `container: ctsPlanHeader / inline-size` to the existing `cts-plan-header` host rule. Make the stacked layout the default: `.planMeta { grid-template-columns: 1fr; gap: var(--space-1) }` with `margin-top: var(--space-3)` on `dt:not(:first-child)` for pair separation. **Preserve the existing outer `.planMeta { margin-top: var(--space-3) }` unchanged in both layouts** — it spaces the whole `<dl>` from the title/lede above and is distinct from the new `dt` pair-separation margin that happens to use the same token; add a comment distinguishing the two so the coincidence doesn't read as duplication. Move `align-items: baseline` into the wide branch (it only does work in the two-column layout; a one-line comment notes it baseline-aligns each label with its value text). While in the file, switch `dd { word-break: break-word }` to `overflow-wrap: anywhere` — the sibling `cts-log-detail-header .logMetaValue` standardized on `overflow-wrap: anywhere` for the same data classes (IDs, variant strings, mono chips), and the two metadata surfaces should share one wrapping idiom; comment the parity. Inside `@container ctsPlanHeader (min-width: 640px)`, restore the two-column layout as `fit-content(180px) minmax(0, 1fr)` with the current gaps and zero the dt margin. Match the file's comment-heavy house style: explain the mobile-first default, the 640px line, the fit-content vs minmax distinction, and the containment/abs-positioning note, in the register of `2b39190e6`'s comments.

**Technical design (directional, not literal):**

```css
/* default = stacked (mobile-first) */
cts-plan-header { container: ctsPlanHeader / inline-size; }
cts-plan-header .planMeta {
  grid-template-columns: 1fr;
  gap: var(--space-1);
  /* existing margin-top: var(--space-3) stays — outer spacing from the
     title/lede, distinct from the dt pair-separation margin below */
}
cts-plan-header .planMeta dd { overflow-wrap: anywhere; /* parity with .logMetaValue */ }
cts-plan-header .planMeta dt:not(:first-child) { margin-top: var(--space-3); }
@container ctsPlanHeader (min-width: 640px) {
  cts-plan-header .planMeta {
    grid-template-columns: fit-content(180px) minmax(0, 1fr);
    gap: var(--space-2) var(--space-4);
    align-items: baseline; /* moved here — only meaningful with two columns */
  }
  cts-plan-header .planMeta dt:not(:first-child) { margin-top: 0; }
}
```

**Patterns to follow:** `cts-log-detail-header.js` `.logMetaTable` block from commit `2b39190e6` (including comment style); `cts-plan-modules.js` container declaration rationale comment; `cts-log-entry.js` R31 `minmax(0, 1fr)` idiom.

**Test scenarios:** owned by U2 (single test-bearing unit so story/e2e assertions land once, after the CSS change — same split as the reference commits).

**Verification:** At 360px the variant/ID/version values render full-width under their labels (compare against the measured 132px baseline); at ≥688px viewports (640px container + page padding) the two-column layout is visually unchanged from current HEAD except for spacing within the tolerance of the same gap tokens.

### U2. Lock both layouts in Storybook and e2e

**Goal:** Regression-proof the stacked and wide layouts at component and page scope.

**Requirements:** R1, R2, R3, R4, R5

**Dependencies:** U1

**Files:**
- `src/main/resources/static/components/cts-plan-header.stories.js`
- `frontend/e2e/plan-detail.spec.js`
- `frontend/e2e/fixtures/mock-test-data.js` (new `MOCK_PLAN_DETAIL_LONG_VARIANT` fixture)

**Approach:** Component scope: add a story-local long-values fixture (66-char `planName`, five-entry `variant`, two-entry `certificationProfileName` array — the certification label is the longest and must be present or the desktop bound measures the wrong track content; the shared `PLAN` const omits it). Add `MetaStacksOnMobile` pinned to `mobile1` (320×568; `parameters: { viewport: { defaultViewport: "mobile1" } }` + `globals: { viewport: { value: "mobile1", isRotated: false } }`, presets from `MINIMAL_VIEWPORTS` registered in `frontend/.storybook/preview.js`) asserting `getComputedStyle(planMeta).gridTemplateColumns` resolves to a single track. Add a **new** desktop-pinned story `MetaTwoColumnOnDesktop` (do not extend or pin any existing story — they must keep passing untouched at the default canvas width) asserting two tracks with `parseFloat(tracks[0]) < 176` — strict bound per the Key Technical Decision above, with a comment explaining why the bound is 176 here and not log-detail's 160. Page scope: add one e2e test at 375×800 (viewport set BEFORE `goto`; route order `setupFailFast` → plan route with the new fixture + `setupTestInfoRoute` stubs for `test-inst-001/002/003` → `setupCommonRoutes`) asserting (a) `document.documentElement.scrollWidth <= clientWidth` and (b) `.planMeta` computes to a single track — mirroring `"page does not overflow and Test details stack at 375px viewport"` in `frontend/e2e/log-detail.spec.js` including its doc-element-not-sub-container comment. Measurement rule throughout: computed-style / `scrollWidth` assertions only; no `getBoundingClientRect()` inside `display: contents` subtrees.

**Patterns to follow:** Commit `32884a48f` (story shape, fixture JSDoc register, e2e comment style); `d00037aef` (strict-bound comment); existing `plan-detail.spec.js` first test (route choreography for the module-instance info calls).

**Test scenarios:**
- Covers R1. Storybook `MetaStacksOnMobile` (mobile1, long fixture): `gridTemplateColumns` of `.planMeta` resolves to exactly one track.
- Covers R2. Same story: the second `dt`'s computed `margin-top` is the pair-separation token (12px) — locks the stacked rhythm cheaply via computed style.
- Covers R3. Storybook `MetaTwoColumnOnDesktop` (desktop preset, long fixture with certification profile): two tracks; `parseFloat(tracks[0]) < 176` (strict — a maximizing-minmax regression resolving to 180 must fail; current healthy measurement is 164.14).
- Covers R4. e2e at 375×800 with `MOCK_PLAN_DETAIL_LONG_VARIANT`: document-level `scrollWidth <= clientWidth`.
- Covers R1. Same e2e test: `.planMeta` computed `grid-template-columns` is a single track.
- Covers R5. Regression: the full Storybook suite (`npx vitest --project=storybook --run`) and the full `plan-detail.spec.js` e2e spec stay green alongside the new tests — the existing stories and e2e tests exercise the header at default canvas/viewport widths where the wide branch must reproduce today's rendering.
- Edge case: existing stories (`Default`, `AdminShowsOwner`, `MissingStarted`, `SummaryRendersMarkdown`) keep passing untouched at the default canvas width — they assert content, not layout, and must not pick up viewport pins.
- Edge case: plan without `certificationProfileName`/`owner` (the shared `PLAN` fixture) still renders the stacked grid without empty rows — covered by existing conditional-render logic and the untouched `Default` story staying green.

**Verification:** `cd frontend && npx vitest --project=storybook --run` fully green; `cd frontend && ./node_modules/.bin/playwright test e2e/plan-detail.spec.js` green; `npm run test:ci` from `frontend/` green (pre-existing-failure caveat: verify any unexpected failure exists on clean HEAD before attributing); scoped files formatted via `npx prettier --config ./.prettierrc.json --write <files>` from `frontend/`.

## Scope Boundaries

**In scope:** the `.planMeta` container-query fix in `cts-plan-header.js` plus its Storybook/e2e coverage and the shared e2e fixture.

**Out of scope (true non-goals):**
- Other plan-detail zones: `cts-plan-modules` already has its own container query (`planModulesCard`), the actions rail stacks at the page's 900px media query, and no overflow was measured page-wide.
- A shared breakpoint/container token in `oidf-tokens.css` — convention change beyond this fix (already noted as deferred in the log-detail plan).
- `cts-log-detail-header.js` — shipped separately; not touched here.

### Deferred to Follow-Up Work

- None identified — this plan *is* the deferred follow-up named by `docs/plans/2026-06-05-002-fix-log-detail-mobile-responsive-plan.md`.

## Workflow Notes

- Work directly on `feat/redesign` (ships via GitLab MR !1998); no new branch, no GitHub PR.
- Two atomic commits mirroring the reference shape: U1 (CSS fix), then U2 (tests). Stage with explicit pathspecs only — concurrent agents may have uncommitted work in the tree. This plan file itself requires force-adding (a `.git/info/exclude` rule blocks new `docs/` files).
- CI baseline caveats: `vc_test` is red on base AND head (pre-existing Authlete drift); `frontend_lint` is `allow_failure` until 2026-06-12 — compare against the base pipeline before attributing failures.

## Sources & Research

- Repro measurements (throwaway Playwright spec, mocked routes, long-values fixture, 360/375/1280px, deleted after): `.planMeta` computes `164.141px 131.859px` at 360px, `164.141px 146.859px` at 375px, `164.141px 791.859px` at 1280px; `docScrollWidth == clientWidth` at all three (no page-level overflow); host `display: flex`.
- Reference commits: `2b39190e6` (CSS fix pattern + comment style), `32884a48f` (story/e2e coverage shape), `d00037aef` (strict-bound rationale).
- Origin plan: `docs/plans/2026-06-05-002-fix-log-detail-mobile-responsive-plan.md` ("Deferred to Follow-Up Work" names this task; its `minmax(120px, 180px)` diagnosis does NOT transfer — corrected in the brief and confirmed by measurement).
- Container-name census: `ctsLogViewer` (`cts-log-viewer.js`), `ctsLogDrawer` (`cts-log-detail-header.js`), `planModulesCard` (`cts-plan-modules.js`), `ctsRunningTestCard` (`cts-running-test-card.js`) — `ctsPlanHeader` is free.
- Institutional learnings (`docs/solutions/`): `web-components/subgrid-alignment-inside-details-blocks-2026-05-28.md` (`minmax(0, 1fr)` is load-bearing; computed-style/scrollWidth measurement, never `getBoundingClientRect()` in `display: contents` subtrees — durable parts of a partially superseded doc); `web-components/cts-navbar-inline-visibility-bug-2026-04-24.md` (host display type matters — verified `cts-plan-header` host is `display: flex`, a real box that can carry the container); `web-components/cts-button-host-vs-inner-button-semantics-2026-04-17.md` (light-DOM render root: assert on the inner `.planMeta`, not the host).
- Page context: `src/main/resources/static/plan-detail.html` `.oidf-plan-detail-grid` (`minmax(0, 1fr) 240px`, stacking at ≤900px) — confirms the header column's width diverges from the viewport on desktop, justifying a container query over a media query.
- Repo-research note: a dedicated repo-research agent pass was skipped — the task brief was authored from the just-completed sibling fix in this same area, and its claims were verified directly against the reference commits, target files, and live measurement rather than re-derived.
