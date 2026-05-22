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
| partial | A6 (spinner half) | almgren | [#note_3371749447](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371749447) | `c63c21ea7` (spinner only) | "The spinner-on-INFO half of this thread is fixed in c63c21ea7. The Start-button-visible-during-WAITING and the wrong 'click Start' action-required copy are tracked as A6 and ship in U3 — coming next. Leaving the thread open until that lands." |
| open | A1 | thomasdarimont, almgren | TBD on next discussion fetch | — | — |
| open | A2 | thomasdarimont | TBD | — | — |
| open | A3 | thomasdarimont | TBD | — | — |
| open | A4 | thomasdarimont | [#note_3371494631](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371494631) (mixed with A5) | — | (closes after U5) |
| open | A6 | almgren | [#note_3371749447](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998#note_3371749447) (mixed with A5) | — | (closes after U3) |
| open | A7 | almgren | TBD | — | — |
| open | B1 | thomasdarimont | TBD | — | — |
| gated | B2 | almgren | TBD | — (G2 must resolve first) | — |
| open | B3 | almgren | TBD | — | — |
| open | B4 | almgren | TBD | — | — |
| open | B5 | almgren | TBD | — | — |
| open | B6 | almgren | TBD | — | — |
| open | B7 | almgren | TBD | — | — |
| open | C1 | thomasdarimont | TBD | — | — |
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
