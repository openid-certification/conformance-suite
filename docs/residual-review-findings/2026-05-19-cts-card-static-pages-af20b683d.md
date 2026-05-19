---
created: 2026-05-19
branch: feat/redesign
feature_commit: 46d1c32b6
review_commit: af20b683d
plan: docs/plans/2026-05-18-010-feat-wire-cts-card-into-static-pages-plan.md
board: docs/plans/2026-05-18-006-orphan-components-wire-up-board.md
run_artifact: /tmp/compound-engineering/ce-code-review/20260519-092839-2318355f/
---

# Residual Review Findings — cts-card wire-up into tokens / login / upload

LFG iteration on Row 4 of the orphan-components board: wire `<cts-card>` into three static pages.

Two commits shipped together: `feat(cts-card): wire static pages into cts-card panels` (`46d1c32b6`) and `fix(review): apply autofix feedback` (`af20b683d`).

ce-code-review autofix surfaced 3 `safe_auto` fixes (applied inline) and 1 residual P1 that requires maintainer decision.

---

## Residual actionable (1)

### [P1] `src/main/resources/static/login.html:25` — Double-chrome from cts-card wrap of cts-login-page

**Corroborated by:** correctness (P1, conf 100) and maintainability (P1, conf 90). Two independent reviewers converged on this finding.

**Status:** Shipped as written. Plan's Open Questions section pre-authorized the descope contingency; the decision belongs to maintainers (Joseph or Thomas per board) at MR review.

**The concern.** `cts-login-page` already paints full panel chrome on its inner `.oidf-login-card` section: `background: var(--bg-elev)`, `border: 1px solid var(--border)`, `border-radius: var(--radius-4)`, `box-shadow: var(--shadow-3)` (see `src/main/resources/static/components/cts-login-page.js` lines 67-102). Wrapping it in `<cts-card>` (which paints `bg-elev`, `border`, `radius-3`, `shadow-1` plus `--space-5` body padding) creates a visible card-inside-card outline on a public-facing unauthenticated page.

The plan's Open Questions section explicitly named this risk:

> *"Is the card-wrap actually a visual improvement, or just visual noise? This is the question Joseph/Thomas may push back on. If the answer is 'noise', descope U2/U3 (the speculative ones) and keep only U1 (tokens.html, the clearest win because the page is otherwise a bare table)."*

`tokens.html` (U1) is unaffected — its `cts-token-manager` has no outer chrome, so the card wrap is genuinely additive. `upload.html` (U3) is also unaffected — `cts-image-upload`'s bg/border styling is on interior elements (the drop zone), not on the outer host.

**Recommended action.** If maintainer review confirms the double-chrome reads as noise: descope U2 with a 3-line revert across two files.

1. `src/main/resources/static/login.html`: remove the `<cts-card>` wrapper and the `<script type="module" src="/components/cts-card.js"></script>` import.
2. `frontend/e2e/login.spec.js:24-27`: remove the `await expect(page.locator("cts-card")).toBeVisible();` assertion and its comment.

If maintainer review accepts the wrap as-is: no action needed; the card chrome will inset slightly relative to the inner login-card border. Consider a JSDoc note on `cts-card.connectedCallback` documenting when the wrapper is appropriate (composite has no outer chrome) vs redundant (composite already paints panel chrome).

---

## Advisory / acknowledged (no change)

### `frontend/e2e/tokens.spec.js:49` — `.oidf-card-header` selector coupling

Reviewer disagreement: testing said keep (anchor 50), correctness said no change (P3 conf 75, strict-match holds because `cts-card.js:93` uses textContent), maintainability said drop (P2 conf 75 — couples test to internal class name). Per conservative-route-on-disagreement: kept as-is. The pair of assertions (`cts-card[header="API Tokens"]` visibility on line 48 + `.oidf-card-header` text on line 49) collectively prove both attribute wiring and the textContent render pipeline.

If `cts-card` is ever refactored to Shadow DOM, revisit this selector — `.oidf-card-header` would become unreachable from light-DOM Playwright locators.

### `src/main/resources/static/components/cts-card.js` — JSDoc note on import order

Correctness reviewer recommended a JSDoc note documenting that `cts-card.js` should be imported before its Lit children to ensure single-upgrade behavior. The `_initialized` guard added in `af20b683d` makes the lifecycle idempotent regardless of import order, so the note is documentation-only. Could be added in a follow-up if reviewers prefer; not blocking.

---

## Pre-existing (separate from this diff)

### `frontend/e2e/login.spec.js:30+` — 4 OAuth-button assertions target `cts-link-button` selectors that don't exist

Pre-existing on `feat/redesign` HEAD. `cts-login-page` renders OAuth buttons as plain `<a class="oidf-btn oidf-btn-secondary oidf-btn-lg" href="/oauth2/authorization/google">` (component comment at line 220 explicitly documents this). The spec asserts `cts-link-button[href="/oauth2/authorization/google"] a` — a selector that finds nothing.

Verified pre-existing via `git stash` of my local edits — the 4 failures reproduce on bare HEAD.

Tracked in user memory: `feedback_e2e_pre_existing_failures_2026_05_18.md`. Recommended action: rewrite the 4 assertions to target the rendered DOM (`a.oidf-btn-secondary[href="/oauth2/authorization/google"]` etc.). Out of scope for this iteration.

---

## Applied safe_auto fixes (3, recorded for the audit trail)

Applied in `af20b683d` (for the cts-card.js guard) and `46d1c32b6` (for the e2e assertion strength fixes, which lived on the same lines as the feature additions and were folded in):

1. **`frontend/e2e/login.spec.js:25`** — `toHaveCount(1)` → `toBeVisible()`. Strictly stronger assertion; catches a card that exists in DOM but is hidden.
2. **`frontend/e2e/upload.spec.js:75`** — same upgrade.
3. **`src/main/resources/static/components/cts-card.js:67`** — added `_initialized` re-entry guard. Pre-existing latent bug surfaced by the 3 new call sites; mirrors the pattern in `docs/solutions/web-components/cts-modal-bootstrap-interop-2026-04-17.md`. Without the guard, any DOM move (Turbo nav, LiveReload swap, manual reparent) would refire `connectedCallback` and nest `.oidf-card > .oidf-card-body > .oidf-card > .oidf-card-body`.

---

## Verification

- `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` succeeds (exit 0).
- `cd frontend && npm run test:ci` clean (format / lint / type-check / jsdoc / lit-analyzer / codegen). 59 pre-existing warnings in unrelated story files; 0 errors.
- `cd frontend && npm run test:e2e` — 104 pass / 20 fail. All 20 failures pre-existing on HEAD (verified for login.spec.js via stash; the other 16 are in specs this iteration didn't touch). Tokens/login/upload e2e additions all pass.
- ce-test-browser smoke verification pending (LFG step 6).

Reviewers: correctness (Opus 4.7), testing / maintainability / project-standards / julik-frontend-races / agent-native / learnings-researcher (Sonnet).
