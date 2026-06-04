import { LitElement, html, nothing, css } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-form-field.js";
import "./cts-button.js";
import "./cts-icon.js";
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
 * @fires cts-validate - After the validate feedback window resolves, with
 *   `{ detail: { valid, errors, config } }`; bubbles, composed. Fires on
 *   every verdict regardless of outcome. Note the timing: the event lands
 *   `VALIDATE_FEEDBACK_DELAY_MS` (~1s) after the click, together with the
 *   inline verdict — a host that layers async checks on this seam should
 *   account for the delay and refresh the verdict if it reassigns `errors`.
 *
 * ## Validate Configuration
 *
 * Submitting the form (Validate Configuration button, or Enter in a field)
 * opens a short feedback window: the button enters its `loading` state
 * (spinner, disabled, accessible name "Validating configuration…") for
 * `VALIDATE_FEEDBACK_DELAY_MS` so the click has a perceptible
 * acknowledgment — the required-fields pass itself is synchronous and
 * would otherwise complete imperceptibly. Re-entrant submits during the
 * window are ignored, and any displayed verdict is cleared as the window
 * opens so the verdict region is empty while the spinner runs. A config
 * change during the window (field edit, JSON edit, or programmatic
 * `config` reassignment) aborts the in-flight validation entirely — a
 * verdict only ever describes a config the user explicitly asked to
 * validate.
 *
 * When the window resolves, the client-side pass collects every visible
 * required field (`x-cts-required: true` on the field schema, or
 * membership in the section-level `required: []` array under nested-mode),
 * reads the current value via `_getValueAtPathRaw`, and flags any path
 * whose value is null, undefined, or an empty string. The resulting map
 * replaces `this.errors` (so inline `.oidf-error` markers light up next to
 * the offending fields), and the verdict renders inline next to the button
 * in a persistent `role="status"` live region (polite announcement; the
 * node pre-exists the verdict so screen readers pick up the update):
 * "Configuration is valid" on a clean pass, or the missing-field count on
 * a failure. The verdict clears on any config change — a form field edit,
 * a JSON-tab edit, or a programmatic `config` reassignment from the host
 * page (e.g. Load Last Configuration) — so a stale verdict never sits next
 * to a different config. Tab switches alone do not clear it: validation
 * reads `this.config`, which both tabs keep in sync.
 *
 * Internal validate state (not part of the public API): `_validating`
 * (boolean; the feedback window is open) and `_validateResult`
 * (`null | { valid, count }`; the rendered verdict).
 *
 * A `cts-validate` event fires with `{ valid, errors, config }` regardless
 * of the verdict, so the host page can layer a backend check on top (e.g.
 * the as-yet-unimplemented `POST /api/plan/validate` endpoint from
 * brainstorm Unit 1D) by listening, calling the endpoint, and reassigning
 * `errors` with the merged result.
 */

const STYLE_ID = "cts-config-form-styles";

/**
 * Length of the validate feedback window in milliseconds. The required-
 * fields pass is synchronous; this delay exists purely so the click has a
 * perceptible acknowledgment (spinner on the button) before the verdict
 * lands. Exported so story tests can derive their `waitFor` timeouts from
 * the same constant instead of hardcoding a wall-clock number.
 */
const VALIDATE_FEEDBACK_DELAY_MS = 1000;

