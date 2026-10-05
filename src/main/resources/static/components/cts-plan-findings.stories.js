import { html } from "lit";
import { expect, userEvent, waitFor } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_PLAN_FINDINGS } from "@fixtures/mock-plan-findings.js";
import "./cts-plan-findings.js";

export default {
  title: "Components/cts-plan-findings",
  component: "cts-plan-findings",
};

const FINDINGS = MOCK_PLAN_FINDINGS.findings;

const rows = (canvasElement) => [...canvasElement.querySelectorAll('[data-testid="plan-finding"]')];

export const Default = {
  render: () => html`<cts-plan-findings .findings=${FINDINGS}></cts-plan-findings>`,
  async play({ canvasElement }) {
    const card = canvasElement.querySelector('[data-testid="plan-findings"]');
    expect(card.open).toBe(true);
    expect(card.querySelector("summary").textContent.trim()).toBe(
      "Summary of results: 1 failure, 1 warning, 1 image to review",
    );

    const [failure, warning, image] = rows(canvasElement);
    expect(failure.querySelector("cts-badge").getAttribute("variant")).toBe("fail");
    expect(warning.querySelector("cts-badge").getAttribute("variant")).toBe("warn");
    expect(image.querySelector("cts-badge").getAttribute("variant")).toBe("review");
    expect(image.querySelector("cts-badge").getAttribute("label")).toBe("IMAGE");

    // A finding in one module links straight to its entry, with the repeat
    // count when the run logged it more than once.
    const single = warning.querySelector('[data-testid="plan-finding-link"]');
    expect(warning.querySelector("details")).toBeNull();
    expect(single.getAttribute("href")).toBe(
      "log-detail.html?log=test-inst-002#entry-test-inst-002-cccc",
    );
    expect(warning.textContent).toContain("×2");

    // Requirement chips ride along on the row.
    expect(failure.querySelector('cts-badge[variant="secondary"]').getAttribute("label")).toBe(
      "OIDCD-4.3",
    );
  },
};

export const SharedAcrossModules = {
  render: () => html`<cts-plan-findings .findings=${FINDINGS}></cts-plan-findings>`,
  async play({ canvasElement }) {
    const [failure] = rows(canvasElement);
    const details = failure.querySelector("details");
    expect(details.open).toBe(false);
    expect(failure.querySelector(".planFindingWhere").textContent.trim()).toMatch(/^in 2 modules/);
    expect(failure.querySelector(".planFindingSrc").textContent).toBe("CheckDiscEndpointIssuer");

    await userEvent.click(details.querySelector("summary"));
    expect(details.open).toBe(true);

    const links = [...failure.querySelectorAll('[data-testid="plan-finding-link"]')];
    expect(links.map((a) => a.textContent.trim())).toEqual([
      "oidcc-server",
      "oidcc-ensure-redirect-uri-in-authorization-request",
    ]);
    expect(links.map((a) => a.getAttribute("href"))).toEqual([
      "log-detail.html?log=test-inst-001#entry-test-inst-001-aaaa",
      "log-detail.html?log=test-inst-003#entry-test-inst-003-bbbb",
    ]);
    // Distinct module names need no variant to tell them apart.
    expect(failure.querySelector(".planFindingVariant")).toBeNull();
  },
};

const SAME_MODULE_TWICE = [
  {
    kind: "FAILURE",
    src: "CheckTokenEndpointReturnedJsonContentType",
    msg: "token endpoint did not return application/json",
    requirements: [],
    occurrences: [
      {
        moduleIndex: 0,
        testModule: "oidcc-server",
        variant: { client_auth_type: "client_secret_basic" },
        testId: "run-basic",
        entryId: "run-basic-1",
        count: 1,
      },
      {
        moduleIndex: 1,
        testModule: "oidcc-server",
        variant: { client_auth_type: "private_key_jwt" },
        testId: "run-pkjwt",
        entryId: "run-pkjwt-1",
        count: 1,
      },
    ],
  },
];

export const SameModuleUnderTwoVariants = {
  render: () => html`<cts-plan-findings .findings=${SAME_MODULE_TWICE}></cts-plan-findings>`,
  async play({ canvasElement }) {
    const [failure] = rows(canvasElement);
    const variants = [...failure.querySelectorAll(".planFindingVariant")];
    expect(variants.map((v) => v.textContent.trim())).toEqual([
      "(client_auth_type=client_secret_basic)",
      "(client_auth_type=private_key_jwt)",
    ]);
  },
};

