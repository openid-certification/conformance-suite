#!/usr/bin/env bash
# UI enablement kit — environment preflight.
#
# Deterministic service/tooling probes that the UI setup + review skills (and
# any harness hook) can shell out to before starting UI work. Prints a
# pass/fail table with a start-command hint for every failing probe.
#
# Probes:
#   - MongoDB reachable          (TCP connect to 127.0.0.1:27017)
#   - HTTPS reverse proxy / app  (curl https://localhost.emobix.co.uk:8443)
#   - Spring app                 (curl http://localhost:8080 — any response = up)
#   - Storybook + /mcp endpoint  (curl http://localhost:6006 and .../mcp)
#   - JDK 21 available           (java -version == 21, or a real 21 via java_home)
#   - frontend/node_modules      (present)
#   - npm major.minor            (matches the packageManager pin in package.json)
#   - Kit symlink integrity      (git-tracked .claude/skills symlinks resolve
#                                 into .agents/skills, none materialized as files)
#
# Exit codes:  0 = every probe passed   1 = one or more probes failed
#
# The script never mutates anything and works from any CWD (the repo root is
# resolved from the script's own location, not $PWD). The only git it runs is
# the read-only `git ls-files`. No network egress beyond localhost probes.
#
# Test seams (env overrides — for exercising the down/up paths in tests only;
# leave unset in normal use):
#   UI_PREFLIGHT_MONGO_HOST      default 127.0.0.1
#   UI_PREFLIGHT_MONGO_PORT      default 27017
#   UI_PREFLIGHT_PROXY_URL       default https://localhost.emobix.co.uk:8443/
#   UI_PREFLIGHT_APP_URL         default http://localhost:8080/
#   UI_PREFLIGHT_STORYBOOK_URL   default http://localhost:6006
#   UI_PREFLIGHT_TIMEOUT         default 4   (seconds; curl + TCP connect budget)

# Deliberately NOT `set -e`: this is an aggregator whose probes are *expected*
# to fail individually and must all run so the table is complete. We keep
# nounset + pipefail for the usual safety.
set -uo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/.." &>/dev/null && pwd)

