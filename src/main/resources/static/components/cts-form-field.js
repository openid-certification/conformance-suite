import { LitElement, html, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";
import { isMultiLineConfigField } from "../lib/config-field-types.js";

/**
 * Renders a single form input driven by a JSON-schema fragment. Supports
 * string, boolean, enum, JSON (object/array), and password inputs.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect (gated by a module-level flag) so the rules
 * appear once regardless of how many `cts-form-field` instances are on the
 * page. Class names are namespaced under `.oidf-form-field` so they do not
 * bleed onto unrelated inputs in the consumer's DOM.
 *
 * ## Type-aware emit / display contract
 *
 * For `schema.type === "object"` and `schema.type === "array"`, this
 * component renders a `<textarea>` whose ON-DISK value the consumer stores
 * is the PARSED object/array — not the raw text. The textarea's displayed
 * text is the pretty-printed JSON of that value. On every input event the
 * component tries to JSON.parse the text; on success it dispatches the
 * parsed object/array via `cts-field-change`. On failure it dispatches the
 * raw string AND sets `setCustomValidity("Invalid JSON")` on the textarea
 * (with `.is-invalid` class) so submit is blocked at the browser layer.
 * This matches the legacy schedule-test scraper's `populateJSON` semantics
 * (parse-then-typecheck) without the host page needing to scrape.
 *
 * For `schema.type === "array", schema.format === "newline-array"`, the
 * textarea splits on `\n` and dispatches an array of non-empty trimmed
 * lines (mirrors the legacy `data-json-type="jsonarray"` convention used
 * by `federation_trust_anchor.immediate_subordinates`).
 *
 * For displayed value:
 *   - object/array values arrive as JS objects; the textarea shows
 *     `JSON.stringify(value, null, 4)` (or `value.join("\n")` for
 *     newline-array).
 *   - string values render verbatim.
 *
 * For `placeholder`: reads only `schema["x-cts-placeholder"]`. `description`
 * is rendered as help-text below the input and is never used as a fallback
 * placeholder — falling back would duplicate the same text inside the input
 * and below it. The adapter routes catalog-declared `tooltip` to
 * `description` and catalog-declared `placeholder` to `x-cts-placeholder`, so
 * the two slots stay independent.
 *
 * String-typed fields whose `name` leaf ends with a PEM/JWKS/key suffix
 * (see `lib/config-field-types.js`) render as `<textarea>` instead of
 * `<input>` so multi-line credentials paste cleanly. The textarea uses
 * `field-sizing: content` to auto-grow with content up to `max-height`;
 * Firefox falls back to the floor `min-height` plus manual `resize:vertical`.
 *
 * @property {object} schema - JSON-schema fragment for this field. May include
 *   `type`, `format`, `enum`, `enumLabels`, `title`, `description`,
 *   `x-cts-placeholder`, `x-cts-required`. `enumLabels` is a parallel array
 *   to `enum` (same length, same order) that overrides the option labels —
 *   used by the publish dropdown to render `""` as "No". When `enum` already
 *   contains `""`, the leading `<option value="">Select...</option>`
 *   placeholder is suppressed so the dropdown does not show two empty-value
 *   options.
 * @property {string} name - Field name used as the `field` key in
 *   `cts-field-change` events.
 * @property {string|object|Array} value - Current field value. Strings render
 *   verbatim; objects/arrays render as pretty-printed JSON in object/array
 *   textareas. Setting via attribute is always a string.
 * @property {string} error - Validation error message shown below the input.
 * @property {boolean} disabled - Disables the input.
 * @fires cts-field-change - On every input/change with
 *   `{ detail: { field, value } }`; bubbles. For type:object/array the
 *   emitted `value` is the parsed object/array on valid JSON, or the raw
 *   string on parse failure (with setCustomValidity raised on the input).
 */

const STYLE_ID = "cts-form-field-styles";

// Per-instance unique id used to wire the <label for>, aria-describedby, and
// aria-invalid relationships. `name` is not unique enough — the same schema
// (e.g. `client.client_id`) is rendered for both `client` and `client2` blocks
// on schedule-test.html, which would collide if used directly as an id.
let uidCounter = 0;

// Inline SVG chevron used as the custom select indicator. Stroke colour is
// `--ink-500` (`#71695E`) — encoded as `%2371695E` in the data: URL.
const SELECT_CHEVRON =
  "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 16 16'><path fill='none' stroke='%2371695E' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' d='M4 6l4 4 4-4'/></svg>\")";

const STYLE_TEXT = `
cts-form-field {
  display: block;
}
.oidf-form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin-bottom: var(--space-4);
}
.oidf-form-field .oidf-label {
  font-family: var(--font-sans);
  font-weight: var(--fw-bold);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
  color: var(--fg-soft);
}
.oidf-form-field .oidf-input,
.oidf-form-field .oidf-select,
.oidf-form-field .oidf-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: var(--space-3);
  border: 1px solid var(--ink-300);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
  color: var(--fg);
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: var(--lh-base);
  /* Reset legacy layout.css \`input[type=text], textarea { text-indent: 5px }\` so
     design-system inputs land at the same x-offset regardless of input type. */
  text-indent: 0;
}
.oidf-form-field .oidf-input,
.oidf-form-field .oidf-select {
  height: var(--control-height);
  padding-top: 0;
  padding-bottom: 0;
}
.oidf-form-field .oidf-textarea {
  /* layout.css ships a global \`textarea { height: 200px }\` for legacy pages
     (index.html etc). An explicit height overrides \`field-sizing: content\`,
     so we reset to \`auto\` and let min-height / max-height carry the bounds. */
  height: auto;
  min-height: calc(var(--space-6) * 4);
  max-height: 50vh;
  /* Auto-grow as the user types/pastes. Firefox lacks support today and
     falls back to the fixed initial size + min-height floor + manual resize
     handle. Width stays at 100% so horizontal jank is impossible. */
  field-sizing: content;
  resize: vertical;
}
.oidf-form-field .oidf-input.is-mono,
.oidf-form-field .oidf-textarea.is-mono {
  font-family: var(--font-mono);
  font-size: var(--fs-12);
}
.oidf-form-field .oidf-select {
  appearance: none;
  -webkit-appearance: none;
  padding-right: 36px;
  background-image: ${SELECT_CHEVRON};
  background-repeat: no-repeat;
  background-position: right 12px center;
  /* Native <select> centers its closed-state text inconsistently across browsers
     when line-height inflates the line box; pin to 1 inside the fixed 34px height. */
  line-height: 1;
}
.oidf-form-field .oidf-input:focus,
.oidf-form-field .oidf-select:focus,
.oidf-form-field .oidf-textarea:focus {
  outline: none;
  border-color: var(--orange-400);
  box-shadow: var(--focus-ring);
}
.oidf-form-field .oidf-input:disabled,
.oidf-form-field .oidf-select:disabled,
.oidf-form-field .oidf-textarea:disabled {
  background: var(--bg-muted);
  color: var(--fg-faint);
  cursor: not-allowed;
}
.oidf-form-field .oidf-input.is-error,
.oidf-form-field .oidf-select.is-error,
.oidf-form-field .oidf-textarea.is-error {
  border-color: var(--rust-400);
}
.oidf-form-field .oidf-error {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--rust-500);
  font-family: var(--font-sans);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
}
.oidf-form-field .oidf-help {
  /* mirrors .t-meta from oidf-tokens.css */
  color: var(--fg-soft);
}
.oidf-form-field .oidf-checkbox-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.oidf-form-field .oidf-checkbox {
  width: var(--space-4);
  height: var(--space-4);
  margin: 0;
  accent-color: var(--orange-500);
}
.oidf-form-field .oidf-checkbox:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
  border-radius: var(--radius-1);
}
.oidf-form-field .oidf-checkbox-label {
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: var(--lh-snug);
  color: var(--fg);
}
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

class CtsFormField extends LitElement {
  static properties = {
    schema: { type: Object },
    name: { type: String },
    value: { type: String },
    error: { type: String },
    disabled: { type: Boolean },
  };

  createRenderRoot() {
    return this;
  }

  constructor() {
    super();
    this.schema = {};
    this.name = "";
    this.value = "";
    this.error = "";
    this.disabled = false;
    this._uid = `cts-ff-${++uidCounter}`;
  }

  _describedByIds() {
    const ids = [];
    if (this.error) ids.push(`${this._uid}-error`);
    if (this.schema && this.schema.description) ids.push(`${this._uid}-help`);
    return ids.length ? ids.join(" ") : null;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  _handleInput(e) {
    const raw = e.target.value;
    const { type, format } = this.schema || {};
    let value = raw;
    let parseError = "";

    if (type === "array" && format === "newline-array") {
      // Newline-delimited array: split on `\n`, trim, drop empties.
      value = raw
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line !== "");
    } else if (type === "object" || type === "array") {
      // JSON textarea: empty string is the no-value sentinel; otherwise
      // try to parse. On failure, emit the raw string so the user can keep
      // editing, and surface the parse error via setCustomValidity so
      // submit is blocked at the browser layer (mirrors legacy
      // validateJSONFromFormElement behavior).
      if (raw.trim() === "") {
        value = type === "array" ? [] : {};
      } else {
        try {
          value = JSON.parse(raw);
        } catch (err) {
          value = raw;
          parseError = err instanceof Error ? err.message : "Invalid JSON";
        }
      }
    }

    if (typeof e.target.setCustomValidity === "function") {
      e.target.setCustomValidity(parseError);
      e.target.classList.toggle("is-invalid", Boolean(parseError));
    }

    this.dispatchEvent(
      new CustomEvent("cts-field-change", {
        bubbles: true,
        detail: { field: this.name, value },
      }),
    );
  }

  _handleCheckbox(e) {
    this.dispatchEvent(
      new CustomEvent("cts-field-change", {
        bubbles: true,
        detail: { field: this.name, value: e.target.checked },
      }),
    );
  }

  /**
   * Format the current `this.value` for display in a textarea/input. Strings
   * pass through; objects/arrays are pretty-printed (or newline-joined for
   * `format: newline-array`). The companion to `_handleInput`'s type-aware
   * parse.
   *
   * @returns {string}
   */
  _displayValue() {
    // The static-properties declaration types `value` as String for Lit's
    // attribute-vs-property bridge, but in practice consumers set objects
    // and arrays via property binding. Cast to any so the narrowing below
    // can branch on the actual runtime shape.
    const v = /** @type {any} */ (this.value);
    if (v == null) return "";
    if (typeof v === "string") return v;
    const { type, format } = this.schema || {};
    if (type === "array" && format === "newline-array" && Array.isArray(v)) {
      return v.join("\n");
    }
    if (type === "object" || type === "array") {
      try {
        return JSON.stringify(v, null, 4);
      } catch {
        return String(v);
      }
    }
    return String(v);
  }

  _renderInput() {
    const { type, format, description } = this.schema;
    const fieldEnum = this.schema.enum;
    const placeholder = this.schema["x-cts-placeholder"] || "";
    const isInvalid = Boolean(this.error);
    const describedBy = this._describedByIds();
    const ariaInvalid = isInvalid ? "true" : nothing;
    const ariaDescribedBy = describedBy || nothing;
    const displayValue = this._displayValue();

    if (fieldEnum) {
      const enumLabels = Array.isArray(this.schema.enumLabels) ? this.schema.enumLabels : null;
      const hasEmptyOption = fieldEnum.includes("");
      return html`
        <select
          id="${this._uid}"
          class=${classMap({ "oidf-select": true, "is-error": isInvalid })}
          .value=${displayValue}
          ?disabled=${this.disabled}
          aria-invalid=${ariaInvalid}
          aria-describedby=${ariaDescribedBy}
          @change=${this._handleInput}
        >
          ${hasEmptyOption ? nothing : html`<option value="">Select...</option>`}
          ${this._renderEnumOptions(fieldEnum, enumLabels)}
        </select>
      `;
    }

    if (type === "object" || type === "array" || format === "json") {
      return html`
        <textarea
          id="${this._uid}"
          class=${classMap({
            "oidf-textarea": true,
            "is-mono": true,
            "is-error": isInvalid,
          })}
          rows="6"
          .value=${displayValue}
          ?disabled=${this.disabled}
          aria-invalid=${ariaInvalid}
          aria-describedby=${ariaDescribedBy}
          @input=${this._handleInput}
          placeholder=${placeholder}
        ></textarea>
      `;
    }

    if (format === "password") {
      return html`<input
        type="password"
        id="${this._uid}"
        class=${classMap({ "oidf-input": true, "is-error": isInvalid })}
        .value=${displayValue}
        ?disabled=${this.disabled}
        aria-invalid=${ariaInvalid}
        aria-describedby=${ariaDescribedBy}
        @input=${this._handleInput}
      />`;
    }

    if (type === "boolean") {
      return html`
        <div class="oidf-checkbox-row">
          <input
            type="checkbox"
            id="${this._uid}"
            class="oidf-checkbox"
            .checked=${this.value === "true" || /** @type {unknown} */ (this.value) === true}
            ?disabled=${this.disabled}
            aria-invalid=${ariaInvalid}
            aria-describedby=${ariaDescribedBy}
            @change=${this._handleCheckbox}
          />
          ${description
            ? html`<label class="oidf-checkbox-label" for="${this._uid}">${description}</label>`
            : nothing}
        </div>
      `;
    }

    // Multi-line affordance for PEM / JWKS / key fields whose names match
    // the suffix matcher in lib/config-field-types.js. URLs (format=uri) are
    // always single-line and short-circuit the lookup so `server.jwks_uri`
    // stays an `<input type=url>` even though its leaf ends in `_uri`.
    if (format !== "uri" && isMultiLineConfigField(this.name)) {
      return html`
        <textarea
          id="${this._uid}"
          class=${classMap({
            "oidf-textarea": true,
            "is-mono": true,
            "is-error": isInvalid,
          })}
          rows="6"
          .value=${displayValue}
          ?disabled=${this.disabled}
          aria-invalid=${ariaInvalid}
          aria-describedby=${ariaDescribedBy}
          @input=${this._handleInput}
          placeholder=${placeholder}
        ></textarea>
      `;
    }

    const inputType = format === "uri" ? "url" : "text";
    return html`<input
      type="${inputType}"
      id="${this._uid}"
      class=${classMap({ "oidf-input": true, "is-error": isInvalid })}
      .value=${displayValue}
      ?disabled=${this.disabled}
      aria-invalid=${ariaInvalid}
      aria-describedby=${ariaDescribedBy}
      @input=${this._handleInput}
      placeholder=${placeholder}
    />`;
  }

  _renderEnumOptions(fieldEnum, enumLabels) {
    return fieldEnum.map((opt, i) => {
      const label = enumLabels && enumLabels[i] != null ? enumLabels[i] : opt;
      return html`<option value="${opt}" ?selected=${this.value === opt}>${label}</option>`;
    });
  }

  render() {
    const { title, description, type } = this.schema;
    const isBoolean = type === "boolean";
    return html`
      <div class="oidf-form-field">
        ${!isBoolean && title
          ? html`<label class="oidf-label" for="${this._uid}">${title}</label>`
          : nothing}
        ${this._renderInput()}
        ${this.error
          ? html`<span id="${this._uid}-error" class="oidf-error" role="alert">${this.error}</span>`
          : nothing}
        ${!isBoolean && description
          ? html`<span id="${this._uid}-help" class="oidf-help t-meta">${description}</span>`
          : nothing}
      </div>
    `;
  }
}
customElements.define("cts-form-field", CtsFormField);
