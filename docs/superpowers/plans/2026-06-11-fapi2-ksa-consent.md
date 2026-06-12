# FAPI2 KSA Consent Steps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the FAPI2 KSA profile to parity with FAPI1 KSA by wiring the existing account-access-consent flow into the FAPI2 OP and RP profile-behaviour hooks.

**Architecture:** FAPI2 dispatches profile-specific behaviour through `FAPI2ProfileBehavior` (OP / server tests) and `FAPI2ClientProfileBehavior` (RP / client tests) hook objects rather than FAPI1's inline `isKSA()` branches. We override the relevant hooks on `KsaProfileBehavior` and `KsaClientProfileBehavior`, reusing the existing KSA sequences and conditions unchanged.

**Tech Stack:** Java 17, Spring Boot, Maven, JUnit (`*_UnitTest`), conformance-suite condition/sequence framework.

---

## Testing note

These are wiring changes (hook overrides + one ported endpoint method + one config-field declaration). The reused sequences/conditions are already individually tested, and the project verifies this kind of integration through the build plus the integration-test harness happy-path runs (see CLAUDE.md "Running integration tests"). There is no meaningful isolated unit test for "a profile behaviour returns a sequence", so verification per task is **the build compiling** (`-Werror`, PMD, Checkstyle) and the final task runs the KSA happy-path integration modules for both OP and RP. This matches the repo's established practice; do not fabricate low-value unit tests.

Build command used throughout (fast compile + static checks, skipping the long test suite):

```bash
mvn -B -Dmaven.test.skip clean package
```

Run this from the repo root: `/home/dcreado/projects/openid/conformance-suite`.

---

## File Structure

- `src/main/java/net/openid/conformance/fapi2spfinal/KsaProfileBehavior.java` — OP: add `getPreAuthorizationSteps()` override.
- `src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalServerTestModule.java` — OP: expose `resource.resourceUrlAccountRequests` for the KSA variant.
- `src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalClientTest.java` — RP: add the `ksaAccountRequestEndpoint(requestId)` method (ported from FAPI1).
- `src/main/java/net/openid/conformance/fapi2spfinal/KsaClientProfileBehavior.java` — RP: add `claimsHttpMtlsPath`, `handleProfileSpecificMtlsPath`, and `validateAuthorizationRequestScope` overrides.

---

## Task 1: OP — wire KSA pre-authorization (consent) steps

**Files:**
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/KsaProfileBehavior.java`

The current class only overrides `requiresMtlsEverywhere()` and `getProfileSpecificDiscoveryChecks()`. We add `getPreAuthorizationSteps()` returning the existing `OpenBankingKSAPreAuthorizationSteps`, mirroring `OpenBankingUkProfileBehavior` (which passes `includeXFapiFinancialId = false` for FAPI2). No `getResourceConfiguration()` override is needed — the default `FAPIResourceConfiguration` already calls `SetProtectedResourceUrlToSingleResourceEndpoint`, which is what KSA requires.

- [ ] **Step 1: Add the import**

In `KsaProfileBehavior.java`, add this import to the import block:

```java
import net.openid.conformance.sequence.client.OpenBankingKSAPreAuthorizationSteps;
```

- [ ] **Step 2: Add the `getPreAuthorizationSteps()` override**

Insert this method into the `KsaProfileBehavior` class body, immediately after the `requiresMtlsEverywhere()` method:

```java
	@Override
	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return () -> new OpenBankingKSAPreAuthorizationSteps(
			module.isSecondClient(),
			false, // includeXFapiFinancialId, as for FAPI2 OpenBanking UK
			module.addClientAuthentication);
	}