const STYLE_TEXT = css`
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
    /* A long error verdict wraps below the button at full row width on
       narrow viewports instead of overflowing the flex row. */
    flex-wrap: wrap;
    gap: var(--space-3);
    margin-top: var(--space-4);
  }
  /* Inline validate verdict — mirrors .oidf-error (cts-form-field) for the
     error tone and .share-modal-success (oidf-app.css) for the success
     tone. The node persists empty so the role="status" live region exists
     before its first update. */
  .oidf-config-form-verdict {
    display: flex;
    align-items: center;
    gap: var(--space-1);
    font-family: var(--font-sans);
    font-size: var(--fs-12);
    line-height: var(--lh-snug);
  }
  .oidf-config-form-verdict.is-ok {
    color: var(--status-pass);
  }
  .oidf-config-form-verdict.is-error {
    color: var(--rust-500);
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
  style.textContent = STYLE_TEXT.cssText;
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
    _validating: { state: true },
    _validateResult: { state: true },
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
    this._validating = false;
    /** @type {null | { valid: boolean, count: number }} */
    this._validateResult = null;
    /** @type {ReturnType<typeof setTimeout> | 0} */
    this._validateTimer = 0;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    // Drop a pending validate window so the timer callback can't fire
    // against a disconnected element (e.g. the form is removed mid-window).
    // Resetting _validating keeps a reconnected element out of a stuck
    // loading state.
    this._cancelPendingValidate();
  }

  // Abort an in-flight validate window: the armed timer would otherwise
  // resolve ~1s later against whatever `this.config` holds THEN, landing a
  // verdict the user never asked for (worst case: a plan switch mid-window
  // gets an unsolicited "N required fields missing" for the new plan).
  // Callers that clear the displayed verdict on config changes call this
  // alongside `_validateResult = null`.
  _cancelPendingValidate() {
    if (this._validateTimer) {
      clearTimeout(this._validateTimer);
      this._validateTimer = 0;
      this._validating = false;
    }
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
      // External reassignment means the verdict no longer describes the
      // current config — drop it and abort any in-flight validation
      // (internal edit paths do the same themselves).
      this._validateResult = null;
      this._cancelPendingValidate();
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
    this._validateResult = null;
    this._cancelPendingValidate();
    this.dispatchEvent(
      new CustomEvent("cts-config-change", { bubbles: true, detail: { config: newConfig } }),
    );
  }

  _handleJsonInput(e) {
    this._jsonText = e.target.value;
    this._validateResult = null;
    this._cancelPendingValidate();
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

  /**
   * Returns the list of visible required field paths to check on validate.
   * Honours both schema conventions used in the codebase:
   * - Per-field `x-cts-required: true` (produced by `config-form-adapter`
   *   from `field.required` in `config-field-catalog.json`).
   * - Section-level `required: ["fieldName"]` arrays (JSON Schema standard,
   *   used by the nested-mode mock fixture).
   *
   * Hidden fields are excluded — a required field the page has chosen to
   * hide is not user-actionable, so flagging it would only confuse.
   * @returns {string[]} Required field paths, in section/render order.
   */
  _collectRequiredPaths() {
    const sections = this.uiSchema?.sections || [];
    const properties = this.schema?.properties || {};
    const paths = [];
    const push = (path, fieldSchema) => {
      if (!fieldSchema) return;
      if (this.hiddenFields?.has(path)) return;
      if (paths.includes(path)) return;
      paths.push(path);
    };

    if (sections.length === 0) {
      const topRequired = Array.isArray(this.schema?.required) ? this.schema.required : [];
      for (const [key, fieldSchema] of Object.entries(properties)) {
        if (fieldSchema?.["x-cts-required"] || topRequired.includes(key)) {
          push(key, fieldSchema);
        }
      }
      return paths;
    }

    for (const section of sections) {
      if (Array.isArray(section.fields)) {
        for (const path of section.fields) {
          if (properties[path]?.["x-cts-required"]) push(path, properties[path]);
        }
        continue;
      }
      const sectionSchema = properties[section.key];
      if (!sectionSchema?.properties) continue;
      const sectionRequired = Array.isArray(sectionSchema.required) ? sectionSchema.required : [];
      for (const [fieldKey, fieldSchema] of Object.entries(sectionSchema.properties)) {
        const fullPath = `${section.key}.${fieldKey}`;
        if (fieldSchema?.["x-cts-required"] || sectionRequired.includes(fieldKey)) {
          push(fullPath, fieldSchema);
        }
      }
    }
    return paths;
  }

  /**
   * Walks the required paths returned by `_collectRequiredPaths` and flags
   * any whose current `config` value is null, undefined, or an empty
   * string. Object/array `{}`/`[]` shapes are NOT flagged — a user can
   * legitimately leave a JWKS placeholder open and the schema-driven
   * adapter does not distinguish "empty container" from "missing".
   * @returns {Record<string, string>} Map of full-path → error message.
   */
  _validateConfig() {
    /** @type {Record<string, string>} */
    const errors = {};
    for (const path of this._collectRequiredPaths()) {
      const value = this._getValueAtPathRaw(this.config, path);
      if (value === null || value === undefined || value === "") {
        errors[path] = "Required field";
      }
    }
    return errors;
  }

  _handleValidate(e) {
    e.preventDefault();
    // Re-entrancy guard. The loading state disables the submit button,
    // blocking clicks and (in practice, though browser-dependent)
    // Enter-key implicit submission — this early-return is the
    // authoritative backstop for both and for programmatic requestSubmit().
    if (this._validating) return;
    this._validateResult = null;
    this._validating = true;
    this._validateTimer = setTimeout(() => {
      this._validateTimer = 0;
      const errors = this._validateConfig();
      const count = Object.keys(errors).length;
      const valid = count === 0;
      this.errors = errors;
      // Verdict, inline field markers, spinner-stop, and the event land
      // together so the feedback reads as one coherent moment.
      this._validateResult = { valid, count };
      this._validating = false;
      this.dispatchEvent(
        new CustomEvent("cts-validate", {
          bubbles: true,
          composed: true,
          detail: { valid, errors, config: this.config },
        }),
      );
    }, VALIDATE_FEEDBACK_DELAY_MS);
  }

  _renderVerdict() {
    const result = this._validateResult;
    if (!result) return nothing;
    // The message is composed as a single string interpolation so the
    // rendered text node stays contiguous — Prettier reflows literal text
    // inside html`` templates, which would split the phrase across lines
    // and break textContent-based assertions.
    if (result.valid) {
      const message = "Configuration is valid";
      return html`<cts-icon name="check-big" size="16" aria-hidden="true"></cts-icon> ${message}`;
    }
    const noun = result.count === 1 ? "field is" : "fields are";
    const message = `${result.count} required ${noun} missing. See inline errors.`;
    return html`<cts-icon name="circle-warning" size="16" aria-hidden="true"></cts-icon>
      ${message}`;
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
                <cts-button
                  type="submit"
                  variant="primary"
                  label="Validate Configuration"
                  ?loading=${this._validating}
                  aria-label=${this._validating ? "Validating configuration…" : nothing}
                ></cts-button>
                <div
                  class=${classMap({
                    "oidf-config-form-verdict": true,
                    "is-ok": Boolean(this._validateResult?.valid),
                    "is-error": Boolean(this._validateResult && !this._validateResult.valid),
                  })}
                  role="status"
                  data-testid="validate-verdict"
                >
                  ${this._renderVerdict()}
                </div>
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

export { VALIDATE_FEEDBACK_DELAY_MS };
