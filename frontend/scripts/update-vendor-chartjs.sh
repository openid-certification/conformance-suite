#!/usr/bin/env bash
#
# Re-download the vendored Chart.js UMD bundle at
# src/main/resources/static/vendor/chart.js/.
#
# Source: the official chart.js npm package
# (https://www.npmjs.com/package/chart.js). We extract only the UMD build,
# `dist/chart.umd.js` -- a single minified, self-contained file (the
# @kurkle/color helper library Chart.js depends on is bundled into it, not
# imported separately) -- plus LICENSE.md. The ESM `dist/chart.js` build is
# NOT vendored: it is a multi-file, unminified tree whose entry point
# imports `./chunks/helpers.dataset.js`, which in turn imports the bare
# specifier `@kurkle/color` -- unresolvable without a bundler or import map.
#
# Why vendor at all: the conformance suite has no bundler; CLAUDE.md "Key
# Dependencies" pins the policy that vendored deps live under
# src/main/resources/static/vendor/ and are loaded via plain <script> tags
# at runtime. `<cts-chart>` lazily injects
# <script src="/vendor/chart.js/chart.umd.js"> and reads `window.Chart`.
#
# Pin is by version + tarball SHA-256. To bump:
#   1. Update CHARTJS_VERSION to the new version on npm.
#   2. Run this script -- it will fail at the digest check.
#   3. If the downloaded tarball looks right (spot-check dist/chart.umd.js
#      still exists and starts with a `/*!` banner), replace
#      EXPECTED_TARBALL_SHA256 with the new digest shown in the error.
#   4. Re-run; the script should succeed.
#   5. Commit the regenerated vendor/chart.js/ files alongside the script
#      change in one MR.
#
# Do NOT run this in CI. It is a maintenance tool invoked on demand when
# bumping the Chart.js vendor dependency.

set -euo pipefail

CHARTJS_VERSION="4.5.1"
EXPECTED_TARBALL_SHA256="f540d98468457ac7a0aabb32006dfb066297e096c5ea063a5d80aa973d1c337a"

URL="https://registry.npmjs.org/chart.js/-/chart.js-${CHARTJS_VERSION}.tgz"

REPO_ROOT="$(git rev-parse --show-toplevel)"
TARGET_DIR="${REPO_ROOT}/src/main/resources/static/vendor/chart.js"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "${WORK_DIR}"' EXIT

TARBALL="${WORK_DIR}/chartjs.tgz"

echo "Fetching ${URL}..."
curl -sSfL "${URL}" -o "${TARBALL}"

ACTUAL_SHA256="$(shasum -a 256 "${TARBALL}" | awk '{print $1}')"
if [ "${ACTUAL_SHA256}" != "${EXPECTED_TARBALL_SHA256}" ]; then
  echo "ERROR: SHA-256 mismatch for chart.js-${CHARTJS_VERSION}.tgz" >&2
  echo "  expected: ${EXPECTED_TARBALL_SHA256}" >&2
  echo "  actual:   ${ACTUAL_SHA256}" >&2
  echo "Either upstream re-published the version (investigate before trusting) or CHARTJS_VERSION was bumped without updating EXPECTED_TARBALL_SHA256." >&2
  exit 1
fi

EXTRACT_DIR="${WORK_DIR}/extract"
mkdir -p "${EXTRACT_DIR}"

# Pull only the UMD bundle and the LICENSE.
tar -xzf "${TARBALL}" -C "${EXTRACT_DIR}" 'package/dist/chart.umd.js' 'package/LICENSE.md'

UMD_FILE="${EXTRACT_DIR}/package/dist/chart.umd.js"

# Sanity-check the extracted file: it should be the expected minified UMD
# banner comment, not something upstream packaging changed underneath us.
FIRST_LINE="$(head -n 1 "${UMD_FILE}")"
if [ "${FIRST_LINE}" != "/*!" ]; then
  echo "ERROR: ${UMD_FILE} does not start with a '/*!' banner comment (got: ${FIRST_LINE})" >&2
  echo "Upstream packaging likely changed. Inspect ${TARBALL} before trusting this version." >&2
  exit 1
fi

if ! grep -q "Chart.js v${CHARTJS_VERSION}" "${UMD_FILE}"; then
  echo "ERROR: ${UMD_FILE} does not contain the expected 'Chart.js v${CHARTJS_VERSION}' banner text" >&2
  echo "Upstream packaging likely changed. Inspect ${TARBALL} before trusting this version." >&2
  exit 1
fi

# Replace the vendored files atomically. We keep README.md from the
# previous checkout (it is tracked in git separately so we don't rewrite it
# from the script).
PRESERVED_README=""
if [ -f "${TARGET_DIR}/README.md" ]; then
  PRESERVED_README="$(cat "${TARGET_DIR}/README.md")"
fi

rm -rf "${TARGET_DIR}"
mkdir -p "${TARGET_DIR}"

cp "${UMD_FILE}" "${TARGET_DIR}/chart.umd.js"
cp "${EXTRACT_DIR}/package/LICENSE.md" "${TARGET_DIR}/LICENSE.md"

if [ -n "${PRESERVED_README}" ]; then
  printf "%s\n" "${PRESERVED_README}" > "${TARGET_DIR}/README.md"
fi

trap - EXIT
rm -rf "${WORK_DIR}"

echo
echo "OK: ${TARGET_DIR} updated to chart.js@${CHARTJS_VERSION}"
echo "    chart.umd.js size: $(wc -c <"${TARGET_DIR}/chart.umd.js") bytes"
