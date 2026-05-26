---
type: plan
status: active
title: "fix: Restore real href on cts-plan-list plan-name links for a11y"
created: 2026-05-26
plan_type: fix
depth: lightweight
---

# fix: Restore real href on cts-plan-list plan-name links for a11y

## Summary

The plan-name links in `<cts-plan-list>` (rendered on `plans.html`) currently use `href="#"` and rely on a JavaScript click handler that calls `event.preventDefault()` and dispatches a `cts-plan-navigate` custom event. The plans.html consumer then sets `window.location.href = 'plan-detail.html?plan=...'` to perform the actual navigation.

This pattern breaks fundamental accessibility and UX affordances of links:

- **Screen readers** announce only "link, #" with no useful destination — assistive technologies cannot preview where the link goes.
- **Hover preview** in browsers shows `#` in the status bar instead of the real destination.
- **Right-click → "Open in new tab"** opens the current page with `#fragment` — broken.
- **Cmd-click / Ctrl-click** (macOS / Windows) — open in new tab — broken; opens current page anchor instead.
- **Middle-click** — open in new tab — broken.
- **No-JS users / `nomodule` browsers** — links go nowhere at all.

The fix: set the real `plan-detail.html?plan=<encoded id>` URL on the anchor's `href`, and update the click handler to only intercept plain left-clicks (no modifier keys). For modifier-key clicks (cmd/ctrl/shift/alt) and non-primary mouse buttons (middle, right), the browser handles navigation natively, restoring the affordances users and assistive technologies expect.

The existing `cts-plan-navigate` event contract is preserved for plain left-clicks, so the plans.html consumer and the storybook `ClickPlanName` test continue to work unchanged.

---

## Problem Frame

`src/main/resources/static/components/cts-plan-list.js:319-327` renders the plan-name cell as:

```js
return html`<a
  href="#"
  class="plan-name-link"
  data-plan-id="${row._id}"
  @click=${this._handlePlanLinkClick}
  >${row.planName}</a
>`;
```

with the click handler at `src/main/resources/static/components/cts-plan-list.js:228-232`:

```js
_handlePlanLinkClick(event) {
  event.preventDefault();
  const planId = event.currentTarget.dataset.planId;
  this._handlePlanClick(planId);
}
```

The actual navigation target is `plan-detail.html?plan=<planId>` — resolved by the page-level event listener in `src/main/resources/static/plans.html:78-82`:

```js
planList.addEventListener('cts-plan-navigate', (evt) => {
  const planId = evt.detail && evt.detail.planId;
  if (!planId) return;
  window.location.href = 'plan-detail.html?plan=' + encodeURIComponent(planId);
});
```

The destination is fully determinable at render time from `row._id`. There is no reason for the anchor to claim it points to `#`.

---

## Scope Boundaries

### In scope

- `<cts-plan-list>` plan-name link: set real `href`, preserve `cts-plan-navigate` event for plain clicks, let browser handle modifier-key/non-primary-button clicks natively.
- Storybook story coverage: add an assertion that the `href` is the real destination, not `#`.
- E2E coverage: add an assertion to the existing plans.spec.js navigation test that the rendered `href` is the real destination.

### Out of scope (Deferred to Follow-Up Work)

- The story files in `cts-link-button.stories.js`, `cts-alert.stories.js`, and `cts-page-head.stories.js` use `href="#"` — these are intentionally placeholder demo content for component galleries, not user-facing app links. They render in storybook only, never on the real app surface. Leave as-is unless explicitly requested.
- `cts-plan-modules.js`, `cts-plan-detail.js`, and any other places that may render placeholder anchors — not in scope for this targeted fix; can be audited in a follow-up if a broader sweep is requested.
- Consolidating the "page consumer routes the custom event into `window.location.href`" pattern into a default behavior in the component itself — would change the API contract for all consumers; out of scope here. The current fix keeps the consumer contract intact and only repairs the href + click semantics.

---

## Requirements

| ID | Requirement |
|----|-------------|
| R1 | The plan-name `<a>` element's `href` attribute MUST be set to `plan-detail.html?plan=<encodeURIComponent(row._id)>` (the same URL the page-level consumer navigates to). |
| R2 | A plain left-click (no modifier keys, primary button) MUST continue to fire the `cts-plan-navigate` event with `{ detail: { planId } }` and MUST call `event.preventDefault()` so the consumer's `window.location.href = ...` is the single source of navigation for plain clicks. |
| R3 | A modifier-key click (cmd, ctrl, shift, alt) or non-primary-button click (middle, right) MUST NOT call `preventDefault()` and MUST NOT dispatch the custom event — the browser MUST be allowed to handle these natively (open in new tab/window). |
| R4 | Storybook `ClickPlanName` story MUST continue to pass — the simulated `userEvent.click` is a plain left-click with no modifiers, so the existing event-based assertion still holds. |
| R5 | The existing plans.spec.js navigation test (`clicking a plan name navigates to plan-detail.html`) MUST continue to pass. |
| R6 | The rendered `href` attribute on the anchor MUST be asserted by a new storybook play function so future regressions to `href="#"` are caught at test time. |

