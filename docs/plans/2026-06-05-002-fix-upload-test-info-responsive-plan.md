---
title: "fix: Make the image-uploader test-info card responsive"
type: fix
status: active
date: 2026-06-05
---

# fix: Make the image-uploader test-info card responsive

## Summary

On narrow viewports the image-uploader page's test-info card (`#testInfo` on `src/main/resources/static/upload.html`) breaks: the "Return to test log" button's `auto` grid column squeezes the label/value meta to a sliver, the Summary JSON payload wraps one word per line, and long unbroken tokens (base64 subject IDs) overflow the viewport horizontally. This plan moves the return link above the card (per the user's request), collapses the card to a single-purpose meta grid that stacks at the phone breakpoint, replaces `word-break: break-all` with the redesign's canonical `overflow-wrap: anywhere` pattern, and adds e2e regression coverage for link position and mobile overflow.

## Requirements

- R1. At a 375px-wide viewport, the test-info card and page render without horizontal overflow (`scrollWidth <= clientWidth`), including when the summary or test ID contains a long unbroken token.
- R2. The "Return to test log" link renders before (above) the test-info card in DOM order and navigates to `log-detail.html?log=<testId>`. It renders as soon as the page loads, without waiting for the `/api/info/` response.
- R3. Short words and hyphenated names (e.g., `authzen-pdp-evaluation-01`) wrap at natural break points; mid-word breaks occur only when a token cannot otherwise fit.
- R4. At desktop widths the meta keeps the two-column `label | value` grid; at or below 640px labels stack above their values in a single column.
- R5. The existing `frontend/e2e/upload.spec.js` suite passes, with new assertions covering R1 and R2.

## Key Technical Decisions

- **Return link becomes static markup above `#testInfo`**, with `href` populated from the `log` URL query param at `DOMContentLoaded` (the same value already passed to `uploader.testId`): the link appears immediately instead of after the `/api/info/` round-trip, and survives an API failure. When the `log` param is absent or empty, set `disabled` on the link instead of an empty href — `cts-link-button` drops the `href` when `disabled`, so the user never navigates to `log-detail.html?log=`. The `actionWrapper` branch in `renderTestInfo()` is deleted. Drop `full-width` — outside the card's `auto` track, `full-width` would stretch the link across the whole page column; let it size to its label. Switch `icon="bookmark"` to `icon="arrow-left-md"` (verified vendored in `vendor/coolicons/icons/`) to read as back-navigation.
- **Collapse the card's outer two-column grid** (`minmax(0, 1fr) auto`): with the action gone, the card has one child and the squeeze disappears at every width. The meta grid (`max-content minmax(0, 1fr)`) stays for desktop.
- **Page-level `@media (max-width: 640px)`** flips the meta grid to a single column (labels stack above values). 640px is the repo's canonical phone breakpoint (`css/oidf-app.css` `.cascade-row`); `@container` queries are the convention for `cts-*` components, not page-level CSS, and this card lives in the page's inline `<style>` block.
- **`overflow-wrap: anywhere` replaces `word-break: break-all`** on values, and is added (with `min-width: 0` on the grid track context) to the summary box, which currently has no wrapping rule at all — the direct cause of the horizontal overflow. This is the redesign's documented pattern (rationale comment in `components/cts-log-entry.js`, ~lines 380–388); `break-all` is what the redesign moved away from. Note: this intentionally diverges from `cts-plan-header`'s own `dd { word-break: break-word }` — when mirroring `.planMeta`, do not copy that property; use `overflow-wrap: anywhere`.
- **Align the meta markup and summary recipe with `cts-plan-header`**, the established near-twin: `dl/dt/dd` for label/value pairs (`.planMeta`, `components/cts-plan-header.js` ~lines 33–54) and the `.planSummary` info-box recipe (dark text + `--status-info` left border on `--status-info-bg`) instead of the current low-contrast `color: var(--status-info)` blue-on-blue.

## Scope Boundaries

- No shared component extraction and no relocation of this page's CSS into `css/oidf-app.css` — the card stays page-local inline `<style>`, matching the page's current structure.
- No `cts-*` component changes, so no Storybook story work.
- **Deferred to follow-up work:** adopting the `cts-crumb` breadcrumb pattern (the redesign's prevailing back-navigation idiom on `log-detail.html` / `plan-detail.html`) — the user asked for the existing link moved above the card, so this plan repositions `cts-link-button` rather than swapping affordances.

## Implementation Units

### U1. Restructure the upload.html test-info card

- **Goal:** Return link above the card; single-column card with responsive meta grid; no horizontal overflow with long tokens.
- **Requirements:** R1, R2, R3, R4
- **Dependencies:** none
- **Files:** `src/main/resources/static/upload.html`
- **Approach:**
  - Add the `cts-link-button` (no `full-width`, `icon="arrow-left-md"`, `label="Return to test log"`) to the static markup between `cts-page-head` and `#testInfo`; set its `href` in the `DOMContentLoaded` handler from the `log` URL param, or set `disabled` when the param is missing/empty. Delete the `actionWrapper` block from `renderTestInfo()`. The `cts-link-button.js` module import already exists on the page.
  - In `renderTestInfo()`, build the meta as `dl/dt/dd` (mirroring `cts-plan-header`'s `.planMeta`); keep the "summary row only when `test.summary` is present" behavior.
  - CSS: remove the outer `minmax(0, 1fr) auto` grid from `.upload-test-info`; on values use `overflow-wrap: anywhere` instead of `word-break: break-all`; give `.upload-test-info__summary` `overflow-wrap: anywhere` plus the `.planSummary` recipe (left border, default ink text), and span it across the full card width (`grid-column: 1 / -1`) so the info-box matches `.planSummary`'s full-width presentation instead of sitting in the value column; add `@media (max-width: 640px)` flipping the meta grid to `grid-template-columns: 1fr`. Keep `min-width: 0` on the value track context so grid items can shrink below content size.
- **Patterns to follow:** `components/cts-plan-header.js` (`.planMeta` dl grid, `.planSummary` recipe); `components/cts-log-entry.js` overflow-wrap rationale; `components/cts-plan-modules.js` (~line 181) for the action-column collapse idiom if any two-column arrangement survives.
- **Test scenarios:** behavioral coverage lands in U2's e2e spec (same change set, split for reviewability). Visual spot-check at 375px / 640px / 1024px widths against a long-token summary.
- **Verification:** at 375px no horizontal scrollbar appears with the screenshot's payload (long base64 `id` token); the return link is visible above the card immediately on load; desktop keeps `label | value` columns; labels stack below 640px.

### U2. E2E regression coverage for link position and mobile overflow

- **Goal:** Lock in R1 and R2 so the squeeze/overflow can't silently regress.
- **Requirements:** R1, R2, R5
- **Dependencies:** U1
- **Files:** `frontend/e2e/upload.spec.js`, `frontend/e2e/fixtures/upload-data.js`
- **Approach:** Follow the existing spec's route-ordering convention (`setupFailFast` first, all routes before `page.goto`). Model the overflow assertion on `frontend/e2e/log-detail.spec.js` (~lines 955–987): `page.setViewportSize({ width: 375, height: 800 })` then assert `scrollWidth <= clientWidth`. Add a named fixture export `MOCK_UPLOAD_TEST_INFO_LONG_TOKEN` to `frontend/e2e/fixtures/upload-data.js` whose `summary` contains a long unbroken ~80-char token mirroring the real-world base64 subject ID; import it in the spec and serve it via the long-token test's `/api/info/` route override.
- **Patterns to follow:** `frontend/e2e/log-detail.spec.js` viewport-overflow test; scope locators to a container (`main` / `#testInfo`) to avoid strict-mode duplicate matches against the navbar (per `docs/solutions/test-failures/playwright-e2e-flaky-after-web-component-merge-2026-04-14.md`).
- **Test scenarios:**
  - Happy path: the return link is present inside `main`, precedes `#testInfo` in DOM order, and its inner anchor `href` ends with `log-detail.html?log=test-upload-001`.
  - Happy path: existing assertions (test name, test ID, summary text render from `/api/info/`) still pass against the `dl/dt/dd` markup.
  - Edge case: at 375px viewport with a long-token summary fixture, `#testInfo` and `document.documentElement` report `scrollWidth <= clientWidth`.
  - Edge case: the return link renders with a populated `href` even when `/api/info/` returns an error (link no longer depends on the API response).
  - Edge case: with no `log` URL param, the return link renders `disabled` (inner anchor exposes no `href`) rather than linking to `log-detail.html?log=`.
- **Verification:** `npm run test:e2e` passes from `frontend/`; the new overflow test fails if `overflow-wrap: anywhere` is removed from the summary (sanity-check the test bites before finalizing).

## Sources

- `src/main/resources/static/upload.html` — current card CSS/DOM (outer grid ~lines 39–43, `word-break` ~line 56, `renderTestInfo()` ~lines 192–220).
- `components/cts-plan-header.js` — `.planMeta` / `.planSummary`, the convention model.
- `components/cts-log-entry.js` — `overflow-wrap: anywhere` + `min-width: 0` rationale comment.
- `frontend/e2e/log-detail.spec.js` — 375px viewport overflow-assertion pattern.
- `docs/solutions/web-components/cts-button-host-vs-inner-button-semantics-2026-04-17.md` — assert on the inner anchor, not the host element.
