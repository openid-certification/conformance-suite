---
name: cts-ui-setup
description: Use when setting up or checking a machine for conformance-suite UI work — clean checkout, app or Storybook not running, scripts/ui-preflight.sh failures, missing JDK 21 / pinned npm / Docker, per-user agent tooling not installed, or harness hooks and repo trust unverified
---

# Conformance Suite UI Development Setup

Take a clean checkout to a running app and Storybook with the per-user UI tooling installed, in either Claude Code or Codex CLI. Every install below is pinned to the version this kit was authored against; on re-run, compare against the pins and **flag drift — never silently upgrade**. This skill leaves services running and reports how to stop them; it does not stop them itself. It never runs state-changing git.

## Process

### 1. Toolchain checks

Verify the three prerequisites before anything else.

**JDK 21 — CRITICAL LANDMINE.** `/usr/libexec/java_home -v 21` is a *minimum-version filter*, not a "give me 21" selector. On a machine whose newest JDK is 26 it returns JDK 26, and newer JDKs crash Error Prone mid-build. Resolve a candidate, then **verify the resolved java actually reports major 21**:

```bash
# Try java_home, then the Homebrew openjdk@21 keg; keep the first that reports 21.
export JAVA_HOME=""
for c in "$(/usr/libexec/java_home -v 21 2>/dev/null)" \
         /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home; do
  if [ -x "$c/bin/java" ] && "$c/bin/java" -version 2>&1 | grep -q 'version "21'; then
    export JAVA_HOME="$c"; break
  fi
done
[ -n "$JAVA_HOME" ] || { echo "No real JDK 21 found — install: brew install openjdk@21"; exit 1; }
"$JAVA_HOME/bin/java" -version   # MUST print 21.x. If it prints 26+, the filter lied — do not proceed.
```

Pin: this machine's real JDK 21 is `openjdk 21.0.11` at `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`. Reuse `$JAVA_HOME` from this step for the Maven start in step 4.

**corepack npm pin.** npm is pinned by the `packageManager` field in `frontend/package.json` (currently `npm@11.13.0`, i.e. 11.13.x). Enable corepack and confirm the pin resolves:

```bash
corepack enable npm
grep packageManager frontend/package.json          # read the live pin — do not hardcode
( cd frontend && npm --version )                    # must match the pinned major.minor (11.13)
```

**Docker present and running.**

```bash
docker version --format '{{.Server.Version}}' \
  || echo "Docker daemon not running — start Docker Desktop / colima and retry"
```

### 2. Services (MongoDB + HTTPS proxy)

macOS native-server path (Spring runs on the host, only Mongo + Nginx in containers):

```bash
docker compose -f docker-compose-dev-mac-nodocker.yml up -d
```

Containerized alternative (whole stack in Docker): `docker-compose-dev.yml`.

**LANDMINE — Mongo must be 6.x.** Mongo 7 crashes app boot via an FCV downgrade. And a stray `mongo:7` container from another project can squat port 27017 — check *who owns the port* before assuming the compose file's Mongo is the one listening:

```bash
docker ps --filter publish=27017 --format '{{.Image}}\t{{.Names}}'   # image tag MUST be mongo:6.x
```

If a non-6.x image owns 27017, stop that container (it belongs to another project) and re-run the compose `up`.

### 3. Frontend dependencies

```bash
cd frontend && npm ci --ignore-scripts
```

Do **not** regenerate the lockfile here. If `npm ci` fails on a lockfile mismatch, the cross-platform regen recipe lives in `frontend/AGENTS.md` "Lockfile rules" (it records the Linux optional deps CI needs) — follow it there, do not improvise.

### 4. Start app + Storybook

**Port-conflict detection first** — a leftover process on any of these ports makes the readiness poll below hang forever:

```bash
lsof -nP -iTCP:8080 -iTCP:8443 -iTCP:6006 -sTCP:LISTEN   # expect no output on a clean machine
```

Start the app in the background with the **verified** JDK 21 from step 1, then poll the HTTPS proxy until it answers:

```bash
JAVA_HOME="$JAVA_HOME" mvn spring-boot:run -Dspring-boot.run.profiles=dev   # run in background
until curl -sk https://localhost.emobix.co.uk:8443/ -o /dev/null; do sleep 3; done && echo "app up"
```

Start Storybook in the background and poll it:

```bash
( cd frontend && npm run storybook )                                        # run in background
until curl -s http://localhost:6006 -o /dev/null; do sleep 3; done && echo "storybook up"
```

**Ownership.** These processes stay running when the skill finishes. To stop them: Ctrl-C (or kill) the two background jobs, and `docker compose -f docker-compose-dev-mac-nodocker.yml down` to stop Mongo + Nginx. The skill does not stop them for you — report this to the user.

