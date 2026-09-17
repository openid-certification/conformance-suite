import { describe, it, expect } from "vitest";
import { PAGE_SIZE, listingParams, readListingPage } from "./listing-request.js";

describe("listingParams", () => {
  it("asks for the first page of the default size with nothing else by default", () => {
    expect(listingParams({}).toString()).toBe(`start=0&length=${PAGE_SIZE}`);
  });

  it("carries the offset, ordering, search term and public flag", () => {
    const params = listingParams({
      start: 50,
      order: "started,desc",
      search: "  rotate keys ",
      isPublic: true,
    });
    expect(params.get("start")).toBe("50");
    expect(params.get("length")).toBe(String(PAGE_SIZE));
    expect(params.get("order")).toBe("started,desc");
    // trimmed, so a term of spaces is no term
    expect(params.get("search")).toBe("rotate keys");
    expect(params.get("public")).toBe("true");
  });

  it("leaves out a blank search and an unset public flag", () => {
    const params = listingParams({ search: "   ", isPublic: false });
    expect(params.has("search")).toBe(false);
    expect(params.has("public")).toBe(false);
  });

  it("appends endpoint-specific parameters as given", () => {
    const extra = new URLSearchParams({ status: "running,waiting", family: "OIDCC" });
    const params = listingParams({ extra });
    expect(params.get("status")).toBe("running,waiting");
    expect(params.get("family")).toBe("OIDCC");
    expect(params.get("start")).toBe("0");
  });
});

describe("readListingPage", () => {
  it("reads the rows of the envelope and knows a further page from the synthetic total", () => {
    // start + length + 1: the server's way of saying there is more
    expect(readListingPage({ recordsTotal: 26, data: [1, 2] }, 0)).toEqual({
      rows: [1, 2],
      hasMore: true,
    });
    // start + data.length: the last page
    expect(readListingPage({ recordsTotal: 27, data: [1, 2] }, 25)).toEqual({
      rows: [1, 2],
      hasMore: false,
    });
  });

  it("treats a plain array as one complete page", () => {
    expect(readListingPage([1, 2, 3], 0)).toEqual({ rows: [1, 2, 3], hasMore: false });
  });

  it("reads nothing out of a body that is not a listing", () => {
    expect(readListingPage(null, 0)).toEqual({ rows: [], hasMore: false });
    expect(readListingPage({ error: "nope" }, 0)).toEqual({ rows: [], hasMore: false });
    expect(readListingPage({ data: "not rows", recordsTotal: 5 }, 0)).toEqual({
      rows: [],
      hasMore: true,
    });
  });
});
