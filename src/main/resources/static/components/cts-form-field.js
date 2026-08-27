import { LitElement, html, nothing, css, unsafeCSS } from "lit";
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
 * ## Catalog-gap fallback (value-shape JSON mode)
 *
 * Backend `configurationFields` missing from `config-field-catalog.json` get
 * a `{ type: "string" }` fallback schema from the adapter, but their bound
 * value can still be a JS object/array (e.g. `client.verifier_info`). When
 * the runtime value is a non-null object/array and the schema doesn't already
 * say so, the field LATCHES into JSON mode: it renders the JSON textarea,
 * pretty-prints the value, and parses edits exactly like a declared
 * `type: "object"`/`"array"` field. The latch is sticky for the element's
 * lifetime so that an invalid-JSON intermediate edit (which the consumer
 * stores back as a raw string) does not flip the control to a single-line
 * `<input>` mid-edit.
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
 * `<input>` so multi-line credentials paste cleanly. The textarea auto-grows
 * to fit content up to `max-height` via `autoGrowTextarea()` (module-level
 * helper, called from `_handleInput` on keystroke and `updated()` for
 * external `.value` writes) rather than the CSS `field-sizing: content`
 * property: Safari's implementation clamps height against `max-height`
 * correctly but ignores an explicit `max-width` entirely, letting a long
 * unbroken value (a pasted JWKS x5c certificate) grow the box wider with
 * every character instead of wrapping (confirmed via Safari's own
 * Computed-styles panel). Driving height from JS sidesteps that CSS
 * feature altogether — width is governed purely by plain `width: 100%`,
 * which needs no auto-sizing feature to work reliably everywhere.
 *
 * The very first `autoGrowTextarea()` call (on mount, before user input)
 * can under-measure: `--font-mono` (JetBrains Mono) loads asynchronously
 * from Google Fonts, so `scrollHeight` may be read against a narrower
 * fallback font's metrics and never re-measured until the user types. Each
 * instance's `connectedCallback` calls `regrowOnceFontsSettle(this)`, which
 * re-grows its textarea once `document.fonts.ready` resolves, so a
 * pre-filled box nobody has typed into still lands at its true height
 * rather than the fallback-font one — regardless of whether fonts were
 * still loading or had already settled by the time it mounted.
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

/**
 * Resize a textarea's height to fit its content, clamped to its own
 * `max-height` (CSS `resize: vertical` can still push it taller by hand).
 * Replaces `field-sizing: content` — see the `.oidf-textarea`
 * CSS comment for why that CSS feature is unsafe to use here. Reset to
 * `"auto"` first so shrinking content (e.g. deleting a paste) reduces
 * `scrollHeight` correctly instead of measuring against the stale height.
 * @param {HTMLTextAreaElement} el - The textarea to resize.
 * @returns {void}
 */
function autoGrowTextarea(el) {
  el.style.height = "auto";
  const maxHeight = parseFloat(getComputedStyle(el).maxHeight);
  const target = Number.isFinite(maxHeight)
    ? Math.min(el.scrollHeight, maxHeight)
    : el.scrollHeight;
  el.style.height = `${target}px`;
}

/**
 * Re-grow `host`'s textarea (if it renders one) once web fonts have
 * settled, in addition to whatever grow already ran at call time.
 * Follow-up: `--font-mono` (JetBrains Mono) is a Google Fonts webfont,
 * loaded asynchronously. The very first `autoGrowTextarea()` call — on
 * initial mount, before the webfont has necessarily finished loading —
 * measures `scrollHeight` against whichever FALLBACK font is rendering at
 * that instant (`ui-monospace` / "SF Mono" on macOS Safari), which can
 * wrap fewer lines than JetBrains Mono's actual metrics once it swaps in.
 * Since `autoGrowTextarea()` otherwise only runs again on user input, a box
 * nobody has typed into stays stuck at the fallback-font height.
 *
 * Deliberately per-instance rather than one module-level listener shared
 * across every `cts-form-field`: `document.fonts.ready` resolves once and
 * stays resolved, but new fields keep mounting well after that first
 * resolution (e.g. every time the user picks a different plan/variant) —
 * a shared listener registered before those later fields exist would never
 * fire again for them. `.then()` on an already-resolved promise still
 * resolves (near-)immediately, so this is correct and cheap whether fonts
 * were still loading or had already settled by the time `host` connected.
 * @param {InstanceType<typeof CtsFormField>} host - The connecting field instance.
 * @returns {void}
 */
