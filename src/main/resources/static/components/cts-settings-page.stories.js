import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { http, HttpResponse } from "msw";
import {
  MOCK_SETTINGS_CONFIGURED,
  MOCK_SETTINGS_EMPTY,
  MOCK_SETTINGS_KEY_MISMATCH,
} from "@fixtures/mock-settings.js";
import "./cts-settings-page.js";

const ENDPOINT = "/api/admin/settings/cmf-chile";

export default {
  title: "Pages/cts-settings-page",
  component: "cts-settings-page",
  parameters: {
    // Page-level story: opt in to Chromatic snapshots. Component stories are
    // excluded by default in frontend/.storybook/preview.js.
    chromatic: { disableSnapshot: false },
  },
};

export const Configured = {
  parameters: {
    msw: { handlers: [http.get(ENDPOINT, () => HttpResponse.json(MOCK_SETTINGS_CONFIGURED))] },
  },
  render: () => html`<cts-settings-page></cts-settings-page>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => expect(canvas.getByLabelText("Directory client ID")).toBeInTheDocument());

    await step("plain fields show the stored values", async () => {
      expect(canvas.getByLabelText("Directory token endpoint URL")).toHaveValue(
        "https://directory.sandbox.example.cl/token",
      );
      expect(canvas.getByLabelText("Directory client ID")).toHaveValue("oidf-conformance");
    });

    await step("the stored secret is reported as set, never shown", async () => {
      const secret = canvas.getByLabelText("Directory client secret");
      expect(secret).toHaveValue("");
      expect(secret).toHaveAttribute("placeholder", "set — leave blank to keep");
    });

    await step("the JWKS is summarised by kid, type and algorithm", async () => {
      expect(canvas.getByText("sig-1")).toBeInTheDocument();
      expect(canvas.getByRole("button", { name: "Replace JWKS" })).toBeInTheDocument();
    });

    await step("each certificate shows its subject, and an expired one is flagged", async () => {
      expect(canvas.getByText("CN=oidf-conformance,O=OpenID Foundation")).toBeInTheDocument();
      expect(canvas.getByText("CN=expired-client")).toBeInTheDocument();
      expect(canvasElement.querySelectorAll('cts-badge[label="Expired"]')).toHaveLength(1);
    });
  },
};

export const Empty = {
  parameters: {
    msw: { handlers: [http.get(ENDPOINT, () => HttpResponse.json(MOCK_SETTINGS_EMPTY))] },
  },
  render: () => html`<cts-settings-page></cts-settings-page>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => expect(canvas.getByText("Not configured yet.")).toBeInTheDocument());
    expect(canvas.getByLabelText("Directory client secret")).toHaveAttribute(
      "placeholder",
      "not set",
    );
    expect(canvas.getByRole("button", { name: "Set JWKS" })).toBeInTheDocument();
    expect(canvas.getAllByText("No certificates.")).toHaveLength(2);
  },
};

export const Forbidden = {
  parameters: {
    msw: { handlers: [http.get(ENDPOINT, () => new HttpResponse(null, { status: 403 }))] },
  },
  render: () => html`<cts-settings-page></cts-settings-page>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() =>
      expect(
        canvas.getByText("Server settings are only available to administrators."),
      ).toBeInTheDocument(),
    );
    expect(canvasElement.querySelector("form")).toBeNull();
  },
};

export const KeyMismatchOnSave = {
  parameters: {
    msw: {
      handlers: [
        http.get(ENDPOINT, () => HttpResponse.json(MOCK_SETTINGS_CONFIGURED)),
        http.put(ENDPOINT, () => HttpResponse.json(MOCK_SETTINGS_KEY_MISMATCH, { status: 400 })),
      ],
    },
  },
  render: () => html`<cts-settings-page></cts-settings-page>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => expect(canvas.getByLabelText("Directory client ID")).toBeInTheDocument());

    await step("saving shows the server's error on the field and in the summary", async () => {
      await userEvent.click(canvas.getByRole("button", { name: "Save" }));
      const fieldError = await waitFor(() => {
        const element = canvasElement.querySelector(
          '[data-error-for="positiveCertificates[0].privateKeyPem"]',
        );
        expect(element).not.toBeNull();
        return element;
      });
      expect(fieldError).toHaveTextContent("does not match the first certificate");
      expect(canvas.getByRole("alert")).toHaveTextContent("The settings were not saved");
    });
  },
};
