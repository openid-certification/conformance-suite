# KSA FAPI2 Message Signing Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `ksa` sub-profile to the FAPI2 Message Signing Final test plans (OP and RP) that enforces MTLS everywhere, private key JWT auth, JAR, MTLS sender constraining, no JARM, no RAR, and OpenID Connect.

**Architecture:** Follows the CBUAE precedent — two new profile behavior classes (`KsaProfileBehavior`, `KsaClientProfileBehavior`), a new `KSA` enum value, `@VariantSetup` wiring in three abstract modules, `certificationProfileName()` cases in four test plans, and `@VariantNotApplicable` updates in six profile-specific test modules.

**Tech Stack:** Java 17, Spring Boot, JUnit 5 (tests named `*_UnitTest.java`), Maven (`mvn test -Dpmd.skip -Dcheckstyle.skip` for fast feedback)

---

### Task 1: Add KSA enum value and profile behavior classes

**Files:**
- Modify: `src/main/java/net/openid/conformance/variant/FAPI2FinalOPProfile.java`
- Create: `src/main/java/net/openid/conformance/fapi2spfinal/KsaProfileBehavior.java`
- Create: `src/main/java/net/openid/conformance/fapi2spfinal/KsaClientProfileBehavior.java`

- [ ] **Step 1: Add `KSA` to the enum**

In `FAPI2FinalOPProfile.java`, add after the `CBUAE` value (line 20):

```java
	// https://openfinance.sa/ (Saudi Arabia)
	KSA,
```

The enum section should look like:
```java
	// https://openfinanceuae.atlassian.net/wiki/spaces/StandardsDraft01/pages/39158001/Security+Profile+-+FAPI
	CBUAE,
	// https://openfinance.sa/ (Saudi Arabia)
	KSA,
	// PLAIN_FAPI utilising client credentials grant only.
	FAPI_CLIENT_CREDENTIALS_GRANT,
```

- [ ] **Step 2: Create `KsaProfileBehavior.java`**

```java
package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationRequestTypesSupportedContainsTestType;
import net.openid.conformance.condition.client.CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

/**
 * Profile behavior for KSA (Saudi Arabia Open Finance).
 * Requires mTLS everywhere. No RAR (unlike CBUAE).
 */
public class KsaProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return DiscoveryEndpointChecks::new;
	}

	public static class DiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointAuthorizationRequestTypesSupportedContainsTestType.class, ConditionResult.WARNING);
		}
	}
}
```

- [ ] **Step 3: Create `KsaClientProfileBehavior.java`**

```java
package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for KSA client tests.
 * Requires mTLS everywhere and treats userinfo as the resource endpoint.
 */
public class KsaClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public boolean userInfoIsResourceEndpoint() {
		return true;
	}
}
```

- [ ] **Step 4: Compile to catch typos**

```bash
mvn -B -Dmaven.test.skip -Dpmd.skip clean package 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/openid/conformance/variant/FAPI2FinalOPProfile.java \
        src/main/java/net/openid/conformance/fapi2spfinal/KsaProfileBehavior.java \
        src/main/java/net/openid/conformance/fapi2spfinal/KsaClientProfileBehavior.java
git commit -m "feat: add KSA profile enum value and behavior classes"
```

---

### Task 2: Wire @VariantSetup in abstract server and client modules

**Files:**
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalServerTestModule.java` (around line 1173)
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalClientTest.java` (around line 1658)
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalDiscoveryEndpointVerification.java` (around line 83)

- [ ] **Step 1: Add @VariantSetup in AbstractFAPI2SPFinalServerTestModule**

Find the block (around line 1173):
```java
	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "cbuae")
	public void setupCbuaeFapi() {
		initProfileBehavior(new CbuaeProfileBehavior());
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "vci")
```

Insert after the `setupCbuaeFapi()` method and before the `vci` setup:
```java
	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "ksa")
	public void setupKsa() {
		initProfileBehavior(new KsaProfileBehavior());
	}

```

- [ ] **Step 2: Add @VariantSetup in AbstractFAPI2SPFinalClientTest**

Find the block (around line 1658):
```java
	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "cbuae")
	public void setupCbuae() {
		initProfileBehavior(new CbuaeClientProfileBehavior());
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "vci")
```

Insert after `setupCbuae()` and before the `vci` setup:
```java
	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "ksa")
	public void setupKsa() {
		initProfileBehavior(new KsaClientProfileBehavior());
	}

