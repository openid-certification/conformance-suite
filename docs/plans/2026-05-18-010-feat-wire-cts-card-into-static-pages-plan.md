---
title: "feat: Wire cts-card into static-page content panels"
type: plan
status: active
created: 2026-05-18
branch: feat/redesign
depth: lightweight
brainstorm: docs/brainstorms/2026-04-25-oidf-design-system-implementation-requirements.md
board: docs/plans/2026-05-18-006-orphan-components-wire-up-board.md
---

# feat: Wire cts-card into static-page content panels

## Summary

`cts-card` (`src/main/resources/static/components/cts-card.js`, 110 lines) is a token-styled vanilla `HTMLElement` container: optional `header` attribute, optional `tone` brand-bar (`orange` | `rust` | `sand`). It's the design-system's basic content-panel primitive — the visual equivalent of a Bootstrap `.card` but token-driven.

This plan adopts `<cts-card>` as the surrounding container for content panels on three static pages whose body is currently a bare element or a Bootstrap-classed wrapper: `tokens.html`, `login.html`, and `upload.html`. The result: every static page that wraps a single composite component (e.g., `cts-token-manager`, `cts-login-page`, `cts-image-upload`) does so inside a consistent `<cts-card>` shell that ships with the brand-bar option and token-driven padding.

## Problem Frame

- **Today:** Static pages mount their composite component directly inside `<main>` with no surrounding panel. The page-level layout (margins, max-width, padding) lives in `oidf-app.css` or per-page `<style>` blocks.
- **Target:** Where the page is a single content panel (not a full-bleed dashboard), the composite component sits inside `<cts-card>` with appropriate `header` and/or `tone`.
- **Why:** The brainstorm's Phase B retokenization list explicitly includes `cts-card`, and Phase D mandates that legacy Bootstrap markup in HTML pages "has been replaced by component composition or token-based per-page CSS." Wrapping composite content in `<cts-card>` is one such composition.

⚠️ **Speculative target list.** Unlike plans 007–009, this plan is the most speculative: the brainstorm does not pin `cts-card` to specific pages, and the visual benefit of adding a card-shell to pages that look fine today is debatable. Treat this plan as a proposal — if Joseph/Thomas push back at MR time, hold or descope before merge.

## Requirements

- R1. `tokens.html` — wrap `<cts-token-manager>` in `<cts-card header="API Tokens">`.
- R2. `login.html` — wrap `<cts-login-page>` in `<cts-card>` (no header; the component owns its own heading).
- R3. `upload.html` — wrap `<cts-image-upload>` in `<cts-card>`, preserving the existing `<cts-page-head>` above the card.
- R4. The visual diff is "additive panel chrome" — no content disappears, no scrolling regressions, navbar and footer unchanged.
- R5. No console errors; the page's golden flow continues to work (e.g., token CRUD on `tokens.html`, image upload on `upload.html`, sign-in flow on `login.html`).

## Scope Boundaries

**In scope:**
- `src/main/resources/static/tokens.html`
- `src/main/resources/static/login.html`
- `src/main/resources/static/upload.html`
- Story confirmation for `cts-card` (the existing story should already cover header + tone variants; no extension expected).
- E2E spec selector updates if specs assert on specific DOM structure that the card insertion breaks.

**Out of scope:**
- `running-test.html`, `plan-detail.html`, `log-detail.html` — these already use composite components (`cts-running-test-card`, `cts-plan-header`, `cts-log-detail-header`) that carry their own panel chrome. Adopting `<cts-card>` there would be redundant.
- `index.html` — the dashboard is a full-bleed grid, not a single panel.
- `plans.html` / `logs.html` / `schedule-test.html` — multi-region pages where a single card wrap would not make sense.
- Brand-bar `tone` decisions per page. Default to no `tone`; the implementer may add one if the design archive specifies, but the absence of guidance means default-off is correct.

### Deferred to Follow-Up Work
- Adopting `<cts-card>` for sub-panels inside the multi-region pages (e.g., a card around the variant selector on `schedule-test.html`). That would be its own plan and depends on maintainer signal.

## Implementation Units

### U1. Wrap `tokens.html` content in `<cts-card>`

- **Goal:** `<cts-token-manager>` sits inside `<cts-card header="API Tokens">`.
- **Requirements:** R1, R4, R5.
- **Dependencies:** None.
- **Files:**
  - `src/main/resources/static/tokens.html` (modify)
- **Approach:**
  - Add `<script type="module" src="/components/cts-card.js"></script>` to the head imports.
  - Wrap the existing `<cts-token-manager></cts-token-manager>` in `<cts-card header="API Tokens">…</cts-card>`.
  - Verify the card's intrinsic padding does not collide with `cts-token-manager`'s own internal padding — if it does, add a one-line page-level `<style>` to neutralize one side.
