/**
 * GET /api/plan/:planId/findings response for MOCK_PLAN_DETAIL: one failure
 * shared by two modules, a warning logged twice by one module, and an
 * uploaded image awaiting review; plus each failed run's earliest failure.
 */
export const MOCK_PLAN_FINDINGS = {
  findings: [
    {
      kind: "FAILURE",
      src: "CheckDiscEndpointIssuer",
      msg: "issuer in discovery document does not match the configured issuer",
      requirements: ["OIDCD-4.3"],
      occurrences: [
        {
          moduleIndex: 0,
          testModule: "oidcc-server",
          variant: { client_auth_type: "client_secret_basic", response_type: "code" },
          testId: "test-inst-001",
          entryId: "test-inst-001-aaaa",
          count: 1,
        },
        {
          moduleIndex: 2,
          testModule: "oidcc-ensure-redirect-uri-in-authorization-request",
          variant: { client_auth_type: "client_secret_basic", response_type: "code" },
          testId: "test-inst-003",
          entryId: "test-inst-003-bbbb",
          count: 1,
        },
      ],
    },
    {
      kind: "WARNING",
      src: "EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys",
      msg: "jwks_uri returned a key without a kid",
      requirements: [],
      occurrences: [
        {
          moduleIndex: 1,
          testModule: "oidcc-server-rotate-keys",
          variant: { client_auth_type: "client_secret_basic", response_type: "code" },
          testId: "test-inst-002",
          entryId: "test-inst-002-cccc",
          count: 2,
        },
      ],
    },
    {
      kind: "IMAGE",
      src: "ExpectRedirectUriMissingErrorPage",
      msg: "Show an error page saying the redirect URI is missing",
      requirements: ["OIDCC-3.1.2.1"],
      occurrences: [
        {
          moduleIndex: 2,
          testModule: "oidcc-ensure-redirect-uri-in-authorization-request",
          variant: { client_auth_type: "client_secret_basic", response_type: "code" },
          testId: "test-inst-003",
          entryId: "test-inst-003-dddd",
          count: 1,
        },
      ],
    },
  ],
  firstFailures: [
    { moduleIndex: 0, testId: "test-inst-001", entryId: "test-inst-001-aaaa" },
    { moduleIndex: 2, testId: "test-inst-003", entryId: "test-inst-003-bbbb" },
  ],
};
