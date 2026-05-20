---
title: "feat: Warn before leaving schedule-test with unsaved form edits"
status: active
created: 2026-05-18
type: feat
depth: Standard
---

## Summary

When a user is part-way through editing the large test-plan configuration form on
`schedule-test.html`, any attempt to leave the page — clicking an in-app link,
the browser Back button, closing the tab, reloading, or typing a new URL — must
prompt them to confirm before the changes are lost. Intentional submissions
("Create Test Plan", "Load last configuration", applying a shared link) must
NOT trigger the prompt.

The implementation introduces a reusable `<cts-unsaved-changes-guard>` Lit
element that:

- Tracks "dirty" state from `cts-config-change` events on `<cts-config-form>`
  and `input`/`change` events on the variant-selectors `<form>`.
- Wires a native `beforeunload` handler with the 2023+ canonical pattern
  (`event.preventDefault()` plus `returnValue = ""` for legacy browsers) so
  browser-level navigation (back/forward, tab close, reload, address bar) shows
  the OS-supplied warning.
- Captures same-document link clicks at the document level and routes them
  through a confirmation dialog hosted by the existing `<cts-modal>` (native
  `<dialog>` element) so in-app link navigation gets a styled "You have unsaved
  changes" prompt instead of the generic OS warning.
- Exposes `markClean()`/`markDirty()` so the page's submit handler and saved-
  config restore paths can suppress the warning when navigation is intentional.

---

## Problem Frame

`schedule-test.html` hosts the largest form in the suite — a multi-section
config editor backed by `<cts-config-form>` (`src/main/resources/static/components/cts-config-form.js`)
with hundreds of potential fields plus a variant-selector `<form>`. Users can
spend several minutes assembling a test-plan configuration (pasting keys,
typing client IDs, switching the JSON tab to tweak structure). Today, any
accidental link click, swipe-back gesture, or tab close silently discards
those edits with no warning.

The conformance suite has no other "long-lived editor" pages — this is the
single page where data loss on accidental exit is materially painful. The
solution should be reusable (the Lit component shape is generic) but only
needs to be wired on `schedule-test.html` in this change.

---

## Scope

**In scope**

- New reusable Lit element: `<cts-unsaved-changes-guard>`.
- Wire-up on `schedule-test.html` covering both `<cts-config-form>` edits and
  variant-selector edits.
- Two warning paths:
  - Browser-level navigation → `beforeunload` (modern signature).
  - Same-document link clicks → in-app `<cts-modal>` confirmation with
    "Stay on page" (default) / "Leave page" actions.
- Suppress the warning when:
  - The user submits via "Create Test Plan" (success path).
  - The page programmatically loads a saved/shared configuration into the
    form (`applyConfigToForm` paths).
  - The user clears the form via "clear" affordances (if any are reachable).

**Out of scope**

- Auto-save / draft persistence beyond what `saveConfig`/`loadConfig` already do.
- Adding the guard to other pages (`upload.html`, `login.html` — both too short
  to justify a guard).
- Reworking `cts-modal` internals — it is reused as-is.
- Persistent dirty state across reloads (the `beforeunload` warning is the
  reload safety net).

### Deferred to Follow-Up Work

- Adopting the `closedby="any"` attribute on `<dialog>` (Baseline 2024) inside
  `cts-modal` for backdrop dismissal of the confirmation dialog — out of scope
  here; would warrant its own modal-component change.
- Generalising the guard for hypothetical future long-form pages — defer until
  a second consumer appears.

---

## Key Technical Decisions

- **Use `beforeunload`, not Navigation API, for browser-level nav.** The
  Navigation API (`window.navigation.addEventListener('navigate', ...)`) is
  the 2023+ "modern" answer for SPAs but only fires for same-document SPA
  navigations and lacks Safari/Firefox support as of 2026. The conformance
  suite is a server-rendered multi-page app; `beforeunload` is the only
  cross-browser way to gate cross-document navigation (back/forward/close/
  reload/address-bar typing). Use the modern beforeunload signature:

  ```js
  window.addEventListener("beforeunload", (e) => {
    if (!this.dirty) return;
    e.preventDefault();
    e.returnValue = "";
  });
  ```

  `preventDefault()` is the canonical trigger per the current HTML spec
  (Chrome 119+ requires it); `returnValue = ""` keeps Safari and older
  Chrome/Firefox happy. The string content is ignored by every modern
  browser — the OS supplies the message.

