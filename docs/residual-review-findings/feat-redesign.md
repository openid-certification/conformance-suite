# Residual Review Findings — feat/redesign

Durable record of non-blocking review findings that were not auto-fixed during
`/lfg` autofix passes on the `feat/redesign` branch (ships via GitLab MR !1998,
so there is no GitHub PR body to host these). Append new findings as later units
land; remove an entry when it is resolved.

## Residual Review Findings

### From U8 + U9 (plans-page-as-home) — review run `20260529-080529-1e428340`, 2026-05-29

- **[P3] Stale `current-page` attributes after the U9 nav collapse** —
  `src/main/resources/static/index.html` sets `current-page="home"` and
  `src/main/resources/static/schedule-test.html` sets `current-page="create-test"`.
  Both page keys were removed from `cts-navbar`'s `NAV_LINKS` in U9, so they now
  match no nav link and highlight nothing (benign dead config, not a bug). Out of
  the U8/U9 changed-file scope. `index.html` is retired in U11 (maintainer-gated),
  so only `schedule-test.html` needs a one-attribute cleanup (`current-page=""`).
  Owner: downstream-resolver.

#### Pre-existing / advisory (not introduced by U8/U9 — recorded for context, no action required here)

- **[P2] `cts-plan-list.js` exceeds 1k lines** (1025 → 1112). Already over the
  threshold before this diff; U8 added 87 coherent lines. The plan's "no
  abstraction without a second consumer" rule defers an empty-state render-helper
  extraction.
- **[P3] `cts-plan-list._fetchPlans` has no `_fetchSeq` fetch-generation guard.**
  Already a tracked U6 deferred follow-up (port the U7 `_fetchSeq` pattern). U8
  does not add a new re-fetch path — the `authenticated` attribute gates render
  only. See `docs/solutions/web-components/fetch-generation-guard-for-page-driven-components.md`.
- **[P3] 36px button height hard-coded** in `cts-plan-list.js` (and 5 other files);
  introduce a shared `--oidf-btn-height` token. Pre-dates this diff.
- **[P3] `cts-link-button` `icon` JSDoc says "Bootstrap Icons"** but the component
  resolves coolicons. Misleading for future callers; `add-plus` resolves correctly.
  Out of this diff's scope.