```

(`Supplier`, `ConditionSequence` are already imported in this file.)

- [ ] **Step 3: Build**

Run: `mvn -B -Dmaven.test.skip clean package`
Expected: `BUILD SUCCESS`. If it fails on `module.isSecondClient()` / `module.addClientAuthentication`, confirm against `OpenBankingUkProfileBehavior.getPreAuthorizationSteps()` — it uses the identical references, so the names are correct.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/net/openid/conformance/fapi2spfinal/KsaProfileBehavior.java
git commit -m "Wire KSA pre-authorization consent steps into FAPI2 OP profile

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: OP — expose the `resource.resourceUrlAccountRequests` config field for KSA

**Files:**
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalServerTestModule.java` (the KSA `@VariantConfigurationFields` block, currently around lines 253-260)

`OpenBankingKSAPreAuthorizationSteps` calls `CallKSAAccountRequestsEndpointWithBearerToken`, which reads `env.getString("resource", "resourceUrlAccountRequests")`. The field must be declared in the KSA variant's `configurationFields` or it stays hidden in the UI. (`resource.resourceUrl` is already shown via the base `@ConfigurationFields`.)

- [ ] **Step 1: Add the field to the KSA variant config block**

Find this annotation:

```java
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "ksa", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
```

Replace it with:

```java
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "ksa", configurationFields = {
	"resource.resourceUrlAccountRequests",
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
```

- [ ] **Step 2: Build**

Run: `mvn -B -Dmaven.test.skip clean package`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalServerTestModule.java
git commit -m "Show account-requests URL field for FAPI2 KSA OP tests

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: RP — add the KSA account-requests endpoint to the client base

**Files:**
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalClientTest.java`

Port FAPI1's `ksaAccountRequestEndpoint` (suite acting as OP: receives the client's account-request POST over mTLS, generates a consent id, returns the account-request response). The KSA-specific conditions are not yet imported in this file.

- [ ] **Step 1: Add imports**

Add these three imports to the import block (they live in `condition.rs`):

```java
import net.openid.conformance.condition.rs.CreateKSAOBAccountRequestResponse;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.condition.rs.GenerateKSAAccountConsentId;
```

(`Status`, `ResponseEntity`, `HttpStatus`, `JsonObject`, `Condition.ConditionResult`, `CreateFapiInteractionIdIfNeeded`, `ClearAccessTokenFromRequest`, and the helper methods `checkMtlsCertificate`, `checkResourceEndpointRequest`, `exposeEnvString`, `headersFromJson` are already present in this file.)

- [ ] **Step 2: Add the `ksaAccountRequestEndpoint` method**

Add this method to `AbstractFAPI2SPFinalClientTest`, immediately after the existing `accountRequestsEndpoint(String requestId)` method (around line 1513). Visibility is `protected` so `KsaClientProfileBehavior` (same package) can call it and test classes can override it:

```java
	protected Object ksaAccountRequestEndpoint(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("New consent endpoint").mapKey("incoming_request", requestId));

		call(exec().mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		call(exec().unmapKey("token_endpoint_request"));

		// Requires method=POST. defined in API docs
		callAndStopOnFailure(EnsureIncomingRequestMethodIsPost.class);

		checkResourceEndpointRequest(true);

		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(GenerateKSAAccountConsentId.class);
		exposeEnvString("account_request_id");

		callAndStopOnFailure(CreateKSAOBAccountRequestResponse.class);

		JsonObject accountRequestResponse = env.getObject("account_request_response");
		JsonObject headerJson = env.getObject("account_request_response_headers");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(accountRequestResponse, headersFromJson(headerJson), HttpStatus.OK);
	}
```

Note: FAPI1 calls `CreateFapiInteractionIdIfNeeded` twice; once before and once after `GenerateKSAAccountConsentId`. The second call is redundant (it is a no-op once the id exists), so this port keeps a single call. This is a deliberate cleanup, not a behavioural change.

- [ ] **Step 3: Build**

Run: `mvn -B -Dmaven.test.skip clean package`
Expected: `BUILD SUCCESS`. If `checkResourceEndpointRequest(true)` fails to resolve, confirm the signature in this same file (it is used elsewhere with a boolean argument).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalClientTest.java
git commit -m "Add KSA account-requests endpoint to FAPI2 client test base

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: RP — wire the KSA client profile behaviour

**Files:**
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/KsaClientProfileBehavior.java`

