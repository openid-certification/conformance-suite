import { describe, it, expect, vi, afterEach } from "vitest";
import {
  CONFIG_JSON_PARAM,
  CONFIG_JSON_COMPRESSED_PARAM,
  supportsCompressedConfigParam,
  bytesToBase64Url,
  base64UrlToBytes,
  compressConfigForUrl,
  decompressConfigFromUrl,
  buildConfigUrlParam,
  hasConfigUrlParam,
  readConfigFromUrlParams,
} from "./config-url-codec.js";

/** A config shaped like the real thing: nested objects, PEM, JWK, unicode. */
const SAMPLE_CONFIG = {
  alias: "oidf-vci-issuer-test",
  description: "VCI Issuer test config (dpop) [fapi2-security-profile-final] — ü/€",
  vci: {
    credential_issuer_url: "https://localhost.emobix.co.uk:8443/test/a/oidf-vci-wallet-test/",
    credential_configuration_id: "eu.europa.ec.eudi.pid.1",
    static_tx_code: "123456",
  },
  client: {
    client_id: "52480754053",
    jwks: {
      keys: [
        {
          kty: "EC",
          d: "C9AWp9_vYfHj-ckdYAo0aYk-ZGXOPwoKJEqmYGlqUGI",
          crv: "P-256",
          kid: "vci-example-key-1",
          x: "yHNp8QgNiVSxSxIH_n_nH23dpUDlNhbgvLKSrjK1hDs",
          y: "3_rlpW_FXqghp8dKPpkjfvbfACQQFLFZwJXxOr319Ac",
          alg: "ES256",
        },
      ],
    },
  },
  mtls: {
    cert: "-----BEGIN CERTIFICATE-----\nMIIDlTCCAn2gAwIBAgIJAKRJoaX7BlZbMA0GCSqGSIb3DQEBCwUAMGAxCzAJBgNV\n-----END CERTIFICATE-----\n",
  },
  browser: [
    {
      match: "https://*/test/a/oidf-vci-wallet-test/authorize*",
      tasks: [{ task: "Verify Complete", commands: [["wait", "id", "submission_complete", 10]] }],
    },
  ],
  options: { browsercontrol_css_enable: false },
};