### 5. Per-user tool installs (pinned, drift-flagged)

Each version below was pinned at kit-authoring time. On re-run, verify the installed version equals the pin and **flag drift; do not silently upgrade**.

**compound-engineering** (Claude Code plugin — the `ce-*` skills). Pinned to **3.18.0** (gitCommitSha `1287b0985ce6a6a2a0b35e2302df5658b171cc6d`), from the `every-marketplace` marketplace:

```bash
claude plugin marketplace add EveryInc/compound-engineering-plugin
# The marketplace registers under the name the ADD command prints — on a
# fresh profile that is compound-engineering-plugin (older installs may
# show every-marketplace). Use whatever name the add step reported:
claude plugin install compound-engineering@compound-engineering-plugin
ls ~/.claude/plugins/cache/*/compound-engineering/   # expect a 3.18.0 dir
```

The Claude Code plugin CLI installs *marketplace-latest* — there is no `@version` pin in the install command — so enforce the pin by checking the resolved version dir equals `3.18.0` and flagging anything newer as drift. **Codex caveat:** compound-engineering is a Claude Code plugin; Codex CLI has no plugin marketplace, so these skills are unavailable under Codex unless separately mirrored into `.agents/skills`. State this honestly rather than pretending the install worked cross-harness.

**modern-web-guidance** (GoogleChrome, Apache-2.0). Runtime is `npx`; the content is pinned by the `--skill-version` flag, which every invocation must carry. Pinned to npm **0.0.174**, content version **`2026_05_16-c5e7870`** (upstream commit `c5e7870`):

```bash
npx -y modern-web-guidance@0.0.174 list --skill-version 2026_05_16-c5e7870   # smoke test
```

The user-global skill copy lives at `~/.agents/skills/modern-web-guidance` (reachable via `~/.claude/skills/modern-web-guidance`); if absent, it is re-fetched by the first `npx` call. **Telemetry:** with the user's consent, persist the opt-out in their shell profile rather than passing it once:

```bash
grep -q 'DISABLE_TELEMETRY=1' ~/.zshrc || echo 'export DISABLE_TELEMETRY=1' >> ~/.zshrc   # ask first
```

**agent-browser CLI.** Pinned to the version installed on this machine, **0.26.0** (npm-latest is newer — do not chase it; the pin is what the kit was validated against). Its `postinstall` downloads a browser binary — that is accepted install-time execution:

```bash
npm install -g agent-browser@0.26.0
agent-browser --version   # expect 0.26.0
```

### 6. Harness trust verification

Trust is not documentation — an untrusted hook is a *silently skipped* hook, so verify the state, do not assume it.

**Claude Code.** Project-settings hooks in `.claude/settings.json` require one-time approval; the settings-changed prompt appears on session start in this repo. Approve it. If you pulled changes to `.claude/`, `.agents/`, or hook config, **restart the session** so the new settings are re-read and re-approved.

**Codex CLI.** Two layers: repo trust, and hook-trust *freshness*. Codex trusts hooks per config hash — after **any** change to `.codex/hooks.json` the user must re-run the hook trust review, or the hooks are silently skipped with no error. Verify the current trust state, and restart the session after pulling changes to `.claude/`, `.codex/`, or `.agents/`.

### 7. Kit symlink verification / repair

Run the preflight, which checks that every git-tracked `.claude/skills` entry resolves into `.agents/skills` and none has been materialized as a plain file:

```bash
scripts/ui-preflight.sh
```

- If it reports a symlink **materialized as a plain file**, recreate the relative link (same shape as the other kit symlinks):
  ```bash
  rm .claude/skills/<name> && ln -s ../../.agents/skills/<name> .claude/skills/<name>
  ```
- If it reports a target that **escapes `.agents/skills/`**, **STOP** — this is possible tampering. Do not auto-repair an escape; flag it to the user and wait.

### 8. Surface the manual steps

These cannot be automated — list them explicitly at the end so the user knows what still needs a human:

- **Plugin marketplace consent prompts** (step 5) — Claude Code asks the user to approve the marketplace and each plugin install interactively.
- **Harness hook-trust approvals** (step 6) — the Claude Code settings prompt and the Codex hook-trust review both require the user to accept.
- **Chromatic app access** — an organizational grant the repo cannot provide; the user must be added to the Chromatic project out-of-band.

### 9. Final preflight + drift report

Re-run the preflight and present its table as the completion signal:

```bash
scripts/ui-preflight.sh   # exit 0 + all-green table = done
```

On a re-run of this whole skill, compare each installed version against its pin (JDK 21, npm 11.13.x, compound-engineering 3.18.0, modern-web-guidance 0.0.174 / `2026_05_16-c5e7870`, agent-browser 0.26.0) and **report any drift without changing anything**. A green table with no drift is the finished state.
