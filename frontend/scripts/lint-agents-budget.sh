#!/usr/bin/env bash
# Enforce the agent-instruction-layer size budgets and structural invariants.
#
# Why this exists:
#   Codex CLI reads at most 32 KiB (default) of combined AGENTS.md content on
#   the path from the repo root to the file being worked on, truncating
#   silently past the cap. The instruction layer is split into a lean root
#   plus nested files precisely to stay inside that budget; this gate turns
#   silent truncation into a loud CI failure when any file grows past its
#   share. Intermediate budgets are deliberately tighter than 32 KiB so the
#   deepest path (root -> static -> components) keeps headroom.
#
# What's checked:
#   1. Every instruction file exists (root + the four nested AGENTS.md).
#   2. Per-path combined byte budgets (see PATH BUDGETS below).
#   3. Every AGENTS.md has a sibling CLAUDE.md shim whose first non-empty
#      line is the `@AGENTS.md` import (root CLAUDE.md may carry extra
#      Claude-only prose; nested shims may add at most one extra line).
#   4. Every path listed in the root AGENTS.md "Conventions map" section
#      (lines shaped `- `path` — description`) resolves to a real file or
#      directory.
#
# Exits 0 on success, 1 on any budget/structure finding, 2 on infrastructure
# error.

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." &>/dev/null && pwd)

ROOT_MD="AGENTS.md"
FRONTEND_MD="frontend/AGENTS.md"
JAVA_MD="src/main/java/net/openid/conformance/AGENTS.md"
STATIC_MD="src/main/resources/static/AGENTS.md"
COMPONENTS_MD="src/main/resources/static/components/AGENTS.md"

findings=()

size_of() {
  # bytes, or -1 if missing
  local f="$REPO_ROOT/$1"
  if [[ -f "$f" ]]; then
    wc -c < "$f" | tr -d ' '
  else
    echo "-1"
  fi
}

for f in "$ROOT_MD" "$FRONTEND_MD" "$JAVA_MD" "$STATIC_MD" "$COMPONENTS_MD"; do
  if [[ ! -f "$REPO_ROOT/$f" ]]; then
    findings+=("$f: missing — the instruction layer requires it (see AGENTS.md conventions map)")
  fi
done

if (( ${#findings[@]} == 0 )); then
  r=$(size_of "$ROOT_MD")
  fe=$(size_of "$FRONTEND_MD")
  jv=$(size_of "$JAVA_MD")
  st=$(size_of "$STATIC_MD")
  co=$(size_of "$COMPONENTS_MD")

  # PATH BUDGETS (bytes). The 32768 figure is Codex's hard default cap
  # (project_doc_max_bytes); the others reserve headroom along each path.
  check_budget() {
    local label="$1" actual="$2" budget="$3" files="$4"
    if (( actual > budget )); then
      findings+=("$label: $actual bytes exceeds the $budget-byte budget ($files). Trim the newest additions or move deep content to an on-demand reference (frontend/README.md, docs/).")
    fi
  }
  check_budget "root path" "$r" 6144 "$ROOT_MD"
  check_budget "frontend path" "$((r + fe))" 10240 "$ROOT_MD + $FRONTEND_MD"
  check_budget "java path" "$((r + jv))" 20480 "$ROOT_MD + $JAVA_MD"
  check_budget "static path" "$((r + st))" 12288 "$ROOT_MD + $STATIC_MD"
  check_budget "components path (Codex hard cap)" "$((r + st + co))" 32768 "$ROOT_MD + $STATIC_MD + $COMPONENTS_MD"
fi

# Shim integrity: every AGENTS.md needs a sibling CLAUDE.md importing it.
check_shim() {
  local agents="$1" nested="$2"
  local dir shim first_nonempty nonempty_count
  dir=$(dirname -- "$REPO_ROOT/$agents")
  shim="$dir/CLAUDE.md"
  local rel="${shim#"$REPO_ROOT"/}"
  if [[ ! -f "$shim" ]]; then
    findings+=("$rel: missing CLAUDE.md shim — create it containing exactly '@AGENTS.md' so Claude Code loads $agents")
    return
  fi
  first_nonempty=$(grep -m1 -v '^[[:space:]]*$' "$shim" || true)
  if [[ "$first_nonempty" != "@AGENTS.md" && "$first_nonempty" != "# CLAUDE.md" ]]; then
    findings+=("$rel: first line must be the '@AGENTS.md' import (found: '$first_nonempty')")
  fi
  if ! grep -qx '@AGENTS.md' "$shim"; then
    findings+=("$rel: does not import '@AGENTS.md' — Claude Code will not load $agents")
  fi
  if [[ "$nested" == "nested" ]]; then
    nonempty_count=$(grep -cv '^[[:space:]]*$' "$shim" || true)
    if (( nonempty_count > 2 )); then
      findings+=("$rel: nested shims must stay minimal (the @AGENTS.md import plus at most one note line; found $nonempty_count non-empty lines). Put substance in $agents instead.")
    fi
  fi
}
check_shim "$ROOT_MD" "root"
check_shim "$FRONTEND_MD" "nested"
check_shim "$JAVA_MD" "nested"
check_shim "$STATIC_MD" "nested"
check_shim "$COMPONENTS_MD" "nested"

# Conventions map: every mapped path must resolve. Map entries are bullet
# lines in the "## Conventions map" section shaped: - `path` — description
if [[ -f "$REPO_ROOT/$ROOT_MD" ]]; then
  map_paths=$(awk '/^## Conventions map/{flag=1; next} /^## /{flag=0} flag' "$REPO_ROOT/$ROOT_MD" \
    | sed -nE 's/^- `([^`]+)`.*/\1/p')
  if [[ -z "$map_paths" ]]; then
    findings+=("$ROOT_MD: no parseable entries under '## Conventions map' (expected lines shaped: - \`path\` — description)")
  else
    while IFS= read -r p; do
      [[ -z "$p" ]] && continue
      if [[ ! -e "$REPO_ROOT/$p" ]]; then
        findings+=("$ROOT_MD: conventions map entry '$p' does not resolve — fix the path or remove the entry")
      fi
    done <<< "$map_paths"
  fi
fi

if (( ${#findings[@]} > 0 )); then
  echo "lint-agents-budget: instruction-layer finding(s):" >&2
  for entry in "${findings[@]}"; do
    echo "  $entry" >&2
  done
  exit 1
fi

echo "lint-agents-budget: budgets OK (root=$r, +frontend=$((r + fe)), +java=$((r + jv)), +static=$((r + st)), deepest=$((r + st + co))/32768)"
