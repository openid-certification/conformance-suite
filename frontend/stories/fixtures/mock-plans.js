/**
 * Mock data matching the /api/plan/available response shape.
 * Each plan has: planName, displayName, profile, specFamily,
 * specVersion, modules, configurationFields, variants, summary.
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
  },
  {
    planName: "oidcc-implicit-certification-test-plan",
    displayName: "OpenID Connect Core: Implicit Certification Profile",
    profile: "implicit",
    specFamily: "OIDCC",
    specVersion: "Final",
    summary: "Implicit OP certification test plan for OpenID Connect Core 1.0",
    modules: [
      { testModule: "oidcc-server-implicit" },
      { testModule: "oidcc-ensure-redirect-uri-in-authorization-request" },
    ],
    configurationFields: ["server.issuer", "client.client_id"],
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
      { testModule: "fapi2-security-profile-ensure-dpop" },
    ],
    configurationFields: ["server.issuer", "client.client_id", "client.jwks", "mtls.cert"],
  },
  {
    planName: "fapi-ciba-test-plan",
    displayName: "FAPI-CIBA: Client Initiated Backchannel Authentication",
    profile: "fapi-ciba",
    specFamily: "FAPI-CIBA",
    specVersion: "Final",
    summary: "CIBA profile test plan for financial-grade APIs",
    modules: [{ testModule: "fapi-ciba-happy-flow" }],
    configurationFields: ["server.issuer", "client.client_id", "client.backchannel_endpoint"],
  },
  {
    planName: "oidcc-client-basic-certification-test-plan",
    displayName: "OpenID Connect Client: Basic Certification",
    profile: "client-basic",
    specFamily: "OIDCC",
    specVersion: "Final",
    summary: "Client-side certification test plan for OpenID Connect",
    modules: [
      { testModule: "oidcc-client-test" },
      { testModule: "oidcc-client-test-signing-algorithms" },
    ],
    configurationFields: ["server.issuer"],
  },
];
