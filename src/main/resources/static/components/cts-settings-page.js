import { LitElement, html, nothing, css } from "lit";
import { ifDefined } from "lit/directives/if-defined.js";
import "./cts-alert.js";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-json-editor.js";
import "./cts-loading-state.js";
import "./cts-time.js";
import { ctsToast } from "../js/cts-toast-api.js";
import {
  CMF_CHILE_ENDPOINT,
  draftFromView,
  errorFor,
  isExpired,
  newEntryDraft,
  requestFromDraft,
} from "./settings-model.js";

/** @typedef {import("./settings-model.js").SettingsView} SettingsView */
/** @typedef {import("./settings-model.js").SettingsDraft} SettingsDraft */
/** @typedef {import("./settings-model.js").EntryDraft} EntryDraft */
/** @typedef {import("./settings-model.js").SettingsError} SettingsError */
/** @typedef {"positiveCertificates"|"negativeCertificates"} ListKey */

/**
 * One of the two certificate lists the page edits.
 * @typedef {object} CertificateList
 * @property {ListKey} key - The draft and request field holding the list.
 * @property {string} label - Visible legend; the server's messages use it too.
 * @property {string} hint - Guidance shown under the legend.
 */

const STYLE_ID = "cts-settings-page-styles";

const STYLE_TEXT = css`
  .cts-settings-page {
    display: block;
    font-family: var(--font-sans);
    color: var(--fg);
  }
  .cts-settings-section {
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    padding: var(--space-4);
    margin-bottom: var(--space-4);
    background: var(--bg-elev, transparent);
  }
  .cts-settings-section h2 {
    font-size: var(--fs-18, 1.125rem);
    margin: 0 0 var(--space-2) 0;
  }
  .cts-settings-meta {
    font-size: var(--fs-13);
    color: var(--fg-muted);
    margin: 0 0 var(--space-3) 0;
  }
  .cts-settings-field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin-bottom: var(--space-3);
  }
  .cts-settings-field > label,
  .cts-settings-label {
    font-weight: 600;
    font-size: var(--fs-13);
  }
  .cts-settings-input {
    font: inherit;
    font-size: var(--fs-13);
    padding: var(--space-2);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    background: var(--bg, #fff);
    color: var(--fg);
  }
  .cts-settings-pem {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    resize: vertical;
  }
  .cts-settings-check {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--fs-13);
  }
  .cts-settings-error {
    margin: 0;
    color: var(--rust-500);
    font-size: var(--fs-13);
  }
  .cts-settings-keys {
    margin: 0 0 var(--space-2) 0;
    padding-left: var(--space-4);
    font-size: var(--fs-13);
  }
  .cts-settings-list {
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    padding: var(--space-3);
    margin: 0 0 var(--space-3) 0;
  }
  .cts-settings-list legend {
    font-weight: 600;
    padding: 0 var(--space-1);
  }
  .cts-settings-entry {
    border-top: 1px solid var(--border);
    padding-top: var(--space-3);
    margin-bottom: var(--space-3);
  }
  .cts-settings-cert-meta {
    display: grid;
    grid-template-columns: max-content 1fr;
    gap: var(--space-1) var(--space-3);
    margin: 0 0 var(--space-3) 0;
    font-size: var(--fs-13);
  }
  .cts-settings-cert-meta dt {
    color: var(--fg-muted);
  }
  .cts-settings-cert-meta dd {
    margin: 0;
    overflow-wrap: anywhere;
  }
  .cts-settings-summary {
    border: 1px solid var(--rust-500);
    border-radius: var(--radius-2);
    padding: var(--space-3);
    margin-bottom: var(--space-4);
    font-size: var(--fs-13);
  }
  .cts-settings-summary p,
  .cts-settings-summary ul {
    margin: 0;
  }
  .cts-settings-actions {
    display: flex;
    gap: var(--space-2);
  }
`;

/**
 * Inject the cts-settings-page scoped stylesheet into `<head>` once.
 * @returns {void}
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/** @type {Array<CertificateList>} */
const CERTIFICATE_LISTS = [
  {
    key: "positiveCertificates",
    label: "Positive DCR client certificates",
    hint: "Certificates the suite registers valid clients with.",
  },
  {
    key: "negativeCertificates",
    label: "Negative DCR client certificates",
    hint: 'Certificates a server must refuse. Name each after its defect, e.g. "expired" or "untrusted CA".',
  },
];