The current class only overrides `requiresMtlsEverywhere()`. Add the three hooks: route the account-requests path over mTLS, dispatch it to the module method from Task 3, and validate the authorization-request scope using `FAPIKSAValidateConsentScope` instead of the default equality check. This mirrors `OpenBankingBrazilClientProfileBehavior`.

- [ ] **Step 1: Replace the file contents**

Replace the entire contents of `KsaClientProfileBehavior.java` with:

```java
package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.FAPIKSAValidateConsentScope;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile behavior for KSA (Saudi Arabia Open Finance) client (RP) tests.
 * Requires mTLS everywhere, handles the account-requests consent endpoint over mTLS,
 * and validates the authorization-request scope against the KSA consent scope.
 */
public class KsaClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public boolean claimsHttpMtlsPath(String path) {
		return AbstractFAPI2SPFinalClientTest.ACCOUNT_REQUESTS_PATH.equals(path);
	}

	@Override
	public Object handleProfileSpecificMtlsPath(String requestId, String path) {
		// Dispatch via the module method so test classes can override and fall through via super.
		return module.ksaAccountRequestEndpoint(requestId);
	}

	@Override
	public ConditionSequence validateAuthorizationRequestScope() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIKSAValidateConsentScope.class);
			}
		};
	}
}
```

- [ ] **Step 2: Build**

Run: `mvn -B -Dmaven.test.skip clean package`
Expected: `BUILD SUCCESS`. If `module.ksaAccountRequestEndpoint` does not resolve, confirm Task 3 was committed and the method is `protected` (same-package access).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/net/openid/conformance/fapi2spfinal/KsaClientProfileBehavior.java
git commit -m "Wire KSA consent endpoint and scope validation into FAPI2 RP profile

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: Full build with static analysis and unit tests

**Files:** none (verification only)

- [ ] **Step 1: Run the full test build**

Run: `mvn test`
Expected: `BUILD SUCCESS`, including PMD, Checkstyle, and ArchUnit. Fix any `-Werror`/PMD/Checkstyle findings inline (e.g. unused imports, import ordering) and amend the relevant commit with `git commit --fixup` + autosquash rebase per the repo Git workflow.

---

## Task 6: Integration verification — KSA happy path (OP and RP)

**Files:** none (verification only)

Prerequisites (see CLAUDE.md): MongoDB on `127.0.0.1:27017`, the Nginx HTTPS proxy, and a `../conformance-suite-private` checkout.

- [ ] **Step 1: List the FAPI plans to find the KSA happy-path module numbers**

Run: `.gitlab-ci/run-tests.sh --fapi-tests --list`
Identify the KSA OP happy-path plan/module and the KSA RP (client) happy-path plan/module from the numbered output.

- [ ] **Step 2: Run the KSA OP happy-path module**

Run: `./scripts/run-integration-tests.sh --fapi-tests --rerun <op_plan_number>`
Read the printed log path with `Read`. Expected: the test reaches the consent block "Use client_credentials grant to obtain OpenBanking KSA consent scope", creates the account-request, and completes. "expected" warnings/skips are acceptable.

- [ ] **Step 3: Run the KSA RP happy-path module**

Run: `./scripts/run-integration-tests.sh --fapi-tests --rerun <rp_plan_number>`
Read the log. Expected: the client calls the account-requests endpoint over mTLS (consent id generated) and the authorization-request consent-scope validation (`FAPIKSAValidateConsentScope`) passes, and the test completes.

- [ ] **Step 4: Final confirmation**

Confirm both happy-path runs completed without unexpected failures. If a run fails, use `superpowers:systematic-debugging` before changing code.