MONGO_HOST=${UI_PREFLIGHT_MONGO_HOST:-127.0.0.1}
MONGO_PORT=${UI_PREFLIGHT_MONGO_PORT:-27017}
PROXY_URL=${UI_PREFLIGHT_PROXY_URL:-https://localhost.emobix.co.uk:8443/}
APP_URL=${UI_PREFLIGHT_APP_URL:-http://localhost:8080/}
STORYBOOK_URL=${UI_PREFLIGHT_STORYBOOK_URL:-http://localhost:6006}
TIMEOUT=${UI_PREFLIGHT_TIMEOUT:-4}

# Aggregated rows: "STATUS|CHECK|DETAIL|HINT" (fields never contain '|').
ROWS=()
ANY_FAIL=0

record() { # status check detail hint
  ROWS+=("$1|$2|$3|$4")
  if [ "$1" = "FAIL" ]; then
    ANY_FAIL=1
  fi
}

# TCP connect probe with a hard timeout, bash-3.2 compatible (no `nc`, no
# associative arrays). A hung connect (unreachable host via an env override)
# is killed by the watchdog and reported as down.
tcp_probe() { # host port
  local host=$1 port=$2
  ( exec 3<>"/dev/tcp/$host/$port" ) 2>/dev/null &
  local cpid=$!
  ( sleep "$TIMEOUT"; kill -9 "$cpid" 2>/dev/null ) &
  local wpid=$!
  if wait "$cpid" 2>/dev/null; then
    kill "$wpid" 2>/dev/null; wait "$wpid" 2>/dev/null
    return 0
  fi
  kill "$wpid" 2>/dev/null; wait "$wpid" 2>/dev/null
  return 1
}

# Echo the HTTP status code if curl connected, else echo "" and return 1.
# `-k` tolerates the self-signed dev cert; a 4xx/5xx still means "up".
http_probe() { # url
  local url=$1 code rc
  if ! command -v curl >/dev/null 2>&1; then
    return 2
  fi
  code=$(curl -k -s -o /dev/null -w '%{http_code}' --max-time "$TIMEOUT" "$url" 2>/dev/null)
  rc=$?
  if [ "$rc" -eq 0 ]; then
    printf '%s\n' "$code"
    return 0
  fi
  printf '%s\n' "$rc"
  return 1
}

# Major version reported by a java binary ("21.0.11" -> 21; "1.8.0" -> 8).
java_major() { # java-binary
  local out
  out=$("$1" -version 2>&1 | head -1) || return 1
  case "$out" in
    *'"1.'*) printf '%s\n' "$out" | sed -E 's/.*version "1\.([0-9]+).*/\1/' ;;
    *)       printf '%s\n' "$out" | sed -E 's/.*version "([0-9]+).*/\1/' ;;
  esac
}

# ---------------------------------------------------------------------------
# 1. MongoDB
# ---------------------------------------------------------------------------
if tcp_probe "$MONGO_HOST" "$MONGO_PORT"; then
  record PASS "MongoDB ($MONGO_HOST:$MONGO_PORT)" "TCP connection accepted" ""
else
  record FAIL "MongoDB ($MONGO_HOST:$MONGO_PORT)" "no TCP connection" \
    "docker compose -f docker-compose-dev-mac-nodocker.yml up -d"
fi

# ---------------------------------------------------------------------------
# 2. HTTPS reverse proxy / app (8443)
# ---------------------------------------------------------------------------
proxy_code=$(http_probe "$PROXY_URL"); proxy_rc=$?
if [ "$proxy_rc" -eq 0 ]; then
  record PASS "HTTPS proxy (8443)" "HTTP $proxy_code (self-signed cert OK)" ""
elif [ "$proxy_rc" -eq 2 ]; then
  record FAIL "HTTPS proxy (8443)" "curl not found" "install curl"
else
  record FAIL "HTTPS proxy (8443)" "no response (curl exit $proxy_code)" \
    "start the nginx proxy: docker compose -f docker-compose-dev-mac-nodocker.yml up -d"
fi

# ---------------------------------------------------------------------------
# 3. Spring app (8080) — any HTTP response counts as up
# ---------------------------------------------------------------------------
app_code=$(http_probe "$APP_URL"); app_rc=$?
if [ "$app_rc" -eq 0 ]; then
  record PASS "Spring app (8080)" "HTTP $app_code" ""
elif [ "$app_rc" -eq 2 ]; then
  record FAIL "Spring app (8080)" "curl not found" "install curl"
else
  record FAIL "Spring app (8080)" "no response (curl exit $app_code)" \
    "JAVA_HOME=\$(/usr/libexec/java_home -v 21) mvn spring-boot:run -Dspring-boot.run.profiles=dev"
fi

# ---------------------------------------------------------------------------
# 4. Storybook (6006) + /mcp endpoint
# ---------------------------------------------------------------------------
sb_code=$(http_probe "${STORYBOOK_URL%/}/"); sb_rc=$?
if [ "$sb_rc" -eq 0 ]; then
  mcp_code=$(http_probe "${STORYBOOK_URL%/}/mcp"); mcp_rc=$?
  if [ "$mcp_rc" -eq 0 ]; then
    record PASS "Storybook (6006)" "root HTTP $sb_code, /mcp HTTP $mcp_code" ""
  else
    record PASS "Storybook (6006)" "root HTTP $sb_code, /mcp unreachable" ""
  fi
elif [ "$sb_rc" -eq 2 ]; then
  record FAIL "Storybook (6006)" "curl not found" "install curl"
else
  record FAIL "Storybook (6006)" "no response (curl exit $sb_code)" \
    "cd frontend && npm run storybook"
fi

# ---------------------------------------------------------------------------
# 5. JDK 21
#
# `java -version == 21` is the primary, reliable signal. The java_home
# fallback is deliberately strict: `/usr/libexec/java_home -v 21` selects the
# highest JDK whose major is >= 21 (the flag is a *minimum*, not an exact
# match), so on a box with only JDK 26 it happily returns the 26 home. We
# therefore verify the *resolved* binary actually reports major 21 before
# trusting it — a false "21 available" would let the build crash Error Prone.
# ---------------------------------------------------------------------------
jdk_ok=0
jdk_detail="no JDK 21 found"
if command -v java >/dev/null 2>&1; then
  pm=$(java_major java 2>/dev/null)
  if [ "$pm" = "21" ]; then
    jdk_ok=1
    jdk_detail="java -version reports ${pm}.x"
  fi
fi
if [ "$jdk_ok" -eq 0 ] && [ -x /usr/libexec/java_home ]; then
  jh=$(/usr/libexec/java_home -v 21 2>/dev/null)
  if [ -n "$jh" ] && [ -x "$jh/bin/java" ]; then
    hm=$(java_major "$jh/bin/java" 2>/dev/null)
    if [ "$hm" = "21" ]; then
      jdk_ok=1
      jdk_detail="JDK 21 at $jh"
    else
      jdk_detail="java_home -v 21 resolved to a JDK ${hm} (>=21 filter, not a real 21)"
    fi
  fi
fi
if [ "$jdk_ok" -eq 1 ]; then
  record PASS "JDK 21" "$jdk_detail" ""
else
  record FAIL "JDK 21" "$jdk_detail" \
    "install JDK 21 and export JAVA_HOME (default JDK 26 crashes Error Prone here)"
fi

# ---------------------------------------------------------------------------
# 6. frontend/node_modules present
# ---------------------------------------------------------------------------
if [ -d "$REPO_ROOT/frontend/node_modules" ]; then
  record PASS "frontend/node_modules" "present" ""
else
  record FAIL "frontend/node_modules" "missing" "cd frontend && npm ci --ignore-scripts"
fi

# ---------------------------------------------------------------------------
# 7. npm major.minor matches the packageManager pin
# ---------------------------------------------------------------------------
pkg_json="$REPO_ROOT/frontend/package.json"
pin_mm=$(grep -E '"packageManager"' "$pkg_json" 2>/dev/null \
  | sed -E 's/.*"npm@([0-9]+\.[0-9]+)\.[0-9]+.*/\1/')
if [ -z "$pin_mm" ] || printf '%s' "$pin_mm" | grep -q '[^0-9.]'; then
  record FAIL "npm version pin" "could not read packageManager pin from package.json" \
    "check the \"packageManager\" field in frontend/package.json"
elif ! command -v npm >/dev/null 2>&1; then
  record FAIL "npm version pin" "npm not found (pin $pin_mm)" "corepack enable"
else
  have=$(npm --version 2>/dev/null)
  have_mm=$(printf '%s' "$have" | sed -E 's/^([0-9]+\.[0-9]+).*/\1/')
  if [ "$have_mm" = "$pin_mm" ]; then
    record PASS "npm version pin" "npm $have matches pin $pin_mm" ""
  else
    record FAIL "npm version pin" "npm $have, pin wants $pin_mm.x" "corepack enable"
  fi
fi

# ---------------------------------------------------------------------------
# 8. Kit symlink integrity
#
# For every git-TRACKED entry under .claude/skills recorded as a symlink
# (mode 120000): it must be a symlink on disk (not materialized into a plain
# file by a symlink-less checkout) AND resolve inside .agents/skills. A target
# escaping .agents/skills is flagged loudly and never offered auto-repair.
# ---------------------------------------------------------------------------
kit_detail=""
kit_hint=""
kit_status=""
if ! command -v git >/dev/null 2>&1 || ! git -C "$REPO_ROOT" rev-parse >/dev/null 2>&1; then
  kit_status=FAIL
  kit_detail="not a git checkout / git unavailable"
  kit_hint="run inside the repository"
else
  tracked=$(git -C "$REPO_ROOT" ls-files -s -- .claude/skills 2>/dev/null)
  link_count=0
  bad=()
  agents_prefix="$REPO_ROOT/.agents/skills/"
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    mode=${line%% *}
    [ "$mode" = "120000" ] || continue
    # `git ls-files -s` is: <mode> <sha> <stage>\t<path>
    path=${line#*$'\t'}
    link_count=$((link_count + 1))
    abspath="$REPO_ROOT/$path"
    if [ ! -L "$abspath" ]; then
      bad+=("$path: tracked as a symlink but is a plain file on disk (materialized) — recreate the symlink into .agents/skills")
      continue
    fi
    target=$(readlink "$abspath")
    linkdir=$(dirname "$abspath")
    resolved_dir=$(cd "$linkdir" && cd "$(dirname "$target")" 2>/dev/null && pwd)
    if [ -z "$resolved_dir" ]; then
      bad+=("$path: broken symlink (target '$target' does not resolve) — recreate the symlink into .agents/skills")
      continue
    fi
    canonical="$resolved_dir/$(basename "$target")"
    case "$canonical/" in
      "$agents_prefix"*) : ;; # inside .agents/skills — good
      *) bad+=("$path: symlink target escapes .agents/skills -> '$canonical' — DO NOT auto-repair; investigate possible tampering") ;;
    esac
  done <<< "$tracked"

  if [ "${#bad[@]}" -gt 0 ]; then
    kit_status=FAIL
    kit_detail="${#bad[@]} of $link_count tracked symlink(s) invalid"
    kit_hint="${bad[0]}"
  elif [ "$link_count" -eq 0 ]; then
    kit_status=PASS
    kit_detail="no kit symlinks tracked yet"
  else
    kit_status=PASS
    kit_detail="$link_count kit symlink(s) resolve into .agents/skills"
  fi
fi
record "$kit_status" "Kit symlinks" "$kit_detail" "$kit_hint"

# ---------------------------------------------------------------------------
# Render the table
# ---------------------------------------------------------------------------
printf '\n%-6s  %-28s  %s\n' "STATUS" "CHECK" "DETAIL"
printf '%-6s  %-28s  %s\n' "------" "----------------------------" "------"
for row in "${ROWS[@]}"; do
  IFS='|' read -r st ck dt hn <<< "$row"
  printf '%-6s  %-28s  %s\n' "$st" "$ck" "$dt"
  if [ "$st" = "FAIL" ] && [ -n "$hn" ]; then
    printf '%-6s  %-28s  hint: %s\n' "" "" "$hn"
  fi
done

echo
if [ "$ANY_FAIL" -eq 0 ]; then
  echo "ui-preflight: all checks passed."
  exit 0
fi
echo "ui-preflight: one or more checks failed (see hints above)."
exit 1
