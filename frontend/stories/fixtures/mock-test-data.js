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

/**
 * MOCK_PLAN_DETAIL.modules augmented with realistic status/result values.
 * Module 1: PASSED, Module 2: WARNING, Module 3: FAILED, Module 4: PENDING (no instance).
 *
 * Module 3 carries `firstFailureRef` to exercise the R28 deep-link
 * contract: when a row is FAILED and the page-level shim has resolved
 * the first failure entry's `LOG-NNNN` ordinal, the lozenge href is
 * appended with that fragment. The non-FAILED rows intentionally do
 * NOT carry `firstFailureRef`, so the result-gate in cts-plan-modules
 * is exercised by the absence as well as by fixtures that set it on
 * the wrong row (see MOCK_MODULES_WRONG_REF_PLACEMENT).
 */
export const MOCK_MODULES_WITH_STATUS = [
  { ...MOCK_PLAN_DETAIL.modules[0], status: "FINISHED", result: "PASSED" },
  { ...MOCK_PLAN_DETAIL.modules[1], status: "FINISHED", result: "WARNING" },
  {
    ...MOCK_PLAN_DETAIL.modules[2],
    status: "FINISHED",
    result: "FAILED",
    firstFailureRef: "LOG-0042",
  },
  { ...MOCK_PLAN_DETAIL.modules[3], status: null, result: null },
];

/**
 * Variant where the FAILED row has no `firstFailureRef` (loading window
 * before the per-FAILED log fetch resolves, or the fetch failed).
 * Used to assert R1 fail-soft: the lozenge falls back to the R28
 * top-of-log href and the original aria-label, never a stray `#`.
 */
export const MOCK_MODULES_FAILED_WITHOUT_REF = [
  { ...MOCK_PLAN_DETAIL.modules[0], status: "FINISHED", result: "PASSED" },
  { ...MOCK_PLAN_DETAIL.modules[2], status: "FINISHED", result: "FAILED" },
];

/**
 * Defensive variant: a PASSED, a WARNING, and a REVIEW row each carry
 * `firstFailureRef`. This pins the result-gate in cts-plan-modules —
 * the fragment is appended only when `result === "FAILED"`, never on
 * the field's mere presence.
 */
export const MOCK_MODULES_WRONG_REF_PLACEMENT = [
  {
    ...MOCK_PLAN_DETAIL.modules[0],
    status: "FINISHED",
    result: "PASSED",
    firstFailureRef: "LOG-0007",
  },
  {
    ...MOCK_PLAN_DETAIL.modules[1],
    status: "FINISHED",
    result: "WARNING",
    firstFailureRef: "LOG-0011",
  },
  {
    ...MOCK_PLAN_DETAIL.modules[2],
    status: "FINISHED",
    result: "REVIEW",
    firstFailureRef: "LOG-0017",
  },
];

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
    authorization_endpoint_request:
      "https://op.example.com/authorize?client_id=test&redirect_uri=...",
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
