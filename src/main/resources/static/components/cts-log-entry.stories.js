import { html } from "lit";
import { expect, within, waitFor, spyOn } from "storybook/test";
import "./cts-log-entry.js";
import { __seedSpecLinks, __resetSpecLinks } from "../lib/spec-links.js";

export default {
  title: "Components/cts-log-entry",
  component: "cts-log-entry",
};

const NOW = Date.now();

const SUCCESS_ENTRY = {
  _id: "entry-success",
  testId: "test-abc",
  src: "CheckServerConfiguration",
  time: NOW - 5000,
  msg: "Server configuration valid",
  result: "SUCCESS",
};

const FAILURE_ENTRY = {
  _id: "entry-fail",
  testId: "test-abc",
  src: "ValidateIdToken",
  time: NOW - 3000,
  msg: "ID token signature validation failed: key not found in JWKS",
  result: "FAILURE",
  requirements: ["OIDCC-3.1.3.7-6", "OIDCC-3.1.3.7-7"],
};

const WARNING_ENTRY = {
  _id: "entry-warn",
  testId: "test-abc",
  src: "EnsureTokenEndpointResponseHasCorrectFields",
  time: NOW - 2000,
  msg: "Token endpoint returned unexpected field 'custom_field'",
  result: "WARNING",
  requirements: ["OIDCC-3.1.3.3"],
};

// Fixture for the new static `info` variant routing. INFO-level log rows
// previously rendered through the spinner-bearing `running` variant (and
// looked like they were perpetually loading); they now route through the
// static `info` variant added in U1 of the MR 1998 maintainer-feedback plan.
const INFO_ENTRY = {
  _id: "entry-info",
  testId: "test-abc",
  src: "BuildRequestObject",
  time: NOW - 4500,
  msg: "Built request object with kid=test-key-1",
  result: "INFO",
};

const HTTP_REQUEST_ENTRY = {
  _id: "entry-http-req",
  testId: "test-abc",
  src: "BuildRedirectUri",
  time: NOW - 4000,
  msg: "Built redirect URI",
  http: "REQUEST",
  more: {
    method: "GET",
    url: "https://op.example.com/authorize?client_id=test-client&scope=openid",
    headers: {
      Accept: "text/html",
      "User-Agent": "conformance-suite/5.1.24",
    },
  },
};

const HTTP_RESPONSE_ENTRY = {
  _id: "entry-http-resp",
  testId: "test-abc",
  src: "CallTokenEndpoint",
  time: NOW - 3500,
  msg: "Token endpoint response",
  http: "RESPONSE",
  result: "SUCCESS",
  more: {
    status: 200,
    body: {
      access_token: "eyJ...",
      token_type: "Bearer",
      expires_in: 3600,
    },
  },
};

const ENTRY_WITH_MORE = {
  _id: "entry-more",
  testId: "test-abc",
  src: "ExtractAccessToken",
  time: NOW - 1000,
  msg: "Extracted access token",
  result: "SUCCESS",
  more: {
    access_token: "eyJhbGciOiJSUzI1NiJ9...",
    token_type: "Bearer",
    expires_in: 3600,
    scope: "openid profile",
  },
};

const ENTRY_NO_MORE = {
  _id: "entry-no-more",
  testId: "test-abc",
  src: "CheckTestOutcome",
  time: NOW - 500,
  msg: "Test passed",
  result: "SUCCESS",
};

const BLOCK_ENTRY = {
  _id: "entry-block",
  testId: "test-abc",
  src: "ValidateAuthResponse",
  time: NOW - 2500,
  msg: "Received valid authorization code",
  blockId: "block-auth",
  result: "SUCCESS",
};

const UPLOAD_ENTRY = {
  _id: "entry-upload",
  testId: "test-abc",
  src: "CheckScreenshot",
  time: NOW - 500,
  msg: "Screenshot required",
  result: "REVIEW",
  upload: "screenshot_consent",
};

// U7 fixtures. These match the shape `/api/log/{testId}` actually serializes:
// application payload sits at the entry's *top level* (request_uri,
// response_body, …) rather than under a `more: {}` envelope. The legacy
// Thymeleaf export computed the disclosure view on the server side via
// LogEntryHelper.visibleFields — the new Lit component has to do the same
// strip itself because the browser API returns raw entries. Before U7 these
// real-shape entries rendered with no Details button at all (D1).
const REAL_HTTP_REQUEST_ENTRY = {
  _id: "entry-real-http-req",
  testId: "test-real",
  testName: "oidcc-server",
  src: "GetDynamicServerConfiguration",
  time: NOW - 6000,
  msg: "HTTP request",
  http: "request",
  // Payload at the entry top level — what real entries actually carry.
  request_uri: "https://oidcc-provider:3000/.well-known/openid-configuration",
  request_method: "GET",
  request_headers: {
    Accept: "text/plain, application/json, application/yaml, application/*+json, */*",
    "Content-Length": "0",
  },
  request_body: "",
};

const REAL_HTTP_RESPONSE_ENTRY = {
  _id: "entry-real-http-resp",
  testId: "test-real",
  testName: "oidcc-server",
  src: "GetDynamicServerConfiguration",
  time: NOW - 5800,
  msg: "HTTP response",
  http: "response",
  // Realistic top-level fields from a discovery-document response.
  response_status_code: 200,
  response_body: {
    issuer: "https://oidcc-provider:3000",
    authorization_endpoint: "https://oidcc-provider:3000/auth",
    token_endpoint: "https://oidcc-provider:3000/token",
  },
  response_headers: {
    "Content-Type": "application/json; charset=utf-8",
  },
};

// Envelope-only entry: every field on it is in the strip set, so the More
// panel must remain hidden — no empty disclosure for an entry with nothing
// to disclose.
const ENVELOPE_ONLY_ENTRY = {
  _id: "entry-envelope-only",
  testId: "test-real",
  testName: "oidcc-server",
  src: "CheckTestOutcome",
  time: NOW - 5500,
  msg: "Test passed",
  result: "SUCCESS",
  // Per-test metadata `/api/log` adds to every entry: this row carries
  // these but nothing else, so the disclosure must stay empty.
  baseUrl: "https://localhost.emobix.co.uk:8443/test/a/oidcc-server",
  baseMtlsUrl: "https://localhost.emobix.co.uk:8444/test/a/oidcc-server",
  variant: { server_metadata: "discovery" },
  alias: "",
  description: null,
  planId: "M1vPYjQWZWxKr",
  config: {},
};

