---
title: "fix: Descope cts-card wrapper from login.html (double-chrome)"
type: plan
status: active
created: 2026-05-19
branch: feat/redesign
depth: lightweight
parent: docs/plans/2026-05-18-010-feat-wire-cts-card-into-static-pages-plan.md
residual: docs/residual-review-findings/2026-05-19-cts-card-static-pages-af20b683d.md
board: docs/plans/2026-05-18-006-orphan-components-wire-up-board.md
---

# fix: Descope cts-card wrapper from login.html (double-chrome)

## Summary

Resolve the P1 residual from the cts-card static-page wire-up (Row 4 of the orphan-components board). Two independent reviewers (correctness P1 conf 100, maintainability P1 conf 90) flagged that `cts-login-page` already paints full panel chrome on its inner `.oidf-login-card` section, so wrapping it in `<cts-card>` produces a visible card-inside-card on the public-facing unauthenticated login page. The parent plan's **Open Questions** section explicitly pre-authorized this descope contingency.

U1 (`tokens.html`) and U3 (`upload.html`) stay shipped — they wrap composites with no outer chrome, so the card is genuinely additive panel chrome. Only U2 (`login.html`) is being reverted.

## Problem Frame

- **Today:** `login.html` mounts `<cts-login-page>` inside `<cts-card>`. The inner `.oidf-login-card` section (`src/main/resources/static/components/cts-login-page.js` lines 67-102) paints `background: var(--bg-elev)`, `border: 1px solid var(--border)`, `border-radius: var(--radius-4)`, `box-shadow: var(--shadow-3)`. The outer `<cts-card>` adds another layer of `bg-elev` / `border` / `radius-3` / `shadow-1` plus `--space-5` body padding. Result: card-inside-card with ~20px inset.
- **Target:** `login.html` mounts `<cts-login-page>` directly inside `<main>` / `<body>`, matching its pre-wire structure. The single layer of panel chrome belongs to the component itself.
- **Why:** The double-chrome reads as visual noise on a public-facing unauthenticated page. The parent plan's Open Questions section names this exact descope path: *"If the answer is 'noise', descope U2/U3 (the speculative ones) and keep only U1."*

## Requirements

- R1. `login.html` no longer renders an outer `<cts-card>` around `<cts-login-page>`.
- R2. The `cts-card.js` module import is removed from `login.html` (no orphan import).
- R3. `frontend/e2e/login.spec.js` no longer asserts `cts-card` presence on the page (the assertion would fail after the revert).
- R4. All other login.html behavior remains intact: `cts-navbar`, `cts-toast-host`, the `?error=` and `?logout=` URL-param handlers, and the token-auth iframe flow.
- R5. Bookkeeping reflects reality: the board row notes the descope, the residual finding moves to "Resolved", and the parent plan's Open Questions section marks the login.html question closed.

## Scope Boundaries

**In scope:**
- `src/main/resources/static/login.html` — remove `<cts-card>` wrapper + `cts-card.js` import.
- `frontend/e2e/login.spec.js` — remove the `cts-card` visibility assertion + its surrounding comment.
- Bookkeeping updates to three docs (board, residual finding, parent plan).

**Out of scope:**
- `tokens.html` (U1) and `upload.html` (U3) — these stay shipped. The descope is U2-only.
- `cts-card.js` itself — no component changes. The `_initialized` guard added in `af20b683d` is correct and unchanged.
- `cts-card.stories.js` — no story changes (the component is unchanged).
- The 4 pre-existing OAuth-button selector failures on `login.spec.js:30+`. Tracked separately in `feedback_e2e_pre_existing_failures_2026_05_18.md`. Out of scope here.
- Touching files the parallel agent is working on. Coordinate via git only — no cross-file dependencies expected for this descope.

### Deferred to Follow-Up Work
- None.

## Key Technical Decisions

- **Two atomic commits, per CLAUDE.md "Git Operations"**: one for the code revert (login.html + login.spec.js), one for the docs updates (board + residual + parent plan). This keeps the code change reviewable in isolation and lets the docs commit serve as the audit trail.
- **Board row stays checked `[x]`**: U1 and U3 still shipped, so the orphan-component is wired. The descope of U2 is a narrowing of the wire-up surface, not an undo. We append a one-line descope note under the existing shipped note rather than unchecking the box.
- **Residual finding moves to a new "Resolved" section**: keep the original content (the diagnosis is the durable artifact), but reframe the status so future readers see at-a-glance that the P1 was acted on.
- **Parent plan's Open Questions section gets an inline resolution note**: the question stays in the document for context, but is marked resolved with a pointer to this plan and the descope commit.