- **Same-document link clicks need their own path.** `beforeunload` *does*
  fire for `<a href>` clicks that result in cross-document navigation, but
  the OS-supplied prompt is generic and unstyled. Intercepting same-origin
  link clicks at the document level lets us show a branded `<cts-modal>` with
  context-appropriate copy. The link interceptor uses a capture-phase
  `click` listener so it runs before the page navigation kicks off, and only
  intercepts under strict conditions (see U3 Approach).

- **Reuse `<cts-modal>` rather than a bespoke dialog.** `cts-modal`
  (`src/main/resources/static/components/cts-modal.js`) already wraps the
  native `<dialog>` element with `showModal()`, `<form method="dialog">`-style
  dismissal, ESC handling, and the design-system look. Its `footer-buttons`
  JSON descriptor renders the two action buttons with the right variants and
  icons — no need to invent a parallel dialog primitive.

- **Light DOM Lit element, consistent with sibling components.** The guard
  emits no rendered UI of its own (the modal is its only visual surface), so
  Light DOM keeps DevTools inspection straightforward and avoids a Shadow DOM
  boundary between the guard and its hosted `<cts-modal>`. This mirrors
  `cts-config-form` and other behavior-heavy CTS elements.

- **`AbortController` for listener cleanup.** All `window`/`document`-level
  listeners (`beforeunload`, capture-phase `click`) are registered with a
  shared `AbortSignal` so `disconnectedCallback` can revoke them in a single
  `abort()`. Pattern used elsewhere in the codebase and matches the 2023+
  recommended approach.

- **Dirty source of truth = guard internal flag, not config-equality.** Each
  user-driven `cts-config-change` flips the flag to `true`. Programmatic
  hydration (via `applyConfigToForm`, "Load last configuration", `configJson`
  URL param) calls `guard.markClean()` *after* setting `ctsConfigForm.config`,
  so reseeding the form does not arm the guard. Comparing JSON snapshots is
  rejected because the dotted-path config object includes hidden-field merges
  that can re-order keys in non-meaningful ways.

---

## Output Structure

```
src/main/resources/static/components/
├── cts-unsaved-changes-guard.js          (new)
├── cts-unsaved-changes-guard.stories.js  (new)
src/main/resources/static/
└── schedule-test.html                    (modified)
frontend/e2e/
└── schedule-test.spec.js                 (modified — add dirty-guard scenarios)
```

---

## High-Level Technical Design

*This illustrates the intended approach and is directional guidance for review,
not implementation specification.*

```
                ┌────────────────────────────┐
                │   schedule-test.html       │
                │                            │
                │   <cts-config-form/>       │──── cts-config-change ──┐
                │   <form id=variant…>       │──── input/change ───────┤
                │                            │                         ▼
                │   <cts-unsaved-changes-    │
                │     guard for="…">         │── dirty=true ──┐
                │                            │                │
                │   <cts-modal id="…"/>      │◄───────────────┘
                │                            │
                └──────────┬─────────────────┘
                           │
                           │  installs listeners (AbortController)
                           ▼
              ┌───────────────────────────────────────┐
              │ window  → beforeunload                │
              │   if (dirty) { preventDefault();      │
              │                returnValue = ""; }    │
              ├───────────────────────────────────────┤
              │ document → click (capture)            │
              │   a = target.closest("a[href]")       │
              │   if (a && sameDoc && noModifier      │
              │       && !target=_blank && dirty)     │
              │     e.preventDefault();               │
              │     modal.show()                      │
              │     ↳ Leave  → markClean(); nav       │
              │     ↳ Stay   → modal.hide()           │
              └───────────────────────────────────────┘

   Submit / programmatic load → guard.markClean() before nav
```

---

## Implementation Units

### U1. New `cts-unsaved-changes-guard` Lit element

**Goal:** Provide a reusable, behavior-only Lit element that tracks dirty
state, installs browser-level + in-app guards, and hosts the confirmation
dialog.

**Requirements:** All — this is the central abstraction.

**Dependencies:** None.

**Files:**
- `src/main/resources/static/components/cts-unsaved-changes-guard.js` (new)

**Approach:**

- Light DOM Lit element. Class extends `LitElement`; `createRenderRoot()` returns
  `this`.
