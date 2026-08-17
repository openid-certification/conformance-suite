/**
 * Mock /api/plan/available response shape.
 *
 * The fields below mirror what `TestPlanApi.getAvailableTestPlans()` returns:
 * planName, displayName, profile, specFamily, specVersion, modules,
 * configurationFields, hidesConfigurationFields, summary, variants.
 *
 * cts-test-selector (the sole plan-entry point) lists plans by `planName` /
 * `displayName` and filters by `specFamily`; the other fields are retained to
 * mirror the real payload. Some `profile` values match guided-tree leaves.
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
  {
    planName: "fapi-ciba-id1-test-plan",
    displayName: "FAPI-CIBA-ID1: Authorization server test",
    profile: "fapi-ciba",
    specFamily: "FAPI-CIBA",
    specVersion: "ID1",
    entityUnderTest: "OP",
    summary: "FAPI-CIBA authorization server test plan",
    modules: [
      { testModule: "fapi-ciba-id1" },
      { testModule: "fapi-ciba-id1-connectid-ensure-authorization-request-with-purpose-succeeds" },
    ],
    configurationFields: [
      "server.discoveryUrl",
      "client.hint_type",
      "client.hint_value",
      "client.login_hint",
      "client.card_primary_account_number",
      "client.payment_amount",
      "client.payment_currency",
      "client.payment_beneficiary_name",
      "client.payment_desc",
    ],
    hidesConfigurationFields: [],
    variants: {
      fapi_ciba_profile: {
        variantInfo: { displayName: "FAPI-CIBA Profile", description: "Profile under test" },
        variantValues: {
          connectid_au: {
            configurationFields: [],
            hidesConfigurationFields: ["client.hint_type", "client.hint_value"],
          },
        },
      },
    },
  },
];

/**
 * Plans whose planNames match guided-wizard-tree.js leaves, shaped like the
 * real /api/plan/available payload. Used by schedule-test-guided.spec.js:
 * the guided journey resolves tree leaves against the live catalog (R4), so
 * the mocked catalog must carry these names for the happy paths — and omit
 * one of them to exercise the dead-end.
 *
 * - fapi2-message-signing-final-test-plan: KSA → OP → private_key_jwt →
 *   SAMA v2 resolution target.
 * - fapi-ciba-id1-client-test-plan / fapi-ciba-id1-test-plan: Brazil and
 *   ConnectID CIBA guided leaves.
 * - fapi1-advanced-final-test-plan: Brazil OP FAPI leaf (carries the
 *   also_required → DCR bundle).
 * - fapi1-advanced-final-brazil-dcr-test-plan: the bundle sibling.
 */
