---
title: "feat: Polish tokens.html page layout"
type: feat
status: completed
date: 2026-06-04
---

# feat: Polish tokens.html page layout

## Summary

Align the API tokens page with the redesign branch's no-card page pattern: drop the `cts-card` wrapper and the component's own padding, center the content in a narrower page wrapper, shrink the header action buttons (API Documentation becomes the tertiary/ghost variant), render Token ID cells in monospace, and move the Copy button after the token value in the created-token modal.

## Problem Frame

`tokens.html` is the last listing-style page still wrapped in a `cts-card` with component-level padding, full-width `<main>`, and `size="lg"` header buttons. Every other migrated page (plans, logs, upload, running-test) uses a centered page wrapper plus `cts-page-head`/tabs with content directly in the main flow. The token table is three narrow columns and doesn't need `.listing-page`'s 1600px; the created-token modal also reads awkwardly with the Copy button before the token it copies.

---

## Requirements

- R1. `.cts-token-manager` no longer applies its own padding; page spacing comes from the page wrapper.
- R2. The page content is not wrapped in a card; the "API Tokens" heading remains visible as a proper page title (`<h1>`).
- R3. `<main>` uses a centered page wrapper narrower than `.listing-page` (1600px), routed through the `--maxw-page` token (1200px).
- R4. The three header action buttons render smaller than `lg`; the API Documentation link renders as the tertiary (ghost) variant.
- R5. Token ID column values render in monospace.
- R6. In the created-token modal, the Copy button displays after the token value (`.cts-token-manager-token-value.created-token-value`).
- R7. Existing Playwright e2e assertions and Storybook play tests are updated in lockstep; all frontend suites pass.

---

## Assumptions

- "Tertiary" maps to `variant="ghost"`. `cts-button.js` defines `primary | secondary | ghost | danger` only; an unknown `variant="tertiary"` would silently fall back to `secondary` (no error), and the codebase documents ghost as the tertiary rung (`cts-log-entry.stories.js` cURL affordance).
- Removing the card must not remove the page title: the card header is currently the only "API Tokens" heading. Replace it with `<cts-page-head title="API Tokens">`, the established Phase D no-card header (already used by `running-test.html` and `upload.html`; a `cts-page-head:not(:defined)` FOUC reservation already exists in `css/layout.css`).
- "Smaller buttons" means the `md` rung (default `--control-height`, no size modifier class) rather than `sm` (30px, used for in-table row actions). `md` is the design-archive default for page-level controls.
- A new generic narrow wrapper class is added rather than reusing `.schedule-test-page` or `upload.html`'s page-local `.upload-page` (see KTD below).

---

## Key Technical Decisions

- **New `.narrow-page` wrapper class at `var(--maxw-page)` (1200px), not `.schedule-test-page` or `.upload-page` reuse:** two sub-1600px wrappers exist today. `.schedule-test-page` (`css/oidf-app.css`, 1100px literal) carries `padding-bottom: calc(var(--cts-action-bar-height, 80px) + var(--space-5))` to clear a sticky action bar tokens.html doesn't have (~100px phantom whitespace), plus page-specific `> h2`/`h3` child rules and a misleading name. `.upload-page` (inline `<style>` in `upload.html`) is page-local but already routes through the `--maxw-page` design token (1200px, `css/oidf-tokens.css`). Add `.narrow-page { max-width: var(--maxw-page); margin: 0 auto; padding: var(--space-6) var(--space-5); }` beside `.listing-page` — token-routed rather than a second hard-coded 1100px literal, and consistent with the closest precedent (`upload.html`, also a `cts-page-head` page). Refactoring `.schedule-test-page` and `upload.html` to compose `.narrow-page` is deferred (see Scope Boundaries).
- **API Documentation button uses `variant="ghost"`:** there is no `tertiary` variant key; ghost is the codebase's tertiary. Using the literal string `tertiary` would silently render as `secondary`.
- **Header buttons drop from `size="lg"` to `size="md"`:** `md` renders no size modifier class (height from base `.oidf-btn` rule = `--control-height`). Set the attribute explicitly rather than omitting it — the component's JS default is `sm`, which is the table-row rung, not the page-action rung.
- **Token ID monospace via the data-table's built-in column mechanism:** add `mono: true` to the `_id` column descriptor in `_columns()`; `cts-data-table` applies `.oidf-dt-cell-mono` (`--font-mono` at `--fs-12`). No custom CSS needed.
- **Copy-row reorder stays inside the stable modal-body wrapper:** `cts-modal` captures its top-level children once on connect, so the reorder must happen within `.cts-token-manager-created-modal-body` (it does — all affected nodes live inside that one div). The `_copyFeedback` span must remain always-rendered with only its text varying, per the existing comment in `_renderModals()`.
- **Reordered modal-body layout: full-width token block, then a copy row beneath it:** the token `<pre>` renders as a full-width block (it wraps long tokens via `pre-wrap`/`break-all`), followed by `.cts-token-manager-copy-row` holding only the Copy button and the feedback span. Do not co-mingle the `<pre>` inside the existing `align-items: center` flex row — a multi-line `<pre>` in that row would shrink-wrap into a narrow column with the button floating vertically centered beside it, and the feedback message would detach from the button it confirms. Keeping button + feedback in their own row preserves their adjacency (the `aria-live` message stays next to its trigger).

