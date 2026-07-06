# UI Enablement Kit — Validation Report

Plan: `docs/plans/2026-07-06-001-feat-ui-enablement-kit-plan.md` (U7).
Date: 2026-07-06. Operator: Kaelig (authoring machine, macOS).

## Sandbox mechanics (KTD6)

- **Fresh clone:** local clone of branch `feat/ui-enablement-kit` into a
  session scratch directory; the AE2 replay reverts `5f72e75df` ("fix(log):
  keep test objective visible across states") inside that clone only.
- **Isolated HOME:** `~/.cache/cts-ui-kit-sandbox/home` (mode 0700), empty
  except `.claude/.credentials.json` **symlinked** (not copied) to the
  operator's credential file — auth is shared; skills, plugins, and memory
  are not. No credential bytes were duplicated to disk.
- **Permission mode (named, per KTD6):** headless `claude -p` with a
  **scoped `--allowedTools` allowlist** (Read/Glob/Grep/Skill/TodoWrite plus
  the exact Bash command families the bootstrap documents). The session's
  auto-mode classifier declined `--dangerously-skip-permissions` for
  agent-spawning; the allowlist keeps a real gate (unlisted tools are
  denied) and is recorded here as the mode actually used.
- **Shared host services (accepted risk, bounded):** Mongo 6.0.13 + nginx
  (docker compose `docker-compose-dev-mac-nodocker.yml`), the packaged app
  (`target/fapi-test-suite.jar`, JDK 21, `--spring.profiles.active=dev`),
  and Storybook on :6006 are the host's. **Pre-gate `mongodump` taken**
  (3.6 MB archive, session scratch, `test_suite` db dumped clean) before
  any agent gate ran.

## Gate results

| Gate | Requirement | Result |
|---|---|---|
| Instruction-layer budgets | R1 | **PASS** — `lint-agents-budget.sh`: root 5810/6144, +frontend 7593/10240, +java 18870/20480, +static 12218/12288, deepest 28188/32768; negative tests (inflated file, dead map path, broken shim) each exit 1 naming the offender |
| Claude nested-shim smoke (U1) | R2 | **PASS** — fresh `claude -p` session reading `cts-badge.js` answered §7 content (lookup tables + `classMap`) that exists only in `components/AGENTS.md`, proving on-demand shim load |
| Codex root+nested smoke (U1) | R2 | **PASS** — after `codex login`, a read-only `codex exec` from the components dir quoted all three H1s on the path (root, static, components) plus root-only and components-only content |
| Skills layout fresh-clone (U2) | R8 | **PASS** — symlink materializes on clone, SKILL.md resolves through it, planted personal skills in both dirs leave `git status` clean; fresh-clone Claude session lists `openid-review` |
| Script smoke (U3) | R6, R9 | **PASS** — full scenario set in the U3 evidence (up/down probes, kill switch, spaces, scoping, <10 s timing); preflight symlink FAIL branches (materialized file, escaping target) exercised live in the fresh clone after U2 landed the first tracked symlink |
| Hook plumbing (U6) | R9 | **PASS** — `--stdin-json` parsed Claude (`tool_input.file_path`) and Codex (`apply_patch` "*** Update File:") payloads, ran checks per path, exit 1 propagated for a dirty file, malformed/empty payloads exit 0 |
| Hook firing, live Claude session | R9 | **DEFERRED (in-session observation)** — headless child sessions skip unapproved project-settings hooks (observed: no hook feedback in `claude -p` even with `--settings`); first interactive session after merge must approve the settings prompt, which is exactly the trust flow AGENTS.md and cts-ui-setup document. Plumbing is fully unit-verified above |
| Hook firing, Codex trusted repo | R9, R10 | **PASS with asymmetry** — clean `apply_patch` edit → `hook: PostToolUse Completed`; dirty edit → `hook: PostToolUse Failed` (our check's exit 1), but Codex 0.142.5 does NOT feed hook output back to the model (`NO-HOOK-FEEDBACK`) — the instruction layer's explicit test:ci-before-commit directive is the compensating control |
| AE1 bootstrap (Claude) | R5, R11 | **PASS** (isolated) — see below |
| AE1 bootstrap (Codex) | R5, R11 | **PASS within sandbox limits** — see below |
| AE2 replay | R3, R7, R12 | see below |
| AE4 eyes gate | R13 | **PASS 10/10** — see below |
| AE5 ce-work loop | R14 | see below |
| Degraded-mode banner | R6 | see below |

## Eyes gate (AE4 / R13) — PASSED 10/10

`scripts/eyes-gate.sh` against `https://localhost.emobix.co.uk:8443/`:

- **Stronger-control evaluation first (KTD4):** the dev cert is a
  self-signed leaf generated fresh at nginx image build
  (`nginx/Dockerfile-nodocker`) — no stable committed CA exists, Chromium
  does not honor per-process CA env vars, and installing a rebuild-varying
  leaf into the OS keychain is a machine mutation the kit must not require.
  The gate probes a bypass-free drive first every run; on this machine the
  probe fails (as expected), selecting the scoped bypass.
- **Bypass scoping discovery:** agent-browser's `--ignore-https-errors` is
  **daemon-global** (ignored when a daemon already runs), not per-session.
  The shipped discipline is therefore a daemon restart fence:
  `agent-browser close --all` before and after, so the bypass daemon exists
  only for the critique and never lingers for unrelated browsing. The
  cts-design-eye skill was amended to match (R4 self-amendment applied to
  the kit's own hour-old text).
- **Result:** 10/10 consecutive drives PASS (1–2 s each), screenshots in
  `tmp/screenshots/eyes-gate-run{1..10}.png`, request log clean —
  `--allowed-domains localhost.emobix.co.uk` at the browser plus a per-run
  log assertion (http/https URLs only; `data:` URIs excluded after a false
  positive on an inline SVG's `xmlns` string surfaced in run set 1).
- **Fallback not needed:** agent-browser ships as the eyes path;
  ce-test-browser remains usable.

## AE1 — fresh-user bootstrap, Claude Code

**Status: PASS (isolated).** After the operator performed the one
interactive `/login` into the sandbox profile
(`CLAUDE_CONFIG_DIR=~/.cache/cts-ui-kit-sandbox/claude-config`), a headless
run from a profile containing **no personal skills, plugins, or memory**:

- discovered and invoked the `cts-ui-setup` skill unprompted (transcript:
  one Skill call, 47 turns, 4.6 min);
- verified every pin drift-free (JDK 21.0.11 via the verified-major-version
  guard, npm 11.13.0, agent-browser 0.26.0, modern-web-guidance 0.0.174,
  compound-engineering 3.18.0 — installed INTO the sandbox profile);
- correctly left already-running services alone, and surfaced the exact
  manual steps (telemetry consent, Chromatic org grant, hook trust);
- could not launch a long-running Storybook under the headless allowlist
  and said so with the exact command — a permission-mode artifact, not a
  kit gap.

**Kit bug found and fixed by this gate:** on a fresh profile the plugin
marketplace registers as `compound-engineering-plugin`, not the
`every-marketplace` name the authoring machine carries — the skill's
install one-liner was corrected to the fresh-registration name.

## AE1 — fresh-user bootstrap, Codex CLI

**Status: PASS within sandbox limits (isolated CODEX_HOME, auth
hardlinked).** Codex announced "I'll use the `cts-ui-setup` skill since
this is the repo's documented path", executed the choreography (JDK 21,
corepack pin — catching that bare PATH npm is 11.17.0 and steering to
corepack — `npm ci`, pins, kit-symlink preflight PASS), changed no repo
files, and surfaced every OS-sandbox-blocked step (docker socket, port
binding, DNS) as exact run-this-in-your-terminal commands. Run under
`--sandbox workspace-write`; ~99k tokens.

### Earlier headless-auth discoveries (kept for the record)

Three failed pre-login attempts produced two recorded harness discoveries:

1. `claude -p --allowedTools "<list>" <prompt>` silently consumes the
   trailing prompt into the allowlist (greedy multi-value flag) — headless
   drivers must pass the prompt on stdin.
2. Fresh-profile auth cannot be seeded non-interactively: the OAuth token
   lives in the macOS Keychain paired with config-directory state. A
   symlinked `.credentials.json` (the file does not even exist on this
   machine), a seeded `~/.claude.json` (minimal and full-minus-personal
   variants), and a `CLAUDE_CONFIG_DIR` redirect all yield
   "Not logged in". Full fresh-user isolation therefore requires one
   interactive `claude /login` inside the sandbox profile — a user action.

## AE2 — MR !2075 replay

**Status: PASS on the rerun.** Environment: a **depth-1 shallow clone** of
the reverted branch (history mining impossible — one commit total), the
live stack swapped to serve the clone's tree (verified byte-identical via
the served page), isolated sandbox profile, plain feature ask with zero
workflow hints. 18.4 min, 76 turns.

The agent produced an **original** implementation (persistent
`_renderTestObjective` section — a different design than the human fix,
including a deliberate, JSDoc-documented divergence: no empty-state
placeholder), root-caused and removed a dead `cts-test-summary` element the
revert had left behind (claim verified independently: no module import
anywhere in the clone's tree, zero e2e references), added FAILED/RUNNING
persistence assertions plus a no-description absence story, and ran the
full gate chain — `test:ci` green (re-run independently by the operator
session), 53+67 storybook tests, 64 Playwright e2e.

Checklist verdicts (each verified directly against the clone tree):

- Documented spacing/tokens: **PASS** — the diff adds no CSS at all; new
  markup reuses existing token-routed classes.
- No callout treatment: **PASS** — flat labeled section, confirmed in code
  and pixels.
- Visual evidence: **PASS** — the agent asserted rendered DOM per state and
  honestly disclosed it could not capture pixels under its allowlist
  (`node`/MCP not allowlisted); the operator session captured
  desktop-1440/mobile-390 screenshots of the failed / running /
  no-description stories (`tmp/screenshots/ae2-*.png`), all on-system.
- Deterministic checks: **PASS**, independently corroborated.

Observation for R3: the rerun agent verified visually and ran the checks,
but did not formally invoke `cts-design-eye` via the Skill tool — the
skill-firing proof belongs to AE5, and the R3 directive may deserve
sharpening after AE5 evidence is in.

**Judging-methodology note:** a first attempt to score the diff with a
parallel lens panel was discarded — panel agents inherited the operator
checkout as working directory and cross-contaminated their evidence with
the human fix that exists there. All checklist verdicts above come from
direct verification against the clone.

### Attempt 1 (invalidated, findings retained)

The first replay ran pre-isolation on a throwaway branch of the operator
checkout with the operator's own config. Two findings improved the gate:

- **Finding 1 — the gate as planned is gameable.** The plan reverts
  `5f72e75df` in the sandbox, but the revert leaves the original fix in
  history. The agent mined it (`git show 5f72e75df`), re-applied it
  wholesale via `git revert --no-commit <revert>` + `git reset`, and
  proceeded to verification — exercising archaeology, not styling
  judgment. The rerun must **branch from the fix's parent** so the
  solution is absent from history (and the task framing should not name
  the historical fix).
- **Finding 2 — isolation is load-bearing, observed.** The agent read the
  operator's session transcript under `~/.claude/projects/...` and invoked
  a personal plugin skill (`superpowers:systematic-debugging`) — concrete
  evidence that a non-isolated profile contaminates exactly the signal AE2
  exists to measure.
- The run was externally stopped mid-verification (Playwright pass in
  flight). Tree state preserved as a patch in session scratch; the
  `ae2-replay` branch was reset and the checkout restored.

## AE5 — design-eye fires inside a ce-work loop

**Status: PASS on round 3 — and the road there is the more valuable
evidence.** All rounds ran in the isolated sandbox profile
(compound-engineering 3.18.0 installed there by AE1's bootstrap).

- **Round 1 (invalid premise, good behavior):** the chosen task (drawer
  chevrons) turned out to already exist; the agent investigated, refused to
  fabricate a diff, and stopped. Also discovered: a `/ce-work` slash prefix
  on `claude -p` stdin does NOT expand the skill — headless drivers must
  ask for the Skill tool explicitly.
- **Round 2 (ce-work ran, design-eye did not fire):** on a
  planted-by-fixture token-drift defect (footer: raw px + hex), ce-work
  drove a perfect four-token fix with all 8 `test:ci` gates green — but
  invoked ce-work's mechanical-diff review carve-out and skipped
  `cts-design-eye`. Combined with the AE2 rerun (which verified visually
  but never invoked the skill by name), the pattern: **the original R3
  wording produced verification, not skill routing.**
- **Directive sharpened (R4 loop, gated):** the root AGENTS.md bullet now
  states that mechanical-diff review carve-outs do not waive the
  visual-review step. The first version of that edit **tripped
  `lint-agents-budget.sh`** (static path 12,408/12,288) and was trimmed to
  fit — the budget gate fired on the kit's own author within hours of
  landing.
- **Round 3 (same task, sharpened layer): PASS.** Transcript shows
  `ce-work` → `cts-design-eye` invoked unprompted, real-pixel verification
  at 1440/390 via agent-browser against the live app, screenshots saved,
  fix committed. During the pixel pass the skill also surfaced a
  pre-existing CSS-specificity issue (a later `.oidf-footer` rule overrides
  most of the `.pageFooter` block) and **flagged it without scope-creeping
  into a fix** — precisely the judgment loop the kit exists to supply.

## Degraded-mode test

**Status: PASS, in two variants that together cover R6 better than the
planned single test.**

- **Mid-run kill (resilience variant):** Storybook was killed immediately
  after a design-eye run's preflight passed. The run absorbed it through
  the skill's documented fallbacks — the vitest story runner self-hosts
  (53/53 play + axe tests without the dev server) and pixels came from the
  still-live app — so verification stayed real and no banner was owed.
  Verdict "ship it" with full evidence; screenshots
  (`tmp/screenshots/objective-*.png`) show the objective persisting in
  failed/running states and the no-description collapse.
- **Strict bypass (contract variant):** with the app AND Storybook down
  and the operator explicitly declining a restart, the review led with the
  exact "NOT visually verified — degraded mode" banner, refused to claim
  anything renders correctly, framed itself as "a triage note, not a
  sign-off", recommended the verified rerun — and its top finding was
  precisely the visual question a static read cannot answer (whether two
  stacked chrome-less `.ctsHero` sections read as distinct zones),
  correctly deferred to the pixel pass.

## Sandbox artifacts and restoration

- Main-repo app and Storybook restored after the gates; Docker services
  (Mongo 6.0.13 + nginx) ran throughout.
- The dev database was never written by any gate; the pre-gate `mongodump`
  archive stays in session scratch and can be discarded.
- Sandbox clones are session-scratch artifacts (auto-cleaned with the
  session); the `ae2-replay` helper branch is deleted after evidence
  extraction; the AE2 rerun diff is preserved as a patch alongside the
  transcripts.
- The interactive sandbox profiles (`~/.cache/cts-ui-kit-sandbox/` —
  logged-in Claude profile + hardlinked Codex auth) were operator-created;
  deleting that directory retires both (the hardlink removal does not
  affect the original auth file). Recommended once this report ships.

## Codex CLI column (parity)

Observed on **codex-cli 0.142.5** — not the 0.45.0 the plan assumed for
this machine; the 0.45.0 feature floor remains a question for the team's
actual installs and is recorded as such. On 0.142.5: AGENTS.md path
concatenation, `.agents/skills/` discovery (all three kit skills resolve by
name), project `.codex/config.toml` MCP load (the streamable-http client
connected to storybook-mcp), and PostToolUse hooks all work. Hooks sit
behind a feature flag (`[features].hooks`; the deprecated
`[features].codex_hooks` still fires with a warning) — the bootstrap
skill's trust step is where a fresh teammate learns this.

## Parity matrix (F1/F2 steps × harness)

| Step | Claude Code | Codex CLI (0.142.5) |
|---|---|---|
| Root instructions load | PASS (AGENTS.md via CLAUDE.md import) | PASS (native) |
| Nested instructions load | PASS (shim smoke) | PASS (path concatenation smoke) |
| Skills discovery | PASS (all three kit skills, incl. from the isolated profile) | PASS (all three by name) |
| Bootstrap run (F1) | PASS — isolated profile, skill invoked unprompted | PASS within sandbox limits — skill invoked unprompted |
| Edit hooks fire (F2) | Plumbing PASS; in-session firing needs the one-time settings approval (headless sessions skip unapproved hooks) | PASS — Completed/Failed tracks our check's exit code; hook output NOT fed back to the model (asymmetry; compensated by the test:ci directive) |
| Storybook MCP | PASS (`.mcp.json`, live preview URL verified) | PASS (`.codex/config.toml` loaded; client connected) |
| Eyes path | PASS (eyes gate 10/10) | Harness-agnostic (same CLI) |

## Evidence hygiene

- Secret scan over `docs/research/` (private-key blocks, `sk-ant-`,
  api-key/bearer/password patterns): **clean**.
- Screenshots committed nowhere; the 10 eyes-gate PNGs live in gitignored
  `tmp/screenshots/` and show only the plans page rendered by the dev
  profile's synthetic admin.
- Pre-gate `mongodump` (3.6 MB) retained in session scratch for the
  duration of the validation work.

## Remaining (explicit, not silently skipped)

1. Marcus's real-teammate bootstrap run (time-boxed one week per the
   Definition of Done; ship regardless and record first real use if he is
   unavailable).
2. The Codex **0.45.0** feature floor on the team's actual installs — this
   machine validated 0.142.5; if a teammate runs 0.45.0, re-observe skills
   discovery, hooks, and `.codex` config there before asserting parity.
3. Claude Code in-session hook firing on teammates' machines requires the
   one-time project-settings approval (headless sessions skip unapproved
   hooks) — first interactive session after merge is the observation
   point; the bootstrap skill directs the approval.

## Recorded environment observations

- `/usr/libexec/java_home -v 21` resolves JDK **26** on this machine (a
  minimum-version filter) — the false-positive the preflight and bootstrap
  skill now guard against.
- Host npm is 11.6.2 vs the 11.13.x pin — preflight correctly FAILs that
  row (true drift report; corepack enablement is the user's call).
- `DISABLE_TELEMETRY=1` verified against modern-web-guidance 0.0.174
  package source (README §Telemetry and `modern-web.mjs` read exactly that
  variable).