- JSDoc `@property` block covering: `for` (id of form element to watch, optional),
  `configFormId` (id of `<cts-config-form>` to watch, optional), `dirty`
  (reactive boolean, default `false`), `heading` (modal heading, default
  "You have unsaved changes"), `confirmLabel` (default "Leave page"),
  `cancelLabel` (default "Stay on page"), `message` (default body copy).
- `render()` returns a single `<cts-modal>` host with `footer-buttons` JSON
  carrying the Leave/Stay buttons. Heading and body are slotted from the
  reactive properties so consumers can override.
- `connectedCallback()`:
  - Create an `AbortController`; store on `this._ac`.
  - Listen for `cts-config-change` on the resolved `<cts-config-form>` (look
    up by `configFormId`, fall back to first `cts-config-form` descendant of
    the page if not specified).
  - Listen for `input`/`change` on the resolved `<form>` (look up by `for`).
  - Add `window.addEventListener("beforeunload", handler, { signal })`.
  - Add `document.addEventListener("click", linkClickHandler, { capture: true, signal })`.
- `disconnectedCallback()` calls `this._ac.abort()`.
- Public methods:
  - `markClean()` — sets `this.dirty = false`.
  - `markDirty()` — sets `this.dirty = true` (rarely needed; exposed for tests
    and explicit edge cases).
- The `beforeunload` handler:
  ```js
  (e) => {
    if (!this.dirty) return;
    e.preventDefault();
    e.returnValue = "";
  }
  ```
- The link click handler is responsible only for *triggering* the modal; the
  actual same-origin / modifier checks live in U3.

**Patterns to follow:**
- `src/main/resources/static/components/cts-config-form.js` — Light DOM Lit
  conventions, scoped style injection (NOT needed here — no rendered chrome
  beyond the modal).
- `src/main/resources/static/components/cts-modal.js` — host element pattern
  with `.show()` / `.hide()`, `footer-buttons` JSON descriptor.

**Test scenarios** (covered by U2 Storybook + U4 wiring + U5 E2E):
- `dirty` starts `false`.
- `markDirty()` flips to `true`; `markClean()` flips back to `false`.
- `cts-config-change` from the resolved config-form flips `dirty` to `true`.
- `input` / `change` on the resolved `<form>` flips `dirty` to `true`.
- `disconnectedCallback` aborts the controller (no listeners leak).
- `beforeunload` handler is a no-op when `dirty === false`.
- `beforeunload` handler calls `preventDefault()` and sets `returnValue = ""`
  when `dirty === true`.

**Verification:** Storybook story renders the element with a mock form; play
function exercises the dirty toggle and asserts modal `.show()` is called on
intercepted link clicks (verified in U2).

---

### U2. Storybook stories + play-function tests for the guard

**Goal:** Cover the guard's behavior with colocated stories and Storybook play
tests, matching the suite-wide convention.

**Requirements:** Component conventions (stories colocated; play tests required);
JSDoc maintained on touched components.

**Dependencies:** U1.

**Files:**
- `src/main/resources/static/components/cts-unsaved-changes-guard.stories.js` (new)

**Approach:**

- Default export: `{ title: "Behaviour/cts-unsaved-changes-guard", component: "cts-unsaved-changes-guard", parameters: { layout: "padded" } }`.
- `Pristine` story: render `<cts-unsaved-changes-guard>` adjacent to a mock
  `<form>` and a stub `<cts-config-form>`-like element; assert `dirty === false`
  initially.
- `DirtyAfterFormEdit` story: dispatch a synthetic `input` event on the mock
  form; play function asserts `dirty === true`.
- `DirtyAfterConfigChange` story: dispatch a synthetic `cts-config-change`
  on a stub element; play function asserts `dirty === true`.
- `LinkClickIntercepted` story: render the guard plus an `<a href="/plans.html">`
  inside the story root; mark dirty; play function clicks the link with a
  spied `event.preventDefault`, asserts `preventDefault()` was called and that
  the modal is open. Tests the in-app interception (U3).
- `SubmitMarksClean` story: programmatically call `guard.markClean()`; assert
  that a subsequent link click is NOT intercepted.
- Snapshot the modal DOM with the `getNormalizedInnerHTML` helper to dodge
  Lit's marker comments (per project feedback).

**Patterns to follow:**
- `src/main/resources/static/components/cts-modal.stories.js` — modal-host
  story pattern + play function shape.
- Vitest browser stale-cache caveat: if play tests fail on selectors that the
  live :6006 Storybook confirms render correctly, restart the test daemon.

