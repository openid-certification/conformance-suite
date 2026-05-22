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

/**
 * The Validate Configuration button is a UI placeholder for the SUS UX
 * review's HIGH-severity "pre-run config validation" recommendation. The
 * backend `POST /api/plan/validate` endpoint is not yet implemented, so
 * the button opens a construction-notice modal that explains the
 * unwired state to OIDF maintainers. A tooltip surfaces the same status
 * on hover/focus.
 */
export const ValidateButtonShowsSusNotice = {
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
    const button = canvas.getByText("Validate Configuration");

    // Hovering the button surfaces the SUS construction-notice tooltip
    // (appended to document.body by cts-tooltip).
    await userEvent.hover(button);
    await waitFor(() => {
      const tip = document.querySelector(".oidf-tooltip");
      expect(tip?.textContent || "").toContain("SUS recommendation");
    });
    await userEvent.unhover(button);

    // cts-button renders the inner <button type="submit"> that submits the form;
    // the form's @submit handler opens the construction-notice modal.
    await userEvent.click(button);
    const modal = /** @type {HTMLElement} */ (
      canvasElement.querySelector("#cts-config-form-sus-notice-modal")
    );
    expect(modal).toBeTruthy();
    // cts-modal mirrors `open` to the host once the dialog opens.
    await waitFor(() => expect(modal.hasAttribute("open")).toBe(true));
    expect(modal.getAttribute("heading")).toContain("not yet implemented");
  },
};

/**
 * Validates hiddenFields in nested-schema mode (the legacy mock-schema shape).
 * Hidden fields disappear from the Form tab, are stripped from the JSON tab's
 * pretty-print, and round-trip losslessly through a JSON-tab edit because the
 * component merges hidden values back from the prior config.
 */
