import { html } from "lit";
import { expect, userEvent } from "storybook/test";
import "./cts-form-field.js";

export default {
  title: "Components/cts-form-field",
  component: "cts-form-field",
};

export const TextInput = {
  render: () => html`
    <cts-form-field name="client.client_id"
      .schema=${{ type: "string", title: "Client ID", description: "OAuth 2.0 client identifier" }}
      value="my-client-123"></cts-form-field>
  `,
  async play({ canvasElement }) {
    expect(canvasElement.querySelector("label").textContent).toContain("Client ID");
    const input = canvasElement.querySelector('input[type="text"]');
    expect(input).toBeTruthy();
    expect(input.value).toBe("my-client-123");
    expect(canvasElement.querySelector(".form-text").textContent).toContain("OAuth 2.0 client identifier");
  },
};

export const UrlInput = {
  render: () => html`
    <cts-form-field name="server.issuer"
      .schema=${{ type: "string", format: "uri", title: "Issuer URL" }}
      value="https://accounts.example.com"></cts-form-field>
  `,
  async play({ canvasElement }) {
    const input = canvasElement.querySelector('input[type="url"]');
    expect(input).toBeTruthy();
    expect(input.value).toBe("https://accounts.example.com");
  },
};

export const PasswordInput = {
  render: () => html`
    <cts-form-field name="client.client_secret"
      .schema=${{ type: "string", format: "password", title: "Client Secret" }}
      value="s3cret"></cts-form-field>
  `,
  async play({ canvasElement }) {
    const input = canvasElement.querySelector('input[type="password"]');
    expect(input).toBeTruthy();
    expect(input.value).toBe("s3cret");
  },
};

export const JsonTextarea = {
  render: () => html`
    <cts-form-field name="client.jwks"
      .schema=${{ type: "object", format: "json", title: "Client JWKS" }}
      value='{"keys":[{"kty":"RSA"}]}'></cts-form-field>
  `,
  async play({ canvasElement }) {
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(textarea.classList.contains("font-monospace")).toBe(true);
  },
};

export const SelectDropdown = {
  render: () => html`
    <cts-form-field name="client.token_endpoint_auth_method"
      .schema=${{ type: "string", title: "Auth Method", enum: ["client_secret_basic", "client_secret_post", "private_key_jwt"] }}
      value="client_secret_basic"></cts-form-field>
  `,
  async play({ canvasElement }) {
    const select = canvasElement.querySelector("select.form-select");
    expect(select).toBeTruthy();
    const options = select.querySelectorAll("option");
    expect(options.length).toBe(4); // 3 enum + 1 placeholder
  },
};

export const BooleanCheckbox = {
  render: () => html`
    <cts-form-field name="client.use_mtls"
      .schema=${{ type: "boolean", title: "Use mTLS", description: "Enable mutual TLS" }}
      value="true"></cts-form-field>
  `,
  async play({ canvasElement }) {
    const checkbox = canvasElement.querySelector('input[type="checkbox"]');
    expect(checkbox).toBeTruthy();
    expect(checkbox.checked).toBe(true);
    expect(canvasElement.querySelector(".form-check-label").textContent).toContain("Enable mutual TLS");
  },
};

export const WithError = {
  render: () => html`
    <cts-form-field name="server.issuer"
      .schema=${{ type: "string", format: "uri", title: "Issuer URL" }}
      value="" error="Required field"></cts-form-field>
  `,
  async play({ canvasElement }) {
    expect(canvasElement.querySelector("input").classList.contains("is-invalid")).toBe(true);
    expect(canvasElement.querySelector(".invalid-feedback").textContent).toBe("Required field");
  },
};

export const ChangeEvent = {
  render: () => html`
    <cts-form-field name="server.issuer"
      .schema=${{ type: "string", format: "uri", title: "Issuer URL" }}
      value=""></cts-form-field>
  `,
  async play({ canvasElement }) {
    let receivedEvent = null;
    canvasElement.addEventListener("cts-field-change", (e) => { receivedEvent = e.detail; });
    const input = canvasElement.querySelector("input");
    await userEvent.type(input, "https://example.com");
    expect(receivedEvent).toBeTruthy();
    expect(receivedEvent.field).toBe("server.issuer");
    expect(receivedEvent.value).toContain("https://example.com");
  },
};

export const Disabled = {
  render: () => html`
    <cts-form-field name="server.issuer"
      .schema=${{ type: "string", title: "Issuer URL" }}
      value="https://locked.example.com" disabled></cts-form-field>
  `,
  async play({ canvasElement }) {
    expect(canvasElement.querySelector("input").disabled).toBe(true);
  },
};
