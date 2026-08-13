# FAPI2 KSA JWT Consent Format — Design

**Date:** 2026-06-15
**Branch:** `fix_ksa2_profile`
**Status:** Approved (design)

## Problem

The FAPI2 KSA (Saudi Arabia Open Finance) account-access-consent flow currently
exchanges the consent as **plain JSON** on both sides:

- **OP tests** (suite acts as client): `CreateKSACreateAccountRequestRequest` builds
  `{"Data":{"Permissions":["ReadAccountsBasic"]}}` and
  `CallKSAAccountRequestsEndpointWithBearerToken` POSTs it as `application/json`,
  parsing the response as JSON.
- **RP tests** (suite acts as bank/OP): `ksaAccountRequestEndpoint` accepts the
  request and `CreateKSAOBAccountRequestResponse` returns plain JSON.

The KSA Account Information Services API
(`library/profiles/ksa-2024.09.01-final-errata1/KSA.AccountInformationServices.yaml`)
defines the consent request and response as a **signed JWT** (`application/jwt`),
schema `OBReadConsentSigned`: a `Jwt` (`iss`, `exp`, `nbf`, optional `aud`/`iat`)
wrapping a `message` object that holds the consent body. The conformance suite does
not currently exercise this signed-JWT format, so KSA FAPI2 tests neither send nor
require the JWT consent the spec mandates.

## Goal

Replace the plain-JSON KSA FAPI2 consent flow with the **signed-JWT** format defined
by the KSA spec, on both the OP and RP sides. KSA FAPI2 always uses the signed-JWT
format (no new selectable variant) — "it is required to be JWT".

The closest existing precedent is Brazil's payment-consent signing:
`FAPIBrazilSignPaymentConsentRequest extends AbstractSignJWT`, wired through
`OpenBankingBrazilPreAuthorizationSteps`.

## Non-Goals

- No changes to FAPI1 KSA behaviour, nor to any non-KSA profile.
- No new variant parameter — the JWT format fully replaces the plain-JSON format for
  KSA FAPI2.
- No change to the KSA consent-scope authorization check
  (`FAPIKSAValidateConsentScope`) or the `accounts:<consentId>` scope shape.

## Key Decisions (confirmed)

1. **Replace, JWT-only.** KSA FAPI2 always uses the signed-JWT consent on both sides.
2. **RP side: require request *and* sign response.** The suite (acting as the bank)
   requires the incoming consent request to be `application/jwt` with a signature it
   verifies against the client's registered jwks, and returns a signed JWT response
   (`OBReadConsentResponseSigned`, `application/jwt`).
3. **Signing key / alg:** sign with the test's client `jwks`, algorithm **PS256**
   (the KSA discovery check already requires PS256 request-object signing). On the OP
   request, `iss` = the **client_id**.
4. **Time claims are dynamic.** `iat`/`nbf` = now, `exp` = now + window; the sample's
   hardcoded epoch values are not reused.
5. **OP request payload** uses the full sample `message` from the YAML
   `ConsentRequestSigned` example (all `Data`/`Risk`/`Subscription` content),
   verbatim except `iss` (and `ExpirationDateTime` on the second-client path).

## Design

### Part 1 — OP tests (`OpenBankingKSAPreAuthorizationSteps`, suite = client)

Replace the plain-JSON build + JSON POST with build → sign → POST-as-JWT → parse-JWT.

1. **`CreateKSAConsentRequest`** (new condition, `condition/client`) — replaces
   `CreateKSACreateAccountRequestRequest`. Builds the JWT claims object into
   `account_requests_endpoint_request`:
   - `iss` = `client_id`
   - `aud` = the account-requests endpoint URL; `iat`/`nbf` = now; `exp` = now + window
   - `message` = the sample KSA consent payload (`Data`/`Risk`/`Subscription`) loaded
     from a resource file `src/main/resources/json/ksa/account-access-consent-request.json`
     (extracted verbatim from the YAML `ConsentRequestSigned` example) rather than
     hand-built as a large `JsonObject`.
   - Second-client path overrides `message.Data.ExpirationDateTime` (replacing
     `CreateKSACreateAccountRequestRequestWithExpiration`).