---

## Implementation Units

### U1. Adopt the no-card narrow page layout on tokens.html

- **Goal:** Remove the card wrapper and component padding; center content in a new narrow page wrapper with a proper page heading.
- **Requirements:** R1, R2, R3, R7
- **Dependencies:** none
- **Files:**
  - `src/main/resources/static/tokens.html` — replace `<cts-card header="API Tokens">` with `<cts-page-head title="API Tokens">` + bare `<cts-token-manager>`; add the narrow wrapper class to `<main id="main-content">`; in the `<head>`, remove `<script type="module" src="/components/cts-card.js">` and add `<script type="module" src="/components/cts-page-head.js">` (without the import the element never upgrades and the page title never renders; without the removal a dangling module import remains).
  - `src/main/resources/static/css/oidf-app.css` — add `.narrow-page` beside `.listing-page` with a comment mirroring the existing wrapper comments.
  - `src/main/resources/static/components/cts-token-manager.js` — remove the `padding` declaration from `.cts-token-manager` in `STYLE_TEXT` (keep `display: block`, font, color).
  - `frontend/e2e/tokens.spec.js` — rewrite the card assertions (`cts-card[header="API Tokens"]`, `.oidf-card-header`) to assert the new structure instead.
- **Approach:** Mirror `plans.html`/`logs.html` (`<main class="...">` wrapper, content directly in flow) and `upload.html`/`running-test.html` (`cts-page-head` for the title). The admin view (`_renderAdminView`) shares `.cts-token-manager`, so the padding removal covers both branches. No new FOUC reservation is needed — `cts-page-head:not(:defined)` and `cts-data-table:not(:defined)` rules already exist in `css/layout.css`.
- **Patterns to follow:** `src/main/resources/static/plans.html` (main wrapper class), `src/main/resources/static/upload.html` (cts-page-head usage), `css/oidf-app.css` wrapper comment style.
- **Test scenarios:**
  - tokens.html renders no `cts-card`; "API Tokens" renders as the `cts-page-head` `<h1>` title.
  - `<main id="main-content">` carries the narrow wrapper class.
  - Token table still renders the fixture rows inside `cts-data-table#tokensListing` (regression guard, existing assertions).
  - The single `cts-toast-host` contract assertion still passes (untouched).
- **Verification:** `frontend/e2e/tokens.spec.js` passes; visual check shows the table centered at `--maxw-page` (1200px) with no card chrome and no double padding.

### U2. Polish token-manager buttons, Token ID column, and copy row