```

- [ ] **Step 3: Add @VariantSetup in FAPI2SPFinalDiscoveryEndpointVerification**

Find the block (around line 83):
```java
	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "cbuae")
	public void setupCBUAE() {
		profileBehavior = new CbuaeProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "vci")
```

Insert after `setupCBUAE()` and before the `vci` setup:
```java
	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "ksa")
	public void setupKsa() {
		profileBehavior = new KsaProfileBehavior();
	}

```

- [ ] **Step 4: Compile**

```bash
mvn -B -Dmaven.test.skip -Dpmd.skip clean package 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalServerTestModule.java \
        src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalClientTest.java \
        src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalDiscoveryEndpointVerification.java
git commit -m "feat: wire KSA profile @VariantSetup in abstract modules and discovery"
```

---

### Task 3: Add ksa case in FAPI2 Message Signing test plans

**Files:**
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2MessageSigningFinalTestPlan.java`
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2MessageSigningFinalClientTestPlan.java`

- [ ] **Step 1: Add ksa case in FAPI2MessageSigningFinalTestPlan (OP plan)**

In `FAPI2MessageSigningFinalTestPlan.java`, find the `certificationProfileName()` switch. After the `cbuae` case and before `default`, insert:

```java
			case "ksa":
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!signedRequest) {
					throw new RuntimeException("Invalid configuration for %s: Only signed requests are supported for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (rar) {
					throw new RuntimeException("Invalid configuration for %s: RAR is not supported for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!mtlsBounded) {
					throw new RuntimeException("Invalid configuration for %s: Only MTLS sender constraining is supported for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM responses are not used for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!openid) {
					throw new RuntimeException("Invalid configuration for %s: OpenID must be selected for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				return List.of("FAPI2MS OP KSA");
```

The `cbuae` case ends at (currently) `return List.of("FAPI2MS OP CBUAE");` — insert the `ksa` case immediately after that line and before `default:`.

- [ ] **Step 2: Add ksa case in FAPI2MessageSigningFinalClientTestPlan (RP plan)**

Same insertion in `FAPI2MessageSigningFinalClientTestPlan.java`, after the `cbuae` case (`return List.of( "FAPI2MS RP CBUAE");`) and before `default:`:

```java
			case "ksa":
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!signedRequest) {
					throw new RuntimeException("Invalid configuration for %s: Only signed requests are supported for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (rar) {
					throw new RuntimeException("Invalid configuration for %s: RAR is not supported for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!mtlsBounded) {
					throw new RuntimeException("Invalid configuration for %s: Only MTLS sender constraining is supported for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM responses are not used for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!openid) {
					throw new RuntimeException("Invalid configuration for %s: OpenID must be selected for KSA".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				return List.of("FAPI2MS RP KSA");
```

- [ ] **Step 3: Compile**

```bash
mvn -B -Dmaven.test.skip -Dpmd.skip clean package 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/net/openid/conformance/fapi2spfinal/FAPI2MessageSigningFinalTestPlan.java \
        src/main/java/net/openid/conformance/fapi2spfinal/FAPI2MessageSigningFinalClientTestPlan.java
git commit -m "feat: add KSA certification profile names in message signing plans"
```

---

### Task 4: Redirect ksa away from SP Final (non-message-signing) plans

**Files:**
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalTestPlan.java`
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalClientTestPlan.java`

- [ ] **Step 1: Add ksa case in FAPI2SPFinalTestPlan**

In `FAPI2SPFinalTestPlan.java`, find the `certificationProfileName()` switch. After the `cbuae` case:
```java
			case "cbuae":
				throw new RuntimeException("CBUAE profile requires the usage of JAR, please use the message signing test plan.");
```

Insert before `default:`:
```java
			case "ksa":
				throw new RuntimeException("KSA profile requires the usage of JAR, please use the message signing test plan.");
```

- [ ] **Step 2: Add ksa case in FAPI2SPFinalClientTestPlan**

In `FAPI2SPFinalClientTestPlan.java`, after:
```java
			case "cbuae":
				throw new RuntimeException("CBUAE profile requires the usage of JAR, please use the message signing test plan.");
```

Insert before `default:`:
```java
			case "ksa":
				throw new RuntimeException("KSA profile requires the usage of JAR, please use the message signing test plan.");
```

- [ ] **Step 3: Compile**

```bash
mvn -B -Dmaven.test.skip -Dpmd.skip clean package 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalTestPlan.java \
        src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalClientTestPlan.java
git commit -m "feat: redirect KSA profile to message signing plan from SP Final plans"
```

---

### Task 5: Update @VariantNotApplicable in profile-specific test modules

**Files:**
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails.java`
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalAustraliaConnectIdEnsureInvalidPurposeFails.java`
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalAustraliaConnectIdRequestObjectWithExpOver10Fails.java`
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalAustraliaConnectIdEnsureRequestObjectWithNbfOver15Fails.java`
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalEnsureServerAcceptsRequestObjectWithMultipleAud.java`
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalPlainFAPIEnsureRegisteredRedirectUri.java`

- [ ] **Step 1: FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails**

Current annotation:
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {
	"plain_fapi",
	"openbanking_uk",
	"consumerdataright_au",
	"connectid_au",
	"cbuae",
	"fapi_client_credentials_grant"
})
```

Change to:
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {
	"plain_fapi",
	"openbanking_uk",
	"consumerdataright_au",
	"connectid_au",
	"cbuae",
	"ksa",
	"fapi_client_credentials_grant"
})
```

- [ ] **Step 2: FAPI2SPFinalAustraliaConnectIdEnsureInvalidPurposeFails**

Current annotation (single line):
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "cbuae", "fapi_client_credentials_grant" })
```

Change to:
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "cbuae", "ksa", "fapi_client_credentials_grant" })
```

- [ ] **Step 3: FAPI2SPFinalAustraliaConnectIdRequestObjectWithExpOver10Fails**

Current annotation (the profile one, leave the auth method one unchanged):
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "cbuae", "fapi_client_credentials_grant" })
```

Change to:
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "cbuae", "ksa", "fapi_client_credentials_grant" })
```

- [ ] **Step 4: FAPI2SPFinalAustraliaConnectIdEnsureRequestObjectWithNbfOver15Fails**

Current annotation (the profile one):
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "cbuae", "fapi_client_credentials_grant" })
```

Change to:
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "cbuae", "ksa", "fapi_client_credentials_grant" })
```

- [ ] **Step 5: FAPI2SPFinalEnsureServerAcceptsRequestObjectWithMultipleAud**

Current annotation:
```java
@VariantNotApplicable(
	parameter = FAPI2FinalOPProfile.class,
	values = { "cbuae", "fapi_client_credentials_grant" }
)
```

Change to:
```java
@VariantNotApplicable(
	parameter = FAPI2FinalOPProfile.class,
	values = { "cbuae", "ksa", "fapi_client_credentials_grant" }
)
```

- [ ] **Step 6: FAPI2SPFinalPlainFAPIEnsureRegisteredRedirectUri**

Current annotation:
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "fapi_client_credentials_grant" })
```

Change to:
```java
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "ksa", "fapi_client_credentials_grant" })
```

- [ ] **Step 7: Compile and run tests**

```bash
mvn test -Dpmd.skip -Dcheckstyle.skip 2>&1 | tail -30
```

Expected: `BUILD SUCCESS` — all tests pass including ArchUnit.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails.java \
        src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalAustraliaConnectIdEnsureInvalidPurposeFails.java \
        src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalAustraliaConnectIdRequestObjectWithExpOver10Fails.java \
        src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalAustraliaConnectIdEnsureRequestObjectWithNbfOver15Fails.java \
        src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalEnsureServerAcceptsRequestObjectWithMultipleAud.java \
        src/main/java/net/openid/conformance/fapi2spfinal/FAPI2SPFinalPlainFAPIEnsureRegisteredRedirectUri.java
git commit -m "feat: exclude KSA profile from non-KSA-specific test modules"
```

---

### Task 6: Final verification

- [ ] **Step 1: Run full test suite**

```bash
mvn test 2>&1 | tail -40
```

Expected: `BUILD SUCCESS` — all tests pass (PMD, checkstyle, ArchUnit, unit tests).

- [ ] **Step 2: Verify snapshot test if it updated**

If `ConfigurationFieldsSnapshot_UnitTest` fails with a diff, run:

```bash
mvn test -Dtest=ConfigurationFieldsSnapshot_UnitTest -Dpmd.skip -Dcheckstyle.skip 2>&1 | grep -A 30 "FAILURE\|expected\|but was"
```

If the snapshot needs updating, the test output will show the expected new snapshot. Update the snapshot file at the path shown in the error and rerun. The snapshot captures configuration fields per profile — KSA adds no new fields so typically no update is needed.
