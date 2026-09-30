/**
 * Pure helpers behind `<cts-settings-page>`: turning the server's redacted
 * view into an editable draft, and the draft into the PUT body.
 *
 * The server never sends stored secrets, so a secret input starts empty and
 * an empty one is left out of the request, which tells the server to keep
 * the stored value.
 */

/** Admin-only endpoint for the Chile CMF Directorio section. */
export const CMF_CHILE_ENDPOINT = "/api/admin/settings/cmf-chile";

/**
 * @typedef {object} CertificateView
 * @property {string} id - Server-assigned entry id.
 * @property {string} label - Admin-chosen name.
 * @property {string} certificateChainPem - The chain, leaf first.
 * @property {string} [subject] - Leaf subject DN.
 * @property {string} [issuer] - Leaf issuer DN.
 * @property {string} [notAfter] - Leaf expiry, ISO-8601.
 * @property {boolean} privateKeySet - Whether a key is stored.
 */

/**
 * @typedef {object} SettingsView
 * @property {string} [directoryTokenEndpoint] - Directorio token endpoint.
 * @property {string} [clientId] - Directorio client id.
 * @property {boolean} clientSecretSet - Whether a client secret is stored.
 * @property {{set: boolean, keys: Array<{kid?: string, kty: string, alg?: string, use?: string}>}} [clientJwks] - Stored JWKS summary.
 * @property {Array<CertificateView>} [positiveCertificates] - Positive DCR certificates.
 * @property {Array<CertificateView>} [negativeCertificates] - Negative DCR certificates.
 * @property {string} [updatedAt] - Last save, ISO-8601.
 * @property {string} [updatedBy] - Who saved last.
 */

/**
 * @typedef {object} EntryDraft
 * @property {string|null} id - Null for an entry not yet saved.
 * @property {string} label - Label input.
 * @property {string} certificateChainPem - Certificate chain input.
 * @property {string} privateKeyPem - Private key input; empty keeps the stored key.
 * @property {boolean} replaceKey - Whether the key input is shown.
 * @property {{subject: string|null, issuer: string|null, notAfter: string|null, privateKeySet: boolean}|null} stored - What the server reported; null for a new entry.
 */

/**
 * @typedef {object} SettingsDraft
 * @property {string} directoryTokenEndpoint - Token endpoint input.
 * @property {string} clientId - Client id input.
 * @property {string} clientSecret - Secret input; empty keeps the stored secret.
 * @property {boolean} clearClientSecret - Remove the stored secret.
 * @property {boolean} replaceJwks - Whether the JWKS editor is shown.
 * @property {string} clientJwksText - JWKS editor text.
 * @property {Array<EntryDraft>} positiveCertificates - Positive entries.
 * @property {Array<EntryDraft>} negativeCertificates - Negative entries.
 */

/** @typedef {{field: string, message: string}} SettingsError */

/**
 * @typedef {object} EntryRequest
 * @property {string} [id] - Omitted for a new entry.
 * @property {string} label - Entry label.
 * @property {string} certificateChainPem - Certificate chain.
 * @property {string} [privateKeyPem] - Omitted to keep the stored key.
 */

/**
 * @typedef {object} SettingsRequest
 * @property {string} directoryTokenEndpoint - Token endpoint.
 * @property {string} clientId - Client id.
 * @property {string} [clientSecret] - Omitted to keep the stored secret.
 * @property {boolean} [clearClientSecret] - Remove the stored secret.
 * @property {object} [clientJwks] - Omitted to keep the stored JWKS.
 * @property {Array<EntryRequest>} positiveCertificates - Positive entries.
 * @property {Array<EntryRequest>} negativeCertificates - Negative entries.
 */

/**
 * @param {CertificateView} entry - An entry from the server's view.
 * @returns {EntryDraft} The editable entry.
 */
function entryDraftFromView(entry) {
  return {
    id: entry.id,
    label: entry.label ?? "",
    certificateChainPem: entry.certificateChainPem ?? "",
    privateKeyPem: "",
    replaceKey: false,
    stored: {
      subject: entry.subject ?? null,
      issuer: entry.issuer ?? null,
      notAfter: entry.notAfter ?? null,
      privateKeySet: entry.privateKeySet === true,
    },
  };
}

/**
 * @param {SettingsView} view - The server's redacted view.
 * @returns {SettingsDraft} The editable draft, with every secret input empty.
 */
export function draftFromView(view) {
  return {
    directoryTokenEndpoint: view.directoryTokenEndpoint ?? "",
    clientId: view.clientId ?? "",
    clientSecret: "",
    clearClientSecret: false,
    replaceJwks: false,
    clientJwksText: "",
    positiveCertificates: (view.positiveCertificates ?? []).map(entryDraftFromView),
    negativeCertificates: (view.negativeCertificates ?? []).map(entryDraftFromView),
  };
}

/** @returns {EntryDraft} A blank entry whose key input is shown from the start. */
export function newEntryDraft() {
  return {
    id: null,
    label: "",
    certificateChainPem: "",
    privateKeyPem: "",
    replaceKey: true,
    stored: null,
  };
}

/**
 * @param {EntryDraft} entry - An editable entry.
 * @returns {EntryRequest} The entry as the PUT body carries it.
 */
function entryRequest(entry) {
  /** @type {EntryRequest} */
  const request = { label: entry.label.trim(), certificateChainPem: entry.certificateChainPem };
  if (entry.id) request.id = entry.id;
  if (entry.privateKeyPem !== "") request.privateKeyPem = entry.privateKeyPem;
  return request;
}

/**
 * @param {SettingsDraft} draft - The editable draft.
 * @returns {{body: SettingsRequest, errors: Array<SettingsError>}} The PUT body, and any
 *   error found before sending it.
 */
export function requestFromDraft(draft) {
  /** @type {Array<SettingsError>} */
  const errors = [];
  /** @type {SettingsRequest} */
  const body = {
    directoryTokenEndpoint: draft.directoryTokenEndpoint.trim(),
    clientId: draft.clientId.trim(),
    positiveCertificates: draft.positiveCertificates.map(entryRequest),
    negativeCertificates: draft.negativeCertificates.map(entryRequest),
  };
  if (draft.clearClientSecret) {
    body.clearClientSecret = true;
  } else if (draft.clientSecret !== "") {
    body.clientSecret = draft.clientSecret;
  }
  if (draft.replaceJwks && draft.clientJwksText.trim() !== "") {
    let parsed;
    try {
      parsed = JSON.parse(draft.clientJwksText);
    } catch {
      parsed = undefined;
    }
    if (parsed !== null && typeof parsed === "object" && !Array.isArray(parsed)) {
      body.clientJwks = parsed;
    } else {
      errors.push({ field: "clientJwks", message: "'Client JWKS' must be a JSON object" });
    }
  }
  return { body, errors };
}

/**
 * @param {Array<SettingsError>} errors - Errors from the last save attempt.
 * @param {string} field - A request field path, e.g. `positiveCertificates[0].label`.
 * @returns {string} The message for that field, or "".
 */
export function errorFor(errors, field) {
  return errors.find((error) => error.field === field)?.message ?? "";
}

/**
 * @param {string|null} notAfter - An ISO-8601 expiry.
 * @param {number} [now] - Current time in ms, for tests.
 * @returns {boolean} Whether the expiry has passed.
 */
export function isExpired(notAfter, now = Date.now()) {
  return notAfter != null && Date.parse(notAfter) < now;
}
