# Residual Review Findings — cts-toast residuals fix

**Branch:** feat/redesign
**Commits:** a7366381a (fix) + fe34323f6 (review autofix)
**Plan:** [`docs/plans/2026-05-19-001-fix-cts-toast-residual-findings-plan.md`](../plans/2026-05-19-001-fix-cts-toast-residual-findings-plan.md)
**Parent residual doc:** [`docs/residual-review-findings/2026-05-19-cts-toast-cross-page-379767a39.md`](2026-05-19-cts-toast-cross-page-379767a39.md)
**ce-code-review run artifact:** `/tmp/compound-engineering/ce-code-review/20260519-115027-18a2f7dd/`
**GitLab MR:** !1998 (https://gitlab.com/openid/conformance-suite/-/merge_requests/1998)
**Date:** 2026-05-19

## Source

LFG pipeline run on `feat/redesign` resolved the 4 prior residual findings (#7-#10 from the parent doc). ce-code-review with `mode:autofix` dispatched 7 reviewers (correctness, testing, maintainability, project-standards, agent-native, learnings, julik-frontend-races). After dedup + cross-reviewer promotion + confidence gate, 3 actionable findings survived. 1 was `safe_auto` and committed in fe34323f6. The 2 below are `gated_auto` and were not autofixed.

`gh pr view` is unavailable in this GitLab checkout, so this file is the durable record. The corresponding GitLab MR is !1998.

## Residual Review Findings

### R-1 — `CtsToastHost.getOrCreate()` singleton broken on double call before `DOMContentLoaded` — **RESOLVED**

**Status:** Resolved on `feat/redesign` (2026-05-19) — see the **Resolution** section below.
**Severity:** P1 — **autofix_class:** gated_auto — **owner:** downstream-resolver — **anchor:** 100
**File:** `src/main/resources/static/components/cts-toast.js:200` (the `static getOrCreate()` method, specifically the `else { document.addEventListener("DOMContentLoaded", ...) }` branch)
**Reviewers:** correctness, julik-frontend-races (cross-reviewer; confidence promoted to 100)

This is a **race I introduced** while fixing finding #7 from the parent doc. The new null-body guard creates a host and queues a `DOMContentLoaded` listener — but does not insert the host into the DOM until that listener fires. A second synchronous call to `getOrCreate()` before `DOMContentLoaded` does `document.querySelector("cts-toast-host")`, which returns `null` (the first host is detached from the DOM), creates a second host, and queues a second listener. After `DOMContentLoaded` both listeners fire and **two sibling `<cts-toast-host>` nodes land in `<body>`**, breaking the singleton invariant for the entire page lifetime.

The `GetOrCreateIdempotent` Storybook story cannot reproduce this — Storybook play functions run with `document.body` already present, so the race condition only fires when `<head>`-time inline scripts double-call the API. No production caller currently does this, but the residual finding #7 in the parent doc explicitly noted that `<head>`-time callability was the future-facing reason for adding the guard.

**Suggested fix (both reviewers converged on this shape):**

```js
let _pendingHost = null;

static getOrCreate() {
  let host = /** @type {CtsToastHost | null} */ (document.querySelector("cts-toast-host"));
  if (!host && _pendingHost) {
    host = _pendingHost;
  }
  if (!host) {
    host = /** @type {CtsToastHost} */ (document.createElement("cts-toast-host"));
    if (document.body) {
      document.body.appendChild(host);
    } else {
      _pendingHost = host;
      document.addEventListener(
        "DOMContentLoaded",
        () => {
          document.body.appendChild(host);
          _pendingHost = null;
        },
        { once: true },
      );
    }
  }
  return host;
}
```

**Verification:** Add a Storybook play function that stubs the `document.body` getter (e.g. via a `Object.defineProperty(document, "body", { get: () => null, configurable: true })` wrapper, restored at end of play()), calls `getOrCreate()` twice, fires a synthetic `DOMContentLoaded`, and asserts `document.querySelectorAll("cts-toast-host").length === 1`. The plan's own U1 open question (the null-body Storybook test fallback) explicitly accepted this gap; a follow-up PR should close it now that the race is named.

**Why this was not autofixed:** Introduces a module-scoped `_pendingHost` variable — a behavioral change beyond the original 1-line alias removal that ce-code-review's autofix gate handles. Worth a small focused PR.

#### Resolution (2026-05-19)

Fix landed as a TDD-driven change on `feat/redesign`:

- **Test first:** `GetOrCreateNullBodyRace` Storybook play function in `src/main/resources/static/components/cts-toast.stories.js`. Stubs `document.body` to `null` via `Object.defineProperty(document, "body", { get: () => null, configurable: true })`, calls `CtsToastHost.getOrCreate()` twice, asserts `first === second` (in-memory singleton), restores `document.body` via `Reflect.deleteProperty`, dispatches a synthetic `DOMContentLoaded`, and asserts `document.querySelectorAll("cts-toast-host").length === 1` (visible DOM-count singleton). Verified RED before the fix — failed at the `expect(first).toBe(second)` line with `expected <cts-toast-host> to be <cts-toast-host> // Object.is equality`.
- **Fix:** module-level `let _pendingHost = null;` cache consulted before `querySelector` when the cached host is set but not yet in the DOM. The deferred-append `DOMContentLoaded` listener clears `_pendingHost = null` after `appendChild`. Shape matches the suggested fix above verbatim; the only addition is a `/** @type {CtsToastHost | null} */` JSDoc annotation so TypeScript narrows the cache cleanly.
- **Bonus:** the closure-narrowing TS2345 at the legacy `cts-toast.js:219` (catalogued in memory as `feedback_cts_toast_typecheck_pre_existing.md`) is also resolved by holding the freshly-created host in a typed `const fresh` local — the deferred listener now captures a value TS knows is non-null. `npm run test:ci` now runs end-to-end on this branch (0 errors).
- **Verification:** `npm run test:ci` clean (0 errors, pre-existing warnings only). All 11 cts-toast stories pass. `upload.spec.js` e2e (7 tests, including the 5.5 s success-toast wire-up) passes.

---

### R-2 — 9 e2e specs duplicate the same cross-page-contract comment + assertion

**Severity:** P2 — **autofix_class:** gated_auto — **owner:** downstream-resolver — **anchor:** 75
**Files:** `frontend/e2e/home.spec.js`, `frontend/e2e/login.spec.js`, `frontend/e2e/logs.spec.js`, `frontend/e2e/plans.spec.js`, `frontend/e2e/plan-detail.spec.js`, `frontend/e2e/log-detail.spec.js`, `frontend/e2e/running-test.spec.js`, `frontend/e2e/schedule-test.spec.js`, `frontend/e2e/tokens.spec.js` (target: new helper in `frontend/e2e/helpers/assertions.js`)
**Reviewer:** maintainability (confidence 75)

Each of the 9 spec edits from finding #8 carries the same 3-line `Cross-page contract: every wired page mounts a single <cts-toast-host>...` comment, varying only in the page name, plus the same `toHaveCount(1)` assertion. The line-number anchor `(Mirrors upload.spec.js:210.)` will silently become stale if `upload.spec.js` is reorganised. The project already hosts shared helpers in `frontend/e2e/helpers/assertions.js` (e.g. `assertNoIdCollisions`, `assertLabelInputPairing`) — adding `expectToastHostMounted(page)` there matches the established pattern.

**Suggested fix:**

```js
// frontend/e2e/helpers/assertions.js
/**
 * Cross-page contract: every wired page mounts a single <cts-toast-host>
 * for window.ctsToast(...). A silent removal of the mount from any wired
 * page would otherwise pass all tests in that spec.
 *
 * @param {import('@playwright/test').Page} page
 */
export async function expectToastHostMounted(page) {
  await expect(page.locator("cts-toast-host")).toHaveCount(1);
}
```

Then each spec becomes a single `await expectToastHostMounted(page);` call after `page.goto(...)`. Net delta: ~27 lines removed from the 9 specs, the line-number anchor disappears, and the rationale lives in one place where it can be updated.

**Why this was not autofixed:** Architectural choice — whether to extract the helper, where it belongs, and what to name it. Mechanical sweep across 10 files. Best handled as a focused refactor PR after R-1 lands.

---

## Advisory / observational (no action required, deferred from parent doc)

- **Post-dismiss toast history API.** Re-flagged by the agent-native reviewer in this round. Still out of scope; revisit if a future agent workflow needs to read dismissed toast content. Tracked in the parent residual doc.
- **`updateComplete` asymmetry between `ViaWindowApi` and `ViaWindowApiError` stories.** Intentional but fragile — a future maintainer copying `ViaWindowApi`'s pattern for a child-querying story may omit the await. Consider a module-scope JSDoc note in `cts-toast.stories.js`.
- **`DOMContentLoaded` callback assumes `document.body` exists at fire time.** YAGNI in normal browsers; matters only for SSR / test-harness contexts that synthesize the event before body exists.

## Pre-existing (not part of this commit's scope)

- **`upload.html:113` — `console.warn` fallback when `window.ctsToast` is unavailable is not agent-observable.** Added in the original cross-page wiring commit (ab0900918), not by this diff. An automation agent exercising upload without `cts-toast-api.js` loading (module fetch failure, broken import path) would see a green test while the user sees no upload confirmation. Worth a separate PR that either (a) promotes the fallback to `FAPI_UI.showError` so it's DOM-observable, or (b) adds a Playwright `page.on('console', ...)` assertion in upload.spec.js.

## Testing gaps (acknowledged)

- ~~**The `document.body === null` branch in `CtsToastHost.getOrCreate()` has no automated test.**~~ **Closed (2026-05-19).** The R-1 resolution above adds `GetOrCreateNullBodyRace` to `cts-toast.stories.js`, exercising both the in-memory singleton invariant and the visible DOM-count invariant after a synthetic `DOMContentLoaded`. The 4-reviewer flag (correctness, testing, maintainability, julik-frontend-races) is now satisfied.

## CI Status

GitLab pipeline on commit fe34323f6 (https://gitlab.com/openid/conformance-suite/-/merge_requests/1998): pending at publication time of this doc. The 18 pre-existing baseline e2e failures catalogued in `feedback_e2e_pre_existing_failures_2026_05_18.md` are expected to persist; the 9 new `toHaveCount(1)` assertions added by this PR were verified locally on 3 stable specs (home, logs, tokens) and the upload e2e smoke test still passes at 5.5s.

Local verification:
- `cd frontend && npm run test:ci` is green (0 errors, pre-existing warnings only)
- `cd frontend && npm run test-storybook -- cts-toast` is green (10/10 tests pass, including the 2 new `ViaWindowApiError` and `GetOrCreateIdempotent` stories)
- Smoke check on `home.spec.js`, `logs.spec.js`, `tokens.spec.js`, `upload.spec.js`: all 4 pass with the new assertion

**Zero regressions from this PR.** No fix path within scope — R-1 introduces a module-scoped variable that needs a paired Storybook test, and R-2 is a cross-file refactor; both are best handled as separate focused PRs.
