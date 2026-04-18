import { LitElement, html, nothing } from "lit";

/**
 * Renders a single form input driven by a JSON-schema fragment. Supports
 * string, boolean, enum, JSON (object/array), and password inputs.
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
    const invalidClass = this.error ? " is-invalid" : "";

    if (fieldEnum) {
      return html`
        <select
          class="form-select${invalidClass}"
          .value=${this.value || ""}
          ?disabled=${this.disabled}
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
          class="form-control font-monospace${invalidClass}"
          rows="6"
          .value=${this.value || ""}
          ?disabled=${this.disabled}
          @input=${this._handleInput}
          placeholder=${description || ""}
        ></textarea>
      `;
    }

    if (format === "password") {
      return html`<input
        type="password"
        class="form-control${invalidClass}"
        .value=${this.value || ""}
        ?disabled=${this.disabled}
        @input=${this._handleInput}
      />`;
    }

    if (type === "boolean") {
      return html`
        <div class="form-check">
          <input
            type="checkbox"
            class="form-check-input"
            .checked=${this.value === "true" || /** @type {unknown} */ (this.value) === true}
            ?disabled=${this.disabled}
            @change=${this._handleCheckbox}
          />
          ${description ? html`<label class="form-check-label">${description}</label>` : nothing}
        </div>
      `;
    }

    const inputType = format === "uri" ? "url" : "text";
    return html`<input
      type="${inputType}"
      class="form-control${invalidClass}"
      .value=${this.value || ""}
      ?disabled=${this.disabled}
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
      <div class="mb-3">
        ${!isBoolean && title ? html`<label class="form-label fw-bold">${title}</label>` : nothing}
        ${this._renderInput()}
        ${this.error ? html`<div class="invalid-feedback d-block">${this.error}</div>` : nothing}
        ${!isBoolean && description
          ? html`<small class="form-text text-muted">${description}</small>`
          : nothing}
      </div>
    `;
  }
}
customElements.define("cts-form-field", CtsFormField);
