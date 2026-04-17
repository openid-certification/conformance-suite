---
title: "Pre-deletion audit — cts-redesign-* and mock-redesign-data"
date: 2026-04-16
plan: docs/plans/2026-04-16-002-feat-regrounded-storybook-prototypes-plan.md
unit: 1
status: clean
---

# Pre-deletion audit — `cts-redesign-*` and `mock-redesign-data`

This artifact records every tracked reference to the scrapped redesign prototypes across both repositories at the time Unit 1 was executed, so Unit 7 (deletion) has a concrete punch list and reviewers can confirm nothing surprising depends on the artifacts being removed.

## Audit method

Run inside each repository from its root:

- `git grep -l "cts-redesign-"`
- `git grep -l "mock-redesign-data"`
- `git grep -l "MOCK_REDESIGN"`
- Manual inspection of `frontend/stories/fixtures/msw-handlers.js` for imports from `./mock-redesign-data.js` or `./mock-redesign-data` (by path or by re-exported symbol name).
- Manual inspection of `frontend/.storybook/main.js` globs, confirming they continue to resolve with `redesign/` absent.
- For `workshop oidf/` (not a git repo): `grep -rl "cts-redesign-" --include='*.md' --include='*.html' --include='*.js'` and HTML inspection of `showcase/index.html` / `showcase/reference.html`.

## Conformance-suite repo

Branch: `feat/regrounded-storybook-prototypes-unit-1-2` (branched from `feat/web-components-poc-cts-navbar`).

### `git grep -l "cts-redesign-"` — 0 tracked matches

The three `cts-redesign-*` component files, their co-located `.stories.js` files, and the `frontend/stories/fixtures/mock-redesign-data.js` fixture **exist on disk** but are **not tracked** in git. They are unstaged, never-committed exploratory work in `feat/web-components-poc-cts-navbar`'s primary worktree.

Files present on disk but untracked:

- `src/main/resources/static/components/redesign/cts-redesign-dashboard.js`
- `src/main/resources/static/components/redesign/cts-redesign-dashboard.stories.js`
- `src/main/resources/static/components/redesign/cts-redesign-plan-creator.js`
- `src/main/resources/static/components/redesign/cts-redesign-plan-creator.stories.js`
- `src/main/resources/static/components/redesign/cts-redesign-test-runner.js`
- `src/main/resources/static/components/redesign/cts-redesign-test-runner.stories.js`
- `frontend/stories/fixtures/mock-redesign-data.js`

**Classification:** Unit 7 is a **filesystem-level delete** (e.g., `rm -rf src/main/resources/static/components/redesign/ && rm frontend/stories/fixtures/mock-redesign-data.js`) — **not** a `git rm`. Nothing needs to leave git history because nothing was ever committed to git history. The plan's Unit 7 language ("`git rm`") should be read as "remove the files from the working tree."

### `git grep -l "mock-redesign-data"` — 0 tracked matches

### `git grep -l "MOCK_REDESIGN"` — 0 tracked matches

### Why these files were never committed

An earlier attempt to commit them as prereqs revealed that all three `.stories.js` files had pre-existing play-function test failures (semantic selector issues — e.g., `Found multiple elements with the text: OpenID Connect Core: Basic OP`). The files were authored quickly as exploratory drafts and never brought to a green state. Rather than ship known-failing tests to CI (or paper over them via story-level skip tags), the prereq-commit approach was abandoned in favor of leaving the files untracked — they never produce CI failures because they never reach any test runner, and Unit 7's deletion simplifies from a "remove the files we just added" loop to a filesystem cleanup.

### `msw-handlers.js` inspection

`frontend/stories/fixtures/msw-handlers.js` does **not** import anything from `./mock-redesign-data.js`. No handler chain in the shared file references the redesign fixture. Unit 7's deletion of the fixture will not break any story other than the three it deletes in the same commit.

### `.storybook/main.js` glob check

`frontend/.storybook/main.js` uses two globs:
- `../../src/main/resources/static/components/**/*.stories.js`
- `../../src/main/resources/static/components/flows/**/*.stories.js`

Both are resilient to the `redesign/` directory being absent — they resolve any remaining stories and silently skip a missing subdirectory. No main.js change is needed for Unit 7.

## Workshop oidf repo

`/Users/kaelig/Downloads/workshop oidf/` is a plain directory, not a git repository — greps are filesystem-scoped.

### `grep -rl "cts-redesign-"` — 5 files

