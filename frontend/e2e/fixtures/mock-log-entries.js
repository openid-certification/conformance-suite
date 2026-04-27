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

/**
 * Three blocks with mixed counts for U5 per-block aggregation tests.
 * Block A: 2 SUCCESS — clean (renders ✓2 only).
 * Block B: 1 SUCCESS, 1 FAILURE — renders ✓1 ✗1.
 * Block C: 1 WARNING, 1 INFO — INFO is excluded from block badges so the
 *          cluster reads ⚠1 only (the INFO still counts toward `total`).
 */
export const MOCK_BLOCKS_WITH_STATUS = [
  {
    _id: "blk-a-start",
    testId: "test-blocks-001",
    src: "StartBlockA",
    time: NOW - 9000,
    msg: "Block A — passing checks",
    blockId: "block-a",
    startBlock: true,
  },
  {
    _id: "blk-a-1",
    testId: "test-blocks-001",
    src: "CheckOne",
    time: NOW - 8800,
    msg: "First check passed",
    blockId: "block-a",
    result: "SUCCESS",
  },
  {
    _id: "blk-a-2",
    testId: "test-blocks-001",
    src: "CheckTwo",
    time: NOW - 8600,
    msg: "Second check passed",
    blockId: "block-a",
    result: "SUCCESS",
  },
  {
    _id: "blk-b-start",
    testId: "test-blocks-001",
    src: "StartBlockB",
    time: NOW - 8000,
    msg: "Block B — one failure",
    blockId: "block-b",
    startBlock: true,
  },
  {
    _id: "blk-b-1",
    testId: "test-blocks-001",
    src: "CheckOne",
    time: NOW - 7800,
    msg: "Setup ok",
    blockId: "block-b",
    result: "SUCCESS",
  },
  {
    _id: "blk-b-2",
    testId: "test-blocks-001",
    src: "ValidateIdToken",
    time: NOW - 7600,
    msg: "Signature validation failed",
    blockId: "block-b",
    result: "FAILURE",
    requirements: ["OIDCC-3.1.3.7-6"],
  },
  {
    _id: "blk-c-start",
    testId: "test-blocks-001",
    src: "StartBlockC",
    time: NOW - 7000,
    msg: "Block C — warning + info",
    blockId: "block-c",
    startBlock: true,
  },
  {
    _id: "blk-c-1",
    testId: "test-blocks-001",
    src: "CheckExtraField",
    time: NOW - 6800,
    msg: "Unexpected extra_claim in id_token",
    blockId: "block-c",
    result: "WARNING",
  },
  {
    _id: "blk-c-2",
    testId: "test-blocks-001",
    src: "LogContext",
    time: NOW - 6600,
    msg: "Context dump",
    blockId: "block-c",
    result: "INFO",
  },
];

/**
 * Block A from MOCK_BLOCKS_WITH_STATUS in two halves: the initial poll
 * returns the start row + 2 successes; the second poll appends a third
 * success and a failure. Used by `BlockCountsUpdateOnPolling` to assert
 * the badge cluster transitions from `✓2` → `✓3 ✗1`.
 */
export const MOCK_BLOCKS_POLL_FIRST = [
  {
    _id: "poll-blk-start",
    testId: "test-poll-001",
    src: "StartBlockPoll",
    time: NOW - 5000,
    msg: "Streaming block",
    blockId: "block-poll",
    startBlock: true,
  },
  {
    _id: "poll-blk-1",
    testId: "test-poll-001",
    src: "FirstCheck",
    time: NOW - 4900,
    msg: "First check passed",
    blockId: "block-poll",
    result: "SUCCESS",
  },
  {
    _id: "poll-blk-2",
    testId: "test-poll-001",
    src: "SecondCheck",
    time: NOW - 4800,
    msg: "Second check passed",
    blockId: "block-poll",
    result: "SUCCESS",
  },
];

export const MOCK_BLOCKS_POLL_SECOND = [
  {
    _id: "poll-blk-3",
    testId: "test-poll-001",
    src: "ThirdCheck",
    time: NOW - 4700,
    msg: "Third check passed",
    blockId: "block-poll",
    result: "SUCCESS",
  },
  {
    _id: "poll-blk-4",
    testId: "test-poll-001",
    src: "FailingCheck",
    time: NOW - 4600,
    msg: "Validation failed",
    blockId: "block-poll",
    result: "FAILURE",
  },
];

/** A block-start row with no children yet — WAITING / RUNNING state. */
export const MOCK_EMPTY_BLOCK = [
  {
    _id: "empty-blk-start",
    testId: "test-empty-block-001",
    src: "StartBlockEmpty",
    time: NOW - 3000,
    msg: "Awaiting checks",
    blockId: "block-empty",
    startBlock: true,
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
