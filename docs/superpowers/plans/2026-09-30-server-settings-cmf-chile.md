# Server settings page (Chile CMF Directorio) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An admin-only "Server settings" page and API that store the suite's Chile CMF Directorio
credentials and client certificates, plus a read-only, parsed, cached accessor that test modules
receive from `TestRunner`.

**Architecture:** New package `net.openid.conformance.settings`. Pure classes handle PEM/JWKS
parsing, merging write-only secrets, validation, redacted views and parsing for modules; they are
unit-tested directly. `ServerSettingsService` composes them over a `ServerSettingsRepository`
(MongoDB collection `SERVER_SETTINGS`) and caches the parsed settings in an `AtomicReference`.
`ServerSettingsApi` exposes GET/PUT `/api/admin/settings/cmf-chile` to admins only. The page is a
Lit light-DOM component, `cts-settings-page`, whose request/draft logic lives in the pure
`settings-model.js`.

**Tech Stack:** Java 21, Spring Boot, MongoDB (`MongoTemplate`), BouncyCastle (`PEMParser`),
Nimbus JOSE (`JWKSet`), Gson, JUnit 5 + AssertJ + Mockito; Lit, Vitest, Storybook, Playwright.

**Spec:** `docs/superpowers/specs/2026-09-30-server-settings-cmf-chile-design.md`

## Global Constraints

- The client secret, private JWK members and private-key PEMs never leave the server: not in API
  responses, `toString()` output or log lines.
- Stored as plaintext; `DBServerSettingsRepository` is the only class that touches MongoDB.
- Admin checks use `authenticationFacade.isAdmin()` and answer 403; `/api/admin/settings/**` is on
  the API chain's matcher and not on the public matcher; `/settings.html` requires `ROLE_ADMIN`.
- Configuration error messages name UI labels in quotes ("'Directory token endpoint URL'").
- JSON access uses `OIDFJSON` helpers (ArchUnit); Java tests compile with `-Werror`.
- Comments describe the current code, not its history.

## Review Focus

1. A record's generated `toString()` printing a secret the first time someone logs it: the stored
   and module-facing records override `toString()`, pinned by tests in Tasks 2 and 4.
2. The same entry `id` twice in one PUT: the second occurrence is a new entry and needs its own
   key (Task 3 test).
3. A save that happens before the first module read: the first read returns the saved settings,
   never a stale load (Task 5 test; `compareAndSet` in the loader).
4. PEMs pasted from a browser textarea with CRLF line endings or surrounding whitespace parse
   (Task 1 test).
5. An encrypted private-key PEM gets a clear "encrypted private keys are not supported" message
   rather than a parser stack trace (Task 1 test).

---

### Task 1: PEM parsing

**Files:** Create `settings/PemParsing.java`; test `settings/PemParsing_UnitTest.java`, helper
`settings/TestPki.java` (test sources).

