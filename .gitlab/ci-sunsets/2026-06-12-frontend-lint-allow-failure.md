---
date: 2026-04-17
sunset-date: 2026-06-12
owner: Joseph or Thomas (core CTS maintainers)
target: .gitlab-ci.yml — `frontend_lint` job
related-plan: ../../docs/plans/2026-04-17-002-feat-frontend-lint-format-typecheck-ci-plan.md
introducing-commit: 94b7a0a0b (Unit 8)
---

# Sunset: `frontend_lint` `allow_failure: true`

## Purpose

This document formalizes the commitment — per R23 of the
[frontend lint/format/type-check CI plan](../../docs/plans/2026-04-17-002-feat-frontend-lint-format-typecheck-ci-plan.md) —
to flip the `frontend_lint` CI job from non-blocking to blocking on a
specific date, rather than letting `allow_failure: true` drift
indefinitely.

## The exact change

In `.gitlab-ci.yml`, inside the `frontend_lint` job:

```yaml
# Before
allow_failure: true     # R22: sunset in 6 weeks — see Unit 9's sunset doc

# After
allow_failure: false
```

That is the entire diff. One line.

## Cut-over date

**2026-06-12** — six weeks after T = 2026-05-01, the target date by which
`frontend_lint` should be green on `main` for two consecutive weeks (the
first R22 promotion criterion). T was picked as a reasonable "job is
green" target given Unit 8 landed on 2026-04-17; if the job proves
flakier than expected the owner posts a written extension rationale in
a new MR (see "Fallback" below).

## Owner

Joseph or Thomas — the core CTS maintainers.

On the cut-over date the owner is responsible for either:

1. Opening an MR that flips the line above, merging it after verifying
   the R22 preconditions (see below), **or**
2. Posting a written extension rationale in a new MR that defers the
   cut-over to a specific later date. Silent slippage is not permitted;
   a calendar-reminder no-op must produce either a flip-MR or an
   extension-MR on the date itself.

## R22 promotion preconditions (must hold on cut-over date)

1. `frontend_lint` has been green on `main` for two consecutive weeks.
   The owner reviews the GitLab pipeline history dashboard and signs
   off in the MR description that flips the line.
2. All open entries in the named **Legacy Overrides** block of
   `frontend/eslint.config.js` are either (a) deleted — meaning the
   rule now errors on all files — or (b) have their cleanup GitLab
   issue closed. Current count as of 2026-04-17: **0 entries**.
3. The A11y CI follow-up has landed — see
   [`docs/brainstorms/2026-04-17-a11y-ci-gate-requirements.md`](../../docs/brainstorms/2026-04-17-a11y-ci-gate-requirements.md).
   The A11y gate must be in place *before* `frontend_lint` promotes to
   blocking, per the plan's Scope Boundaries → Deferred commitment.

If any precondition is unmet on the cut-over date, extend via an
extension-MR — do not flip silently and do not hide the failure.

## Fallback procedure

**Primary path:** a calendar reminder on the owner's calendar, set on
the date Unit 8 landed, for 2026-06-12. This is the lowest-friction
mechanism and does not depend on GitLab scheduled pipelines or bot
tokens.

**If the owner is unavailable on the date:** a backup maintainer (the
other of Joseph or Thomas) posts the extension-MR. The goal is to
force an *explicit* decision (flip or extend) on the date, visible in
the commit log — not to require the primary owner specifically.

**If the owner forgets:** a second maintainer who notices
`allow_failure: true` still in place past 2026-06-12 files a "sunset
overdue" MR linking this document. This is the final safety net; the
calendar reminder should prevent it from being needed.

## Stretch: scheduled-pipeline + `glab mr create` mechanism

For a future improvement, GitLab Scheduled Pipelines can open the
flip-MR automatically:

- A scheduled pipeline fires on 2026-06-12.
- Job runs `glab mr create` (or the REST API) with an API-scoped CI
  token, opening a draft MR that swaps `allow_failure: true` →
  `allow_failure: false` on a dedicated branch.
- The owner reviews, confirms the R22 preconditions in the MR
  description, and merges — or extends — within the week.

This is **not a prerequisite** for this sunset to land. The calendar
reminder is the committed path. The scheduled-pipeline mechanism is
listed here as an optional future improvement; building it requires
provisioning an API-scoped CI token and is worth its own MR.

## Renegotiation clause

A second extension past whatever date the first extension-MR specifies
requires explicit renegotiation of R10 ("default severity is error")
in a new plan or brainstorm document. The `allow_failure: true` window
is a one-time grace period, not an indefinite state.

## References

- Plan: [`docs/plans/2026-04-17-002-feat-frontend-lint-format-typecheck-ci-plan.md`](../../docs/plans/2026-04-17-002-feat-frontend-lint-format-typecheck-ci-plan.md) (R22, R23)
- Introducing commit: `94b7a0a0b` (Unit 8 — adds `frontend_lint` job)
- A11y follow-up: [`docs/brainstorms/2026-04-17-a11y-ci-gate-requirements.md`](../../docs/brainstorms/2026-04-17-a11y-ci-gate-requirements.md)
- Local severity-ladder doc: `frontend/README.md`
