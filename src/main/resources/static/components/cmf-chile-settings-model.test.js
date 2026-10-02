import { describe, it, expect } from "vitest";
import {
  draftFromView,
  errorFor,
  isExpired,
  newEntryDraft,
  requestFromDraft,
} from "./cmf-chile-settings-model.js";

const VIEW = {
  directoryTokenEndpoint: "https://directory.example.cl/token",
  softwareStatementEndpoint: "https://directory.example.cl/software-statement",
  clientId: "oidf-conformance",
  clientSecretSet: true,
  clientJwks: { set: true, keys: [{ kid: "sig-1", kty: "RSA", alg: "PS256", use: "sig" }] },
  positiveCertificates: [
    {
      id: "pos-1",
      label: "primary",
      certificateChainPem: "CERT-P",
      subject: "CN=positive",
      issuer: "CN=ca",
      notAfter: "2027-01-01T00:00:00Z",
      privateKeySet: true,
    },
  ],
  negativeCertificates: [],
};

describe("draftFromView", () => {
  it("copies plain fields and leaves every secret input empty", () => {
    const draft = draftFromView(VIEW);

    expect(draft.directoryTokenEndpoint).toBe("https://directory.example.cl/token");
    expect(draft.softwareStatementEndpoint).toBe("https://directory.example.cl/software-statement");
    expect(draft.clientId).toBe("oidf-conformance");
    expect(draft.clientSecret).toBe("");
    expect(draft.clearClientSecret).toBe(false);
    expect(draft.replaceJwks).toBe(false);
    expect(draft.clearClientJwks).toBe(false);
    expect(draft.clientJwksText).toBe("");
    expect(draft.positiveCertificates[0]).toEqual({
      id: "pos-1",
      label: "primary",
      certificateChainPem: "CERT-P",
      privateKeyPem: "",
      replaceKey: false,
      stored: {
        subject: "CN=positive",
        issuer: "CN=ca",
        notAfter: "2027-01-01T00:00:00Z",
        privateKeySet: true,
      },
    });
  });

  it("treats absent fields of an empty section as blank", () => {
    const draft = draftFromView({ clientSecretSet: false });

    expect(draft.directoryTokenEndpoint).toBe("");
    expect(draft.softwareStatementEndpoint).toBe("");
    expect(draft.clientId).toBe("");
    expect(draft.positiveCertificates).toEqual([]);
    expect(draft.negativeCertificates).toEqual([]);
  });
});

describe("requestFromDraft", () => {
  it("sends plain fields and entry ids but no secret that was not typed", () => {
    const { body, errors } = requestFromDraft(draftFromView(VIEW));

    expect(errors).toEqual([]);
    expect(body).toEqual({
      directoryTokenEndpoint: "https://directory.example.cl/token",
      softwareStatementEndpoint: "https://directory.example.cl/software-statement",
      clientId: "oidf-conformance",
      positiveCertificates: [{ id: "pos-1", label: "primary", certificateChainPem: "CERT-P" }],
      negativeCertificates: [],
    });
  });

  it("sends a typed secret, a replaced key and a new entry's key", () => {
    const draft = draftFromView(VIEW);
    draft.clientSecret = "new-secret";
    draft.positiveCertificates[0].privateKeyPem = "KEY-P2";
    draft.negativeCertificates.push({
      ...newEntryDraft(),
      label: " expired ",
      certificateChainPem: "CERT-N",
      privateKeyPem: "KEY-N",
    });

    const { body } = requestFromDraft(draft);

    expect(body.clientSecret).toBe("new-secret");
    expect(body.positiveCertificates[0].privateKeyPem).toBe("KEY-P2");
    expect(body.negativeCertificates).toEqual([
      { label: "expired", certificateChainPem: "CERT-N", privateKeyPem: "KEY-N" },
    ]);
  });

  it("sends the clear flag instead of any typed secret", () => {
    const draft = draftFromView(VIEW);
    draft.clientSecret = "typed-then-cleared";
    draft.clearClientSecret = true;

    const { body } = requestFromDraft(draft);

    expect(body.clearClientSecret).toBe(true);
    expect(body).not.toHaveProperty("clientSecret");
  });

  it("sends the clear flag instead of any replacement JWKS", () => {
    const draft = draftFromView(VIEW);
    draft.replaceJwks = true;
    draft.clientJwksText = '{"keys": [{"kty": "RSA", "kid": "k2"}]}';
    draft.clearClientJwks = true;

    const { body, errors } = requestFromDraft(draft);

    expect(errors).toEqual([]);
    expect(body.clearClientJwks).toBe(true);
    expect(body).not.toHaveProperty("clientJwks");
  });

  it("sends a replacement JWKS as an object", () => {
    const draft = draftFromView(VIEW);
    draft.replaceJwks = true;
    draft.clientJwksText = '{"keys": [{"kty": "RSA", "kid": "k2"}]}';

    const { body, errors } = requestFromDraft(draft);

    expect(errors).toEqual([]);
    expect(body.clientJwks).toEqual({ keys: [{ kty: "RSA", kid: "k2" }] });
  });

  it("does not send an empty replacement JWKS", () => {
    const draft = draftFromView(VIEW);
    draft.replaceJwks = true;
    draft.clientJwksText = "   ";

    expect(requestFromDraft(draft).body).not.toHaveProperty("clientJwks");
  });

  it.each([["not json"], ["[1, 2]"], ["null"], ['"text"']])(
    "reports a JWKS of %s as a field error",
    (text) => {
      const draft = draftFromView(VIEW);
      draft.replaceJwks = true;
      draft.clientJwksText = text;

      expect(requestFromDraft(draft).errors).toEqual([
        {
          field: "clientJwks",
          message: "'Client JWKS' must be a JSON object",
        },
      ]);
    },
  );
});

describe("errorFor", () => {
  it("finds the message for a field, or an empty string", () => {
    const errors = [{ field: "clientId", message: "bad" }];

    expect(errorFor(errors, "clientId")).toBe("bad");
    expect(errorFor(errors, "clientSecret")).toBe("");
  });
});

describe("isExpired", () => {
  const now = Date.parse("2026-09-30T00:00:00Z");

  it("is true for a date in the past", () => {
    expect(isExpired("2026-01-01T00:00:00Z", now)).toBe(true);
  });

  it("is false for a date in the future or no date", () => {
    expect(isExpired("2027-01-01T00:00:00Z", now)).toBe(false);
    expect(isExpired(null, now)).toBe(false);
  });
});