// R30 fixtures. Modeled on real conditions in the Java backend so the labeling
// matches what users see in production. The Brazil scope-validation pattern
// (`args("expected", "payments", "actual", scope)`) is one of the most common
// shapes — see e.g. FAPIBrazilEnsureAuthorizationRequestScopesContainPayments.
const ENTRY_EXPECTED_VS_ACTUAL = {
  _id: "entry-expected-vs-actual",
  testId: "test-abc",
  src: "FAPIBrazilEnsureAuthorizationRequestScopesContainPayments",
  time: NOW - 1500,
  msg: "Authorization request did not contain the required 'payments' scope",
  result: "FAILURE",
  requirements: ["BrazilOPIN-5.2.2-2"],
  // Note: declared with `actual` first to prove the helper reorders to
  // expected-first regardless of insertion order.
  more: {
    actual: "openid profile",
    expected: "payments",
  },
};

// Suffix-variant pattern from FAPIBrazilGenerateGetPaymentConsentResponse:
// `args("requested_consent_id", x, "expected_consent_id", y)`. Demonstrates
// that prefix-matched keys carry their suffix into the displayLabel and that
// genuinely-other keys (`requested_consent_id`) stay un-classified.
const ENTRY_SUFFIX_VARIANTS = {
  _id: "entry-suffix-variants",
  testId: "test-abc",
  src: "FAPIBrazilGenerateGetPaymentConsentResponse",
  time: NOW - 1200,
  msg: "Consent ID mismatch between request and stored consent",
  result: "FAILURE",
  more: {
    requested_consent_id: "urn:bancoex:C1DD33123",
    expected_consent_id: "urn:bancoex:C1DD33999",
    actual_http_method: "GET",
  },
};

// Substring-boundary fixture: `unexpected_field` contains "expected" but is
// NOT prefixed `expected_`. The classifier uses startsWith with a mandatory
// underscore separator, so this key must classify as "other". This is the
// explicit risk-table mitigation from the R30 plan — a refactor that loosened
// the rule to `includes("expected")` would silently mis-label real production
// keys, and this story would catch it.
const ENTRY_SUBSTRING_BOUNDARY = {
  _id: "entry-substring-boundary",
  testId: "test-abc",
  src: "ValidateTokenResponseBody",
  time: NOW - 800,
  msg: "Token response contained an unexpected field",
  result: "WARNING",
  more: {
    unexpected_field: "custom_claim",
  },
};

export const SuccessEntry = {
  render: () => html`<cts-log-entry .entry=${SUCCESS_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("SUCCESS")).toBeInTheDocument();
    });
    expect(canvas.getByText("Server configuration valid")).toBeInTheDocument();
    expect(canvas.getByText("CheckServerConfiguration")).toBeInTheDocument();

    // Canonical OIDF status palette: SUCCESS -> pass.
    const badge = canvasElement.querySelector('cts-badge[variant="pass"]');
    expect(badge).toBeTruthy();

    // U3: layout reflows via container queries — track-count assertions
    // moved to the explicit-width SmallContainerLayout / DesktopContainerLayout
    // stories below so they don't depend on the storybook canvas width.
    const item = canvasElement.querySelector(".logItem");
    const style = getComputedStyle(item);
    expect(style.display).toBe("grid");

    const time = canvasElement.querySelector(".logTime");
    expect(getComputedStyle(time).fontVariantNumeric).toContain("tabular-nums");

    // The time cell renders a native <time> via cts-time. Visible text is the
    // clock time only (no date), but hovering reveals the full absolute form
    // via the title attribute — the disambiguation affordance this surface
    // previously lacked.
    const timeEl = /** @type {HTMLTimeElement} */ (time.querySelector("time"));
    expect(timeEl).toBeTruthy();
    const titleAttr = timeEl.getAttribute("title") || "";
    expect(titleAttr).toMatch(/\d{4}/); // absolute form carries a 4-digit year
    expect(timeEl.textContent || "").not.toMatch(/\d{4}/); // visible text is time-only
    expect(timeEl.getAttribute("datetime")).toBeTruthy();
  },
};

export const FailureEntry = {
  render: () => html`<cts-log-entry .entry=${FAILURE_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("FAILURE")).toBeInTheDocument();
    });

    const badge = canvasElement.querySelector('cts-badge[variant="fail"]');
    expect(badge).toBeTruthy();

    expect(canvas.getByText("OIDCC-3.1.3.7-6")).toBeInTheDocument();
    expect(canvas.getByText("OIDCC-3.1.3.7-7")).toBeInTheDocument();

    // Failure rows get the left-edge red gradient marker class.
    const item = canvasElement.querySelector(".logItem");
    expect(item.classList.contains("is-fail")).toBe(true);
  },
};

