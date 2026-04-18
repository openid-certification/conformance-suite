#!/usr/bin/env bash
#
# Re-download the vendored Lit bundle at src/main/resources/static/vendor/lit/lit.js.
#
# Source: https://github.com/lit/dist -- the official "lit-all" UMD/ESM bundle
# published alongside each Lit release. `lit-all.min.js` exports every directive
# as a named export, so the importmap in src/main/resources/static/*.html can
# alias every `lit/directives/*.js` path to this single file.
#
# Why this script exists only for Lit: Lit is the only vendored dependency
# distributed from a `dist` GitHub repo (`lit/dist`) rather than a conventional
# npm-tarball release page. The CDN-fetch + SHA-256-pin pattern here is not
# meant to generalize. Other vendored deps under
# src/main/resources/static/vendor/ (bootstrap, jquery, lodash, …) are bumped
# by downloading from their release pages; they have no equivalent script.
#
# Pin is by git tag (immutable). To bump:
#   1. Update LIT_DIST_TAG to the new tag.
#   2. Run this script -- it will fail at the digest check.
#   3. If the downloaded file looks right (spot-check size and named exports),
#      replace EXPECTED_SHA256 with the new digest shown in the error message.
#   4. Re-run; the script should succeed.
#   5. Commit the new vendor/lit/lit.js alongside the script change in one MR.
#
# Do NOT run this in CI. It is a maintenance tool invoked on demand when bumping
# the Lit vendor dependency.

set -euo pipefail

LIT_DIST_TAG="v3.3.1"
EXPECTED_SHA256="e44caa21b3f434eccbb580c00dba2d895aa81c65fc0bf84524ca4326fffaf3ae"
URL="https://cdn.jsdelivr.net/gh/lit/dist@${LIT_DIST_TAG}/all/lit-all.min.js"

REPO_ROOT="$(git rev-parse --show-toplevel)"
TARGET="${REPO_ROOT}/src/main/resources/static/vendor/lit/lit.js"

TMP="$(mktemp)"
trap 'rm -f "${TMP}"' EXIT

echo "Fetching ${URL}..."
curl -sSfL "${URL}" -o "${TMP}"

ACTUAL_SHA256="$(shasum -a 256 "${TMP}" | awk '{print $1}')"
if [ "${ACTUAL_SHA256}" != "${EXPECTED_SHA256}" ]; then
  echo "ERROR: SHA-256 mismatch for lit-all.min.js@${LIT_DIST_TAG}" >&2
  echo "  expected: ${EXPECTED_SHA256}" >&2
  echo "  actual:   ${ACTUAL_SHA256}" >&2
  echo "Either upstream changed (investigate before trusting) or the pinned tag was bumped without updating EXPECTED_SHA256." >&2
  exit 1
fi

mv -f "${TMP}" "${TARGET}"
trap - EXIT

echo "OK: ${TARGET} updated to lit-all.min.js@${LIT_DIST_TAG} ($(wc -c <"${TARGET}") bytes)"
