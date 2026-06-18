/**
 * Per-field-name affordance hints for the conformance-suite config form.
 *
 * `config-field-catalog.json` types every leaf as `string` unless it is a
 * JSON object/array, so the catalog can't distinguish a single-line
 * identifier (`client.client_id`) from a multi-line PEM blob
 * (`client.certificate`). The legacy schedule-test page hand-wrote
 * `<textarea>` elements for the PEM-shaped fields; the new design-system
 * renderer derives the same affordance from the field name instead, per
 * KTD6 of the MR-1998 maintainer-feedback plan.
 *
 * The match is anchored to the leaf segment (everything after the last
 * `.`) so `client.certificate`, `client2.certificate`, and
 * `mtls.cert` all route through the same rule without the caller having
 * to split the path. The leaf must end with one of:
 *
 *   - `certificate` (covers `client.certificate`, `client2.certificate`)
 *   - `cert`        (covers `mtls.cert`, `mtls2.cert`)
 *   - `jwks`        (covers `client.org_jwks`, `vci.client_attester_keys_jwks`)
 *   - `key`         (covers `mtls.key`, `private_key`, `public_key`,
 *                    `vci.cwt_signing_key_jwk`)
 *   - `pem`         (covers `vci.*_trust_anchor_pem`)
 *
 * An optional trailing `_chain` is accepted (`certificate_chain` etc.).
 * `_uri` is excluded by the trailing `$` anchor — `server.jwks_uri` is a
 * single-line URL, not a PEM blob.
 */

const MULTILINE_LEAF_RE = /(?:^|_)(certificate|cert|jwks|key|pem)(?:_chain)?$/i;

/**
 * Whether a config-form field name should render as a `<textarea>` for
 * multi-line PEM / JWKS / key input. Inspects the leaf segment (after the
 * last `.`) so namespace prefixes (`client.`, `mtls.`, `vci.`) are
 * intentionally ignored.
 *
 * @param {string} name - Dotted config-field path, e.g. `client.certificate`.
 * @returns {boolean}
 */
export function isMultiLineConfigField(name) {
  if (!name || typeof name !== "string") return false;
  const leaf = name.includes(".") ? name.slice(name.lastIndexOf(".") + 1) : name;
  return MULTILINE_LEAF_RE.test(leaf);
}