export const WarningEntry = {
  render: () => html`<cts-log-entry .entry=${WARNING_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("WARNING")).toBeInTheDocument();
    });

    const badge = canvasElement.querySelector('cts-badge[variant="warn"]');
    expect(badge).toBeTruthy();

    expect(canvas.getByText("OIDCC-3.1.3.3")).toBeInTheDocument();

    // Warning rows get the left-edge orange gradient marker class.
    const item = canvasElement.querySelector(".logItem");
    expect(item.classList.contains("is-warn")).toBe(true);
  },
};

// U8: spec-link chips resolve to anchors via the memoised loader at
// `lib/spec-links.js`. The legacy UI does this through
// `api/ui/spec_links` + a longest-prefix match; the new component does
// the same but caches the in-flight Promise once per page.

// Decorator that seeds the spec-link cache before the component mounts.
// The component reads from the cache inside its connectedCallback (which
// fires synchronously during storyFn render), so seeding has to happen
// *before* storyFn() — a play function would be too late.
function withSeededSpecLinks(map) {
  return (storyFn) => {
    __resetSpecLinks();
    __seedSpecLinks(map);
    return storyFn();
  };
}

// Fixture entry whose requirements all match the seeded spec-link map
// (and one that intentionally doesn't, so we can assert the fallback to
// a static <span>).
const SPEC_LINK_ENTRY = {
  _id: "entry-spec-link",
  testId: "test-abc",
  src: "ValidateIdToken",
  time: NOW - 1000,
  msg: "ID token validation failed",
  result: "FAILURE",
  requirements: [
    "OIDCC-3.1.3.7-6", // matches the seeded `OIDCC-` prefix
    "RFC7517-1.1", // matches the seeded `RFC7517-` prefix
    "TYPO9999-1.2.3", // intentionally unmatched: stays a static chip
  ],
};

const SEEDED_SPEC_LINKS = {
  "OIDCC-": "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.",
  "RFC7517-": "https://tools.ietf.org/html/rfc7517#section-",
};

export const SpecLinkResolvesMappedRefsToAnchors = {
  decorators: [withSeededSpecLinks(SEEDED_SPEC_LINKS)],
  render: () => html`<cts-log-entry .entry=${SPEC_LINK_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const oidcc = canvasElement.querySelector('a.logRequirement[href*="openid-connect-core"]');
      expect(oidcc).toBeTruthy();
    });

    const oidcc = canvasElement.querySelector('a.logRequirement[href*="openid-connect-core"]');
    // Anchor preserves the original requirement text verbatim — no rewrite.
    expect(oidcc.textContent.trim()).toBe("OIDCC-3.1.3.7-6");
    // URL is longest-prefix-match: prefix URL + suffix.
    expect(oidcc.getAttribute("href")).toBe(
      "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.3.7-6",
    );
    // Always open in a new tab; the suite stays focused on the failing log.
    expect(oidcc.getAttribute("target")).toBe("_blank");
    // noopener prevents the spec page from holding a reference back to
    // the conformance-suite window (window.opener); noreferrer hides
    // the origin from the spec host.
    expect(oidcc.getAttribute("rel")).toContain("noopener");

    const rfc = canvasElement.querySelector('a.logRequirement[href*="rfc7517"]');
    expect(rfc).toBeTruthy();
    expect(rfc.getAttribute("href")).toBe("https://tools.ietf.org/html/rfc7517#section-1.1");
  },
};

export const SpecLinkUnknownRefStaysStatic = {
  decorators: [withSeededSpecLinks(SEEDED_SPEC_LINKS)],
  render: () => html`<cts-log-entry .entry=${SPEC_LINK_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    // Wait for the requirements row to render (any chip present).
    await waitFor(() => {
      expect(canvasElement.querySelector(".logRequirement")).toBeTruthy();
    });

    // The unmapped chip must NOT be an anchor — otherwise the link target
    // would be undefined and the user would land on `#typo9999-1.2.3`.
    const chips = canvasElement.querySelectorAll(".logRequirement");
    const typo = Array.from(chips).find((el) => el.textContent.trim() === "TYPO9999-1.2.3");
    expect(typo).toBeTruthy();
    expect(typo.tagName).toBe("SPAN");
    expect(typo.getAttribute("href")).toBeNull();
  },
};

export const SpecLinkEmptyMapKeepsAllChipsStatic = {
  // Simulates the loader returning {} — either /api/ui/spec_links 404'd
  // or the response was empty. Every chip must degrade to <span>; no
  // broken anchors with empty hrefs.
  decorators: [withSeededSpecLinks({})],
  render: () => html`<cts-log-entry .entry=${SPEC_LINK_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      expect(canvasElement.querySelectorAll(".logRequirement").length).toBe(3);
    });
    const chips = canvasElement.querySelectorAll(".logRequirement");
    chips.forEach((chip) => {
      expect(chip.tagName).toBe("SPAN");
    });
    expect(canvasElement.querySelector("a.logRequirement")).toBeNull();
  },
};

export const SpecLinkLongestPrefixWins = {
  // Both prefixes legitimately match the chip — `FOO-` is 4 chars,
  // `FOO-BAR-` is 8 chars and is also a startsWith match. The longer
  // one must win regardless of insertion order, otherwise links would
  // silently break whenever the map happened to iterate `FOO-` first.
  decorators: [
    withSeededSpecLinks({
      "FOO-": "https://example.com/foo#",
      "FOO-BAR-": "https://example.com/foo-bar#",
    }),
  ],
  render: () => html`
    <cts-log-entry
      .entry=${{
        ...SPEC_LINK_ENTRY,
        _id: "entry-spec-link-longest",
        requirements: ["FOO-BAR-3.1"],
      }}
    ></cts-log-entry>
  `,
  async play({ canvasElement }) {
    await waitFor(() => {
      expect(canvasElement.querySelector("a.logRequirement")).toBeTruthy();
    });
    const anchor = canvasElement.querySelector("a.logRequirement");
    expect(anchor.getAttribute("href")).toBe("https://example.com/foo-bar#3.1");
  },
};

export const HttpRequestEntry = {
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("REQUEST")).toBeInTheDocument();
    });

    const curlBtn = canvas.getByText("cURL");
    expect(curlBtn).toBeInTheDocument();

    // The disclosure toggle is icon + count (no "More" text); query it
    // by its class hook instead of by visible label.
    const moreBtn = canvasElement.querySelector(".moreBtn button");
    expect(moreBtn).toBeTruthy();

    // U2: the REQUEST pill must route through the static `info` variant
    // (added in U1), not `running`. Before the fix, this pill carried
    // the perpetual spinner from `running` even though the request was
    // a finished log row, not a live operation.
    const httpBadge = canvasElement.querySelector('.logHttp cts-badge[variant="info"]');
    expect(httpBadge).toBeTruthy();
    expect(canvasElement.querySelector('.logHttp cts-badge[variant="running"]')).toBeNull();
    expect(httpBadge.querySelector(".cts-badge-spin")).toBeNull();
  },
};

export const HttpResponseEntry = {
  render: () => html`<cts-log-entry .entry=${HTTP_RESPONSE_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("RESPONSE")).toBeInTheDocument();
    });

    expect(canvas.queryByText("cURL")).toBeNull();

    // U2: same static-info routing for RESPONSE rows. No spinner DOM.
    const httpBadge = canvasElement.querySelector('.logHttp cts-badge[variant="info"]');
    expect(httpBadge).toBeTruthy();
    expect(canvasElement.querySelector('.logHttp cts-badge[variant="running"]')).toBeNull();
    expect(httpBadge.querySelector(".cts-badge-spin")).toBeNull();
  },
};

