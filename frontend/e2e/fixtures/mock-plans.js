/**
 * Mock /api/plan/available response shape.
 *
 * The fields below mirror what `TestPlanApi.getAvailableTestPlans()` returns:
 * planName, displayName, shortName, certifiable, alpha, profile, specFamily,
 * specVersion, modules, configurationFields, hidesConfigurationFields, summary,
 * variants. The entries here predate shortName / certifiable / alpha and leave
 * them out, which the selector treats as "use displayName, certifiable, not
 * alpha"; MOCK_GROUPED_PLANS carries them.
 *
 * cts-test-selector (the sole plan-entry point) lists plans by `planName` /
 * `displayName` and filters by `specFamily`; the other fields are retained to
 * mirror the real payload. Some `profile` values match guided-tree leaves.
 *
 * `variants` keys are listed in the order the backend emits them (by
 * `@VariantParameter.sortOrder`, ties alphabetical by name) — the UI renders
 * them in payload order, so fixture order is what the e2e-rendered dropdown
 * order reflects. Keep new entries sorted the same way.
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
      response_type: {
        variantInfo: { displayName: "Response Type", description: "OAuth 2.0 response type" },
        variantValues: {
          code: { configurationFields: [], hidesConfigurationFields: [] },
        },
      },
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
 *   SAMA v2 resolution target, and the Chile → OP leaf (mTLS + Grant Management).
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
      fapi_profile: {
        variantInfo: {
          displayName: "FAPI Profile",
          description: "The FAPI sub-profile to use.",
        },
        variantValues: {
          plain_fapi: { configurationFields: [] },
          ksa: { configurationFields: [] },
          connectid_au: { configurationFields: [] },
          consumerdataright_au: { configurationFields: [] },
          openbanking_chile: { configurationFields: ["mtls.cert", "mtls.key"] },
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
        // mirrors @VariantNotApplicableWhen: AU-CDR permits a single combination
        notApplicableWhen: { fapi_profile: { consumerdataright_au: ["rar"] } },
      },
      client_auth_type: {
        variantInfo: {
          displayName: "Client Authentication Type",
          description: "The type of client authentication your software supports.",
        },
        variantValues: {
          private_key_jwt: { configurationFields: [] },
          mtls: { configurationFields: ["mtls.cert", "mtls.key"] },
        },
        // mirrors @VariantNotApplicableWhen: AU-CDR permits a single combination
        notApplicableWhen: { fapi_profile: { consumerdataright_au: ["mtls"] } },
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
        // mirrors @VariantNotApplicableWhen: AU-CDR permits a single combination
        notApplicableWhen: { fapi_profile: { consumerdataright_au: ["unsigned"] } },
      },
      grant_management: {
        variantInfo: {
          displayName: "Grant Management",
          description: "Whether Grant Management support is required.",
        },
        variantValues: {
          disabled: { configurationFields: [] },
          enabled: { configurationFields: [] },
        },
        // Mirrors the @VariantNotApplicableWhen pair on the FAPI2 bases: grant management is forced on
        // for Chile and unavailable for the other non-generic profiles, so in both cases the dropdown
        // is left with a single value and should not be shown at all.
        notApplicableWhen: {
          fapi_profile: {
            openbanking_chile: ["disabled"],
            ksa: ["enabled"],
            connectid_au: ["enabled"],
          },
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
        // mirrors @VariantNotApplicableWhen: AU-CDR permits a single combination
        notApplicableWhen: { fapi_profile: { consumerdataright_au: ["plain_response"] } },
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
        // mirrors @VariantNotApplicableWhen: AU-CDR permits a single combination
        notApplicableWhen: { fapi_profile: { consumerdataright_au: ["plain_oauth"] } },
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
        // mirrors @VariantNotApplicableWhen: AU-CDR permits a single combination
        notApplicableWhen: { fapi_profile: { consumerdataright_au: ["dpop"] } },
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
          openbanking_brazil: {
            configurationFields: [
              "resource.brazilCpf",
              "resource.brazilCnpj",
              "resource.brazilLoggedUserName",
            ],
          },
          connectid_au: { configurationFields: [] },
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
      fapi_profile: {
        variantInfo: {
          displayName: "FAPI Profile",
          description: "The FAPI sub-profile to use.",
        },
        variantValues: {
          plain_fapi: { configurationFields: [] },
          openbanking_brazil: { configurationFields: ["resource.consentUrl"] },
          openinsurance_brazil: { configurationFields: ["resource.consentUrl"] },
        },
      },
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
      fapi_profile: {
        variantInfo: {
          displayName: "FAPI Profile",
          description: "The FAPI sub-profile to use.",
        },
        variantValues: {
          plain_fapi: { configurationFields: [] },
          openbanking_brazil: { configurationFields: ["resource.consentUrl"] },
          openinsurance_brazil: { configurationFields: ["resource.consentUrl"] },
        },
      },
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

/**
 * One spec family spanning two entities and several versions, shaped like the
 * real OID4VP plans, for the selector's in-family grouping: entity headings
 * from `profile`, rows named by a `shortName` that leads with the version,
 * and a status badge.
 *
 * TWIN: `frontend/stories/fixtures/mock-plans.js` carries the same plans and is kept in sync BY HAND.
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
      : `OpenID for Verifiable Presentations ${specVersion}: Test a ${entity} ` +
        `(alpha version - not part of certification program - use the HAIP ${entity} plan to certify)`,
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
// per card from `/api/plan/<id>` (see MOCK_PLAN_INFO below). Keeping
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
      // Never run — empty instances. Static skip dot, no status fetch.
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

// Latest-run `{ status, result }` per instance for the listing's modules, as
// `/api/plan/<id>` attaches them to that plan's modules.
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

/**
 * What `GET /api/plan/filter-options` answers: the families and plan names the
 * listing's Family and Plan controls offer. Includes a retired name, because
 * most of what an old listing is made of is names the suite no longer
 * publishes, and the control marks them.
 */