- **Test scenarios:**
  - Page renders with a card around the token-manager; header text visible.
  - Create-token flow still works end-to-end.
  - Admin variant (read-only message) still renders.
- **Verification:** Manual click-through on `mvn spring-boot:run -Dspring-boot.run.profiles=dev`; no visual collisions.

### U2. Wrap `login.html` content in `<cts-card>`

- **Goal:** `<cts-login-page>` sits inside `<cts-card>` (headerless — the login page owns its heading and brand glyph).
- **Requirements:** R2, R4, R5.
- **Dependencies:** None (parallel with U1).
- **Files:**
  - `src/main/resources/static/login.html` (modify)
- **Approach:**
  - Add the `cts-card` script import.
  - Wrap `<cts-login-page id="loginPage"></cts-login-page>` in `<cts-card>…</cts-card>`.
  - Optional: constrain the card width via a page-level `<style>` if `cts-login-page` would otherwise stretch to full viewport width inside the card. Use `max-width: var(--maxw-form)` or equivalent token if available.
- **Test scenarios:**
  - Page renders with a card around the login form; the Google and GitLab sign-in buttons still work (just visually — no auth round-trip needed).
- **Verification:** Manual click-through; no console errors.

### U3. Wrap `upload.html` content in `<cts-card>`

- **Goal:** `<cts-image-upload>` sits inside `<cts-card>`, preserving the existing `<cts-page-head>` above the card.
- **Requirements:** R3, R4, R5.
- **Dependencies:** None (parallel with U1, U2).
- **Files:**
  - `src/main/resources/static/upload.html` (modify)
- **Approach:**
  - Add the `cts-card` script import.
  - Wrap `<cts-image-upload></cts-image-upload>` in `<cts-card>…</cts-card>`. Keep `<cts-page-head>` outside the card so the page heading stays as a page-level header pattern, not a card header.
- **Test scenarios:**
  - Page renders with the page-head above and a card below containing the image upload UI.
  - Uploading an image still works (drag-drop and file picker paths).
- **Verification:** Manual click-through; no console errors.

### U4. E2E spec adjustments (if needed)

- **Goal:** E2E selectors that assumed a specific DOM nesting still pass.
- **Requirements:** R5.
- **Dependencies:** U1, U2, U3.
- **Files:**
  - `frontend/e2e/tokens.spec.js` (if exists)
  - `frontend/e2e/login.spec.js` (if exists)
  - `frontend/e2e/upload.spec.js` (if exists)
- **Approach:**
  - Run the suite. Adjust only the selectors that break. `cts-card` is Light DOM, so most `getByRole` / `getByText` queries pass through unchanged.
- **Test scenarios:** Existing E2E test cases continue to pass; no new tests added.
- **Verification:** `cd frontend && npm run test:e2e` is green.

## Open Questions

- **Is the card-wrap actually a visual improvement, or just visual noise?** This is the question Joseph/Thomas may push back on. If the answer is "noise", descope U2/U3 (the speculative ones) and keep only U1 (`tokens.html`, the clearest win because the page is otherwise a bare table).
  - **Resolved 2026-05-19:** Descoped U2 (`login.html`) per [`2026-05-19-004-fix-descope-cts-card-from-login-plan.md`](2026-05-19-004-fix-descope-cts-card-from-login-plan.md); commit `c5964b1b2`. `cts-login-page` already paints full panel chrome on its inner `.oidf-login-card`, so the wrap produced visible card-inside-card. U1 (`tokens.html`) and U3 (`upload.html`) stay — their composite children have no outer chrome, so the card is genuinely additive panel chrome. Residual finding moved to "Resolved" at [`docs/residual-review-findings/2026-05-19-cts-card-static-pages-af20b683d.md`](../residual-review-findings/2026-05-19-cts-card-static-pages-af20b683d.md).
- **Brand-bar `tone`.** The brainstorm does not specify when to use orange/rust/sand. Default off across all three pages until the design archive provides guidance.
- **Page-head vs. card-header dual.** On `upload.html` the plan keeps `<cts-page-head>` outside the card. If the design archive eventually prescribes "use card-header instead of page-head when the page is a single panel", revisit this in a follow-up.

## Verification

- `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` succeeds.
- `cd frontend && npm run test:ci` is green.
- `cd frontend && npm run test:e2e` is green.
- Manual click-through of `/tokens.html`, `/login.html`, `/upload.html` against `mvn spring-boot:run -Dspring-boot.run.profiles=dev` confirms the visual addition reads as polish, not regression.
- The board file row 4 is checked with the MR number.
