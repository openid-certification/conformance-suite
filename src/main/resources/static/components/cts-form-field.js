import { LitElement, html, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";

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
 * @property {object} schema - JSON-schema fragment for this field. May include
 *   `type`, `format`, `enum`, `title`, `description`.
 * @property {string} name - Field name used as the `field` key in
 *   `cts-field-change` events.
 * @property {string} value - Current field value (always stringified).
 * @property {string} error - Validation error message shown below the input.
 * @property {boolean} disabled - Disables the input.
 * @fires cts-field-change - On every input/change with
 *   `{ detail: { field, value } }`; bubbles.
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
  height: 34px;
  padding-top: 0;
  padding-bottom: 0;
}
.oidf-form-field .oidf-textarea {
  min-height: calc(var(--space-6) * 4);
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
    this.dispatchEvent(
      new CustomEvent("cts-field-change", {
        bubbles: true,
        detail: { field: this.name, value: e.target.value },
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

  _renderInput() {
    const { type, format, description } = this.schema;
    const fieldEnum = this.schema.enum;
    const isInvalid = Boolean(this.error);
    const describedBy = this._describedByIds();
    const ariaInvalid = isInvalid ? "true" : nothing;
    const ariaDescribedBy = describedBy || nothing;

    if (fieldEnum) {
      return html`
        <select
          id="${this._uid}"
          class=${classMap({ "oidf-select": true, "is-error": isInvalid })}
          .value=${this.value || ""}
          ?disabled=${this.disabled}
          aria-invalid=${ariaInvalid}
          aria-describedby=${ariaDescribedBy}
          @change=${this._handleInput}
        >
          <option value="">Select...</option>
          ${this._renderEnumOptions(fieldEnum)}
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
          .value=${this.value || ""}
          ?disabled=${this.disabled}
          aria-invalid=${ariaInvalid}
          aria-describedby=${ariaDescribedBy}
          @input=${this._handleInput}
          placeholder=${description || ""}
        ></textarea>
      `;
    }

    if (format === "password") {
      return html`<input
        type="password"
        id="${this._uid}"
        class=${classMap({ "oidf-input": true, "is-error": isInvalid })}
        .value=${this.value || ""}
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

    const inputType = format === "uri" ? "url" : "text";
    return html`<input
      type="${inputType}"
      id="${this._uid}"
      class=${classMap({ "oidf-input": true, "is-error": isInvalid })}
      .value=${this.value || ""}
      ?disabled=${this.disabled}
      aria-invalid=${ariaInvalid}
      aria-describedby=${ariaDescribedBy}
      @input=${this._handleInput}
      placeholder=${description || ""}
    />`;
  }

  _renderEnumOptions(fieldEnum) {
    return fieldEnum.map(
      (opt) => html`<option value="${opt}" ?selected=${this.value === opt}>${opt}</option>`,
    );
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
