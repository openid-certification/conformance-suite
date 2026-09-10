#!/bin/sh
# Decides whether the `chromatic` job needs to publish a Storybook build.
#
#   exit 0 - run Chromatic
#   exit 1 - every changed file is on the exclusion list, skip it
#
# This lives here rather than in the job's `rules:changes` because GitLab's
# `changes:` keyword has no exclusion syntax. Matching is
#
#   File.fnmatch?(glob, path, File::FNM_PATHNAME | File::FNM_DOTMATCH | File::FNM_EXTGLOB)
#
# and Ruby's FNM_EXTGLOB enables only `{a,b}` brace alternation, not `!(...)`
# negation. Expressing "everything under static/ except this one file" as an
# include list therefore needs character-class patterns that are unreadable and
# stop matching silently when a similarly-named file is added.
#
# Fails open: any doubt about the diff base and Chromatic runs. On master the
# base resolves to HEAD itself, so the job always runs there and keeps the
# auto-accepted baseline (chromatic.config.json: autoAcceptChanges) current.
set -eu

exclude_file="$(dirname "$0")/chromatic-exclude-paths.txt"
head_sha="${CI_COMMIT_SHA:-HEAD}"

base=""
if [ -n "${CI_MERGE_REQUEST_DIFF_BASE_SHA:-}" ]; then
  base="$CI_MERGE_REQUEST_DIFF_BASE_SHA"
elif git fetch -q origin master 2>/dev/null; then
  base="$(git merge-base HEAD FETCH_HEAD 2>/dev/null || true)"
fi

if [ -z "$base" ] || [ "$base" = "$(git rev-parse "$head_sha")" ]; then
  echo "chromatic-should-run: no usable diff base, running Chromatic."
  exit 0
fi

changed="$(git diff --name-only "$base" "$head_sha")"
if [ -z "$changed" ]; then
  echo "chromatic-should-run: nothing changed against $base, running Chromatic."
  exit 0
fi

# Drop comments and blank lines; what is left is a newline-separated set of
# literal paths, which `grep -F` treats as one pattern per line.
excludes="$(grep -v '^[[:space:]]*\(#.*\)\?$' "$exclude_file" || true)"

remaining="$changed"
if [ -n "$excludes" ]; then
  remaining="$(printf '%s\n' "$changed" | grep -vxF "$excludes" || true)"
fi

if [ -n "$remaining" ]; then
  echo "chromatic-should-run: $(printf '%s\n' "$remaining" | wc -l | tr -d ' ') non-excluded file(s) changed, running Chromatic."
  exit 0
fi

echo "chromatic-should-run: only excluded (non-visual) files changed:"
printf '%s\n' "$changed" | sed 's/^/  /'
exit 1
