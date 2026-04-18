const NOW = Date.now();

/**
 * Mock log entries matching the /api/log/{id} response shape.
 * Each entry has: _id, testId, src, time, msg, and optionally
 * result, blockId, startBlock, http, requirements, more, upload.
 */
export const MOCK_LOG_ENTRIES = [
  {
    _id: "entry-1",
    testId: "test-abc-123",
    src: "CheckServerConfiguration",
    time: NOW - 10000,
    msg: "Checking server configuration",
    result: "SUCCESS",
  },
  {
    _id: "entry-2",
    testId: "test-abc-123",
    src: "CreateAuthorizationRequest",
    time: NOW - 9000,
    msg: "Authorization endpoint",
    blockId: "block-auth",
    startBlock: true,
  },
  {
    _id: "entry-3",
    testId: "test-abc-123",
    src: "BuildRedirectUri",
    time: NOW - 8500,
    msg: "Built redirect URI: https://example.com/callback?code=abc",
    blockId: "block-auth",
    http: "REQUEST",
    more: {
      method: "GET",
      url: "https://op.example.com/authorize?client_id=test-client&redirect_uri=https%3A%2F%2Fexample.com%2Fcallback&response_type=code&scope=openid",
      headers: {
        Accept: "text/html",
        "User-Agent": "conformance-suite/5.1.24",
      },
    },
  },
  {
    _id: "entry-4",
    testId: "test-abc-123",
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
    testId: "test-abc-123",
    src: "CallTokenEndpoint",
    time: NOW - 6000,
    msg: "Token endpoint",
    blockId: "block-token",
    startBlock: true,
  },
  {
    _id: "entry-6",
    testId: "test-abc-123",
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
    testId: "test-abc-123",
    src: "ExtractAccessToken",
    time: NOW - 4500,
    msg: "Extracted access token from token endpoint response",
    blockId: "block-token",
    result: "SUCCESS",
  },
  {
    _id: "entry-8",
    testId: "test-abc-123",
    src: "ValidateIdToken",
    time: NOW - 3000,
    msg: "ID token signature validation failed: key not found in JWKS",
    result: "FAILURE",
    requirements: ["OIDCC-3.1.3.7-6"],
  },
  {
    _id: "entry-9",
    testId: "test-abc-123",
    src: "CheckForUnexpectedFieldsInIdToken",
    time: NOW - 2000,
    msg: "Unexpected field 'extra_claim' in ID token",
    result: "WARNING",
  },
  {
    _id: "entry-10",
    testId: "test-abc-123",
    src: "CheckTestOutcome",
    time: NOW - 1000,
    msg: "Test failed: 1 failure, 2 warnings",
    result: "FAILURE",
  },
];

/** An entry with upload requirement (for image upload stories) */
export const MOCK_UPLOAD_ENTRY = {
  _id: "entry-upload-1",
  testId: "test-abc-123",
  src: "CheckScreenshot",
  time: NOW - 500,
  msg: "Screenshot required: Upload a screenshot showing the consent screen",
  result: "REVIEW",
  upload: "screenshot_consent",
};

export const MOCK_EMPTY_LOG = [];

export const MOCK_SUCCESS_LOG = [
  {
    _id: "s-1",
    testId: "test-ok-456",
    src: "CheckServerConfiguration",
    time: NOW - 5000,
    msg: "Server configuration valid",
    result: "SUCCESS",
  },
  {
    _id: "s-2",
    testId: "test-ok-456",
    src: "ValidateIdToken",
    time: NOW - 3000,
    msg: "ID token is valid",
    result: "SUCCESS",
  },
  {
    _id: "s-3",
    testId: "test-ok-456",
    src: "CheckTestOutcome",
    time: NOW - 1000,
    msg: "Test passed",
    result: "SUCCESS",
  },
];
