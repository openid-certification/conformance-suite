/** Mock GET/PUT /api/admin/settings/cmf-chile response shapes. */

const CERTIFICATE_PEM =
  "-----BEGIN CERTIFICATE-----\nMIIBszCCAVmgAwIBAgIUQ29uZm9ybWFuY2U=\n-----END CERTIFICATE-----\n";

/** A fully configured section, as the redacted view reports it. */
export const MOCK_SETTINGS_CONFIGURED = {
  directoryTokenEndpoint: "https://directory.sandbox.example.cl/token",
  clientId: "oidf-conformance",
  clientSecretSet: true,
  clientJwks: { set: true, keys: [{ kid: "sig-1", kty: "RSA", alg: "PS256", use: "sig" }] },
  positiveCertificates: [
    {
      id: "pos-1",
      label: "primary",
      certificateChainPem: CERTIFICATE_PEM,
      subject: "CN=oidf-conformance,O=OpenID Foundation",
      issuer: "CN=Sandbox CA",
      notAfter: "2099-06-30T00:00:00Z",
      privateKeySet: true,
    },
  ],
  negativeCertificates: [
    {
      id: "neg-1",
      label: "expired",
      certificateChainPem: CERTIFICATE_PEM,
      subject: "CN=expired-client",
      issuer: "CN=Sandbox CA",
      notAfter: "2025-01-31T00:00:00Z",
      privateKeySet: true,
    },
  ],
  updatedAt: "2026-09-30T12:00:00Z",
  updatedBy: "Admin User",
};

/** A section an admin has never saved. */
export const MOCK_SETTINGS_EMPTY = {
  clientSecretSet: false,
  clientJwks: { set: false, keys: [] },
  positiveCertificates: [],
  negativeCertificates: [],
};

/** A 400 body refusing a key that does not match its certificate. */
export const MOCK_SETTINGS_KEY_MISMATCH = {
  errors: [
    {
      field: "positiveCertificates[0].privateKeyPem",
      message:
        "'Private key' of 'primary' in 'Positive DCR client certificates' does not match the first certificate in its chain",
    },
  ],
};