describe("config-url-codec", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("runs in a runtime with CompressionStream (sanity for the rest of the suite)", () => {
    expect(supportsCompressedConfigParam()).toBe(true);
  });

  describe("base64url", () => {
    it("round-trips arbitrary bytes including values that need - and _", () => {
      const bytes = new Uint8Array(256);
      for (let i = 0; i < 256; i++) bytes[i] = i;
      const text = bytesToBase64Url(bytes);
      expect(text).not.toMatch(/[+/=]/);
      expect(Array.from(base64UrlToBytes(text))).toEqual(Array.from(bytes));
    });

    it("is URL-safe: encodeURIComponent is the identity on the output", () => {
      const text = bytesToBase64Url(new Uint8Array([0xfb, 0xff, 0xbf, 0x3e, 0x3f]));
      expect(encodeURIComponent(text)).toBe(text);
    });

    it("accepts padded input too", () => {
      expect(new TextDecoder().decode(base64UrlToBytes("aGk="))).toBe("hi");
      expect(new TextDecoder().decode(base64UrlToBytes("aGk"))).toBe("hi");
    });

    it("handles inputs larger than one fromCharCode chunk", () => {
      const bytes = new Uint8Array(100_000).map((_, i) => i % 251);
      expect(Array.from(base64UrlToBytes(bytesToBase64Url(bytes)))).toEqual(Array.from(bytes));
    });
  });

  describe("compress / decompress", () => {
    it("round-trips a realistic config", async () => {
      const value = await compressConfigForUrl(SAMPLE_CONFIG);
      expect(await decompressConfigFromUrl(value)).toEqual(SAMPLE_CONFIG);
    });

    it("is materially shorter than the URL-encoded JSON it replaces", async () => {
      const legacy = encodeURIComponent(JSON.stringify(SAMPLE_CONFIG));
      const value = await compressConfigForUrl(SAMPLE_CONFIG);
      expect(encodeURIComponent(value)).toBe(value);
      expect(value.length).toBeLessThan(legacy.length * 0.75);
    });

    it("rejects garbage", async () => {
      await expect(decompressConfigFromUrl("not-deflate-data")).rejects.toThrow();
    });
  });

  describe("buildConfigUrlParam", () => {
    it("emits the compressed parameter when the runtime supports it", async () => {
      const param = await buildConfigUrlParam(SAMPLE_CONFIG);
      expect(param.name).toBe(CONFIG_JSON_COMPRESSED_PARAM);
      expect(await decompressConfigFromUrl(param.value)).toEqual(SAMPLE_CONFIG);
    });

    it("falls back to legacy configJson without CompressionStream", async () => {
      vi.stubGlobal("CompressionStream", undefined);
      const param = await buildConfigUrlParam(SAMPLE_CONFIG);
      expect(param.name).toBe(CONFIG_JSON_PARAM);
      expect(JSON.parse(param.value)).toEqual(SAMPLE_CONFIG);
    });

    it("falls back to legacy configJson when compression throws", async () => {
      vi.stubGlobal(
        "CompressionStream",
        class {
          constructor() {
            throw new Error("boom");
          }
        },
      );
      const warn = vi.spyOn(console, "warn").mockImplementation(() => {});
      const param = await buildConfigUrlParam(SAMPLE_CONFIG);
      expect(param.name).toBe(CONFIG_JSON_PARAM);
      expect(JSON.parse(param.value)).toEqual(SAMPLE_CONFIG);
      expect(warn).toHaveBeenCalled();
    });
  });

  describe("readConfigFromUrlParams", () => {
    it("returns null when neither parameter is present or both are empty", async () => {
      expect(await readConfigFromUrlParams(new URLSearchParams(""))).toBeNull();
      expect(
        await readConfigFromUrlParams(new URLSearchParams("configJson=&configJsonZ=")),
      ).toBeNull();
      expect(hasConfigUrlParam(new URLSearchParams("configJson=&configJsonZ="))).toBe(false);
    });

    it("decodes the compressed parameter", async () => {
      const value = await compressConfigForUrl(SAMPLE_CONFIG);
      const params = new URLSearchParams({ [CONFIG_JSON_COMPRESSED_PARAM]: value });
      expect(hasConfigUrlParam(params)).toBe(true);
      expect(await readConfigFromUrlParams(params)).toEqual(SAMPLE_CONFIG);
    });

    it("still decodes legacy configJson links", async () => {
      const params = new URLSearchParams({ [CONFIG_JSON_PARAM]: JSON.stringify(SAMPLE_CONFIG) });
      expect(hasConfigUrlParam(params)).toBe(true);
      expect(await readConfigFromUrlParams(params)).toEqual(SAMPLE_CONFIG);
    });

    it("prefers the compressed parameter when both are present", async () => {
      const params = new URLSearchParams({
        [CONFIG_JSON_COMPRESSED_PARAM]: await compressConfigForUrl({ alias: "z" }),
        [CONFIG_JSON_PARAM]: JSON.stringify({ alias: "plain" }),
      });
      expect(await readConfigFromUrlParams(params)).toEqual({ alias: "z" });
    });

    it("throws on an undecodable compressed value", async () => {
      const params = new URLSearchParams({ [CONFIG_JSON_COMPRESSED_PARAM]: "@@not-base64@@" });
      await expect(readConfigFromUrlParams(params)).rejects.toThrow();
    });

    it("throws on invalid legacy JSON", async () => {
      const params = new URLSearchParams({ [CONFIG_JSON_PARAM]: "{not json" });
      await expect(readConfigFromUrlParams(params)).rejects.toThrow();
    });

    it("throws (does not silently ignore) a compressed link in a runtime without DecompressionStream", async () => {
      const value = await compressConfigForUrl(SAMPLE_CONFIG);
      vi.stubGlobal("DecompressionStream", undefined);
      const params = new URLSearchParams({ [CONFIG_JSON_COMPRESSED_PARAM]: value });
      await expect(readConfigFromUrlParams(params)).rejects.toThrow(/DecompressionStream/);
    });
  });
});
