import { LitElement, html, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-form-field.js";
import "./cts-button.js";

/**
 * Dual-mode (Form / JSON) configuration editor. In Form mode, renders
 * sections/fields driven by a JSON schema and optional UI schema. In JSON
 * mode, offers a raw textarea that keeps `config` in sync on valid JSON.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect (gated by a module-level flag) so the rules
 * appear once regardless of how many `cts-config-form` instances are on the
 * page. Class names are namespaced under `.oidf-config-form` so they do not
 * bleed onto unrelated form markup in the consumer's DOM.
 *
 * The schema-driven fields delegate to `cts-form-field`, which already
 * carries the `.oidf-input` / `.oidf-select` / `.oidf-textarea` / `.oidf-error`
 * tokenized look. This container only owns the surrounding tabs, section
 * fieldsets, divider lines, and the JSON textarea.
 *
 * NOTE: The legacy `schedule-test.html` page renders its own static HTML
 * form using `.config-form-element*` / `[data-json-target]` / `[data-json-type]`
 * selectors that an inline jQuery script scrapes. That page does NOT use
 * `cts-config-form`, so this component intentionally does not emit those
 * class names — keeping them would imply a contract that does not exist.
 *
 * @property {object} schema - JSON schema for the config; `properties` is
 *   iterated to build fields.
 * @property {object} uiSchema - UI hints; `sections[]` groups properties into
 *   fieldsets. Reflects the `ui-schema` attribute.
 * @property {object} config - Current configuration object.
 * @property {object} errors - Map of dotted-path field name to error message
 *   string.
 * @fires cts-config-change - On every field edit or valid JSON edit, with
 *   `{ detail: { config } }`; bubbles.
 * @fires cts-validate - When the Validate Configuration button is clicked;
 *   bubbles.
 */

const STYLE_ID = "cts-config-form-styles";

const STYLE_TEXT = `
.oidf-config-form {
  font-family: var(--font-sans);
  color: var(--fg);
}
.oidf-config-form-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--border);
  margin: 0 0 var(--space-5) 0;
  padding: 0;
  list-style: none;
}
.oidf-config-form-tabs > li {
  margin: 0;
  padding: 0;
  list-style: none;
}
.oidf-config-form-tab {
  appearance: none;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  padding: var(--space-3) var(--space-4);
  font-family: inherit;
  font-size: var(--fs-13);
  font-weight: var(--fw-medium);
  color: var(--ink-500);
  cursor: pointer;
  transition: color var(--dur-1) var(--ease-standard),
              border-color var(--dur-1) var(--ease-standard);
}
.oidf-config-form-tab:hover {
  color: var(--ink-900);
}
.oidf-config-form-tab:focus {
  outline: none;
}
.oidf-config-form-tab:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
  border-radius: var(--radius-2);
}
.oidf-config-form-tab.is-active {
  color: var(--ink-900);
  border-bottom-color: var(--orange-400);
}
.oidf-config-form-section {
  border: 0;
  margin: 0 0 var(--space-5) 0;
  padding: 0 0 var(--space-4) 0;
  border-bottom: 1px solid var(--ink-100);
}
.oidf-config-form-section:last-of-type {
  border-bottom: 0;
  padding-bottom: 0;
  margin-bottom: var(--space-4);
}
.oidf-config-form-section-title {
  /* mirrors .t-overline from oidf-tokens.css */
  font-family: var(--font-sans);
  font-weight: var(--fw-medium);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--fg-soft);
  margin: 0 0 var(--space-3) 0;
  padding: 0;
}
.oidf-config-form-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
.oidf-config-form-json {
  width: 100%;
  box-sizing: border-box;
  min-height: calc(var(--space-6) * 16);
  padding: var(--space-3);
  border: 1px solid var(--ink-300);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
  color: var(--fg);
  font-family: var(--font-mono);
  font-size: var(--fs-13);
  line-height: var(--lh-base);
  resize: vertical;
  /* Reset legacy layout.css \`input[type=text], textarea { text-indent: 5px }\`. */
  text-indent: 0;
}
.oidf-config-form-json:focus {
  outline: none;
  border-color: var(--orange-400);
  box-shadow: var(--focus-ring);
}
.oidf-config-form-json.is-error {
  border-color: var(--rust-400);
}
.oidf-config-form-json-error {
  display: block;
  margin-top: var(--space-2);
  color: var(--rust-500);
  font-family: var(--font-sans);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
}
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

class CtsConfigForm extends LitElement {
  static properties = {
    schema: { type: Object },
    uiSchema: { type: Object, attribute: "ui-schema" },
    config: { type: Object },
    errors: { type: Object },
    _activeTab: { state: true },
    _jsonText: { state: true },
    _jsonError: { state: true },
  };

  createRenderRoot() {
    return this;
  }

  constructor() {
    super();
    this.schema = {};
    this.uiSchema = {};
    this.config = {};
    this.errors = {};
    this._activeTab = "form";
    this._jsonText = "";
    this._jsonError = "";
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
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
    this.dispatchEvent(
      new CustomEvent("cts-config-change", { bubbles: true, detail: { config: newConfig } }),
    );
  }

  _handleJsonInput(e) {
    this._jsonText = e.target.value;
    try {
      this.config = JSON.parse(this._jsonText);
      this._jsonError = "";
      this.dispatchEvent(
        new CustomEvent("cts-config-change", { bubbles: true, detail: { config: this.config } }),
      );
    } catch (err) {
      // Invalid JSON — don't update config until valid, but surface the
      // error so the user knows their edits aren't being saved.
      const message = err instanceof Error ? err.message : String(err);
      this._jsonError = `Invalid JSON — configuration not updated (${message})`;
    }
  }

  _handleTabSwitch(e) {
    const tab = e.currentTarget.dataset.tab;
    if (!tab) return;
    if (tab === "json") {
      this._jsonText = JSON.stringify(this.config, null, 2);
      this._jsonError = "";
    }
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
          <cts-form-field
            name="${key}"
            .schema=${fieldSchema}
            value="${this._getFieldValue(key)}"
            error="${this.errors?.[key] || ""}"
          ></cts-form-field>
        `,
      );
    }

    return sections.map((section) => {
      const sectionSchema = properties[section.key];
      if (!sectionSchema?.properties) return nothing;
      return html`
        <fieldset class="oidf-config-form-section">
          <legend class="oidf-config-form-section-title">${section.title}</legend>
          ${this._renderSectionFields(section.key, sectionSchema.properties)}
        </fieldset>
      `;
    });
  }

  _renderSectionFields(sectionKey, sectionProperties) {
    return Object.entries(sectionProperties).map(
      ([key, fieldSchema]) => html`
        <cts-form-field
          name="${sectionKey}.${key}"
          .schema=${fieldSchema}
          value="${this._getFieldValue(`${sectionKey}.${key}`)}"
          error="${this.errors?.[`${sectionKey}.${key}`] || ""}"
        ></cts-form-field>
      `,
    );
  }

  render() {
    const isFormTab = this._activeTab === "form";
    const isJsonTab = this._activeTab === "json";
    return html`
      <div class="oidf-config-form">
        <ul class="oidf-config-form-tabs" role="tablist">
          <li role="presentation">
            <button
              type="button"
              class=${classMap({ "oidf-config-form-tab": true, "is-active": isFormTab })}
              role="tab"
              aria-selected=${isFormTab ? "true" : "false"}
              data-tab="form"
              @click=${this._handleTabSwitch}
            >
              Form
            </button>
          </li>
          <li role="presentation">
            <button
              type="button"
              class=${classMap({ "oidf-config-form-tab": true, "is-active": isJsonTab })}
              role="tab"
              aria-selected=${isJsonTab ? "true" : "false"}
              data-tab="json"
              @click=${this._handleTabSwitch}
            >
              JSON
            </button>
          </li>
        </ul>
        ${isFormTab
          ? html`
              <form @cts-field-change=${this._handleFieldChange} @submit=${this._handleValidate}>
                ${this._renderSections()}
                <div class="oidf-config-form-actions">
                  <cts-button
                    type="submit"
                    variant="primary"
                    label="Validate Configuration"
                  ></cts-button>
                </div>
              </form>
            `
          : html`
              <textarea
                class=${classMap({
                  "oidf-config-form-json": true,
                  "is-error": Boolean(this._jsonError),
                })}
                rows="20"
                aria-invalid=${this._jsonError ? "true" : "false"}
                .value=${this._jsonText}
                @input=${this._handleJsonInput}
              ></textarea>
              ${this._jsonError
                ? html`<div
                    class="oidf-config-form-json-error"
                    role="alert"
                    aria-live="polite"
                    data-testid="json-error"
                  >
                    ${this._jsonError}
                  </div>`
                : nothing}
            `}
      </div>
    `;
  }
}
customElements.define("cts-config-form", CtsConfigForm);

export {};
