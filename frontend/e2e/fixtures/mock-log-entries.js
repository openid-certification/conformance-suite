/** Mock /api/log/:testId response shapes. */

const NOW = Date.now();

export const MOCK_LOG_ENTRIES = [
  {
    _id: "entry-1",
    testId: "test-inst-001",
    src: "CheckServerConfiguration",
    time: NOW - 10000,
    msg: "Checking server configuration",
    result: "SUCCESS",
  },
  {
    _id: "entry-2",
    testId: "test-inst-001",
    src: "CreateAuthorizationRequest",
    time: NOW - 9000,
    msg: "Authorization endpoint",
    blockId: "block-auth",
    startBlock: true,
  },
  {
    _id: "entry-3",
    testId: "test-inst-001",
    src: "BuildRedirectUri",
    time: NOW - 8500,
    msg: "Built redirect URI: https://example.com/callback?code=abc",
    blockId: "block-auth",
    http: "REQUEST",
    more: {
      method: "GET",
      url: "https://op.example.com/authorize?client_id=test-client&response_type=code&scope=openid",
      headers: {
        Accept: "text/html",
        "User-Agent": "conformance-suite/5.1.24",
      },
    },
  },
  {
    _id: "entry-4",
    testId: "test-inst-001",
    src: "ValidateAuthorizationResponse",
    time: NOW - 7000,
    msg: "Received valid authorization code",
    blockId: "block-auth",
    result: "SUCCESS",
    http: "INCOMING",
    more: {
      code: "auth-code-12345",
      state: "random-state-value",
    },
  },
  {
    _id: "entry-5",
    testId: "test-inst-001",
    src: "CallTokenEndpoint",
    time: NOW - 6000,
    msg: "Token endpoint",
    blockId: "block-token",
    startBlock: true,
  },
  {
    _id: "entry-6",
    testId: "test-inst-001",
    src: "EnsureTokenEndpointResponseHasCorrectFields",
    time: NOW - 5000,
    msg: "Token endpoint returned unexpected field 'custom_field'",
    blockId: "block-token",
    result: "WARNING",
    requirements: ["OIDCC-3.1.3.3"],
    http: "RESPONSE",
    more: {
      status: 200,
      body: {
        access_token: "eyJ...",
        token_type: "Bearer",
        expires_in: 3600,
        id_token: "eyJ...",
        custom_field: "unexpected",
      },
    },
  },
  {
    _id: "entry-7",
    testId: "test-inst-001",
    src: "ExtractAccessToken",
    time: NOW - 4500,
    msg: "Extracted access token from token endpoint response",
    blockId: "block-token",
    result: "SUCCESS",
  },
];

/** Log entries for a failed test (R19) */
export const MOCK_FAILED_LOG_ENTRIES = [
  {
    _id: "f-1",
    testId: "test-fail-001",
    src: "CheckServerConfiguration",
    time: NOW - 10000,
    msg: "Checking server configuration",
    result: "SUCCESS",
  },
  {
    _id: "f-2",
    testId: "test-fail-001",
    src: "ValidateIdToken",
    time: NOW - 3000,
    msg: "ID token signature validation failed: key not found in JWKS",
    result: "FAILURE",
    requirements: ["OIDCC-3.1.3.7-6"],
  },
  {
    _id: "f-3",
    testId: "test-fail-001",
    src: "CheckTestOutcome",
    time: NOW - 1000,
    msg: "Test failed: 1 failure",
    result: "FAILURE",
  },
];

/** Log entries for a test with warnings (R20) */
export const MOCK_WARNING_LOG_ENTRIES = [
  {
    _id: "w-1",
    testId: "test-warn-001",
    src: "CheckServerConfiguration",
    time: NOW - 10000,
    msg: "Checking server configuration",
    result: "SUCCESS",
  },
  {
    _id: "w-2",
    testId: "test-warn-001",
    src: "CheckForUnexpectedFields",
    time: NOW - 5000,
    msg: "Unexpected field 'extra_claim' in ID token",
    result: "WARNING",
    requirements: ["OIDCC-3.1.3.3"],
  },
  {
    _id: "w-3",
    testId: "test-warn-001",
    src: "CheckTestOutcome",
    time: NOW - 1000,
    msg: "Test passed with 1 warning",
    result: "WARNING",
  },
];
