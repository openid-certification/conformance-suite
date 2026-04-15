/** Mock /api/plan/available response shape (spec cascade data). */

export const MOCK_PLANS = [
  {
    planName: "oidcc-basic-certification-test-plan",
    displayName: "OpenID Connect Core: Basic Certification Profile",
    profile: "basic",
    specFamily: "OIDCC",
    specVersion: "Final",
    entityUnderTest: "OP",
    summary: "Basic OP certification test plan for OpenID Connect Core 1.0",
    modules: [
      { testModule: "oidcc-server" },
      { testModule: "oidcc-server-rotate-keys" },
      { testModule: "oidcc-ensure-redirect-uri-in-authorization-request" },
      { testModule: "oidcc-codereuse" },
    ],
    configurationFields: [
      "server.issuer",
      "client.client_id",
      "client.client_secret",
    ],
    hidesConfigurationFields: [],
    variants: {
      client_auth_type: {
        variantInfo: { displayName: "Client Authentication Type", description: "How the client authenticates to the token endpoint" },
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
        variantInfo: { displayName: "Server Metadata", description: "How server metadata is obtained" },
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
    entityUnderTest: "OP",
    summary: "FAPI 2.0 Security Profile conformance test plan",
    modules: [
      { testModule: "fapi2-security-profile-happy-flow" },
      { testModule: "fapi2-security-profile-ensure-signed-request" },
    ],
    configurationFields: [
      "server.issuer",
      "client.client_id",
      "client.jwks",
      "mtls.cert",
    ],
    hidesConfigurationFields: [],
    variants: {
      client_auth_type: {
        variantInfo: { displayName: "Client Authentication Type", description: "How the client authenticates to the token endpoint" },
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
  entityUnderTest: "RP",
  summary: "Client-side certification test plan for OpenID Connect",
  modules: [{ testModule: "oidcc-client-test" }],
  configurationFields: ["server.issuer"],
  variants: {},
};

/** Mock /api/plan list response shape (for plans.html DataTable). */

const NOW = Date.now();
const DAY_MS = 86400000;

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
      {
        testModule: "oidcc-server",
        instances: ["inst-001"],
        status: "FINISHED",
        result: "PASSED",
      },
      {
        testModule: "oidcc-server-rotate-keys",
        instances: ["inst-002"],
        status: "FINISHED",
        result: "WARNING",
      },
      {
        testModule: "oidcc-codereuse",
        instances: [],
        status: null,
        result: null,
      },
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
    modules: [
      {
        testModule: "fapi2-security-profile-happy-flow",
        instances: ["inst-003"],
        status: "FINISHED",
        result: "PASSED",
      },
    ],
    config: { "server.issuer": "https://fapi.example.com" },
    publish: "summary",
    immutable: false,
  },
];