export const MOCK_PLAN_FILTER_OPTIONS = {
  families: ["FAPI-CIBA", "OpenID Connect Core"],
  plans: [
    { name: "fapi-ciba-id1-test-plan", family: "FAPI-CIBA", retired: false },
    { name: "fapi-ciba-test-plan", family: "FAPI-CIBA", retired: true },
    { name: "oidcc-basic-certification-test-plan", family: "OpenID Connect Core", retired: false },
  ],
  // per plan, because a variant parameter means nothing without the plan that
  // defines it; a retired plan has no entry, since the registry is the source
  variants: {
    "fapi-ciba-id1-test-plan": {
      ciba_mode: ["ping", "poll"],
      client_auth_type: ["mtls", "private_key_jwt"],
    },
    "oidcc-basic-certification-test-plan": {
      server_metadata: ["discovery", "static"],
    },
  },
};

/**
 * Variant entry helper for the EU fixtures: every value carries no extra fields.
 * @param {string} displayName
 * @param {string} description
 * @param {string[]} values
 */
const euVariant = (displayName, description, values) => ({
  variantInfo: { displayName, description },
  variantValues: Object.fromEntries(values.map((v) => [v, { configurationFields: [] }])),
});

const VP_CREDENTIAL_FORMAT = euVariant(
  "Credential Format",
  "The credential format that will be returned by the Wallet.",
  ["sd_jwt_vc", "iso_mdl"],
);
const VCI_CREDENTIAL_FORMAT = euVariant(
  "Credential Format",
  "The credential format that will be returned to the Wallet.",
  ["sd_jwt_vc", "mdoc"],
);
/**
 * Mirrors @VariantHidesConfigurationFields on AbstractVCIWalletTest / AbstractVCIIssuerTestModule:
 * the credential offer endpoint is only used by issuer-initiated flows.
 */
