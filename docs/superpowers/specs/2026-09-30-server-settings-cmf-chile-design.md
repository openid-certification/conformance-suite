# Server settings page — Chile CMF Directorio section

Date: 2026-09-30
Status: approved design, pending implementation plan

## Purpose

Give suite administrators a page to configure server-wide settings that test modules need but
testers must not supply or see. The first (and for now only) section holds the credentials the
suite uses as a participant of the Chilean Open Finance CMF Directorio, which the Chile CMF DCR
registration modules will use to obtain software statements (SSAs) and to register clients over
mTLS (see `2026-09-29-cmf-chile-dcr-discovery-pilot-design.md`, "Open points for later modules").

The section holds:

- the Directorio token endpoint URL;
- the Directorio software statement (SSA) generation endpoint URL;
- the Directorio client ID;
- the Directorio client secret;
- the client JWKS (with private keys);
- a named list of client certificates and private keys for positive DCR cases;
- a named list of client certificates and private keys for negative DCR cases.

## Constraints

- **The credentials belong to the suite operator. A tester must never see them**: not in the UI,
  the API, test logs, environment dumps or exported zips. This is different from the stance in
  AGENTS.md "Deliberate non-features" that private keys appear in logs, which covers keys the
  tester supplies.
- **Stored as plaintext in MongoDB for now.** Encryption at rest is planned for later, so all
  storage goes through one repository class, where encryption can be added without affecting
  any caller.
- Only admins can read or change the settings. API tokens are always `ROLE_USER`, so the settings
  are reachable only from an admin's browser session; there is no scripted provisioning.

## Out of scope

- Calling the Directorio token endpoint or obtaining an SSA.
- Encryption at rest.
- Sections for other ecosystems.
- Optimistic locking: saves are last-write-wins.

## Components

New package `net.openid.conformance.settings`:

| Unit | Purpose |
|---|---|
| `CMFChileSettings` (record) | Stored model: `directoryTokenEndpoint`, `softwareStatementEndpoint`, `clientId`, `clientSecret`, `clientJwks` (JSON, including private members), `positiveCertificates`, `negativeCertificates`, `updatedAt`, `updatedBy`. |
| `CertificateEntry` (record) | `id` (server-assigned), `label`, `certificateChainPem`, `privateKeyPem`. |
| `ServerSettingsRepository` | The only class that accesses MongoDB collection `SERVER_SETTINGS`, one document per section; this section's `_id` is `cmf-chile`. |
| `CMFChileSettingsValidator` | Pure validation of a merged `CMFChileSettings`; returns a list of `{field, message}`. |
| `SettingsSecretMerge` | Pure merge of an update request onto the stored settings, applying the write-only rules below. |
| `CMFChileSettingsView` (record) | Redacted shape returned to the browser. |
| `ServerSettingsService` | Load, merge, validate, save, audit, and keep the parsed settings cached. Also exposes the module-facing read accessor, `Optional<CMFChileDirectorySettings> getCMFChileDirectorySettings()` (see "Module-facing model"). |
| `CMFChileDirectorySettings` (record) | Parsed, module-facing form of the whole section: `directoryTokenEndpoint`, `softwareStatementEndpoint`, `clientId`, `clientSecret`, `clientJwks` (Nimbus `JWKSet`), `positiveCertificates`, `negativeCertificates`. |
| `ClientCertificate` (record) | `label`, `certificateChain` (`List<X509Certificate>`, leaf first), `privateKey` (`java.security.PrivateKey`), with a `leaf()` convenience. |
| `CMFChileSettingsParser` | Pure conversion of a stored `CMFChileSettings` into a `CMFChileDirectorySettings`. The PEM and JWKS parsing is shared with `CMFChileSettingsValidator`. |
| `ServerSettingsReader` | Read-only interface handed to test modules; `ServerSettingsService` implements it. |
| `ServerSettingsApi` | REST controller for `/api/admin/settings/cmf-chile`. Every handler returns 403 unless `authenticationFacade.isAdmin()`. It is `@Hidden` from the OpenAPI document, because an API token can never be admin, so the documented token-based access could not reach it. |

Security configuration, following the statistics page:

- `WebSecurityResourceServerConfig.getApiMatcher()` gains `/api/admin/settings/**`. It is not
  added to `PUBLIC_GET_PATHS`.
- `WebSecurityOidcLoginConfig` gates `/settings.html` with `hasAuthority(ROLE_ADMIN)`, next to
  `/statistics.html`.
- The `cts-navbar` link list gains `{ page: "settings", label: "Settings", href: "settings.html",
  adminOnly: true }`.

## API

### `GET /api/admin/settings/cmf-chile`

Returns 200 with the redacted view, or 403 for a non-admin. If nothing has been saved yet, it
returns a view with every field unset, not 404.

