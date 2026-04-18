---
date: 2026-04-17
topic: a11y-ci-gate
status: stub
supersedes: none
extends: 2026-04-17-frontend-lint-format-typecheck-ci-requirements.md
owner: Joseph or Thomas (to be assigned at filing time)
---

# A11y CI Gate — Brainstorm Stub

## Status

**Stub.** This document captures the commitment. A full brainstorm +
plan follow in a later MR.

## Why this exists

The [frontend lint/format/type-check CI plan](../plans/2026-04-17-002-feat-frontend-lint-format-typecheck-ci-plan.md)'s
Scope Boundaries → Deferred section committed — explicitly — to an
a11y CI gate landing **before** the `frontend_lint` job promotes from
`allow_failure: true` to blocking. That commitment (option (a) in the
deferred list) is a precondition of R22.

This stub exists so the commitment is filed, discoverable, and
cross-linked from the sunset document that governs the `allow_failure`
flip. Landing this gate is a precondition for promotion; it is not a
nice-to-have.

## Scope sketch

Two tools on the rendered DOM, not on template literals:

1. **Storybook a11y addon** — runs axe against each rendered story in
   the Storybook test runner. Each `*.stories.js` already has a
   `play()` function (per `components/AGENTS.md` §6); the a11y addon
   plugs in alongside the existing play-function assertions.

2. **`@axe-core/playwright`** — runs axe against pages rendered by the
   existing `frontend/e2e/*.spec.js` Playwright specs. This extends
   the a11y surface beyond individual components to whole-page
   rendering paths that the static HTML + JS code takes.

## Why rendered DOM, not template literals

The alternative — `eslint-plugin-lit-a11y` — lints the Lit `html\`…\``
template strings statically. Two problems:

- **False negatives.** Template literals can't show what the DOM
  actually looks like once reactive properties, slot distribution,
  and `connectedCallback` side-effects have run. `cts-card`,
  `cts-modal`, `cts-alert`, and `cts-tooltip` all do slot-capture
  work in `connectedCallback` (per `components/AGENTS.md` §4); the
  template literal doesn't reflect the final DOM.
- **False positives on light-DOM patterns.** Every `cts-*` component
  renders to light DOM (`createRenderRoot() { return this; }`, per
  `components/AGENTS.md` §2). Rules that assume shadow-root
  encapsulation — label associations, accessible-name resolution
  across shadow boundaries — flag benign patterns.

A11y belongs on the *rendered* DOM. Running axe on the DOM after it
has settled catches real violations that lint-the-string tools miss,
and avoids paying for false positives from tools that assume shadow
DOM.

## What this gate is NOT

- **Not** a replacement for the Storybook play-function tests
  (`components/AGENTS.md` §6). Those remain required.
- **Not** an exhaustive a11y audit of the whole app. The gate covers
  rendered components and rendered page paths reachable from the
  existing Playwright specs.
- **Not** going to include `eslint-plugin-lit-a11y`. That tool's
  template-literal scope is the wrong cut for light-DOM Lit
  components; adding it adds friction without proportional benefit.

## Owner

Assigned at filing time — Joseph or Thomas. Whoever opens the
follow-up brainstorm/plan MR assigns themselves.

## Next steps

1. File a GitLab issue linking to this stub; assign the owner.
2. A full brainstorm document specifies: the exact axe rules in scope,
   the treatment of preset violations that already exist on current
   components, the CI job shape (new job vs. extending
   `frontend_e2e_test`), and whether the Storybook-a11y run shares an
   image with the Storybook test CI job already deferred in the
   parent plan.
3. A plan document cuts the work into units.
4. Implementation units land.
5. The `frontend_lint` sunset (calendar reminder on 2026-06-12, owned by
   Joseph or Thomas) verifies this gate has landed as one of its R22
   preconditions.

## References

- Parent plan: [`docs/plans/2026-04-17-002-feat-frontend-lint-format-typecheck-ci-plan.md`](../plans/2026-04-17-002-feat-frontend-lint-format-typecheck-ci-plan.md)
- Parent brainstorm: [`docs/brainstorms/2026-04-17-frontend-lint-format-typecheck-ci-requirements.md`](2026-04-17-frontend-lint-format-typecheck-ci-requirements.md)
- Sunset: calendar reminder on 2026-06-12 (owner: Joseph or Thomas); R22 criteria live in the `.gitlab-ci.yml` comment block above the `frontend_lint` job.
- Component authoring rules (light DOM, play functions): `src/main/resources/static/components/AGENTS.md` §2, §6
