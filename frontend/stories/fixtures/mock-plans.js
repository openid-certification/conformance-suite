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

/**
 * One spec family spanning two entities and several versions, shaped like the
 * real OID4VP plans, for the selector's in-family grouping: entity headings
 * from `profile`, rows named by a `shortName` that leads with the version,
 * and a status badge.
 *
 * TWIN: `frontend/e2e/fixtures/mock-plans.js` carries the same plans and is kept in sync BY HAND.
 */
export const MOCK_GROUPED_PLANS = /** @type {Array<[string, string, string, boolean]>} */ ([
  ["oid4vp-1final-verifier-haip-test-plan", "Test a OpenID4VP Verifier", "1.0 Final", true],
  ["oid4vp-1final-verifier-test-plan", "Test a OpenID4VP Verifier", "1.0 Final", false],
  ["oid4vp-id2-verifier-test-plan", "Test a OpenID4VP Verifier", "ID2", false],
  ["oid4vp-1final-wallet-haip-test-plan", "Test a OpenID4VP wallet", "1.0 Final", true],
]).map(([planName, profile, specVersion, haip]) => {
  const shortName = haip ? `${specVersion} + HAIP` : specVersion;
  const entity = profile.endsWith("wallet") ? "wallet" : "verifier";
  return {
    planName,
    displayName: haip
      ? `OpenID for Verifiable Presentations ${specVersion}/HAIP: Test a ${entity}`
      : `OpenID for Verifiable Presentations ${specVersion}: Test a ${entity} - alpha tests ` +
        `(not part of certification program - use the HAIP ${entity} plan to certify)`,
    shortName,
    certifiable: haip,
    alpha: !haip,
    profile,
    specFamily: "OID4VP",
    specVersion,
    summary: "",
    modules: [{ testModule: `${planName.replace(/-test-plan$/, "")}-happy-flow` }],
    configurationFields: [],
    hidesConfigurationFields: [],
    variants: {},
  };
});

// --- Plan list (GET /api/plan) ---

const NOW = Date.now();
const DAY_MS = 86400000;

// The real `/api/plan` listing serializes `Plan.Module`, which carries only
// `testModule` and `instances` — never `status`/`result`. Those are fetched
// per card from `/api/plan/<id>` (see MOCK_PLAN_INFO below and the
// cts-plan-list status-dot resolution). Keeping this fixture faithful to the
// backend shape is what makes the dot-resolution stories test reality rather
// than a shape the server never returns.
export const MOCK_PLAN_LIST = [
  {
    _id: "plan-001",
    planName: "oidcc-basic-certification-test-plan",
    description: "OpenID Connect Core: Basic Certification Profile",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    started: new Date(NOW - 2 * DAY_MS).toISOString(),
    owner: { sub: "12345", iss: "https://accounts.google.com" },
    modules: [
      { testModule: "oidcc-server", instances: ["inst-001"] },
      { testModule: "oidcc-server-rotate-keys", instances: ["inst-002"] },
      // Never run — empty instances. Renders a static (not pulsing) skip dot
      // and triggers no status fetch.
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
    variant: { client_auth_type: "private_key_jwt", fapi_response_mode: "plain_response" },
    started: new Date(NOW - DAY_MS).toISOString(),
    owner: { sub: "12345", iss: "https://accounts.google.com" },
    modules: [
      { testModule: "fapi2-security-profile-happy-flow", instances: ["inst-003"] },
      { testModule: "fapi2-security-profile-ensure-signed-request", instances: ["inst-004"] },
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
    modules: [{ testModule: "oidcc-server-implicit", instances: ["inst-005"] }],
    config: { "server.issuer": "https://implicit.example.com" },
    publish: "everything",
    immutable: true,
  },
];

// Latest-run `{ status, result }` per instance for the listing's modules, as
// `/api/plan/<id>` attaches them to that plan's modules.
// Mirrors what the backend returns when the plans listing resolves each
// module's latest run. Stories/specs register an instance-keyed `/api/info`
// handler from this map so the status dots resolve to distinct colors
// (pass / warn / fail) rather than a single shared status.
export const MOCK_PLAN_INFO = {
  "inst-001": { status: "FINISHED", result: "PASSED" },
  "inst-002": { status: "FINISHED", result: "WARNING" },
  "inst-003": { status: "FINISHED", result: "PASSED" },
  "inst-004": { status: "INTERRUPTED", result: "FAILED" },
  "inst-005": { status: "FINISHED", result: "PASSED" },
};
