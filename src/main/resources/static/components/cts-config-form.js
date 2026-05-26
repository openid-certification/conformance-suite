import { LitElement, html, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-form-field.js";
import "./cts-button.js";
import "./cts-json-editor.js";
import "./cts-tabs.js";
import "./cts-tooltip.js";
import "./cts-modal.js";

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
 * ## Two schema-shape modes
 *
 * The render path supports two `uiSchema.sections[*]` conventions:
 *
 * - **Nested mode** (legacy): `uiSchema.sections[*]` has `{ key, title }`.
 *   For each section, `schema.properties[section.key].properties` lists the
 *   fields, and field paths compose as `${section.key}.${fieldKey}`. Used by
 *   the original Storybook stories and the mock-schema fixture.
 *
 * - **Explicit-fields mode** (Phase 2): `uiSchema.sections[*]` has
 *   `{ key, title, fields: ["full.dotted.path", ...] }`. `schema.properties`
 *   is flat-keyed by full path, and each `fields[]` entry names which paths
 *   render under that section. Required because `schedule-test.html`'s real
 *   field catalog has UI sections that mix data prefixes (e.g. the
 *   "Credential Issuer" UI section contains both `vci.*` and `credential.*`
 *   data paths) and sections that share a data prefix (the four federation
 *   sections all use `federation.*`). The explicit list disambiguates.
 *
 * The two modes coexist: a `section` with `fields` triggers explicit mode,
 * without it falls back to nested mode. Adopters do not need to migrate.
 *
 * ## hiddenFields
 *
 * When `hiddenFields` is a non-empty Set, the listed full-path keys are
 * filtered out of both tabs:
 * - Form tab: matching `cts-form-field` elements are not rendered.
 *   Sections that become entirely empty disappear from the layout.
 * - JSON tab: the pretty-printed text omits the hidden keys. When the user
 *   edits the JSON tab, the resulting `this.config` is the parsed-visible
 *   shape MERGED with the current hidden values, so the hidden portion
 *   round-trips losslessly even though the user can't see it.
 *
 * The component never mutates `this.config` based on `hiddenFields` —
 * toggling visibility is a render concern, not a data one. `cts-config-change`
 * always emits the full config (visible + hidden), so consumers POST the
 * full object regardless of which fields are currently shown.
 *
 * @property {object} schema - JSON schema for the config. In nested mode,
 *   `properties` is nested by section key. In explicit-fields mode, `properties`
 *   is flat-keyed by full dotted path.
 * @property {object} uiSchema - UI hints; `sections[]` groups properties into
 *   fieldsets. Each section is `{ key, title, fields? }` — when `fields` is
 *   provided (Array of full-path strings), explicit-fields mode is used.
 *   Reflects the `ui-schema` attribute.
 * @property {object} config - Current configuration object.
 * @property {object} errors - Map of dotted-path field name to error message
 *   string.
 * @property {Set<string>} hiddenFields - Set of full-path keys to filter out
 *   of both Form and JSON tabs. Hidden values stay in `this.config` and are
 *   included in `cts-config-change` emissions. Default: empty Set.
 * @fires cts-config-change - On every field edit or valid JSON edit, with
 *   `{ detail: { config } }`; bubbles. `config` is always the full object,
 *   including any hidden-field values that were preserved through the
 *   JSON-tab merge.
 *
 * The Validate Configuration button is a UI placeholder for the SUS UX
 * review's HIGH-severity "pre-run config validation" recommendation
 * (`docs/SUS_OIDF_UX_UI_Review_2025.md` lines 1512–1564). The backend
 * `POST /api/plan/validate` endpoint (brainstorm Unit 1D) is not yet
 * implemented, so clicking the button opens an explanatory cts-modal
 * instead of dispatching a validation event. A construction-notice
 * cts-tooltip wraps the button to make the placeholder state visible
 * on hover/focus. Remove the tooltip + modal scaffold and re-introduce
 * `cts-validate` event dispatch once the endpoint lands.
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
.oidf-config-form-sus-notice h4 {
  font-family: var(--font-sans);
  font-weight: var(--fw-medium);
  font-size: var(--fs-14);
  margin: var(--space-4) 0 var(--space-2) 0;
  color: var(--fg);
}
.oidf-config-form-sus-notice p,
.oidf-config-form-sus-notice blockquote {
  font-family: var(--font-sans);
  font-size: var(--fs-14);
  line-height: var(--lh-snug);
  margin: 0 0 var(--space-3) 0;
  color: var(--fg);
}
.oidf-config-form-sus-notice blockquote {
  border-left: 2px solid var(--ink-200);
  padding: 0 var(--space-3);
  color: var(--fg-soft);
  font-style: italic;
}
.oidf-config-form-sus-notice code {
  font-family: var(--font-mono);
  font-size: 0.92em;
  background: var(--bg-muted);
  padding: 0 var(--space-1);
  border-radius: var(--radius-1);
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
    hiddenFields: { attribute: false },
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
    this.hiddenFields = new Set();
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

  // U12 (B4): keep `_jsonText` in sync when `.config` is reassigned from
  // outside (e.g. schedule-test.html's Load-last-configuration flow).
  // Internal edit paths (`_handleFieldChange`, `_handleJsonInput`) always
  // reassign both `this.config` and `this._jsonText` in the same microtask,
  // so they appear together in `changedProperties` — external assignment
  // touches only `config`, which is the case this refresh targets.
  // `hasUpdated` guards the first-render cycle so the constructor's
  // initial empty `_jsonText` survives until `_handleTabChange` seeds it
  // on the JSON tab's first activation.
  willUpdate(changedProperties) {
    if (this.hasUpdated && changedProperties.has("config") && !changedProperties.has("_jsonText")) {
      this._jsonText = this._filteredJsonText();
    }
  }

  _getFieldValue(fieldPath) {
    const parts = fieldPath.split(".");
    let obj = this.config;
    for (const part of parts) {
      if (obj == null) return "";
      obj = obj[part];
    }
    // Return the raw value (string | object | array | boolean | number).
    // cts-form-field's _displayValue formats objects/arrays for display
    // based on its own schema; stringifying here would lose the type and
    // produce "[object Object]" in textareas bound to type:object fields.
    return obj == null ? "" : obj;
  }

  _setAtPath(obj, fieldPath, value) {
    const parts = fieldPath.split(".");
    let cur = obj;
    for (let i = 0; i < parts.length - 1; i++) {
      cur[parts[i]] = cur[parts[i]] || {};
      cur = cur[parts[i]];
    }
    cur[parts[parts.length - 1]] = value;
  }

  _deleteAtPath(obj, fieldPath) {
    const parts = fieldPath.split(".");
    let cur = obj;
    for (let i = 0; i < parts.length - 1; i++) {
      if (cur == null || typeof cur !== "object") return;
      cur = cur[parts[i]];
    }
    if (cur != null && typeof cur === "object") {
      delete cur[parts[parts.length - 1]];
    }
  }

  _hasHidden() {
    return this.hiddenFields instanceof Set && this.hiddenFields.size > 0;
  }

  _filterHidden(obj) {
    if (!this._hasHidden()) return obj;
    const out = structuredClone(obj || {});
    for (const path of this.hiddenFields) this._deleteAtPath(out, path);
    return out;
  }

  // Merge an incoming visible-only config with any hidden-field values from
  // the current `this.config`, so hidden data round-trips losslessly even
  // when the user edits the (filtered) JSON tab.
  _mergeHiddenFromCurrent(visible) {
    if (!this._hasHidden()) return visible;
    const out = structuredClone(visible || {});
    for (const path of this.hiddenFields) {
      const current = this._getValueAtPathRaw(this.config, path);
      if (current !== undefined) this._setAtPath(out, path, current);
    }
    return out;
  }

  _getValueAtPathRaw(obj, fieldPath) {
    const parts = fieldPath.split(".");
    let cur = obj;
    for (const part of parts) {
      if (cur == null || typeof cur !== "object") return undefined;
      cur = cur[part];
    }
    return cur;
  }

  _filteredJsonText() {
    return JSON.stringify(this._filterHidden(this.config), null, 2);
  }

  _handleFieldChange(e) {
    const { field, value } = e.detail;
    const newConfig = structuredClone(this.config);
    this._setAtPath(newConfig, field, value);
    this.config = newConfig;
    this._jsonText = JSON.stringify(this._filterHidden(newConfig), null, 2);
    this.dispatchEvent(
      new CustomEvent("cts-config-change", { bubbles: true, detail: { config: newConfig } }),
    );
  }

  _handleJsonInput(e) {
    this._jsonText = e.target.value;
    try {
      const parsed = JSON.parse(this._jsonText);
      this.config = this._mergeHiddenFromCurrent(parsed);
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
      this._jsonText = this._filteredJsonText();
      this._jsonError = "";
      this._jsonTabActivated = true;
    }
  }

  _handleValidate(e) {
    e.preventDefault();
    const modal = /** @type {(HTMLElement & { show?: () => void }) | null} */ (
      this.querySelector("#cts-config-form-sus-notice-modal")
    );
    if (modal && typeof modal.show === "function") modal.show();
  }

  _renderSections() {
    const sections = this.uiSchema?.sections || [];
    const properties = this.schema?.properties || {};

    if (sections.length === 0) {
      return Object.entries(properties).map(([key, fieldSchema]) =>
        this._renderField(key, fieldSchema),
      );
    }

    return sections.map((section) => {
      if (Array.isArray(section.fields)) {
        // Explicit-fields mode: section.fields lists full-path keys that
        // exist directly on the (flat) schema.properties map.
        const visible = section.fields.filter((path) => !this.hiddenFields?.has(path));
        if (visible.length === 0) return nothing;
        return html`
          <fieldset class="oidf-config-form-section">
            <legend class="oidf-config-form-section-title">${section.title}</legend>
            ${visible.map((path) => this._renderField(path, properties[path]))}
          </fieldset>
        `;
      }

      // Nested mode: walk schema.properties[section.key].properties.
      const sectionSchema = properties[section.key];
      if (!sectionSchema?.properties) return nothing;
      const renderedFields = this._renderSectionFields(section.key, sectionSchema.properties);
      if (renderedFields.every((f) => f === nothing)) return nothing;
      return html`
        <fieldset class="oidf-config-form-section">
          <legend class="oidf-config-form-section-title">${section.title}</legend>
          ${renderedFields}
        </fieldset>
      `;
    });
  }

  _renderSectionFields(sectionKey, sectionProperties) {
    return Object.entries(sectionProperties).map(([key, fieldSchema]) => {
      const fullPath = `${sectionKey}.${key}`;
      return this._renderField(fullPath, fieldSchema);
    });
  }

  _renderField(fullPath, fieldSchema) {
    if (!fieldSchema) return nothing;
    if (this.hiddenFields?.has(fullPath)) return nothing;
    return html`
      <cts-form-field
        name="${fullPath}"
        .schema=${fieldSchema}
        .value=${this._getFieldValue(fullPath)}
        error="${this.errors?.[fullPath] || ""}"
      ></cts-form-field>
    `;
  }

  render() {
    return html`
      <div class="oidf-config-form">
        <cts-tabs aria-label="Configure test input mode" @cts-tab-change=${this._handleTabChange}>
          <cts-tab-panel label="Form" id="cts-config-form-form-panel">
            <form @cts-field-change=${this._handleFieldChange} @submit=${this._handleValidate}>
              ${this._renderSections()}
              <div class="oidf-config-form-actions">
                <cts-tooltip content="SUS recommendation – requires wiring 🚧">
                  <cts-button
                    type="submit"
                    variant="primary"
                    label="Validate Configuration"
                  ></cts-button>
                </cts-tooltip>
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
        ${this._renderSusNoticeModal()}
      </div>
    `;
  }

  /**
   * Construction-notice modal explaining that the Validate Configuration
   * button is a UI placeholder for an unwired SUS recommendation. Body
   * content is static (no reactive bindings) because `cts-modal`
   * physically moves its children into the inner `<dialog>` on first
   * connect — see the lifecycle note in cts-token-manager.js. Remove
   * this modal and the `_handleValidate` modal-show indirection once the
   * `POST /api/plan/validate` endpoint lands and the page wires up
   * `cts-validate`.
   * @returns {ReturnType<typeof html>} Lit template for the notice modal.
   */
  _renderSusNoticeModal() {
    return html`
      <cts-modal
        id="cts-config-form-sus-notice-modal"
        heading="Validate Configuration — not yet implemented"
        size="lg"
      >
        <div class="oidf-config-form-sus-notice">
          <p>
            This button is a UI placeholder for a feature recommended by Super User Studio's 2025 UX
            review of the OpenID Conformance Suite. The backend validation endpoint has not been
            implemented yet, so clicking the button currently does nothing actionable.
          </p>
          <h4>What it should do</h4>
          <p>
            Run pre-flight diagnostic checks against the current test configuration (e.g. ClientID
            format, certificate validity, missing required fields) <em>before</em> the user creates
            the test plan. Surface any errors inline against the offending fields so the user can
            fix them in-place.
          </p>
          <h4>Why it matters (severity: HIGH)</h4>
          <p>
            Today, users cannot distinguish configuration errors from real conformance errors until
            <em>after</em> they create and run a plan. This forces them into a trial-and-error setup
            loop and inflates support load.
          </p>
          <blockquote>
            "I was not knowing if it was an error until I created a test plan because the required
            field was missing here... when I click the 'create test plan' I should already see a
            pop-up error." — QA, Raidiam
          </blockquote>
          <h4>How to implement</h4>
          <p>
            The frontend already emits a <code>cts-validate</code>
            request when the button is clicked (currently routed to this modal). The brainstorm at
            <code>.superpowers/brainstorm/63162-1776122675</code>
            proposes a <code>POST /api/plan/validate</code> endpoint (Unit 1D) that returns
            structured errors keyed by field path; the page would set
            <code>ctsConfigForm.errors</code> and gate <code>#createPlanBtn</code> on a clean
            response.
          </p>
          <p>
            <strong>References:</strong>
            <code>docs/SUS_OIDF_UX_UI_Review_2025.md</code> lines 1512–1564 (findings +
            recommendations); brainstorm traceability row R7.
          </p>
        </div>
      </cts-modal>
    `;
  }
}
customElements.define("cts-config-form", CtsConfigForm);

export {};