---

## Key Technical Decisions

### Use a `(planId) => string` URL builder, inlined at render time

The destination URL `plan-detail.html?plan=<encoded id>` is short, owned by this component, and not shared with any other surface (the only other place it appears is plans.html line 81, which constructs the same string). Inline a small helper or just use a template literal in `_cellRenderer` — no need for a separate exported constant or builder function.

### Modifier-key escape hatch is the standard SPA-link pattern

```js
_handlePlanLinkClick(event) {
  // Let the browser handle modifier-key clicks (open in new tab/window)
  // and non-primary-button clicks natively. This is the standard
  // progressive-enhancement pattern for SPA-style links — without it,
  // cmd-click and middle-click would open a new tab AND fire our
  // intercept, causing the current tab to navigate too.
  if (
    event.metaKey ||
    event.ctrlKey ||
    event.shiftKey ||
    event.altKey ||
    event.button !== 0
  ) {
    return;
  }
  event.preventDefault();
  const planId = event.currentTarget.dataset.planId;
  this._handlePlanClick(planId);
}
```

This is well-known prior art in SPA frameworks (React Router's `<Link>`, Next.js `<Link>`, Vue Router's `<router-link>` all implement the same modifier-key escape hatch).

### Preserve the custom event contract, do not remove it

CTS is a multi-page server-rendered application — clicking a plan name does a hard navigation to a different `.html` file, not an SPA route transition. In principle the component could just render a plain `<a href>` with no JS handler at all, and the browser would handle every click natively.

However, the storybook `ClickPlanName` story (`src/main/resources/static/components/cts-plan-list.stories.js:130-154`) depends on the `cts-plan-navigate` event firing on `userEvent.click` so it can assert the planId. Removing the event would force the storybook test to either (a) actually navigate the iframe (which fails because `plan-detail.html` is not served in storybook), or (b) be rewritten to intercept the click some other way.

Keep the existing event contract. The only change is the `href` value and the modifier-key escape hatch.

---

## Implementation Units

### U1. Set real href and add modifier-key escape hatch in cts-plan-list

**Goal:** Replace `href="#"` with the real destination URL, and update `_handlePlanLinkClick` to only intercept plain left-clicks.

**Requirements:** R1, R2, R3, R4, R5

**Dependencies:** none

**Files:**
- `src/main/resources/static/components/cts-plan-list.js` — modify `_cellRenderer` (planName branch) and `_handlePlanLinkClick`.

**Approach:**

1. In `_cellRenderer`, change the `href="#"` to `href="plan-detail.html?plan=${encodeURIComponent(row._id)}"`.
2. In `_handlePlanLinkClick`, add an early return that detects modifier keys (`metaKey`, `ctrlKey`, `shiftKey`, `altKey`) and non-primary buttons (`event.button !== 0`), and bails out without preventDefault / event dispatch so the browser navigates natively.
3. Add a brief comment above the early return explaining the progressive-enhancement intent — this is the kind of "why" that's non-obvious from reading the code (future maintainers will wonder why some clicks dispatch and others don't).

**Patterns to follow:**
- The same modifier-key escape hatch idiom appears in major SPA-link components (React Router, Next.js, Vue Router). The shape is standard, no novel decisions.
- Use `encodeURIComponent` for the plan ID — matches what `plans.html:81` already does for the runtime URL construction; consistent encoding on both sides.

**Test scenarios:**
- Storybook `ClickPlanName` (existing, in `cts-plan-list.stories.js`): plain `userEvent.click` on a plan name MUST still cause the `cts-plan-navigate` event to fire with the correct planId. Covered by R4.
- Storybook (new assertion, in the `Default` story or a dedicated `PlanNameHrefIsRealUrl` story): after the table renders, query `a.plan-name-link` and assert `getAttribute('href') === 'plan-detail.html?plan=plan-001'` (or whichever fixture row is first). Covers R1, R6.
- E2E (existing, in `frontend/e2e/plans.spec.js` `clicking a plan name navigates to plan-detail.html`): plain click MUST still result in `plan-detail.html?plan=plan-001` in the URL. Already covered by R5; no change needed.
- E2E (optional, in the same e2e test): before clicking, assert `await expect(planLink).toHaveAttribute('href', 'plan-detail.html?plan=plan-001')` so a regression to `href="#"` is caught at e2e level too. Strengthens R6.

