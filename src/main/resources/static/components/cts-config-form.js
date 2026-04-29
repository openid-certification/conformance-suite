import { LitElement, html, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-form-field.js";
import "./cts-button.js";
import "./cts-json-editor.js";
import "./cts-tabs.js";

/**
 * Dual-mode (Form / JSON) configuration editor. In Form mode, renders
 * sections/fields driven by a JSON schema and optional UI schema. In JSON
 * mode, hosts a Monaco-backed `<cts-json-editor>` that keeps `config` in sync
 * on valid JSON. The editor exposes `.value` as a plain string and dispatches
 * `input`/`change` events identical to a `<textarea>`, so the parse-error
 * UX below it stays unchanged.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect (gated by a module-level flag) so the rules
 * appear once regardless of how many `cts-config-form` instances are on the
 * page. Class names are namespaced under `.oidf-config-form` so they do not
 * bleed onto unrelated form markup in the consumer's DOM.
 *
 * The schema-driven fields delegate to `cts-form-field`, which already
 * carries the `.oidf-input` / `.oidf-select` / `.oidf-textarea` / `.oidf-error`
 * tokenized look. The Form/JSON tabs delegate to `<cts-tabs>` /
 * `<cts-tab-panel>`, so the tab chrome (underline, orange-400 active
 * indicator, keyboard navigation) matches the rest of the suite without
 * duplicating CSS. This container only owns the section fieldsets, divider
 * lines, and the JSON editor host.
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
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: calc(var(--space-6) * 16);
}
.oidf-config-form-json.is-error .oidf-json-editor {
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
    _jsonText: { state: true },
    _jsonError: { state: true },
    _jsonTabActivated: { state: true },
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
    this._jsonText = "";
    this._jsonError = "";
    // Lazy-mount guard for `<cts-json-editor>`. Monaco's `editor.create()`
    // measures its host on mount; if the host sits inside a `<cts-tabs>`
    // panel that's currently `hidden` (the inactive tab), the host reports
    // zero width/height and Monaco's `automaticLayout` ResizeObserver does
    // not reliably recover when the panel later becomes visible. Mounting
    // the editor only after the JSON tab has been activated at least once
    // sidesteps the issue and matches the pre-cts-tabs render contract
    // (the editor never existed while the Form tab was active).
    this._jsonTabActivated = false;
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

  _handleTabChange(e) {
    // cts-tabs fires `cts-tab-change` with the activated panel id. Use it
    // as both the form→JSON serialization trigger and the lazy-mount cue
    // for the editor (see `_jsonTabActivated` in the constructor).
    if (e.detail?.id === "cts-config-form-json-panel") {
      this._jsonText = JSON.stringify(this.config, null, 2);
      this._jsonError = "";
      this._jsonTabActivated = true;
    }
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
    return html`
      <div class="oidf-config-form">
        <cts-tabs @cts-tab-change=${this._handleTabChange}>
          <cts-tab-panel label="Form" id="cts-config-form-form-panel">
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
          </cts-tab-panel>
          <cts-tab-panel label="JSON" id="cts-config-form-json-panel">
            ${this._jsonTabActivated
              ? html`
                  <cts-json-editor
                    class=${classMap({
                      "oidf-config-form-json": true,
                      "is-error": Boolean(this._jsonError),
                    })}
                    aria-label="Configuration JSON"
                    aria-invalid=${this._jsonError ? "true" : "false"}
                    .value=${this._jsonText}
                    @input=${this._handleJsonInput}
                  ></cts-json-editor>
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
                `
              : nothing}
          </cts-tab-panel>
        </cts-tabs>
      </div>
    `;
  }
}
customElements.define("cts-config-form", CtsConfigForm);

export {};
