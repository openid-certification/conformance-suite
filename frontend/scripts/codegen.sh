#!/usr/bin/env bash
#
# frontend/scripts/codegen.sh — Java↔JS API type-parity codegen.
#
# Refreshes the committed cache at frontend/src/api/:
#   openapi.json       (springdoc /v3/api-docs snapshot)
#   samples/<key>.json (captured wire-payload samples for each fixture)
#   api-types.d.ts     (merged path-level + sample-derived TS types)
#
# Two modes:
#
#   ./scripts/codegen.sh
#       Online mode. Requires a dev Spring server reachable at $CTS_BASE_URL
#       (default http://localhost:8080) with profile `dev` active so
#       DummyUserFilter satisfies auth. Steps:
#         1. curl /v3/api-docs               → src/api/openapi.json
#         2. for each fixture under src/api/fixtures/<key>.json:
#            replay it against the dev server → src/api/samples/<key>.json
#         3. openapi-typescript openapi.json → /tmp/paths.d.ts
#         4. quicktype each sample           → /tmp/<key>.d.ts
#         5. merge into                      → src/api/api-types.d.ts
#
#   ./scripts/codegen.sh --offline
#       Offline mode. Skips steps 1-2 (no server contact). Re-runs steps 3-5
#       from the already-committed openapi.json and samples/. Used by CI
#       (frontend_lint) to detect cache-internal incoherence — i.e. someone
#       hand-edited api-types.d.ts or partially regenerated. Does NOT detect
#       Java drift (that surfaces the next time anyone runs online codegen).
#
# Why this script exists frontend-side and not as a Maven build step: the user
# explicitly chose minimal backend touching. See
# docs/plans/2026-04-27-003-feat-frontend-dev-loop-and-type-parity-plan.md.
#
# Pattern reference: frontend/scripts/update-vendor-{lit,monaco}.sh — this
# follows the same "vendored-artifact refresh" idiom.

set -euo pipefail

OFFLINE=0
for arg in "$@"; do
	case "$arg" in
		--offline) OFFLINE=1 ;;
		-h|--help)
			sed -n '3,33p' "$0"
			exit 0
			;;
		*)
			echo "Unknown argument: $arg" >&2
			echo "Usage: $0 [--offline]" >&2
			exit 2
			;;
	esac
done

REPO_ROOT="$(git rev-parse --show-toplevel)"
API_DIR="${REPO_ROOT}/frontend/src/api"
FIXTURES_DIR="${API_DIR}/fixtures"
SAMPLES_DIR="${API_DIR}/samples"
OPENAPI_FILE="${API_DIR}/openapi.json"
TYPES_FILE="${API_DIR}/api-types.d.ts"
BASE_URL="${CTS_BASE_URL:-http://localhost:8080}"

mkdir -p "${API_DIR}" "${FIXTURES_DIR}" "${SAMPLES_DIR}"

cd "${REPO_ROOT}/frontend"

# Use the local devDependency installs; npx will resolve them from node_modules.
NPX="npx --no-install"

