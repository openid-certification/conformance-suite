#!/usr/bin/env bash
#
# Re-download the vendored Monaco editor at
# src/main/resources/static/vendor/monaco-editor/.
#
# Source: the official monaco-editor npm package
# (https://www.npmjs.com/package/monaco-editor). We extract only the AMD
# `min/` distribution, then prune to a minimal English-only JSON-language
# subset so the on-disk footprint stays under ~4 MB.
#
# Why vendor at all: the conformance suite has no bundler; CLAUDE.md "Key
# Dependencies" pins the policy that vendored deps live under
# src/main/resources/static/vendor/ and are loaded via plain <script> tags
# at runtime. <cts-json-editor> bootstraps Monaco via vs/loader.js, the
# AMD entry point, exactly the way the upstream "I don't have a bundler"
# guide recommends.
#
# Pin is by version + tarball SHA-256. To bump:
#   1. Update MONACO_VERSION to the new version on npm.
#   2. Run this script -- it will fail at the digest check.
#   3. If the downloaded tarball looks right (spot-check the AMD layout
#      still ships vs/loader.js and vs/language/json/), replace
#      EXPECTED_TARBALL_SHA256 with the new digest shown in the error.
#   4. Re-run; the script should succeed.
#   5. Commit the regenerated vendor/monaco-editor/ tree alongside the
#      script change in one MR.
#
# Do NOT run this in CI. It is a maintenance tool invoked on demand when
# bumping the Monaco vendor dependency.

set -euo pipefail

MONACO_VERSION="0.45.0"
EXPECTED_TARBALL_SHA256="1aa02fd4319545377de0ae10faab957f75ae0060dcc7c09915950613f232c4ab"

URL="https://registry.npmjs.org/monaco-editor/-/monaco-editor-${MONACO_VERSION}.tgz"

REPO_ROOT="$(git rev-parse --show-toplevel)"
TARGET_DIR="${REPO_ROOT}/src/main/resources/static/vendor/monaco-editor"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "${WORK_DIR}"' EXIT

TARBALL="${WORK_DIR}/monaco.tgz"

echo "Fetching ${URL}..."
curl -sSfL "${URL}" -o "${TARBALL}"

ACTUAL_SHA256="$(shasum -a 256 "${TARBALL}" | awk '{print $1}')"
if [ "${ACTUAL_SHA256}" != "${EXPECTED_TARBALL_SHA256}" ]; then
  echo "ERROR: SHA-256 mismatch for monaco-editor-${MONACO_VERSION}.tgz" >&2
  echo "  expected: ${EXPECTED_TARBALL_SHA256}" >&2
  echo "  actual:   ${ACTUAL_SHA256}" >&2
  echo "Either upstream re-published the version (investigate before trusting) or MONACO_VERSION was bumped without updating EXPECTED_TARBALL_SHA256." >&2
  exit 1
fi

EXTRACT_DIR="${WORK_DIR}/extract"
mkdir -p "${EXTRACT_DIR}"

# Pull the AMD distribution and the LICENSE only. --strip-components=2
# turns "package/min/vs/..." into "vs/...".
tar -xzf "${TARBALL}" -C "${EXTRACT_DIR}" --strip-components=2 'package/min'
tar -xzf "${TARBALL}" -C "${EXTRACT_DIR}" --strip-components=1 'package/LICENSE'

# Sanity-check the AMD layout. If any of these are missing, the upstream
# packaging changed and the wrapper component will fail to boot.
for f in vs/loader.js vs/editor/editor.main.js vs/editor/editor.main.css \
         vs/base/worker/workerMain.js vs/language/json/jsonMode.js \
         vs/language/json/jsonWorker.js; do
  if [ ! -f "${EXTRACT_DIR}/${f}" ]; then
    echo "ERROR: expected file ${f} missing from monaco-editor-${MONACO_VERSION} min/ tarball" >&2
    echo "Upstream packaging likely changed. Inspect ${TARBALL} before trusting this version." >&2
    exit 1
  fi
done

# Replace the vendored tree atomically. We keep VERSION.txt and README.md
# from the previous checkout (the README is tracked in git separately so
# we don't rewrite it from the script).
PRESERVED_README=""
if [ -f "${TARGET_DIR}/README.md" ]; then
  PRESERVED_README="$(cat "${TARGET_DIR}/README.md")"
fi

rm -rf "${TARGET_DIR}"
mkdir -p "${TARGET_DIR}"

# Curated minimal subset. Everything else in min/ is for other languages or
# locales we don't ship. Keeping the on-disk footprint small means the
# conformance-suite jar grows by single-digit MB, not 50+.
KEEP=(
  "vs/loader.js"
  "vs/editor/editor.main.js"
  "vs/editor/editor.main.css"
  "vs/editor/editor.main.nls.js"
  "vs/base/worker/workerMain.js"
  "vs/language/json/jsonMode.js"
  "vs/language/json/jsonWorker.js"
)

for rel in "${KEEP[@]}"; do
  src="${EXTRACT_DIR}/${rel}"
  dst="${TARGET_DIR}/${rel}"
  mkdir -p "$(dirname "${dst}")"
  cp "${src}" "${dst}"
done

cp "${EXTRACT_DIR}/LICENSE" "${TARGET_DIR}/LICENSE"

cat > "${TARGET_DIR}/VERSION.txt" <<EOF
monaco-editor ${MONACO_VERSION}
tarball: ${URL}
sha256: ${EXPECTED_TARBALL_SHA256}
vendored on $(date -u +%Y-%m-%dT%H:%M:%SZ) by frontend/scripts/update-vendor-monaco.sh
EOF

if [ -n "${PRESERVED_README}" ]; then
  printf "%s\n" "${PRESERVED_README}" > "${TARGET_DIR}/README.md"
fi

trap - EXIT
rm -rf "${WORK_DIR}"

echo
echo "OK: ${TARGET_DIR} updated to monaco-editor@${MONACO_VERSION}"
echo "    on-disk size: $(du -sh "${TARGET_DIR}" | awk '{print $1}')"
