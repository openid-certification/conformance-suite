# KSA Profile for FAPI2 Message Signing

## Overview

Add a `ksa` sub-profile to the FAPI2 Message Signing Final test plans (OP and RP). The profile targets Saudi Arabia deployments and enforces a fixed, narrow set of constraints on top of plain FAPI2.

## Constraints

| Parameter | Required value |
|---|---|
| `client_auth_type` | `private_key_jwt` |
| `fapi_request_method` | `signed_non_repudiation` (JAR) |
| `sender_constrain` | `mtls` |
| `fapi_response_mode` | `plain_response` (no JARM) |
| `openid` | `openid_connect` |
| `authorization_request_type` | `plain` (no RAR) |
| MTLS on all endpoints | yes |

Any other combination is invalid and should raise a `RuntimeException` in `certificationProfileName()`.

## Architecture

Follows the CBUAE precedent exactly — CBUAE is the closest existing profile (MTLS everywhere, private key JWT, JAR, MTLS sender constraining, OpenID required). KSA differs only in that RAR is **not** required (and not permitted), which removes the `RARSupport.ExtractRARFromConfig` discovery check.

## Files Changed

### New files

| File | Purpose |
|---|---|
| `fapi2spfinal/KsaProfileBehavior.java` | Server-side profile behavior: `requiresMtlsEverywhere()=true`; discovery checks for authorization_code and PS256 (no RAR) |
| `fapi2spfinal/KsaClientProfileBehavior.java` | Client-side profile behavior: `requiresMtlsEverywhere()=true`, `userInfoIsResourceEndpoint()=true` |

### Modified files

| File | Change |
|---|---|
| `variant/FAPI2FinalOPProfile.java` | Add `KSA` enum value |
| `fapi2spfinal/AbstractFAPI2SPFinalServerTestModule.java` | Add `@VariantSetup(parameter=FAPI2FinalOPProfile.class, value="ksa")` wiring `KsaProfileBehavior` |
| `fapi2spfinal/AbstractFAPI2SPFinalClientTest.java` | Add `@VariantSetup` wiring `KsaClientProfileBehavior` |
| `fapi2spfinal/FAPI2SPFinalDiscoveryEndpointVerification.java` | Add `@VariantSetup("ksa")` pointing to `KsaProfileBehavior` |
| `fapi2spfinal/FAPI2MessageSigningFinalTestPlan.java` | Add `ksa` case → returns `List.of("FAPI2MS OP KSA")`; validates constraints |
| `fapi2spfinal/FAPI2MessageSigningFinalClientTestPlan.java` | Add `ksa` case → returns `List.of("FAPI2MS RP KSA")`; validates constraints |
| `fapi2spfinal/FAPI2SPFinalTestPlan.java` | Add `ksa` case → throws "KSA profile requires JAR, please use the message signing test plan" |
| `fapi2spfinal/FAPI2SPFinalClientTestPlan.java` | Same as above |
| Profile-specific `@VariantNotApplicable` modules | Add `"ksa"` alongside `"cbuae"` in Brazil-specific, UK-specific, ConnectID-specific tests, and `EnsureServerAcceptsRequestObjectWithMultipleAud` |

### @VariantNotApplicable updates

Add `"ksa"` to the `values` array in the following modules (alongside `"cbuae"` already present):

- `FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails`
- `FAPI2SPFinalAustraliaConnectIdEnsureInvalidPurposeFails`
- `FAPI2SPFinalAustraliaConnectIdRequestObjectWithExpOver10Fails`
- `FAPI2SPFinalAustraliaConnectIdEnsureRequestObjectWithNbfOver15Fails`
- `FAPI2SPFinalEnsureServerAcceptsRequestObjectWithMultipleAud`
- `FAPI2SPFinalPlainFAPIEnsureRegisteredRedirectUri`

## Certification Profile Name

- OP (server) test plan: `"FAPI2MS OP KSA"`
- RP (client) test plan: `"FAPI2MS RP KSA"`

## Discovery Endpoint Checks

`KsaProfileBehavior.DiscoveryEndpointChecks` runs:
1. `CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode` (FAILURE)
2. `CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256` (FAILURE)
3. `CheckDiscEndpointAuthorizationRequestTypesSupportedContainsTestType` (WARNING)

No RAR extraction (unlike CBUAE).

## Out of Scope

- FAPI2 SP ID2 (`fapi2spid2`) plans — not requested; can be added separately.
- Any KSA-specific test modules beyond the standard message signing suite.
