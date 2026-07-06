# OpenID Conformance Suite — Agent Guide

Canonical, harness-neutral instruction root. Deep guidance lives in nested
AGENTS.md files (see the conventions map); each has a sibling CLAUDE.md
shim so Claude Code loads the same guidance on demand.

## Project overview

Spring Boot application that validates implementations of OpenID Connect,
FAPI1, FAPI2, FAPI-CIBA, eKYC, OpenID Federation, Verifiable Credentials
(VCI), and Verifiable Presentations (VP).

## Workflow rules

After making code changes, always run the project build and tests before
committing. If tests fail, fix them before presenting the result as complete.

## Build commands

```bash
mvn -B -Dmaven.test.skip -Dpmd.skip clean package  # build, skip tests
mvn test                                           # all (+ PMD, checkstyle)
mvn test -Dtest=ClassName_UnitTest                 # single test class
mvn test -Dtest='*ArchUnit*'                       # ArchUnit (quote glob)
```

Build with JDK 21 — newer JDKs crash Error Prone. The built JAR is
`target/fapi-test-suite.jar`. Test-framework and Java conventions:
`src/main/java/net/openid/conformance/AGENTS.md` (read it before Java work,
including tests under `src/test/java/`).

## Running locally

Requires MongoDB and an HTTPS reverse proxy: `devenv up` (Nix) or
`docker-compose -f docker-compose-dev.yml up` (native-server macOS variant:
`docker compose -f docker-compose-dev-mac-nodocker.yml up -d`; needs Mongo 6.x
— Mongo 7 crashes boot, and a stray `mongo:7` container can squat 27017).
Run the app with Spring profile `dev`; it serves
`https://localhost.emobix.co.uk:8443` (mTLS on `:8444`). Never set
`SPRING_PROFILES_ACTIVE=dev` outside a dev machine — it injects a synthetic
admin user and bypasses real authentication. Save-and-see loop:
`src/main/resources/static/AGENTS.md` "Dev loop". To set up a machine end to
end, run the `cts-ui-setup` skill.

## UI work — expected workflow

Any agent touching `src/main/resources/static/` is expected to (this is the
workflow, not a hard gate):

- Run the `cts-ui-setup` skill first if the app or Storybook is not running
  (`scripts/ui-preflight.sh` probes both).
- Review with the `cts-design-eye` skill before presenting ANY change to
  rendered output — even a small styling tweak. Mechanical-diff review
  carve-outs do not waive it; skip only when nothing rendered changes.
- Let the edit hooks run `frontend/scripts/agent-edit-check.sh` on each edit
  (kill switch: `CTS_SKIP_EDIT_CHECKS=1`), and run
  `cd frontend && npm run test:ci` before committing.

## Self-amending conventions

When a change knowingly deviates from a documented convention, update the
convention documentation in the same change: find the owning surface in the
conventions map, supersede any conflicting guidance (never only append), and
flag contradictions you notice between surfaces. Consistency comes from the
system evolving, not from freezing it.

The enforcement layer is outside self-amendment: the workflow directives
above, hooks, check scripts, and the review-sensitive file set change only
via deliberate maintainer-reviewed edits.

## Conventions map

- `AGENTS.md` — universal workflow, git conventions, this map.
- `src/main/java/net/openid/conformance/AGENTS.md` — test framework:
  modules, conditions, variants, configuration fields, error messages.
- `src/main/resources/static/AGENTS.md` — UI conventions: dev loop, icons,
  badges, E2E tests, quality gates, vendored frontend dependencies.
- `src/main/resources/static/components/AGENTS.md` — cts-component
  authoring rules (Lit, light DOM, JSDoc, stories).
- `frontend/AGENTS.md` — npm toolchain and lockfile rules.
- `src/main/resources/static/css/oidf-tokens.css` — design tokens
  (vendored: append token decisions to its "Deliberate deviations" list,
  never edit values in place).
- `frontend/README.md` — human cross-reference (not canonical).
- `docs/solutions/` — dated learnings.

## Review-sensitive files

Diffs touching `frontend/scripts/agent-edit-check.sh`, the `scripts/*.sh`
files skills invoke, `.claude/settings.json`, `.codex/`, the `.gitignore`
skills allowlist, or `.claude/skills/*` symlinks execute code on teammates'
machines outside the harness trust gates — flag for named maintainer review.

## Code review

When asked to review a commit or branch, structure the review by file and
call out correctness issues (especially dead code or unreachable paths), API
misuse, and behavioral changes. Don't just summarize — actively look for
bugs. The `openid-review` skill carries the full checklist.

## Git

- Create separate atomic commits per logical change; verify the build passes
  for each commit independently.
- To fix up a non-HEAD commit: `git commit -m "fixup! <target message>"`,
  then `git -c sequence.editor=true rebase -i --autosquash <base>`.
- If a change closes a GitLab issue, end the commit message with a trailer
  like `Closes #1650` / `Fixes #1650`. If the issue link isn't obvious, ask.

## Scratch artifacts

Save screenshots, traces, and any other unversioned dev artifacts under
`tmp/` at the repo root (screenshots: `tmp/screenshots/<name>.png`) — it is
gitignored. Never write screenshots to the repo root or to `screenshots/`;
both clutter `git status`. Tracked image assets are unaffected.

## Agent harness notes

- Sessions read instruction and hook config at start: after pulling changes
  to AGENTS.md files, `.claude/`, `.codex/`, or `.agents/skills/`, restart
  your session.
- Codex trusts hooks per config hash: whenever `.codex/hooks.json` changes,
  re-run the hook trust review — changed hooks are silently skipped until
  re-trusted. Claude Code instead prompts once to approve project-settings
  hooks; approve to get the edit checks.
- Committed skills live in `.agents/skills/` (canonical) with committed
  `.claude/skills/` symlinks.
