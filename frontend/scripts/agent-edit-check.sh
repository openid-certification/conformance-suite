#!/usr/bin/env bash
# UI enablement kit — per-file edit check for agent edit hooks.
#
# Given ONE path, runs the fast, deterministic frontend checks that
# `npm run test:ci` would apply to that file — file-scoped where the tools are
# file-scoped (prettier, eslint) and whole-tree for the cheap presence greps
# (lint:icons, lint:jsdoc). Intended to be wired to an edit hook so an agent
# gets immediate, actionable feedback after touching a static/ frontend file.
#
# Usage:  agent-edit-check.sh <file>
#   <file> may be absolute or repo-relative.
#
# Exit codes:  0 = clean / not applicable   1 = findings   2 = usage error
#
# Kill switch:  set CTS_SKIP_EDIT_CHECKS=1 to bypass entirely (prints a notice
#               to stderr and exits 0). Use this exact variable name.
#
# Scope (all handled HERE so the hook wiring can stay broad):
#   - Only files under src/main/resources/static/ are checked; anything else
#     exits 0 silently.
#   - A path that no longer exists (a deletion) exits 0 silently.
#   - If frontend/node_modules is absent (bootstrap not run yet) exits 0
#     silently, so this never blocks a first-time setup.
#
# Tool scoping — WHY prettier/eslint only run under static/components:
#   `npm run format` targets `. ../src/main/resources/static/components` and
#   `npm run lint` targets `../frontend ../src/main/resources/static/components`.
#   Within the static tree that is ONLY static/components. Other static
#   subtrees (js/, css/, *.html, templates/, vendor/) are deliberately outside
#   the project's prettier/eslint scope (see frontend/.prettierignore and the
#   global `ignores` in eslint.config.js) — real committed files there (e.g.
#   js/log-detail.js) do NOT satisfy `prettier --check`, so running those tools
#   on them would emit false positives. We therefore mirror test:ci exactly.
#   The whole-tree greps still run for the file types they scan.

# Not `set -e`: we run several independent checks and aggregate their results;
# an expected non-zero from one check must not abort the rest.
set -uo pipefail

# --- Kill switch (before anything else, including arg validation) ------------
if [ "${CTS_SKIP_EDIT_CHECKS:-}" = "1" ]; then
  echo "agent-edit-check: skipped (CTS_SKIP_EDIT_CHECKS=1)" >&2
  exit 0
fi

# --- Usage -------------------------------------------------------------------
if [ "$#" -ne 1 ]; then
  echo "usage: agent-edit-check.sh <file>" >&2
  echo "       (exactly one path argument; absolute or repo-relative)" >&2
  exit 2
fi

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." &>/dev/null && pwd)
FRONTEND_DIR="$REPO_ROOT/frontend"

arg=$1

# Resolve the argument to an absolute path without requiring the file to exist
# (deletions must resolve too). Prefer an absolute arg, then a CWD-relative
# match, then a repo-relative interpretation.
if [ "${arg#/}" != "$arg" ]; then
  abs=$arg
elif [ -e "$PWD/$arg" ]; then
  abs="$PWD/$arg"
else
  abs="$REPO_ROOT/$arg"
fi

# Canonicalize via the parent directory so `..` / symlinks are resolved even
# when the leaf file does not exist. Quoting throughout keeps spaces and shell
# metacharacters in filenames safe; nothing is ever eval'd.
parent=$(cd -- "$(dirname -- "$abs")" 2>/dev/null && pwd)
if [ -n "$parent" ]; then
  canonical="$parent/$(basename -- "$abs")"
else
  canonical=$abs
fi