**Produces:** `List<X509Certificate> parseCertificateChain(String pem)`,
`PrivateKey parsePrivateKey(String pem)` (PKCS#1 and PKCS#8, RSA and EC),
`boolean keyMatchesCertificate(PrivateKey, X509Certificate)`; all throw
`IllegalArgumentException` with a user-facing message.

- [ ] Tests: chain of two certificates in order; CRLF and surrounding whitespace; blank or
  garbage gives "no certificate found"; a key PEM in the chain is rejected; PKCS#1 RSA, PKCS#8
  RSA and EC keys; an encrypted PKCS#8 key is rejected; two keys in one PEM are rejected; the
  matching key gives true; RSA key vs other RSA cert and EC key vs RSA cert give false.
- [ ] Implement, run `mvn -q test -Dtest=PemParsing_UnitTest -Dpmd.skip -Dcheckstyle.skip`,
  commit.

### Task 2: Stored model and repository

**Files:** Create `CertificateEntry`, `CMFChileSettings` (field-name constants, `empty()`,
`withAudit`, `toDocument`/`fromDocument`, `changedFields`, redacted `toString`),
`SettingsError`, `ServerSettingsRepository`, `DBServerSettingsRepository`; test
`CMFChileSettings_UnitTest`.

- [ ] Tests: document round trip (with and without optional fields); `changedFields`; neither
  `toString()` contains the secret or key.
- [ ] Implement, run, commit.

### Task 3: Update request, merge and validation

**Files:** Create `CertificateEntryUpdate`, `CMFChileSettingsUpdate` (`fromJson`),
`SettingsSecretMerge`, `CMFChileSettingsValidator`; tests for each of the three classes.

- [ ] Update tests: all fields; absent lists become empty; wrong types throw
  `IllegalArgumentException` naming the field.
- [ ] Merge tests: blank secret keeps it; clear flag removes it; omitted JWKS keeps it; a known
  `id` without a key keeps the key; a new entry has no key; an omitted entry is dropped; an `id`
  from the other list does not match; a repeated `id` matches once.
- [ ] Validator tests: valid settings; empty settings; http and relative URLs; bad JWKS JSON;
  empty JWKS; public-only key; missing `kid`; empty and duplicate labels; bad chain; missing key;
  key mismatch; expired certificate accepted; PKCS#1 and EC keys.
- [ ] Implement, run, commit.

### Task 4: Module-facing model, view, service, reader wiring

**Files:** Create `ClientCertificate`, `CMFChileDirectorySettings`, `CMFChileSettingsParser`,
`CMFChileSettingsView`, `ServerSettingsReader`, `ServerSettingsService`; modify `TestModule`,
`AbstractTestModule` and `TestRunner`; tests for the parser, the view and the service.

**Produces:** `ServerSettingsReader.getCMFChileDirectorySettings()`;
`ServerSettingsService.getCMFChileSettings()` and
`saveCMFChileSettings(CMFChileSettingsUpdate, String updatedBy)`, returning
`SaveResult(settings, errors)`; `AbstractTestModule.getServerSettingsReader()`.

- [ ] Parser tests: full parse; unset fields; corrupt PEM gives `IllegalStateException` naming
  the field; `toString` is redacted.
- [ ] View test: no secret, no private JWK member names, no `PRIVATE KEY` marker; certificate
  metadata is present.
- [ ] Service tests (in-memory repository): empty when unsaved; loads and parses once; a save
  replaces the cache; a failed save leaves the cache alone; a corrupt document is not cached; a
  save before the first read is returned by that read; audit fields are set.
- [ ] Wire the reader into `TestModule`, `AbstractTestModule` and `TestRunner`.
- [ ] Implement, run, commit.

### Task 5: API, security configuration and security tests

**Files:** Create `ServerSettingsApi` (`@Hidden`) and `ServerSettingsApi_UnitTest`; modify
`WebSecurityResourceServerConfig.getApiMatcher`, `WebSecurityOidcLoginConfig` and
`scripts/run-security-tests.py`.

- [ ] API tests: a non-admin gets 403 on GET and PUT and the service is not called; admin GET
  returns the view; a malformed body gives 400 with errors; validation errors give 400; success
  gives 200 with the view.
- [ ] Security tests: unauthenticated GET/PUT 401, also with `?public=true`; token user 403;
  private link 403; plan/test JWT bearer 401/403; anonymous `settings.html` redirected to login.
- [ ] Run the tests (plus `ApiDocumentationConventions_UnitTest` and ArchUnit), commit.

### Task 6: Page, component, model, stories

**Files:** Create `static/settings.html`, `components/settings-model.js` (+ `.test.js`),
`components/cts-settings-page.js` (+ `.stories.js`), `frontend/e2e/fixtures/mock-settings.js`;
modify the `cts-navbar.js` links.

- [ ] Model tests: draft from the view; the request omits a blank secret, an unreplaced JWKS and
  unreplaced keys; the clear flag; a JWKS that is not a JSON object is a client-side error;
  `isExpired`.
- [ ] Component and stories: Configured, Empty, Forbidden, ValidationError.
- [ ] Run `npm run test:ci` and `npm run test-storybook -- cts-settings-page`, commit.

### Task 7: Playwright e2e

**Files:** Create `frontend/e2e/settings.spec.js`.

- [ ] Admin sees the Settings link and the stored values; non-admin gets the alert and no link;
  expired session; a save sends only changed secrets; a typed secret is cleared after the save;
  a 400 error lands on its field; add and remove a certificate entry.
- [ ] Run `npx playwright test e2e/settings.spec.js`, commit.

### Task 8: Full verification

- [ ] `mvn test`; `cd frontend && npm run test:ci`; run the app (maintain-app skill) and check
  the page with a real admin session: save, reload, replace each secret type.
