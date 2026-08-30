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

/**
 * The expiry presets (1 week / 1 month / 6 months / 1 year) set the days
 * input in one click; the preset matching the current value is the pressed
 * one, and typing a non-preset value un-presses them all.
 */
export const ExpiryPresets = {
  render: () => html`<cts-private-link-dialog share-url=${SHARE_URL}></cts-private-link-dialog>`,
  async play({ canvasElement, step }) {
    const el = /** @type {any} */ (canvasElement.querySelector("cts-private-link-dialog"));
    el.show();
    await waitFor(() => expect(canvasElement.querySelector(".plinkDays")).toBeTruthy());
    const days = () => canvasElement.querySelector(".plinkDays");
    const preset = (d) => canvasElement.querySelector(`.plinkPreset[data-days="${d}"] button`);
    const pressedDays = () =>
      [...canvasElement.querySelectorAll('.plinkPreset button[aria-pressed="true"]')].map((b) =>
        b.closest(".plinkPreset").getAttribute("data-days"),
      );

    await step("renders the four presets, 1 month pressed by default", async () => {
      expect(canvasElement.querySelectorAll(".plinkPreset").length).toBe(4);
      expect(preset(7)?.textContent.trim()).toBe("1 week");
      expect(preset(30)?.textContent.trim()).toBe("1 month");
      expect(preset(180)?.textContent.trim()).toBe("6 months");
      expect(preset(365)?.textContent.trim()).toBe("1 year");
      expect(pressedDays()).toEqual(["30"]);
    });

    await step("clicking 1 year sets 365 days and moves the pressed state", async () => {
      await userEvent.click(preset(365));
      await waitFor(() => expect(days().value).toBe("365"));
      expect(pressedDays()).toEqual(["365"]);
      expect(canvasElement.querySelector(".plinkGenerateBtn button")?.disabled).toBe(false);
    });

    await step("typing a custom value un-presses every preset", async () => {
      await userEvent.clear(days());
      await userEvent.type(days(), "45");
      await waitFor(() => expect(pressedDays()).toEqual([]));
    });

    await step("typing a preset value re-presses that preset", async () => {
      await userEvent.clear(days());
      await userEvent.type(days(), "7");
      await waitFor(() => expect(pressedDays()).toEqual(["7"]));
    });
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
