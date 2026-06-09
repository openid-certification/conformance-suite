# WAITING-state activity indicator

**Status:** proposed (not started)
**Driver:** Maintainer feedback (Thomas Darimont): "I think we need a more
explicit indicator that shows that the test is really doing something… on
the right side of the video I only see 'waiting' which is IMHO a bit
subtle — perhaps a waiting with an animated hourglass would match the
'running…' animation."

## Problem

A test in `WAITING` status is *alive* — the suite is listening for an
external party (a wallet callback, a browser redirect, a user click on
Start) — but the UI renders it as a static amber `WAITING` pill
(`cts-badge variant="warn"`). Next to the `RUNNING` pill, which carries a
built-in rotating-arc spinner (`cts-badge.js` `running` variant, 1.1s
arc), the static pill reads as "nothing is happening", and testers assume
the run is stuck.

## Design direction

No hourglass — it implies *elapsing time / progress*, which WAITING is
not, and it reads dated. The semantic to communicate is **"alive and
listening"**, distinct from running's **"working"**. The modern idiom for
that is a **pulsing dot** (the "radar ping" used by uptime dashboards and
presence indicators):

- A small filled dot in the badge, with a soft expanding ring that fades
  out on a ~1.6s ease-out loop. Calmer cadence than the 1.1s running
  spinner — deliberate, so the two states stay visually distinguishable
  at a glance while both read as live.
- Colour stays in the existing `warn`/amber status palette
  (token-routed), so no new status colour is introduced and the
  segment/chip colour-consistency rule is respected.
- `prefers-reduced-motion: reduce`: swap the ping for a slow opacity
  pulse, mirroring what `cts-spinner.js` already does for its rotation.
  (Audit while in there: the `running` badge spin in `cts-badge.js` has
  **no** reduced-motion fallback today — fix as part of U1.)

Secondary reinforcement (cheap, optional): the status-bar support text
("Waiting for external input — no action required") already explains the
state; an animated ellipsis would be noise on top of the ping. Skip it.

## Where WAITING renders today

| Surface | File | Current rendering |
| --- | --- | --- |
| log-detail status bar pill | `cts-log-detail-header.js` (`_renderWaitingBar`, `STATUS_BADGE_VARIANTS.WAITING: "warn"`) | static warn pill + support text |
| logs list status column | `cts-log-list.js` | static badge |
| run status strip | `cts-run-status-strip.js` | status segment |
| running-test card | `cts-running-test-card.js` | static badge |
| batch runner | `cts-batch-runner.js` | status text/badge |
| plan-detail module boxes | `js/module-status.js` / `cts-plan-modules` | colour-coded box |

## Implementation units

- **U1 — `cts-badge` `waiting` variant.** Add a first-class `waiting`
  status variant to `cts-badge.js` (amber warn palette + the ping-dot
  glyph, built like the existing `running` spinner span). Include the
  reduced-motion fallback for BOTH `waiting` and the pre-existing
  `running` spin. JSDoc `@property` update, Storybook story with play
  test (assert glyph node exists, assert animation present, assert
  reduced-motion override via `matchMedia` emulation), badge inventory
  doc block update.
- **U2 — log-detail adoption.** `STATUS_BADGE_VARIANTS.WAITING:
  "waiting"` in `cts-log-detail-header.js`; keep label "WAITING". This is
  the surface Thomas was looking at. Story/e2e assertions that pin the
  pill variant.
- **U3 — sweep the secondary surfaces.** `cts-log-list`,
  `cts-run-status-strip`, `cts-running-test-card`, `cts-batch-runner`:
  route their WAITING badges to the new variant. Plan-detail module
  boxes stay colour-coded boxes (per the established status-box
  decision) — no animation there; a grid of pinging boxes would be
  visual noise.
- **U4 — CLAUDE.md badge section.** Document the new variant and the
  "running = spinner (working), waiting = ping (listening)" semantic so
  future surfaces pick the right one.

## Open questions for maintainers

1. Should the ping also run on the logs *list* page (many rows could
   ping simultaneously)? Proposal: yes on detail surfaces, no on bulk
   tables — bulk tables keep the static pill. (U3 default: static in
   `cts-log-list`, animated elsewhere.)
2. Cadence/intensity sign-off after a visual review on a live WAITING
   test (VP wallet flow is the easiest repro).

## Verification

- Storybook play tests per variant (motion + reduced-motion).
- e2e: log-detail WAITING fixture asserts the pill carries the
  `waiting` variant.
- Manual: run a VP wallet test to the QR-code step; confirm the bar
  reads as live next to the heroes' QR/browser slots.
