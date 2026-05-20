/** Mock /api/log response shape for cts-log-list stories. */

const NOW = Date.now();
const DAY_MS = 86400000;
const HOUR_MS = 3600000;

const OWNER = { sub: "12345", iss: "https://accounts.google.com" };
const OTHER_OWNER = { sub: "67890", iss: "https://login.microsoftonline.com" };

export const MOCK_LOG_LIST = [
  {
    testId: "test-log-001",
    testName: "oidcc-server",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    description: "Tests basic OpenID Connect server functionality.",
    started: new Date(NOW - DAY_MS).toISOString(),
    planId: "plan-001",
    status: "FINISHED",
    result: "PASSED",
    owner: OWNER,
  },
  {
    testId: "test-log-002",
    testName: "oidcc-server-rotate-keys",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    description:
      "Tests key rotation behaviour: server publishes a new key, clients pick it up on next JWKS poll, the overlap window honours the previous key for in-flight requests.",
    started: new Date(NOW - HOUR_MS * 3).toISOString(),
    planId: "plan-001",
    status: "FINISHED",
    result: "WARNING",
    owner: OWNER,
  },
  {
    testId: "test-log-003",
    testName: "fapi2-running",
    variant: { client_auth_type: "private_key_jwt", response_type: "code" },
    description: "An actively-running test.",
    started: new Date(NOW - HOUR_MS).toISOString(),
    planId: "plan-002",
    status: "RUNNING",
    result: "UNKNOWN",
    owner: OWNER,
  },
  {
    testId: "test-log-004",
    testName: "fapi2-waiting",
    variant: { client_auth_type: "private_key_jwt", response_type: "code" },
    description: "A test waiting on user interaction.",
    started: new Date(NOW - HOUR_MS / 2).toISOString(),
    planId: "plan-002",
    status: "WAITING",
    result: "UNKNOWN",
    owner: OTHER_OWNER,
  },
  {
    testId: "test-log-005",
    testName: "vci-failed",
    variant: { credential_format: "sd_jwt_vc" },
    description: "A finished test that hit a hard failure during the issuance handshake.",
    started: new Date(NOW - HOUR_MS * 8).toISOString(),
    planId: "plan-003",
    status: "FINISHED",
    result: "FAILED",
    owner: OWNER,
  },
  {
    testId: "test-log-006",
    testName: "vp-review",
    variant: { credential_format: "ldp_vc" },
    description:
      "A verifiable-presentation test whose outcome requires reviewer judgement before a result is recorded.",
    started: new Date(NOW - HOUR_MS * 5).toISOString(),
    planId: "plan-004",
    status: "FINISHED",
    result: "REVIEW",
    owner: OWNER,
  },
];

/**
 * Generate a 60-row fixture for "Show more" pagination testing. Each row
 * mirrors the shape of `MOCK_LOG_LIST` but with deterministic ids.
 */
export const MOCK_LOG_LIST_LARGE = Array.from({ length: 60 }, (_, i) => ({
  testId: `test-log-${String(i + 1).padStart(3, "0")}`,
  testName: `oidcc-test-${String.fromCharCode(97 + (i % 26))}${i}`,
  variant: { client_auth_type: "client_secret_basic", response_type: "code" },
  description: `Test fixture row ${i + 1} for pagination behaviour.`,
  started: new Date(NOW - HOUR_MS * (i + 1)).toISOString(),
  planId: `plan-${String(Math.floor(i / 5) + 1).padStart(3, "0")}`,
  status: i % 7 === 0 ? "RUNNING" : "FINISHED",
  result: i % 5 === 0 ? "FAILED" : i % 3 === 0 ? "WARNING" : "PASSED",
  owner: OWNER,
}));