export const WithHiddenFields = {
  render: () => html`
    <cts-config-form
      .schema=${MOCK_SCHEMA.schema}
      .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{
        server: { issuer: "https://example.com" },
        client: { client_id: "visible-id", client_secret: "hidden-secret" },
      }}
      .errors=${{}}
      .hiddenFields=${new Set(["client.client_secret"])}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Form tab: client_id renders, client_secret does not.
    const clientIdField = canvasElement.querySelector('cts-form-field[name="client.client_id"]');
    const clientSecretField = canvasElement.querySelector(
      'cts-form-field[name="client.client_secret"]',
    );
    expect(clientIdField).toBeTruthy();
    expect(clientSecretField).toBeNull();

    // Sections that still have visible fields render normally.
    const legends = canvasElement.querySelectorAll("legend.oidf-config-form-section-title");
    expect(legends.length).toBe(2);

    // Switch to JSON tab; pretty-print omits the hidden key but full submit
    // shape preserves it via the cts-config-change merge.
    await userEvent.click(canvas.getByRole("tab", { name: "JSON" }));
    const editor = await waitForJsonEditor(canvasElement);
    const json = JSON.parse(/** @type {any} */ (editor).value);
    expect(json.client.client_id).toBe("visible-id");
    expect(json.client.client_secret).toBeUndefined();

    // Edit JSON: change visible value. cts-config-change should emit the full
    // config with the hidden client_secret still present (merged back).
    /** @type {any} */
    let receivedConfig = null;
    canvasElement.addEventListener("cts-config-change", (e) => {
      receivedConfig = /** @type {CustomEvent} */ (e).detail.config;
    });

    const editedJson = JSON.stringify(
      { server: { issuer: "https://example.com" }, client: { client_id: "edited-id" } },
      null,
      2,
    );
    /** @type {any} */ (editor).value = editedJson;
    editor.dispatchEvent(new Event("input", { bubbles: true }));

    await waitFor(() => {
      expect(receivedConfig).not.toBeNull();
    });
    expect(receivedConfig.client.client_id).toBe("edited-id");
    expect(receivedConfig.client.client_secret).toBe("hidden-secret");

    // Form-tab edit path: typing into a visible field also preserves the
    // hidden value. _handleFieldChange runs structuredClone(this.config),
    // _setAtPath the new value, and emits — no merge step is needed
    // because the clone carries the hidden key forward. This is a
    // distinct code path from the JSON-tab merge above; a future
    // refactor that constructs the config from scratch in
    // _handleFieldChange would break it.
    receivedConfig = null;
    await userEvent.click(canvas.getByRole("tab", { name: "Form" }));
    const visibleInput = canvasElement.querySelector(
      'cts-form-field[name="client.client_id"] input',
    );
    expect(visibleInput).toBeTruthy();
    /** @type {any} */ (visibleInput).value = "from-form-tab";
    visibleInput.dispatchEvent(new Event("input", { bubbles: true }));
    await waitFor(() => {
      expect(receivedConfig).not.toBeNull();
    });
    expect(receivedConfig.client.client_id).toBe("from-form-tab");
    expect(receivedConfig.client.client_secret).toBe("hidden-secret");
  },
};

/**
 * Validates the type:object round-trip: a JWKS object passed in via
 * `.config` renders as pretty-printed JSON in the textarea, and an edit to
 * that JSON dispatches `cts-config-change` with a PARSED object (not a
 * string). This is the contract the schedule-test page depends on — every
 * federation JWKS field, every `server.jwks`/`client.jwks`/etc., the
 * `*.presentation_definition`, the brazil payment-consent textareas. A
 * regression here would cause `JSON.stringify(currentConfig)` on submit to
 * emit `"jwks":"{...}"` (string) instead of `"jwks":{...}` (object), which
 * the backend rejects.
 */
export const ObjectFieldRoundTrip = {
  render: () => html`
    <cts-config-form
      .schema=${{
        type: "object",
        properties: {
          alias: { type: "string", title: "alias" },
          "server.jwks": {
            type: "object",
            title: "server_jwks",
            description: "JWKS for the server",
          },
        },
      }}
      .uiSchema=${{
        sections: [{ key: "_root", title: "Test", fields: ["alias", "server.jwks"] }],
      }}
      .config=${{ server: { jwks: { keys: [{ kty: "RSA", alg: "RS256" }] } } }}
      .errors=${{}}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    // Display: the textarea text is the pretty-printed JSON of the config
    // value, not "[object Object]". Verifies cts-form-field._displayValue
    // formats type:object values.
    const jwksField = canvasElement.querySelector('cts-form-field[name="server.jwks"]');
    expect(jwksField).toBeTruthy();
    const textarea = /** @type {HTMLTextAreaElement} */ (jwksField.querySelector("textarea"));
    expect(textarea).toBeTruthy();
    expect(textarea.value).toContain('"keys"');
    expect(textarea.value).toContain('"RSA"');
    expect(textarea.value).not.toBe("[object Object]");

    // Emit: editing the textarea to valid JSON dispatches an OBJECT, not a
    // string. The page's currentConfig listener (and the eventual
    // JSON.stringify on submit) sees the parsed shape.
    /** @type {any} */
    let receivedConfig = null;
    canvasElement.addEventListener("cts-config-change", (e) => {
      receivedConfig = /** @type {CustomEvent} */ (e).detail.config;
    });
    textarea.value = JSON.stringify({ keys: [{ kty: "EC", crv: "P-256" }] });
    textarea.dispatchEvent(new Event("input", { bubbles: true }));
    await waitFor(() => {
      expect(receivedConfig).not.toBeNull();
    });
    expect(typeof receivedConfig.server.jwks).toBe("object");
    expect(receivedConfig.server.jwks.keys[0].kty).toBe("EC");
    expect(receivedConfig.server.jwks.keys[0].crv).toBe("P-256");

    // Invalid JSON: dispatches the raw string AND surfaces setCustomValidity
    // so submit is blocked at the browser layer. Mirrors legacy
    // validateJSONFromFormElement semantics.
    receivedConfig = null;
    textarea.value = "{broken json";
    textarea.dispatchEvent(new Event("input", { bubbles: true }));
    await waitFor(() => {
      expect(receivedConfig).not.toBeNull();
    });
    expect(receivedConfig.server.jwks).toBe("{broken json");
    expect(textarea.validationMessage).not.toBe("");
    expect(textarea.classList.contains("is-invalid")).toBe(true);
  },
};

