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
    // User-set, URL-safe alias. Lives inside `config` on the authenticated
    // /api/plan/{id} shape; the plan-detail header reads `plan.config.alias`.
    alias: "oidcc-basic-run-1",
    "server.issuer": "https://op.example.com",
    "client.client_id": "test-client-id",
    "client.client_secret": "test-client-secret",
  },
};

/**
 * GET /api/plan/:planId — MOCK_PLAN_DETAIL with a long plan name, a
 * five-entry variant map, and a two-entry certification profile.
 * Reproduces the conditions of the mobile readability bug fixed in
 * cts-plan-header: the metadata <dl> must stack at phone widths so
 * long values get the full header width (the short default values
 * never exercise the squeeze), and "Certification profile:" — the
 * longest label — must be present so the layout is measured against
 * the label that sized the legacy max-content track.
 */
export const MOCK_PLAN_DETAIL_LONG_VARIANT = {
  ...MOCK_PLAN_DETAIL,
  _id: "plan-long-001",
  planName: "oid4vci-id2-issuer-test-plan-credential-offer-pre-authorized-code",
  variant: {
    client_auth_type: "client_secret_basic",
    response_type: "code",
    credential_format: "sd_jwt_vc",
    sender_constrain: "dpop",
    response_mode: "direct_post.jwt",
  },
  certificationProfileName: ["FAPI2SP Final OP w/ MTLS", "FAPI2MS ID1 OP w/ Private Key"],
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
 * GET /api/info/:testId — PASSED, with a long module name and a
 * five-entry variant map. Reproduces the conditions of the mobile
 * readability bug fixed in cts-log-detail-header: the name is long
 * enough that the sticky status bar must truncate it (the short
 * default `oidcc-server` never overflows), and the variant map
 * exercises the stacked metadata layout at phone widths.
 */
export const MOCK_TEST_STATUS_LONG_VARIANT = {
  ...MOCK_TEST_STATUS,
  _id: "test-long-001",
  testId: "test-long-001",
  testName: "oid4vci-id2-issuer-credential-offer-flow-with-pre-authorized-code",
  variant: {
    client_auth_type: "client_secret_basic",
    response_type: "code",
    credential_format: "sd_jwt_vc",
    sender_constrain: "dpop",
    response_mode: "direct_post.jwt",
  },
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