**Test scenarios:** Five play stories above. Each asserts the specific
behavior named in its title; no padding/duplicate coverage.

**Verification:** `run-story-tests` for these specific stories passes locally;
preview URLs included in the user-facing summary.

---

### U3. Same-document link interception logic

**Goal:** Translate a same-document link click into either "fall through
normally" or "show the confirmation dialog", driven by the dirty flag.

**Requirements:** Same-origin in-app navigation must show the styled dialog;
external/new-tab/modifier-key clicks must behave normally.

**Dependencies:** U1 (handler lives inside the guard).

**Files:**
- `src/main/resources/static/components/cts-unsaved-changes-guard.js` (modified
  during U1 — splitting the click handler into its own method here for
  clarity, but no separate file).

**Approach:**

The capture-phase click handler resolves the candidate anchor and bails out
when *any* of the following are true:

- `event.defaultPrevented` (another handler already cancelled).
- `event.button !== 0` (not the primary mouse button).
- `event.metaKey || event.ctrlKey || event.shiftKey || event.altKey` (the
  user wants a new tab / window / download).
- No `closest("a[href]")` ancestor.
- `anchor.target` is set and not `"_self"` (e.g., `_blank`).
- `anchor.hasAttribute("download")`.
- `anchor.origin !== window.location.origin`.
- `anchor.pathname === window.location.pathname && anchor.search === window.location.search`
  (pure hash-only navigation on the same page — no data loss).
- `!this.dirty`.

When none of the bail conditions hold, the handler:

1. Calls `event.preventDefault()` to stop the browser-initiated navigation.
2. Stashes `anchor.href` on `this._pendingHref`.
3. Calls `this._modal.show()`.

The modal's `cts-modal-close` listener inspects the dismiss reason. The
Leave button (`dismiss:true` descriptor with `id="ctsLeaveBtn"` or matching
data-action) calls `this.markClean()` and `window.location.assign(this._pendingHref)`.
The Stay button (`dismiss:true` with `data-action="stay"`) is a no-op. ESC
key behaves the same as Stay.

**Patterns to follow:**
- `src/main/resources/static/components/cts-modal.js` — `footer-buttons` JSON
  descriptor; how button clicks correlate to `cts-modal-close` reasons.

**Technical design — bail-condition order:**

```text
event → defaultPrevented? ──yes──► return
      → button !== 0?       ──yes──► return
      → modifier keys?      ──yes──► return
      → closest("a[href]")? ──no───► return
      → target=_blank?      ──yes──► return
      → download attr?      ──yes──► return
      → cross-origin?       ──yes──► return
      → pure hash on page?  ──yes──► return
      → !dirty?             ──yes──► return
      → preventDefault();
        modal.show();
        wait for Leave/Stay → markClean()+navigate / no-op
```

**Test scenarios:**

- Same-origin same-doc link, no modifiers, dirty=true → intercepted (modal opens).
- Same-origin same-doc link, no modifiers, dirty=false → falls through (no modal).
- `target="_blank"` link, dirty=true → falls through (new tab).
- Cmd-click (metaKey), dirty=true → falls through.
- Cross-origin link (`https://openid.net/...`), dirty=true → falls through.
- Pure hash link to current pathname, dirty=true → falls through.
- `download` attribute, dirty=true → falls through.
- Right-click (`button !== 0`), dirty=true → falls through (browser shows context menu).
- After clicking "Leave page", the guard navigates to `pendingHref` and never
  re-prompts on `beforeunload` (markClean was called).
- After clicking "Stay on page", the guard remains dirty and re-prompts on the
  next link click.
- ESC on the modal behaves like "Stay".

**Verification:** Storybook play function (U2) and Playwright E2E spec (U5)
both cover the canonical happy-path and the modifier/target/cross-origin
bail cases.

---

### U4. Wire `<cts-unsaved-changes-guard>` into `schedule-test.html`

**Goal:** Instantiate the guard on the schedule-test page, point it at the
config-form and the variant-selectors `<form>`, and call `markClean()` from
the submit handler and the programmatic-load paths.

**Requirements:** Browser-level + in-app navigation gating; intentional
submit/load does not warn.

**Dependencies:** U1, U3.

**Files:**
- `src/main/resources/static/schedule-test.html` (modified)

**Approach:**

