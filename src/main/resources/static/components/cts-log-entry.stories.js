import { html } from "lit";
import { expect, within, waitFor, fn } from "storybook/test";
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

export const SuccessEntry = {
  render: () => html`<cts-log-entry .entry=${SUCCESS_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("SUCCESS")).toBeInTheDocument();
    });
    expect(canvas.getByText("Server configuration valid")).toBeInTheDocument();
    expect(canvas.getByText("CheckServerConfiguration")).toBeInTheDocument();

    const badge = canvasElement.querySelector('cts-badge[variant="success"]');
    expect(badge).toBeTruthy();
  },
};

export const FailureEntry = {
  render: () => html`<cts-log-entry .entry=${FAILURE_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("FAILURE")).toBeInTheDocument();
    });

    const badge = canvasElement.querySelector('cts-badge[variant="failure"]');
    expect(badge).toBeTruthy();

    expect(canvas.getByText("OIDCC-3.1.3.7-6")).toBeInTheDocument();
    expect(canvas.getByText("OIDCC-3.1.3.7-7")).toBeInTheDocument();
  },
};

export const WarningEntry = {
  render: () => html`<cts-log-entry .entry=${WARNING_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("WARNING")).toBeInTheDocument();
    });

    const badge = canvasElement.querySelector('cts-badge[variant="warning"]');
    expect(badge).toBeTruthy();

    expect(canvas.getByText("OIDCC-3.1.3.3")).toBeInTheDocument();
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

    const moreBtn = canvas.getByText("More");
    await moreBtn.click();

    await waitFor(() => {
      expect(canvasElement.querySelector(".moreInfo")).toBeTruthy();
    });

    expect(canvas.getByText("access_token")).toBeInTheDocument();
    expect(canvas.getByText("token_type")).toBeInTheDocument();

    await moreBtn.click();

    await waitFor(() => {
      expect(canvasElement.querySelector(".moreInfo")).toBeNull();
    });
  },
};

export const CopyAsCurl = {
  render: () => html`<cts-log-entry .entry=${HTTP_REQUEST_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const mockWriteText = fn().mockResolvedValue(undefined);
    const originalClipboard = navigator.clipboard;
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: mockWriteText },
      writable: true,
      configurable: true,
    });

    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("cURL")).toBeInTheDocument();
    });

    const curlBtn = canvas.getByText("cURL");
    await curlBtn.click();

    expect(mockWriteText).toHaveBeenCalledOnce();
    const curlCmd = mockWriteText.mock.calls[0][0];
    expect(curlCmd).toContain("curl -X GET");
    expect(curlCmd).toContain("op.example.com");

    // Restore
    Object.defineProperty(navigator, "clipboard", {
      value: originalClipboard,
      writable: true,
      configurable: true,
    });
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
      // Block entries are visually distinguished by a colored left border.
      // Assert both the width/style AND a non-transparent color so a regression
      // that drops the color (leaving a black "3px solid") is caught.
      const style = getComputedStyle(item);
      expect(style.borderLeftWidth).toBe("3px");
      expect(style.borderLeftStyle).toBe("solid");
      expect(style.borderLeftColor).not.toBe("rgba(0, 0, 0, 0)");
      expect(style.borderLeftColor).not.toBe("transparent");
    });
  },
};

export const UploadRequired = {
  render: () => html`<cts-log-entry .entry=${UPLOAD_ENTRY}></cts-log-entry>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("IMAGE REQUIRED")).toBeInTheDocument();
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

    let moreInfo;
    await waitFor(() => {
      moreInfo = canvasElement.querySelector(".moreInfo");
      expect(moreInfo).toBeTruthy();
    });

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

    let moreInfo;
    await waitFor(() => {
      moreInfo = canvasElement.querySelector(".moreInfo");
      expect(moreInfo).toBeTruthy();
    });

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
