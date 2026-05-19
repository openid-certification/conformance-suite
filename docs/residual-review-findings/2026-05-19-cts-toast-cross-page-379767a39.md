# Residual Review Findings — cts-toast cross-page wiring

**Branch:** feat/redesign
**Commits:** ab0900918 (feat) + 379767a39 (review autofix)
**Plan:** [`docs/plans/2026-05-18-011-feat-wire-cts-toast-cross-page-plan.md`](../plans/2026-05-18-011-feat-wire-cts-toast-cross-page-plan.md)
**Board:** [`docs/plans/2026-05-18-006-orphan-components-wire-up-board.md`](../plans/2026-05-18-006-orphan-components-wire-up-board.md) row 5
**ce-code-review run artifact:** `/tmp/compound-engineering/ce-code-review/20260519-100554-0917b9e2/`
**Date:** 2026-05-19

## Source

LFG pipeline run on `feat/redesign` with `mode:autofix`. ce-code-review dispatched 7 reviewers (correctness, testing, maintainability, project-standards, agent-native, learnings, julik-frontend-races). After dedup + cross-reviewer promotion + confidence gate, 10 findings survived. 6 were `safe_auto` and committed in 379767a39. The 4 below are `gated_auto` / `manual` and were not autofixed.

`gh pr view` is unavailable in this GitLab checkout, so this file is the durable record. The corresponding GitLab MR is !1998 (https://gitlab.com/openid/conformance-suite/-/merge_requests/1998).

## Residual Review Findings

### #7 — `CtsToastHost.getOrCreate` lacks `document.body` null guard on auto-create path

**Severity:** P2 — **autofix_class:** gated_auto — **owner:** downstream-resolver
**File:** `src/main/resources/static/components/cts-toast.js:186` (the `static getOrCreate()` method)
**Reviewer:** julik-frontend-races (confidence 75)

`CtsToastHost.getOrCreate()` calls `document.body.appendChild(host)` unconditionally. If invoked before `<body>` has been parsed — e.g. from an inline `<head>` script or a synchronous module-evaluation path — `document.body` is `null` and the call throws a `TypeError`. Every current caller sits inside a `DOMContentLoaded` handler or an interactive callback, so the path is not currently reachable. However, `window.ctsToast` is now a global and `CtsToastHost.show` is an exported static — the next developer who calls either from a `<head>` inline script or from a synchronous import at module evaluation time will hit this.

**Suggested fix:** defer the append when `<body>` is not yet present:

```js
static getOrCreate() {
  let host = document.querySelector('cts-toast-host');
  if (!host) {
    host = document.createElement('cts-toast-host');
    if (document.body) {
      document.body.appendChild(host);
    } else {
      document.addEventListener('DOMContentLoaded',
        () => document.body.appendChild(host), { once: true });
    }
  }
  return host;
}
```

Why this was not autofixed: behavioral change to a pre-existing component outside the commit's direct scope. Worth a separate small PR or fold into the next cts-toast touch.

---

### #8 — R1 cross-page coverage gap: 9 wired pages lack `cts-toast-host` presence assertion

**Severity:** P2 — **autofix_class:** manual — **owner:** downstream-resolver
**File:** `frontend/e2e/upload.spec.js:210` (the only host-presence assertion) + 9 other specs that should mirror it
**Reviewer:** testing, project-standards (cross-reviewer; confidence promoted to 100)

The commit mounts `<cts-toast-host>` on 10 pages (index, login, logs, plans, plan-detail, log-detail, running-test, schedule-test, tokens, upload). Only `upload.spec.js` asserts the host exists. A future accidental removal from, e.g., `logs.html` would pass all tests silently.

**Suggested fix:** add `await expect(page.locator("cts-toast-host")).toHaveCount(1);` to at least one existing test in each of the 9 other specs. Spot-checking `home.spec.js` (index.html) and `logs.spec.js` first covers the two most different page shapes without adding new test files. The full fan-out is mechanical but tedious; a single follow-up PR can sweep all 9.

Why this was not autofixed: requires careful selection of the right host test per spec (existing tests vary by setup and may have pre-existing flakes per the project memory). Best handled as a focused PR.

---

### #9 — R3 error kind is not tested via the `window.ctsToast` global path

**Severity:** P3 — **autofix_class:** gated_auto — **owner:** downstream-resolver
**File:** `src/main/resources/static/components/cts-toast.stories.js` (existing `ErrorKind` story at ~line 182, plus potential new variant in `ViaWindowApi` or a dedicated `ViaWindowApiError`)
**Reviewer:** testing (confidence 75)

The existing `ErrorKind` story exercises `kind="error"` via a static HTML attribute, bypassing `window.ctsToast` and `CtsToastHost.show` entirely. The new play-function stories (`ViaWindowApi`, `Persistent`) both pass `kind:"ok"`. If a future refactor silently dropped `kind` from `CtsToastHost.show`'s option propagation, `ErrorKind` would still pass.

**Suggested fix:** add a `ViaWindowApiError` story (or extend `ViaWindowApi` with an `kind:"error"` call) and assert the rust left-rule (`var(--rust-400)`) and `close-circle` glyph appear via the global API path.

Why this was not autofixed: design call — whether to add a third sibling story or fold into existing one. Author preference.

---

### #10 — E2E auto-dismiss assertion is time-coupled to the 5000ms default duration

**Severity:** P3 — **autofix_class:** manual — **owner:** downstream-resolver
**File:** `frontend/e2e/upload.spec.js:235` (the `toHaveCount(0, { timeout: 6000 })` assertion)
**Reviewer:** testing, agent-native (cross-reviewer; confidence 75)

The new upload-toast e2e test waits up to 6 s for the toast to auto-dismiss, adding ~5.5 s of wall-clock to every CI run. If the component default duration were raised above ~5800ms the assertion would flake; lowered, it would be silently slow.

**Suggested fix (option A — accept cost):** document the coupling explicitly in the test comment (the autofix round already softened the comment but did not address the time cost).
**Suggested fix (option B — eliminate cost):** plumb a `duration: 500` override into the dispatched `cts-image-uploaded` event handler (e.g. read `event.detail?.toastDurationOverride` in `upload.html` for test-only injection) so the auto-dismiss completes in <1 s and the test timeout can drop to 1500ms.

Why this was not autofixed: design call — option A keeps the production path clean but accepts the wall-clock cost; option B compromises production code for test speed. Maintainer call.

---

## Advisory / observational (no action required)

- **agent-native obs #2:** no post-dismiss history API on `CtsToastHost`. Out of scope for this commit; revisit if a future agent workflow needs to read dismissed toast content.
- **maintainability M3:** 10-page repetition with no extraction point. Architectural ceiling — defer until a second page-wide chrome element arrives.
- **learnings #1-5:** institutional precedents (cts-copy-flash, cts-button host learning, lit-importmap drift spec, async-init Playwright pattern, bridging anti-loop rule). All confirm conventions; no changes needed.

## Pre-existing (not part of this commit's scope)

- `cts-toast.js:238` — `CtsToast` Lit property `title` shadows `HTMLElement.prototype.title`. Pre-existing in the component; unchanged by this commit.
