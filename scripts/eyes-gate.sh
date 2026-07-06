#!/usr/bin/env bash
# Eyes-gate: prove the agent "eyes" path against the local HTTPS app is
# reliable — N consecutive drives with zero failures — before the UI
# enablement kit may rely on it (and as a post-ship health check).
#
# What one drive does (mirrors the cts-design-eye skill's discipline):
#   - a DEDICATED agent-browser session (--session) used only for the target
#     origin, with navigation restricted to that host (--allowed-domains);
#   - open the app root over HTTPS, wait for the page, screenshot it into
#     tmp/screenshots/;
#   - read the session's network request log and assert every requested URL
#     stays on the allowlisted origin;
#   - close the session.
#
# Certificate trust (the KTD4 evaluation, resolved):
#   The dev proxy's cert is a self-signed leaf generated fresh at nginx image
#   build (nginx/Dockerfile-nodocker) — there is no stable committed CA to
#   trust, Chromium does not honor per-process CA env vars, and installing a
#   rebuild-varying leaf into the OS keychain is a system-level mutation this
#   kit must not require. The gate therefore PROBES the stronger control
#   first: run 1 attempts the drive WITHOUT --ignore-https-errors; if the
#   machine's trust store already accepts the cert, the whole gate runs
#   bypass-free. Otherwise every session uses --ignore-https-errors, scoped
#   to this dedicated session only (never a global default).
#
# Usage: eyes-gate.sh [-n RUNS] [-u URL]
#   Defaults: RUNS=10, URL=https://localhost.emobix.co.uk:8443/
#
# Exits 0 when all N drives pass AND the request log never leaves the
# allowlisted host; 1 on any drive failure or origin escape; 2 on
# infrastructure error (agent-browser missing, target not listening).

set -uo pipefail

RUNS=10
URL="https://localhost.emobix.co.uk:8443/"
while getopts "n:u:" opt; do
  case "$opt" in
    n) RUNS=$OPTARG ;;
    u) URL=$OPTARG ;;
    *) echo "usage: eyes-gate.sh [-n RUNS] [-u URL]" >&2; exit 2 ;;
  esac
done

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/.." &>/dev/null && pwd)
SHOT_DIR="$REPO_ROOT/tmp/screenshots"
mkdir -p "$SHOT_DIR"

HOST=$(printf '%s' "$URL" | sed -E 's#^https?://([^/:]+).*#\1#')

if ! command -v agent-browser >/dev/null 2>&1; then
  echo "eyes-gate: agent-browser not found — install per the cts-ui-setup skill" >&2
  exit 2
fi
if ! curl -sk --max-time 8 -o /dev/null "$URL"; then
  echo "eyes-gate: nothing answering at $URL — start the stack (cts-ui-setup) first" >&2
  exit 2
fi

# The bypass flag is DAEMON-global: agent-browser ignores --ignore-https-errors
# when a daemon is already running. Fence the gate with a daemon restart so
# the flag state is exactly what each phase requests, and close everything at
# the end so no bypass-enabled daemon lingers for unrelated browsing.
agent-browser close --all >/dev/null 2>&1

# --- Trust probe: can we drive WITHOUT the bypass? ---------------------------
BYPASS="--ignore-https-errors"
probe_session="eyes-gate-probe-$$"
if agent-browser --session "$probe_session" --allowed-domains "$HOST" \
     open "$URL" >/dev/null 2>&1; then
  probe_url=$(agent-browser --session "$probe_session" get url 2>/dev/null)
  case "$probe_url" in
    https://"$HOST"*) BYPASS=""
       echo "eyes-gate: trust probe PASSED — OS trust store accepts the dev cert; running bypass-free" ;;
    *) echo "eyes-gate: trust probe inconclusive (landed on '$probe_url') — using scoped --ignore-https-errors" ;;
  esac
else
  echo "eyes-gate: trust probe failed (expected for the self-signed dev cert) — using scoped --ignore-https-errors"
fi
agent-browser --session "$probe_session" close >/dev/null 2>&1
# Restart the daemon so the drive phase gets its requested flag state (the
# probe may have started a bypass-less daemon).
agent-browser close --all >/dev/null 2>&1

# --- N consecutive drives -----------------------------------------------------
fails=0
escapes=0
i=1
while [ "$i" -le "$RUNS" ]; do
  session="eyes-gate-$$-$i"
  shot="$SHOT_DIR/eyes-gate-run$i.png"
  t0=$(date +%s)
  ok=1

  # shellcheck disable=SC2086  # $BYPASS is deliberately empty or one flag
  agent-browser --session "$session" $BYPASS --allowed-domains "$HOST" \
    open "$URL" >/dev/null 2>&1 || ok=0

  if [ "$ok" -eq 1 ]; then
    landed=$(agent-browser --session "$session" $BYPASS get url 2>/dev/null)
    case "$landed" in
      https://"$HOST"*) : ;;
      *) ok=0 ;;
    esac
  fi

  if [ "$ok" -eq 1 ]; then
    agent-browser --session "$session" $BYPASS screenshot "$shot" >/dev/null 2>&1 || ok=0
    [ -s "$shot" ] || ok=0
  fi

  # Origin assertion from the session's own request log: every http(s)
  # request must target the allowlisted host. Parse the URL column (field 3
  # of "[id] METHOD <url> (Type) [status]") and keep only real http(s)
  # fetches — data: URIs embed xmlns strings like http://www.w3.org/2000/svg
  # that are not network requests.
  if [ "$ok" -eq 1 ]; then
    offlist=$(agent-browser --session "$session" $BYPASS network requests 2>/dev/null \
      | awk '{print $3}' | grep -E '^https?://' | sed -E 's#^https?://##; s#[/:].*$##' \
      | sort -u | grep -vxF "$HOST" || true)
    if [ -n "$offlist" ]; then
      echo "run $i: ORIGIN ESCAPE — request log contains non-allowlisted host(s): $offlist" >&2
      escapes=$((escapes + 1))
      ok=0
    fi
  fi

  agent-browser --session "$session" close >/dev/null 2>&1
  t1=$(date +%s)

  if [ "$ok" -eq 1 ]; then
    echo "run $i/$RUNS: PASS ($((t1 - t0))s, $shot)"
  else
    echo "run $i/$RUNS: FAIL ($((t1 - t0))s)" >&2
    fails=$((fails + 1))
  fi
  i=$((i + 1))
done

# Leave no bypass-enabled daemon behind for unrelated browsing.
agent-browser close --all >/dev/null 2>&1

mode_desc="dedicated --ignore-https-errors daemon (restart-fenced)"
[ -z "$BYPASS" ] && mode_desc="bypass-free (OS trust store)"

if [ "$fails" -gt 0 ]; then
  echo "eyes-gate: FAILED — $fails/$RUNS drives failed ($escapes origin escape(s)); mode: $mode_desc" >&2
  echo "eyes-gate: if failures persist, fall back to Playwright MCP per the kit plan and re-run this gate" >&2
  exit 1
fi
echo "eyes-gate: PASSED — $RUNS/$RUNS consecutive drives, request log clean; mode: $mode_desc"
