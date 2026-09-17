/**
 * Mock /api/log list response shape. Each row carries `planName`, which the
 * server attaches from the plan the test belongs to.
 */

const NOW = Date.now();
const DAY_MS = 86400000;

const OWNER = { sub: "12345", iss: "https://accounts.google.com" };

export const MOCK_LOG_LIST = [
  {
    testId: "test-log-001",
    testName: "oidcc-server",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    description: "Tests basic OpenID Connect server functionality",
    started: new Date(NOW - DAY_MS).toISOString(),
    planId: "plan-001",
    planName: "oidcc-basic-certification-test-plan",
    status: "FINISHED",
    result: "PASSED",
    owner: OWNER,
  },
  {
    testId: "test-log-002",
    testName: "oidcc-server-rotate-keys",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    description: "Tests key rotation behavior",
    started: new Date(NOW - DAY_MS / 2).toISOString(),
    planId: "plan-001",
    planName: "oidcc-basic-certification-test-plan",
    status: "FINISHED",
    result: "WARNING",
    owner: OWNER,
  },
  // Active rows used by URL-filter tests (?status=running,waiting).
  {
    testId: "test-log-003",
    testName: "fapi2-running",
    variant: { client_auth_type: "private_key_jwt", response_type: "code" },
    description: "An actively-running test",
    started: new Date(NOW - DAY_MS / 4).toISOString(),
    planId: "plan-002",
    planName: "fapi2-security-profile-final-test-plan",
    status: "RUNNING",
    result: "UNKNOWN",
    owner: OWNER,
  },
  {
    testId: "test-log-004",
    testName: "fapi2-waiting",
    variant: { client_auth_type: "private_key_jwt", response_type: "code" },
    description: "A test waiting on user interaction",
    started: new Date(NOW - DAY_MS / 8).toISOString(),
    planId: "plan-002",
    planName: "fapi2-security-profile-final-test-plan",
    status: "WAITING",
    result: "UNKNOWN",
    owner: OWNER,
  },
  // Failure rows used by URL-filter tests (?result=failed,unknown). The
  // UNKNOWN-result rows above also satisfy `?result=failed,unknown`.
  {
    testId: "test-log-005",
    testName: "vci-failed",
    variant: { credential_format: "sd_jwt_vc" },
    description: "A finished test that hit a hard failure",
    started: new Date(NOW - DAY_MS / 16).toISOString(),
    planId: "plan-003",
    planName: "vci-id-1-wallet-test-plan",
    status: "FINISHED",
    result: "FAILED",
    owner: OWNER,
  },
];
