---
title: MR 1911 PR review remediation
type: fix
status: completed
date: 2026-04-17
deepened: 2026-04-17
completed: 2026-04-17
---

# MR 1911 PR review remediation

## Overview

Address the 7 critical, 14 important, and 8 minor findings from the `/pr-review-toolkit:review-pr` pass on GitLab MR !1911 ("feat(frontend): Web Components POC with Storybook + E2E coverage"). Most items are code-hygiene, error-handling, and test-coverage fixes grounded in established patterns that already exist in the MR. One item (cts-modal Bootstrap interop claim) requires a small contractual decision, captured here.

The goal is to clear blockers so the MR can exit Draft, not to expand scope.

## Problem Frame

MR !1911 introduced ~22 Lit-based Web Components, Storybook infrastructure, MSW mocking, and Playwright E2E coverage (138 files, +20.7k/−1.3k LoC). A multi-agent review surfaced a mix of:

- Real bugs (dead `FormData`, orphan poll timer)
- Systemic convention violations (22/27 components missing JSDoc `@property` tags; 3 components missing Storybook stories — both violate `src/main/resources/static/components/AGENTS.md` §5 and §6)
- Silent-failure clusters in data-fetching components
- One contractual issue (cts-modal's coincidental `bootstrap.Modal` compatibility is documented as a contract but is actually happenstance)
- Test-coverage gaps (misnamed `error-paths.spec.js`, missing a11y stories, weak MSW handlers)

Every finding has a specific `file:line` reference — there is no ambiguity about *what* to change, only about *how* to batch the work and one decision about the cts-modal API surface.

## Requirements Trace

Critical (must-fix before removing Draft):

- R-C1 — cts-image-upload: remove dead `FormData` or wire it as the body
- R-C2 — Close the JSDoc `@property` gap across 22 components (AGENTS.md §5)
- R-C3 — Add stories (or document internal-subcomponent status) for `cts-plan-actions`, `cts-plan-header`, `cts-plan-modules` (AGENTS.md §6). **Verified 2026-04-17: already satisfied via colocation in `cts-plan-detail.stories.js` (17 play-function stories across the three sub-components). Unit 6 reduced to a verification + AGENTS.md clarification step.**
- R-C4 — Resolve silent-failure cluster. Decomposed:
  - R-C4a — `cts-log-viewer`: surface persistent fetch failures after N retries (Unit 2)
  - R-C4b — `cts-dashboard`, `cts-navbar`, `cts-spec-cascade`: log non-401 errors; distinguish 401 from 5xx/network (Unit 3)
  - R-C4c — `cts-token-manager`, `cts-plan-actions`: clipboard failure feedback via aria-live (Unit 3)
- R-C5 — `cts-log-viewer` orphan poll timer on disconnect
- R-C6 — Retract-or-extend cts-modal's `bootstrap.Modal` backward-compat claim
- R-C7 — `error-paths.spec.js` must exercise actual error branches (DataTables 500, `/api/info/:id` 404, `POST /api/plan` 400)

Important (should-fix before merge):

- R-I1 — cts-modal a11y & focus coverage (Escape, static-backdrop, `role="dialog"`, `aria-modal`, focus return)
- R-I2 — cts-modal `_createButton` icon descriptor support (restore `bi-box-arrow-in-right` on private-link modal)
- R-I3 — cts-modal `_createButton` variant-key prefix-strip bug (`btn-outline-primary` silently downgraded)
- R-I4 — cts-badge attribute-change wipes captured slot children (document or fix)
- R-I5 — cts-form-field `WithError` story covers only input (missing textarea/select/checkbox)
- R-I6 — cts-tabs keyboard activation (Enter/Space) + `cts-tab-change` emission coverage
- R-I7 — fapi.ui.js: 4 catch-all / unhandled-rejection sites
- R-I8 — cts-tooltip dynamic-init timing (`:scope > *` too narrow)
- R-I9 — cts-config-form invalid JSON produces no visible indicator
- R-I10 — tokens delete test asserts request fired but not row removal
- R-I11 — MSW handlers return 200 on any shape (publish / makemutable / delete / uploadimage / certificationpackage / delete-token)
- R-I12 — journeys.spec.js has no error-flow coverage
- R-I13a — AGENTS.md §18 cts-tabs LitElement hedge removed
- R-I13b — AGENTS.md quick-ref table gains a `cts-card` row
- R-I14 — cts-login-page uses `<cts-link-button>` without explicit import

Minor (batch-polish):

- R-M1 — cts-navbar `_fetchUser` spams 401s on `login.html`
- R-M2 — cts-button `_renderIcon` label fallback redundancy
- R-M3 — cts-log-detail-header inconsistent optional chaining
- R-M4 — cts-log-entry.stories `BlockEntry` border-color assertion
- R-M5 — cts-tooltip.stories `BottomPlacement` should assert popup position, not just attribute
- R-M6 — clipboard.spec.js spy installation misses first assignment
- R-M7 — cts-link-button.js:47 icon comment truncated vs. cts-button.js:86-87
- R-M8 — vitest.config.js chromium-only; Safari operators uncovered

## Scope Boundaries

- **Not changing** the broader architecture of cts-* components, their composition with Bootstrap, Lit, or MSW.
- **Not introducing** new components. Three missing stories (R-C3) are additions for existing components only.
- **Not adding** Chromatic, percy, or other visual-regression tooling beyond Unit 8's vitest webkit smoke consideration.
- **Not migrating** more legacy HTML pages to Web Components (that is a separate plan: `docs/plans/2026-04-16-002-refactor-html-to-web-components-plan.md`). Minimal integration patches to existing pages — e.g., `privateLinkModals.html` icon restoration (Unit 4) and `login.html` `auth-state` attribute (Unit 8 Sub-theme C) — ARE in scope.
- **Not touching** vendored third-party files under `src/main/resources/static/vendor/**`.

### Deferred to Separate Tasks

- ArchUnit / static-analysis enforcement of the `@property` JSDoc rule — valuable, but a separate engineering lift; file as follow-up.
- Solution doc for CTS-wide silent-failure conventions — nice to have, but out of scope here; add as follow-up.
- Webkit-parallel CI run for the full vitest suite (R-M8 addresses only a smoke run).

## Context & Research

### Relevant Code and Patterns

- **JSDoc exemplar** — `src/main/resources/static/components/cts-button.js:4-55`. Class-level `@property` tags + markdown sub-sections ("Programmatic activation", "Light-DOM dependencies"). Five compliant components today: `cts-button`, `cts-link-button`, `cts-badge`, `cts-alert`, `cts-modal`.
- **Error-handling exemplar** — `src/main/resources/static/components/cts-token-manager.js`. `response.ok` check → `throw new Error(\`HTTP ${status}\`)` → `catch (err) { this._error = err.message; this._showError = true; }` → `_renderErrorModal()` mounted in `render()`. Internal state is `_error`, `_showError` (both `{ state: true }`).
- **Stories pattern** — `src/main/resources/static/components/cts-button.stories.js`. Default export carrying `title`, `component`, `argTypes`; each story exports `{ args, render, play }`. `play` queries the inner rendered DOM, not the host (per `AGENTS.md` §6).
- **Playwright fail-fast** — `frontend/e2e/helpers/routes.js:108-139`. `setupFailFast()` must be called first (routes match in reverse registration order); `expectNoUnmockedCalls(page)` is called in `afterEach` or at the end of a test.
- **MSW handler shape** — `frontend/stories/fixtures/msw-handlers.js`. `http.get/post(path, ({ request }) => HttpResponse.json(...))`. Error variants (`unauthenticatedHandlers`) live alongside happy-path handlers in the same file.
- **Shared button class helper** — `src/main/resources/static/components/_button-classes.js`. Explicit lookup tables for variant/size (no `btn-${x}` interpolation). cts-modal should consume the same helper when rendering footer buttons.
- **AGENTS.md §5 (quoted verbatim)** — "Every `cts-*` component **must** have a JSDoc `@property` annotation for each entry in its `static properties` declaration (LitElement) or each entry in `static observedAttributes` (vanilla HTMLElement). ... This is enforced in code review."
- **AGENTS.md quick-ref table** (lines 232-245) — currently covers 10 of 27 components. Rows missing: `cts-batch-runner`, `cts-config-form`, `cts-dashboard`, `cts-icon`, `cts-image-upload`, `cts-log-detail-header`, `cts-log-entry`, `cts-log-viewer`, `cts-login-page`, `cts-plan-actions`, `cts-plan-detail`, `cts-plan-header`, `cts-plan-list`, `cts-plan-modules`, `cts-running-test-card`, `cts-spec-cascade`, `cts-test-selector`, `cts-token-manager`.

### Institutional Learnings

- **External exemplar — `docs/solutions/web-components/cts-button-host-vs-inner-button-semantics-2026-04-17.md`** (already exists in the repo; load-bearing for Unit 4). Defines the "host = integration point (id/class/data/reactive props); inner = interaction point (clicks/focus/form)" convention with a "What breaks silently" table. **This is the structural template for Unit 4's new cts-modal API contract doc** — same frontmatter, same "What works" / "What breaks silently" / "Operational rule" sections.
- **`docs/solutions/test-failures/playwright-e2e-flaky-after-web-component-merge-2026-04-14.md`** — partial relevance. Confirms Playwright route-registration order convention used by `routes.js` helpers.

### External References

None — this is a local-remediation plan; the existing repo patterns are sufficient.

## Key Technical Decisions

- **D-1 — Delete, don't wire, the `cts-image-upload` `FormData`.** Rationale: the `FormData` allocation at lines 71-79 is **incomplete scaffolding — built but never referenced as a body**, not an intentional multipart design that was abandoned. The `@RequestBody String encoded` signature at `src/main/java/net/openid/conformance/logging/ImageAPI.java:112-115` confirms the endpoint accepts the dataURL string body directly. Switching to multipart would change the server contract and expand scope. Delete the dead lines and add a one-line comment explaining why the body is a string.
- **D-2 — Retract cts-modal's "backward-compatible with bootstrap.Modal API" framing; document the inner `.modal` as the integration point.** Rationale: **no existing caller is broken by this change** — every call site goes through `document.getElementById(id)`, which resolves to the inner `.modal` after id-transfer (`cts-modal.js:49-53`). The retraction is documentation housekeeping, not behavior change. It future-proofs against a regression if the host template ever changes (shadow DOM, nested render). A solution doc modeled after `cts-button-host-vs-inner-button-semantics-2026-04-17.md` makes the contract explicit and cross-links from the cts-button doc.
- **D-3 — Data-fetch error-handling pattern is copied from `cts-token-manager`.** Rationale: already in the MR, already passing review. Add `console.warn(err)` at non-401 branches to aid diagnosability; keep `_error` reactive-state render for user-visible paths (`cts-spec-cascade`, `cts-log-viewer`). For `cts-dashboard` and `cts-navbar`, keep silent fallback but log; for clipboard failures, add `catch { this._copyFeedback = "Copy failed — select text manually" }` + aria-live announcement.
- **D-4 — cts-plan-* subcomponents get real stories, not "internal" exemption.** Rationale: all three take explicit props from `cts-plan-detail` (inspectable in `cts-plan-detail.stories.js`). Story-rendering each with fixture props is straightforward and preserves the AGENTS.md §6 rule uniformly.
- **D-5 — JSDoc `@property` pass is a single atomic commit, but NOT mechanical for every component.** Rationale: drift risk is low if landed atomically. The pass has two internal stages of work:
  - *Stage A (mechanical, ~15 components)* — simple String/Boolean/number `static properties` like `cts-navbar`, `cts-icon`, `cts-login-page`, `cts-form-field`, `cts-test-selector`, etc. Transcription from the property declaration.
  - *Stage B (requires thinking, ~4 components)* — `cts-log-detail-header.js` (`testInfo` is a complex object needing a `@typedef` block), `cts-plan-modules.js` (array element shape inferred from `cts-plan-detail` callers), `cts-config-form.js` (4 Object props with JSON-schema-driven shapes). Budget 15-20 min per component for reading-and-thinking.
  - *Vanilla-HTMLElement group (`cts-tooltip`, `cts-card`, `cts-modal`, `cts-alert`)* — declare neither `static properties` nor `observedAttributes`; read attributes via `getAttribute()` in `connectedCallback`. Extend AGENTS.md §5: "For vanilla HTMLElements that read attributes imperatively, document each attribute read via `getAttribute` as a `@property` tag on the class — the rule is about documenting the component's external attribute API, not about the mechanism of property declaration."
  - Land all 22 components in one commit (atomicity beats splitting) but honor the time estimate.
- **D-8 — AGENTS.md §5 enforcement gets an automated check (addresses the root cause of the 22/27 violation).** Rationale: the rule says "enforced in code review" but a POC landed with 22 violations. Add a grep-based lint script (`frontend/scripts/lint-jsdoc-properties.sh`) that asserts every `cts-*.js` has at least one `@property` tag (lightweight presence check, not type-completeness). Wire it into `npm run lint:jsdoc` and update AGENTS.md §5's enforcement sentence to reference the script. Per-component type/shape correctness remains a human-review concern.
- **D-6 — `error-paths.spec.js` is augmented, not renamed.** Rationale: **the existing test IS a legitimate error-branch test** — it exercises `showDialogError()` in `plan-detail.html` via an oversized-upload path that injects a danger `cts-alert` into the DOM. The file's header docstring calls out that this is the only dynamic cts-alert injection path in the static pages. Unit 7 adds three *additional* error branches (logs 500, log-detail 404, schedule-test 400) as parallel `test(...)` blocks with their own framing; it does not rename, replace, or deprecate the existing test.
- **D-7 — Phase 2 is a single "polish" commit per theme, not per finding.** Rationale: 14 important + 8 minor findings touching ~15 files — bundling them by theme (test coverage expansion; error logging; component polish) keeps reviewability high without 22 micro-commits.

## Open Questions

### Resolved During Planning

- *Wire FormData as multipart, or delete dead code?* → Delete (D-1).
- *Extend cts-modal to expose a Bootstrap-compatible `show`/`hide`/`getInstance` passthrough on the host, or retract the claim?* → Retract (D-2). A passthrough is a defensible future direction but adds surface area the POC doesn't need.
- *Write stories for cts-plan-* subcomponents, or mark as internal?* → Write stories (D-4).
- *Should the silent-fetch in `cts-navbar` surface a user-visible error?* → No — a 401 on `login.html` is expected; non-401 errors should `console.warn` but render the logged-out navbar (the page that needs a working navbar doesn't have a better fallback).

### Deferred to Implementation

- Exact phrasing of `console.warn` messages in the five error-handling sites (keep them short; include the endpoint path).
- Precise test-file placement for MSW handler validation (may colocate assertions in existing story play functions vs. adding a dedicated spec).
- Whether `cts-badge` slot preservation becomes a code fix or a JSDoc-only acknowledgement — depends on whether any real caller re-renders attributes post-mount. If none, a comment is sufficient; if there's even one, cache children in a detached fragment.
- Specific selector for asserting the deleted-token row is gone (`tokens.spec.js`) — likely `expect(page.getByTestId(\`token-row-${tokenId}\`)).toBeHidden()`, but the DOM may not use data-testid yet.

## Implementation Units

### Phase 1 — Critical must-fix

- [x] **Unit 1: Remove dead FormData in cts-image-upload**

**Goal:** Eliminate misleading dead code in the upload path; document why the body is a dataURL string.

**Requirements:** R-C1

**Dependencies:** None

**Files:**
- Modify: `src/main/resources/static/components/cts-image-upload.js` (lines 71-79)

**Approach:**
- Delete the two `formData` lines.
- Add a one-line comment explaining that the endpoint accepts the dataURL body directly (the pre-MR upload behavior).
- Optionally extract the fetch into a named method for readability, but keep the diff minimal.

**Patterns to follow:** N/A (local cleanup).

**Test scenarios:**
- *Happy path*: `cts-image-upload` upload story (`src/main/resources/static/components/cts-image-upload.stories.js`) still passes its existing play function — no behavioral change expected.
- *Integration*: rerun `frontend/e2e/upload.spec.js` to confirm the existing upload flow still works end-to-end against the MSW handler.

**Verification:**
- Build green; the component renders and uploads as before; diff is reduced by 2 lines plus the clarifying comment.

---

- [x] **Unit 2: cts-log-viewer disconnect-safe polling + retry surfacing**

**Goal:** Prevent the orphan poll-timer leak on disconnect and surface persistent log-fetch failures to the user after N retries instead of retrying silently forever.

**Requirements:** R-C5, R-C4 (log-viewer slice)

**Dependencies:** None

**Files:**
- Modify: `src/main/resources/static/components/cts-log-viewer.js` (lines 32-53 and the `_pollTimer` lifecycle)
- Modify: `src/main/resources/static/components/cts-log-viewer.stories.js` (add persistent-failure story)

**Approach:**
- In the `finally` block that schedules the next `setTimeout`, early-return if `!this.isConnected`.
- Introduce `_consecutiveFailures` reactive state. After e.g. 3 consecutive `catch` hits, set `_error = "Log connection lost — retrying…"` and render it as a banner. Reset to 0 on success.
- Keep `console.warn(err)` in the catch for diagnosability.
- Consider an `AbortController` on the in-flight fetch in `disconnectedCallback` (small follow-up, only if straightforward).

**Patterns to follow:** `cts-token-manager.js` `_error` + reactive render pattern.

**Test scenarios:**
- *Happy path*: MSW returns log entries on every poll → `_consecutiveFailures` stays at 0; no banner renders; entries accumulate.
- *Error path*: MSW returns 500 for 3 consecutive polls → banner renders with the "Log connection lost" message.
- *Recovery*: After 3 failures, MSW returns 200 → banner disappears; `_consecutiveFailures` resets.
- *Lifecycle*: Element disconnects mid-fetch (simulate via `element.remove()` after the fetch starts) → no further `setTimeout` fires; assert via a timer-count spy or by checking no additional MSW hits occur.

**Verification:**
- Story tests pass. Manual check: mount and unmount the component repeatedly in a test page; no growing timer count in devtools.

---

- [x] **Unit 3: Data-fetch error-handling hardening (cts-dashboard, cts-navbar, cts-spec-cascade, clipboard in cts-token-manager + cts-plan-actions)**

**Goal:** Replace empty `catch { }` sites and silent fallbacks with the cts-token-manager error pattern. Distinguish 401 (expected) from network/5xx errors in navbar. Give clipboard-copy actions user-visible feedback on failure.

**Requirements:** R-C4

**Dependencies:** Unit 2 (establishes the retry-with-UI-state precedent); not strictly blocking.

**Files:**
- Modify: `src/main/resources/static/components/cts-dashboard.js` (lines 42-44)
- Modify: `src/main/resources/static/components/cts-navbar.js` (lines 34-48)
- Modify: `src/main/resources/static/components/cts-spec-cascade.js` (lines 37-39)
- Modify: `src/main/resources/static/components/cts-token-manager.js` (lines 130-137, `_copyToken`)
- Modify: `src/main/resources/static/components/cts-plan-actions.js` (lines 33-39, `_handleCopyConfig`)
- Modify: `src/main/resources/static/components/cts-token-manager.stories.js` and `src/main/resources/static/components/cts-spec-cascade.stories.js` (add error stories)
- Create: `src/main/resources/static/components/cts-dashboard.stories.js` additions and `cts-navbar.stories.js` additions if existing error stories are thin.

**Approach:**
- cts-dashboard: keep the "non-critical" framing but `console.warn("[cts-dashboard] /api/server failed:", err)`; do not silently swallow.
- cts-navbar: check `response.status === 401` explicitly; set `_user = null` only in that case; for other errors `console.warn` and still set `_user = null` as a safe fallback, but record `_authFetchFailed` reactive state that a future PR could surface.
- cts-spec-cascade: mirror `cts-plan-list.js` — **add a `response.ok` check first** (the current `_fetchPlans` at `cts-spec-cascade.js:32-42` has no check, so 5xx responses that parse as JSON silently return an error payload without throwing into the catch), then set `_error`, render a short inline message ("Unable to load plans — please reload"), `console.warn(err)`.
- cts-token-manager `_copyToken` and cts-plan-actions `_handleCopyConfig`: wrap `await navigator.clipboard.writeText(...)` in try/catch; on failure, set a `_copyFeedback` aria-live state rendered next to the button ("Copy failed — please select manually"). 2s timeout to clear.

**Patterns to follow:** `cts-token-manager.js` `_error` reactive state + template conditional; aria-live announcement pattern already used in some existing components.

**Test scenarios:**
- *cts-dashboard — error path*: MSW returns 500 for `/api/server` → component still renders its cards; `console.warn` is called (assert via `vi.spyOn(console, 'warn')`).
- *cts-navbar — error path*: MSW returns 500 for `/api/currentuser` → component renders public links; `console.warn` is called; 401 still produces silent public-links render.
- *cts-spec-cascade — error path*: MSW returns 500 for `/api/runner/available` → inline error message is visible in the DOM; no `plans` rendered.
- *Clipboard error path*: Stub `navigator.clipboard.writeText` to reject → `_copyFeedback` text is rendered; aria-live region announced; clears after 2s.

**Verification:**
- Story tests pass. No empty `catch {}` remain in the five files (grep check).

---

- [x] **Unit 4: cts-modal API contract retraction + icon & variant-key fixes**

**Goal:** Make explicit that the inner `.modal` element is the Bootstrap integration point; restore icon support in footer-button descriptors; fix variant-key prefix-strip regression.

**Requirements:** R-C6, R-I2, R-I3

**Dependencies:** None for work order — but Unit 5 also touches `cts-modal.js` JSDoc; land Unit 4 first, then Unit 5 picks up. Unit 8 Sub-theme A also touches `cts-modal.stories.js`; land Unit 4 before Sub-theme A.

**Files:**
- Modify: `src/main/resources/static/components/cts-modal.js` (JSDoc header, `_createButton` around line 190, footer-buttons descriptor schema)
- Modify: `src/main/resources/static/components/cts-modal.stories.js` (add variant-key regression story; add icon-in-footer-button story — note: Unit 8 Sub-theme A also modifies this file for a11y stories; stage Unit 4 first)
- Modify: `src/main/resources/static/templates/privateLinkModals.html` (restore `bi-box-arrow-in-right` icon via the new descriptor `icon` field)
- Create: `docs/solutions/web-components/cts-modal-bootstrap-interop-2026-04-17.md` (API contract doc)

**Approach:**
- **JSDoc + contract doc**: Rewrite cts-modal's class-level JSDoc to say "Renders a Bootstrap-compatible `<div class="modal">` in the light DOM. The inner element is the integration point for `bootstrap.Modal` — do not call `bootstrap.Modal.getOrCreateInstance(ctsModalHostEl)`; it will stop working if the render tree changes." Write the solution doc mirroring `cts-button-host-vs-inner-button-semantics-2026-04-17.md`'s structure (DOM tree, "What works" bullets, "What breaks silently" table, "Operational rule", "Related artifacts").
- **`_createButton` variant-key fix**: replace the ad-hoc `desc.class` prefix-strip with the `_button-classes.js` helper. If the descriptor says `class: "btn-outline-primary"`, pass `variant: "outline-primary"` to `buildButtonClasses` (after widening the helper's lookup tables, or use the existing variant key without munging — see per-unit technical design).
- **`_createButton` icon support**: extend the descriptor shape with `{ icon?: string }`. When present, prepend `<span class="bi bi-${icon}"></span> ` to the button label. Sanitize: allow only `[a-z0-9-]+` to prevent injection via descriptor JSON.
- **privateLinkModals.html**: update the copy button descriptor to include `icon: "box-arrow-in-right"`; verify the rendered button matches the pre-MR visual.

**Patterns to follow:** `_button-classes.js` lookup-table pattern; `cts-button-host-vs-inner-button-semantics-2026-04-17.md` doc structure.

**Technical design** *(directional, not implementation specification)*:

```
Footer-button descriptor shape (extended):
  {
    label: string,
    class?: string,           // e.g. "btn-outline-primary"
    variant?: string,         // preferred; passed straight to _button-classes.js
    icon?: string,            // [a-z0-9-] — Bootstrap icon name
    dismiss?: boolean,
    id?: string,
    data?: Record<string, string>,
  }

Rendering:
  sanitizedIcon := (icon && icon.match(/^[a-z0-9-]+$/)) ? icon : null
  // If sanitization rejects the icon (e.g. "<script>"), OMIT the span entirely —
  // do NOT render `<span class="bi bi-">` which produces an invisible broken icon.
  innerHTML := (sanitizedIcon ? `<span class="bi bi-${sanitizedIcon}" aria-hidden="true"></span> ` : "") + label
  className := buildButtonClasses({ variant: variant || deriveFromClass(class), size })
```

**Test scenarios:**
- *Happy path*: story using `footer-buttons` with `icon: "box-arrow-in-right"` renders a `<span class="bi bi-box-arrow-in-right">` inside the button.
- *Edge case*: descriptor with `class: "btn-outline-primary"` renders a button with the `btn-outline-primary` class intact — NOT `btn-light`. (This is the regression test for R-I3.)
- *Edge case*: descriptor with no `icon` field renders a button with no icon span.
- *Error path*: descriptor with `icon: "<script>"` is rejected by the `^[a-z0-9-]+$` regex; the span is **omitted entirely** (not rendered with an empty `bi bi-` class). Acceptance: `expect(button.querySelector("span.bi")).toBeNull()`; no script tag anywhere in the DOM.
- *Contract*: a new story calls `bootstrap.Modal.getOrCreateInstance(document.querySelector('#myModal .modal'))` and asserts show/hide work. The solution doc explicitly warns against calling `getOrCreateInstance` on the host.

**Verification:**
- privateLinkModals.html private-link modal renders with the box-arrow icon on the Copy button, matching the pre-MR visual.
- `grep -r "btn-outline" src/main/resources/static/components/` produces no silent-downgrade cases.
- The solution doc exists and is linked from `cts-modal.js` JSDoc via `@see`.

---

- [x] **Unit 5: Systematic JSDoc `@property` coverage across 22 components + AGENTS.md sync + lint check**

**Goal:** Restore AGENTS.md §5 compliance: every `cts-*` component has a class-level JSDoc block with `@property` entries, plus `@fires` tags for custom events. Update the AGENTS.md quick-ref table. Add a `npm run lint:jsdoc` check that prevents future regression (per D-8).

**Requirements:** R-C2, R-I13a, R-I13b

**Dependencies:** Unit 4 (cts-modal JSDoc is touched there too) — serialize to avoid merge conflicts.

**Files** *(all under `src/main/resources/static/components/` unless noted)*:
- Modify (22): `cts-navbar.js`, `cts-icon.js`, `cts-form-field.js`, `cts-config-form.js`, `cts-dashboard.js`, `cts-image-upload.js`, `cts-log-entry.js`, `cts-log-viewer.js`, `cts-log-detail-header.js`, `cts-login-page.js`, `cts-plan-list.js`, `cts-plan-actions.js`, `cts-plan-header.js`, `cts-plan-modules.js`, `cts-running-test-card.js`, `cts-spec-cascade.js`, `cts-tabs.js`, `cts-token-manager.js`, `cts-batch-runner.js`, `cts-test-selector.js`, `cts-card.js`, `cts-tooltip.js`
- Modify: `src/main/resources/static/components/AGENTS.md` (quick-ref table rows, §18 cts-tabs hedge removal, cts-card row addition, §5 rule extension for vanilla HTMLElements, §5 enforcement sentence update)
- Create: `frontend/scripts/lint-jsdoc-properties.sh`
- Modify: `frontend/package.json` (add `lint:jsdoc` script; wire into the existing lint aggregate)

**Approach:**
- *Stage A (LitElements, mechanical)*: for each component, read its `static properties = { ... }` and write `@property {type} name - description` for each non-underscore-prefixed key.
- *Stage B (complex types)*: for `cts-log-detail-header.js` add a `@typedef TestInfo` block above the class describing `testInfo`'s shape (status, results, startDate, variant, plan); for `cts-plan-modules.js` document the `modules` array element shape by tracing callers in `cts-plan-detail`; for `cts-config-form.js` treat schema/uiSchema/config/errors as `{Object}` with a one-line description each (JSON-schema shape is documented elsewhere).
- *Stage C (vanilla HTMLElements — `cts-tooltip`, `cts-card`, `cts-modal`, `cts-alert`)*: these have no `static properties` / `observedAttributes` declarations but read attributes imperatively. Document every `getAttribute(...)` call site as a `@property` tag. For `cts-modal` (already has a JSDoc header updated in Unit 4) this means extending the existing block; for the others, add a new class-level JSDoc.
- *Enforcement (D-8)*: add `frontend/scripts/lint-jsdoc-properties.sh` — a POSIX shell script that greps every `cts-*.js` under `src/main/resources/static/components/` (excluding `.stories.js` and `_button-classes.js`) and fails if any file lacks a `@property` tag. Wire into `frontend/package.json` as `"lint:jsdoc"`. Update AGENTS.md §5 final sentence from "This is enforced in code review" to "This is enforced by `npm run lint:jsdoc` (see `frontend/scripts/lint-jsdoc-properties.sh`) AND in code review. The lint check is a presence check; semantic correctness remains a reviewer concern."
- Add `@fires eventName - description` for every `this.dispatchEvent(new CustomEvent(...))` call in the file. Components known to dispatch events per the comment-analyzer report:
  - `cts-image-upload` → `cts-image-uploaded`
  - `cts-log-detail-header` → 7 events
  - `cts-plan-list` → `cts-plan-navigate`
  - `cts-plan-actions` → 8 events
  - `cts-plan-modules` → `cts-run-test`, `cts-download-log`
  - `cts-running-test-card` → `cts-download-log`
  - `cts-spec-cascade` → `cts-plan-selected`
  - `cts-batch-runner` → `cts-run-all`, `cts-run-remaining`
  - `cts-test-selector` → `cts-plan-select`
  - `cts-tabs` → `cts-tab-change`
- Underscore-prefixed state (e.g. `_user`, `_loading`) is intentionally NOT documented (AGENTS.md pattern); cite this briefly in AGENTS.md §5.
- Update AGENTS.md quick-ref table: add rows for all 17 missing components; remove the "(if it had reactive state)" hedge on the cts-tabs line in §18; add a `cts-card` row.

**Patterns to follow:** `src/main/resources/static/components/cts-button.js:4-55` verbatim for the JSDoc block shape.

**Test scenarios:**
- Test expectation: none — JSDoc is documentation metadata; no runtime behavior change.
- Consistency check (manual or grep-based): for each component, number of `@property` tags ≥ number of `static properties` non-underscore-prefixed keys.

**Verification:**
- `npm run lint:jsdoc` passes (no `cts-*.js` component missing `@property`).
- AGENTS.md quick-ref table contains 27 rows.
- `grep -c "@fires" src/main/resources/static/components/cts-plan-actions.js` ≥ 8 (one of the highest-event-dispatch components per the comment-analyzer report).

---

- [x] **Unit 6: Verify cts-plan-* sub-component story coverage; clarify AGENTS.md §6 to match reality**

**Goal:** Confirm R-C3 is already satisfied by colocated stories in `cts-plan-detail.stories.js`, and add a clarifying sentence to AGENTS.md §6 so future contributors don't re-open this question.

**Requirements:** R-C3 (verification pass)

**Dependencies:** None

**Context (verified 2026-04-17):** `src/main/resources/static/components/cts-plan-detail.stories.js` already exports **17 play-function stories** covering all three sub-components — PlanHeader (3: Default, Admin, Published), Modules (5: Default, RunTest, Readonly, Immutable, ReadonlyAndImmutable), Actions (9: ViewConfig, PrivateLink, PrivateLinkValidation, DeletePlan, ImmutablePlan, PublishedPlan, GenerateLinkResult, DeletePlanCancel, CopyConfig). AGENTS.md §6's rule is "every component story must have a `play()` function" — it does not mandate a dedicated stories file per component.

**Files:**
- Modify: `src/main/resources/static/components/AGENTS.md` (add a colocation clarification to §6)

**Approach:**
- Re-grep `cts-plan-detail.stories.js` to confirm the 17 stories are present and each has a `play()` function.
- Add a sentence to AGENTS.md §6: "Stories may live in a parent's `.stories.js` when a sub-component is only rendered as part of a composite (e.g., `cts-plan-header`, `cts-plan-modules`, `cts-plan-actions` are covered in `cts-plan-detail.stories.js`). The rule is play-function coverage per component, not one file per component."

**Test scenarios:**
- Test expectation: none — verification + documentation.

**Verification:**
- `grep -E "^export const (PlanHeader|Modules|Actions)" src/main/resources/static/components/cts-plan-detail.stories.js | wc -l` returns ≥ 17.
- AGENTS.md §6 contains the colocation clarification.

**Escalation path:** If the verification reveals gaps (e.g., a sub-component without any play-function story), escalate by extracting per-component stories — but budget for that only if gaps are found.

---

- [x] **Unit 7: error-paths.spec.js expansion (genuine error-branch coverage)**

**Goal:** Match the filename to the content. Add real end-to-end error-branch tests for the three highest-value paths.

**Requirements:** R-C7

**Dependencies:** None

**Files:**
- Modify: `frontend/e2e/error-paths.spec.js`
- Reference: `frontend/e2e/helpers/routes.js` (setupFailFast, expectNoUnmockedCalls)
- Reference: `frontend/e2e/fixtures/*.js` (existing mock data)

**Approach:**
- Add three new `test(...)` blocks:
  1. `logs.html DataTables 500` — mock `GET /api/log` to return 500 via `page.route`; load `logs.html`; assert the DataTables error indicator (or the fapi.ui.js-managed error modal) is visible.
  2. `log-detail.html /api/info/:id 404` — mock `GET /api/info/:testId` → 404; load `log-detail.html?log=xyz`; assert the "not found" banner is visible.
  3. `schedule-test.html POST /api/plan 400` — happy-path load; fill the form; mock `POST /api/plan` → 400 with an error body; click Submit; assert the validation/error message renders and the user is not navigated away.
- Each test calls `setupFailFast(page)` first and `expectNoUnmockedCalls(page)` at the end.

**Patterns to follow:** `frontend/e2e/log-detail.spec.js` banner-transition tests (cited by the test-analyzer as exemplary).

**Test scenarios:** The unit *is* tests — the scenarios above are the tests themselves.

**Verification:**
- `npm run test:e2e -- e2e/error-paths.spec.js` passes with 4 tests total (1 existing + 3 new). All `expectNoUnmockedCalls` assertions pass.

---

### Phase 2 — Important and minor polish

- [x] **Unit 8: Test-coverage and component polish** (8A/8B/8C shipped; 8D vitest-webkit deferred per T-13)

**Goal:** Land the remaining important and minor findings organized into four sub-themes (A/B/C/D). Each sub-theme is a single commit boundary — reviewers can approve each independently.

**Requirements:** R-I1, R-I4 through R-I12, R-I14, R-M1 through R-M8

**Dependencies:** Unit 3 (Sub-theme C's `cts-navbar` `auth-state` edit must not collide with Unit 3's 401-vs-5xx distinction); Unit 4 (so the cts-modal changes are already in place for R-I1); Unit 5 (JSDoc and AGENTS.md already consistent). Land order: 3 → 4 → 5 → 8 (A → B → C → D).

**Files** — grouped by sub-theme:

*Sub-theme A — cts-modal a11y & keyboard coverage (R-I1):*
- Modify: `src/main/resources/static/components/cts-modal.stories.js`

*Sub-theme B — Remaining story + E2E gaps (R-I5, R-I6, R-I10, R-I11, R-I12, R-M4, R-M5, R-M6):*
- Modify: `src/main/resources/static/components/cts-form-field.stories.js`
- Modify: `src/main/resources/static/components/cts-tabs.stories.js`
- Modify: `src/main/resources/static/components/cts-log-entry.stories.js`
- Modify: `src/main/resources/static/components/cts-tooltip.stories.js`
- Modify: `frontend/e2e/tokens.spec.js`
- Modify: `frontend/e2e/clipboard.spec.js`
- Modify: `frontend/e2e/journeys.spec.js`
- Modify: `frontend/stories/fixtures/msw-handlers.js`

*Sub-theme C — Component polish (R-I4, R-I7, R-I8, R-I9, R-I14, R-M1, R-M2, R-M3, R-M7):*
- Modify: `src/main/resources/static/js/fapi.ui.js` (four catch-all sites)
- Modify: `src/main/resources/static/components/cts-tooltip.js` (dynamic-init timing)
- Modify: `src/main/resources/static/components/cts-config-form.js` (invalid-JSON indicator state + template)
- Modify: `src/main/resources/static/components/cts-badge.js` (slot preservation — either a fragment-cache fix or a clearer JSDoc)
- Modify: `src/main/resources/static/components/cts-login-page.js` (add `import "./cts-link-button.js";`)
- Modify: `src/main/resources/static/components/cts-navbar.js` (accept an `auth-state` attribute — `anonymous`/`authenticated`/`unknown` — that pages with known state set to avoid the `/api/currentuser` call; default to current fetch behavior when unset)
- Modify: `src/main/resources/static/login.html` (add `auth-state="anonymous"` on `<cts-navbar>`)
- Modify: `src/main/resources/static/components/cts-button.js` (simplify `${this.label ? this.label : nothing}` → `${this.label}`)
- Modify: `src/main/resources/static/components/cts-log-detail-header.js` (consistent optional chaining: either all optional or all direct, with a `_testInfoReady` guard in render if going direct)
- Modify: `src/main/resources/static/components/cts-link-button.js` (align icon comment with cts-button.js:86-87)

*Sub-theme D — CI & misc (R-M8):*
- Modify: `frontend/vitest.config.js` (add a webkit smoke config — minimal, only the critical-path stories)

**Approach:**

*Sub-theme A — cts-modal a11y:* add stories `EscapeDismisses`, `StaticBackdropNoKeyboard` (negative — Escape does NOT close), `AriaAttributes` (asserts `role="dialog"`, `aria-modal="true"`, `aria-labelledby` pointing to the title id), `FocusReturnsToTrigger` (opens via button click, closes, asserts focus returns to the trigger button).

*Sub-theme B:* cts-form-field `WithError` gets three variants: `WithErrorTextarea`, `WithErrorSelect`, `WithErrorCheckbox`, each asserting `.is-invalid` on the correct inner element. cts-tabs `KeyboardNavigation` is extended (or a new `KeyboardActivation` story added) exercising Enter and Space keys; the play asserts `cts-tab-change` was dispatched. tokens.spec.js delete test gains a `expect(row).toBeHidden()` (or `toHaveCount(0)`) assertion on the deleted-id row selector. clipboard.spec.js spy installation is refactored to install BEFORE `page.goto` and to assert the spy was called at least once before the first test assertion. journeys.spec.js gains a `"plan creation fails → user stays on schedule-test with error"` test. MSW handlers for publish/makemutable/delete/uploadimage/certificationpackage/delete-token gain minimal body-shape checks (assert expected params present; return 400 if missing). cts-log-entry.stories `BlockEntry` gains a border-color assertion. cts-tooltip.stories `BottomPlacement` asserts the rendered tooltip popup's `getBoundingClientRect().top` > trigger's `top` after show.

*Sub-theme C:* fapi.ui.js 4 sites add `console.warn` with the site name (e.g. `"[fapi.ui.js:462 currentuser]"`); no behavioral change. cts-tooltip's `connectedCallback` replaces the `:scope > *` selector with `:scope *` and guards the bootstrap init with a MutationObserver that fires when the first child arrives (bounded to 2 seconds via `this._tooltipWaitTimer`). cts-config-form's invalid-JSON branch sets `_jsonError = "Invalid JSON — configuration not updated"` rendered next to the textarea. cts-badge: if any caller in the repo re-renders attributes post-mount (grep check), implement the detached-fragment cache; otherwise, JSDoc the contract ("slot captured once; attribute re-render discards subsequent children"). cts-login-page adds the explicit import. cts-navbar accepts `auth-state` attribute; `login.html` and any page with known anonymous state sets it. cts-button template simplification. cts-log-detail-header: add `_testInfoReady` guard at render entry and drop all optional-chaining in the rendered block (consistent with the guarded-assumption style). cts-link-button comment alignment.

*Sub-theme D:* vitest.config.js gains an additional `project` entry for `"chromium"` (existing) and `"webkit"` (smoke — limited to 3-5 critical-path story files). Webkit is not expected to block CI; it is a signal.

**Patterns to follow:** existing story shapes, existing E2E helpers, cts-token-manager's `_error` pattern for the invalid-JSON indicator.

**Test scenarios** *(unit-level — most sub-themes are test additions themselves; call out only non-test code changes below)*:

- *fapi.ui.js*: Test expectation: none — stubbing console.warn in existing E2E tests is sufficient; no new tests needed for log lines alone.
- *cts-tooltip dynamic init*: New story `DynamicallyAttached` — creates a `<cts-tooltip>` via JS after connect, appends a `<button>` child, asserts that after a microtask flush the tooltip instance is initialized.
- *cts-config-form invalid JSON*: New story `InvalidJsonIndicator` — play enters malformed JSON into the JSON tab, asserts the error message renders and the form values are not mutated.
- *cts-navbar auth-state attribute*: New story `KnownAnonymous` — renders `<cts-navbar auth-state="anonymous">` and asserts no `/api/currentuser` request is issued (spy on fetch / assert MSW handler counter).
- *cts-badge slot preservation*: if implementing the fix, add a story that mutates an observed attribute after mount and asserts slot children still render; if JSDoc-only, skip.
- *cts-login-page explicit import*: Test expectation: none — existing stories continue to work; verification is static (grep for `import "./cts-link-button.js"`).
- *vitest webkit*: Test expectation: the webkit smoke run passes on the 3-5 critical-path story files. If it fails flakily, gate it to nightly.

**Verification:**
- `npm run test-storybook` passes with the new stories.
- `npm run test:e2e` passes all journeys and updated tokens/clipboard specs.
- Storybook renders all existing components unchanged visually.
- `grep "console.warn" src/main/resources/static/js/fapi.ui.js` shows the new log lines present.

---

## System-Wide Impact

- **Interaction graph:** Phase 1 Unit 3 changes the error-fallback semantics in `cts-navbar` subtly — pages that render it while offline/broken now still render a public-nav but also log a warning. `cts-login-page` already knows anonymous state; Unit 8 Sub-theme C passes that down via `auth-state`, which silences the 401 on that page.
- **Server-observable change (no server code change):** Unit 8 Sub-theme C's `auth-state="anonymous"` on `login.html`'s navbar suppresses one `/api/currentuser` call per login-page load. Operators monitoring 401 rates on that endpoint will see a drop. No Java-side changes.
- **Error propagation:** Data-fetch errors now produce `console.warn` at every caught site. Some components additionally surface a UI error state (`cts-spec-cascade`, `cts-log-viewer`). `cts-dashboard` and `cts-navbar` deliberately keep the silent-UI fallback (they're always-rendered chrome); only the log is added.
- **State lifecycle risks:** Unit 2's `AbortController` and `isConnected` guard touch `cts-log-viewer`'s lifecycle — verify the component handles rapid mount/unmount cycles (Storybook hot-reload is a good proxy). Unit 6's new stories mount-render-unmount the three plan sub-components with fixture props; the fixtures must be complete enough to avoid null-deref during unmount.
- **API surface parity:** cts-modal's backward-compat *retraction* (Unit 4) affects any future caller that assumed `bootstrap.Modal.getInstance(ctsModalHostEl)` works. The solution doc and JSDoc update make the contract explicit but do NOT break existing pages (which already pass the inner `.modal` div, because of the id transfer). This is a documentation/expectation change, not a breaking change.
- **Integration coverage:** The journeys.spec.js addition (Sub-theme B) is the only new cross-page integration test; most other additions are component-level Storybook play tests.
- **Unchanged invariants:** All public HTML page markup (other than `privateLinkModals.html` icon restoration and `login.html` adding `auth-state="anonymous"`) is unchanged. The cts-modal element's external DOM (the inner `<div class="modal">` with transferred id) is unchanged — only the documented contract changes.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| JSDoc pass (Unit 5) drifts if the HTML-to-WC migration plan (`docs/plans/2026-04-16-002-refactor-html-to-web-components-plan.md`) adds new components in parallel | Land Unit 5 as a single atomic commit AFTER Unit 4. Monitor the migration plan's branches; if a new component merges before Unit 5, add its JSDoc in the same Unit 5 commit. |
| cts-modal contract retraction surprises a reviewer expecting the backward-compat claim to be honored | Call it out explicitly in the MR description update; the solution doc provides the rationale reviewers can cite. |
| Unit 2's `AbortController` / `isConnected` change introduces a subtle race that existing E2E tests don't catch | Add the disconnect-mid-fetch test scenario explicitly (already listed). Exercise manually in Storybook with HMR a few times. |
| Unit 8 is large (one commit per sub-theme, but still one unit) and reviewers fatigue | Split sub-themes A-D into separate commits so the reviewer can page through logically. Keep each commit under ~300 lines. |
| MSW handler body-shape checks (Sub-theme B) break existing stories that send incomplete payloads | Run the Storybook test suite immediately after the MSW change; fix the payload on the caller side rather than loosening the handler. |
| Unit 6's three new stories reveal real bugs in cts-plan-* subcomponents (since they were effectively untested before) | Budget +1h of unplanned work per sub-component for triage; if findings are non-blocking, file follow-up issues. |

## Documentation / Operational Notes

- Update the MR !1911 description to link to this plan and note the scope boundaries.
- Unit 4 creates `docs/solutions/web-components/cts-modal-bootstrap-interop-2026-04-17.md` — cross-link from `cts-button-host-vs-inner-button-semantics-2026-04-17.md` via `related-solutions:`.
- Unit 5 updates `src/main/resources/static/components/AGENTS.md` — after merge, the quick-ref table is the single source of truth for component-list status.
- No rollout flagging needed; all changes are front-end and ship with the next build.

## Known Caveats & Inline TODOs (from 2026-04-17 document review)

The document-review pass surfaced findings beyond the three P0 blockers that were resolved above. These are **deferred to implementation-time judgment** — each one has a concrete call to make once the implementer is in the file. Address in context as part of the relevant Unit.

### Unit 3 — error handling and clipboard UX

- **T-1** *(design-lens, P1)* — Clipboard failure aria-live needs: (a) `aria-live="polite"`, (b) minimum 5s display (2s is likely too short for a screen-reader announcement to complete), (c) explicit handling when `navigator.clipboard` is absent (not just rejected), (d) visual placement spec — render as `.text-danger.small` within the same flex container as the Copy button so it is visually adjacent, not an inline footnote.
- **T-2** *(design-lens, P1)* — `cts-spec-cascade` error state is indistinguishable from externally-set `plans=[]`. Add distinct copy: empty-not-error ("No plans are available for this profile yet") vs. load-failed ("Unable to load plans — please reload"). Use `_error` state to route between them.
- **T-3** *(feasibility, P2)* — `_authFetchFailed` reactive state is written but never read. Either surface a small banner now ("Authentication check failed — your session may have expired") or drop the field and rely on `console.warn` alone. Don't introduce dead reactive state.
- **T-4** *(adversarial, residual)* — `console.warn` is the diagnosability tool for five call sites. Not stress-tested against test-environment noise or production log aggregation. Consider: the existing E2E helpers don't assert on console output; confirm new warnings don't fail `expect-no-console-errors`-style checks if they exist.

### Unit 2 — cts-log-viewer lifecycle

- **T-5** *(feasibility, residual)* — The `isConnected` guard must be placed **inside** the `finally` block, immediately before the `setTimeout` call — not at the top of `_fetchEntries` (the in-flight fetch has already started). `AbortController` is optional polish; skip if not straightforward.

### Unit 4 — cts-modal

- **T-6** *(design-lens, P1)* — `aria-modal="true"` is NOT currently set on the inner `.modal` div. Decide at implementation: (a) set it in `connectedCallback` after the div is attached (component takes responsibility), (b) set it only when Bootstrap's modal plugin `show()` is called (matches Bootstrap's own behavior), or (c) drop the assertion. Same decision applies to `aria-describedby`. Document the choice in the new cts-modal solution doc.
- **T-7** *(feasibility + design-lens, P1)* — `_button-classes.js` `VARIANT_CLASSES` lookup table does NOT contain outline variants. For the `btn-outline-primary` fix (R-I3), choose one:
  - (a) Add 8 outline entries (`outline-primary`, `outline-secondary`, `outline-danger`, `outline-info`, `outline-light`, `outline-dark`, `outline-success`, `outline-warning`) to `VARIANT_CLASSES`.
  - (b) Create a sibling `OUTLINE_VARIANT_CLASSES` table and fall through.
  - (c) When `desc.class` is passed and its stripped key is not found in `VARIANT_CLASSES`, pass `desc.class` verbatim as the class attribute (bypass the helper).
  - Option (a) is simplest and makes `cts-button`/`cts-link-button` newly accept outline variants — which may be desired but expands scope. Option (c) minimizes blast radius.

### Unit 7 — error-paths.spec.js

- **T-8** *(design-lens, P1)* — Each of the three new error-branch tests should include one "realistic next user action" assertion: (a) `schedule-test 400`: form fields retain their values (test by filling, submitting, asserting a field still contains its value), (b) `log-detail 404`: a back-link or reload affordance is visible, (c) `logs 500`: retry button is offered or DataTables re-tries after a period.

### Unit 8 — scope and structure

- **T-9** *(scope-guardian + adversarial, P1)* — **Sub-theme C `auth-state` attribute is over-engineered for R-M1 (a Minor finding).** Alternatives to evaluate at implementation time:
  - (a) One-line path check in `cts-navbar._fetchUser`: `if (window.location.pathname.endsWith("/login.html")) { this._loading = false; return; }` — zero new API surface.
  - (b) `_skipFetch` reactive state set from outside via imperative assignment — minimal API.
  - (c) The `auth-state` attribute as currently planned — full tri-state public attribute.
  - If (a) is chosen, delete the `auth-state` story, the `login.html` attribute addition, and the JSDoc `@property auth-state` from Unit 5's scope. **Note also (design-lens F6):** the tri-state doesn't model `_loading`; if (c) is kept, define `auth-state="anonymous"` as synchronously setting `_loading = false` and `_user = null` without a loading placeholder, and assert that in the story.
- **T-10** *(scope-guardian + adversarial, P1)* — Consider decomposing Unit 8 into **8A / 8B / 8C / 8D** as distinct units (rather than sub-themes of one unit). A/B/D are low-risk (tests, CI); C is behavior change. Reviewers can then approve A/B/D quickly and engage more deeply with C.
- **T-11** *(feasibility, P2)* — Sub-theme B MSW handler tightening: before tightening, run `grep -rn "publish\|makemutable\|uploadimage\|certificationpackage" src/main/resources/static/components/*.stories.js` to enumerate which stories send payloads to these handlers; fix caller-side shape first to avoid breaking existing stories.
- **T-12** *(design-lens, P1)* — Sub-theme B `cts-tabs` Enter/Space addition: choose WCAG pattern explicitly. Recommendation: keep current automatic activation (arrow keys select) and add Enter/Space as no-op aliases that simply dispatch `cts-tab-change` for the currently focused tab (defensive redundancy, not a new pattern). Document in the story.
- **T-13** *(scope-guardian + feasibility, P2)* — **Consider deferring R-M8 (vitest webkit)** to a separate CI-improvement issue. The multi-project vs multi-instance structure, the `test-storybook` npm-script update, and the "gate to nightly if flaky" conditional are more than "polish" scope. If kept in-scope, specify: a single project with `instances: [{browser: "chromium"}, {browser: "webkit"}]` limited via `include:` to 3-5 story files, and update `"test-storybook"` script accordingly.

### Cross-cutting

- **T-14** *(feasibility, residual)* — R-I14 (cts-login-page missing import) verification is static-only. Spot-check by loading `login.html` in the browser with a fresh cache and observing the DOM; grep alone won't catch load-order issues.

---

## Sources & References

- **PR review output:** `/pr-review-toolkit:review-pr` run on 2026-04-17 in this conversation (findings reproduced in the Requirements Trace).
- **MR:** https://gitlab.com/openid/conformance-suite/-/merge_requests/1911 (commits `83e5d58...f539bff`)
- **Exemplars:**
  - `src/main/resources/static/components/cts-button.js:4-55` (JSDoc)
  - `src/main/resources/static/components/cts-token-manager.js` (error handling)
  - `src/main/resources/static/components/cts-button.stories.js` (stories shape)
  - `frontend/e2e/helpers/routes.js` (fail-fast)
  - `docs/solutions/web-components/cts-button-host-vs-inner-button-semantics-2026-04-17.md` (solution doc template)
- **Related plans:**
  - `docs/plans/2026-04-13-002-feat-web-components-poc-plan.md` (original POC plan)
  - `docs/plans/2026-04-15-001-feat-cts-modal-enhancement-plan.md` (prior cts-modal work)
  - `docs/plans/2026-04-16-002-refactor-html-to-web-components-plan.md` (ongoing HTML-to-WC migration)
- **CLAUDE.md conventions:** frontend E2E section (route-ordering); "no dynamic class concatenation" memory rule.
- **AGENTS.md §5 and §6** in `src/main/resources/static/components/AGENTS.md` (JSDoc and play-function rules).