2. **`SignKSAConsentRequest`** (new condition extending `AbstractSignJWT`,
   `condition/client`) — signs `account_requests_endpoint_request` with the client
   `jwks`, PS256, into `account_requests_endpoint_request_signed`. Mirrors
   `FAPIBrazilSignPaymentConsentRequest`.

3. **`CallKSAAccountRequestsEndpointWithBearerToken`** (modified) —
   - `getBody` returns the signed JWS string.
   - `getContentType` returns `application/jwt`.
   - `handleClientResponse` parses the bank's `application/jwt` response, extracts the
     JWT claims, and stores the inner `message` object as
     `account_requests_endpoint_response`, so the existing
     `ExtractAccountRequestIdFromKSAAccountRequestsEndpointResponse` (reads
     `Data.ConsentId`) keeps working unchanged.

Downstream (`ExtractAccountRequestIdFromKSAAccountRequestsEndpointResponse`,
`FAPIKSASetClientScopeToAccountsConsentIdOpenId`) is unchanged.

### Part 2 — RP tests (`AbstractFAPI2SPFinalClientTest.ksaAccountRequestEndpoint`, suite = bank)

1. **Require the request to be a signed JWT:**
   - `EnsureIncomingRequestContentTypeIsApplicationJwt` (exists in `condition/rs`).
   - New condition to parse the incoming JWS, **verify the signature against the
     client's registered jwks**, and extract the inner `message` for processing.
2. Keep `GenerateKSAAccountConsentId` and `CreateKSAOBAccountRequestResponse`.
3. **Sign the response:** new condition that wraps the response body as a JWT
   `message` and signs it with the **test suite's server signing key** (`server_jwks`),
   PS256. Return it with `Content-Type: application/jwt`, matching
   `OBReadConsentResponseSigned`.

## Components Touched

New:
- `condition/client/CreateKSAConsentRequest.java`
- `condition/client/SignKSAConsentRequest.java`
- `condition/rs/` — incoming-consent JWT parse/verify condition
- `condition/rs/` — response signing condition
- `src/main/resources/json/ksa/account-access-consent-request.json` (sample `message`)

Modified:
- `sequence/client/OpenBankingKSAPreAuthorizationSteps.java` — swap in build+sign,
  drop the plain-JSON conditions.
- `condition/client/CallKSAAccountRequestsEndpointWithBearerToken.java` — JWT body,
  `application/jwt` content type, JWT response parsing.
- `fapi2spfinal/AbstractFAPI2SPFinalClientTest.java` — `ksaAccountRequestEndpoint`
  require-JWT + sign-response wiring.

Reused (unchanged):
- `AbstractSignJWT`, `ExtractAccountRequestIdFromKSAAccountRequestsEndpointResponse`,
  `FAPIKSASetClientScopeToAccountsConsentIdOpenId`, `GenerateKSAAccountConsentId`,
  `CreateKSAOBAccountRequestResponse`, `FAPIKSAValidateConsentScope`,
  `EnsureIncomingRequestContentTypeIsApplicationJwt`.

Likely removable after the swap (verify no other caller): `CreateKSACreateAccountRequestRequest`,
`CreateKSACreateAccountRequestRequestWithExpiration`.

## Testing / Verification

1. `mvn -B -Dmaven.test.skip -Dpmd.skip clean package`, then `mvn test` for full
   checks (PMD, checkstyle, ArchUnit, unit tests). Add `*_UnitTest` coverage for the
   new conditions (claims shape incl. `iss=client_id`, signing produces a verifiable
   PS256 JWS, RP-side rejects non-JWT / bad-signature requests).
2. Integration: find a KSA happy-path plan via `.gitlab-ci/run-tests.sh --fapi-tests
   --list`, then `./scripts/run-integration-tests.sh --fapi-tests --rerun <n>` for an
   OP happy-path and an RP happy-path, confirming the consent is exchanged as
   `application/jwt` end-to-end.

## Open Questions / Risks

- **RP-side signature verification** depends on the client under test having a
  registered signing jwks; confirm the existing client config exposes one for KSA RP
  tests (the same key used for request-object / private_key_jwt). If only an mTLS
  client is configured without a signing jwks, signature verification needs a fallback
  decision.
- **OP-side response parsing** assumes the bank returns `application/jwt` per the
  spec; if a bank under test returns JSON, the parse should fail clearly (the spec
  requires JWT, so a hard failure is correct test behaviour).