/**
 * Validates the new explicit-fields uiSchema shape used by the Phase 2
 * field-catalog adapter: a flat schema.properties keyed by full dotted paths,
 * with uiSchema.sections[*].fields[] naming which paths render under each
 * section. The two existing nested-mode stories above remain the contract for
 * the legacy mock-schema fixture; this story locks in the new contract.
 */
export const ExplicitFieldsMode = {
  render: () => html`
    <cts-config-form
      .schema=${{
        type: "object",
        properties: {
          alias: { type: "string", title: "alias" },
          "client.client_id": { type: "string", title: "client_id" },
          "client.tls_client_auth_subject_dn": {
            type: "string",
            title: "tls_client_auth_subject_dn",
          },
          "federation.entity_identifier": {
            type: "string",
            title: "entity_identifier",
          },
        },
      }}
      .uiSchema=${{
        sections: [
          { key: "_root", title: "Test Information", fields: ["alias"] },
          {
            key: "client",
            title: "Client",
            fields: ["client.client_id"],
          },
          {
            key: "client_tls_auth",
            title: "tls_client_auth configuration",
            fields: ["client.tls_client_auth_subject_dn"],
          },
          {
            key: "federation_entity",
            title: "Federation entity",
            fields: ["federation.entity_identifier"],
          },
        ],
      }}
      .config=${{}}
      .errors=${{}}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    // Four sections render, one field each.
    const legends = canvasElement.querySelectorAll("legend.oidf-config-form-section-title");
    expect(legends.length).toBe(4);
    expect(legends[0].textContent).toContain("Test Information");
    expect(legends[1].textContent).toContain("Client");
    expect(legends[2].textContent).toContain("tls_client_auth configuration");
    expect(legends[3].textContent).toContain("Federation entity");

    // The cross-prefix case: a section keyed "client_tls_auth" rendering a
    // field whose data path lives under client.* — the composed field name
    // must be the full path, not "client_tls_auth.client.tls_client_auth_…".
    const tlsField = canvasElement.querySelector(
      'cts-form-field[name="client.tls_client_auth_subject_dn"]',
    );
    expect(tlsField).toBeTruthy();
    const wrongComposition = canvasElement.querySelector(
      'cts-form-field[name="client_tls_auth.client.tls_client_auth_subject_dn"]',
    );
    expect(wrongComposition).toBeNull();
  },
};

/**
 * In explicit-fields mode, hiding every field in a section makes the section
 * disappear from the layout entirely (no empty fieldset).
 */
export const ExplicitFieldsHiddenSectionDisappears = {
  render: () => html`
    <cts-config-form
      .schema=${{
        type: "object",
        properties: {
          alias: { type: "string", title: "alias" },
          "client.client_id": { type: "string", title: "client_id" },
          "client.client_secret": { type: "string", title: "client_secret" },
        },
      }}
      .uiSchema=${{
        sections: [
          { key: "_root", title: "Test Information", fields: ["alias"] },
          {
            key: "client",
            title: "Client",
            fields: ["client.client_id", "client.client_secret"],
          },
        ],
      }}
      .config=${{}}
      .errors=${{}}
      .hiddenFields=${new Set(["client.client_id", "client.client_secret"])}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    // Only the "Test Information" section renders; the Client section is gone
    // because every field is hidden.
    const legends = canvasElement.querySelectorAll("legend.oidf-config-form-section-title");
    expect(legends.length).toBe(1);
    expect(legends[0].textContent).toContain("Test Information");
  },
};