/**
 * U2 regression guard: INFO-level result rows render through the static
 * `info` cts-badge variant — no spinner. Before U1+U2 landed, an
 * `result: "INFO"` row mapped to `variant="running"` and inherited the
 * perpetual spinner glyph, which read as "this row is still loading"
 * forever. The fix is one-line in cts-log-entry's RESULT_BADGE_VARIANTS
 * map; this story exists so a regression on that line fails loudly.
 */
export const InfoSeverityEntry = {
  render: () => html`<cts-log-entry .entry=${INFO_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("INFO")).toBeInTheDocument();
    });

    // Routed to the static `info` variant, not the spinner-bearing
    // `running` variant.
    const badge = canvasElement.querySelector('.logSeverity cts-badge[variant="info"]');
    expect(badge).toBeTruthy();
    expect(canvasElement.querySelector('.logSeverity cts-badge[variant="running"]')).toBeNull();

    // No spinner DOM inside the badge — the bug we are guarding against.
    expect(badge.querySelector(".cts-badge-spin")).toBeNull();
    expect(badge.querySelector("svg")).toBeNull();
  },
};

export const ClickMoreToggle = {
  render: () => html`<cts-log-entry .entry=${ENTRY_WITH_MORE}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvasElement.querySelector("cts-button.moreBtn")).toBeTruthy();
    });

    expect(canvasElement.querySelector(".moreInfo")).toBeNull();

    // The disclosure toggle is a single ghost button labeled "Details".
    // Confirm both the class hook and the visible label before driving
    // the click path.
    const moreHost = canvasElement.querySelector("cts-button.moreBtn");
    expect(moreHost).toBeTruthy();
    expect(moreHost.getAttribute("label")).toBe("Details");
    const moreBtn = moreHost.querySelector("button");
    expect(moreBtn).toBeTruthy();
    expect(moreBtn.getAttribute("aria-expanded")).toBe("false");
    await moreBtn.click();

    await waitFor(() => {
      expect(canvasElement.querySelector(".moreInfo")).toBeTruthy();
    });

    // R30: keys are now humanized for display ("access_token" → "Access token").
    // The original key is preserved on the dt's data-key attribute for tooling.
    expect(canvas.getByText("Access token")).toBeInTheDocument();
    expect(canvas.getByText("Token type")).toBeInTheDocument();
    expect(canvasElement.querySelector('[data-key="access_token"]')).toBeTruthy();

    // After expanding, aria-expanded flips to true so screen readers
    // announce the new state without depending on the chevron glyph alone.
    expect(canvasElement.querySelector(".moreBtn button").getAttribute("aria-expanded")).toBe(
      "true",
    );

    await canvasElement.querySelector(".moreBtn button").click();

    await waitFor(() => {
      expect(canvasElement.querySelector(".moreInfo")).toBeNull();
    });
  },
};

export const CopyAsCurl = {
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    // Spy on navigator.clipboard.writeText. The previous version mocked
    // navigator.copy by mistake; the real clipboard call then failed in
    // headless Chromium with NotAllowedError. restoreMocks: true in
    // vitest.config.js handles teardown.
    const writeSpy = spyOn(navigator.clipboard, "writeText").mockResolvedValue();

    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("cURL")).toBeInTheDocument();
    });

    const curlBtn = canvas.getByText("cURL");
    await curlBtn.click();

    await waitFor(() => expect(writeSpy).toHaveBeenCalledOnce());
    const curlCmd = writeSpy.mock.calls[0][0];
    expect(curlCmd).toContain("curl -X GET");
    expect(curlCmd).toContain("op.example.com");
  },
};

export const NoMoreFields = {
  render: () => html`<cts-log-entry .entry=${ENTRY_NO_MORE}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Test passed")).toBeInTheDocument();
    });

    // No `more` payload → no disclosure toggle should render at all.
    expect(canvasElement.querySelector(".moreBtn")).toBeNull();
  },
};

export const BlockEntry = {
  render: () => html`<cts-log-entry .entry=${BLOCK_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const item = canvasElement.querySelector(".logItem");
      expect(item).toBeTruthy();
      expect(item.classList.contains("is-block")).toBe(true);

      // The block-membership cue is a ::before pseudo-element rather
      // than a real border-left, so blocked rows and non-blocked rows
      // share the same content start position. Assert the pseudo's
      // width AND a non-transparent background so a regression that
      // drops the orange-400 token is caught.
      const beforeStyle = getComputedStyle(item, "::before");
      expect(beforeStyle.content).toBe('""');
      expect(beforeStyle.position).toBe("absolute");
      expect(beforeStyle.width).toBe("3px");
      expect(beforeStyle.backgroundColor).not.toBe("rgba(0, 0, 0, 0)");
      expect(beforeStyle.backgroundColor).not.toBe("transparent");

      // Inline alignment regression guard: the row itself must NOT
      // carry a real border-left (any width > 0 would push content).
      const itemStyle = getComputedStyle(item);
      expect(itemStyle.borderLeftWidth).toBe("0px");
    });
  },
};