## Implementation Units

### U1. Revert `<cts-card>` wrapper from `login.html`

- **Goal:** Restore login.html to its pre-wire structure: `<cts-login-page>` mounted directly in `<body>`, no outer card chrome, no `cts-card.js` import.
- **Requirements:** R1, R2, R4.
- **Dependencies:** None.
- **Files:**
  - `src/main/resources/static/login.html` (modify — 2 edits: remove import line 17, unwrap lines 26-28).
- **Approach:**
  - Remove `<script type="module" src="/components/cts-card.js"></script>` from the `<head>` imports (line 17).
  - Unwrap `<cts-card>…</cts-card>` so `<cts-login-page id="loginPage"></cts-login-page>` sits directly as a sibling of `<cts-navbar>` and `<footer>`.
  - Preserve all other markup: `cts-navbar`, the skip-link, the footer, the DOMContentLoaded script (error/logout/token URL-param handlers), and `cts-toast-host`.
- **Patterns to follow:** Mirror the structure of `tokens.html` / `upload.html` for the other static pages, but for U1/U3 the wrap **stays** — only login.html unwraps.
- **Test scenarios:** *(behavior verified by U2's e2e spec changes — see below)*
  - Page renders with `cts-login-page` directly under `cts-navbar`; no outer card frame in the rendered DOM.
  - Visual check: the login card no longer has an outer inset frame around it (compare against `docs/screenshots/Before/` baseline if present).
  - Manual click-through: the Google and GitLab OAuth buttons still render and route on click.
  - `?error=…` and `?logout=…` URL params still surface the appropriate alerts inside `cts-login-page`.
- **Verification:** Load `https://localhost.emobix.co.uk:8443/login.html` against `mvn spring-boot:run -Dspring-boot.run.profiles=dev`. No console errors. Screenshot to `tmp/screenshots/cts-card-login-descope-after.png` and diff against any pre-wire baseline in `docs/screenshots/Before/`.

### U2. Remove `cts-card` assertion from `login.spec.js`

- **Goal:** The e2e spec no longer requires `<cts-card>` to be visible on `login.html`. (Without this edit, the spec would fail after U1.)
- **Requirements:** R3.
- **Dependencies:** U1.
- **Files:**
  - `frontend/e2e/login.spec.js` (modify — remove the assertion at lines 24-27 plus its lead-in comment).
- **Approach:**
  - In the `renders cts-login-page with both OAuth cts-link-button elements` test, delete the comment block at lines 23-26 ("The page wraps cts-login-page in a headerless `<cts-card>`…") and the `await expect(page.locator("cts-card")).toBeVisible();` assertion at line 27.
  - Leave the rest of the test intact — the `cts-login-page` visibility and OAuth-button assertions still hold.
  - Do **not** touch the 4 pre-existing OAuth-button selector assertions on lines 30+ (`cts-link-button[href="…"]`). They are pre-existing failures tracked in user memory and out of scope here.
- **Patterns to follow:** None — this is a deletion, not a replacement. The `cts-toast-host` mount assertion on line 98 stays (cross-page contract, unrelated to the card wrap).
- **Test scenarios:**
  - The `renders cts-login-page…` test passes after U1 (no longer trips on the missing `<cts-card>`).
  - The other tests in the file (Bootstrap-asset, error-alert, logout-alert, navbar) are unchanged.
- **Verification:** `cd frontend && ./node_modules/.bin/playwright test e2e/login.spec.js`. The `cts-card` assertion no longer exists; pre-existing OAuth-button failures continue to fail (verified-pre-existing per `feedback_e2e_pre_existing_failures_2026_05_18.md`).

### U3. Commit the code revert

- **Goal:** One atomic commit captures U1 + U2.
- **Requirements:** R1, R2, R3, R4.
- **Dependencies:** U1, U2.
- **Files:** *(no file changes — this is the commit step)*
- **Approach:**
  - Stage only `src/main/resources/static/login.html` and `frontend/e2e/login.spec.js`. Do **not** `git add -A` (avoid sweeping in the parallel agent's work).
  - Commit message follows the repo's conventional-commit style. Suggested first line: `fix(cts-card): descope card wrapper from login.html (double-chrome)`. Body should reference the parent plan, the residual finding, and the Open Questions pre-authorization.
- **Test scenarios:** none — this is a commit step.
- **Verification:** `git log -1 --stat` shows exactly the two files modified.

### U4. Update bookkeeping (board + residual finding + parent plan)

- **Goal:** Three docs reflect that U2 of Row 4 was descoped, with the commit SHA from U3 cited.
- **Requirements:** R5.
- **Dependencies:** U3 (the commit SHA from U3 goes into the bookkeeping).
- **Files:**
  - `docs/plans/2026-05-18-006-orphan-components-wire-up-board.md` (modify — append a "Descoped U2 (login.html)" line under Row 4's existing shipped note; do **not** uncheck the box).
  - `docs/residual-review-findings/2026-05-19-cts-card-static-pages-af20b683d.md` (modify — move the P1 from "Residual actionable (1)" to a new "Resolved (1)" section, citing the descope commit SHA and date).
  - `docs/plans/2026-05-18-010-feat-wire-cts-card-into-static-pages-plan.md` (modify — under "Open Questions", mark the "Is the card-wrap actually a visual improvement, or just visual noise?" question as resolved with the descope decision and a pointer to this plan + the descope commit).
- **Approach:**
  - On the **board**: append a single line under Row 4's existing shipped note, format: `Descoped U2 (login.html): <commit-sha> on feat/redesign (2026-05-19) — reason: double-chrome with cts-login-page's built-in panel chrome.` The `[x]` box stays — U1 and U3 still shipped.
  - On the **residual finding**: rename the section header from `## Residual actionable (1)` to `## Resolved (1)` and add a short `**Resolution:**` line under the `[P1]` heading citing the U3 commit SHA, the date, and a pointer to this plan. Keep the original diagnosis prose — it's the durable record of *why* the descope happened.
  - On the **parent plan**: under the "Open Questions" bullet that starts with `**Is the card-wrap actually a visual improvement, or just visual noise?**`, append a sub-bullet or inline note: `**Resolved 2026-05-19:** Descoped U2 (login.html) per [`004-fix-descope-cts-card-from-login-plan.md`](2026-05-19-004-fix-descope-cts-card-from-login-plan.md); commit <sha>. U1 (tokens.html) and U3 (upload.html) stay.`
- **Test scenarios:** none — these are documentation updates.
- **Verification:** Re-read each of the three files and confirm: (a) board box still `[x]`, (b) residual finding's `## Resolved` section cites a commit SHA, (c) parent plan's Open Question is annotated with the resolution.

### U5. Commit the docs updates

- **Goal:** A second atomic commit captures U4.
- **Requirements:** R5.
- **Dependencies:** U4.
- **Files:** *(no file changes — this is the commit step)*
- **Approach:**
  - Stage only the three docs files from U4. Do **not** `git add -A`.
  - Commit message: `docs(cts-card): record descope of login.html wrap`. Body should reference the descope commit from U3 and the parent plan.
- **Test scenarios:** none — this is a commit step.
- **Verification:** `git log -2 --stat` shows U3 (code revert, 2 files) followed by U5 (docs, 3 files). Both commits authored on `feat/redesign`. **Do not push to main.**

## Open Questions

- None. The parent plan's Open Questions section pre-authorized this exact descope path.

## Coordination with the parallel agent

Another agent is working on other files in this repo on the same `feat/redesign` branch. Coordination notes:

- **No file overlap expected.** This plan touches only `login.html`, `login.spec.js`, the board, the residual finding, and the parent plan. None of these are likely candidates for a different stream of work.
- **Always `git add <specific-files>`**, never `git add -A` / `git add .`. Per CLAUDE.md Git Safety Protocol, this is already the default — reinforced here because of the parallel-agent context.
- **Pull before push.** If `git push` reports the remote has moved, `git pull --rebase` and re-run verification. Do **not** force-push.
- **If a conflict arises in any of the five files this plan touches**, stop and surface it — do not auto-resolve.

## Verification

- `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` succeeds (sanity check; no Java code touched, expected pass).
- `cd frontend && npm run test:ci` is green (format / lint / type-check / jsdoc / lit-analyzer). Pre-existing cts-toast.js TS2345 baseline applies per `feedback_cts_toast_typecheck_pre_existing.md` — verify pre-existing by `git stash` if it surfaces.
- `cd frontend && ./node_modules/.bin/playwright test e2e/login.spec.js` — the `renders cts-login-page…` test now passes after the assertion removal. Pre-existing OAuth-button failures continue to fail; verify pre-existing by stash if uncertain.
- Visual smoke: `https://localhost.emobix.co.uk:8443/login.html` against `spring-boot:run` profile `dev`. Screenshot saved to `tmp/screenshots/cts-card-login-descope-after.png`. Confirm: no outer card frame; the inner `cts-login-page` chrome (`.oidf-login-card`) is the only panel layer visible.
- Two commits on `feat/redesign`: code revert (2 files), then docs updates (3 files). Branch pushed to its existing upstream; PR/MR (if open) updates automatically.