/**
 * Admin-only server settings editor. Loads the Chile CMF Directorio section
 * from `/api/admin/settings/cmf-chile` and saves it back with PUT.
 *
 * Stored secrets never reach the browser: the view says only whether each
 * is set, and a secret input left empty keeps the stored value. After a
 * save the form is rebuilt from the server's response, so whatever was typed
 * into a secret input is gone from the DOM. The endpoint's 403 is the admin
 * check; the page does not ask `/api/currentuser`.
 */
class CtsSettingsPage extends LitElement {
  static properties = {
    _status: { state: true },
    _loadError: { state: true },
    _view: { state: true },
    _draft: { state: true },
    _errors: { state: true },
    _saving: { state: true },
  };

  constructor() {
    super();
    /** @type {"loading"|"ready"|"forbidden"|"unauthenticated"|"error"} */
    this._status = "loading";
    this._loadError = "";
    /** @type {SettingsView|null} */
    this._view = null;
    /** @type {SettingsDraft|null} */
    this._draft = null;
    /** @type {Array<SettingsError>} */
    this._errors = [];
    this._saving = false;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    this._load();
  }

  async _load() {
    this._status = "loading";
    try {
      const response = await fetch(CMF_CHILE_ENDPOINT);
      if (this._handleAuthStatus(response)) return;
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      this._applyView(await response.json());
    } catch (error) {
      this._loadError = error instanceof Error ? error.message : String(error);
      this._status = "error";
    }
  }

  /**
   * @param {Response} response - A response from the settings endpoint.
   * @returns {boolean} Whether it ended the page's session state.
   */
  _handleAuthStatus(response) {
    if (response.status === 401) {
      this._status = "unauthenticated";
      return true;
    }
    if (response.status === 403) {
      this._status = "forbidden";
      return true;
    }
    return false;
  }

  /** @param {SettingsView} view - The server's redacted view. */
  _applyView(view) {
    this._view = view;
    this._draft = draftFromView(view);
    this._errors = [];
    this._status = "ready";
  }