export const UploadRequired = {
  render: () => html`<cts-log-entry .entry=${UPLOAD_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("IMAGE")).toBeInTheDocument();
    });

    expect(canvas.getByText("REVIEW")).toBeInTheDocument();
  },
};

export const ClickMoreHttpRequest = {
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("REQUEST")).toBeInTheDocument();
    });

    // Block hidden initially
    expect(canvasElement.querySelector(".moreInfo")).toBeNull();

    // Click More to reveal request details
    const moreBtn = canvasElement.querySelector(".moreBtn button");
    if (!moreBtn) throw new Error(".moreBtn button did not render");
    await moreBtn.click();

    /** @type {Element | null | undefined} */
    let moreInfo;
    await waitFor(() => {
      moreInfo = canvasElement.querySelector(".moreInfo");
      expect(moreInfo).toBeTruthy();
    });
    if (!moreInfo) throw new Error(".moreInfo did not appear");

    // Revealed content includes method, URL substring, and a header key
    const revealedText = moreInfo.textContent;
    expect(revealedText).toContain("GET");
    expect(revealedText).toContain("op.example.com/authorize");
    expect(revealedText).toContain("Accept");

    // Click again to collapse
    await moreBtn.click();
    await waitFor(() => {
      expect(canvasElement.querySelector(".moreInfo")).toBeNull();
    });
  },
};

export const ClickMoreHttpResponse = {
  render: () => html`<cts-log-entry .entry=${HTTP_RESPONSE_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("RESPONSE")).toBeInTheDocument();
    });

    // Block hidden initially
    expect(canvasElement.querySelector(".moreInfo")).toBeNull();

    // Click More to reveal response details
    const moreBtn = canvasElement.querySelector(".moreBtn button");
    if (!moreBtn) throw new Error(".moreBtn button did not render");
    await moreBtn.click();

    /** @type {Element | null | undefined} */
    let moreInfo;
    await waitFor(() => {
      moreInfo = canvasElement.querySelector(".moreInfo");
      expect(moreInfo).toBeTruthy();
    });
    if (!moreInfo) throw new Error(".moreInfo did not appear");

    // Revealed content includes status code and a body field
    const revealedText = moreInfo.textContent;
    expect(revealedText).toContain("200");
    expect(revealedText).toContain("access_token");

    // Click again to collapse
    await moreBtn.click();
    await waitFor(() => {
      expect(canvasElement.querySelector(".moreInfo")).toBeNull();
    });
  },
};

/**
 * U7: real-shape HTTP request entry — payload lives at the entry top level
 * (`request_uri`, `request_method`, `request_headers`, `request_body`),
 * not under a `more: {}` envelope. Before U7 the Details button never
 * rendered against this shape because `entry.more` was undefined and the
 * component short-circuited; the new `extractMoreFields` helper synthesises
 * the disclosure view by stripping the envelope keys (mirror of
 * LogEntryHelper.visibleFields). This story is the regression guard.
 */
export const ClickMoreRealHttpRequest = {
  render: () => html`<cts-log-entry .entry=${REAL_HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("REQUEST")).toBeInTheDocument();
    });

    const moreBtn = canvasElement.querySelector(".moreBtn button");
    if (!moreBtn) throw new Error(".moreBtn button did not render against real-shape entry");
    await moreBtn.click();

    /** @type {Element | null | undefined} */
    let moreInfo;
    await waitFor(() => {
      moreInfo = canvasElement.querySelector(".moreInfo");
      expect(moreInfo).toBeTruthy();
    });
    if (!moreInfo) throw new Error(".moreInfo did not appear");

    // Top-level payload fields surface in the panel — sentence-cased via
    // humanizeKey ("request_uri" → "Request uri"). The data-key attribute
    // preserves the raw key for tooling.
    expect(canvasElement.querySelector('[data-key="request_uri"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-key="request_method"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-key="request_headers"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-key="request_body"]')).toBeTruthy();
    expect(moreInfo.textContent).toContain("oidcc-provider");
    expect(moreInfo.textContent).toContain("GET");

    // Envelope keys (src, testId, time, http, testName) must NOT leak into
    // the disclosure — they are rendered elsewhere in the row or are
    // per-test metadata identical across every entry of the test.
    expect(canvasElement.querySelector('[data-key="src"]')).toBeNull();
    expect(canvasElement.querySelector('[data-key="testId"]')).toBeNull();
    expect(canvasElement.querySelector('[data-key="time"]')).toBeNull();
    expect(canvasElement.querySelector('[data-key="http"]')).toBeNull();
    expect(canvasElement.querySelector('[data-key="testName"]')).toBeNull();
  },
};

/**
 * U7: real-shape HTTP response entry — same shape contract as
 * REAL_HTTP_REQUEST_ENTRY, validating the response side renders top-level
 * payload (`response_status_code`, `response_body`, `response_headers`)
 * without ever invoking the legacy `more: {}` fast path.
 */
export const ClickMoreRealHttpResponse = {
  render: () => html`<cts-log-entry .entry=${REAL_HTTP_RESPONSE_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("RESPONSE")).toBeInTheDocument();
    });

    const moreBtn = canvasElement.querySelector(".moreBtn button");
    if (!moreBtn) throw new Error(".moreBtn button did not render against real-shape entry");
    await moreBtn.click();

    /** @type {Element | null | undefined} */
    let moreInfo;
    await waitFor(() => {
      moreInfo = canvasElement.querySelector(".moreInfo");
      expect(moreInfo).toBeTruthy();
    });
    if (!moreInfo) throw new Error(".moreInfo did not appear");

    expect(canvasElement.querySelector('[data-key="response_status_code"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-key="response_body"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-key="response_headers"]')).toBeTruthy();
    expect(moreInfo.textContent).toContain("authorization_endpoint");
    expect(moreInfo.textContent).toContain("200");
  },
};

/**
 * U7 boundary: an entry whose only fields are envelope keys (`_id`, `src`,
 * `time`, `result`, plus per-test metadata like `baseUrl`/`variant`/`planId`)
 * has nothing to disclose. The Details button must NOT render — surfacing
 * an empty disclosure for every plain success row would re-introduce the
 * noise the strip set was designed to remove.
 */
