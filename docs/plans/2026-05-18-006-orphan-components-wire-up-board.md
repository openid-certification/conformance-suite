---
title: "Orphan cts-* components — wire-up execution board"
type: board
status: active
date: 2026-05-18
branch: feat/redesign
brainstorm: docs/brainstorms/2026-04-25-oidf-design-system-implementation-requirements.md
---

# Orphan cts-* components — wire-up execution board

Execution tracker for the six `cts-*` web components that exist with full Storybook coverage but are not yet wired into any HTML page. Each row links to a self-contained plan; the plans are independent and can run in any order, but the recommended sequence (top-to-bottom) is **risk × payoff** — highest-payoff and least-ambiguous wiring first.

Use with `/lfg` — when the user says **"lfg the next orphaned component"**, scan this board top-to-bottom, pick the first `[ ]` row, and execute the linked plan. Check the box when the unit's MR merges to `feat/redesign`.

The brainstorm at [`docs/brainstorms/2026-04-25-oidf-design-system-implementation-requirements.md`](../brainstorms/2026-04-25-oidf-design-system-implementation-requirements.md) is the design source of truth (Phase D — Per-page migration off Bootstrap). This file is only a state tracker — do not duplicate plan content here.

---

## How to use

1. **Pick the next item.** Find the first `[ ]` row in the table below. The board does not encode hard dependencies — each plan is independent — but the order is recommended.
2. **Open the linked plan** and execute it. The plan's `## Implementation Units` and `## Verification` sections are the definition of done.
3. **Check the box** when the unit's MR merges to `feat/redesign` (not when local work completes — merge is the gate). Replace `[ ]` with `[x]`.
4. **Append a one-line note** under the row with the MR number and merge date, e.g. `Shipped: !1234 (2026-05-24)`.
5. **If a plan pauses**, leave the box `[ ]` and append a one-line note starting with `Blocked:` plus the reason. Surface in the next maintainer sync.

---

## Plans

| # | State | Component | Target | Plan | Risk |
|---|---|---|---|---|---|
| 1 | [x] | `cts-plan-list` | `plans.html` | [`007-feat-wire-cts-plan-list-into-plans-page-plan.md`](2026-05-18-007-feat-wire-cts-plan-list-into-plans-page-plan.md) | Low — wrapper already encapsulates the page's inline JS |

Shipped: 97b37ab2c (feat) + 084ef1afb (review autofix) on feat/redesign (2026-05-18). Surfaced and fixed a latent backend-contract bug in cts-plan-list: real /api/plan returns a PaginationResponse envelope while the component assumed a plain array — would have rendered "No test plans found" in production. Verified live against MongoDB + Spring Boot on https://localhost.emobix.co.uk:8443/plans.html. Residual findings tracked in run artifact /tmp/compound-engineering/ce-code-review/20260518-224204-cbdb9f41/.
| 2 | [x] | `cts-stat` | `cts-dashboard` (`index.html`) | [`008-feat-wire-cts-stat-into-dashboard-plan.md`](2026-05-18-008-feat-wire-cts-stat-into-dashboard-plan.md) | Low — additive tile, no replacement |

Shipped: 05df5c786 on feat/redesign (2026-05-19). 4-tile stats row (Your plans / Your logs / In progress / With failures) above the existing nav grid. Two real semantic-correctness fixes surfaced by ce-code-review verified against the codebase: in-progress filter whitelists RUNNING + WAITING (the negate-FINISHED shape would miscount INTERRUPTED + pre-execution states from TestModule.Status); failures filter includes UNKNOWN to match LogApi.java's existing "FAILED || UNKNOWN" convention. Boundary fix: overflow indicator "+" now fires when `data.length >= STATS_PAGE_SIZE` even without recordsTotal. Latent pre-existing bug fixed in index.html: page-script auth probe used `removeAttribute('is-authenticated')` on a `<cts-dashboard>` that never had the attribute set — a DOM no-op that MutationObserver ignores, leaving Lit's property stuck at the constructor default of true. Switched to direct property assignment per `docs/solutions/web-components/cts-button-host-vs-inner-button-semantics-2026-04-17.md`. Residual findings (no fetch timeouts, Promise.all latency coupling, isAuthenticated false→true transition not re-fetched, "Your" labels misleading for admins) tracked at `docs/residual-review-findings/2026-05-19-cts-stat-dashboard-05df5c786.md`. Run artifact at `/tmp/compound-engineering/ce-code-review/20260519-011458-6ced17c3/`.
| 3 | [x] | `cts-test-selector` | `schedule-test.html` | [`009-feat-wire-cts-test-selector-into-schedule-test-plan.md`](2026-05-18-009-feat-wire-cts-test-selector-into-schedule-test-plan.md) | Medium — replaces an existing `<select>`, lots of surrounding JS |

