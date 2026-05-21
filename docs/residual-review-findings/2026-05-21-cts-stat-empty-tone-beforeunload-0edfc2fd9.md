# Residual Review Findings — cts-stat empty tone + beforeunload integration test

**Source PR run:** `ce-code-review mode:autofix`, run-id `20260521-000050-fab179d2`
**Branch:** `feat/redesign`
**Tip after autofix + commit:** `0edfc2fd9`
**Plan:** [docs/plans/2026-05-20-006-fix-cts-stat-empty-tone-and-dirty-form-reload-test-plan.md](../plans/2026-05-20-006-fix-cts-stat-empty-tone-and-dirty-form-reload-test-plan.md)
**Source residuals (closed by this commit):**
- [docs/residual-review-findings/2026-05-18-dirty-form-exit-guard.md](2026-05-18-dirty-form-exit-guard.md) — P1 beforeunload coverage gap (closed)
- [docs/residual-review-findings/2026-05-19-cts-stat-dashboard-05df5c786.md](2026-05-19-cts-stat-dashboard-05df5c786.md) — "Zero failures + zero logs renders tone='pass' misleadingly" (closed)
**Reviewers (6/6 returned):** correctness, testing, maintainability, project-standards, agent-native, learnings-researcher.
**Tracker:** No GitHub remote configured (repo lives on GitLab — `gh pr view` failed). No sink available — these findings are inlined verbatim as the durable record.

## Verdict

**Ready with fixes.** Three `safe_auto` items applied in commit `0edfc2fd9` (baked into the main commit because the autofix delta touched files ce-work hadn't yet committed). Three residual items below need owner decisions before the work can be declared fully shipped.

## Applied safe_auto fixes (in commit 0edfc2fd9)

1. `resolveTone` JSDoc `@returns` updated to list `"empty"` as a valid return value (cross-reviewer corroboration: correctness + agent-native + maintainability + project-standards).
2. Dirty-form `beforeunload` test comment no longer claims the handler sets `event.returnValue` — the production handler at `src/main/resources/static/components/cts-unsaved-changes-guard.js:165-168` only calls `event.preventDefault()` (corroboration: correctness + testing).
3. `ToneEmpty` story now exercises `DELTA_COLOR['empty']` via a non-empty delta value plus computed-style assertion, matching the coverage shape used by `TonePass` and `ToneFail`.

## Residual Actionable Work

### 1. [P2] Dirty-form beforeunload test comment cites task artifacts — violates CLAUDE.md "no current-task references" rule

- **Files:** `frontend/e2e/schedule-test.spec.js:1140-1156` (opening block of the `dirty form: window beforeunload event has its default prevented` test).
- **Reviewers:** project-standards (anchor 100).
- **What's wrong:** The comment block opens with `Closes the P1 testing gap from / docs/residual-review-findings/2026-05-18-dirty-form-exit-guard.md`, names "the residual file's fix recipe", and says "The residual explicitly permitted test.skip in that case". All three phrases reference the current task / fix / source artifact, which CLAUDE.md proscribes ("Don't reference the current task, fix, or callers ('used by X', 'added for the Y flow', 'handles the case from issue #123')"). The residual file is gitignored locally and may be renamed or removed later; the citation will rot.
- **What's worth keeping:** the headless-Chromium WHY (synthetic event dispatch as a sufficient proxy when `page.reload()` and `page.close({runBeforeUnload:true})` both fail to surface the dialog) is a genuinely non-obvious constraint — that part of the comment earns its keep.
- **Fix:** Rewrite the opening block to keep only the headless-Chromium rationale. Remove the `Closes the P1 testing gap from <path>` line, the `The residual file's fix recipe was page.reload() + page.on('dialog', dismiss)` paragraph, and the `The residual explicitly permitted test.skip` framing. A two-or-three-sentence note about the headless constraint is enough.

### 2. [P3] `ZeroLogsEmptyTone` story comment names sibling story and references "the bug fix"

- **File:** `src/main/resources/static/components/cts-dashboard.stories.js:567`.
- **Reviewers:** project-standards (anchor 75).
- **What's wrong:** The trailing paragraph reads `// tile must still carry "pass" — exercised by AuthenticatedWithStatsAllPassing / // above. This story complements it by pinning the empty-of-empty / // branch that the misleading-tone bug fix targets.` Naming the sibling story by identifier (`AuthenticatedWithStatsAllPassing`) and referencing "the misleading-tone bug fix" both violate the same caller/task-reference rule.
- **Fix:** Trim the trailing paragraph. The value + tone + aria-label assertions above already document the contract. If a comment is desired, name only the contract being pinned, not the cross-story relationship or the bug history.

### 3. [P2] `ariaSuffix` field on `StatTile` co-varies 1:1 with `tone="empty"` — structurally redundant

- **Files:** `src/main/resources/static/components/cts-dashboard.js:39` (StatTile typedef), `:451-452` (failures tile descriptor), `:534` (render template).
- **Reviewers:** maintainability (anchor 75). Confidence 75 (not 100) because `ariaSuffix` *could* technically carry a custom suffix independent of tone — but no current consumer uses it that way, and no foreseen tile needs the flexibility.
- **What's wrong:** `tone: "empty"` and `ariaSuffix: "(no logs yet)"` are always set together by the same `logs.length === 0` predicate at line 451-452. A future tile author adding a second empty-state surface must remember to set both fields with no enforcement. Two fields = two places to drift.
- **Fix paths (pick one):**
  1. **Derive in the render template.** Drop the `ariaSuffix` field from `StatTile`. In the render template at `:534`, conditionally append `"(no logs yet)"` when `tile.tone === "empty"`. One source of truth. Trade-off: ties the aria-suffix string to the tone enum, so a non-empty-tone tile that wants a different suffix would need a different mechanism. Acceptable given current usage.
  2. **Document the redundancy and keep both.** Add a JSDoc note on `ariaSuffix` saying "Currently always set when tone === 'empty'; the separate field preserves future flexibility for tone-independent suffix overrides." Keeps the door open for a tone-independent suffix; pays in maintenance cost now.
- **Recommendation:** Path 1. The flexibility cost of removing `ariaSuffix` is zero-valued today and easy to reintroduce when a real consumer needs it.

## Advisory / FYI (no fix required)

- **`home.spec.js` e2e has no zero-logs scenario.** The new tri-state tone logic is covered by Storybook (`ZeroLogsEmptyTone`) but not by the page-level e2e suite. Storybook runs in CI against the same source, so the regression risk is small. If a future change moves logic into the e2e-only render path, add a zero-logs scenario to `home.spec.js`.
- **`data-stat-key` reflection not in `StatTile` typedef.** Play functions query `[data-stat-key="failed"]` but the field isn't documented. Low-stakes; no agent or future contributor has been bitten yet.
- **Synthetic beforeunload event dispatch cannot prove headless Chromium surfaces the actual prompt.** The handler-wiring contract is what's testable in CI; the browser-UI side is out of reach. Acknowledged in the test comment.

## Reviewer disagreement transparency

- `resolveTone` JSDoc finding: 3 reviewers said `safe_auto`, project-standards said `manual`. Synthesis applied as `safe_auto` because the fix is purely mechanical (add one word to the `@returns` list) and three reviewers explicitly committed to the same fix shape.
- All other findings had reviewer-route alignment; no other disagreements to resolve.