# Repo-relative form. If the prefix does not strip, the file is outside the
# repository → out of scope.
rel=${canonical#"$REPO_ROOT"/}
if [ "$rel" = "$canonical" ]; then
  exit 0
fi

# Scope gate: static/ only.
case "$rel" in
  src/main/resources/static/*) : ;;
  *) exit 0 ;;
esac

# Deletion (or otherwise absent): nothing to check.
[ -e "$canonical" ] || exit 0

# Bootstrap-before-hooks ordering: no toolchain yet → do nothing.
[ -d "$FRONTEND_DIR/node_modules" ] || exit 0

# --- Classify the file -------------------------------------------------------
case "$rel" in
  src/main/resources/static/components/*) in_components=1 ;;
  *) in_components=0 ;;
esac

base=$(basename -- "$canonical")
case "$base" in
  *.*) ext=$(printf '%s' "${base##*.}" | tr 'A-Z' 'a-z') ;;
  *)   ext="" ;;
esac

# Extension coverage matrices.
prettier_supported=0
case " js mjs cjs jsx ts tsx json jsonc json5 css scss less html htm vue md markdown mdx yaml yml " in
  *" $ext "*) prettier_supported=1 ;;
esac
eslint_supported=0
case " js mjs cjs " in
  *" $ext "*) eslint_supported=1 ;;
esac

# Path as addressed from the frontend/ CWD (mirrors the npm scripts and the
# `../src/...` patterns in .prettierignore / eslint.config.js).
frontend_rel="../$rel"

TMPD=$(mktemp -d "${TMPDIR:-/tmp}/agent-edit-check.XXXXXX")
trap 'rm -rf "$TMPD"' EXIT

findings=()   # human-readable finding blocks (multi-line strings)
ran=()        # names of checks that actually executed
skipped_note=""

# --- prettier (file-scoped) --------------------------------------------------
if [ "$in_components" -eq 1 ] && [ "$prettier_supported" -eq 1 ]; then
  if [ ! -x "$FRONTEND_DIR/node_modules/.bin/prettier" ]; then
    echo "agent-edit-check: prettier binary missing from node_modules — skipping (run: cd frontend && npm ci)" >&2
  else
    ran+=("prettier")
    if ( cd "$FRONTEND_DIR" && ./node_modules/.bin/prettier \
          --config ./.prettierrc.json --check -- "$frontend_rel" ) >"$TMPD/prettier" 2>&1; then
      :
    else
      findings+=("[prettier] $rel is not formatted to the project style:
$(sed 's/^/    /' "$TMPD/prettier")
    fix: (cd frontend && ./node_modules/.bin/prettier --config ./.prettierrc.json --write \"$frontend_rel\")")
    fi
  fi
fi

# --- eslint (file-scoped) ----------------------------------------------------
if [ "$in_components" -eq 1 ] && [ "$eslint_supported" -eq 1 ]; then
  if [ ! -x "$FRONTEND_DIR/node_modules/.bin/eslint" ]; then
    echo "agent-edit-check: eslint binary missing from node_modules — skipping (run: cd frontend && npm ci)" >&2
  else
    ran+=("eslint")
    ( cd "$FRONTEND_DIR" && ./node_modules/.bin/eslint \
        --no-warn-ignored -- "$frontend_rel" ) >"$TMPD/eslint" 2>&1
    erc=$?
    if [ "$erc" -eq 1 ]; then
      findings+=("[eslint] $rel has lint errors:
$(sed 's/^/    /' "$TMPD/eslint")
    fix: (cd frontend && ./node_modules/.bin/eslint --fix -- \"$frontend_rel\") then resolve what remains")
    elif [ "$erc" -eq 2 ]; then
      echo "agent-edit-check: eslint internal error (exit 2) on $rel — skipping eslint (not a code finding)" >&2
      sed 's/^/    /' "$TMPD/eslint" >&2
    fi
  fi
fi

# --- lint:icons (whole-tree presence grep; only for icon-bearing file types) -
case " html htm js mjs cjs " in
  *" $ext "*)
    ran+=("lint:icons")
    ( cd "$FRONTEND_DIR" && bash scripts/lint-icon-names.sh ) >"$TMPD/icons" 2>&1
    crc=$?
    if [ "$crc" -eq 1 ]; then
      findings+=("[lint:icons] invalid cts-icon name(s) in the static tree (whole-tree check):
$(sed 's/^/    /' "$TMPD/icons")")
    elif [ "$crc" -eq 2 ]; then
      echo "agent-edit-check: lint:icons internal error (exit 2) — skipping (not a code finding)" >&2
    fi
    ;;
esac

# --- lint:jsdoc (whole-tree presence grep over components) -------------------
if [ "$in_components" -eq 1 ] && [ "$eslint_supported" -eq 1 ]; then
  ran+=("lint:jsdoc")
  ( cd "$FRONTEND_DIR" && bash scripts/lint-jsdoc-properties.sh ) >"$TMPD/jsdoc" 2>&1
  jrc=$?
  if [ "$jrc" -eq 1 ]; then
    findings+=("[lint:jsdoc] a cts-*.js component is missing @property JSDoc (whole-tree check):
$(sed 's/^/    /' "$TMPD/jsdoc")")
  elif [ "$jrc" -eq 2 ]; then
    echo "agent-edit-check: lint:jsdoc internal error (exit 2) — skipping (not a code finding)" >&2
  fi
fi

# --- Report ------------------------------------------------------------------
if [ "$in_components" -eq 0 ]; then
  skipped_note=" (prettier/eslint skipped: outside static/components, which the project does not format/lint)"
fi

if [ "${#findings[@]}" -gt 0 ]; then
  echo "agent-edit-check: findings for $rel" >&2
  echo >&2
  for f in "${findings[@]}"; do
    printf '%s\n\n' "$f" >&2
  done
  exit 1
fi

if [ "${#ran[@]}" -eq 0 ]; then
  echo "agent-edit-check: OK — no applicable checks for $rel$skipped_note"
else
  # Join ran[] with ", " (bash-3.2 friendly).
  joined=""
  for c in "${ran[@]}"; do
    if [ -z "$joined" ]; then joined=$c; else joined="$joined, $c"; fi
  done
  echo "agent-edit-check: OK ($joined) — $rel$skipped_note"
fi
exit 0
