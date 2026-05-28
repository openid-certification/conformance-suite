# Residual Review Findings — log-entry noise reduction

Source: `/code-review ultra`-style multi-agent review (9 reviewers) run by `/lfg` on
branch `feat/redesign`, scoped to the 3 commits implementing
`docs/plans/2026-05-27-003-feat-log-entry-noise-reduction-plan.md`.

Run artifact: `/tmp/compound-engineering/ce-code-review/20260527-183925-fffe4709/`
(local; not in repo). Verdict: **Ready with fixes** — 9 fixes applied in
`fix(review): apply autofix feedback`; the items below were deferred as
contract changes, pre-existing behavior, or judgment calls.

This file is the durable sink because the remote is GitLab (no GitHub PR to
append to). Fold these into the feat/redesign MR description or resolve directly.

## Residual Review Findings

- **#10 (P2, gated_auto → downstream-resolver)** `src/main/resources/static/components/cts-log-entry.js:598,612` + the `.testId` binding in `cts-log-viewer.js` — **Remove the now-dead `testId` property.** Its only consumer was the retired `cts-log-entry-id` chip; the timestamp deep-link uses a relative `#LOG-NNNN` fragment that needs no `testId`. Flagged by maintainability (M-01, P1, conf100) and project-standards (PS-001). The misleading JSDoc was corrected in the autofix pass; the property removal is a public-attribute contract change, so it was deferred. Removing it also lets the viewer drop `.testId=${this.testId}`.
- **#11 (P2, manual → downstream-resolver)** `cts-log-viewer.js` `_fetchEntries` append path — **Duplicate `referenceId` host-id collision.** Entries are appended without dedup; if the backend re-delivers a same-timestamp entry (an inclusive `?since=` boundary), two `<cts-log-entry id="LOG-NNNN">` render and `document.getElementById` resolves the wrong one, so the deep link / `:target` highlight lands on the duplicate. Adversarial A1 (conf50). Needs confirmation of the backend `?since=` boundary semantics; pre-existing append behavior, made more visible by deep-link reliance on the id.
- **#12 (P3, manual → downstream-resolver)** `cts-log-viewer.js` `_onHashChange` — **Ungated hashchange re-opens a user-collapsed `<details>` and re-scrolls on browser back/forward.** Any fragment change (including history navigation) runs the open-ancestors + scroll routine. Adversarial A2 (conf65). Debatable whether jumping to the anchor on back/forward is a bug or correct behavior.
- **#13 (P3, manual → downstream-resolver)** `cts-log-viewer.js:636` — **Initial-scroll retry never terminates for an unreachable/out-of-range hash.** `_scrollToHashIfPresent` returns `false` forever for `#LOG-9999` (or a block-start ordinal), so `_initialHashScrolled` never flips and every poll re-evaluates it for the life of the page. Cheap (one `getElementById`, no scroll), but consider a terminal-state stop. Adversarial A3 (conf70).
- **#14 (P3, advisory → human)** `cts-log-viewer.js` — **`_initialHashScrolled` not reset on `testId` change.** Latent SPA-navigation bug: if `testId` is reassigned without a page reload, the new test's initial hash scroll never fires. No in-app SPA navigation today. Frontend-races RR-03.

## Testing gaps (advisory)

- Disconnect listener-removal is not directly tested (dispatch a `hashchange` after disconnect, assert no scroll).
- `aria-label` same-second disambiguation not directly tested (two entries, identical time, different `referenceId`).
- Post-load `hashchange` combined with a late-poll arrival is not covered (only the initial-load late-arrival path is).
- `DeepLinkHighlight` does not assert the `:target` highlight clears when the fragment is removed.

## Worth capturing as a docs/solutions/ learning

The URL-fragment scroll + Lit child-id timing race, the `:target` highlight
specificity tie (wins by source order), and the `hashchange` listener teardown
pattern are undocumented. A `docs/solutions/web-components/` write-up would help
the next contributor who hits the same timing question.
