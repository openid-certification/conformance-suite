import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { MOCK_SCHEMA } from "@fixtures/mock-schema.js";
import "./cts-config-form.js";

/**
 * Resolve once the `<cts-json-editor>` inside the JSON tab is interactive
 * (Monaco mounted or fallback textarea rendered). Tests read `.value` off
 * the host element after this resolves.
 *
 * The host is conditionally rendered when the JSON tab activates, so we
 * first poll for attachment, then delegate to the primitive's own
 * `whenReady()` Promise instead of polling for inner DOM.
 * @param {Element} canvasElement
 * @returns {Promise<HTMLElement>}
 */
async function waitForJsonEditor(canvasElement) {
  const editor = /** @type {any} */ (
    await waitFor(
      () => {
        const el = canvasElement.querySelector("cts-json-editor.oidf-config-form-json");
        if (!el) throw new Error("cts-json-editor.oidf-config-form-json not yet attached");
        return el;
      },
      { timeout: 10000 },
    )
  );
  await editor.whenReady();
  return /** @type {HTMLElement} */ (editor);
}

export default {
  title: "Components/cts-config-form",
  component: "cts-config-form",
};

export const EmptyForm = {
  render: () => html`
    <cts-config-form
      .schema=${MOCK_SCHEMA.schema}
      .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{}}
      .errors=${{}}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    // Container carries the OIDF namespace so scoped CSS rules apply.
    expect(canvasElement.querySelector(".oidf-config-form")).toBeTruthy();
    // Tabs delegate to <cts-tabs>, which renders the OIDF tablist primitive.
    // The form tab is active by default (cts-tabs marks the first panel active).
    const formTab = canvas.getByRole("tab", { name: "Form" });
    const jsonTab = canvas.getByRole("tab", { name: "JSON" });
    expect(formTab.classList.contains("oidf-tab")).toBe(true);
    expect(jsonTab.classList.contains("oidf-tab")).toBe(true);
    expect(formTab.classList.contains("oidf-tab-active")).toBe(true);
    expect(jsonTab.classList.contains("oidf-tab-active")).toBe(false);
    expect(formTab.getAttribute("aria-selected")).toBe("true");
    expect(jsonTab.getAttribute("aria-selected")).toBe("false");
    // Sections render with OIDF section markup and overline-style legends.
    const sections = canvasElement.querySelectorAll("fieldset.oidf-config-form-section");
    expect(sections.length).toBe(2);
    const legends = canvasElement.querySelectorAll("legend.oidf-config-form-section-title");
    expect(legends[0].textContent).toContain("Server Configuration");
    expect(legends[1].textContent).toContain("Client Configuration");
    // Field labels delegated to cts-form-field still render.
    expect(canvas.getByText("Issuer URL")).toBeTruthy();
    expect(canvas.getByText("Client ID")).toBeTruthy();
    // Validate action delegates to cts-button — verify the inner button renders.
    const validateBtn = canvas.getByText("Validate Configuration");
    expect(validateBtn).toBeTruthy();
    expect(validateBtn.tagName).toBe("BUTTON");
    expect(validateBtn.getAttribute("type")).toBe("submit");
    // No Bootstrap form-* classes leak through into the rendered DOM.
    expect(canvasElement.querySelector(".form-control")).toBeNull();
    expect(canvasElement.querySelector(".nav-tabs")).toBeNull();
    expect(canvasElement.querySelector(".nav-link")).toBeNull();
    expect(canvasElement.querySelector(".oidf-btn-primary")).toBeTruthy(); // from cts-button, not config-form
  },
};

export const PrefilledForm = {
  render: () => html`
    <cts-config-form
      .schema=${MOCK_SCHEMA.schema}
      .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{
        server: { issuer: "https://accounts.example.com" },
        client: { client_id: "my-client" },
      }}
      .errors=${{}}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    const issuerInput = canvasElement.querySelector('input[type="url"]');
    expect(issuerInput).toBeTruthy();
    expect(issuerInput.classList.contains("oidf-input")).toBe(true);
    expect(issuerInput.value).toBe("https://accounts.example.com");
    const clientIdInput = canvasElement.querySelector('input[type="text"]');
    expect(clientIdInput).toBeTruthy();
    expect(clientIdInput.classList.contains("oidf-input")).toBe(true);
    expect(clientIdInput.value).toBe("my-client");
  },
};

