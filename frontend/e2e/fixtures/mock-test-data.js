/** Mock test/plan detail data matching API response shapes. */

const NOW = Date.now();
const DAY_MS = 86400000;

/** GET /api/plan/:planId response shape */
export const MOCK_PLAN_DETAIL = {
  _id: "plan-abc-123",
  planName: "oidcc-basic-certification-test-plan",
  description: "OpenID Connect Core: Basic Certification Profile authorization server test",
  variant: {
    client_auth_type: "client_secret_basic",
    response_type: "code",
    server_metadata: "discovery",
  },
  version: "5.1.24-SNAPSHOT (9063a08)",
  started: new Date(NOW - 2 * DAY_MS).toISOString(),
  owner: { sub: "12345", iss: "https://accounts.google.com" },
  publish: null,
  immutable: false,
  certificationProfileName: "OC Basic OP",
  summary: "Basic OP certification test plan for OpenID Connect Core 1.0",
  modules: [
    {
      testModule: "oidcc-server",
      testSummary: "Verify basic OpenID Connect server functionality",
      variant: {
        client_auth_type: "client_secret_basic",
        response_type: "code",
      },
      instances: ["test-inst-001"],
    },
    {
      testModule: "oidcc-server-rotate-keys",
      testSummary: "Verify key rotation behavior",
      variant: {
        client_auth_type: "client_secret_basic",
        response_type: "code",
      },
      instances: ["test-inst-002"],
    },
    {
      testModule: "oidcc-ensure-redirect-uri-in-authorization-request",
      testSummary: "Ensure redirect_uri is included in authorization request",
      variant: {
        client_auth_type: "client_secret_basic",
        response_type: "code",
      },
      instances: ["test-inst-003"],
    },
    {
      testModule: "oidcc-codereuse",
      testSummary: "Verify authorization code reuse detection",
      variant: {
        client_auth_type: "client_secret_basic",
        response_type: "code",
      },
      instances: [],
    },
  ],
  config: {
    "server.issuer": "https://op.example.com",
    "client.client_id": "test-client-id",
    "client.client_secret": "test-client-secret",
  },
};

/** GET /api/info/:testId response shape — PASSED */
export const MOCK_TEST_STATUS = {
  _id: "test-inst-001",
  testId: "test-inst-001",
  testName: "oidcc-server",
  status: "FINISHED",
  result: "PASSED",
  version: "5.1.24-SNAPSHOT (9063a08)",
  created: new Date(NOW - DAY_MS).toISOString(),
  description: "Tests the basic OpenID Connect authorization server functionality",
  planId: "plan-abc-123",
  variant: {
    client_auth_type: "client_secret_basic",
    response_type: "code",
  },
  owner: { sub: "12345", iss: "https://accounts.google.com" },
  config: {
    "server.issuer": "https://op.example.com",
    "client.client_id": "test-client-id",
    "client.client_secret": "test-client-secret",
  },
  exposed: {},
};

/**
 * GET /api/info/:testId — summary contains the R24 split marker
 * `\n\n---\n\n`. The pre-marker half describes what the test checks;
 * the post-marker half is imperative user instructions. The frontend
 * (cts-log-detail-header) renders the two halves as separate
 * "About this test" and "What you need to do" zones.
 */
export const MOCK_TEST_STATUS_WITH_INSTRUCTIONS = {
  ...MOCK_TEST_STATUS,
  _id: "test-instr-001",
  testId: "test-instr-001",
  summary:
    "This test calls the authorization endpoint with a login_hint, which must not result in errors.\n\n---\n\nPlease remove any cookies you may have received from the OpenID Provider before proceeding. A fresh login page is needed.",
};

/** GET /api/info/:testId — summary present, no R24 split marker. */
export const MOCK_TEST_STATUS_WITH_DESCRIPTION_ONLY = {
  ...MOCK_TEST_STATUS,
  _id: "test-desc-001",
  testId: "test-desc-001",
  summary:
    "This test calls the authorization endpoint and verifies the OP returns a normal login page.",
};

/** GET /api/info/:testId — FAILED */
export const MOCK_TEST_FAILED = {
  ...MOCK_TEST_STATUS,
  _id: "test-fail-001",
  testId: "test-fail-001",
  result: "FAILED",
};

/** GET /api/info/:testId — WARNING */
export const MOCK_TEST_WARNING = {
  ...MOCK_TEST_STATUS,
  _id: "test-warn-001",
  testId: "test-warn-001",
  result: "WARNING",
};

/** A running test with exposed URLs */
export const MOCK_TEST_RUNNING = {
  ...MOCK_TEST_STATUS,
  _id: "test-running-001",
  testId: "test-running-001",
  testName: "oidcc-server",
  status: "RUNNING",
  result: null,
  exposed: {
    authorization_endpoint_request:
      "https://op.example.com/authorize?client_id=test&redirect_uri=https://example.com/callback",
  },
};

/** A second running test */
export const MOCK_TEST_RUNNING_2 = {
  ...MOCK_TEST_STATUS,
  _id: "test-running-002",
  testId: "test-running-002",
  testName: "oidcc-server-rotate-keys",
  status: "WAITING",
  result: null,
  exposed: {},
};