export const EnvelopeOnlyEntryHasNoMoreButton = {
  render: () => html`<cts-log-entry .entry=${ENVELOPE_ONLY_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Test passed")).toBeInTheDocument();
    });
    expect(canvasElement.querySelector(".moreBtn")).toBeNull();
    expect(canvasElement.querySelector(".moreInfo")).toBeNull();
  },
};

/**
 * R30: Failure entry whose `more` payload carries `expected` and `actual` keys.
 * Verifies (a) the labels read "Expected (per spec)" and "Actual (received)",
 * (b) expected always renders before actual even when declared in reverse
 * order on the source object, and (c) each `<dt>` gets the per-kind class hook.
 */
export const ExpectedVsActualEntry = {
  render: () => html`<cts-log-entry .entry=${ENTRY_EXPECTED_VS_ACTUAL}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("FAILURE")).toBeInTheDocument();
    });

    const moreBtn = canvasElement.querySelector(".moreBtn button");
    if (!moreBtn) throw new Error(".moreBtn button did not render");
    await moreBtn.click();

    /** @type {Element | null | undefined} */
    let moreInfo;
    await waitFor(() => {
      moreInfo = canvasElement.querySelector(".moreInfo");
      expect(moreInfo).toBeTruthy();
    });
    if (!moreInfo) throw new Error(".moreInfo did not appear");

    // Both labels are rendered.
    expect(canvas.getByText("Expected (per spec)")).toBeInTheDocument();
    expect(canvas.getByText("Actual (received)")).toBeInTheDocument();

    // Per-kind class hooks are applied so CSS can target each kind.
    const dts = moreInfo.querySelectorAll("dt");
    expect(dts.length).toBe(2);
    expect(dts[0].classList.contains("moreInfo-key--expected")).toBe(true);
    expect(dts[1].classList.contains("moreInfo-key--actual")).toBe(true);

    // Expected renders before actual even though the source declared actual
    // first — this is the helper's reordering behavior under test.
    expect(dts[0].getAttribute("data-key")).toBe("expected");
    expect(dts[1].getAttribute("data-key")).toBe("actual");

    // Values follow their labels in source order within each row.
    const dds = moreInfo.querySelectorAll("dd");
    expect(dds[0].textContent).toContain("payments");
    expect(dds[1].textContent).toContain("openid profile");
  },
};

/**
 * R30: Suffix-variant pattern. `expected_consent_id` and `actual_http_method`
 * are classified by prefix and carry the humanized suffix in their label, while
 * `requested_consent_id` (no `expected_` / `actual_` prefix) stays as "other".
 */
export const ExpectedActualSuffixVariants = {
  render: () => html`<cts-log-entry .entry=${ENTRY_SUFFIX_VARIANTS}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("FAILURE")).toBeInTheDocument();
    });

    const moreBtn = canvasElement.querySelector(".moreBtn button");
    if (!moreBtn) throw new Error(".moreBtn button did not render");
    await moreBtn.click();

    /** @type {Element | null | undefined} */
    let moreInfo;
    await waitFor(() => {
      moreInfo = canvasElement.querySelector(".moreInfo");
      expect(moreInfo).toBeTruthy();
    });
    if (!moreInfo) throw new Error(".moreInfo did not appear");

    // The expected_ and actual_ prefixed keys carry their suffix into the label.
    expect(canvas.getByText("Expected (per spec) — Consent id")).toBeInTheDocument();
    expect(canvas.getByText("Actual (received) — Http method")).toBeInTheDocument();

    // The unprefixed key stays as "other" with humanized text — not labeled
    // as expected/actual even though it shares the "consent_id" suffix.
    expect(canvas.getByText("Requested consent id")).toBeInTheDocument();

    const dts = moreInfo.querySelectorAll("dt");
    // Order: expected_consent_id, actual_http_method, requested_consent_id.
    expect(dts[0].getAttribute("data-key")).toBe("expected_consent_id");
    expect(dts[0].classList.contains("moreInfo-key--expected")).toBe(true);
    expect(dts[1].getAttribute("data-key")).toBe("actual_http_method");
    expect(dts[1].classList.contains("moreInfo-key--actual")).toBe(true);
    expect(dts[2].getAttribute("data-key")).toBe("requested_consent_id");
    expect(dts[2].classList.contains("moreInfo-key--other")).toBe(true);
  },
};

/**
 * R30 substring-boundary regression: a key like `unexpected_field` contains
 * the substring "expected" but does NOT start with `expected_`. The classifier
 * must keep it in the "other" bucket. This story exists to fail loudly if
 * someone refactors the rule from `startsWith("expected_")` to a looser
 * `includes("expected")`. The mitigation is called out explicitly in the R30
 * plan's Risks & Dependencies table.
 */
export const SubstringBoundaryRespectsPrefix = {
  render: () => html`<cts-log-entry .entry=${ENTRY_SUBSTRING_BOUNDARY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("WARNING")).toBeInTheDocument();
    });

    const moreBtn = canvasElement.querySelector(".moreBtn button");
    if (!moreBtn) throw new Error(".moreBtn button did not render");
    await moreBtn.click();

    /** @type {Element | null | undefined} */
    let moreInfo;
    await waitFor(() => {
      moreInfo = canvasElement.querySelector(".moreInfo");
      expect(moreInfo).toBeTruthy();
    });
    if (!moreInfo) throw new Error(".moreInfo did not appear");

    const dts = moreInfo.querySelectorAll("dt");
    expect(dts.length).toBe(1);
    // Substring "expected" inside "unexpected_field" must NOT trigger the
    // expected/actual classification — the rule is exact-match or strict
    // prefix, not substring.
    expect(dts[0].getAttribute("data-key")).toBe("unexpected_field");
    expect(dts[0].classList.contains("moreInfo-key--other")).toBe(true);
    expect(dts[0].classList.contains("moreInfo-key--expected")).toBe(false);
    // Label is humanized only, no "Expected (per spec)" prefix.
    expect(dts[0].textContent.trim()).toBe("Unexpected field");
  },
};

/**
 * R31: a log message containing a single long unbreakable URL must wrap
 * inside its grid track instead of pushing the body column wider than the
 * container. The narrow wrapper (320 px) is well below the URL's intrinsic
 * width; with `overflow-wrap: anywhere` + `min-width: 0` on the body track,
 * the URL breaks at any character to fit. The play function asserts that
 * the rendered body width does not exceed the wrapper width — i.e. the
 * URL did not push the layout sideways.
 */