| Path | Classification |
|---|---|
| `docs/plans/2026-04-16-001-feat-ux-showcase-report-plan.md` | **Documentation — no action required.** Describes the original plan that this plan supersedes. The supersession markers added by Unit 1 Step 2 make the redirect visible. |
| `docs/plans/2026-04-16-002-feat-regrounded-storybook-prototypes-plan.md` | **Documentation — no action required.** This plan itself. References `cts-redesign-*` in the Problem Frame and Unit 7's Files list. |
| `docs/brainstorms/showcase-report-requirements.md` | **Documentation — no action required.** Historical brainstorm document. |
| `docs/brainstorms/storybook-prototype-grounding-audit.md` | **Documentation — no action required.** The audit document that motivated this plan; its citations of `cts-redesign-*` are the record of what was audited. |
| `docs/brainstorms/regrounded-storybook-prototypes-requirements.md` | **Documentation — no action required.** Origin document for this plan. |

These five files document the artifacts, they do not depend on them; they stay as historical record.

### External consumer discovered — `showcase/index.html`

`workshop oidf/showcase/index.html` does **not** contain the literal string `cts-redesign-`, so it did not surface in the `git grep "cts-redesign-"` pass. Manual inspection found three references in its Section 5 (Before/After) that Unit 7 affects indirectly:

1. `screenshots/after-dashboard.png` — regenerated by Unit 8 from Unit 2's `Active` story.
2. `screenshots/after-create-plan.png` — regenerated by Unit 8 from Unit 3's `TypingFAPI` story.
3. `screenshots/after-test-execution.png` — regenerated by Unit 8 from Unit 5's `ExpandedFailMultiple` story.

Additionally, each of those three Section 5 cards has a `storyLink` slug pointing at a Storybook story slug of the now-to-be-deleted redesigns:

| `storyLink` slug | Target story | After Unit 7 |
|---|---|---|
| `flows-redesign-dashboard-onboarding--returning-implementer` | `Flows/Redesign: Dashboard + Onboarding` story `ReturningImplementer` | Broken — slug points to a deleted story. |
| `flows-redesign-test-plan-creation--empty-combobox` | `Flows/Redesign: Test Plan Creation` story `EmptyCombobox` | Broken — slug points to a deleted story. |
| `flows-redesign-test-execution--mixed-results` | `Flows/Redesign: Test Execution` story `MixedResults` | Broken — slug points to a deleted story. |

**Classification:** **Rewritten by Unit 9 of the original plan** (`docs/plans/2026-04-16-001-feat-ux-showcase-report-plan.md`). Unit 9 is the owner of `showcase/index.html` Section 5 copy and links; updating the three `storyLink` slugs to point at the corresponding `Exploration/…` stories (Home Plan Grid → Active, Searchable Spec Cascade → TypingFAPI, Failure Expandable Lozenge → ExpandedFailMultiple) is the Unit 9 task. Unit 7 must not ship before Unit 9 has updated these slugs, or the three Section 5 links will 404 until Unit 9 lands.

This dependency is **already captured in Unit 7's "Dependencies" and the original plan's Unit 9 coordination note** — recording it here completes the paper trail.

### `showcase/reference.html` and `showcase/tokens.css`

Both files have zero matches for `cts-redesign-`, `mock-redesign-data`, or `MOCK_REDESIGN`. No action required.

## Screenshots requiring regeneration

| Path | Regenerating unit | Source story |
|---|---|---|
| `workshop oidf/showcase/screenshots/after-dashboard.png` | Unit 8 | `Exploration/Home Plan Grid` → `Active` (Unit 2) |
| `workshop oidf/showcase/screenshots/after-create-plan.png` | Unit 8 | `Exploration/Searchable Spec Cascade` → `TypingFAPI` (Unit 3) |
| `workshop oidf/showcase/screenshots/after-test-execution.png` | Unit 8 | `Exploration/Failure Expandable Lozenge` → `ExpandedFailMultiple` (Unit 5) |

The three corresponding `before-*.png` files exist and are unchanged by any unit in this plan.

## Verdict

**Audit is clean.** Zero tracked matches in the conformance-suite repo — the redesign components and fixture were never committed to git, so there is no historical artifact to remove. Workshop oidf matches are documentation-only. The one cross-repo consumer (`workshop oidf/showcase/index.html`) is handled by the original plan's Unit 9 (slug updates) and this plan's Unit 8 (screenshot regeneration), both of which gate Unit 7.

Unit 7 may proceed once:

- Unit 8 has re-captured the three `after-*.png` files.
- The original plan's Unit 9 has updated the three `storyLink` slugs in `showcase/index.html`.
- Units 2, 3, and 5 have landed (their stories are Unit 8's capture sources).
