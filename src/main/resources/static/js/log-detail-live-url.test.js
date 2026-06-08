import { describe, it, expect } from "vitest";
import { parseLogUrl } from "./log-detail-live-url.js";

describe("parseLogUrl", () => {
  it("maps a log-detail page URL to the test id and /api/info URL (same origin)", () => {
    const { id, apiUrl } = parseLogUrl(
      "https://localhost.emobix.co.uk:8443/log-detail.html?log=ABC123",
    );
    expect(id).toBe("ABC123");
    expect(apiUrl).toBe("https://localhost.emobix.co.uk:8443/api/info/ABC123");
  });

  it("percent-encodes ids that contain URL-significant characters", () => {
    const { id, apiUrl } = parseLogUrl(
      "https://example.com/log-detail.html?log=a%2Fb%20c",
    );
    // searchParams decodes the value; the apiUrl must re-encode it so the
    // slash/space land in the path segment, not as a new path or query.
    expect(id).toBe("a/b c");
    expect(apiUrl).toBe("https://example.com/api/info/a%2Fb%20c");
  });

  it("throws a paste prompt on blank input", () => {
    expect(() => parseLogUrl("")).toThrow("Paste a log-detail.html?log=<id> URL.");
    expect(() => parseLogUrl("   ")).toThrow("Paste a log-detail.html?log=<id> URL.");
  });

  it("throws an invalid-URL message on non-URL input", () => {
    expect(() => parseLogUrl("not a url")).toThrow("That doesn't look like a URL.");
  });

  it("throws a missing-param message when the log query parameter is absent", () => {
    expect(() => parseLogUrl("https://localhost.emobix.co.uk:8443/log-detail.html")).toThrow(
      "URL is missing the ?log=<id> parameter.",
    );
  });
});