const LONG_URL_ENTRY = {
  _id: "entry-long-url",
  testId: "test-abc",
  src: "BuildRedirectUri",
  time: NOW - 6000,
  msg: "Built redirect URI https://op.example.com/very/long/path/with/many/segments/and/an/exhaustively/verbose/query?client_id=conformance-suite-acme-corp-fapi2-final&scope=openid%20profile%20email%20payments&state=01J9X2KQZWZW8Y6V0E5T7H1P9R&nonce=01J9X2KQZWZW8Y6V0E5T7H1P9S&redirect_uri=https%3A%2F%2Fclient.example.com%2Fcallback",
  result: "INFO",
};

export const LongUrlMessage = {
  decorators: [
    (Story) => html`
      <div
        data-testid="long-url-wrapper"
        style="width: 320px; max-width: 100%; border: 1px dashed var(--ink-300); resize: horizontal; overflow: auto;"
      >
        ${Story()}
      </div>
    `,
  ],
  render: () => html`<cts-log-entry .entry=${LONG_URL_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText(/Built redirect URI/)).toBeInTheDocument();
    });

    const wrapper = canvasElement.querySelector('[data-testid="long-url-wrapper"]');
    if (!wrapper) throw new Error("wrapper did not render");
    const body = canvasElement.querySelector(".logBody");
    if (!body) throw new Error(".logBody did not render");

    // R31: with overflow-wrap: anywhere + min-width: 0, the URL wraps
    // inside the body column. The body's rendered width must therefore
    // stay within the wrapper's content box — no horizontal overflow,
    // no off-screen URL push.
    const wrapperBox = wrapper.getBoundingClientRect();
    const bodyBox = body.getBoundingClientRect();
    expect(bodyBox.width).toBeLessThanOrEqual(wrapperBox.width);

    // Sanity: the message text actually rendered (didn't get clipped to
    // empty). A failing wrap rule would still leave the text in the DOM,
    // so this is a presence check, not the wrap proof.
    expect(body.textContent).toContain("op.example.com");
  },
};

// --- U3: container-query reflow ----------------------------------------
// Plan: docs/plans/2026-04-26-004-feat-log-entry-container-query-reflow-plan.md
// Each story wraps the entry in a fixed-width container so the host's
// inline-size matches the named breakpoint without relying on the
// storybook canvas width. The dashed border + `resize: horizontal` is a
// manual-QA convenience: drag the wrapper edge in storybook to watch the
// layout flip at 640px without re-running the story.
//
// Wrappers at >= 640 px also declare `container-type: inline-size` +
// `container-name: ctsLogViewer` to stand in for cts-log-viewer, which in
// production publishes that same named container on its host (see
// cts-log-viewer.js). Without the name, the wide-layout rule in
// cts-log-entry.js (`@container ctsLogViewer (min-width: 640px) { ... }`)
// finds no matching ancestor and is silently skipped — named container
// queries do not warn when no container matches — leaving the entry stuck
// in its 2-track small-layout default and these stories asserting a 5-track
// grid that never materializes. Sub-640 px stories (SmallContainerLayout,
// EntryIdAtSmallLayout) omit the declaration because the small layout is
// the rule-less default that applies whenever the wide rule does not match.

/**
 * Below the 640px container-query threshold the row collapses to:
 *   row 1: meta (timestamp · severity · http) — wraps via flex
 *   row 2: body + actions
 *   row 3: footer (more panel) when expanded
 * The grid uses `grid-template-areas` so the existing inner classes carry
 * the layout without a markup change at small widths.
 */
export const SmallContainerLayout = {
  decorators: [
    (Story) => html`
      <div
        style="width: 360px; max-width: 100%; border: 1px dashed var(--ink-300); resize: horizontal; overflow: auto;"
      >
        ${Story()}
      </div>
    `,
  ],
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("REQUEST")).toBeInTheDocument();
    });

    const item = canvasElement.querySelector(".logItem");
    if (!item) throw new Error(".logItem did not render");
    const style = getComputedStyle(item);

    // Default (small) layout uses two grid columns: 1fr + auto.
    const tracks = style.gridTemplateColumns.split(/\s+/).filter(Boolean);
    expect(tracks.length).toBe(2);

    // Named grid areas drive the row layout below 640px. Browsers serialize
    // grid-template-areas as quoted strings; assert the metaRow row exists.
    expect(style.gridTemplateAreas).toContain("metaRow");

    // The .logMetaRow wrapper participates in layout (not display: contents)
    // at small widths so its three children stack as a flex row.
    const metaRow = canvasElement.querySelector(".logMetaRow");
    if (!metaRow) throw new Error(".logMetaRow did not render");
    expect(getComputedStyle(metaRow).display).toBe("flex");
  },
};

/**
 * Exactly at the 640px container-query threshold the wide layout takes
 * effect — verifies the boundary inclusively (`min-width: 640px`).
 */
export const BoundaryContainerLayout = {
  decorators: [
    (Story) => html`
      <div
        style="width: 640px; max-width: 100%; container-type: inline-size; container-name: ctsLogViewer;"
      >
        ${Story()}
      </div>
    `,
  ],
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("REQUEST")).toBeInTheDocument();
    });

    const item = canvasElement.querySelector(".logItem");
    if (!item) throw new Error(".logItem did not render");
    const style = getComputedStyle(item);

    // Five-track grid: 110px / 70px / 60px / 1fr / auto.
    const tracks = style.gridTemplateColumns.split(/\s+/).filter(Boolean);
    expect(tracks.length).toBe(5);

    // Wide layout flattens the .logMetaRow wrapper via display: contents so
    // its children rejoin the parent grid as direct cells.
    const metaRow = canvasElement.querySelector(".logMetaRow");
    if (!metaRow) throw new Error(".logMetaRow did not render");
    expect(getComputedStyle(metaRow).display).toBe("contents");
  },
};

/** Tablet width — well past the 640px threshold. */
export const TabletContainerLayout = {
  decorators: [
    (Story) => html`
      <div
        style="width: 720px; max-width: 100%; container-type: inline-size; container-name: ctsLogViewer;"
      >
        ${Story()}
      </div>
    `,
  ],
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("REQUEST")).toBeInTheDocument();
    });

    const item = canvasElement.querySelector(".logItem");
    if (!item) throw new Error(".logItem did not render");
    const tracks = getComputedStyle(item).gridTemplateColumns.split(/\s+/).filter(Boolean);
    expect(tracks.length).toBe(5);
  },
};

/**
 * Desktop width — the U16-era 5-column track is the designed-for surface.
 * Carries the explicit track-count assertion that used to live on the
 * default SuccessEntry story (which is now layout-neutral because the
 * canvas width can no longer be assumed wide).
 */
export const DesktopContainerLayout = {
  decorators: [
    (Story) => html`
      <div
        style="width: 1280px; max-width: 100%; container-type: inline-size; container-name: ctsLogViewer;"
      >
        ${Story()}
      </div>
    `,
  ],
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("REQUEST")).toBeInTheDocument();
    });

    const item = canvasElement.querySelector(".logItem");
    if (!item) throw new Error(".logItem did not render");
    const style = getComputedStyle(item);

    // Five-column track: 92px (logTime) / max-content (severity) /
    // max-content (http) / 1fr (body) / auto (actions). The two
    // max-content columns resolve to the rendered badge widths, so the
    // assertion below pins only the fixed timestamp column and the column
    // count — relying on resolved pixel values for max-content tracks
    // would couple this test to badge typography.
    const tracks = style.gridTemplateColumns.split(/\s+/).filter(Boolean);
    expect(tracks.length).toBe(5);
    expect(tracks[0]).toBe("92px");
  },
};

/**
 * U6: at small container widths (< 640 px) the reference chip lives in
 * its own grid row above the body, between the meta row and the body
 * row, so it never crowds the message text in the narrow column.
 */
export const EntryIdAtSmallLayout = {
  decorators: [(Story) => html` <div style="width: 360px; max-width: 100%;">${Story()}</div> `],
  render: () =>
    html`<cts-log-entry
      .entry=${SUCCESS_ENTRY}
      reference-id="LOG-0007"
      test-id="storybook-test"
    ></cts-log-entry>`,
  async play({ canvasElement }) {
    const host = await waitFor(() => {
      const el = canvasElement.querySelector("cts-log-entry");
      if (!el) throw new Error("cts-log-entry did not render");
      return /** @type {HTMLElement} */ (el);
    });

    // The host carries the referenceId as its `id` so URL fragments resolve.
    expect(host.id).toBe("LOG-0007");

    // The small-layout slot is visible; the inline slot is collapsed.
    const row = canvasElement.querySelector(".logIdRow");
    const inline = canvasElement.querySelector(".logIdInline");
    if (!row) throw new Error(".logIdRow did not render");
    if (!inline) throw new Error(".logIdInline did not render");
    expect(getComputedStyle(row).display).not.toBe("none");
    expect(getComputedStyle(inline).display).toBe("none");

    // The visible chip carries the right reference label.
    const chip = row.querySelector('[data-testid="log-entry-id-chip"]');
    if (!chip) throw new Error("chip did not render in row slot");
    expect(chip.textContent).toContain("LOG-0007");
  },
};

/**
 * U6: at desktop container widths (≥ 640 px) the chip renders inline
 * inside the body cell, after the source label and before the message
 * text. This is the compact, "in-flow" placement.
 */
export const EntryIdAtDesktopLayout = {
  decorators: [
    (Story) => html`
      <div
        style="width: 1280px; max-width: 100%; container-type: inline-size; container-name: ctsLogViewer;"
      >
        ${Story()}
      </div>
    `,
  ],
  render: () =>
    html`<cts-log-entry
      .entry=${SUCCESS_ENTRY}
      reference-id="LOG-0042"
      test-id="storybook-test"
    ></cts-log-entry>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector("cts-log-entry");
      if (!el) throw new Error("cts-log-entry did not render");
      return el;
    });

    const row = canvasElement.querySelector(".logIdRow");
    const inline = canvasElement.querySelector(".logIdInline");
    if (!row) throw new Error(".logIdRow did not render");
    if (!inline) throw new Error(".logIdInline did not render");
    expect(getComputedStyle(row).display).toBe("none");
    expect(getComputedStyle(inline).display).not.toBe("none");

    // The inline chip lives inside .logBody (so it sits between the
    // source label and the message text in the body's flow).
    const body = canvasElement.querySelector(".logBody");
    if (!body) throw new Error(".logBody did not render");
    expect(body.contains(inline)).toBe(true);
  },
};

