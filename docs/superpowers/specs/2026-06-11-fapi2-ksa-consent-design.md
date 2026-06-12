# FAPI2 KSA Consent Steps — Design

**Date:** 2026-06-11
**Branch:** `fix_ksa2_profile`
**Status:** Approved (design)

## Problem

The KSA (Saudi Arabia Open Finance) profile exists for both FAPI2 OP tests (server
test modules) and FAPI2 RP tests (client test modules), but neither runs the
account-access-consent flow that the FAPI1 KSA profile implements. As a result the
FAPI2 KSA tests do not exercise the consent creation / consent-scope behaviour that
KSA Open Finance requires.

FAPI1 KSA implements the consent flow on both sides:

- **OP tests** (suite acts as client): `setupOpenBankingKSA()` wires
  `OpenBankingKSAPreAuthorizationSteps` (client_credentials grant → create
  account-request → extract `account_request_id` → set the authorization scope to the
  consent scope) and a KSA resource configuration that exposes
  `resource.resourceUrlAccountRequests`.
- **RP tests** (suite acts as OP): a KSA account-requests mTLS endpoint
  (`GenerateKSAAccountConsentId` → `CreateKSAOBAccountRequestResponse`) plus
  `FAPIKSAValidateConsentScope` during authorization-request validation.

FAPI2 has equivalents of all of these conditions/sequences already, but the FAPI2 KSA
profile behaviours do not wire them in.

## Goal

Bring FAPI2 KSA to behavioural parity with FAPI1 KSA by wiring the **existing**
consent sequences/conditions through FAPI2's `profileBehavior` hook abstraction. This
is an exact behavioural port that reuses existing classes — no new copies of
sequences/conditions, and no KSA-specific spec deltas beyond what FAPI2 already
forces (e.g. PAR, no `x-fapi-financial-id`).

## Non-Goals

- No changes to FAPI1 KSA behaviour.
- No new consent conditions or sequences; reuse `OpenBankingKSAPreAuthorizationSteps`,
  `GenerateKSAAccountConsentId`, `CreateKSAOBAccountRequestResponse`,
  `FAPIKSAValidateConsentScope`.
- No additional scope assertions beyond what FAPI1 KSA does (FAPI1 KSA only calls
  `FAPIKSAValidateConsentScope`; the port does **not** add an
  `EnsureScopeContainsAccounts`-style check).

## Design

FAPI2 routes profile-specific behaviour through a `profileBehavior` object
(`FAPI2ProfileBehavior` for OP, `FAPI2ClientProfileBehavior` for RP) instead of
FAPI1's inline `isKSA()` branches. The port maps onto existing hooks.

### Part 1 — OP tests (`KsaProfileBehavior extends FAPI2ProfileBehavior`)

Mirror `OpenBankingUkProfileBehavior` and FAPI1 `setupOpenBankingKSA()`.

1. **Override `getPreAuthorizationSteps()`** to return:

   ```java
   () -> new OpenBankingKSAPreAuthorizationSteps(
       module.isSecondClient(),
       false,                          // includeXFapiFinancialId, as for FAPI2 UK
       module.addClientAuthentication)
   ```

   This runs: client_credentials grant → create account-request →
   extract `account_request_id` → set the authorization scope to the consent scope.

2. **No resource-config override needed.** FAPI1 KSA used a custom
   `OpenBankingKSAResourceConfiguration` calling
   `SetProtectedResourceUrlToSingleResourceEndpoint` because FAPI1's *default* resource
   config differs. In FAPI2 the default `FAPIResourceConfiguration` (returned by
   `FAPI2ProfileBehavior.getResourceConfiguration()`) **already** calls
   `SetProtectedResourceUrlToSingleResourceEndpoint`, which is exactly what KSA needs.
   So `KsaProfileBehavior` inherits the default and adds no override here.

3. **Expose the consent-creation URL field**: add
   `"resource.resourceUrlAccountRequests"` to the
   `@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "ksa", ...)`
   block on `AbstractFAPI2SPFinalServerTestModule` (currently only mTLS fields), so
   the field appears in the UI for KSA.