- **Goal:** Smaller header actions with ghost API-docs link, monospace Token ID cells, Copy button after the token value.
- **Requirements:** R4, R5, R6, R7
- **Dependencies:** U1 (e2e spec edits touch the same file; land after the layout settles)
- **Files:**
  - `src/main/resources/static/components/cts-token-manager.js` — `_renderCreateButtons()`: `size="lg"` → `size="md"` on all three controls, `variant="secondary"` → `variant="ghost"` on the API Documentation `cts-link-button`; `_columns()`: add `mono: true` to the `_id` descriptor; `_renderModals()`: reorder the created-token modal body to explanation `<p>`, then the full-width token `<pre>`, then `.cts-token-manager-copy-row` holding the Copy button and the always-rendered feedback span (per the modal-body layout KTD — the `<pre>` stays out of the flex row); adjust `.cts-token-manager-copy-row`/`.cts-token-manager-token-value` CSS in `STYLE_TEXT` as needed.
  - `frontend/e2e/tokens.spec.js` — update the "three header action buttons" test (size `md`, ghost variant on the docs link); add a Token ID mono-class assertion and a copy-button-after-token DOM-order assertion.
  - `src/main/resources/static/components/cts-token-manager.stories.js` — update any play assertions broken by the reorder (the Copy button is located by `getByTitle`, which is order-independent, so existing stories likely stay green without proving the new order). Add the DOM-order assertion to the existing `CreateTemporaryToken` story's play function: after the modal opens, assert the `.created-token-value` `<pre>` precedes the Copy button (e.g., `pre.compareDocumentPosition(copyButton) & Node.DOCUMENT_POSITION_FOLLOWING`).
- **Approach:** Variants/sizes set only via host attributes (never classes) per the light-DOM cts-button contract. The reorder is a Lit template change inside the single captured modal-body div — safe under cts-modal's child-capture rule; keep the feedback span always rendered. Layout follows the modal-body KTD: full-width `<pre>`, then the button + feedback row. The behavioral contract is DOM order (token value before Copy button) and unchanged copy/feedback behavior.
- **Patterns to follow:** ghost usage in `src/main/resources/static/components/cts-log-entry.js` (cURL copy button); `mono: true` column documentation in `src/main/resources/static/components/cts-data-table.js`.
- **Test scenarios:**
  - All three header controls have `size="md"` and inner elements render the base `.oidf-btn` surface; the two create buttons keep `variant="primary"`.
  - The API Documentation link has `variant="ghost"` and its inner `<a>` carries `.oidf-btn-ghost`.
  - Token ID cells (`[data-column-key="_id"]` / first column `<td>`) carry `.oidf-dt-cell-mono`.
  - After creating a token, `.created-token-value` precedes the Copy button in DOM order, and clicking Copy still writes the token to the clipboard and shows the feedback message (existing story flow).
  - Edge: admin view renders no actions row (existing test, unaffected); empty token list message unaffected.
- **Verification:** `frontend/e2e/tokens.spec.js` and the cts-token-manager Storybook suite pass; `npm run test:ci` from `frontend/` is green.

---

## Scope Boundaries

- **Deferred to Follow-Up Work:**
  - Refactor `schedule-test.html` / `.schedule-test-page` (1100px literal) and `upload.html` / `.upload-page` (page-local inline style) to compose the new `.narrow-page` class instead of duplicating centered-wrapper geometry.
- **Non-goals:** backend changes; other pages' layouts; redesigning the token table itself; changing copy/clipboard behavior.

---

## Sources & Research

- `src/main/resources/static/components/cts-button.js` — `VARIANT_CLASSES` (no `tertiary`; unknown → `secondary`), `SIZE_CLASSES` (`md` = no modifier), defaults `variant="secondary"` / `size="sm"`.
- `src/main/resources/static/css/oidf-app.css` — `.listing-page` (1600px) and `.schedule-test-page` (1100px + action-bar padding); `upload.html` carries a page-local `.upload-page` at `var(--maxw-page)` (1200px, `css/oidf-tokens.css`).
- `src/main/resources/static/components/cts-data-table.js` — column `mono: true` / `format: "mono"` → `.oidf-dt-cell-mono`.
- `frontend/e2e/tokens.spec.js` — card-wrapper and `size="lg"` assertions written deliberately to fail on this change; must be updated, not deleted.
- `docs/solutions/web-components/cts-button-host-vs-inner-button-semantics-2026-04-17.md` — set variant/size via host attributes only.
- `docs/solutions/web-components/cts-modal-bootstrap-interop-2026-04-17.md` — modal children captured once; mutate inner template content only.
- `docs/solutions/test-failures/playwright-e2e-flaky-after-web-component-merge-2026-04-14.md` — update structural selectors in lockstep; suspect stale dev server (`reuseExistingServer`) before suspecting the change.
