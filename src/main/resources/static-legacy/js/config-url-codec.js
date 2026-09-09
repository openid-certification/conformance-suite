// NOTE: verbatim copy of /static/js/config-url-codec.js for the legacy-ui Spring profile, which
// serves ONLY the static-legacy/ tree (see application-legacy-ui.properties). Keep the two in
// sync: the new and legacy schedule-test pages must read/write the same share-link formats.
/**
 * Codec for carrying a test-plan configuration inside a schedule-test.html URL
 * ("Share Test Plan Configuration" links).
 *
 * Two wire formats are understood:
 *
 *   ?configJson=<encodeURIComponent(JSON)>        legacy, uncompressed
 *   ?configJsonZ=<base64url(deflate(JSON))>        compressed (preferred)
 *
 * Why compress: the whole config (JWKS, PEM certificates, browser automation
 * scripts, ...) rides in the query string, and every byte of it is charged
 * twice against the server's request-header budget because the nginx proxy
 * also forwards the full URL as X-Forwarded-Uri. A VCI/HAIP issuer config is
 * ~16KB URL-encoded and tipped the old 32KB Tomcat limit; deflate brings it
 * down to ~10KB. (The server never parses either parameter — schedule-test.html
 * hydrates the form client-side — so the format is entirely the frontend's.)
 *
 * Encoding falls back to the legacy parameter when the browser lacks
 * CompressionStream, and decoding always accepts both, so old links keep
 * working and new links still open in browsers without the API.
 *
 * Deliberately dependency-free: only web-platform APIs (CompressionStream,
 * Blob, Response, btoa/atob), all of which also exist in Node >= 18 so the
 * unit tests run in the vitest "unit" (node) project.
 */

/** Legacy query parameter: URL-encoded JSON. */
export const CONFIG_JSON_PARAM = "configJson";

/** Compressed query parameter: base64url(deflate(JSON)). */
export const CONFIG_JSON_COMPRESSED_PARAM = "configJsonZ";

/**
 * "deflate" (zlib-wrapped) rather than "deflate-raw": identical support in
 * Safari/Firefox (both arrived with CompressionStream itself) but "deflate"
 * also covers Chromium 80–102, for the cost of a 6-byte zlib envelope.
 */
const COMPRESSION_FORMAT = "deflate";

/**
 * Upper bound on the inflated size of a `configJsonZ` payload. The legacy
 * `configJson` parameter was implicitly bounded by the proxy's request-line
 * limit (~32KB); deflate lifts that, and a crafted payload of that size can
 * inflate ~1000x. Capping the inflated byte count keeps a malicious share
 * link from ballooning to tens of MB in the victim's tab before JSON.parse.
 * 5MB is orders of magnitude above any real test-plan config.
 */
export const MAX_INFLATED_CONFIG_BYTES = 5 * 1024 * 1024;

/**
 * Whether this runtime can produce compressed links.
 *
 * @returns {boolean}
 */
export function supportsCompressedConfigParam() {
  return (
    typeof CompressionStream === "function" &&
    typeof DecompressionStream === "function" &&
    typeof Blob === "function" &&
    typeof Response === "function"
  );
}

/**
 * Encode bytes as unpadded base64url (RFC 4648 §5). Every output character is
 * URL-unreserved, so encodeURIComponent() leaves the value untouched — the
 * link is exactly as long as the encoding.
 *
 * @param {Uint8Array} bytes
 * @returns {string}
 */
