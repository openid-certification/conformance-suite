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
