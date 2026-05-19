#!/usr/bin/env bash
# Dump configuration fields for in-scope FAPI2 server/issuer test plans from a running
# conformance suite instance. Used as a sanity check that the snapshot unit test's
# offline VariantService walk matches what the live TestPlanApi controller serializes.
#
# Usage:
#   scripts/dump-config-fields.sh [output-dir]
#
# Default output dir: /tmp/config-fields-<branch>-<timestamp>/
#
# Prereqs:
#   - jq installed
#   - conformance suite running and reachable at $BASE_URL (default
#     https://localhost.emobix.co.uk:8443)

set -euo pipefail

BASE_URL="${BASE_URL:-https://localhost.emobix.co.uk:8443}"

PLANS=(
  "fapi2-security-profile-final-test-plan"
  "fapi2-security-profile-final-brazil-dcr-test-plan"
  "fapi2-message-signing-final-test-plan"
  "oid4vci-1_0-issuer-test-plan"
  "oid4vci-1_0-issuer-haip-test-plan"
)

BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null | tr '/' '-' || echo unknown)"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="${1:-/tmp/config-fields-${BRANCH}-${TIMESTAMP}}"

mkdir -p "$OUT_DIR"

for plan in "${PLANS[@]}"; do
  echo "Dumping $plan ..."
  curl -sk "$BASE_URL/api/plan/info/$plan" \
    | jq --sort-keys '
        # Recursively sort arrays of strings; sort arrays of objects by "testModule".
        def sort_recursive:
          if type == "array" then
            map(sort_recursive)
            | if length > 0 and (.[0] | type) == "string" then sort
              elif length > 0 and (.[0] | type) == "object" and (.[0] | has("testModule")) then sort_by(.testModule)
              else . end
          elif type == "object" then
            with_entries(.value |= sort_recursive)
          else . end;
        sort_recursive' \
    > "$OUT_DIR/$plan.json"
done

echo
echo "Wrote $(ls "$OUT_DIR" | wc -l | tr -d ' ') plan dumps to $OUT_DIR"