const IMAGES_WITHOUT_MESSAGE = [
  {
    kind: "IMAGE",
    src: "ExpectErrorPage",
    requirements: [],
    occurrences: [
      {
        moduleIndex: 0,
        testModule: "oidcc-server",
        variant: {},
        testId: "run-1",
        entryId: "run-1-a",
        count: 1,
      },
    ],
  },
  {
    kind: "IMAGE",
    src: "_image-api",
    requirements: [],
    occurrences: [
      {
        moduleIndex: 0,
        testModule: "oidcc-server",
        variant: {},
        testId: "run-1",
        entryId: "run-1-b",
        count: 1,
      },
    ],
  },
];

export const ImagesWithoutAMessage = {
  render: () => html`<cts-plan-findings .findings=${IMAGES_WITHOUT_MESSAGE}></cts-plan-findings>`,
  async play({ canvasElement }) {
    const [placeholder, freeUpload] = rows(canvasElement);
    expect(placeholder.querySelector(".planFindingSrc").textContent).toBe("ExpectErrorPage");
    expect(placeholder.querySelector(".planFindingMsg").textContent).toBe(
      "uploaded image awaiting review",
    );
    // An image added on the upload page has no condition to name.
    expect(freeUpload.querySelector(".planFindingSrc")).toBeNull();
    expect(freeUpload.querySelector(".planFindingMsg").textContent).toBe("Uploaded image");
    expect(
      canvasElement.querySelector('[data-testid="plan-findings"] summary').textContent.trim(),
    ).toBe("Summary of results: 2 images to review");
  },
};

const CAPTURED_PAGES = [
  {
    kind: "PAGE",
    src: "ExpectResponseTypeErrorPage",
    msg: "Show an error page saying the response type is invalid",
    requirements: [],
    occurrences: [
      {
        moduleIndex: 0,
        testModule: "module-a",
        variant: {},
        testId: "run-a",
        entryId: "run-a-1",
        count: 1,
      },
      {
        moduleIndex: 1,
        testModule: "module-b",
        variant: {},
        testId: "run-b",
        entryId: "run-b-1",
        count: 1,
      },
    ],
  },
  {
    kind: "PAGE",
    src: "ExpectErrorPage",
    requirements: [],
    occurrences: [
      {
        moduleIndex: 0,
        testModule: "module-a",
        variant: {},
        testId: "run-a",
        entryId: "run-a-2",
        count: 1,
      },
    ],
  },
];

export const PagesCapturedByBrowserAutomation = {
  render: () => html`<cts-plan-findings .findings=${CAPTURED_PAGES}></cts-plan-findings>`,
  async play({ canvasElement }) {
    expect(
      canvasElement.querySelector('[data-testid="plan-findings"] summary').textContent.trim(),
    ).toBe("Summary of results: 2 captured pages to review");
    const [shared, bare] = rows(canvasElement);
    expect(shared.querySelector("cts-badge").getAttribute("variant")).toBe("review");
    expect(shared.querySelector("cts-badge").getAttribute("label")).toBe("PAGE");
    expect(bare.querySelector(".planFindingMsg").textContent).toBe(
      "page captured by browser automation, awaiting review",
    );
  },
};

// A 1x1 PNG, standing in for an uploaded screenshot.
const PIXEL = Uint8Array.from(
  atob(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
  ),
  (c) => c.charCodeAt(0),
);