export function bytesToBase64Url(bytes) {
  let binary = "";
  // Chunked to stay clear of the argument-count limit of fromCharCode.apply.
  const CHUNK = 0x8000;
  for (let i = 0; i < bytes.length; i += CHUNK) {
    binary += String.fromCharCode.apply(null, Array.from(bytes.subarray(i, i + CHUNK)));
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/**
 * Decode unpadded (or padded) base64url back to bytes.
 *
 * @param {string} text
 * @returns {Uint8Array}
 */
export function base64UrlToBytes(text) {
  const base64 = text.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

/**
 * @param {Uint8Array} bytes
 * @param {ReadableWritablePair<Uint8Array, Uint8Array>} transform
 * @returns {Promise<Uint8Array>}
 */
async function pipeBytes(bytes, transform) {
  const stream = new Blob([bytes]).stream().pipeThrough(transform);
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

/**
 * Compress a config object into the `configJsonZ` value.
 *
 * @param {object} config
 * @returns {Promise<string>} base64url(deflate(JSON.stringify(config)))
 */
export async function compressConfigForUrl(config) {
  const json = new TextEncoder().encode(JSON.stringify(config));
  const deflated = await pipeBytes(json, new CompressionStream(COMPRESSION_FORMAT));
  return bytesToBase64Url(deflated);
}

/**
 * Like {@link pipeBytes}, but aborts as soon as the transform has emitted more
 * than `maxBytes` — the whole point is to stop inflating before the memory is
 * allocated, so this reads chunk-by-chunk instead of buffering via Response.
 *
 * @param {Uint8Array} bytes
 * @param {ReadableWritablePair<Uint8Array, Uint8Array>} transform
 * @param {number} maxBytes
 * @returns {Promise<Uint8Array>}
 * @throws if the transform emits more than `maxBytes`
 */
async function pipeBytesCapped(bytes, transform, maxBytes) {
  const reader = new Blob([bytes]).stream().pipeThrough(transform).getReader();
  const chunks = [];
  let total = 0;
  try {
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      total += value.byteLength;
      if (total > maxBytes) {
        throw new Error(`decompressed data exceeds the ${maxBytes} byte limit`);
      }
      chunks.push(value);
    }
  } finally {
    reader.cancel().catch(() => {});
  }
  const out = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    out.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return out;
}

/**
 * Inverse of {@link compressConfigForUrl}.
 *
 * @param {string} value a `configJsonZ` value
 * @returns {Promise<any>} the parsed config
 * @throws if the value is not valid base64url / deflate / JSON, or inflates
 *   past {@link MAX_INFLATED_CONFIG_BYTES}
 */
export async function decompressConfigFromUrl(value) {
  const inflated = await pipeBytesCapped(
    base64UrlToBytes(value),
    new DecompressionStream(COMPRESSION_FORMAT),
    MAX_INFLATED_CONFIG_BYTES,
  );
  return JSON.parse(new TextDecoder().decode(inflated));
}

/**
 * Build the query parameter that carries `config` in a share link: compressed
 * when the runtime supports it, the legacy URL-encoded JSON otherwise.
 *
 * @param {object} config
 * @returns {Promise<{name: string, value: string}>} `value` is NOT yet
 *   URI-encoded — callers pass it through encodeURIComponent() (a no-op for
 *   the compressed form).
 */
export async function buildConfigUrlParam(config) {
  if (supportsCompressedConfigParam()) {
    try {
      return { name: CONFIG_JSON_COMPRESSED_PARAM, value: await compressConfigForUrl(config) };
    } catch (e) {
      console.warn("[config-url-codec] compression failed, falling back to configJson", e);
    }
  }
  return { name: CONFIG_JSON_PARAM, value: JSON.stringify(config) };
}

/**
 * Whether `params` carries a config in either wire format.
 *
 * @param {URLSearchParams} params
 * @returns {boolean}
 */
export function hasConfigUrlParam(params) {
  return Boolean(params.get(CONFIG_JSON_COMPRESSED_PARAM) || params.get(CONFIG_JSON_PARAM));
}

/**
 * Read a config from the URL query, preferring the compressed parameter.
 *
 * @param {URLSearchParams} params
 * @returns {Promise<any|null>} the parsed config, or null when neither
 *   parameter is present (empty values count as absent, matching the page's
 *   existing truthiness checks)
 * @throws if the present parameter cannot be decoded
 */
export async function readConfigFromUrlParams(params) {
  const compressed = params.get(CONFIG_JSON_COMPRESSED_PARAM);
  if (compressed) {
    if (!supportsCompressedConfigParam()) {
      throw new Error(
        "this browser cannot decode compressed configJsonZ links (no DecompressionStream)",
      );
    }
    return decompressConfigFromUrl(compressed);
  }
  const plain = params.get(CONFIG_JSON_PARAM);
  if (plain) {
    return JSON.parse(plain);
  }
  return null;
}
