import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { MOCK_SCHEMA } from "./fixtures/mock-schema.js";
import "../../src/main/resources/static/components/cts-config-form.js";

export default {
  title: "Components/cts-config-form",
  component: "cts-config-form",
};

export const EmptyForm = {
  render: () => html`
    <cts-config-form .schema=${MOCK_SCHEMA.schema} .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{}} .errors=${{}}></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    expect(canvas.getByText("Form")).toBeTruthy();
    expect(canvas.getByText("JSON")).toBeTruthy();
    expect(canvas.getByText("Form").classList.contains("active")).toBe(true);
    expect(canvas.getByText("Server Configuration")).toBeTruthy();
    expect(canvas.getByText("Client Configuration")).toBeTruthy();
    expect(canvas.getByText("Issuer URL")).toBeTruthy();
    expect(canvas.getByText("Client ID")).toBeTruthy();
    expect(canvas.getByText("Validate Configuration")).toBeTruthy();
  },
};

export const PrefilledForm = {
  render: () => html`
    <cts-config-form .schema=${MOCK_SCHEMA.schema} .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{ server: { issuer: "https://accounts.example.com" }, client: { client_id: "my-client" } }}
      .errors=${{}}></cts-config-form>
  `,
  async play({ canvasElement }) {
    const issuerInput = canvasElement.querySelector('input[type="url"]');
    expect(issuerInput).toBeTruthy();
    expect(issuerInput.value).toBe("https://accounts.example.com");
  },
};

export const JsonTab = {
  render: () => html`
    <cts-config-form .schema=${MOCK_SCHEMA.schema} .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{ server: { issuer: "https://example.com" } }}
      .errors=${{}}></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const jsonTab = canvas.getByText("JSON");
    await userEvent.click(jsonTab);
    await waitFor(() => { expect(jsonTab.classList.contains("active")).toBe(true); });
    const textarea = canvasElement.querySelector("textarea.font-monospace");
    expect(textarea).toBeTruthy();
    const json = JSON.parse(textarea.value);
    expect(json.server.issuer).toBe("https://example.com");
  },
};

export const WithValidationErrors = {
  render: () => html`
    <cts-config-form .schema=${MOCK_SCHEMA.schema} .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{}} .errors=${{ "server.issuer": "Required field", "client.jwks": "Invalid JSON" }}
    ></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    expect(canvas.getByText("Required field")).toBeTruthy();
  },
};

export const ConfigChangeEvent = {
  render: () => html`
    <cts-config-form .schema=${MOCK_SCHEMA.schema} .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{}} .errors=${{}}></cts-config-form>
  `,
  async play({ canvasElement }) {
    let receivedConfig = null;
    canvasElement.addEventListener("cts-config-change", (e) => { receivedConfig = e.detail.config; });
    const issuerInput = canvasElement.querySelector('input[type="url"]');
    await userEvent.type(issuerInput, "https://new.example.com");
    expect(receivedConfig).toBeTruthy();
    expect(receivedConfig.server.issuer).toContain("https://new.example.com");
  },
};

export const ValidateEvent = {
  render: () => html`
    <cts-config-form .schema=${MOCK_SCHEMA.schema} .uiSchema=${MOCK_SCHEMA.uiSchema}
      .config=${{ server: { issuer: "https://example.com" } }}
      .errors=${{}}></cts-config-form>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    let validateFired = false;
    canvasElement.addEventListener("cts-validate", () => { validateFired = true; });
    await userEvent.click(canvas.getByText("Validate Configuration"));
    expect(validateFired).toBe(true);
  },
};