  async _save() {
    if (!this._draft) return;
    const { body, errors } = requestFromDraft(this._draft);
    if (errors.length > 0) {
      this._errors = errors;
      return;
    }
    this._saving = true;
    try {
      const response = await fetch(CMF_CHILE_ENDPOINT, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (this._handleAuthStatus(response)) return;
      if (response.status === 400) {
        const refused = await response.json();
        this._errors = Array.isArray(refused.errors) ? refused.errors : [];
        return;
      }
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      this._applyView(await response.json());
      ctsToast({ title: "Settings saved" });
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this._errors = [{ field: "", message: `Saving failed: ${message}` }];
    } finally {
      this._saving = false;
    }
  }

  _onSubmit(/** @type {Event} */ event) {
    event.preventDefault();
    this._save();
  }

  _onDiscard() {
    if (this._view) this._applyView(this._view);
  }

  /**
   * @param {keyof SettingsDraft} name - Draft field.
   * @param {unknown} value - New value.
   */
  _setField(name, value) {
    this._draft = /** @type {SettingsDraft} */ ({ ...this._draft, [name]: value });
  }

  /**
   * Input handler for top-level text fields; the control's `data-field`
   * names the draft field.
   * @param {Event} event - `input` from an input or `<cts-json-editor>`.
   */
  _onFieldInput(event) {
    const control = /** @type {HTMLInputElement} */ (event.currentTarget);
    this._setField(/** @type {keyof SettingsDraft} */ (control.dataset.field), control.value);
  }

  /** @param {Event} event - `change` from the clear-secret checkbox. */
  _onClearSecretChange(event) {
    const checked = /** @type {HTMLInputElement} */ (event.currentTarget).checked;
    this._draft = /** @type {SettingsDraft} */ ({
      ...this._draft,
      clearClientSecret: checked,
      clientSecret: "",
    });
  }

  _onReplaceJwks() {
    this._setField("replaceJwks", true);
  }

  /**
   * @param {Event} event - An event from a control inside a certificate entry.
   * @returns {{listKey: ListKey, index: number, control: HTMLInputElement}} Which entry it belongs to.
   */
  _entryTarget(event) {
    const control = /** @type {HTMLInputElement} */ (event.currentTarget);
    return {
      listKey: /** @type {ListKey} */ (control.dataset.list),
      index: Number(control.dataset.index),
      control,
    };
  }

  /**
   * @param {ListKey} listKey - Which certificate list.
   * @param {number} index - Entry position.
   * @param {Partial<EntryDraft>} patch - Fields to change.
   */
  _setEntry(listKey, index, patch) {
    if (!this._draft) return;
    const entries = this._draft[listKey].map((entry, i) =>
      i === index ? { ...entry, ...patch } : entry,
    );
    this._setField(listKey, entries);
  }

  /** @param {Event} event - `input` from an entry control; `data-entry-field` names the field. */
  _onEntryInput(event) {
    const { listKey, index, control } = this._entryTarget(event);
    this._setEntry(listKey, index, { [String(control.dataset.entryField)]: control.value });
  }

  /** @param {Event} event - `cts-click` from an entry's Replace key button. */
  _onReplaceKey(event) {
    const { listKey, index } = this._entryTarget(event);
    this._setEntry(listKey, index, { replaceKey: true });
  }

  /** @param {Event} event - `cts-click` from a list's Add certificate button. */
  _onAddEntry(event) {
    const { listKey } = this._entryTarget(event);
    if (!this._draft) return;
    this._setField(listKey, [...this._draft[listKey], newEntryDraft()]);
  }

  /** @param {Event} event - `cts-click` from an entry's Remove button. */
  _onRemoveEntry(event) {
    const { listKey, index } = this._entryTarget(event);
    if (!this._draft) return;
    this._setField(
      listKey,
      this._draft[listKey].filter((_, i) => i !== index),
    );
    // server errors are keyed by entry position, which has just shifted
    this._errors = [];
  }

  render() {
    switch (this._status) {
      case "loading":
        return html`<cts-loading-state label="Loading settings"></cts-loading-state>`;
      case "forbidden":
        return html`<cts-alert variant="warning" data-testid="settings-forbidden"
          >Server settings are only available to administrators.</cts-alert
        >`;
      case "unauthenticated":
        return html`<cts-alert variant="warning" data-testid="settings-forbidden"
          >Your session has expired. Sign in again to manage server settings.</cts-alert
        >`;
      case "error":
        return html`<cts-alert variant="danger" data-testid="settings-load-error"
          >Could not load the server settings: ${this._loadError}</cts-alert
        >`;
      default:
        return this._renderForm();
    }
  }

  _renderForm() {
    const view = this._view;
    if (!view || !this._draft) return nothing;
    return html`
      <form class="cts-settings-page" novalidate @submit=${this._onSubmit}>
        ${this._errors.length > 0
          ? html`<div class="cts-settings-summary" role="alert" data-testid="settings-errors">
              <p>The settings were not saved:</p>
              <ul>
                ${this._errors.map((error) => html`<li>${error.message}</li>`)}
              </ul>
            </div>`
          : nothing}
        <section class="cts-settings-section" aria-labelledby="settings-cmf-chile-heading">
          <h2 id="settings-cmf-chile-heading">Chile (CMF) — Directorio</h2>
          <p class="cts-settings-meta" data-testid="settings-last-saved">
            ${view.updatedAt
              ? html`Last saved
                  <cts-time mode="compact" value=${view.updatedAt}></cts-time>
                  by ${view.updatedBy ?? "unknown"}.`
              : "Not configured yet."}
          </p>
          ${this._textField("directoryTokenEndpoint", "Directory token endpoint URL", "url")}
          ${this._textField("softwareStatementEndpoint", "Software statement endpoint URL", "url")}
          ${this._textField("clientId", "Directory client ID", "text")} ${this._secretField()}
          ${this._jwksField()} ${CERTIFICATE_LISTS.map((list) => this._certificateList(list))}
        </section>
        <div class="cts-settings-actions">
          <cts-button
            variant="primary"
            type="submit"
            label="Save"
            ?loading=${this._saving}
          ></cts-button>
          <cts-button
            variant="secondary"
            label="Discard changes"
            ?disabled=${this._saving}
            @cts-click=${this._onDiscard}
          ></cts-button>
        </div>
      </form>
    `;
  }

  /**
   * Attributes tying a control to its error line, so assistive technology
   * announces the message with the control.
   * @param {string} inputId - The control's id.
   * @param {string} field - Request field path.
   * @returns {{invalid: "true"|"false", describedBy: string|undefined}} The `aria-invalid` value, and the
   *   `aria-describedby` id when there is an error.
   */
  _errorAria(inputId, field) {
    const invalid = errorFor(this._errors, field) !== "";
    return {
      invalid: invalid ? "true" : "false",
      describedBy: invalid ? `${inputId}-error` : undefined,
    };
  }

  /**
   * @param {string} inputId - The control's id.
   * @param {string} field - Request field path.
   * @returns {unknown} The error line, or nothing.
   */
  _error(inputId, field) {
    const message = errorFor(this._errors, field);
    return message
      ? html`<p id=${`${inputId}-error`} class="cts-settings-error" data-error-for=${field}>
          ${message}
        </p>`
      : nothing;
  }

  /**
   * @param {"directoryTokenEndpoint"|"softwareStatementEndpoint"|"clientId"} name - Draft field.
   * @param {string} label - Visible label.
   * @param {"url"|"text"} type - Input type.
   * @returns {unknown} The field.
   */
  _textField(name, label, type) {
    const id = `settings-${name}`;
    const aria = this._errorAria(id, name);
    return html`<div class="cts-settings-field">
      <label for=${id}>${label}</label>
      <input
        id=${id}
        type=${type}
        class="cts-settings-input"
        data-field=${name}
        .value=${this._draft?.[name] ?? ""}
        aria-invalid=${aria.invalid}
        aria-describedby=${ifDefined(aria.describedBy)}
        @input=${this._onFieldInput}
      />
      ${this._error(id, name)}
    </div>`;
  }

  _secretField() {
    const id = "settings-clientSecret";
    const isSet = this._view?.clientSecretSet === true;
    const clearing = this._draft?.clearClientSecret === true;
    const aria = this._errorAria(id, "clientSecret");
    return html`<div class="cts-settings-field">
      <label for=${id}>Directory client secret</label>
      <input
        id=${id}
        type="password"
        autocomplete="new-password"
        class="cts-settings-input"
        data-field="clientSecret"
        placeholder=${isSet ? "set — leave blank to keep" : "not set"}
        .value=${this._draft?.clientSecret ?? ""}
        ?disabled=${clearing}
        aria-invalid=${aria.invalid}
        aria-describedby=${ifDefined(aria.describedBy)}
        @input=${this._onFieldInput}
      />
      ${isSet
        ? html`<label class="cts-settings-check">
            <input type="checkbox" .checked=${clearing} @change=${this._onClearSecretChange} />
            Clear the stored secret
          </label>`
        : nothing}
      ${this._error(id, "clientSecret")}
    </div>`;
  }

  _jwksField() {
    const jwks = this._view?.clientJwks ?? { set: false, keys: [] };
    return html`<div class="cts-settings-field">
      <span class="cts-settings-label" id="settings-clientJwks-label">Client JWKS</span>
      ${jwks.set
        ? html`<ul class="cts-settings-keys" aria-labelledby="settings-clientJwks-label">
            ${jwks.keys.map(
              (key) =>
                html`<li>
                  <code>${key.kid ?? "(no kid)"}</code> ${key.kty}${key.alg
                    ? ` · ${key.alg}`
                    : ""}${key.use ? ` · ${key.use}` : ""}
                </li>`,
            )}
          </ul>`
        : html`<p class="cts-settings-meta">Not set.</p>`}
      ${this._draft?.replaceJwks
        ? html`<p class="cts-settings-meta">
              Paste the full JWKS, private keys included. It replaces the stored one when you save.
            </p>
            <cts-json-editor
              placeholder='{"keys": [ ... ]}'
              data-field="clientJwksText"
              .value=${this._draft.clientJwksText}
              @input=${this._onFieldInput}
            ></cts-json-editor>`
        : html`<div>
            <cts-button
              variant="secondary"
              size="xs"
              label=${jwks.set ? "Replace JWKS" : "Set JWKS"}
              @cts-click=${this._onReplaceJwks}
            ></cts-button>
          </div>`}
      ${this._error("settings-clientJwks", "clientJwks")}
    </div>`;
  }

  /**
   * @param {CertificateList} list - Which list.
   * @returns {unknown} The list.
   */
  _certificateList({ key, label, hint }) {
    const entries = this._draft?.[key] ?? [];
    return html`<fieldset class="cts-settings-list" data-testid=${`settings-${key}`}>
      <legend>${label}</legend>
      <p class="cts-settings-meta">${hint}</p>
      ${entries.length === 0 ? html`<p class="cts-settings-meta">No certificates.</p>` : nothing}
      ${entries.map((entry, index) => this._certificateEntry(key, label, entry, index))}
      <cts-button
        variant="secondary"
        size="xs"
        label="Add certificate"
        aria-label=${`Add a certificate to ${label}`}
        data-list=${key}
        @cts-click=${this._onAddEntry}
      ></cts-button>
    </fieldset>`;
  }

  /**
   * @param {ListKey} listKey - Which list.
   * @param {string} listLabel - The list's visible label.
   * @param {EntryDraft} entry - The entry.
   * @param {number} index - Its position.
   * @returns {unknown} The entry.
   */
  _certificateEntry(listKey, listLabel, entry, index) {
    const field = `${listKey}[${index}]`;
    const idBase = `settings-${listKey}-${index}`;
    const stored = entry.stored;
    const name = entry.label || `entry ${index + 1}`;
    const labelAria = this._errorAria(`${idBase}-label`, `${field}.label`);
    const chainAria = this._errorAria(`${idBase}-chain`, `${field}.certificateChainPem`);
    const keyAria = this._errorAria(`${idBase}-key`, `${field}.privateKeyPem`);
    return html`<div class="cts-settings-entry" data-entry=${field}>
      <div class="cts-settings-field">
        <label for=${`${idBase}-label`}>Label</label>
        <input
          id=${`${idBase}-label`}
          class="cts-settings-input"
          data-list=${listKey}
          data-index=${index}
          data-entry-field="label"
          .value=${entry.label}
          aria-invalid=${labelAria.invalid}
          aria-describedby=${ifDefined(labelAria.describedBy)}
          @input=${this._onEntryInput}
        />
        ${this._error(`${idBase}-label`, `${field}.label`)}
      </div>
      ${stored?.subject
        ? html`<dl class="cts-settings-cert-meta">
            <dt>Subject</dt>
            <dd>${stored.subject}</dd>
            <dt>Issuer</dt>
            <dd>${stored.issuer}</dd>
            <dt>Expires</dt>
            <dd>
              <cts-time mode="compact" value=${stored.notAfter ?? ""}></cts-time>
              ${isExpired(stored.notAfter)
                ? html`<cts-badge variant="warn" label="Expired"></cts-badge>`
                : nothing}
            </dd>
          </dl>`
        : nothing}
      <div class="cts-settings-field">
        <label for=${`${idBase}-chain`}>Certificate chain (PEM, leaf first)</label>
        <textarea
          id=${`${idBase}-chain`}
          class="cts-settings-input cts-settings-pem"
          rows="6"
          spellcheck="false"
          data-list=${listKey}
          data-index=${index}
          data-entry-field="certificateChainPem"
          .value=${entry.certificateChainPem}
          aria-invalid=${chainAria.invalid}
          aria-describedby=${ifDefined(chainAria.describedBy)}
          @input=${this._onEntryInput}
        ></textarea>
        ${this._error(`${idBase}-chain`, `${field}.certificateChainPem`)}
      </div>
      <div class="cts-settings-field">
        ${entry.replaceKey
          ? html`<label for=${`${idBase}-key`}>Private key (PEM, unencrypted)</label>
              <textarea
                id=${`${idBase}-key`}
                class="cts-settings-input cts-settings-pem"
                rows="6"
                spellcheck="false"
                autocomplete="off"
                data-list=${listKey}
                data-index=${index}
                data-entry-field="privateKeyPem"
                .value=${entry.privateKeyPem}
                aria-invalid=${keyAria.invalid}
                aria-describedby=${ifDefined(keyAria.describedBy)}
                @input=${this._onEntryInput}
              ></textarea>`
          : html`<span class="cts-settings-label">Private key</span>
              <p class="cts-settings-meta">
                Set.
                <cts-button
                  variant="secondary"
                  size="xs"
                  label="Replace key"
                  aria-label=${`Replace the private key of ${name}`}
                  data-list=${listKey}
                  data-index=${index}
                  @cts-click=${this._onReplaceKey}
                ></cts-button>
              </p>`}
        ${this._error(`${idBase}-key`, `${field}.privateKeyPem`)}
      </div>
      <cts-button
        variant="danger"
        size="xs"
        label="Remove"
        aria-label=${`Remove ${name} from ${listLabel}`}
        data-list=${listKey}
        data-index=${index}
        @cts-click=${this._onRemoveEntry}
      ></cts-button>
    </div>`;
  }
}

customElements.define("cts-settings-page", CtsSettingsPage);