export const ImageThumbnailsOpenAPopup = {
  parameters: {
    msw: {
      handlers: [
        http.get(
          "/api/plan/:planId/findings/:entryId/image",
          () => new HttpResponse(PIXEL, { headers: { "Content-Type": "image/png" } }),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-plan-findings plan-id="plan-abc-123" .findings=${FINDINGS}></cts-plan-findings>`,
  async play({ canvasElement }) {
    const [failure, , image] = rows(canvasElement);
    // Only an IMAGE finding has thumbnails, one per module run.
    expect(failure.querySelector('[data-testid="plan-finding-thumb"]')).toBeNull();
    const thumbs = image.querySelectorAll('[data-testid="plan-finding-thumb"]');
    expect(thumbs.length).toBe(1);
    expect(thumbs[0].querySelector("img").getAttribute("src")).toBe(
      "/api/plan/plan-abc-123/findings/test-inst-003-dddd/image",
    );

    await userEvent.click(thumbs[0]);

    // The popup says what the image is expected to show, where it was
    // uploaded, and shows the image itself.
    const modal = canvasElement.querySelector('[data-testid="plan-finding-image-modal"]');
    await waitFor(() => expect(modal.querySelector("dialog").open).toBe(true));
    const expected = modal.querySelector('[data-testid="plan-finding-expected"]');
    expect(expected.querySelector(".planFindingSrc").textContent).toBe(
      "ExpectRedirectUriMissingErrorPage",
    );
    expect(expected.querySelector(".planFindingMsg").textContent).toBe(
      "Show an error page saying the redirect URI is missing",
    );
    const link = modal.querySelector('[data-testid="plan-finding-viewed-link"]');
    expect(link.textContent).toBe("oidcc-ensure-redirect-uri-in-authorization-request");
    expect(link.getAttribute("href")).toBe(
      "log-detail.html?log=test-inst-003#entry-test-inst-003-dddd",
    );
    expect(modal.querySelector(".planFindingFullImage").getAttribute("src")).toBe(
      "/api/plan/plan-abc-123/findings/test-inst-003-dddd/image",
    );
  },
};

export const NoThumbnailsWithoutAPlan = {
  render: () => html`<cts-plan-findings .findings=${FINDINGS}></cts-plan-findings>`,
  async play({ canvasElement }) {
    expect(canvasElement.querySelector('[data-testid="plan-finding-thumb"]')).toBeNull();
  },
};

const MODULES = [
  {
    testModule: "module-passed",
    variant: {},
    instances: ["r1"],
    status: "FINISHED",
    result: "PASSED",
  },
  {
    testModule: "module-failed",
    variant: {},
    instances: ["r2"],
    status: "INTERRUPTED",
    result: "FAILED",
  },
  { testModule: "module-waiting", variant: {}, instances: ["r3"], status: "WAITING" },
  { testModule: "module-never-run-a", variant: {}, instances: [] },
  { testModule: "module-never-run-b", variant: {}, instances: [] },
  // Its latest run is not visible to this viewer: in no state to report.
  { testModule: "module-run-not-visible", variant: {}, instances: ["r6"] },
];

export const ModulesThatHaveNotFinished = {
  render: () => html`<cts-plan-findings .modules=${MODULES}></cts-plan-findings>`,
  async play({ canvasElement }) {
    expect(
      canvasElement.querySelector('[data-testid="plan-findings"] summary').textContent.trim(),
    ).toBe("Summary of results: 1 module interrupted, 1 module in progress, 2 modules not run");

    const [interrupted, inProgress, notRun] = rows(canvasElement);
    expect(interrupted.getAttribute("data-kind")).toBe("INTERRUPTED");
    expect(interrupted.querySelector("cts-badge").getAttribute("variant")).toBe("fail");
    expect(inProgress.querySelector("cts-badge").getAttribute("label")).toBe("IN PROGRESS");
    expect(notRun.querySelector("cts-badge").getAttribute("label")).toBe("NOT RUN");
    expect(notRun.querySelector(".planFindingMsg").textContent).toBe("Not run yet");

    // Each module links to its own row on the plan page, by plan position.
    expect(
      interrupted.querySelector('[data-testid="plan-finding-link"]').getAttribute("href"),
    ).toBe("#cts-module-1");
    const links = [...notRun.querySelectorAll('[data-testid="plan-finding-link"]')];
    expect(links.map((a) => a.textContent.trim())).toEqual([
      "module-never-run-a",
      "module-never-run-b",
    ]);
    expect(links.map((a) => a.getAttribute("href"))).toEqual(["#cts-module-3", "#cts-module-4"]);
    expect(canvasElement.textContent).not.toContain("module-run-not-visible");
    expect(canvasElement.textContent).not.toContain("module-passed");
  },
};

export const FindingsThenUnfinishedModules = {
  render: () =>
    html`<cts-plan-findings .findings=${FINDINGS} .modules=${MODULES}></cts-plan-findings>`,
  async play({ canvasElement }) {
    expect(rows(canvasElement).map((row) => row.getAttribute("data-kind"))).toEqual([
      "FAILURE",
      "WARNING",
      "IMAGE",
      "INTERRUPTED",
      "IN_PROGRESS",
      "NOT_RUN",
    ]);
  },
};

/**
 * Spy on the clipboard so the play function can read what a copy button
 * wrote; the test runner grants no real clipboard access.
 * @returns {{ written: string[], restore: () => void }}
 */
function spyClipboard() {
  /** @type {string[]} */
  const written = [];
  const clipboard = navigator.clipboard;
  const original = clipboard && clipboard.writeText;
  Object.defineProperty(navigator, "clipboard", {
    configurable: true,
    value: {
      writeText: (text) => {
        written.push(text);
        return Promise.resolve();
      },
    },
  });
  return {
    written,
    restore: () => {
      Object.defineProperty(navigator, "clipboard", { configurable: true, value: clipboard });
      if (clipboard && original) clipboard.writeText = original;
    },
  };
}

export const CopyAsTextAndDownloadCsv = {
  render: () =>
    html`<cts-plan-findings
      plan-id="plan-abc-123"
      .findings=${FINDINGS}
      .modules=${MODULES}
    ></cts-plan-findings>`,
  async play({ canvasElement }) {
    const spy = spyClipboard();
    try {
      await userEvent.click(
        canvasElement.querySelector('[data-testid="plan-findings-copy-text"] button'),
      );
      await waitFor(() => expect(spy.written.length).toBe(1));
      const text = spy.written[0];
      // One block per row: the headline first, then each finding with its
      // modules as absolute links, then the unfinished modules.
      expect(text.startsWith("Summary of results: 1 failure, 1 warning, 1 image to review")).toBe(
        true,
      );
      expect(text).toContain("FAILURE: CheckDiscEndpointIssuer\n");
      expect(text).toContain(
        "  Requirements: OIDCD-4.3\n  In 2 modules:\n  - oidcc-server (client_auth_type=client_secret_basic, response_type=code): http",
      );
      expect(text).toContain("#entry-test-inst-001-aaaa\n");
      expect(text).toContain("WARNING: EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys");
      expect(text).toContain("(x2)");
      expect(text).toContain("NOT RUN: Not run yet\n  In 2 modules:");
      expect(text).toContain("#cts-module-3\n");

      // The CSV is saved as a file: capture the blob the download link points at.
      /** @type {Blob[]} */
      const blobs = [];
      /** @type {string[]} */
      const names = [];
      const createObjectURL = URL.createObjectURL;
      const click = HTMLAnchorElement.prototype.click;
      URL.createObjectURL = (blob) => {
        blobs.push(/** @type {Blob} */ (blob));
        return "blob:summary";
      };
      HTMLAnchorElement.prototype.click = function () {
        names.push(this.download);
      };
      try {
        await userEvent.click(
          canvasElement.querySelector('[data-testid="plan-findings-download-csv"] button'),
        );
      } finally {
        URL.createObjectURL = createObjectURL;
        HTMLAnchorElement.prototype.click = click;
      }
      expect(names).toEqual(["summary-of-results-plan-abc-123.csv"]);
      expect(blobs.length).toBe(1);
      expect(blobs[0].type).toBe("text/csv;charset=utf-8");
      const lines = (await blobs[0].text()).trimEnd().split("\r\n");
      expect(lines[0]).toBe(
        "kind,condition,message,requirements,module,variant,test_id,count,link",
      );
      // One line per module occurrence: 2 + 1 + 1 findings, then 1 + 1 + 2 modules.
      expect(lines.length).toBe(1 + 4 + 4);
      expect(lines[1].startsWith("FAILURE,CheckDiscEndpointIssuer,")).toBe(true);
      // Fields with commas are quoted.
      expect(lines[2]).toContain('"client_auth_type=client_secret_basic, response_type=code"');
      expect(lines[lines.length - 1].startsWith("NOT RUN,,Not run yet,,module-never-run-b,")).toBe(
        true,
      );
    } finally {
      spy.restore();
    }
  },
};

export const Empty = {
  render: () => html`<cts-plan-findings .findings=${[]}></cts-plan-findings>`,
  async play({ canvasElement }) {
    expect(canvasElement.querySelector('[data-testid="plan-findings"]')).toBeNull();
    expect(canvasElement.querySelector("cts-plan-findings").textContent.trim()).toBe("");
  },
};