export const JsonTab = {
  render: () => html`
    <cts-config-form
      .schema=${MOCK_SCHEMA.schema}
      .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{ server: { issuer: "https://example.com" } }}
      .errors=${{}}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const jsonTab = canvas.getByRole("tab", { name: "JSON" });
    await userEvent.click(jsonTab);
    await waitFor(() => {
      expect(jsonTab.classList.contains("oidf-tab-active")).toBe(true);
    });
    expect(jsonTab.getAttribute("aria-selected")).toBe("true");
    const editor = await waitForJsonEditor(canvasElement);
    expect(editor.classList.contains("is-error")).toBe(false);
    expect(editor.getAttribute("aria-invalid")).toBe("false");
    const json = JSON.parse(/** @type {any} */ (editor).value);
    expect(json.server.issuer).toBe("https://example.com");
  },
};

export const ValidJsonDispatchesConfigChange = {
  render: () => html`
    <cts-config-form
      .schema=${MOCK_SCHEMA.schema}
      .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{ server: { issuer: "https://example.com" } }}
      .errors=${{}}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    /** @type {any} */
    let receivedConfig = null;
    canvasElement.addEventListener("cts-config-change", (e) => {
      receivedConfig = /** @type {CustomEvent} */ (e).detail.config;
    });

    await userEvent.click(canvas.getByRole("tab", { name: "JSON" }));
    const editor = await waitForJsonEditor(canvasElement);

    // Drive the success branch end-to-end — the parse-error branch is
    // covered by InvalidJsonShowsError above. After the swap to
    // <cts-json-editor>, this is the only story that confirms a valid
    // edit through the editor still flows through `_handleJsonInput` →
    // `JSON.parse` → `cts-config-change`. The wrapper accepts `.value`
    // assignment as the equivalent of typing, mirroring the legacy
    // <textarea> contract.
    const nextJson = JSON.stringify(
      { server: { issuer: "https://updated.example.com" }, client: { client_id: "abc-123" } },
      null,
      2,
    );
    /** @type {any} */ (editor).value = nextJson;
    editor.dispatchEvent(new Event("input", { bubbles: true }));

    await waitFor(() => {
      expect(receivedConfig).toBeTruthy();
    });
    expect(receivedConfig.server.issuer).toBe("https://updated.example.com");
    expect(receivedConfig.client.client_id).toBe("abc-123");
    // The error state must NOT be set on the success branch — the
    // parse-error overlay is what InvalidJsonShowsError already covers.
    expect(editor.classList.contains("is-error")).toBe(false);
    expect(editor.getAttribute("aria-invalid")).toBe("false");
  },
};

export const InvalidJsonShowsError = {
  render: () => html`
    <cts-config-form
      .schema=${MOCK_SCHEMA.schema}
      .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{ server: { issuer: "https://example.com" } }}
      .errors=${{}}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByRole("tab", { name: "JSON" }));
    const editor = await waitForJsonEditor(canvasElement);
    // Drive the parse-error path by assigning an invalid JSON string and
    // dispatching `input` — the wrapper exposes the same contract the
    // legacy textarea did, but Monaco's keystroke surface is harder to
    // type into reliably under jsdom. The host's `_handleJsonInput`
    // reads `e.target.value`, which is what setting `.value` produces.
    /** @type {any} */ (editor).value = "{not valid";
    editor.dispatchEvent(new Event("input", { bubbles: true }));
    await waitFor(() => {
      expect(editor.classList.contains("is-error")).toBe(true);
    });
    expect(editor.getAttribute("aria-invalid")).toBe("true");
    const errorEl = canvasElement.querySelector('[data-testid="json-error"]');
    expect(errorEl).toBeTruthy();
    expect(errorEl.classList.contains("oidf-config-form-json-error")).toBe(true);
  },
};

export const WithValidationErrors = {
  render: () => html`
    <cts-config-form
      .schema=${MOCK_SCHEMA.schema}
      .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{}}
      .errors=${{
        "server.issuer": "Required field",
        "client.jwks": "Invalid JSON",
      }}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    // Errors roll up from cts-form-field via the .oidf-error class.
    const errors = canvasElement.querySelectorAll(".oidf-error");
    expect(errors.length).toBe(2);
    expect(canvas.getByText("Required field")).toBeTruthy();
    expect(canvas.getByText("Invalid JSON")).toBeTruthy();
    // Inputs in error state pick up the rust border via .is-error.
    const issuerInput = canvasElement.querySelector('input[type="url"]');
    expect(issuerInput.classList.contains("is-error")).toBe(true);
  },
};

export const ConfigChangeEvent = {
  render: () => html`
    <cts-config-form
      .schema=${MOCK_SCHEMA.schema}
      .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{}}
      .errors=${{}}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    /** @type {any} */
    let receivedConfig = null;
    canvasElement.addEventListener("cts-config-change", (e) => {
      receivedConfig = /** @type {CustomEvent} */ (e).detail.config;
    });
    const issuerInput = canvasElement.querySelector('input[type="url"]');
    await userEvent.type(issuerInput, "https://new.example.com");
    expect(receivedConfig).toBeTruthy();
    expect(receivedConfig.server.issuer).toContain("https://new.example.com");
  },
};

export const ValidateEvent = {
  render: () => html`
    <cts-config-form
      .schema=${MOCK_SCHEMA.schema}
      .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{ server: { issuer: "https://example.com" } }}
      .errors=${{}}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    let validateFired = false;
    canvasElement.addEventListener("cts-validate", () => {
      validateFired = true;
    });
    // cts-button renders the inner <button type="submit"> that submits the form;
    // the form's @submit handler dispatches cts-validate.
    await userEvent.click(canvas.getByText("Validate Configuration"));
    expect(validateFired).toBe(true);
  },
};
