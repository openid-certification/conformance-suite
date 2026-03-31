#!/bin/bash
#
# Run integration tests locally.
#
# Usage:
#   ./scripts/run-integration-tests.sh [run-tests.sh option] [--rerun ID] [--stdout]
#
# Examples:
#   ./scripts/run-integration-tests.sh --federation-tests
#   ./scripts/run-integration-tests.sh --federation-tests --stdout
#   ./scripts/run-integration-tests.sh --vc-tests
#   ./scripts/run-integration-tests.sh --ekyc-tests --rerun 3
#   ./scripts/run-integration-tests.sh --ekyc-tests --rerun 3:2
#   ./scripts/run-integration-tests.sh --ekyc-tests --rerun 1,3
#
# Available options (passed through to run-tests.sh):
#   --client-tests --oidcc-tests --fapi-tests --ciba-tests
#   --local-provider-tests --panva-tests --ekyc-tests --authzen-tests
#   --federation-tests --ssf-tests --vc-tests
#
# Rerun options (requires same suite option as original run):
#   --rerun N       Rerun plan number N
#   --rerun N:M     Rerun module M of plan N
#   --rerun N,M     Rerun plans N and M
#
# Additional options:
#   --stdout        Print output directly to terminal instead of a temporary file
#
# Prerequisites:
#   - MongoDB running on 127.0.0.1:27017 (via devenv or docker-compose)
#   - Nginx HTTPS proxy running (ports 8443/8444 -> 8080)
#   - ../conformance-suite-private checkout (for test configs)
#   - Python 3 with pyparsing >= 3

set -euo pipefail

SUITE_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# For worktrees, devenv state lives in the main repo. Resolve it.
MAIN_REPO="$(dirname "$(git -C "$SUITE_DIR" rev-parse --git-common-dir 2>/dev/null)" 2>/dev/null)"

# Add devenv profile to PATH for tools like mongosh (added first so venv python takes priority)
for candidate in "${SUITE_DIR}/.devenv/profile/bin" "${MAIN_REPO}/.devenv/profile/bin"; do
    if [ -d "$candidate" ]; then
        export PATH="${candidate}:$PATH"
        break
    fi
done

# Activate the devenv venv so python3 has all required dependencies (httpx, pyparsing, cryptography)
# Added after profile so venv python (with httpx etc) takes priority over profile python
for candidate in "${SUITE_DIR}/.devenv/state/venv" "${MAIN_REPO}/.devenv/state/venv"; do
    if [ -d "$candidate" ]; then
        export PATH="${candidate}/bin:$PATH"
        break
    fi
done

JAR="${SUITE_DIR}/target/fapi-test-suite.jar"
SERVER_PORT=8080
SERVER_LOG="${SUITE_DIR}/target/server.log"

# Parse arguments
PRINT_TO_TERMINAL=false
ARGS=()
for arg in "$@"; do
    if [ "$arg" == "--stdout" ]; then
        PRINT_TO_TERMINAL=true
    else
        ARGS+=("$arg")
    fi
done
set -- "${ARGS[@]}"

# Auto-capture all output to a log file unless --stdout is specified
if [ "$PRINT_TO_TERMINAL" = false ]; then
    TEST_LOG="/tmp/integration-test-$(date +%Y%m%d-%H%M%S).log"
    echo "==> Logging to: $TEST_LOG"
    exec > "$TEST_LOG" 2>&1
fi

# Default test suite if none specified
if [ "$#" -eq 0 ]; then
    set -- --federation-tests
fi
TEST_SUITE="$1"

die() { echo "ERROR: $*" >&2; exit 1; }

# --- 1. Build ---
echo "==> Building JAR..."
cd "$SUITE_DIR"
mvn -B -DskipTests -Dpmd.skip clean package || die "Build failed"
[ -f "$JAR" ] || die "JAR not found at $JAR"

