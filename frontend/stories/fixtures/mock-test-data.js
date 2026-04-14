/**
 * Mock test/plan detail data matching API response shapes.
 *
 * GET /api/plan/:planId — returns a plan with modules, variant, owner, etc.
 * GET /api/info/:testId — returns test status/result info.
 * GET /api/runner/running — returns array of running tests.
 * GET /api/server — returns version/build info.
 */

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
  owner: {
    sub: "12345",
    iss: "https://accounts.google.com",
  },
  publish: null,
  immutable: false,
  certificationProfileName: "OC Basic OP",
  summary: "Basic OP certification test plan for OpenID Connect Core 1.0",
  modules: [
    {
      testModule: "oidcc-server",
      testSummary: "Verify basic OpenID Connect server functionality",
      variant: { client_auth_type: "client_secret_basic", response_type: "code" },
      instances: ["test-inst-001"],
    },
    {
      testModule: "oidcc-server-rotate-keys",
      testSummary: "Verify key rotation behavior",
      variant: { client_auth_type: "client_secret_basic", response_type: "code" },
      instances: ["test-inst-002"],
    },
    {
      testModule: "oidcc-ensure-redirect-uri-in-authorization-request",
      testSummary: "Ensure redirect_uri is included in authorization request",
      variant: { client_auth_type: "client_secret_basic", response_type: "code" },
      instances: ["test-inst-003"],
    },
    {
      testModule: "oidcc-codereuse",
      testSummary: "Verify authorization code reuse detection",
      variant: { client_auth_type: "client_secret_basic", response_type: "code" },
      instances: [],
    },
  ],
};

/** A published, immutable plan for edge-case stories */
export const MOCK_PLAN_PUBLISHED = {
  ...MOCK_PLAN_DETAIL,
  _id: "plan-pub-789",
  publish: "everything",
  immutable: true,
};

/** GET /api/info/:testId response shape */
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
  variant: { client_auth_type: "client_secret_basic", response_type: "code" },
  owner: { sub: "12345", iss: "https://accounts.google.com" },
  config: {
    "server.issuer": "https://op.example.com",
    "client.client_id": "test-client-id",
    "client.client_secret": "test-client-secret",
  },
  exposed: {},
};

/** A running test for running-test stories */
export const MOCK_TEST_RUNNING = {
  ...MOCK_TEST_STATUS,
  _id: "test-running-001",
  testId: "test-running-001",
  status: "RUNNING",
  result: null,
  exposed: {
    authorization_endpoint_request: "https://op.example.com/authorize?client_id=test&redirect_uri=...",
  },
};

/** A waiting test for waiting-state stories */
export const MOCK_TEST_WAITING = {
  ...MOCK_TEST_STATUS,
  _id: "test-waiting-001",
  testId: "test-waiting-001",
  status: "WAITING",
  result: null,
};

/** A failed test */
export const MOCK_TEST_FAILED = {
  ...MOCK_TEST_STATUS,
  _id: "test-fail-001",
  testId: "test-fail-001",
  status: "FINISHED",
  result: "FAILED",
};

/** GET /api/runner/running response shape */
export const MOCK_RUNNING_TESTS = [
  {
    _id: "test-running-001",
    testId: "test-running-001",
    testName: "oidcc-server",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    version: "5.1.24-SNAPSHOT (9063a08)",
    created: new Date(NOW - 300000).toISOString(),
    owner: { sub: "12345", iss: "https://accounts.google.com" },
    status: "RUNNING",
    planId: "plan-abc-123",
  },
  {
    _id: "test-running-002",
    testId: "test-running-002",
    testName: "oidcc-server-rotate-keys",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    version: "5.1.24-SNAPSHOT (9063a08)",
    created: new Date(NOW - 120000).toISOString(),
    owner: { sub: "12345", iss: "https://accounts.google.com" },
    status: "WAITING",
    planId: "plan-abc-123",
  },
];

/** GET /api/server response shape */
export const MOCK_SERVER_INFO = {
  version: "5.1.24-SNAPSHOT",
  revision: "9063a08",
  tag: "v5.1.24-SNAPSHOT",
  build_time: "2026-04-12T10:30:00Z",
  external_ip: "192.168.1.100",
};
