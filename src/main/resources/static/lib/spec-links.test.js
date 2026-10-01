import { describe, expect, it } from "vitest";
import { resolveSpecLink } from "./spec-links.js";

const MAP = {
  "OIDCC-": "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.",
  "FAPI2-SP-FINAL-": "https://openid.net/specs/fapi-security-profile-2_0-final.html#section-",
  "HAIP-":
    "https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html#section-",
  "RFC7517-": "https://tools.ietf.org/html/rfc7517#section-",
  "CDR-": "https://consumerdatastandardsaustralia.github.io/standards/#",
  CBUAE: "https://example.com/Security+Profile+-+FAPI#",
};

describe("resolveSpecLink", () => {
  it("links a section-only requirement to that section", () => {
    expect(resolveSpecLink("RFC7517-1.1", MAP)).toBe(
      "https://tools.ietf.org/html/rfc7517#section-1.1",
    );
  });

  it("drops a trailing list-item number and links to the section", () => {
    expect(resolveSpecLink("FAPI2-SP-FINAL-5.3.2.2-5", MAP)).toBe(
      "https://openid.net/specs/fapi-security-profile-2_0-final.html#section-5.3.2.2",
    );
    expect(resolveSpecLink("OIDCC-3.1.3.7-6", MAP)).toBe(
      "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.3.7",
    );
  });

  it("drops a sub-item part and an item of a top-level section", () => {
    expect(resolveSpecLink("FAPI2-SP-FINAL-5.3.2.1-5.1", MAP)).toBe(
      "https://openid.net/specs/fapi-security-profile-2_0-final.html#section-5.3.2.1",
    );
    expect(resolveSpecLink("HAIP-5-5", MAP)).toBe(
      "https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html#section-5",
    );
  });

  it("keeps named fragments that contain hyphens", () => {
    expect(resolveSpecLink("CDR-request-object", MAP)).toBe(
      "https://consumerdatastandardsaustralia.github.io/standards/#request-object",
    );
    expect(resolveSpecLink("CDR-levels-of-assurance-loas", MAP)).toBe(
      "https://consumerdatastandardsaustralia.github.io/standards/#levels-of-assurance-loas",
    );
  });

  it("keeps a suffix that starts with a hyphen", () => {
    expect(resolveSpecLink("CBUAE-2.-Security-Profile", MAP)).toBe(
      "https://example.com/Security+Profile+-+FAPI#-2.-Security-Profile",
    );
  });

  const SECTIONS = {
    "BrazilOBDCR-7.1.1": "https://example.com/dcr#7.1.1.-Applying-Server-Defaults",
    "BrazilOBDCR-7.1": "https://example.com/dcr#7.1.-Authorization-server",
  };
  const BRAZIL_MAP = { ...MAP, "BrazilOBDCR-": "https://example.com/dcr#" };

  it("links a section requirement to its section URL", () => {
    expect(resolveSpecLink("BrazilOBDCR-7.1", BRAZIL_MAP, SECTIONS)).toBe(
      "https://example.com/dcr#7.1.-Authorization-server",
    );
    expect(resolveSpecLink("BrazilOBDCR-7.1-10", BRAZIL_MAP, SECTIONS)).toBe(
      "https://example.com/dcr#7.1.-Authorization-server",
    );
    expect(resolveSpecLink("BrazilOBDCR-7.1.1-2", BRAZIL_MAP, SECTIONS)).toBe(
      "https://example.com/dcr#7.1.1.-Applying-Server-Defaults",
    );
  });

  it("does not match a section link against a longer section number", () => {
    expect(resolveSpecLink("BrazilOBDCR-7.10", BRAZIL_MAP, SECTIONS)).toBe(
      "https://example.com/dcr#7.10",
    );
    expect(resolveSpecLink("BrazilOBDCR-7.1.2-1", BRAZIL_MAP, SECTIONS)).toBe(
      "https://example.com/dcr#7.1.2",
    );
  });

  it("falls back to the prefix map without section links", () => {
    expect(resolveSpecLink("BrazilOBDCR-7.1-10", BRAZIL_MAP)).toBe("https://example.com/dcr#7.1");
    expect(resolveSpecLink("BrazilOBDCR-7.1", null, SECTIONS)).toBe(
      "https://example.com/dcr#7.1.-Authorization-server",
    );
  });

  it("returns null when nothing matches", () => {
    expect(resolveSpecLink("TYPO9999-1.2.3", MAP)).toBeNull();
    expect(resolveSpecLink("", MAP)).toBeNull();
    expect(resolveSpecLink("RFC7517-1.1", null)).toBeNull();
  });
});