# --- 2. Kill existing server ---
echo "==> Stopping any existing server..."
pkill -f "fapi-test-suite.jar" || true
# Kill anything on the server port
kill $(lsof -tiTCP:${SERVER_PORT} -sTCP:LISTEN 2>/dev/null) 2>/dev/null || true
sleep 1

# --- 3. Detect ngrok tunnel ---
NGROK_HOSTNAME=""
NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | python3 -c 'import sys,json; t=json.load(sys.stdin).get("tunnels",[]); print(t[0]["public_url"] if t else "")' 2>/dev/null || true)
if [ -n "$NGROK_URL" ]; then
    echo "==> Detected ngrok tunnel: $NGROK_URL"
    export EXTERNAL_URL="$NGROK_URL"
    NGROK_HOSTNAME=$(python3 -c "import urllib.parse; print(urllib.parse.urlsplit('$NGROK_URL').hostname)")
    echo "==> Ngrok hostname: $NGROK_HOSTNAME"
    # Generate VP test signing key+cert with ngrok hostname in SAN so x509_san_dns tests work
    if [ "$TEST_SUITE" = "--vc-tests" ]; then
        echo "==> Generating VP test cert with ngrok hostname in SAN..."
        python3 "${SUITE_DIR}/scripts/generate-vp-test-cert.py" \
            --hostname "$NGROK_HOSTNAME" \
            --output "${SUITE_DIR}/scripts/certs-keys/vp-signing-jwk.json"
    fi
fi

# --- 4. Start server in background ---
echo "==> Starting server (logging to ${SERVER_LOG})..."
if [ "$TEST_SUITE" = "--security-tests" ]; then
    # Security tests run in non-dev mode with API token auth
    SECURITY_TOKEN=$(python3 -c "import secrets; print(secrets.token_urlsafe(48))")
    mongosh mongodb://127.0.0.1:27017/test_suite --quiet --eval "
      db.API_TOKEN.updateOne(
        { _id: 'security_test_token' },
        { \$set: { _id: 'security_test_token', owner: { sub: 'security-test', iss: 'https://localhost.emobix.co.uk:8443' }, info: {}, token: '${SECURITY_TOKEN}', expires: null } },
        { upsert: true }
      )" || die "Failed to insert API token into MongoDB"
    export CONFORMANCE_TOKEN="$SECURITY_TOKEN"
    echo "    API token inserted into MongoDB"
    java -jar "$JAR" \
      --fintechlabs.devmode=false \
      --spring.data.mongodb.uri=mongodb://127.0.0.1:27017/test_suite \
      --fintechlabs.base_url=https://localhost.emobix.co.uk:8443 \
      --fintechlabs.base_mtls_url=https://localhost.emobix.co.uk:8444 \
      --server.port="$SERVER_PORT" \
      > "$SERVER_LOG" 2>&1 &
else
    java -jar "$JAR" \
      --spring.profiles.active=dev \
      --server.port="$SERVER_PORT" \
      > "$SERVER_LOG" 2>&1 &
fi
SERVER_PID=$!
echo "    Server PID: $SERVER_PID"

# Ensure we clean up the server on exit
cleanup() {
    echo ""
    echo "==> Stopping server (PID $SERVER_PID)..."
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
    # Restore VP signing JWK overwritten by generate-vp-test-cert.py
    git -C "$SUITE_DIR" checkout -- scripts/certs-keys/vp-signing-jwk.json 2>/dev/null || true
    echo "==> Done."
}
trap cleanup EXIT INT TERM

# --- 5. Run tests ---
echo "==> Running tests: $TEST_SUITE"
"${SUITE_DIR}/.gitlab-ci/run-tests.sh" "$@" && TEST_EXIT=0 || TEST_EXIT=$?

echo ""
if [ "$TEST_EXIT" -eq 0 ]; then
    echo "==> Tests passed!"
else
    echo "==> Tests failed (exit code $TEST_EXIT)"
fi

exit "$TEST_EXIT"
