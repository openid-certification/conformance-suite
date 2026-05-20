# Residual Review Findings — feat: Modernise the loading modal

**Source PR run:** ce-code-review autofix, run-id `20260520-000452-556c1802`
**Branch:** `feat/redesign`
**Tip after autofix:** `226e36eeb`
**Plan:** [docs/plans/2026-05-19-009-feat-modern-loading-modal-plan.md](../plans/2026-05-19-009-feat-modern-loading-modal-plan.md)
**Reviewers (9/9 returned):** correctness, testing, maintainability, project-standards, agent-native, learnings, kieran-typescript, julik-frontend-races, adversarial.

This file is the durable handoff target for the LFG pipeline running on GitLab (the standard pipeline targets GitHub PR bodies; this repo is on GitLab, so the fallback file is the record). Existing GitLab MR <https://gitlab.com/openid/conformance-suite/-/merge_requests/1998> tracks the umbrella `feat/redesign` work — this file captures the per-LFG-run residuals.

## Verdict

**Ready with fixes.** Three `safe_auto` items applied in commit `226e36eeb`. Five residual items below need owner decisions before the loading-modal modernisation can be declared fully shipped — the highest-priority one is the exit-animation issue (P1).

## Residual Actionable Work

### 1. [P1] Modal exit animation cancelled by host `display: none`

- **Files:** `src/main/resources/static/components/cts-modal.js:48` (host display rule), `:101-130` (animation block).
- **Reviewers:** correctness + julik-frontend-races + adversarial (3-way agreement, confidence 100).
- **What's wrong:** The autofix added a `dialog.oidf-modal:not([open])::backdrop` exit-state rule, so the backdrop scrim now fades on close. But the dialog *body* still snap-closes. When `host.removeAttribute("open")` fires, the host's `cts-modal:not([open]) { display: none }` rule yanks the entire subtree out of layout the same frame, which aborts the inner `<dialog>`'s `:not([open])` transition before it can render a single frame. The entry fade-in works correctly (top-layer rendering ignores ancestor `display: none` on the way in); the exit does not.
- **Fix paths (pick one):**
  1. **Animate the host too** — duplicate the `opacity` / `transform` / `allow-discrete` machinery onto `cts-modal` so the host stays in layout for `var(--dur-2)` after `[open]` is removed. Least invasive to existing Playwright `toBeVisible()` assertions.
  2. **Defer the host attribute strip** — in the dialog's `close` event listener, await `transitionend` on the dialog before calling `this.removeAttribute("open")`. Adds latency to the host's perceived-closed state.
  3. **Visibility-based gate** — replace `display: none` on the closed host with `visibility: hidden` + `pointer-events: none`. Keeps the host in layout permanently; tests that check `display` would need to switch to `visibility`. Playwright's `toBeVisible()` already accounts for both.
- **Verification:** visual smoke test of `host.show()` / `host.hide()` under default motion (exit fade should be visible); a reduced-motion DOM test that confirms no one-frame flash occurs at `transition-duration: 0ms`.

### 4. [P3] Reduced-motion CSS branches untested