- Add `<script type="module" src="/components/cts-unsaved-changes-guard.js"></script>`
  to `<head>` alongside the other component imports.
- Insert `<cts-unsaved-changes-guard id="exitGuard" config-form-id="ctsConfigForm" for="variantSelectors"></cts-unsaved-changes-guard>`
  inside `<main>` (anywhere — it produces no visible chrome until the modal opens).
- In `loadScheduleTestPage()`, after the `cts-config-change` wiring at
  `src/main/resources/static/schedule-test.html:638`, grab the guard reference
  and stash on a page-local `const exitGuard = document.getElementById("exitGuard");`.
- Insert `exitGuard.markClean()` at the start of `applyConfigToForm`
  (right after the function entry / once the new config is set on the
  config-form). The schedule-test page already routes "Load last
  configuration", `?configJson=…`, and saved-config restore through
  `applyConfigToForm`; one hook point covers all three.
- Insert `exitGuard.markClean()` inside `createPlanBtn.onclick`, just before
  the `fetch(createUrl, …)` call (after `saveConfig(...)`, before the POST).
  This way, even if the network is slow, the redirect at line ~710
  (`window.location.assign('/plan-detail.html?…')`) does not trigger the modal.
  If the POST fails and the user stays on the page, they will edit again →
  `cts-config-change` re-fires → dirty re-arms. This is the correct UX:
  "intent to submit" clears the guard, and a failed submit re-arms naturally
  when the user resumes editing.
- Insert `exitGuard.markClean()` in `clearConfigForNewPlan` (referenced near
  line 1004) so swapping plans does not leave the guard armed on a now-empty
  form.

**Patterns to follow:**
- `src/main/resources/static/schedule-test.html:645–656` — pattern for
  resolving a custom-element ref and wiring listeners.

**Test scenarios** (covered by U5):

- Edit a config field → click the OIDF Foundation Certification Instructions
  link → modal opens; Stay keeps the user; Leave navigates to openid.net.
  (NB: this link is cross-origin → falls through per U3; the test instead
  uses an internal link like the dashboard link in the header.)
- Edit a config field → click an internal nav link (e.g., header logo to
  `/`) → modal opens.
- Edit a config field → click "Create Test Plan" → no modal; POST proceeds;
  page navigates to plan-detail on success.
- Edit a config field → click "Load last configuration" → no modal; form
  hydrates; dirty cleared.
- Apply a `?configJson=` URL → no modal on the immediate redirect.
- Reload via Cmd-R after editing → browser-level prompt fires (asserted via
  `page.on("dialog")` in Playwright).
- Reload via Cmd-R on a pristine form → no browser prompt.