const VCI_FLOW_VARIANT = {
  variantInfo: {
    displayName: "Authorization Code Flow Variant",
    description: "VCI Authorization Code Flow Variant to be used.",
  },
  variantValues: {
    wallet_initiated: {
      configurationFields: [],
      hidesConfigurationFields: ["vci.credential_offer_endpoint"],
    },
    issuer_initiated: { configurationFields: [] },
  },
};

/** The wallet plan's credential offer parameter, applicable to issuer-initiated flows. */
const VCI_OFFER_VARIANT = euVariant(
  "Credential Offer Variant",
  "VCI Credential Offer Parameter Variant to be used.",
  ["by_value", "by_reference"],
);

/**
 * The four HAIP plans the guided wizard's EU journeys resolve to (#2005). Kept out of
 * MOCK_GUIDED_PLANS so the advanced-mode specs' catalog stays unchanged; the EU journeys pass
 * these to setupScheduleTestRoutes via `plans`.
 */
export const MOCK_EU_PLANS = [
  {
    planName: "oid4vp-1final-wallet-haip-test-plan",
    displayName: "OpenID for Verifiable Presentations 1.0 Final/HAIP: Test a wallet",
    profile: "Test a wallet",
    specFamily: "OID4VP",
    specVersion: "1.0 Final",
    summary: "",
    modules: [{ testModule: "oid4vp-1final-wallet-happy-flow", configurationFields: [] }],
    variants: {
      credential_format: VP_CREDENTIAL_FORMAT,
      credential_type: euVariant(
        "Credential Type",
        "The credential the wallet will be asked to present.",
        ["eudi_pid", "mdl", "photoid", "custom"],
      ),
      response_mode: euVariant("Response Mode", "The response_mode that will be used.", [
        "direct_post.jwt",
        "dc_api.jwt",
      ]),
    },
  },
  {
    planName: "oid4vp-1final-verifier-haip-test-plan",
    displayName: "OpenID for Verifiable Presentations 1.0 Final/HAIP: Test a verifier",
    profile: "Test a verifier",
    specFamily: "OID4VP",
    specVersion: "1.0 Final",
    summary: "",
    modules: [{ testModule: "oid4vp-1final-verifier-happy-flow", configurationFields: [] }],
    variants: {
      credential_format: VP_CREDENTIAL_FORMAT,
      response_mode: euVariant("Response Mode", "The response_mode that will be used.", [
        "direct_post.jwt",
      ]),
    },
  },
  {
    planName: "oid4vci-1_0-wallet-haip-test-plan",
    displayName: "OpenID for Verifiable Credential Issuance 1.0 Final/HAIP: Test a wallet",
    profile: "Test a wallet",
    specFamily: "OID4VCI",
    specVersion: "",
    summary: "",
    modules: [
      {
        testModule: "oid4vci-1_0-wallet-happy-path",
        configurationFields: ["vci.credential_configuration_id", "vci.credential_offer_endpoint"],
      },
    ],
    variants: {
      credential_format: VCI_CREDENTIAL_FORMAT,
      vci_authorization_code_flow_variant: VCI_FLOW_VARIANT,
      vci_credential_offer_variant: VCI_OFFER_VARIANT,
    },
  },
  {
    planName: "oid4vci-1_0-issuer-haip-test-plan",
    displayName: "OpenID for Verifiable Credential Issuance 1.0 Final/HAIP: Test an issuer",
    profile: "Test a credential issuer",
    specFamily: "OID4VCI",
    specVersion: "",
    summary: "",
    modules: [
      {
        testModule: "oid4vci-1_0-issuer-happy-flow",
        configurationFields: ["vci.credential_issuer_url", "vci.credential_configuration_id"],
      },
    ],
    variants: {
      credential_format: VCI_CREDENTIAL_FORMAT,
      vci_authorization_code_flow_variant: VCI_FLOW_VARIANT,
    },
  },
];