```json
{
  "directoryTokenEndpoint": "https://…/token",
  "softwareStatementEndpoint": "https://…/software-statement",
  "clientId": "abc",
  "clientSecretSet": true,
  "clientJwks": { "set": true, "keys": [ { "kid": "k1", "kty": "RSA", "alg": "PS256", "use": "sig" } ] },
  "positiveCertificates": [
    { "id": "…", "label": "primary", "certificateChainPem": "-----BEGIN CERTIFICATE-----…",
      "subject": "CN=…", "issuer": "CN=…", "notAfter": "2027-…", "privateKeySet": true }
  ],
  "negativeCertificates": [],
  "updatedAt": "…",
  "updatedBy": "…"
}
```

The view **never** contains the client secret, private JWK members (`d`, `p`, `q`, `dp`, `dq`,
`qi`, `k`, `oth`) or private-key PEMs. Certificates are public and are returned so they can be
displayed and edited.

### `PUT /api/admin/settings/cmf-chile`

Replaces the whole section. The body has the view's shape, with these write-only inputs:

- `clientSecret`: absent or empty keeps the stored value; `"clearClientSecret": true` removes it.
- `clientJwks`: when present, a full JWKS with private members that replaces the stored one. When
  absent, the stored JWKS is kept; `"clearClientJwks": true` removes it.
- Certificate entries: `privateKeyPem` may be omitted only when the entry's `id` matches a stored
  entry in the same list, in which case the stored key is kept. An entry without an `id` is new:
  the server assigns an `id`, and `privateKeyPem` is required. A stored entry whose `id` is not in
  the request is deleted.

Returns 200 with the new redacted view; 400 with `{ "errors": [ { "field", "message" } ] }`, in
which case nothing is stored; or 403 for a non-admin.

### Validation

Validation runs on the merged result. Messages use the UI labels, as AGENTS.md requires for
configuration errors.

- Directory token endpoint URL and Software statement endpoint URL: when set, each an absolute
  `https://` URL.
