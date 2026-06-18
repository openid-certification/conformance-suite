#!/usr/bin/env bash
# Presence check for the AGENTS.md §5 JSDoc @property rule.
#
# Every `cts-*.js` component under src/main/resources/static/components/
# must carry at least one `@property` tag in its class-level JSDoc block.
# This check does NOT verify per-property correctness, argument types, or
# completeness — those remain a reviewer concern. It catches the regression
# of landing a new component with zero @property coverage.
#
# Excluded:
#   *.stories.js — test-only files, not components.
#   _button-classes.js — shared helper module, not a custom element.
#
# Exits non-zero and lists any component file missing the tag.

set -euo pipefail

# Resolve the project root relative to this script so the check works from
# any CWD (CI runners, IDE tasks, pre-commit).
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." &>/dev/null && pwd)
COMPONENTS_DIR="$REPO_ROOT/src/main/resources/static/components"

if [[ ! -d "$COMPONENTS_DIR" ]]; then
  echo "lint-jsdoc-properties: components directory not found: $COMPONENTS_DIR" >&2
  exit 2
fi

missing=()
for file in "$COMPONENTS_DIR"/cts-*.js; do
  # Skip *.stories.js (test-only) and _button-classes.js (shared helper).
  case "$(basename "$file")" in
    *.stories.js|_*) continue ;;
  esac
  if ! grep -q "@property" "$file"; then
    missing+=("$file")
  fi
done

if (( ${#missing[@]} > 0 )); then
  echo "lint-jsdoc-properties: missing @property JSDoc in:" >&2
  for f in "${missing[@]}"; do
    echo "  $f" >&2
  done
  echo "" >&2
  echo "Add a class-level JSDoc block with @property tags for each" >&2
  echo "public reactive property / attribute. See cts-button.js for the" >&2
  echo "canonical shape, or src/main/resources/static/components/AGENTS.md §5." >&2
  exit 1
fi

echo "lint-jsdoc-properties: all cts-*.js files carry @property tags ($(ls "$COMPONENTS_DIR"/cts-*.js | grep -Ev '\.stories\.js$' | wc -l | tr -d ' ') files checked)"
