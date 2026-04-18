/**
 * Mock data matching the /api/runner/available response shape.
 * Each plan has: planName, displayName, profile, specFamily,
 * specVersion, modules, configurationFields, variants, summary.
 *
 * Also includes /api/plan response shape for plan listing
 * (paginated, with owner, status, variant, started date).
 */

// --- Available plans (GET /api/runner/available) ---

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
    configurationFields: ["server.issuer", "client.client_id", "client.client_secret"],
    variants: {
      client_auth_type: ["client_secret_basic", "client_secret_post", "private_key_jwt"],
      response_type: ["code"],
      server_metadata: ["discovery", "static"],
    },
  },
  {
    planName: "oidcc-implicit-certification-test-plan",
    displayName: "OpenID Connect Core: Implicit Certification Profile",
    profile: "implicit",
    specFamily: "OIDCC",
    specVersion: "Final",
    entityUnderTest: "OP",
    summary: "Implicit OP certification test plan for OpenID Connect Core 1.0",
    modules: [
      { testModule: "oidcc-server-implicit" },
      { testModule: "oidcc-ensure-redirect-uri-in-authorization-request" },
    ],
    configurationFields: ["server.issuer", "client.client_id"],
    variants: {
      response_type: ["id_token", "id_token token"],
      server_metadata: ["discovery", "static"],
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
      { testModule: "fapi2-security-profile-ensure-dpop" },
    ],
    configurationFields: ["server.issuer", "client.client_id", "client.jwks", "mtls.cert"],
    variants: {
      client_auth_type: ["private_key_jwt", "mtls"],
      fapi_response_mode: ["plain_response", "jarm"],
    },
  },
  {
    planName: "fapi-ciba-test-plan",
    displayName: "FAPI-CIBA: Client Initiated Backchannel Authentication",
    profile: "fapi-ciba",
    specFamily: "FAPI-CIBA",
    specVersion: "Final",
    entityUnderTest: "OP",
    summary: "CIBA profile test plan for financial-grade APIs",
    modules: [{ testModule: "fapi-ciba-happy-flow" }],
    configurationFields: ["server.issuer", "client.client_id", "client.backchannel_endpoint"],
    variants: {
      ciba_mode: ["poll", "ping"],
    },
  },
  {
    planName: "oidcc-client-basic-certification-test-plan",
    displayName: "OpenID Connect Client: Basic Certification",
    profile: "client-basic",
    specFamily: "OIDCC",
    specVersion: "Final",
    entityUnderTest: "RP",
    summary: "Client-side certification test plan for OpenID Connect",
    modules: [
      { testModule: "oidcc-client-test" },
      { testModule: "oidcc-client-test-signing-algorithms" },
    ],
    configurationFields: ["server.issuer"],
    variants: {},
  },
  {
    planName: "ssf-transmitter-test-plan",
    displayName: "Shared Signals Framework: Transmitter",
    profile: "ssf-transmitter",
    specFamily: "SSF",
    specVersion: "Draft",
    entityUnderTest: "Transmitter",
    summary: "SSF Transmitter conformance test plan",
    modules: [{ testModule: "ssf-transmitter-happy-flow" }],
    configurationFields: ["server.issuer", "server.ssf_endpoint"],
    variants: {},
  },
];

// --- Plan list (GET /api/plan) ---

const NOW = Date.now();
const DAY_MS = 86400000;

export const MOCK_PLAN_LIST = [
  {
    _id: "plan-001",
    planName: "oidcc-basic-certification-test-plan",
    description: "OpenID Connect Core: Basic Certification Profile",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    started: new Date(NOW - 2 * DAY_MS).toISOString(),
    owner: { sub: "12345", iss: "https://accounts.google.com" },
    modules: [
      { testModule: "oidcc-server", instances: ["inst-001"], status: "FINISHED", result: "PASSED" },
      {
        testModule: "oidcc-server-rotate-keys",
        instances: ["inst-002"],
        status: "FINISHED",
        result: "WARNING",
      },
      { testModule: "oidcc-codereuse", instances: [], status: null, result: null },
    ],
    config: { "server.issuer": "https://op.example.com" },
    publish: null,
    immutable: false,
  },
  {
    _id: "plan-002",
    planName: "fapi2-security-profile-final-test-plan",
    description: "FAPI 2.0 Security Profile",
    variant: { client_auth_type: "private_key_jwt", fapi_response_mode: "plain_response" },
    started: new Date(NOW - DAY_MS).toISOString(),
    owner: { sub: "12345", iss: "https://accounts.google.com" },
    modules: [
      {
        testModule: "fapi2-security-profile-happy-flow",
        instances: ["inst-003"],
        status: "FINISHED",
        result: "PASSED",
      },
      {
        testModule: "fapi2-security-profile-ensure-signed-request",
        instances: ["inst-004"],
        status: "FINISHED",
        result: "FAILED",
      },
    ],
    config: { "server.issuer": "https://fapi.example.com" },
    publish: "summary",
    immutable: false,
  },
  {
    _id: "plan-003",
    planName: "oidcc-implicit-certification-test-plan",
    description: "OpenID Connect Core: Implicit Certification Profile",
    variant: { response_type: "id_token" },
    started: new Date(NOW - 5 * DAY_MS).toISOString(),
    owner: { sub: "admin-001", iss: "https://accounts.google.com" },
    modules: [
      {
        testModule: "oidcc-server-implicit",
        instances: ["inst-005"],
        status: "FINISHED",
        result: "PASSED",
      },
    ],
    config: { "server.issuer": "https://implicit.example.com" },
    publish: "everything",
    immutable: true,
  },
];