- Client JWKS: when set, parses with Nimbus, and every key has a private part and a `kid`.
- Each certificate entry:
  - its label is non-empty and unique within its list;
  - the certificate chain PEM parses to at least one certificate;
  - the private key PEM parses (PKCS#8 or PKCS#1);
  - the private key matches the public key of the leaf (first) certificate.
- Expired or not-yet-valid certificates are accepted, because a negative case may be one on
  purpose. The view reports `notAfter`.
- Every field is optional on its own, so a partially configured section can be saved. The
  consuming module decides whether the settings are complete enough.

### Audit

Each successful save logs one INFO line to the application log with the admin's identity, the
section and the names of the changed fields. Values are never logged.

## Page

`settings.html`, built like `statistics.html`:

- `cts-navbar current-page="settings"`;
- `cts-page-head` with title "Server settings" and sub "Suite-wide configuration. Admin only.";
- one new component, `cts-settings-page`.

The page makes no `/api/currentuser` check: the API's 403 is authoritative, and the component
renders an "admin only" `cts-alert` from it.

`cts-settings-page` shows one card-styled `<section>`, "Chile (CMF) — Directorio". It is not a
`cts-card`, which captures its children once and so cannot host a re-rendering form. Fields are
native labelled inputs with an inline error line below each, rather than `cts-form-field`, which
is schema-driven for the test configuration form.

| UI label | Control |
|---|---|
| Directory token endpoint URL | text input |
| Software statement endpoint URL | text input |
| Directory client ID | text input |
| Directory client secret | password input. The placeholder says "set — leave blank to keep" or "not set". A "Clear" checkbox is next to it. |
| Client JWKS | A read-only list of the stored keys' `kid`/`kty`/`alg`. "Replace JWKS" reveals an empty `<cts-json-editor>`. A "Clear" checkbox removes it. The stored JWKS is never shown. |
| Positive DCR client certificates | A list of entries (see below), with "Add certificate". |
| Negative DCR client certificates | The same list. The label hint suggests naming the defect, e.g. "expired", "untrusted CA". |

Each certificate entry shows:

- its label;
- subject, issuer and expiry, with a warning `cts-badge` when the certificate has expired;
- the certificate chain PEM in an editable textarea;
- the private key as "set", with a "Replace key" link that reveals an empty textarea (new
  entries show the textarea directly);
- a "Remove" button.

An action row below the section holds "Save" and "Discard changes".

- On a 400, each error is shown on its field, and a summary block with `role="alert"` lists them
  all.
- On success, a toast is shown and the component reloads the redacted view, so typed secrets do
  not stay in the DOM.

## Module-facing model

Test modules retrieve the whole section in one call,
`ServerSettingsService.getCMFChileDirectorySettings()`, already parsed:

- certificate chains as `java.security.cert.X509Certificate`, leaf first;
- private keys as `java.security.PrivateKey`;
- the client JWKS as a Nimbus `JWKSet`;
- the two endpoints, the client ID and the client secret as strings.

The accessor returns `Optional.empty()` when the section has never been saved. Fields that are
unset in a saved section are `null` (strings, JWKS) or empty lists (certificates).

The parsed settings are cached in memory, so a call does no MongoDB read and no parsing:

- `ServerSettingsService` holds the parsed `CMFChileDirectorySettings` in an `AtomicReference`.
- The first call loads the document and parses it, which happens once after startup.
- A successful save puts the settings it just validated and parsed into the cache, so the next
  test to start sees them. A test already running keeps the settings it already obtained.
- `CMFChileDirectorySettings`, `ClientCertificate` and their lists are immutable, and the same
  instance is shared across concurrent tests.
- The cache assumes a single suite instance per MongoDB database, as running test modules already
  do. A change made directly in MongoDB is only seen after a restart.

Stored data always passed validation when it was saved, so a parse failure on first load means
the stored document is corrupt. The accessor then throws `IllegalStateException` naming the
field, caches nothing and never returns partially parsed data.

The accessor is declared on a read-only interface, `ServerSettingsReader`, which
`ServerSettingsService` implements. `TestModule` gains `setServerSettingsReader(ServerSettingsReader)`,
called by `TestRunner.createTestModule` next to `setTestStatusWaiterService`, and
`AbstractTestModule` exposes it to subclasses as `getServerSettingsReader()`.

## Module consumption contract

Every module that consumes the settings must follow these rules:

- Modules only see `ServerSettingsReader`. They never receive the save path.
- The client secret, JWKS private members and certificate private keys never enter the
  `Environment` and are never passed to `log(...)` or `args(...)`. Directory token calls and mTLS
  with operator certificates run in a server-side helper, and only non-secret facts are logged:
  endpoint, HTTP status, certificate label and subject, SSA claims.
- A missing required setting is a FAILURE, not a skip. Its message tells the tester to ask the
  suite operator to configure "Chile (CMF) — Directorio" in Server settings.

## Testing

- Unit tests:
  - `CMFChileSettingsValidator_UnitTest`:
    - non-HTTPS URL;
    - JWKS whose keys lack private parts or a `kid`;
    - key not matching the certificate;
    - duplicate and empty labels;
    - expired certificate accepted;
    - PKCS#1 and PKCS#8 keys.
  - `SettingsSecretMerge_UnitTest`:
    - blank secret keeps the stored one;
    - the clear flag removes it;
    - omitted `clientJwks` keeps the stored JWKS, and `clearClientJwks` removes it;
    - an entry with a known `id` and no key keeps the stored key;
    - a new entry without a key is rejected;
    - a stored entry missing from the request is deleted;
    - an `id` from the other list does not match.
  - `CMFChileSettingsParser_UnitTest`:
    - a full section parses to `X509Certificate` chains (leaf first), `PrivateKey`s and a
      `JWKSet`;
    - an RSA and an EC key are handled;
    - unset fields come back as `null` or empty lists;
    - a corrupt stored PEM throws `IllegalStateException` naming the field.
  - `ServerSettingsService` cache behaviour, with an in-memory `ServerSettingsRepository` fake:
    - repeated calls load and parse only once;
    - a save replaces the cached instance;
    - a failed (400) save leaves the cache untouched;
    - a corrupt document is not cached.
  - `CMFChileSettingsView_UnitTest`: serialize a fully populated view and assert that the output
    contains no client secret, no private JWK member names and no `PRIVATE KEY` PEM marker.
- Security tests, in a new section "Server settings (admin only)" of
  `scripts/run-security-tests.py`:
  - unauthenticated GET and PUT return 401, including with `?public=true`;
  - a user API token gets 403 on GET and PUT;
  - plan share-link and private-link tokens get 401/403;
  - an anonymous request for `settings.html` is sent to login.

  As with statistics, the script cannot exercise the admin 200 path, because API tokens are never
  admin.
- Frontend:
  - `cts-settings-page.stories.js`: empty, fully set, validation errors, forbidden.
  - `frontend/e2e/settings.spec.js`, with `fixtures/mock-settings.js`; the GET/PUT routes are
    registered in the spec, as `statistics.spec.js` does. It checks that:
    - the Settings link shows for an admin but not for a non-admin;
    - a 403 renders the admin-only alert;
    - a save sends only the secrets that changed;
    - a 400 error appears on the right field;
    - stored secrets never appear in the DOM.
  - `npm run test:ci`.
- `mvn test`, including Checkstyle, PMD, ArchUnit and `-Werror`.
- A manual check against the running app: an admin saves, reloads and replaces each secret type,
  and a non-admin is refused.
