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

# --- 3. Start server in background ---
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
    echo "==> Done."
}
trap cleanup EXIT INT TERM

# --- 4. Run tests ---
echo "==> Running tests: $TEST_SUITE"
"${SUITE_DIR}/.gitlab-ci/run-tests.sh" "$@" && TEST_EXIT=0 || TEST_EXIT=$?

echo ""
if [ "$TEST_EXIT" -eq 0 ]; then
    echo "==> Tests passed!"
else
    echo "==> Tests failed (exit code $TEST_EXIT)"
fi

exit "$TEST_EXIT"
