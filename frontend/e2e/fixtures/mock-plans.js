/**
 * Mock /api/plan/available response shape (spec cascade data).
 *
 * The fields below mirror what `TestPlanApi.getAvailableTestPlans()` returns:
 * planName, displayName, profile, specFamily, specVersion, modules,
 * configurationFields, hidesConfigurationFields, summary, variants.
 *
 * The cts-spec-cascade component keys its level-2 ("Entity Under Test")
 * dropdown by `profile`, so e2e tests select profile values
 * ("basic" / "fapi2-security-profile" / "client-basic"), not entity nouns.
 */

export const MOCK_PLANS = [
  {
    planName: "oidcc-basic-certification-test-plan",
    displayName: "OpenID Connect Core: Basic Certification Profile",
    profile: "basic",
    specFamily: "OIDCC",
    specVersion: "Final",
    summary: "Basic OP certification test plan for OpenID Connect Core 1.0",
    modules: [
      { testModule: "oidcc-server" },
      { testModule: "oidcc-server-rotate-keys" },
      { testModule: "oidcc-ensure-redirect-uri-in-authorization-request" },
      { testModule: "oidcc-codereuse" },
    ],
    configurationFields: ["server.issuer", "client.client_id", "client.client_secret"],
    hidesConfigurationFields: [],
    variants: {
      client_auth_type: {
        variantInfo: {
          displayName: "Client Authentication Type",
          description: "How the client authenticates to the token endpoint",
        },
        variantValues: {
          client_secret_basic: { configurationFields: [], hidesConfigurationFields: [] },
          client_secret_post: { configurationFields: [], hidesConfigurationFields: [] },
          private_key_jwt: { configurationFields: ["client.jwks"], hidesConfigurationFields: [] },
        },
      },
      response_type: {
        variantInfo: { displayName: "Response Type", description: "OAuth 2.0 response type" },
        variantValues: {
          code: { configurationFields: [], hidesConfigurationFields: [] },
        },
      },
      server_metadata: {
        variantInfo: {
          displayName: "Server Metadata",
          description: "How server metadata is obtained",
        },
        variantValues: {
          discovery: { configurationFields: [], hidesConfigurationFields: [] },
          static: { configurationFields: ["server.jwks_uri"], hidesConfigurationFields: [] },
        },
      },
    },
  },
  {
    planName: "fapi2-security-profile-final-test-plan",
    displayName: "FAPI 2.0 Security Profile",
    profile: "fapi2-security-profile",
    specFamily: "FAPI",
    specVersion: "Final",
    summary: "FAPI 2.0 Security Profile conformance test plan",
    modules: [
      { testModule: "fapi2-security-profile-happy-flow" },
      { testModule: "fapi2-security-profile-ensure-signed-request" },
    ],
    configurationFields: ["server.issuer", "client.client_id", "client.jwks", "mtls.cert"],
    hidesConfigurationFields: [],
    variants: {
      client_auth_type: {
        variantInfo: {
          displayName: "Client Authentication Type",
          description: "How the client authenticates to the token endpoint",
        },
        variantValues: {
          private_key_jwt: { configurationFields: ["client.jwks"], hidesConfigurationFields: [] },
          mtls: { configurationFields: ["mtls.cert", "mtls.key"], hidesConfigurationFields: [] },
        },
      },
      fapi_response_mode: {
        variantInfo: { displayName: "FAPI Response Mode", description: "Response mode for FAPI" },
        variantValues: {
          plain_response: { configurationFields: [], hidesConfigurationFields: [] },
          jarm: { configurationFields: [], hidesConfigurationFields: [] },
        },
      },
    },
  },
];

/** A plan with no variants — simpler cascade for submission tests */
export const MOCK_PLAN_NO_VARIANTS = {
  planName: "oidcc-client-basic-certification-test-plan",
  displayName: "OpenID Connect Client: Basic Certification",
  profile: "client-basic",
  specFamily: "OIDCC",
  specVersion: "Final",
  summary: "Client-side certification test plan for OpenID Connect",
  modules: [{ testModule: "oidcc-client-test" }],
  configurationFields: ["server.issuer"],
  variants: {},
};

/** Mock /api/plan list response shape (for plans.html DataTable). */

const NOW = Date.now();
const DAY_MS = 86400000;

// The real `/api/plan` listing serializes `Plan.Module`, which carries only
// `testModule` and `instances` — never `status`/`result`. Those are fetched
// per-module from `/api/info/<instance>` (see MOCK_PLAN_INFO below). Keeping
// this fixture faithful to the backend shape is what makes the status-dot
// e2e assertions test reality, not a shape the server never returns.
export const MOCK_PLAN_LIST = [
  {
    _id: "plan-001",
    planName: "oidcc-basic-certification-test-plan",
    description: "OpenID Connect Core: Basic Certification Profile",
    variant: {
      client_auth_type: "client_secret_basic",
      response_type: "code",
    },
    started: new Date(NOW - 2 * DAY_MS).toISOString(),
    owner: { sub: "12345", iss: "https://accounts.google.com" },
    modules: [
      { testModule: "oidcc-server", instances: ["inst-001"] },
      { testModule: "oidcc-server-rotate-keys", instances: ["inst-002"] },
      // Never run — empty instances. Static skip dot, no /api/info fetch.
      { testModule: "oidcc-codereuse", instances: [] },
    ],
    config: { "server.issuer": "https://op.example.com" },
    publish: null,
    immutable: false,
  },
  {
    _id: "plan-002",
    planName: "fapi2-security-profile-final-test-plan",
    description: "FAPI 2.0 Security Profile",
    variant: {
      client_auth_type: "private_key_jwt",
      fapi_response_mode: "plain_response",
    },
    started: new Date(NOW - DAY_MS).toISOString(),
    owner: { sub: "12345", iss: "https://accounts.google.com" },
    modules: [{ testModule: "fapi2-security-profile-happy-flow", instances: ["inst-003"] }],
    config: { "server.issuer": "https://fapi.example.com" },
    publish: "summary",
    immutable: false,
  },
  {
    // Created without saved configuration — DBTestPlanService persists
    // `config` as an empty `org.bson.Document`, so the wire payload is
    // `config: {}`. Used to exercise the "no Config button" branch in
    // cts-plan-list and the corresponding plans.html e2e assertion.
    _id: "plan-003",
    planName: "oidcc-implicit-certification-test-plan",
    description: "OpenID Connect Core: Implicit Certification Profile",
    variant: {
      response_type: "id_token",
    },
    started: new Date(NOW - 5 * DAY_MS).toISOString(),
    owner: { sub: "admin-001", iss: "https://accounts.google.com" },
    modules: [{ testModule: "oidcc-server-implicit", instances: ["inst-005"] }],
    config: {},
    publish: "everything",
    immutable: true,
  },
];

// Per-instance `/api/info/<instance>` payloads for the listing's modules.
// Mirrors what the backend returns when the plans listing resolves each
// module's latest run. The plans spec registers an instance-keyed
// `/api/info` route from this map so the module status dots resolve to
// distinct colors (pass / warn) rather than staying gray.
export const MOCK_PLAN_INFO = {
  "inst-001": { status: "FINISHED", result: "PASSED" },
  "inst-002": { status: "FINISHED", result: "WARNING" },
  "inst-003": { status: "FINISHED", result: "PASSED" },
  "inst-005": { status: "FINISHED", result: "PASSED" },
};