function regrowOnceFontsSettle(host) {
  if (typeof document === "undefined" || !document.fonts) return;
  document.fonts.ready.then(() => {
    if (!host.isConnected) return;
    const textarea = /** @type {HTMLTextAreaElement | null} */ (
      host.querySelector("textarea.oidf-textarea")
    );
    if (textarea) autoGrowTextarea(textarea);
  });
}

const STYLE_TEXT = css`
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
     (index.html etc). An explicit height overrides the JS-driven auto-grow
     below, so we reset to \`auto\` and let min-height / max-height carry the
     resting bounds. */
    height: auto;
    min-height: calc(var(--space-6) * 4);
    max-height: 50vh;
    resize: vertical;
    /* Long unbroken values (base64 x5c certs inside a pasted JWKS,
     PEM blobs, JWTs) need a mid-word break to actually wrap. Chrome and
     Firefox already force this inside a <textarea> without any CSS; Safari
     does not — it lets the line overflow horizontally instead, so this
     must be explicit for wrapping to work there too. */
    overflow-wrap: break-word;
    /* Auto-grow height is driven by autoGrowTextarea() in JS, NOT
     field-sizing: content. Verified via Safari's own Web Inspector Computed
     panel: field-sizing: content clamped height against max-height
     correctly (height == max-height once full) but completely ignored an
     explicit max-width: 100% — width read out at 8755px against a 100%
     ceiling. That's a real WebKit gap, not a specificity/ordering mistake
     to fix in CSS. Driving height from JS instead sidesteps field-sizing
     entirely, so width is governed only by the plain \`width: 100%\` above,
     which every browser already honors reliably with no auto-sizing
     feature involved. Do not reintroduce field-sizing here without
     re-verifying this exact failure mode in real Safari first. */
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
    background-image: ${unsafeCSS(SELECT_CHEVRON)};
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
  style.textContent = STYLE_TEXT.cssText;
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
    /**
     * Latched JSON mode for catalog-gap fields: "" (off), "object", or
     * "array". Set from the bound value's runtime shape in `willUpdate`;
     * never cleared, so invalid-JSON intermediate edits (raw strings) keep
     * the JSON textarea. See the class JSDoc.
     * @type {"" | "object" | "array"}
     */
    this._jsonValueMode = "";
  }

  willUpdate() {
    const v = /** @type {any} */ (this.value);
    if (v !== null && typeof v === "object") {
      this._jsonValueMode = Array.isArray(v) ? "array" : "object";
    }
  }

  /**
   * Grow the textarea (if this field renders one) to fit its current value
   * after every render where `value` changed — covers the initial mount
   * (a plan loaded with a pre-filled JWKS/PEM), external `.value` writes
   * (e.g. cts-config-form syncing from the JSON tab), and Lit's own
   * re-render after `_handleInput` updates `this.value` indirectly via the
   * parent. `_handleInput` also calls this directly on keystroke, so typing
   * doesn't wait on this async round-trip for a responsive feel.
   * @param {Map<string, unknown>} changed - Lit's changed-properties map.
   * @returns {void}
   */
  updated(changed) {
    if (!changed.has("value")) return;
    const textarea = /** @type {HTMLTextAreaElement | null} */ (
      this.querySelector("textarea.oidf-textarea")
    );
    if (textarea) autoGrowTextarea(textarea);
  }

  /**
   * Effective JSON type for this field: the declared schema type when it is
   * object/array, otherwise the latched value-shape mode ("" when neither).
   *
   * @returns {"" | "object" | "array"}
   */
  _effectiveJsonType() {
    const { type } = this.schema || {};
    if (type === "object" || type === "array") return type;
    return this._jsonValueMode;
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
    regrowOnceFontsSettle(this);
  }

  _handleInput(e) {
    const raw = e.target.value;
    const { type, format } = this.schema || {};
    const jsonType = this._effectiveJsonType();
    let value = raw;
    let parseError = "";

    // Grow immediately on keystroke — `updated()` also does this once
    // `this.value` round-trips back through the parent, but that's async;
    // this keeps typing responsive on every input event.
    if (e.target.tagName === "TEXTAREA") autoGrowTextarea(e.target);

    if (type === "array" && format === "newline-array") {
      // Newline-delimited array: split on `\n`, trim, drop empties.
      value = raw
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line !== "");
    } else if (jsonType) {
      // JSON textarea: empty string is the no-value sentinel; otherwise
      // try to parse. On failure, emit the raw string so the user can keep
      // editing, and surface the parse error via setCustomValidity so
      // submit is blocked at the browser layer (mirrors legacy
      // validateJSONFromFormElement behavior).
      if (raw.trim() === "") {
        value = jsonType === "array" ? [] : {};
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
    if (this._effectiveJsonType()) {
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

    if (this._effectiveJsonType() || format === "json") {
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
