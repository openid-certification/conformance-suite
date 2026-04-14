import { LitElement, html, nothing } from "lit";
import "./cts-form-field.js";

class CtsConfigForm extends LitElement {
  static properties = {
    schema: { type: Object },
    uiSchema: { type: Object, attribute: "ui-schema" },
    config: { type: Object },
    errors: { type: Object },
    _activeTab: { state: true },
    _jsonText: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this.schema = {};
    this.uiSchema = {};
    this.config = {};
    this.errors = {};
    this._activeTab = "form";
    this._jsonText = "";
  }

  _getFieldValue(fieldPath) {
    const parts = fieldPath.split(".");
    let obj = this.config;
    for (const part of parts) {
      if (obj == null) return "";
      obj = obj[part];
    }
    return obj != null ? String(obj) : "";
  }

  _handleFieldChange(e) {
    const { field, value } = e.detail;
    const newConfig = structuredClone(this.config);
    const parts = field.split(".");
    let obj = newConfig;
    for (let i = 0; i < parts.length - 1; i++) {
      obj[parts[i]] = obj[parts[i]] || {};
      obj = obj[parts[i]];
    }
    obj[parts[parts.length - 1]] = value;
    this.config = newConfig;
    this._jsonText = JSON.stringify(newConfig, null, 2);
    this.dispatchEvent(new CustomEvent("cts-config-change", { bubbles: true, detail: { config: newConfig } }));
  }

  _handleJsonInput(e) {
    this._jsonText = e.target.value;
    try {
      this.config = JSON.parse(this._jsonText);
      this.dispatchEvent(new CustomEvent("cts-config-change", { bubbles: true, detail: { config: this.config } }));
    } catch {
      // Invalid JSON -- don't update config until valid
    }
  }

  _handleTabSwitch(tab) {
    if (tab === "json") this._jsonText = JSON.stringify(this.config, null, 2);
    this._activeTab = tab;
  }

  _handleValidate(e) {
    e.preventDefault();
    this.dispatchEvent(new CustomEvent("cts-validate", { bubbles: true }));
  }

  _renderSections() {
    const sections = this.uiSchema?.sections || [];
    const properties = this.schema?.properties || {};

    if (sections.length === 0) {
      return Object.entries(properties).map(
        ([key, fieldSchema]) => html`
          <cts-form-field name="${key}" .schema=${fieldSchema}
            value="${this._getFieldValue(key)}" error="${this.errors?.[key] || ""}"></cts-form-field>
        `,
      );
    }

    return sections.map((section) => {
      const sectionSchema = properties[section.key];
      if (!sectionSchema?.properties) return nothing;
      return html`
        <fieldset class="mb-4">
          <legend class="fs-6 fw-bold border-bottom pb-2">${section.title}</legend>
          ${Object.entries(sectionSchema.properties).map(
            ([key, fieldSchema]) => html`
              <cts-form-field name="${section.key}.${key}" .schema=${fieldSchema}
                value="${this._getFieldValue(`${section.key}.${key}`)}"
                error="${this.errors?.[`${section.key}.${key}`] || ""}"></cts-form-field>
            `,
          )}
        </fieldset>
      `;
    });
  }

  render() {
    return html`
      <div>
        <ul class="nav nav-tabs mb-3">
          <li class="nav-item">
            <button class="nav-link${this._activeTab === "form" ? " active" : ""}"
              @click=${() => this._handleTabSwitch("form")}>Form</button>
          </li>
          <li class="nav-item">
            <button class="nav-link${this._activeTab === "json" ? " active" : ""}"
              @click=${() => this._handleTabSwitch("json")}>JSON</button>
          </li>
        </ul>
        ${this._activeTab === "form"
          ? html`
              <form @cts-field-change=${this._handleFieldChange} @submit=${this._handleValidate}>
                ${this._renderSections()}
                <button type="submit" class="btn btn-sm btn-primary bg-gradient border border-secondary">
                  Validate Configuration
                </button>
              </form>
            `
          : html`
              <textarea class="form-control font-monospace" rows="20"
                .value=${this._jsonText} @input=${this._handleJsonInput}></textarea>
            `}
      </div>
    `;
  }
}
customElements.define("cts-config-form", CtsConfigForm);
