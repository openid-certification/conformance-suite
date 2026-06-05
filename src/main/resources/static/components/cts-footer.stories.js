import { html } from "lit";
import { expect, within, waitFor, fn } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_SERVER_INFO } from "@fixtures/mock-test-data.js";
import "./cts-footer.js";

export default {
  title: "Primitives/cts-footer",
  component: "cts-footer",
};

// --- Stories ---

// Happy path: /api/server resolves, so the footer renders the static brand
// line AND the server-info line with every label key from SERVER_INFO_LABELS.
export const Default = {
  parameters: {
    msw: {
      handlers: [http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO))],
    },
  },
  render: () => html`<cts-footer></cts-footer>`,

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("static brand line renders immediately, independent of the fetch", async () => {
      expect(canvas.getByText("OpenID Foundation conformance suite")).toBeInTheDocument();
    });

    await step("server info appears once the fetch resolves", async () => {
      await waitFor(
        () => {
          expect(canvas.getByText(/Version:/)).toBeInTheDocument();
        },
        { timeout: 3000 },
      );
    });

    await step("every SERVER_INFO_LABELS field renders with the fixture value", async () => {
      const fields = [
        ["external_ip", MOCK_SERVER_INFO.external_ip],
        ["version", MOCK_SERVER_INFO.version],
        ["revision", MOCK_SERVER_INFO.revision],
        ["tag", MOCK_SERVER_INFO.tag],
        ["build_time", MOCK_SERVER_INFO.build_time],
      ];
      for (const [key, value] of fields) {
        const el = canvasElement.querySelector(`#serverinfo-${key}`);
        expect(el).toBeTruthy();
        expect(el.textContent).toBe(value);
      }
    });

    await step(
      "footer carries the .t-meta token class so type stays on the design scale",
      async () => {
        const footer = canvasElement.querySelector("footer.oidf-footer");
        expect(footer).toBeTruthy();
        expect(footer.classList.contains("t-meta")).toBe(true);
      },
    );
  },
};

// Fail-soft: a 500 from /api/server must leave only the static brand line —
// no server-info fields, no throw, and a console.warn for operators.
export const ServerInfoError = {
  parameters: {
    msw: {
      handlers: [http.get("/api/server", () => new HttpResponse(null, { status: 500 }))],
    },
  },
  render: () => html`<cts-footer></cts-footer>`,

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;

    try {
      await step("static brand line is always present, even on the error path", async () => {
        expect(canvas.getByText("OpenID Foundation conformance suite")).toBeInTheDocument();
      });

      await step(
        "after the fetch rejects, the serverInfo container exists but stays empty",
        async () => {
          await waitFor(
            () => {
              const serverInfoDiv = canvasElement.querySelector(".serverInfo");
              expect(serverInfoDiv).toBeTruthy();
              expect(serverInfoDiv.querySelector("#serverinfo-version")).toBeNull();
            },
            { timeout: 3000 },
          );

          // No server-info field rendered at all.
          expect(canvasElement.querySelector('[id^="serverinfo-"]')).toBeNull();
        },
      );

      await step("the 500 produced a diagnostic warn naming the component + endpoint", async () => {
        await waitFor(() => {
          expect(warnSpy).toHaveBeenCalled();
          const joined = warnSpy.mock.calls.flat().join(" ");
          expect(joined).toContain("cts-footer");
          expect(joined).toContain("/api/server");
        });
      });
    } finally {
      console.warn = origWarn;
    }
  },
};
