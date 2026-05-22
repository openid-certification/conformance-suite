# Residual Review Findings — log-detail layout reflow fix

**Source PR run:** `ce-code-review mode:autofix`, run-id `20260521-203737-95558891`
**Branch:** `feat/redesign`
**Tip after autofix + commit:** `a6064fe79`
**Plan:** [docs/plans/2026-05-21-002-fix-log-detail-layout-reflows-plan.md](../plans/2026-05-21-002-fix-log-detail-layout-reflows-plan.md)
**MR:** https://gitlab.com/openid/conformance-suite/-/merge_requests/1998
**Reviewers (7/7 returned):** correctness, testing, maintainability, project-standards, agent-native, learnings-researcher, julik-frontend-races.
**Tracker:** Repo lives on GitLab. No GitHub Issues sink configured. These findings are inlined verbatim as the durable record.

## Verdict

**Ready to merge.** Six `safe_auto` fixes applied in commit `a6064fe79`; three residual items below describe test-coverage gaps the reviewers flagged but that were not appropriate for autonomous autofix (each adds substantive new test surface or branching). All gate the depth of the regression net, not the correctness of the shipped fix.

## Applied safe_auto fixes (in commit `a6064fe79`)

1. **Plan U2 KTD prose updated** to reflect the actual `min-height + max-height` implementation and the `cts-json-editor.resolveBounds()` constraint that drove the refinement. The original text said "switch to fixed `height`" and explicitly rejected the pair we shipped, which would mislead a future reader bisecting a height regression. Cross-reviewer corroboration: maintainability (P1, anchor 90), project-standards, julik-frontend-races.
2. **Reflow regression e2e test now stalls the initial `/api/log` fetch** with a manually-released promise so `colsBefore` is provably captured while the rail is still `[hidden]`. Adds a load-bearing `expect(rail).toHaveAttribute("hidden", "")` ahead of the snapshot. Without the stall the bootstrap fetch resolved fast enough that the equality check could have passed trivially against post-event state — verified empirically when the assertion failed before the stall was added. Cross-reviewer corroboration: correctness (P2), testing (medium, anchor 75), julik-frontend-races (warning, anchor 75).
3. **Tightened `grid-template-columns` regex** from `/\s320px$/` to `/^\d+(\.\d+)?px 320px$/` in the two assertions touched by this change. The looser pattern would have matched stray third tracks (e.g., `'200px 200px 320px'`). Testing reviewer (T2, medium anchor 75).
4. **`scrollHeight > 0` strengthened to `> hostRect.height`** in `ConfigDrawerHeightLockedAtFixedValue`. The bare `> 0` check would have passed for an empty Monaco editor even if the oversized 60-key payload failed to load. Correctness reviewer (P3, anchor 50).
5. **`ViewConfigViaKebab` gains a floor assertion**: `Math.abs(rect.height - 336) <= 1` so a small (3-key) config still pins the editor at exactly 336px via `min-height`. Pairs with the ceiling assertion in `ConfigDrawerHeightLockedAtFixedValue` so a future change that drops `min-height` and keeps only `max-height` is caught. Testing reviewer (T4, low anchor 50).
6. **`cts-log-toc.stories.js` `EmptyDuringWaiting` comment** rewritten to drop the removed `:has()` reference and describe the new unconditional grid contract. Correctness reviewer (P3, anchor 100).

## Residual Actionable Work

### 1. [P2] Drawer close-path produces no layout-shift coverage

