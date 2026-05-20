# Residual review findings — logs owner cell + icon guardrails

**Commit:** `3e8201332` — fix(logs,cts-icon): repair owner cell + chip close icon + add icon-name guardrails
**Plan:** `docs/plans/2026-05-19-007-fix-logs-owner-cell-and-icon-guardrails-plan.md`
**ce-code-review run:** `20260519-230311-823d2556`
**Mode:** autofix
**Reviewers:** correctness, testing, maintainability, project-standards, julik-frontend-races (5 personas)

## Context

Three logs.html UI bugs (active-filter chip close icon, owner-cell layout, globe-tooltip orphan) plus a cross-cutting icon-name guardrail layer (`lint:icons` CI script + `cts-icon.js` runtime warning + `CLAUDE.md` progressive-disclosure pointer + AllIcons Storybook HEAD-fetch). Eight `safe_auto` findings were applied inline during the autofix pass; what follows is the residual work that needs maintainer-driven follow-up. All four items are P3 advisory — none block merging.

## Applied during autofix (folded into 3e8201332)

The cross-reviewer corroboration during synthesis promoted several findings into the actionable tier before they landed:

1. **`firstUpdated()` → `updated()` + `_watchedUseEl` ref** — julik-frontend-races (P1, conf 90) + correctness (P3, conf 75). The original `firstUpdated()` would attach the `error` listener once, on first update — when first render returns `nothing` (empty `name`), `querySelector('svg use')` returns null and the listener never wires. The fix moves to `updated()` and tracks the watched element by ref so re-renders don't double-wire but the deferred-name lifecycle is guarded.
2. **`ValidIconNoWarning` timing fix** — 3 reviewers (testing P3 conf 80, maintainability P2 conf 75, correctness P3 conf 50). The original `setTimeout(500)` passed vacuously on slow runners where the fetch was still in-flight at assertion time. Replaced with `waitFor(use element present) + 50ms drain tick`.
3. **Lint script: single-quoted setAttribute literals** — correctness (P3, conf 100). Broadened the regex to accept both quote styles since the project doesn't pin one.
4. **`warnedNames` JSDoc honesty** — maintainability (P1, conf 75). Documented the module-lifetime scope honestly rather than changing to per-instance (per-instance would break the WarnsOncePerName test contract, which is "warn once per *name*, ever").
5. **U2 plan scenarios: viewport-shrink height assertion + negative-shape assertion** — testing (T-001 and T-002, both P2 conf 90). These were prescribed in the plan but skipped in the initial ce-work pass; added during autofix.
6. **CLAUDE.md "Frontend quality gates" + `frontend/README.md` test:ci row** — project-standards (PS-001 P2 conf 100, PS-002 P3 conf 100). The new `lint:icons` step was added to `test:ci` in `package.json` but the docs enumerating the chain were stale.

## Residual actionable work (downstream-resolver)

All four items are P3 advisory and none block merging the MR.

| # | Sev | File | Title | Reviewer |
|---|-----|------|-------|----------|
| 9 | P3 | `frontend/scripts/lint-icon-names.sh` | Lint script over-cautious in mixed-context files. If a single file builds cts-icon elements AND uses `setAttribute("name", "<short>")` on an unrelated input element (e.g. a form input), the file-scope proxy will false-positive flag the input's name. No such file exists today; documented as a known trade-off in the script. The fix would be either per-file allowlist support or extracting cts-icon construction into its own module. | maintainability M-02, correctness #1 |
| 10 | P3 | `frontend/scripts/lint-icon-names.sh` | Lint script lacks an automated self-test. Plan U3 enumerated three test scenarios (clean tree exits 0, planted bad icon exits non-zero with hint, dynamic-names file exits 0); only the regression-detection scenario was exercised via manual sed-revert verification. A `bats`/`shellspec` test or an inline `--test` mode would lock in the behavior. | testing T-004 |
| 11 | P3 | `frontend/package.json` | `lint:icons` inserted before `lint:lit-analyzer` rather than after, per plan U3 spec (`after lint:lit-analyzer and before codegen:check`). Functionally equivalent ordering. Current placement groups bash lint scripts together (`lint:jsdoc`, `lint:icons`) before the TS-driven gates. Either accept the deviation (update the plan to reflect the actual placement) or move the entry to plan-specified position. | project-standards PS-003 |
| 12 | P3 | `frontend/e2e/logs.spec.js` | Owner-cell test hard-codes fixture values `12345` / `https://accounts.google.com` instead of reading from `MOCK_LOG_LIST[0].owner`. Confidence 50, below the synthesis gate, kept as residual. Coupling is real but the fixture is stable; worth a small follow-up to derive expected strings from the fixture so the test self-describes intent. | maintainability M-04 |

## Findings suppressed during synthesis

- **AllIcons.play 48-concurrent-HEAD-fetch concurrency risk** (julik RR-001, conf 40) — HTTP/1.1 connection limit batches into rounds of 6 under load; CI flake risk only. Not a component race.
- **WarnsOncePerName dedupe documentation** (julik RR-002, conf 30) — current `Date.now() + Math.random()` strategy is correct; risk is "future editor removes uniqueness without understanding it." Could document in story JSDoc; deferred.
- **Lint script `is_iconish()` filter skips uppercase/underscore variants** (correctness RR-2, conf 50) — coolicons names are canonically lowercase kebab; this is a defensive choice that admits the rare case of case-variant typos at runtime where the warning catches it.

## Pre-existing (not introduced)

Verified pre-existing on `feat/redesign` HEAD before the autofix pass:

- 1 e2e flake (`logs.spec.js "search triggers cts-data-table re-fetch with Enter key"`) — matches user-memory `feedback_e2e_pre_existing_failures_2026_05_18`.
- 12 storybook flakes across 8 files (cts-tooltip, cts-log-entry, cts-data-table, cts-log-viewer, log-detail, cts-failure-summary, cts-json-editor, cts-log-detail-header) — matches user-memory `feedback_storybook_pre_existing_flakes`.

## Verification after autofixes

- `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` — exit 0
- `npm run test:ci` — 0 errors across all 7 sub-checks (format:check, lint, type-check, lint:jsdoc, lint:icons, lint:lit-analyzer, codegen:check)
- `cts-icon` storybook — 11/11 pass (8 originals + 3 new warning-behavior stories)
- `logs.spec.js` Owner + chip regression tests — 2/2 pass, including the new viewport-shrink height assertion and negative-shape regression guard
