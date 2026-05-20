# Residual Review Findings — Dirty form exit guard

**Branch:** `feat/redesign`
**HEAD at review:** `09aca8ec8` (after autofixes)
**Source plan:** [`docs/plans/2026-05-18-013-feat-dirty-form-exit-guard-plan.md`](../plans/2026-05-18-013-feat-dirty-form-exit-guard-plan.md)
**Review run:** `/tmp/compound-engineering/ce-code-review/20260519-205228-c6a6767d/`
**Tracker:** No GitHub remote configured (repo lives on GitLab — `gh pr view` failed). No sink available — these findings are inlined verbatim as the durable record.

## Residual Review Findings

### Cross-reviewer corroborated (highest confidence)

- **[P1] `frontend/e2e/schedule-test.spec.js` — beforeunload reload path has no automated coverage at any level.** (`testing`, `agent-native` — anchor 100). Plan U5 explicitly required a "browser reload on dirty form" test (with `test.skip` allowed if Playwright cannot surface beforeunload reliably). Neither was added. The `_onBeforeUnload` branch is entirely untested at the integration level. **Fix:** add the test; use `page.on('dialog', d => d.dismiss())` per the modern-web-guidance pattern.

- **[P2] `src/main/resources/static/schedule-test.html:1376` — Generate JWKS writes to `ctsConfigForm.config` without arming the guard.** (`adversarial` — anchor 100). `cts-config-form` only dispatches `cts-config-change` on user-driven `_handleFieldChange`/`_handleJsonInput`, not on external `.config` assignment. Generated keys are not stashed in sessionStorage by `saveConfig`. **Fix:** call `exitGuard.markDirty()` after each `addGenerateJwks` write.

- **[P2] `src/main/resources/static/components/cts-unsaved-changes-guard.js:247` — modalId computed in two independent code paths.** (`maintainability`, `julik-frontend-races` — anchor 100). `render()` and `_onModalButtonClick` each derive `modalId` from `this.id`; `this.id` is not a reactive property, so a post-render id change silently drifts the click router. **Fix:** introduce a `get _modalId()` accessor used in both places, or switch to `data-action="leave"|"stay"` on the footer-button descriptors (requires verifying `cts-modal._createButton` propagates `data-*` attributes).

- **[P2] `src/main/resources/static/components/cts-unsaved-changes-guard.js:67` — cts-modal one-shot init makes guard's reactive `heading`/`confirmLabel`/`cancelLabel` properties misleading.** (`correctness`, `maintainability` — anchor 100). Lit advertises dynamic updates; `cts-modal` reads `heading` and `footer-buttons` exactly once at its own `connectedCallback`. **Fix:** document the one-shot limitation in JSDoc, or force-replace the inner modal on label change via Lit's `repeat`/key pattern.

- **[P3] `src/main/resources/static/components/cts-unsaved-changes-guard.js:60` — JSDoc claims `cts-unsaved-changes-stay` fires on ESC/backdrop/X dismissal, implementation only fires it on Stay button click.** (`correctness` — anchor 100). **Fix:** add a `cts-modal-close` listener that calls `_onStayClick()` when `_pendingHref` is still set; make `_onStayClick` idempotent (early-return when `_pendingHref` is null) to handle the Stay-button case where both listeners would otherwise fire.

- **[P3] `src/main/resources/static/components/cts-unsaved-changes-guard.js:247` — Multiple `<cts-unsaved-changes-guard>` instances collide on modal id fallback.** (`correctness`, `adversarial` — anchor 100). Fallback `cts-unsaved-guard-modal` is a literal. **Fix:** per-instance unique suffix via the `cts-form-field._uid` counter pattern.

- **[P2] `frontend/e2e/schedule-test.spec.js` — "Load last configuration does not trigger the guard" test missing.** (`testing` — anchor 100). Plan U5 explicitly required this case; `clearConfigForNewPlan` markClean and `loadLastConfigFromServer` `applyConfigToForm` paths are wired but unverified.

- **[P2] `src/main/resources/static/components/cts-unsaved-changes-guard.stories.js:139` — No symmetric "Leave clears dirty" story.** (`testing` — anchor 100). `_onLeaveClick` calls `markClean()` before navigating; the invariant is unverified at the story level.

### Single-reviewer findings (anchor 75)

- **[P2] `src/main/resources/static/schedule-test.html:703` — POST failure leaves guard disarmed.** (`julik-frontend-races`). Even after the variant-validation move, `markClean()` still runs *before* `fetch()`. If POST fails and the user clicks away without further editing, no warning fires. **Fix:** move `markClean()` into the `.then()` success branch; call `markDirty()` in `.catch()` to restore the guard on failure.

- **[P2] `src/main/resources/static/components/cts-unsaved-changes-guard.js:145` — URL-driven variant restore fires synthetic `change` event, arms guard pre-edit.** (`correctness`). `schedule-test.html:1198` and `registerEkycSecurityProfileChangeEvent:286` dispatch synthetic `Event('change')` to repopulate variant `<select>`s. **Fix:** add `if (event && !event.isTrusted) return;` at the top of `_onSubjectEdit`.

- **[P3] `src/main/resources/static/schedule-test.html:471` — Three identical `getElementById('exitGuard')` + `typeof markClean` + call patterns.** (`maintainability`). **Fix:** extract a `disarmExitGuard()` helper at the top of the inline script.

- **[P3] `src/main/resources/static/components/cts-unsaved-changes-guard.js:199` — Empty `href=""` bypass treated as hash-only nav.** (`correctness`). **Fix:** require `anchor.hash !== ""` and `anchor.hash !== window.location.hash` in the bail check.

- **[P2] `src/main/resources/static/components/cts-unsaved-changes-guard.stories.js:133` — Stories and E2E couple to `cts-modal` internal class `dialog.oidf-modal[open]` rather than the public `cts-modal[open]` attribute.** (`testing`). **Fix:** refactor selectors to use the host attribute for stability against `cts-modal` internal class renames.

- **[P3] `src/main/resources/static/components/cts-unsaved-changes-guard.stories.js:209` — Missing `change` event and `disconnectedCallback` cleanup stories.** (`testing`). Production case for `change` is the variantSelectors `<select>`; cleanup was listed in plan U1 test scenarios but not exercised.

## Advisory (no fix required)

- **`composedPath()` silently omits anchors inside closed shadow roots** (`julik-frontend-races`). All current CTS components use open shadow roots; document the limitation in JSDoc.
- **`window.location.*` / form submit / non-anchor navigation skips the styled modal** (`adversarial`). Schedule-test does not currently use these patterns; document the composition gap.
- **`markDirty()` public API exists only for test usage** (`maintainability`). Either remove and have stories trigger the real event path, or annotate JSDoc as test-only.

## Modern-web-guidance validation

Confirmed in this run:

- `beforeunload` with `preventDefault()` + `returnValue = ""` is the correct cross-browser pattern.
- Navigation API is correctly identified as the wrong tool for MPA cross-document guards (no Safari/Firefox support; same-document scope only).
- `composedPath()` is the standard for shadow-boundary anchor lookup.
- `AbortController`/`AbortSignal` for listener cleanup is current best practice (catalog `featuresUsed`).
- Reflected `dirty` attribute is valid; `CustomStateSet` (`:state(dirty)`) is the modern alternative but optional.
- `cts-modal`'s JS-based backdrop dismissal matches the modern-web-guidance fallback for `closedby="any"` (which Safari does not yet support).