**Verification:**
- `npm run test:ci` (from `frontend/`) passes — lint, type-check, codegen check, lit-analyzer all clean.
- Storybook tests for `cts-plan-list` pass: `Default`, `Search`, `ClickPlanName`, `ViewConfig`, `ConfigButtonHiddenWhenConfigIsEmpty`, `EmptyList`, `LoadingState`, `ApiError`, `AdminView`, `PublicView`, `TableHasNoBootstrapClasses`.
- E2E tests for `plans.spec.js` pass — specifically `clicking a plan name navigates to plan-detail.html`.
- Manual verification on https://localhost.emobix.co.uk:8443/plans.html: hover a plan-name link, see `plan-detail.html?plan=...` in the browser status bar. Cmd-click on a plan name, see a new tab open at the real destination. Right-click → "Open in new tab" works.

---

### U2. Add storybook play assertion that href is a real URL

**Goal:** Catch any future regression where the `href` drifts back to `#` (or any other non-destination value).

**Requirements:** R6

**Dependencies:** U1

**Files:**
- `src/main/resources/static/components/cts-plan-list.stories.js` — extend the existing `Default` story's play function, or add a new dedicated story.

**Approach:**

Prefer extending `Default`'s play function with one additional assertion — keeps the story inventory small and avoids a story whose only purpose is asserting a single attribute. After the `expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument()` line, add:

```js
const firstPlanLink = canvasElement.querySelector('a.plan-name-link');
expect(firstPlanLink?.getAttribute('href')).toBe('plan-detail.html?plan=plan-001');
```

This is one assertion line, covers the regression case, and keeps the story focused on overall rendering. The fixture's first row has `_id: "plan-001"` (verified by the existing `ClickPlanName` story which asserts `expect(receivedPlanId).toBe('plan-001')`).

**Patterns to follow:**
- Existing storybook play assertions in this file use `canvasElement.querySelector(...)` for direct DOM access and `expect(...).toBe(...)` for value assertions. Mirror that style.

**Test scenarios:**
- The assertion runs as part of the `Default` story's play function. If the href ever changes (e.g., back to `#`, or to a malformed value), the storybook test fails with a clear diff.

**Verification:**
- Storybook test runner: `npm run test:storybook` (or `run-story-tests`) passes for `Pages/cts-plan-list` `Default`.
- Pre-flight check: temporarily revert U1's href change to `href="#"` and confirm the new assertion fails — confirms the test would catch a regression.

---

## Risks

- **None of substance.** This is a 5-line code change to a Lit template + click handler, fully covered by storybook + e2e tests, and the modifier-key escape hatch is a well-trodden SPA-link pattern. The blast radius is one component; if the change breaks anything, storybook + e2e will catch it before commit.
- The only theoretical risk is that some consumer relies on plain `<a href="#">` behavior (i.e., expects the page NOT to navigate on any kind of click). No such consumer exists — grep confirms `cts-plan-list` is only mounted in `plans.html`, which actively wants navigation on click.

---

## Verification Strategy

Run, in order:

1. `cd frontend && npm run test:ci` — lint, format, type-check, codegen, lit-analyzer, icon-lint all pass.
2. Storybook tests for `cts-plan-list` (focused): `Default`, `ClickPlanName`, and the new assertion in `Default`.
3. E2E test for `plans.spec.js`: `clicking a plan name navigates to plan-detail.html`.
4. Manual browser verification at https://localhost.emobix.co.uk:8443/plans.html:
   - Hover a plan name; status bar shows `plan-detail.html?plan=...`.
   - Plain left-click navigates to the plan detail page.
   - Cmd-click (or middle-click) opens the detail page in a new tab.
   - Right-click → "Open in new tab" opens the detail page in a new tab.

---

## Deferred Implementation Notes

- Exact wording of the JSDoc comment above the modifier-key early return — short, one or two lines, explaining "browsers handle modifier-key clicks natively; preventing default here would also open the new tab AND navigate the current one."
- Whether to add the href assertion to `Default` or split into a new `PlanNameHrefIsRealUrl` story — prefer extending `Default` for minimal story inventory growth; revisit only if the play function becomes too long to read.
