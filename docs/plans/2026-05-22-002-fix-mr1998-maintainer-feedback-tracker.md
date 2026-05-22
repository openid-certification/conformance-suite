---
title: "MR 1998 maintainer-feedback thread tracker"
type: tracker
status: active
created: 2026-05-22
plan: docs/plans/2026-05-22-002-fix-mr1998-maintainer-feedback-plan.md
mr: https://gitlab.com/openid/conformance-suite/-/merge_requests/1998
---

# MR 1998 — maintainer-thread tracker

State of every review thread on [MR 1998](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998) versus the unit-by-unit execution of `docs/plans/2026-05-22-002-fix-mr1998-maintainer-feedback-plan.md`.

**Update protocol** (from `executing-plan-unit-by-unit` skill): before invoking `compound-engineering:ce-commit` for any unit, this table must reflect the change. Stage this file alongside the unit's code files so the commit captures both.

**Status values:**

- `open` — no commit yet addresses the thread.
- `partial` — at least one commit closes part of the thread; sub-issues remain. Reply on the thread but **leave it unresolved** on the MR until the rest lands.
- `closed` — every concern in the thread is addressed. Reply and mark resolved on the MR.
- `gated` — blocked on a Decision Gate (G1, G2, G3). No code-side work possible until the gate resolves.

---

## Threads