# ----- Online steps (1, 2) ---------------------------------------------------
if [ "${OFFLINE}" -eq 0 ]; then
	echo "→ Online codegen against ${BASE_URL}"

	echo "  [1/5] Fetching ${BASE_URL}/v3/api-docs"
	if ! curl -fsS "${BASE_URL}/v3/api-docs" -o "${OPENAPI_FILE}.tmp"; then
		echo "ERROR: could not reach ${BASE_URL}/v3/api-docs." >&2
		echo "  Start the dev Spring server first:" >&2
		echo "    mvn spring-boot:run -Dspring-boot.run.profiles=dev" >&2
		echo "  See CLAUDE.md 'Dev loop'." >&2
		rm -f "${OPENAPI_FILE}.tmp"
		exit 1
	fi
	# Pretty-print so diffs stay reviewable.
	$NPX prettier --parser json "${OPENAPI_FILE}.tmp" >"${OPENAPI_FILE}"
	rm -f "${OPENAPI_FILE}.tmp"

	echo "  [2/5] Replaying fixtures → samples"
	if [ -d "${FIXTURES_DIR}" ] && [ "$(ls -A "${FIXTURES_DIR}" 2>/dev/null)" ]; then
		for fixture in "${FIXTURES_DIR}"/*.json; do
			[ -e "${fixture}" ] || continue
			key="$(basename "${fixture}" .json)"
			echo "    - ${key}"
			method="$(node -e "console.log(JSON.parse(require('fs').readFileSync('${fixture}','utf8')).method || 'GET')")"
			path="$(node -e "console.log(JSON.parse(require('fs').readFileSync('${fixture}','utf8')).path)")"
			body_file="$(mktemp)"
			has_body="$(node -e "const f=JSON.parse(require('fs').readFileSync('${fixture}','utf8')); if(f.body!==undefined){require('fs').writeFileSync('${body_file}', JSON.stringify(f.body)); console.log('1')} else {console.log('0')}")"
			out_tmp="$(mktemp)"
			if [ "${has_body}" = "1" ]; then
				curl -fsS -X "${method}" -H 'Content-Type: application/json' --data-binary "@${body_file}" "${BASE_URL}${path}" -o "${out_tmp}" || {
					echo "ERROR: ${method} ${path} failed" >&2
					rm -f "${body_file}" "${out_tmp}"
					exit 1
				}
			else
				curl -fsS -X "${method}" "${BASE_URL}${path}" -o "${out_tmp}" || {
					echo "ERROR: ${method} ${path} failed" >&2
					rm -f "${body_file}" "${out_tmp}"
					exit 1
				}
			fi
			$NPX prettier --parser json "${out_tmp}" >"${SAMPLES_DIR}/${key}.json"
			rm -f "${body_file}" "${out_tmp}"
		done
	else
		echo "    (no fixtures yet; skipping sample capture)"
	fi
else
	echo "→ Offline codegen (skipping steps 1-2)"
	if [ ! -f "${OPENAPI_FILE}" ]; then
		echo "  No committed openapi.json yet — nothing to merge."
		echo "  Run \`npm run codegen\` once with the dev server up to seed the cache."
		exit 0
	fi
fi

# ----- Merge steps (3, 4, 5) -------------------------------------------------

TMPDIR="$(mktemp -d)"
trap 'rm -rf "${TMPDIR}"' EXIT

echo "  [3/5] openapi-typescript → paths interface"
$NPX openapi-typescript "${OPENAPI_FILE}" --output "${TMPDIR}/paths.d.ts"

echo "  [4/5] quicktype each sample → field-level types"
sample_keys=()
if [ -d "${SAMPLES_DIR}" ]; then
	for sample in "${SAMPLES_DIR}"/*.json; do
		[ -e "${sample}" ] || continue
		key="$(basename "${sample}" .json)"
		# quicktype top-level type name: kebab → PascalCase
		type_name="$(node -e "const k='${key}'; console.log(k.split(/[-_]/).map(s=>s.charAt(0).toUpperCase()+s.slice(1)).join(''))")"
		$NPX quicktype --src-lang json --lang ts --just-types --top-level "${type_name}Sample" "${sample}" --out "${TMPDIR}/${key}.d.ts"
		sample_keys+=("${key}")
	done
fi

echo "  [5/5] Merging → ${TYPES_FILE#"${REPO_ROOT}/"}"
{
	cat <<'HEADER'
/**
 * AUTO-GENERATED — DO NOT EDIT BY HAND.
 *
 * Regenerate with:
 *   cd frontend && npm run codegen        (online; needs dev Spring server)
 *   cd frontend && npm run codegen:offline (re-merge from committed cache)
 *
 * Source: frontend/src/api/openapi.json (springdoc snapshot)
 *         frontend/src/api/samples/*.json (captured wire payloads)
 *
 * See frontend/src/api/README.md for the full workflow and read-only invariant.
 */
/* eslint-disable */

HEADER
	cat "${TMPDIR}/paths.d.ts"
	for key in "${sample_keys[@]}"; do
		echo
		echo "// ─── sample-derived: ${key} ────────────────────────────────────────"
		cat "${TMPDIR}/${key}.d.ts"
	done
} >"${TYPES_FILE}.tmp"

$NPX prettier --parser typescript "${TYPES_FILE}.tmp" >"${TYPES_FILE}"
rm -f "${TYPES_FILE}.tmp"

echo "✓ Done. Review \`git diff frontend/src/api/\` and commit."