### Part 2 — RP tests (`KsaClientProfileBehavior extends FAPI2ClientProfileBehavior`)

Mirror `OpenBankingBrazilClientProfileBehavior` and FAPI1 `ksaAccountRequestEndpoint`.

1. **Override `claimsHttpMtlsPath(path)`** to return `true` for
   `ACCOUNT_REQUESTS_PATH`. FAPI1 routes the KSA account-requests endpoint over mTLS
   only (the non-mTLS handler throws for KSA).

2. **Override `handleProfileSpecificMtlsPath(requestId, path)`** to delegate to a new
   `module.ksaAccountRequestEndpoint(requestId)` wrapper method added to
   `AbstractFAPI2SPFinalClientTest`, ported from FAPI1: mTLS cert check →
   `EnsureIncomingRequestMethodIsPost` → resource-endpoint request checks →
   `GenerateKSAAccountConsentId` → expose `account_request_id` →
   `CreateKSAOBAccountRequestResponse`, returning the account-request response.
   Delegating through a module wrapper (as Brazil does) lets individual test classes
   override behaviour and fall through via `super`.

3. **Override `validateAuthorizationRequestScope()`** to return a sequence that calls
   `FAPIKSAValidateConsentScope` instead of the default
   `EnsureRequestedScopeIsEqualToConfiguredScope`.

## Components Touched

- `fapi2spfinal/KsaProfileBehavior.java` — add the three OP overrides.
- `fapi2spfinal/AbstractFAPI2SPFinalServerTestModule.java` — add
  `resource.resourceUrlAccountRequests` to the KSA `@VariantConfigurationFields`.
- `fapi2spfinal/KsaClientProfileBehavior.java` — add the three RP overrides.
- `fapi2spfinal/AbstractFAPI2SPFinalClientTest.java` — add the
  `ksaAccountRequestEndpoint(requestId)` wrapper method.

## Reused (unchanged)

- `sequence/client/OpenBankingKSAPreAuthorizationSteps`
- `condition/rs/GenerateKSAAccountConsentId`
- `condition/rs/CreateKSAOBAccountRequestResponse`
- `condition/as/FAPIKSAValidateConsentScope`

## Testing / Verification

1. `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` (or `mvn test` for full checks).
2. Run a KSA happy-path module from each side using the integration harness:
   `.gitlab-ci/run-tests.sh --fapi-tests --list` to find the KSA plan numbers, then
   `./scripts/run-integration-tests.sh --fapi-tests --rerun <n>` for an OP happy-path
   and an RP happy-path module, confirming the consent step executes end-to-end (the
   pre-authorization "Use client_credentials grant to obtain OpenBanking KSA consent
   scope" block on OP, and the account-requests endpoint call + consent-scope
   validation on RP).

## Resolved During Design

- `resource.resourceUrlAccountRequests` is read directly from the `resource` config
  object by `CallKSAAccountRequestsEndpointWithBearerToken` (inside
  `OpenBankingKSAPreAuthorizationSteps`). Exposing the config field is all that is
  required for the OP side. `resource.resourceUrl` is already shown for KSA via the
  base `@ConfigurationFields`.
- FAPI2's default `FAPIResourceConfiguration` already calls
  `SetProtectedResourceUrlToSingleResourceEndpoint`, matching FAPI1 KSA, so no custom
  resource-config class is needed.
- RP-endpoint helpers (`EnsureIncomingRequestMethodIsPost`,
  `GenerateKSAAccountConsentId`, `CreateKSAOBAccountRequestResponse` in
  `condition.rs`; `FAPIKSAValidateConsentScope` in `condition.as`) all exist, and the
  FAPI2 client base already provides `checkMtlsCertificate`,
  `checkResourceEndpointRequest`, `exposeEnvString`, `headersFromJson`,
  `ClearAccessTokenFromRequest`, and `CreateFapiInteractionIdIfNeeded`.