- **Files:** `src/main/resources/static/components/cts-spinner.stories.js`, `src/main/resources/static/components/cts-modal.stories.js`.
- **Reviewers:** testing + project-standards (cross-corroborated against the plan's Test scenarios for U1 + U2).
- **What's missing:** Plan U1 called for a `getComputedStyle(svg).animationName === "cts-spinner-pulse"` assertion under reduced motion; U2 called for `getComputedStyle(dialog).transitionDuration === "0s"`. Neither landed because the storybook test runner's reduced-motion emulation is finicky. The CSS branches are still real — they just have no regression guard.
- **Suggested fix:** Add one play story per component using Storybook's `parameters: { reducedMotion: "reduce" }` (Storybook 9 honours this in the addon-themes-style stack on this repo). Assert against `getComputedStyle` on the inner SVG (cts-spinner) and inner dialog (cts-modal). If the assertions are flaky in the Vitest browser harness, document the omission with a `// no runner-stable way to assert this — see ce-code-review run 20260520-000452-556c1802` comment next to each `@media` block.

### 5. [P3] E2E cutover assertions absent on the 6 rolled-out pages

- **Files:** `frontend/e2e/{logs,schedule-test,plan-detail,log-detail,running-test,upload}.spec.js`.
- **Reviewers:** testing.
- **What's missing:** Storybook `StaticBackdrop` story now actively rejects any `<img>` inside the loading modal (autofix `auto-2` broadened the check). That covers the component contract. It does not cover the page-level integration — a hand-edit on one HTML page reintroducing an `<img>` would only fail a story that includes that page's HTML. The plan U3 verification list explicitly called for per-page e2e assertions; the agent covered them in the story instead.
- **Suggested fix:** ~6 lines per spec. Wrap the assertion in `page.waitForSelector('#loadingModal cts-spinner svg circle')` to catch it during the `FAPI_UI.showBusy()` window (the loading modal disappears as soon as the page fetch resolves; the assertion must run before that). Reference `docs/solutions/test-failures/playwright-e2e-flaky-after-web-component-merge-2026-04-14.md` for the timing pattern.

### 8. [P3] `cts-spinner.replaceChildren` wipes consumer-supplied content with no slot contract

- **File:** `src/main/resources/static/components/cts-spinner.js:132`.
- **Reviewers:** adversarial.
- **What's wrong:** A consumer writing `<cts-spinner>Processing…</cts-spinner>` loses the text on first `connectedCallback` invocation. No JSDoc warns about it. Every other `cts-*` component in the repo either uses Shadow DOM slots or documents the no-slot contract; cts-spinner does neither.
- **Suggested fix paths:**
  1. **Document and accept** — add to `cts-spinner.js` JSDoc: `cts-spinner has no slot — any descendant content is overwritten on upgrade. Pass the accessible label via the \`label\` attribute.` Matches current behaviour; lowest churn.
  2. **Preserve consumer content** — gate the `replaceChildren` call on `if (!this.firstElementChild)`. Consumer text would survive and could be announced alongside `role="status"`. Slightly more code; potential AT-announcement interaction with the visually-hidden span.

### meta-1. [P3] `git add <file-by-name>` still captures pre-existing unstaged WIP

- **File:** `CLAUDE.md` (the existing `## Git Workflow Preference` / `## Git Operations` section).
- **Reviewers:** adversarial + learnings (learnings flagged the existing `docs/solutions/web-components/coolicons-system-2026-04-25.md` already documents this trap).
- **What's wrong:** During this LFG run, U3's `git add <files-by-name>` for `plan-detail.html` and `schedule-test.html` silently committed unrelated unstaged work from two other in-progress LFG runs (a partial revert of the cts-batch-runner integration, and half of the cts-test-selector keyboard-nav plan). Commit `acb3fa38b` corrected the symptom, but no tooling, instruction, or convention prevents the next recurrence.
- **Suggested fix:** Add a "Pathspec discipline" note to the existing Git section in CLAUDE.md. Suggested wording:
  > Before `git add <path>` during a multi-step LFG run, `git diff <path>` first. Staging by name captures any pre-existing unstaged edits in that file from a concurrent LFG run — silently reverting shipped features or landing partial-feature WIP. Use `git add -p <path>` to interactively pick hunks when the working tree has divergent WIP.
- The existing solution doc (`coolicons-system-2026-04-25.md`) already mentions pathspec discipline as the cure; promoting it into CLAUDE.md raises its visibility for future LFG runs.

## Pre-existing (out of scope)

- **`FAPI_UI.showBusy` upgrade race** — `js/fapi.ui.js:328` reads `loadingModal-title` before the `cts-modal` module guarantees that element exists. Surfaced by adversarial. Risk has existed since the cts-modal rewrite, not introduced by this work.

## Suppressed (below confidence gate)

- Stale `text-center` wrapper class on 4 of 5 pages — Bootstrap utility, only loaded on `schedule-test.html`. Maintainability finding at anchor 55; suppressed.
- Filename-coupled GIF regression check (`img[src*="spinner.gif"]`) — addressed by autofix `auto-2` (now checks any `img`).
