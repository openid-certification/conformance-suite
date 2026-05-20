#!/usr/bin/env bash
# Validate that every literal `cts-icon name="<name>"` in the static frontend
# resolves to a real SVG under src/main/resources/static/vendor/coolicons/icons/.
#
# Why this exists:
#   coolicons ships 442 per-icon SVG files. An invalid name (e.g. "x" when the
#   actual files are "close-md", "close-sm", "close-lg") produces a silent
#   404 on the <use href> fetch and renders an empty box. The runtime warning
#   in cts-icon.js catches dynamic/templated names at dev time; this CI gate
#   catches literal typos at PR time.
#
# What's checked:
#   1. Inline HTML attributes: `<cts-icon ... name="<literal>" ...>` in HTML,
#      Lit `html`` `` templates, and lodash `<%- %>` templates.
#   2. Imperative DOM construction: `setAttribute("name", "<literal>")` in
#      files that mention `cts-icon` (proxy for "this file deals with the
#      icon component").
#
# What's skipped:
#   - Dynamic / templated values containing ${...}, <%- ... %>, or {{ ... }} —
#     these can't be validated statically; the runtime warning is the fallback.
#   - The vendor directory itself.
#   - cts-icon.js (the component's own JSDoc/render carry illustrative names).
#
# Hints:
#   When a literal is exactly `x`, `cross`, or `close`, the error message
#   suggests close-md / close-sm / close-lg (the canonical close affordances).
#
# Exits 0 on success, 1 on any invalid name, 2 on infrastructure error.

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." &>/dev/null && pwd)
ICONS_DIR="$REPO_ROOT/src/main/resources/static/vendor/coolicons/icons"
STATIC_DIR="$REPO_ROOT/src/main/resources/static"

if [[ ! -d "$ICONS_DIR" ]]; then
  echo "lint-icon-names: vendored icons directory not found: $ICONS_DIR" >&2
  exit 2
fi
if [[ ! -d "$STATIC_DIR" ]]; then
  echo "lint-icon-names: static frontend directory not found: $STATIC_DIR" >&2
  exit 2
fi

bad_lines=()

is_dynamic() {
  case "$1" in
    *'$'*|*'<%'*|*'{{'*|'') return 0 ;;
    *) return 1 ;;
  esac
}

# Real coolicon filenames are strict kebab-case-lowercase-with-digits — any
# value containing characters outside [a-z0-9-] is something else (a comment
# placeholder like `...`, an XSS test fixture like `<script>...</script>`, a
# template expression we missed), not an icon name. Skip silently rather than
# false-positive.
is_iconish() {
  case "$1" in
    *[!a-z0-9-]*|'') return 1 ;;
    *) return 0 ;;
  esac
}

hint_for() {
  case "$1" in
    x|cross|close) echo "    (did you mean close-md, close-sm, or close-lg?)" ;;
  esac
}

check_name() {
  local name="$1" file="$2" line="$3"
  if is_dynamic "$name"; then
    return
  fi
  if ! is_iconish "$name"; then
    return
  fi
  if [[ -f "$ICONS_DIR/$name.svg" ]]; then
    return
  fi
  # Strip the repo-root prefix for a portable relative path in the error.
  local relpath="${file#$REPO_ROOT/}"
  bad_lines+=("$relpath:$line: cts-icon name=\"$name\" — no such file at src/main/resources/static/vendor/coolicons/icons/$name.svg")
  local hint
  hint=$(hint_for "$name")
  if [[ -n "$hint" ]]; then
    bad_lines+=("$hint")
  fi
}

# 1. Inline HTML attribute pattern. We match the FULL element-opening token so
#    the literal name="..." is anchored to a cts-icon element (avoids matching
#    e.g. `<input name="foo">` next to an unrelated `cts-icon` mention).
#    The grep produces lines shaped: <abs-path>:<line>:<source>
html_hits=$(grep -rnE \
  --include='*.html' --include='*.js' \
  --exclude-dir='vendor' \
  --exclude='cts-icon.js' \
  'cts-icon[^<>]*name="[^"]*"' "$STATIC_DIR" 2>/dev/null || true)

if [[ -n "$html_hits" ]]; then
  while IFS= read -r raw; do
    file="${raw%%:*}"
    rest="${raw#*:}"
    line="${rest%%:*}"
    source_line="${rest#*:}"
    # Extract every `name="..."` value on the source line that is preceded by
    # the cts-icon element opening (the regex above already anchored the line).
    # We may have multiple cts-icon elements per line — iterate them all.
    while IFS= read -r match; do
      name=${match#name=\"}
      name=${name%\"}
      check_name "$name" "$file" "$line"
    done < <(echo "$source_line" | grep -oE 'name="[^"]*"' || true)
  done <<< "$html_hits"
fi

# 2. Imperative setAttribute("name", "literal") pattern. Restrict to files that
#    mention cts-icon (proxy for "this file constructs cts-icon elements")
#    so we don't trip on form-input setAttribute calls in unrelated files.
icon_files=$(grep -rlE 'cts-icon' \
  --include='*.html' --include='*.js' \
  --exclude-dir='vendor' \
  --exclude='cts-icon.js' \
  "$STATIC_DIR" 2>/dev/null || true)

if [[ -n "$icon_files" ]]; then
  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    # File-scope proxy: any file mentioning `cts-icon` is assumed to be a
    # call site. Pass-1's line-scope anchor (`cts-icon[^<>]*name=`) cannot
    # catch imperative DOM construction because `createElement("cts-icon")`
    # and `setAttribute("name", "...")` typically live on different lines.
    # The file-scope proxy is safe today because the four real setAttribute
    # call sites (logs.html, cts-modal.js, cts-alert.js, cts-icon.stories.js)
    # are all cts-icon contexts; no file uses setAttribute("name", "...") on
    # an unrelated element while ALSO building cts-icons. The is_iconish()
    # guard further filters most false positives (form-input "name" values
    # often contain characters outside [a-z0-9-]).
    # Match either quote style — the project does not pin a quote style and
    # a future call site using single quotes would otherwise slip the gate.
    setattr_hits=$(grep -nE 'setAttribute\(("name"|'"'"'name'"'"'), *("[^"]+"|'"'"'[^'"'"']+'"'"')\)' "$file" 2>/dev/null || true)
    if [[ -n "$setattr_hits" ]]; then
      while IFS= read -r raw; do
        line="${raw%%:*}"
        source_line="${raw#*:}"
        # Extract the literal name value, tolerating both quote styles.
        name=$(echo "$source_line" | sed -nE 's/.*setAttribute\(("name"|'"'"'name'"'"'), *"([^"]+)".*/\2/p; s/.*setAttribute\(("name"|'"'"'name'"'"'), *'"'"'([^'"'"']+)'"'"'.*/\2/p' | head -n1)
        check_name "$name" "$file" "$line"
      done <<< "$setattr_hits"
    fi
  done <<< "$icon_files"
fi

if (( ${#bad_lines[@]} > 0 )); then
  echo "lint-icon-names: invalid cts-icon name(s) detected:" >&2
  for entry in "${bad_lines[@]}"; do
    echo "  $entry" >&2
  done
  echo "" >&2
  echo "Find vendored icon names: ls src/main/resources/static/vendor/coolicons/icons/" >&2
  echo "Or browse Storybook → Primitives/cts-icon → AllIcons." >&2
  exit 1
fi

count=$(ls "$ICONS_DIR"/*.svg 2>/dev/null | wc -l | tr -d ' ')
echo "lint-icon-names: all literal cts-icon names resolve to vendored files ($count icons available)"