Shipped: 8e93d6df9 on feat/redesign (2026-05-19). Mounted as a peer to cts-spec-cascade — typing filters the plan list, clicking a result routes through cascade.selectPlanByName so the existing downstream listeners (clearConfigForNewPlan, updateVariants, updateConfigFieldVisibility) fire exactly as if the user used the cascade dropdown. Cascade selections bridge back to highlight the matching search row via the `selected` attribute, regardless of trigger source. Plan's Open Question (a) "Single source of truth for the plan list" resolved as Option (a): page-level loadAvailablePlans was already lifting the fetch and handing it to cascade.plans — extended with one symmetric line handing the same array to planSearch.plans. Zero cascade refactor needed. Did NOT replace cts-spec-cascade (the plan's "Out of scope" boundary held). ce-code-review autofix applied in-commit: planSearchEl → planSearch local-name consistency; WithSelection "no event dispatched" assertion (re-dispatch would wipe in-flight config); cascade-highlight e2e pinned to toHaveCount(1) for `.is-active`. Run artifact at /tmp/compound-engineering/ce-code-review/20260519-014845-89d97f6c/. Residual actionable work: none.
| 4 | [x] | `cts-card` | `tokens.html` / `login.html` / `upload.html` panels | [`010-feat-wire-cts-card-into-static-pages-plan.md`](2026-05-18-010-feat-wire-cts-card-into-static-pages-plan.md) | Low — pure wrapper; speculative target picks |

Shipped: 46d1c32b6 (feat) + af20b683d (review autofix) + a44f47ad6 (residual findings) on feat/redesign (2026-05-19). U1 (tokens.html) and U3 (upload.html) ship clean — additive panel chrome on pages whose composite child has no outer chrome. U2 (login.html) ships with a P1 residual: cts-login-page already paints full panel chrome (bg-elev / border / radius-4 / shadow-3) on its inner .oidf-login-card, and the cts-card wrap creates visible card-inside-card (verified at https://localhost.emobix.co.uk:8443/login.html — outer 6px-radius card frames the inner 8px-radius login card with ~20px inset padding). The plan's Open Questions explicitly pre-authorized descoping U2 if it reads as "visual noise" — Joseph or Thomas to call at MR review. Descope is a 3-line revert across login.html and login.spec.js if desired. Pre-existing latent bug fixed in-line: cts-card.connectedCallback now has an `_initialized` re-entry guard (julik P1 conf 100 + learnings corroboration via docs/solutions/web-components/cts-modal-bootstrap-interop-2026-04-17.md) — Turbo/LiveReload/manual reparents previously would have nested .oidf-card > .oidf-card-body > .oidf-card > .oidf-card-body. Residual findings tracked at `docs/residual-review-findings/2026-05-19-cts-card-static-pages-af20b683d.md`. Run artifact at `/tmp/compound-engineering/ce-code-review/20260519-092839-2318355f/`.
| 5 | [ ] | `cts-toast` | cross-page (mounted in every HTML page) | [`011-feat-wire-cts-toast-cross-page-plan.md`](2026-05-18-011-feat-wire-cts-toast-cross-page-plan.md) | Medium — needs a global `window.ctsToast(...)` API |
| 6 | [ ] | `cts-batch-runner` | `plan-detail.html` | [`012-feat-wire-cts-batch-runner-into-plan-detail-plan.md`](2026-05-18-012-feat-wire-cts-batch-runner-into-plan-detail-plan.md) | High — needs Joseph/Thomas sign-off before merge |

---

## Sequencing rules

- **No hard dependencies.** Each plan touches a different file region and a different (or new) JS surface; running them out of order is safe.
- **Recommended order is risk × payoff.** Plans 1-3 are clear specifications. Plans 4-6 carry product/UX decisions; each plan's `## Open Questions` section names what needs sign-off before merge.
- **Brainstorm parity is the visual bar.** Every plan must end with the page rendering visually equivalent to (or strictly improving on) what was there before — no regressions. Verification steps include "click through the page's golden flow in a real browser" per the brainstorm's Phase D acceptance criteria.
- **Tests must stay green.** Each plan's verification requires `frontend/npm run test:ci`, `frontend/npm run test:e2e`, and a Storybook play-function test for the new wiring path.

---

## Status

All plans are `[ ]` as of board creation. When all six rows are `[x]`, the orphan-component sweep is complete and these six components are no longer orphaned — the design system covers every visible surface from the brainstorm's in-scope list.
