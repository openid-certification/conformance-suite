#!/bin/bash
#
# Run integration tests locally.
#
# Usage:
#   ./scripts/run-integration-tests.sh [run-tests.sh option] [--rerun ID]
#
# Examples:
#   ./scripts/run-integration-tests.sh --federation-tests
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
# Prerequisites:
#   - MongoDB running on 127.0.0.1:27017 (via devenv or docker-compose)
#   - Nginx HTTPS proxy running (ports 8443/8444 -> 8080)
#   - ../conformance-suite-private checkout (for test configs)
#   - Python 3 with pyparsing >= 3

set -euo pipefail

SUITE_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# Activate the devenv venv so python3 has all required dependencies (httpx, pyparsing, cryptography)
VENV_DIR="${SUITE_DIR}/.devenv/state/venv"
if [ -d "$VENV_DIR" ]; then
    export PATH="${VENV_DIR}/bin:$PATH"
fi

JAR="${SUITE_DIR}/target/fapi-test-suite.jar"
SERVER_PORT=8080
SERVER_LOG="${SUITE_DIR}/target/server.log"

# Auto-capture all output to a log file
TEST_LOG="/tmp/integration-test-$(date +%Y%m%d-%H%M%S).log"
echo "==> Logging to: $TEST_LOG"
exec > "$TEST_LOG" 2>&1

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
        VP_CONFIGS="${SUITE_DIR}/scripts/test-configs-rp-against-op"
        echo "==> Generating VP test cert with ngrok hostname in SAN..."
        python3 "${SUITE_DIR}/scripts/generate-vp-test-cert.py" \
            --hostname "$NGROK_HOSTNAME" \
            --patch "${VP_CONFIGS}/vp-wallet-test-config.json" \
            --patch "${VP_CONFIGS}/vp-wallet-test-config-dcql.json" \
            --patch "${VP_CONFIGS}/vp-verifier-test-config.json" \
            --patch "${VP_CONFIGS}/vp-verifier-test-config-with-redirect.json" \
            --patch "${VP_CONFIGS}/vp-verifier-test-config-with-redirect-alt.json" \
            --patch "${VP_CONFIGS}/vp-verifier-test-config-with-redirect-no-client-id.json"
    fi
fi

# --- 4. Start server in background ---
echo "==> Starting server (logging to ${SERVER_LOG})..."
java -jar "$JAR" \
  --spring.profiles.active=dev \
  --server.port="$SERVER_PORT" \
  > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!
echo "    Server PID: $SERVER_PID"

# Ensure we clean up the server on exit
cleanup() {
    echo ""
    echo "==> Stopping server (PID $SERVER_PID)..."
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
    # Restore VP test configs patched by generate-vp-test-cert.py
    git -C "$SUITE_DIR" checkout -- scripts/test-configs-rp-against-op/vp-*.json 2>/dev/null || true
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