- **Files:** `src/main/resources/static/components/cts-log-detail-header.stories.js`
- **Reviewers:** testing (TG1), julik-frontend-races (TG-1).
- **What's wrong:** Plan requirement R3 says "opening **or closing** the disclosure produces exactly one layout shift." The new `ConfigDrawerHeightLockedAtFixedValue` story only exercises the open path. A regression that left the closed drawer with a `336px`-tall body (e.g., a future refactor that mounted Monaco eagerly outside the `<details>` body) would not be caught.
- **Fix path:** Extend `ConfigDrawerHeightLockedAtFixedValue` to (a) open the drawer, await Monaco mount, snapshot `<main>` `.scrollHeight` as `openHeight`, (b) close the drawer via the kebab "View configuration" toggle (or by setting `details.open = false`), (c) assert the body collapses by approximately `openHeight - <chrome-baseline>` — i.e., the closed drawer's contribution to page height is zero, not 336px. Use the existing `clickOverflowAction` helper for symmetry.
- **Recommendation:** Apply in a follow-up unit. Low complexity (~15 lines added to the existing story).

### 2. [P3] Sub-1440px viewport not exercised for U1

- **Files:** `frontend/e2e/log-detail.spec.js`
- **Reviewer:** testing (TG2).
- **What's wrong:** The new and updated e2e tests run at viewport 1500px. The `@media (min-width: 1440px)` guard means at narrower widths the grid should NOT activate — but no test asserts this. A typo that changes the breakpoint (e.g., `1440` → `1040`) would activate the rail column on mobile/tablet without any test failing.
- **Fix path:** Add one e2e test at viewport 1200px that loads the same fixture as the reflow-guard test and asserts `getComputedStyle(main).gridTemplateColumns === "none"` (single-column at narrow widths). This also doubles as coverage for the `@media (max-width: 1439px) { #ctsLogToc { display: none; } }` rule.
- **Recommendation:** Apply in a follow-up unit. Trivial (~20 lines), guards a one-character typo class.

### 3. [P3] Monaco-fallback path branching in `ConfigDrawerHeightLockedAtFixedValue`

- **Files:** `src/main/resources/static/components/cts-log-detail-header.stories.js`
- **Reviewer:** testing (T3, low anchor 75).
- **What's wrong:** `await configJson.whenReady()` resolves with `{kind: 'monaco' | 'fallback', el}`. If Monaco fails to load (timeout, CSP, missing vendor file), `configJson.querySelector('.monaco-scrollable-element')` returns `null` and `expect(scrollable).toBeTruthy()` throws a generic null-pointer error rather than a meaningful "Monaco did not load" failure. The fallback textarea's height contract is also unverified.
- **Fix path:** Capture the return value of `whenReady()` as `const ready = await configJson.whenReady()`. If `ready.kind === 'monaco'`, run the current `.monaco-scrollable-element` assertions. If `ready.kind === 'fallback'`, assert `configJson.querySelector('.oidf-json-editor-fallback')` is truthy and that its `scrollHeight > hostRect.height` (the textarea also needs to be scrollable for long content under the bounded host).
- **Recommendation:** Apply when next touching this story. Edge-case improvement — failure surfaces as a less-readable error rather than a wrong test result, so impact is on debuggability rather than correctness.

## Residual Risks (acknowledged, no action proposed)

- **The `336` magic number is repeated across three test sites** (one e2e comment, two story assertions). If `--space-6` ever changes from 24px, all three drift. Mitigation: a JS helper that reads `getPropertyValue('--space-6')` and multiplies by 14 would fix this but adds indirection for a token unlikely to change. Left as a coupling-risk note.
- **Rapid open/close/open of `drawer-config` is untested** (julik TG-1). The current `_bootMonaco` path has an `isConnected` guard and an `_editor` non-null guard, so the second open should reuse the already-mounted instance — but no test exercises this. Not a current risk; would only matter if the disclosure pattern were replaced with a more aggressive disconnect-on-close flow.
- **The reversal of the 2026-05-20-002 U2 decision** (column always reserved at ≥1440px) is documented in both the inline HTML comment and the new plan's KTD. Whoever reads the 2026-05-20-002 plan next will be confused unless they follow the cross-reference. The learnings researcher suggested adding a dated footnote to the 2026-05-20-002 plan itself; not done because the cross-reference chain is already two-way (HTML comment → both plans, new plan → old plan).
