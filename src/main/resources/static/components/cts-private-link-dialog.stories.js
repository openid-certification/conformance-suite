import { html } from "lit";
import { expect, waitFor, userEvent } from "storybook/test";
import "./cts-private-link-dialog.js";

export default {
  title: "Components/cts-private-link-dialog",
  component: "cts-private-link-dialog",
};

const SHARE_URL = "/api/test-share";

/** The dialog opened with its default expiry; days input + Generate ready. */
export const Default = {
  render: () => html`<cts-private-link-dialog share-url=${SHARE_URL}></cts-private-link-dialog>`,
  async play({ canvasElement }) {
    const el = /** @type {any} */ (canvasElement.querySelector("cts-private-link-dialog"));
    el.show();
    await waitFor(() => {
      expect(
        canvasElement.querySelector('[data-testid="private-link-dialog"]')?.hasAttribute("open"),
      ).toBe(true);
    });
    const days = canvasElement.querySelector(".plinkDays");
    expect(days).toBeTruthy();
    expect(days.value).toBe("30");
    expect(canvasElement.querySelector(".plinkGenerateBtn button")?.disabled).toBe(false);
  },
};

/** Generate is gated on a valid expiry (1–3650 days). */
export const DaysValidation = {
  render: () => html`<cts-private-link-dialog share-url=${SHARE_URL}></cts-private-link-dialog>`,
  async play({ canvasElement, step }) {
    const el = /** @type {any} */ (canvasElement.querySelector("cts-private-link-dialog"));
    el.show();
    await waitFor(() => expect(canvasElement.querySelector(".plinkDays")).toBeTruthy());
    const days = canvasElement.querySelector(".plinkDays");
    const generate = () => canvasElement.querySelector(".plinkGenerateBtn button");

    await step("0 days is invalid → Generate disabled", async () => {
      await userEvent.clear(days);
      await userEvent.type(days, "0");
      await waitFor(() => expect(generate()?.disabled).toBe(true));
    });

    await step("3651 days is invalid → Generate disabled", async () => {
      await userEvent.clear(days);
      await userEvent.type(days, "3651");
      await waitFor(() => expect(generate()?.disabled).toBe(true));
    });

    await step("365 days is valid → Generate enabled", async () => {
      await userEvent.clear(days);
      await userEvent.type(days, "365");
      await waitFor(() => expect(generate()?.disabled).toBe(false));
    });
  },
};

/**
 * Clicking Generate POSTs to the share endpoint and renders the link + a Copy
 * button. fetch is stubbed; the clipboard auto-copy is best-effort (guarded by
 * try/catch in the component), so this story asserts the result display, not
 * the clipboard write — that is covered end-to-end in the page specs.
 */
export const GenerateShowsResult = {
  render: () => html`<cts-private-link-dialog share-url=${SHARE_URL}></cts-private-link-dialog>`,
  async play({ canvasElement }) {
    const SHARE_LINK = "https://example.test/login.html?token=story";
    const originalFetch = window.fetch;
    window.fetch = () =>
      Promise.resolve(
        new Response(JSON.stringify({ link: SHARE_LINK, message: "Heads up: server restart" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    try {
      const el = /** @type {any} */ (canvasElement.querySelector("cts-private-link-dialog"));
      el.show();
      await waitFor(() =>
        expect(canvasElement.querySelector(".plinkGenerateBtn button")).toBeTruthy(),
      );
      await userEvent.click(canvasElement.querySelector(".plinkGenerateBtn button"));

      await waitFor(() => {
        const result = canvasElement.querySelector('[data-testid="private-link-result"]');
        expect(result).toBeTruthy();
        expect(result.textContent).toContain(SHARE_LINK);
      });
      expect(canvasElement.querySelector(".plinkMessage")?.textContent).toContain("server restart");
      expect(canvasElement.querySelector(".plinkCopyBtn")).toBeTruthy();
    } finally {
      window.fetch = originalFetch;
    }
  },
};
