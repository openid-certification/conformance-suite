import { html } from "lit";
import { expect, userEvent } from "storybook/test";
import "./cts-form-field.js";

export default {
  title: "Components/cts-form-field",
  component: "cts-form-field",
};

export const TextInput = {
  render: () => html`
    <cts-form-field
      name="client.client_id"
      .schema=${{ type: "string", title: "Client ID", description: "OAuth 2.0 client identifier" }}
      value="my-client-123"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const label = canvasElement.querySelector(".oidf-label");
    expect(label).toBeTruthy();
    expect(label.textContent).toContain("Client ID");
    // .t-overline carries the OIDF label typography (uppercase / bold / fg-soft).
    expect(label.classList.contains("t-overline")).toBe(true);
    const input = canvasElement.querySelector('input[type="text"]');
    expect(input).toBeTruthy();
    expect(input.classList.contains("oidf-input")).toBe(true);
    expect(input.value).toBe("my-client-123");
    const help = canvasElement.querySelector(".oidf-help");
    expect(help.textContent).toContain("OAuth 2.0 client identifier");
    expect(help.classList.contains("t-meta")).toBe(true);
  },
};

export const UrlInput = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", format: "uri", title: "Issuer URL" }}
      value="https://accounts.example.com"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const input = canvasElement.querySelector('input[type="url"]');
    expect(input).toBeTruthy();
    expect(input.classList.contains("oidf-input")).toBe(true);
    expect(input.value).toBe("https://accounts.example.com");
  },
};

export const PasswordInput = {
  render: () => html`
    <cts-form-field
      name="client.client_secret"
      .schema=${{ type: "string", format: "password", title: "Client Secret" }}
      value="s3cret"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const input = canvasElement.querySelector('input[type="password"]');
    expect(input).toBeTruthy();
    expect(input.classList.contains("oidf-input")).toBe(true);
    expect(input.value).toBe("s3cret");
  },
};

export const JsonTextarea = {
  render: () => html`
    <cts-form-field
      name="client.jwks"
      .schema=${{ type: "object", format: "json", title: "Client JWKS" }}
      value='{"keys":[{"kty":"RSA"}]}'
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(textarea.classList.contains("oidf-textarea")).toBe(true);
    // JSON inputs render in monospace.
    expect(textarea.classList.contains("is-mono")).toBe(true);
  },
};

export const SelectDropdown = {
  render: () => html`
    <cts-form-field
      name="client.token_endpoint_auth_method"
      .schema=${{
        type: "string",
        title: "Auth Method",
        enum: ["client_secret_basic", "client_secret_post", "private_key_jwt"],
      }}
      value="client_secret_basic"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const select = canvasElement.querySelector("select.oidf-select");
    expect(select).toBeTruthy();
    const options = select.querySelectorAll("option");
    expect(options.length).toBe(4); // 3 enum + 1 placeholder
  },
};

export const BooleanCheckbox = {
  render: () => html`
    <cts-form-field
      name="client.use_mtls"
      .schema=${{ type: "boolean", title: "Use mTLS", description: "Enable mutual TLS" }}
      value="true"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const checkbox = canvasElement.querySelector('input[type="checkbox"]');
    expect(checkbox).toBeTruthy();
    expect(checkbox.classList.contains("oidf-checkbox")).toBe(true);
    expect(checkbox.checked).toBe(true);
    const checkLabel = canvasElement.querySelector(".oidf-checkbox-label");
    expect(checkLabel.textContent).toContain("Enable mutual TLS");
  },
};

export const WithError = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", format: "uri", title: "Issuer URL" }}
      value=""
      error="Required field"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const input = canvasElement.querySelector("input");
    // Error state lands on the rendered control via .is-error so the rust
    // border colour applies — no host-level class manipulation.
    expect(input.classList.contains("is-error")).toBe(true);
    const error = canvasElement.querySelector(".oidf-error");
    expect(error).toBeTruthy();
    expect(error.textContent).toBe("Required field");
    expect(error.getAttribute("role")).toBe("alert");
  },
};

/**
 * The original WithError only covered the text-input branch. JSON / select /
 * checkbox each render a different control and the `is-error` class has to
 * land on the right element — these stories pin that contract.
 */

export const WithErrorTextarea = {
  render: () => html`
    <cts-form-field
      name="client.jwks"
      .schema=${{ type: "object", format: "json", title: "Client JWKS" }}
      value="not json"
      error="Must be valid JSON"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(textarea.classList.contains("is-error")).toBe(true);
    // is-error must NOT land on a sibling or parent.
    expect(canvasElement.querySelector("input")).toBeNull();
    expect(canvasElement.querySelector(".oidf-error").textContent).toBe("Must be valid JSON");
  },
};

export const WithErrorSelect = {
  render: () => html`
    <cts-form-field
      name="client.token_endpoint_auth_method"
      .schema=${{
        type: "string",
        title: "Auth Method",
        enum: ["client_secret_basic", "private_key_jwt"],
      }}
      value=""
      error="Pick an auth method"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const select = canvasElement.querySelector("select.oidf-select");
    expect(select).toBeTruthy();
    expect(select.classList.contains("is-error")).toBe(true);
    expect(canvasElement.querySelector(".oidf-error").textContent).toBe("Pick an auth method");
  },
};

export const WithErrorCheckbox = {
  render: () => html`
    <cts-form-field
      name="client.use_mtls"
      .schema=${{ type: "boolean", title: "Use mTLS", description: "Enable mutual TLS" }}
      value="false"
      error="mTLS is required for this profile"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const checkbox = canvasElement.querySelector('input[type="checkbox"]');
    expect(checkbox).toBeTruthy();
    // The error message still renders next to the checkbox row — the visual
    // signal is the rust-coloured message, not a class on the checkbox itself.
    expect(canvasElement.querySelector(".oidf-error").textContent).toBe(
      "mTLS is required for this profile",
    );
  },
};

export const ChangeEvent = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", format: "uri", title: "Issuer URL" }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    /** @type {any} */
    let receivedEvent = null;
    canvasElement.addEventListener("cts-field-change", (e) => {
      receivedEvent = /** @type {CustomEvent} */ (e).detail;
    });
    const input = canvasElement.querySelector("input");
    await userEvent.type(input, "https://example.com");
    expect(receivedEvent).toBeTruthy();
    expect(receivedEvent.field).toBe("server.issuer");
    expect(receivedEvent.value).toContain("https://example.com");
  },
};

export const Disabled = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", title: "Issuer URL" }}
      value="https://locked.example.com"
      disabled
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    expect(canvasElement.querySelector("input").disabled).toBe(true);
  },
};

/**
 * Focus state must be keyboard-visible: the input picks up the OIDF orange
 * border + focus ring (`--focus-ring`) the moment it gains focus. We assert
 * via the computed style after focusing the rendered control.
 */
export const FocusState = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", title: "Issuer URL" }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const input = /** @type {HTMLInputElement} */ (canvasElement.querySelector("input"));
    input.focus();
    expect(document.activeElement).toBe(input);
    const computed = getComputedStyle(input);
    // --orange-400 = #EB8B35 → rgb(235, 139, 53)
    expect(computed.borderTopColor).toBe("rgb(235, 139, 53)");
    // --focus-ring expands to a 3px box-shadow; assert it is non-empty.
    expect(computed.boxShadow).not.toBe("none");
  },
};