export const MOCK_GUIDED_PLANS = [
  {
    planName: "fapi2-message-signing-final-test-plan",
    displayName: "FAPI2-Message-Signing-Final: Authorization server test",
    profile: "Test an OpenID Provider / Authorization Server",
    specFamily: "FAPI2 Message Signing",
    specVersion: "Final",
    summary: "",
    modules: [
      {
        testModule: "fapi2-security-profile-final-happy-flow",
        configurationFields: ["server.discoveryUrl", "client.scope", "client.jwks"],
      },
    ],
    variants: {
      client_auth_type: {
        variantInfo: {
          displayName: "Client Authentication Type",
          description: "The type of client authentication your software supports.",
        },
        variantValues: {
          private_key_jwt: { configurationFields: [] },
          mtls: { configurationFields: ["mtls.cert", "mtls.key"] },
        },
      },
      sender_constrain: {
        variantInfo: {
          displayName: "Sender Constraining",
          description: "The method to use to sender constrain access tokens.",
        },
        variantValues: {
          mtls: { configurationFields: ["mtls.cert", "mtls.key"] },
          dpop: { configurationFields: [] },
        },
      },
      authorization_request_type: {
        variantInfo: {
          displayName: "Authorization Request Type",
          description: "The authorization request type to be used.",
        },
        variantValues: {
          simple: { configurationFields: [] },
          rar: { configurationFields: [] },
        },
      },
      openid: {
        variantInfo: {
          displayName: "Test OpenID",
          description: "If your server supports issuing id_tokens, pick 'openid connect'.",
        },
        variantValues: {
          plain_oauth: { configurationFields: [] },
          openid_connect: { configurationFields: [] },
        },
      },
      fapi_request_method: {
        variantInfo: {
          displayName: "Request Method",
          description: "The method to use to pass the request to the PAR endpoint.",
        },
        variantValues: {
          unsigned: { configurationFields: [] },
          signed_non_repudiation: { configurationFields: [] },
        },
      },
      fapi_profile: {
        variantInfo: {
          displayName: "FAPI Profile",
          description: "The FAPI sub-profile to use.",
        },
        variantValues: {
          plain_fapi: { configurationFields: [] },
          ksa: { configurationFields: [] },
          connectid_au: { configurationFields: [] },
        },
      },
      fapi_response_mode: {
        variantInfo: {
          displayName: "FAPI Response Mode",
          description: "The response mode that will be used.",
        },
        variantValues: {
          plain_response: { configurationFields: [] },
          jarm: { configurationFields: [] },
        },
      },
    },
  },
  {
    planName: "fapi-ciba-id1-client-test-plan",
    displayName: "FAPI-CIBA-ID1: Client test",
    profile: "Test a Client",
    specFamily: "FAPI-CIBA",
    specVersion: "ID1",
    summary: "",
    modules: [
      {
        testModule: "fapi-ciba-id1-client-happy-flow",
        configurationFields: ["client.client_id", "client.jwks", "server.issuer"],
      },
    ],
    variants: {
      client_auth_type: {
        variantInfo: {
          displayName: "Client Authentication Type",
          description: "The type of client authentication your software supports.",
        },
        variantValues: {
          private_key_jwt: { configurationFields: ["client.jwks"] },
          mtls: { configurationFields: ["mtls.cert", "mtls.key"] },
        },
      },
      ciba_mode: {
        variantInfo: {
          displayName: "CIBA Mode",
          description: "The CIBA notification mode to test.",
        },
        variantValues: {
          ping: { configurationFields: [] },
          poll: { configurationFields: [] },
        },
      },
      fapi_ciba_profile: {
        variantInfo: {
          displayName: "FAPI-CIBA Profile",
          description: "The FAPI-CIBA ecosystem profile to test.",
        },
        variantValues: {
          openbanking_brazil: { configurationFields: [] },
          connectid_au: { configurationFields: [] },
        },
      },
    },
  },
  {
    planName: "fapi-ciba-id1-test-plan",
    displayName: "FAPI-CIBA-ID1: Authorization server test",
    profile: "Test an OpenID Provider / Authorization Server",
    specFamily: "FAPI-CIBA",
    specVersion: "ID1",
    summary: "",
    modules: [
      {
        testModule: "fapi-ciba-id1-happy-flow",
        configurationFields: ["server.discoveryUrl", "client.client_id", "client.jwks"],
      },
    ],
    variants: {
      client_auth_type: {
        variantInfo: {
          displayName: "Client Authentication Type",
          description: "The type of client authentication your software supports.",
        },
        variantValues: {
          private_key_jwt: { configurationFields: ["client.jwks"] },
          mtls: { configurationFields: ["mtls.cert", "mtls.key"] },
        },
      },
      fapi_ciba_profile: {
        variantInfo: {
          displayName: "FAPI-CIBA Profile",
          description: "The FAPI-CIBA ecosystem profile to test.",
        },
        variantValues: {
          openbanking_brazil: { configurationFields: [] },
          connectid_au: { configurationFields: [] },
        },
      },
      ciba_mode: {
        variantInfo: {
          displayName: "CIBA Mode",
          description: "The CIBA notification mode to test.",
        },
        variantValues: {
          ping: { configurationFields: [] },
          poll: { configurationFields: [] },
        },
      },
      client_registration: {
        variantInfo: {
          displayName: "Client Registration",
          description: "How the client is registered.",
        },
        variantValues: {
          static_client: { configurationFields: [] },
          dynamic_client: { configurationFields: [] },
        },
      },
    },
  },
  {
    planName: "fapi1-advanced-final-test-plan",
    displayName: "FAPI1-Advanced-Final: Authorization server test",
    profile: "Test an OpenID Provider / Authorization Server",
    specFamily: "FAPI1 Advanced",
    specVersion: "",
    summary: "",
    modules: [
      {
        testModule: "fapi1-advanced-final",
        configurationFields: ["server.discoveryUrl", "client.scope", "client.jwks"],
      },
    ],
    variants: {
      client_auth_type: {
        variantInfo: {
          displayName: "Client Authentication Type",
          description: "The type of client authentication your software supports.",
        },
        variantValues: {
          private_key_jwt: { configurationFields: [] },
          mtls: { configurationFields: ["mtls.cert", "mtls.key"] },
        },
      },
      fapi_auth_request_method: {
        variantInfo: {
          displayName: "Request Object Method",
          description: "The method to use to pass the request object.",
        },
        variantValues: {
          by_value: { configurationFields: [] },
          pushed: { configurationFields: [] },
        },
      },
      fapi_profile: {
        variantInfo: {
          displayName: "FAPI Profile",
          description: "The FAPI sub-profile to use.",
        },
        variantValues: {
          plain_fapi: { configurationFields: [] },
          openbanking_brazil: { configurationFields: ["resource.consentUrl"] },
        },
      },
      fapi_response_mode: {
        variantInfo: {
          displayName: "FAPI Response Mode",
          description: "The response mode that will be used.",
        },
        variantValues: {
          plain_response: { configurationFields: [] },
          jarm: { configurationFields: [] },
        },
      },
    },
  },
  {
    planName: "fapi1-advanced-final-brazil-dcr-test-plan",
    displayName:
      "FAPI1-Advanced-Final: Brazil Dynamic Client Registration Authorization server test",
    profile: "Test an OpenID Provider / Authorization Server",
    specFamily: "FAPI1 Advanced",
    specVersion: "",
    summary: "",
    modules: [
      {
        testModule: "fapi1-advanced-final-brazildcr-happy-flow",
        configurationFields: ["server.discoveryUrl", "client.scope", "client.jwks"],
      },
    ],
    variants: {
      client_auth_type: {
        variantInfo: {
          displayName: "Client Authentication Type",
          description: "The type of client authentication your software supports.",
        },
        variantValues: {
          private_key_jwt: { configurationFields: [] },
          mtls: { configurationFields: ["mtls.cert", "mtls.key"] },
        },
      },
      fapi_auth_request_method: {
        variantInfo: {
          displayName: "Request Object Method",
          description: "The method to use to pass the request object.",
        },
        variantValues: {
          by_value: { configurationFields: [] },
          pushed: { configurationFields: [] },
        },
      },
      fapi_profile: {
        variantInfo: {
          displayName: "FAPI Profile",
          description: "The FAPI sub-profile to use.",
        },
        variantValues: {
          plain_fapi: { configurationFields: [] },
          openbanking_brazil: { configurationFields: ["resource.consentUrl"] },
        },
      },
      fapi_response_mode: {
        variantInfo: {
          displayName: "FAPI Response Mode",
          description: "The response mode that will be used.",
        },
        variantValues: {
          plain_response: { configurationFields: [] },
          jarm: { configurationFields: [] },
        },
      },
    },
  },
];

/** A plan with no variants — simpler selection for submission tests */
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
