import { html } from "lit";
import { expect, within, waitFor, spyOn } from "storybook/test";
import "./cts-log-entry.js";

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

export const HttpRequestEntry = {
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("REQUEST")).toBeInTheDocument();
    });

    const curlBtn = canvas.getByText("cURL");
    expect(curlBtn).toBeInTheDocument();

    const moreBtn = canvas.getByText("More");
    expect(moreBtn).toBeInTheDocument();
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
  },
};

export const ClickMoreToggle = {
  render: () => html`<cts-log-entry .entry=${ENTRY_WITH_MORE}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("More")).toBeInTheDocument();
    });

    expect(canvasElement.querySelector(".moreInfo")).toBeNull();

    expect(canvasElement.querySelector("cts-button.moreBtn")).toBeTruthy();
    const countBadge = canvasElement.querySelector("cts-badge[count]");
    expect(countBadge).toBeTruthy();
    expect(countBadge.textContent.trim()).toBe(String(Object.keys(ENTRY_WITH_MORE.more).length));

    const moreBtn = canvas.getByText("More");
    await moreBtn.click();

    await waitFor(() => {
      expect(canvasElement.querySelector(".moreInfo")).toBeTruthy();
    });

    // R30: keys are now humanized for display ("access_token" → "Access token").
    // The original key is preserved on the dt's data-key attribute for tooling.
    expect(canvas.getByText("Access token")).toBeInTheDocument();
    expect(canvas.getByText("Token type")).toBeInTheDocument();
    expect(canvasElement.querySelector('[data-key="access_token"]')).toBeTruthy();

    await moreBtn.click();

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

    expect(canvas.queryByText("More")).toBeNull();
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
    const moreBtn = canvas.getByText("More");
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
    const moreBtn = canvas.getByText("More");
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

    const moreBtn = canvas.getByText("More");
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

    const moreBtn = canvas.getByText("More");
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

    const moreBtn = canvas.getByText("More");
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

// --- U3: container-query reflow ----------------------------------------
// Plan: docs/plans/2026-04-26-004-feat-log-entry-container-query-reflow-plan.md
// Each story wraps the entry in a fixed-width container so the host's
// inline-size matches the named breakpoint without relying on the
// storybook canvas width. The dashed border + `resize: horizontal` is a
// manual-QA convenience: drag the wrapper edge in storybook to watch the
// layout flip at 640px without re-running the story.

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
  decorators: [(Story) => html` <div style="width: 640px; max-width: 100%;">${Story()}</div> `],
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
  decorators: [(Story) => html` <div style="width: 720px; max-width: 100%;">${Story()}</div> `],
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
  decorators: [(Story) => html` <div style="width: 1280px; max-width: 100%;">${Story()}</div> `],
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("REQUEST")).toBeInTheDocument();
    });

    const item = canvasElement.querySelector(".logItem");
    if (!item) throw new Error(".logItem did not render");
    const style = getComputedStyle(item);

    // U16-era five-column track: 110px / 70px / 60px / 1fr / auto.
    const tracks = style.gridTemplateColumns.split(/\s+/).filter(Boolean);
    expect(tracks.length).toBe(5);
    expect(style.gridTemplateColumns).toMatch(/110px\s+70px\s+60px/);
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
  decorators: [(Story) => html` <div style="width: 1280px; max-width: 100%;">${Story()}</div> `],
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