**Verification:** Manual smoke test on `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
(load schedule-test, edit a field, click various exit affordances, observe
the right prompt or no prompt). Playwright E2E in U5 covers the regression
suite.

---

### U5. E2E coverage in `frontend/e2e/schedule-test.spec.js`

**Goal:** Add Playwright scenarios to lock in the wire-up behavior and protect
against regressions.

**Requirements:** Submit/load do not warn; internal link click on a dirty form
shows the modal; `beforeunload` fires for browser-level nav.

**Dependencies:** U4.

**Files:**
- `frontend/e2e/schedule-test.spec.js` (modified)

**Approach:**

- Group new tests under `describe("unsaved changes guard", …)`.
- Reuse existing fixture setup (`setupFailFast`, `wrapDataTablesResponse`, etc.).
- Tests:

  - `"pristine form: internal link click navigates without prompt"` — load page,
    click an internal nav link with no edits, assert nav completed without a
    dialog event firing.
  - `"dirty form: internal link click opens the unsaved-changes modal"` — load,
    edit a field via the rendered `<cts-form-field>`, click an internal link,
    assert `cts-modal[open]` is present with the expected heading.
  - `"dirty form: Stay on page dismisses modal and keeps the user on the page"` —
    after the modal opens, click Stay, assert modal closes and URL is
    unchanged.
  - `"dirty form: Leave page navigates"` — after the modal opens, click Leave,
    assert URL changed to the link target.
  - `"create test plan does not trigger the unsaved-changes modal"` — edit
    the form, click "Create Test Plan", assert the POST fires (existing
    fixture) and no modal opens before navigation.
  - `"load last configuration does not trigger the unsaved-changes modal"` —
    edit the form, click Load Last Configuration, assert form is hydrated and
    modal never opens.
  - `"browser reload on dirty form triggers beforeunload prompt"` —
    `page.on("dialog", d => { dialogText = d.message(); d.dismiss(); })`,
    edit the form, call `page.reload()`, assert the dialog event fired and
    was dismissed (URL unchanged). Mark `test.skip` with a comment if the
    Playwright version in use does not surface `beforeunload` dialogs
    reliably — pre-existing flakes are documented in memory; we will
    `stash`-verify before treating it as a regression.

**Patterns to follow:**
- Existing `frontend/e2e/schedule-test.spec.js` test shape and fixture helpers.

**Test scenarios:** The seven cases listed above. Each is a `test(…)` block.

**Verification:** `cd frontend && ./node_modules/.bin/playwright test e2e/schedule-test.spec.js`
passes locally; CI `frontend_lint` and any E2E job stay green.

---

## System-Wide Impact

- **Other pages:** `upload.html`, `login.html`, and `plan-detail.html` are
  unaffected — the guard ships as a component but is wired only on
  schedule-test for now. No existing event listeners are modified.
- **`cts-modal`:** Reused as-is. No API change. One additional consumer.
- **`cts-config-form`:** No change. The guard listens to the existing
  `cts-config-change` event that is already emitted on every edit.
- **`saveConfig` / `loadConfig` / `clearSavedConfig`:** No change. The guard
  is layered on top via explicit `markClean()` calls in `applyConfigToForm`,
  `clearConfigForNewPlan`, and the submit handler.
- **Accessibility:** `<dialog>` `showModal()` traps focus and is announced
  by AT (cts-modal already handles this). The browser-level prompt is the
  OS one, so platform-native announcement.

---

## Risks and Mitigations

- **Risk:** `beforeunload` is throttled / suppressed by Chrome until the user
  has interacted with the page (sticky user activation). On schedule-test the
  act of editing is itself the activation, so this is unlikely to bite, but
  some automated tests on a fresh page can hit this. **Mitigation:** the
  Playwright `reload-while-dirty` test edits the form first; flakiness around
  beforeunload is annotated in the test.

- **Risk:** Capture-phase document click listeners can fight with code that
  also intercepts clicks. **Mitigation:** the bail conditions in U3 are
  strict; `event.defaultPrevented` is the first check, so any earlier
  handler that already cancelled wins.

- **Risk:** Programmatic navigation (`window.location.assign(...)` outside
  the submit handler) bypasses our link interception but still fires
  `beforeunload`. **Mitigation:** the `beforeunload` path is the safety net
  for any code path we miss. The submit handler explicitly calls
  `markClean()` so it never trips the warning.

- **Risk:** The `?configJson=…` apply path uses `queueMicrotask` (per
  `schedule-test.html:224`). **Mitigation:** the `markClean()` call lives
  inside `applyConfigToForm`, which is the function the microtask invokes,
  so timing is consistent.

- **Risk:** Lit marker comments break Playwright DOM snapshots (project memory).
  **Mitigation:** snapshot tests in U2 use the `getNormalizedInnerHTML` helper
  per the established convention.

---

## Verification

- `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` succeeds.
- `cd frontend && npm run test:ci` succeeds (format → lint → type-check →
  jsdoc → lit-analyzer).
- `cd frontend && ./node_modules/.bin/playwright test e2e/schedule-test.spec.js`
  passes locally, including the new `describe("unsaved changes guard")` block.
- Storybook play tests for the new component pass (`run-story-tests` for the
  cts-unsaved-changes-guard stories).
- Manual smoke on `https://localhost.emobix.co.uk:8443/schedule-test.html`:
  - Edit a config field → click the dashboard link in the header → modal.
  - Edit a config field → click "Create Test Plan" → no modal; plan creates.
  - Edit, reload → browser prompt; cancel → still on page.
  - Edit, reload → browser prompt; confirm → page reloads, form is empty
    (browser default reload behaviour; the guard does not preserve drafts).
  - Apply `?configJson=…` URL → no modal flicker on the apply.

---

## Open Questions and Deferred Notes

- The modal copy ("You have unsaved changes" / "Stay on page" / "Leave page")
  follows common conventions; if Joseph or Thomas (core maintainers) prefer
  alternate wording, swap via the guard's reactive props — no structural
  change.
- Whether to extend the same guard to `upload.html` / future long forms is
  deferred until a second consumer appears. The component is built to be
  reusable but the wire-up here is only for schedule-test.