/**
 * U6: the entry's outer wrapper (the host element) carries scroll-
 * margin-top derived from --status-bar-height + --banner-height so
 * deep-link navigation lands the row visibly below the sticky chrome.
 * The story sets a 56 px status bar height on documentElement and
 * asserts the resolved scroll-margin-top reflects it.
 */
export const EntryAnchorScrollMargin = {
  render: () =>
    html`<cts-log-entry
      .entry=${SUCCESS_ENTRY}
      reference-id="LOG-0099"
      test-id="storybook-test"
    ></cts-log-entry>`,
  async play({ canvasElement }) {
    // Simulate a sticky bar publishing its height — U2's mechanic.
    const prev = document.documentElement.style.getPropertyValue("--status-bar-height");
    document.documentElement.style.setProperty("--status-bar-height", "56px");
    try {
      const host = await waitFor(() => {
        const el = canvasElement.querySelector("cts-log-entry");
        if (!el) throw new Error("cts-log-entry did not render");
        return /** @type {HTMLElement} */ (el);
      });

      const margin = getComputedStyle(host).scrollMarginTop;
      // Resolved px should include the 56 px from the status bar plus
      // an additional spacing-token allowance (--space-4). Anything ≥ 56
      // is acceptable; the actual value will be 56 + 16 (--space-4) for
      // a total of 72 px.
      const px = parseFloat(margin);
      expect(Number.isFinite(px)).toBe(true);
      expect(px).toBeGreaterThanOrEqual(56);
    } finally {
      if (prev) document.documentElement.style.setProperty("--status-bar-height", prev);
      else document.documentElement.style.removeProperty("--status-bar-height");
    }
  },
};