| Status | Finding | Author | Thread | Closing commit(s) | Suggested reply |
|---|---|---|---|---|---|
| closed | A5 (spinner half) | thomasdarimont | [#note_3371494631](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371494631) | `c63c21ea7` | "Fixed in c63c21ea7 — added a static `info` cts-badge variant and re-pointed cts-log-entry's INFO and HTTP request/response/incoming/outgoing markers to it. Verified in-situ on a real 27-row log: no `.cts-badge-spin` elements remain on any blue pill." *Note: this thread also contains the progress-bar (A4) concern. Reply scoped to "indicators stay animated"; leave thread unresolved until A4/U5 ships.* |
| closed | A5 + #21 | almgren | [#note_3371763018](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371763018) | `c63c21ea7` | "Fixed in c63c21ea7 — see Thomas's thread for details. The blue status pills now route through a static `info` variant; the spinner only fires for the genuinely-running state. The 'misalignment' subjective impression may have been the spinner's perpetual motion catching the eye — please reopen if you still see geometric drift after pulling." |
| closed | A6 | almgren | [#note_3371749447](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371749447) | `c63c21ea7` (spinner), `dc7e053a3` (WAITING copy + Start visibility) | "The remaining half of this thread is fixed in `dc7e053a3` (U3). The WAITING-state copy now distinguishes two cases — when no conditions have run yet the bar/hero keep 'Click Start Test when you're ready'; once any results have been recorded the test is waiting on an external party, so the hero switches to 'Test running' / 'Waiting for an external request — no action required from you.' and the Start button is suppressed. Verified in-situ via Storybook (`WaitingHeroDistinguishesExternalVsUserAction` and `WaitingHeroKeepsStartWhenFreshTest`)." |
| closed | A1 | thomasdarimont, almgren | [#note_3371485017](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371485017) | `dc7e053a3` | "Fixed in `dc7e053a3` (U3). The status bar and hero now derive a lifecycle phase from `(status, result)` rather than from `status` alone, so a test whose verdict has landed but whose status field hasn't yet flipped to FINISHED renders the terminal branch immediately — no more Start button hanging around after 'Test has run to completion'. Verified on a real PASSED test (`zyH62JQ9NeDN0bD`): the bar shows PASSED + Repeat Test, no Start CTA. Story: `StaleStatusBarReadsTerminalWhenResultIsSet`." |
| closed | A2 | thomasdarimont | [#note_3371499052](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371499052) | `dc7e053a3` | "Fixed in `dc7e053a3` (U3). The header now renders a prominent terminal-state banner ('Test passed' on the pass palette, with the `circle-check` glyph) between the sticky bar and the hero whenever a test has reached a terminal phase. The verdict reads at a glance without scrolling to find the SUCCESS chip. Screenshot: `tmp/screenshots/u3-finished-pass-in-situ.png`." |
| closed | A3 | thomasdarimont | [#note_3371528678](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371528678) (A3 portion) | `U4_COMMIT_SHA` | "The HTTP 400 portion is fixed in `U4_COMMIT_SHA` (U4). Root cause: the v2 page-level handler in `log-detail.js` was sending the runtime test ID (e.g. `zyH62JQ9NeDN0bD`) as the `test=` query param, but `POST /api/runner` expects the *module name* (e.g. `oidcc-server`) — `TestRunner.java:215`. Continue Plan was 400'ing for a different reason in the same handler: it omitted `test=` entirely, expecting the backend to figure out 'next module' from `plan=` alone. The fix builds the URL via the same shape as `plan-detail.html`'s `buildRunnerUrl` (test=testName, &plan=, &variant=, Content-Type: application/json) and resolves the next module from the cached `/api/plan` modules list using subset-match on the variant constraints. Verified in-situ on a real OIDCC plan: Repeat POST returns HTTP 201, Continue POST returns HTTP 201 with the next module's testName. Error path now extracts the server's `{error:'…'}` body instead of showing 'HTTP 400'. This thread also contained C1 (label consistency, closed by `dc7e053a3`) — with A3 closed, the thread is fully addressed and can be resolved on the MR." |
| open | A4 | thomasdarimont | [#note_3371494631](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371494631) (mixed with A5) | — | (closes after U5) |
| closed | A7 | almgren | [#note_3371772858](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371772858) | `dc7e053a3` | "Fixed in `dc7e053a3` (U3). The same terminal banner that closes A2 also applies to FAILED ('Test failed' on the fail palette, `close-circle` glyph) and INTERRUPTED ('Test interrupted'). Verified on a real INTERRUPTED+FAILED test (`VeJL6Fu5kmisBa3`): the banner sits above the existing interrupted alert + failure list, so 'did this test fail?' answers itself before any scrolling. Screenshot: `tmp/screenshots/u3-interrupted-in-situ.png`. Stories: `TerminalBannerFailed`, `TerminalBannerInterrupted`." |
| open | B1 | thomasdarimont | TBD | — | — |
| gated | B2 | almgren | TBD | — (G2 must resolve first) | — |
| open | B3 | almgren | TBD | — | — |
| open | B4 | almgren | TBD | — | — |
| open | B5 | almgren | TBD | — | — |
| open | B6 | almgren | TBD | — | — |
| open | B7 | almgren | TBD | — | — |
| closed | C1 | thomasdarimont | [#note_3371528678](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371528678) (label-consistency portion) | `dc7e053a3` | "Action label consistency landed in `dc7e053a3` (U3): the status-bar primaries now read 'Start Test' (WAITING) and 'Repeat Test' (FINISHED), matching cts-test-nav-controls' 'Repeat Test' / 'Continue Plan' so every action follows the `<Verb> <Object>` convention. The HTTP 400 portion of this thread (A3) ships in U4 — with that landed, the whole thread can be resolved on the MR." |
| open | C2 | thomasdarimont, almgren | TBD | — | (rename portion gated on G3) |
| open | D1 | almgren | TBD | — | — |
| open | D2 | almgren | TBD | — | — |
| gated | E1 (grid) | almgren | TBD | — (G1 must resolve first) | — |
| open | E1 (Config column) | almgren | TBD | — | — |

**Rows marked `TBD on next discussion fetch`:** I have not yet fetched every thread URL for every finding. Bootstrap rule from the skill: on the first execution invocation that touches one of these findings, fetch the corresponding thread via `glab api projects/openid%2Fconformance-suite/merge_requests/1998/discussions --paginate` and fill in the row before implementing.

---

## Decision Gate resolutions

Record maintainer decisions on G1 / G2 / G3 here as they land. Empty until the user (or a maintainer) answers.

| Gate | Question | Decision | Date | Decided by | Notes |
|---|---|---|---|---|---|
| G1 | `plans.html` per-module status grid — restore / aggregate / list-only | — | — | — | Blocks U17 |
| G2 | `schedule-test.html` "search test plans" header — remove / collapse / keep | — | — | — | Blocks U15 |
| G3 | "About this test" labelling — rename to "Description" / "Title" or keep | — | — | — | Affects U6 follow-up (formatting ships regardless) |

---

## How this file gets used

1. The `executing-plan-unit-by-unit` skill reads this file at the start of every unit-execution loop.
2. After verification passes and before `compound-engineering:ce-commit` fires, the unit's row(s) get `status` and `closing commit` filled.
3. The post-commit handoff to the user pulls the suggested-reply text from here.
4. The MR's discussion-level "Resolve" action is the user's call — the agent does not toggle thread resolution on the MR itself.
