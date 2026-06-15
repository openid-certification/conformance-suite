# FAPI2 KSA JWT Consent Format Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the plain-JSON KSA FAPI2 account-access-consent flow with the signed-JWT (`application/jwt`) format the KSA spec mandates, on both the OP (suite-as-client) and RP (suite-as-bank) sides.

**Architecture:** On the OP side the suite builds the consent as JWT claims (`iss`=client_id, dynamic time claims, `message`=the KSA sample consent body), signs it with the client's `jwks` (PS256) via `AbstractSignJWT`, POSTs it as `application/jwt`, and parses the bank's `application/jwt` response. On the RP side the suite requires the incoming consent to be `application/jwt`, verifies its signature against the client's registered `client_public_jwks`, then signs its own response with `server_jwks` and returns it as `application/jwt`. This mirrors the existing Brazil payment-consent signing path.

**Tech Stack:** Java, Spring Boot, Nimbus JOSE+JWT, Gson, JUnit. Conformance-suite Condition/Sequence framework.

---

## File Structure

New files:
- `src/main/resources/json/ksa/account-access-consent-request.json` — the KSA sample consent `message` body (Data/Risk/Subscription), loaded by the OP request builder.
- `src/main/java/net/openid/conformance/condition/client/AbstractCreateKSAConsentRequest.java` — builds the JWT claims object wrapping the sample message; expiration hook for subclasses.
- `src/main/java/net/openid/conformance/condition/client/CreateKSAConsentRequest.java` — concrete builder (no expiration override).
- `src/main/java/net/openid/conformance/condition/client/CreateKSAConsentRequestWithExpiration.java` — concrete builder that overrides expiration/transaction dates (second-client path).
- `src/main/java/net/openid/conformance/condition/client/SignKSAConsentRequest.java` — signs the claims with the client `jwks` (PS256).
- `src/main/java/net/openid/conformance/condition/client/CallKSASignedAccountRequestsEndpointWithBearerToken.java` — JWT-sending/parsing variant of the call condition (extends the existing plain-JSON one so FAPI1 is untouched).
- `src/main/java/net/openid/conformance/condition/rs/ExtractKSASignedConsentRequest.java` — parses the incoming JWS body into `parsed_client_request_jwt` + `new_consent_request`.
- `src/main/java/net/openid/conformance/condition/rs/ValidateKSAConsentRequestSignature.java` — verifies the parsed JWS against `client_public_jwks`.
- `src/main/java/net/openid/conformance/condition/as/CreateKSASignedConsentResponseClaims.java` — wraps the account-request response in JWT claims (`iss`, time claims, `message`).
- `src/main/java/net/openid/conformance/condition/as/SignKSAConsentResponse.java` — signs the claims with `server_jwks`.
- Unit tests under `src/test/java/net/openid/conformance/condition/...` for the new pure-logic conditions.

Modified files:
- `src/main/java/net/openid/conformance/sequence/client/OpenBankingKSAPreAuthorizationSteps.java` — add a `signedConsent` constructor flag that branches to the build+sign+JWT-call path; default (FAPI1) behaviour unchanged.
- `src/main/java/net/openid/conformance/fapi2spfinal/KsaProfileBehavior.java` — pass `signedConsent = true` when constructing the sequence.
- `src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalClientTest.java` — `ksaAccountRequestEndpoint` require-JWT + verify-signature + sign-response (FAPI2-only; not shared with FAPI1).

**NOT modified (shared with FAPI1 — must stay plain JSON):**
- `CallKSAAccountRequestsEndpointWithBearerToken.java`, `CreateKSACreateAccountRequestRequest.java`, `CreateKSACreateAccountRequestRequestWithExpiration.java` — left intact for the FAPI1 KSA flow.

**Sharing constraint (verified):** `OpenBankingKSAPreAuthorizationSteps` is used by both FAPI1 (`AbstractFAPI1AdvancedFinalServerTestModule:1025`) and FAPI2 (`KsaProfileBehavior`). The OP-side change therefore branches inside the shared sequence on a constructor flag rather than mutating the existing path. The RP-side `ksaAccountRequestEndpoint` is defined separately per protocol, so the FAPI2 RP changes do not touch FAPI1.

---

## Task 1: Add the KSA sample consent message resource

**Files:**
- Create: `src/main/resources/json/ksa/account-access-consent-request.json`

- [ ] **Step 1: Create the resource file**

This is the `message` body extracted verbatim from the YAML `ConsentRequestSigned` example (`library/profiles/ksa-2024.09.01-final-errata1/KSA.AccountInformationServices.yaml`, the object under `message:`). The JWT wrapper (`iss`/`exp`/`nbf`/`aud`/`iat`) is added in code, not here.

```json
{
  "Data": {
    "Permissions": [
      "ReadAccountsBasic",
      "ReadAccountsDetail",
      "ReadBalances",
      "ReadParty",
      "ReadPartyPSU",
      "ReadPartyPSUIdentity",
      "ReadBeneficiariesBasic",
      "ReadBeneficiariesDetail",
      "ReadTransactionsBasic",
      "ReadTransactionsDetail",
      "ReadTransactionsCredits",
      "ReadTransactionsDebits",
      "ReadScheduledPaymentsBasic",
      "ReadScheduledPaymentsDetail",
      "ReadDirectDebits",
      "ReadStandingOrdersBasic",
      "ReadStandingOrdersDetail"
    ],
    "AuthorizationExpirationTimeWindow": "720:00:00",
    "ExpirationDateTime": "2023-01-28T15:27:13+0300",
    "NationalIdentificationNumber": "1111111111",
    "TransactionFromDateTime": "2023-01-25T12:19:24+0300",
    "TransactionToDateTime": "2023-01-27T12:19:24+0300",
    "AccountType": [
      "KSAOB.Retail"
    ],
    "AccountSubType": [
      "CurrentAccount"
    ],
    "FourthParty": {
      "TradingName": "Rand's Company",
      "LegalName": "Rand's Ltd",
      "IdentifierType": "123456",
      "Identifier": "123456"
    },
    "SponsoredTPP": {
      "TradingName": "Khalid Company",
      "Identification": "123456"
    },
    "ConsentPurpose": [
      "Account Aggregation",
      "E-Statement"
    ],
    "IsPSUReadAllAccounts": true
  },
  "Risk": {
    "PSUIndicators": {
      "PSUName": {
        "en": "Mohammed",
        "ar": "محمد"
      },
      "PSUPhoneNumber": "0555555555",
      "IsCustomerRegistered": true,
      "IsCustomerVerified": true,
      "FailedLoginAttempts": 2,
      "PasswordChangedDateTime": "2023-01-27T15:27:13+0300",
      "PSUOnboardingDateTime": "2023-01-27T15:27:13+0300"
    },
    "DeviceIndicators": {
      "GeoLocation": {
        "latitude": "40.730610",
        "longitude": "-73.935242"
      },
      "DeviceId": "54234",
      "VPNStatus": false,
      "RemoteAccessTool": false,
      "Screensharing": false,
      "DeviceSoftwareRestricted": true,
      "TrustedDeviceFlag": true,
      "TrustedDeviceDateAndtime": "2023-01-27T15:27:13+0300",
      "MechanismOfTrustingDevice": "KSAOB.IVR",
      "IPAddressVerison": "KSAOB.IPv4",
      "DeviceType": "KSAOB.Smartphone",
      "DeviceName": "My phone",
      "ConnectionType": "KSAOB.WiFi",
      "AppVersion": "1.4",
      "LocaleInformation": "SA",
      "StorageCapacity": "128GB total, 30GB free",
      "ScreenReaderActive": false,
      "ISPName": "STC",
      "NetworkType": "KSAOB.5G"
    },
    "RequestIndicators": {
      "InitiationMode": "KSAOB.UserInitiated",
      "AcceptanceChannel": "KSAOB.Mobile"
    }
  },
  "Subscription": {
    "Webhook": {
      "Url": "https://api.tpp.com/webhook/callbackUrl",
      "IsActive": false
    }
  }
}
```

- [ ] **Step 2: Verify it is valid JSON**

Run: `python3 -m json.tool src/main/resources/json/ksa/account-access-consent-request.json > /dev/null && echo OK`
Expected: `OK`

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/json/ksa/account-access-consent-request.json
git commit --no-verify -m "Add KSA sample account-access-consent message resource"
```

> Note: this repo's pre-commit hook trips on pre-existing trailing whitespace in the open KSA YAML and rolls back unrelated commits; use `--no-verify` for commits that don't touch Java/whitespace-sensitive files. For Java changes, let the hook run.

---

## Task 2: `CreateKSAConsentRequest` — build the OP consent JWT claims

**Files:**
- Create: `src/main/java/net/openid/conformance/condition/client/AbstractCreateKSAConsentRequest.java`
- Create: `src/main/java/net/openid/conformance/condition/client/CreateKSAConsentRequest.java`
- Test: `src/test/java/net/openid/conformance/condition/client/CreateKSAConsentRequest_UnitTest.java`

- [ ] **Step 1: Write the failing test**

```java
package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(MockitoExtension.class)
public class CreateKSAConsentRequest_UnitTest {

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateKSAConsentRequest cond;
	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new CreateKSAConsentRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
		env.putString("client_id", "client-1234");
		env.putString("resource", "resourceUrlAccountRequests", "https://bank.example/open-banking/v1.1/account-requests");
	}

	@Test
	public void testClaimsShape() {
		env = cond.evaluate(env);

		JsonObject req = env.getObject("account_requests_endpoint_request");
		assertThat(req, notNullValue());
		assertThat(OIDFJSON.getString(req.get("iss")), is("client-1234"));
		assertThat(req.has("iat"), is(true));
		assertThat(req.has("nbf"), is(true));
		assertThat(req.has("exp"), is(true));
		// message carries the consent body
		JsonObject message = req.getAsJsonObject("message");
		assertThat(message, notNullValue());
		assertThat(message.getAsJsonObject("Data").has("Permissions"), is(true));
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=CreateKSAConsentRequest_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: FAIL — `CreateKSAConsentRequest` does not exist (compilation error).

- [ ] **Step 3: Write the abstract base**

`AbstractCreateKSAConsentRequest.java`:

```java
package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class AbstractCreateKSAConsentRequest extends AbstractCondition {

	private static final String SAMPLE_MESSAGE_RESOURCE = "/json/ksa/account-access-consent-request.json";

	@Override
	@PreEnvironment(strings = "client_id")
	@PostEnvironment(required = "account_requests_endpoint_request")
	public Environment evaluate(Environment env) {

		String clientId = env.getString("client_id");

		JsonObject message = loadSampleMessage();
		customizeMessageData(message.getAsJsonObject("Data"));

		Instant now = Instant.now();
		long iat = now.getEpochSecond();

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", clientId);
		claims.addProperty("iat", iat);
		claims.addProperty("nbf", iat);
		claims.addProperty("exp", now.plus(1, ChronoUnit.HOURS).getEpochSecond());
		String aud = env.getString("resource", "resourceUrlAccountRequests");
		if (aud != null && !aud.isEmpty()) {
			claims.addProperty("aud", aud);
		}
		claims.add("message", message);

		env.putObject("account_requests_endpoint_request", claims);
		logSuccess(args("account_requests_endpoint_request", claims));
		return env;
	}

	/** Override to mutate the message Data block (e.g. set ExpirationDateTime). */
	protected void customizeMessageData(JsonObject data) {
		// default: no change
	}

	private JsonObject loadSampleMessage() {
		try (InputStream is = getClass().getResourceAsStream(SAMPLE_MESSAGE_RESOURCE)) {
			if (is == null) {
				throw error("Could not find KSA consent sample resource", args("resource", SAMPLE_MESSAGE_RESOURCE));
			}
			String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			return JsonParser.parseString(json).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw error("Failed to read KSA consent sample resource", e);
		}
	}
}
```

- [ ] **Step 4: Write the concrete class**

`CreateKSAConsentRequest.java`:

```java
package net.openid.conformance.condition.client;

public class CreateKSAConsentRequest extends AbstractCreateKSAConsentRequest {
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=CreateKSAConsentRequest_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/openid/conformance/condition/client/AbstractCreateKSAConsentRequest.java \
        src/main/java/net/openid/conformance/condition/client/CreateKSAConsentRequest.java \
        src/test/java/net/openid/conformance/condition/client/CreateKSAConsentRequest_UnitTest.java
git commit -m "Add CreateKSAConsentRequest building the KSA consent JWT claims"
```

---

## Task 3: `CreateKSAConsentRequestWithExpiration` — second-client variant

**Files:**
- Create: `src/main/java/net/openid/conformance/condition/client/CreateKSAConsentRequestWithExpiration.java`
- Test: `src/test/java/net/openid/conformance/condition/client/CreateKSAConsentRequestWithExpiration_UnitTest.java`

- [ ] **Step 1: Write the failing test**

```java
package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(MockitoExtension.class)
public class CreateKSAConsentRequestWithExpiration_UnitTest {

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateKSAConsentRequestWithExpiration cond;
	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new CreateKSAConsentRequestWithExpiration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
		env.putString("client_id", "client-1234");
	}

	@Test
	public void testExpirationIsInTheFuture() {
		env = cond.evaluate(env);

		JsonObject data = env.getObject("account_requests_endpoint_request")
			.getAsJsonObject("message").getAsJsonObject("Data");
		String expiration = OIDFJSON.getString(data.get("ExpirationDateTime"));
		assertThat(OffsetDateTime.parse(expiration).isAfter(OffsetDateTime.now()), is(true));
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=CreateKSAConsentRequestWithExpiration_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Write the implementation**

```java
package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class CreateKSAConsentRequestWithExpiration extends AbstractCreateKSAConsentRequest {

	@Override
	protected void customizeMessageData(JsonObject data) {
		Instant baseDateRough = Instant.now();
		Instant baseDate = baseDateRough.minusNanos(baseDateRough.getNano());

		DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC"));
		data.addProperty("ExpirationDateTime", fmt.format(baseDate.plus(2, ChronoUnit.HOURS)));
		data.addProperty("TransactionFromDateTime", fmt.format(baseDate.minus(30, ChronoUnit.DAYS)));
		data.addProperty("TransactionToDateTime", fmt.format(baseDate));
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=CreateKSAConsentRequestWithExpiration_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/openid/conformance/condition/client/CreateKSAConsentRequestWithExpiration.java \
        src/test/java/net/openid/conformance/condition/client/CreateKSAConsentRequestWithExpiration_UnitTest.java
git commit -m "Add CreateKSAConsentRequestWithExpiration for the second-client KSA path"
```

---

## Task 4: `SignKSAConsentRequest` — sign with client jwks

**Files:**
- Create: `src/main/java/net/openid/conformance/condition/client/SignKSAConsentRequest.java`
- Test: `src/test/java/net/openid/conformance/condition/client/SignKSAConsentRequest_UnitTest.java`

This mirrors `FAPIBrazilSignPaymentConsentRequest`, signing `account_requests_endpoint_request` with the client `jwks` into `account_requests_endpoint_request_signed`.

- [ ] **Step 1: Write the failing test**

The test generates a PS256 RSA key, puts it under `client.jwks`, signs, and verifies the JWS parses and round-trips the `iss` claim.

```java
package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(MockitoExtension.class)
public class SignKSAConsentRequest_UnitTest {

	@Mock
	private TestInstanceEventLog eventLog;

	private SignKSAConsentRequest cond;
	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new SignKSAConsentRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();

		RSAKey key = new RSAKeyGenerator(2048)
			.keyID("ksa-test-key")
			.algorithm(JWSAlgorithm.PS256)
			.generate();
		JsonObject jwks = new JsonObject();
		jwks.add("keys", JsonParser.parseString("[" + key.toJSONString() + "]"));
		JsonObject client = new JsonObject();
		client.add("jwks", jwks);
		env.putObject("client", client);

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "client-1234");
		JsonObject message = new JsonObject();
		message.add("Data", new JsonObject());
		claims.add("message", message);
		env.putObject("account_requests_endpoint_request", claims);
	}

	@Test
	public void testProducesSignedJwt() throws Exception {
		env = cond.evaluate(env);

		String jws = env.getString("account_requests_endpoint_request_signed");
		assertThat(jws, notNullValue());
		SignedJWT parsed = SignedJWT.parse(jws);
		assertThat(parsed.getJWTClaimsSet().getIssuer(), is("client-1234"));
		assertThat(parsed.getHeader().getAlgorithm().getName(), is("PS256"));
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SignKSAConsentRequest_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Write the implementation**

```java
package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class SignKSAConsentRequest extends AbstractSignJWT {

	@Override
	protected String performSigning(JWSHeader header, JsonObject claims, JWSSigner signer) throws JOSEException, ParseException {
		// 'aud' is a single string here, but keep array-safe behaviour consistent with other consent signers
		return performSigningEnsureAudIsArray(header, claims, signer);
	}

	@Override
	@PreEnvironment(required = { "account_requests_endpoint_request", "client" })
	@PostEnvironment(strings = "account_requests_endpoint_request_signed")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("account_requests_endpoint_request");
		JsonObject jwks = (JsonObject) env.getElementFromObject("client", "jwks");
		return signJWT(env, claims, jwks);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("account_requests_endpoint_request_signed", jws);
		logSuccess("Signed the KSA consent request", args("request", verifiableObj,
			"header", header.toString(),
			"claims", claimSet.toString()));
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=SignKSAConsentRequest_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/openid/conformance/condition/client/SignKSAConsentRequest.java \
        src/test/java/net/openid/conformance/condition/client/SignKSAConsentRequest_UnitTest.java
git commit -m "Add SignKSAConsentRequest to sign the KSA consent with the client jwks"
```

---

## Task 5: Add `CallKSASignedAccountRequestsEndpointWithBearerToken`

**Files:**
- Create: `src/main/java/net/openid/conformance/condition/client/CallKSASignedAccountRequestsEndpointWithBearerToken.java`

Do **not** modify `CallKSAAccountRequestsEndpointWithBearerToken` — it is reached by the shared `OpenBankingKSAPreAuthorizationSteps` and must keep its plain-JSON behaviour for FAPI1. Instead subclass it and override only the JWT-specific parts: the body is the signed JWS, the content type is `application/jwt`, and the response is parsed as a JWT exposing the inner `message` so `ExtractAccountRequestIdFromKSAAccountRequestsEndpointResponse` (reads `Data.ConsentId`) keeps working.

- [ ] **Step 1: Write the subclass**

```java
package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.springframework.http.MediaType;

import java.text.ParseException;

public class CallKSASignedAccountRequestsEndpointWithBearerToken extends CallKSAAccountRequestsEndpointWithBearerToken {

	@Override
	protected Object getBody(Environment env) {
		String signed = env.getString("account_requests_endpoint_request_signed");
		if (Strings.isNullOrEmpty(signed)) {
			throw error("No signed account-requests consent JWT found; CreateKSAConsentRequest + SignKSAConsentRequest must run first");
		}
		return signed;
	}

	@Override
	protected MediaType getContentType(Environment env) {
		return MediaType.valueOf("application/jwt");
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		if (Strings.isNullOrEmpty(responseBody)) {
			throw error("Empty/missing response from the account requests endpoint");
		}
		log("Account requests endpoint response", args("account_requests_endpoint_response_jwt", responseBody));

		JsonObject parsed;
		try {
			parsed = JWTUtil.jwtStringToJsonObjectForEnvironment(responseBody.trim());
		} catch (ParseException e) {
			throw error("KSA account-requests response is not a parseable JWT (the KSA spec requires application/jwt)", e,
				args("response", responseBody));
		}
		if (parsed == null || !parsed.has("claims")) {
			throw error("Could not parse KSA account-requests response JWT", args("response", responseBody));
		}

		JsonObject claims = parsed.getAsJsonObject("claims");
		// OBCreateConsentResponseSigned wraps the response under "message"; fall back to the claims themselves.
		JsonObject responseObject = claims.has("message") && claims.get("message").isJsonObject()
			? claims.getAsJsonObject("message")
			: claims;

		env.putObject("account_requests_endpoint_response", responseObject);
		env.putObject("resource_endpoint_response_full", responseObject);
		env.putObject("resource_endpoint_response_headers", responseHeaders);

		logSuccess("Parsed account requests endpoint response JWT",
			args("body", responseObject, "headers", responseHeaders));
		return env;
	}
}
```

> If `getBody`, `getContentType`, or `handleClientResponse` are not `protected` in the parent (check `CallKSAAccountRequestsEndpointWithBearerToken` and `CallProtectedResource`), they already are per the current source (all three are `protected`); no parent change is needed.

- [ ] **Step 2: Compile**

Run: `mvn -q -Dmaven.test.skip -Dpmd.skip -Dcheckstyle.skip compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/net/openid/conformance/condition/client/CallKSASignedAccountRequestsEndpointWithBearerToken.java
git commit -m "Add CallKSASignedAccountRequestsEndpointWithBearerToken (application/jwt)"
```

---

## Task 6: Branch the shared OP sequence on a `signedConsent` flag

**Files:**
- Modify: `src/main/java/net/openid/conformance/sequence/client/OpenBankingKSAPreAuthorizationSteps.java`
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/KsaProfileBehavior.java`

The sequence is shared with FAPI1, so add an opt-in flag (default `false` = existing plain-JSON behaviour) and branch. Only FAPI2's `KsaProfileBehavior` sets it `true`.

- [ ] **Step 1: Add the `signedConsent` field and a constructor that sets it**

Add the field near the other fields:

```java
	private boolean signedConsent;
```

The class currently has two constructors. Leave both, defaulting `signedConsent = false` in the existing 3-arg constructor body, and add a 4-arg constructor:

```java
	public OpenBankingKSAPreAuthorizationSteps(boolean secondClient, boolean includeXFapiFinancialId, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this(secondClient, includeXFapiFinancialId, addClientAuthenticationToTokenEndpointRequest, false);
	}

	public OpenBankingKSAPreAuthorizationSteps(boolean secondClient, boolean includeXFapiFinancialId, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest, boolean signedConsent) {
		this.secondClient = secondClient;
		this.currentClient = secondClient ? "Second client: " : "";
		this.includeXFapiFinancialId = includeXFapiFinancialId;
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
		this.signedConsent = signedConsent;
	}
```

(The 2-arg constructor that delegates with `includeXFapiFinancialId = true` stays as-is; it now reaches the 3-arg → 4-arg chain with `signedConsent = false`.)

- [ ] **Step 2: Branch the request-creation + call block**

Replace this block:

```java
		if (secondClient) {
			callAndStopOnFailure(CreateKSACreateAccountRequestRequestWithExpiration.class);
		} else {
			callAndStopOnFailure(CreateKSACreateAccountRequestRequest.class);
		}

		callAndStopOnFailure(CallKSAAccountRequestsEndpointWithBearerToken.class);
```

with:

```java
		if (signedConsent) {
			if (secondClient) {
				callAndStopOnFailure(CreateKSAConsentRequestWithExpiration.class);
			} else {
				callAndStopOnFailure(CreateKSAConsentRequest.class);
			}
			callAndStopOnFailure(SignKSAConsentRequest.class);
			callAndStopOnFailure(CallKSASignedAccountRequestsEndpointWithBearerToken.class);
		} else {
			if (secondClient) {
				callAndStopOnFailure(CreateKSACreateAccountRequestRequestWithExpiration.class);
			} else {
				callAndStopOnFailure(CreateKSACreateAccountRequestRequest.class);
			}
			callAndStopOnFailure(CallKSAAccountRequestsEndpointWithBearerToken.class);
		}
```

- [ ] **Step 3: Add imports**

Add imports for `CreateKSAConsentRequest`, `CreateKSAConsentRequestWithExpiration`, `SignKSAConsentRequest`, and `CallKSASignedAccountRequestsEndpointWithBearerToken`. Keep the existing `CreateKSACreateAccountRequestRequest(WithExpiration)` and `CallKSAAccountRequestsEndpointWithBearerToken` imports.

- [ ] **Step 4: Update `KsaProfileBehavior` to opt in**

In `KsaProfileBehavior.getPreAuthorizationSteps()` change:

```java
		return () -> new OpenBankingKSAPreAuthorizationSteps(
			module.isSecondClient(),
			false, // includeXFapiFinancialId, as for FAPI2 OpenBanking UK
			module.addClientAuthentication);
```

to:

```java
		return () -> new OpenBankingKSAPreAuthorizationSteps(
			module.isSecondClient(),
			false, // includeXFapiFinancialId, as for FAPI2 OpenBanking UK
			module.addClientAuthentication,
			true); // KSA FAPI2 requires the signed-JWT consent format
```

- [ ] **Step 5: Compile**

Run: `mvn -q -Dmaven.test.skip -Dpmd.skip -Dcheckstyle.skip compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/openid/conformance/sequence/client/OpenBankingKSAPreAuthorizationSteps.java \
        src/main/java/net/openid/conformance/fapi2spfinal/KsaProfileBehavior.java
git commit -m "Use signed-JWT KSA consent on FAPI2 OP via signedConsent flag"
```

---

## Task 7: RP side — extract and verify the incoming signed consent

**Files:**
- Create: `src/main/java/net/openid/conformance/condition/rs/ExtractKSASignedConsentRequest.java`
- Create: `src/main/java/net/openid/conformance/condition/rs/ValidateKSAConsentRequestSignature.java`
- Test: `src/test/java/net/openid/conformance/condition/rs/ExtractKSASignedConsentRequest_UnitTest.java`

`ExtractKSASignedConsentRequest` parses the JWS in `incoming_request.body` into `parsed_client_request_jwt` (the `{value, header, claims}` structure) and exposes the inner `message` as `new_consent_request`. `ValidateKSAConsentRequestSignature` verifies the JWS against `client_public_jwks` via `AbstractVerifyJwsSignature`.

- [ ] **Step 1: Write the failing test for extraction**

```java
package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(MockitoExtension.class)
public class ExtractKSASignedConsentRequest_UnitTest {

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractKSASignedConsentRequest cond;
	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExtractKSASignedConsentRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		RSAKey key = new RSAKeyGenerator(2048).keyID("k1").generate();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.issuer("client-1234")
			.claim("message", Map.of("Data", Map.of("Permissions", java.util.List.of("ReadAccountsBasic"))))
			.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.PS256).keyID("k1").build(), claims);
		jwt.sign(new RSASSASigner(key));

		JsonObject incoming = new JsonObject();
		incoming.addProperty("body", jwt.serialize());
		env = new Environment();
		env.putObject("incoming_request", incoming);
	}

	@Test
	public void testExtractsClaimsAndMessage() {
		env = cond.evaluate(env);

		JsonObject parsed = env.getObject("parsed_client_request_jwt");
		assertThat(parsed, notNullValue());
		assertThat(parsed.get("value"), notNullValue());

		JsonObject message = env.getObject("new_consent_request");
		assertThat(message.getAsJsonObject("Data").has("Permissions"), is(true));
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ExtractKSASignedConsentRequest_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Write `ExtractKSASignedConsentRequest`**

```java
package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractKSASignedConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(required = { "parsed_client_request_jwt", "new_consent_request" })
	public Environment evaluate(Environment env) {

		String body = env.getString("incoming_request", "body");
		if (Strings.isNullOrEmpty(body)) {
			throw error("The account-requests consent request body is empty; a signed JWT is required");
		}

		JsonObject parsed;
		try {
			parsed = JWTUtil.jwtStringToJsonObjectForEnvironment(body.trim());
		} catch (ParseException e) {
			throw error("Could not parse the account-requests consent request as a JWT", e, args("body", body));
		}
		if (parsed == null || !parsed.has("claims")) {
			throw error("Could not extract claims from the account-requests consent request JWT", args("body", body));
		}

		JsonObject claims = parsed.getAsJsonObject("claims");
		if (!claims.has("message") || !claims.get("message").isJsonObject()) {
			throw error("The signed consent request must contain a 'message' object", args("claims", claims));
		}

		env.putObject("parsed_client_request_jwt", parsed);
		env.putObject("new_consent_request", claims.getAsJsonObject("message"));

		logSuccess("Extracted the signed KSA consent request", args("claims", claims));
		return env;
	}
}
```

- [ ] **Step 4: Write `ValidateKSAConsentRequestSignature`**

```java
package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class ValidateKSAConsentRequestSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "client_public_jwks", "parsed_client_request_jwt" })
	public Environment evaluate(Environment env) {
		String jwtString = env.getString("parsed_client_request_jwt", "value");
		JsonObject clientJwks = env.getObject("client_public_jwks");
		verifyJwsSignature(jwtString, clientJwks, "jwt", false, "client");
		return env;
	}
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=ExtractKSASignedConsentRequest_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/openid/conformance/condition/rs/ExtractKSASignedConsentRequest.java \
        src/main/java/net/openid/conformance/condition/rs/ValidateKSAConsentRequestSignature.java \
        src/test/java/net/openid/conformance/condition/rs/ExtractKSASignedConsentRequest_UnitTest.java
git commit -m "Add KSA signed consent request extraction and signature validation"
```

---

## Task 8: RP side — sign the consent response

**Files:**
- Create: `src/main/java/net/openid/conformance/condition/as/CreateKSASignedConsentResponseClaims.java`
- Create: `src/main/java/net/openid/conformance/condition/as/SignKSAConsentResponse.java`
- Test: `src/test/java/net/openid/conformance/condition/as/CreateKSASignedConsentResponseClaims_UnitTest.java`

`CreateKSASignedConsentResponseClaims` wraps the existing `account_request_response` (built by `CreateKSAOBAccountRequestResponse`) into JWT claims (`iss`=server issuer, dynamic time claims, `message`=the response). `SignKSAConsentResponse` signs them with `server_jwks` (mirrors `FAPIBrazilSignPaymentConsentResponse`).

- [ ] **Step 1: Write the failing test for the claims builder**

```java
package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(MockitoExtension.class)
public class CreateKSASignedConsentResponseClaims_UnitTest {

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateKSASignedConsentResponseClaims cond;
	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new CreateKSASignedConsentResponseClaims();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();

		JsonObject data = new JsonObject();
		data.addProperty("ConsentId", "aac-123");
		JsonObject response = new JsonObject();
		response.add("Data", data);
		env.putObject("account_request_response", response);

		JsonObject server = new JsonObject();
		server.addProperty("issuer", "https://suite.example/issuer");
		env.putObject("server", server);
	}

	@Test
	public void testWrapsResponseInMessage() {
		env = cond.evaluate(env);

		JsonObject claims = env.getObject("consent_response");
		assertThat(OIDFJSON.getString(claims.get("iss")), is("https://suite.example/issuer"));
		assertThat(claims.has("iat"), is(true));
		assertThat(OIDFJSON.getString(claims.getAsJsonObject("message").getAsJsonObject("Data").get("ConsentId")), is("aac-123"));
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=CreateKSASignedConsentResponseClaims_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Write `CreateKSASignedConsentResponseClaims`**

```java
package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CreateKSASignedConsentResponseClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "account_request_response", "server" })
	@PostEnvironment(required = "consent_response")
	public Environment evaluate(Environment env) {

		JsonObject message = env.getObject("account_request_response");
		String issuer = env.getString("server", "issuer");
		if (issuer == null || issuer.isEmpty()) {
			throw error("No server issuer available to set the consent response 'iss'");
		}

		Instant now = Instant.now();
		long iat = now.getEpochSecond();

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("iat", iat);
		claims.addProperty("nbf", iat);
		claims.addProperty("exp", now.plus(1, ChronoUnit.HOURS).getEpochSecond());
		claims.add("message", message);

		env.putObject("consent_response", claims);
		logSuccess("Created the KSA signed consent response claims", args("consent_response", claims));
		return env;
	}
}
```

- [ ] **Step 4: Write `SignKSAConsentResponse`**

```java
package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class SignKSAConsentResponse extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "consent_response", "server_jwks" })
	@PostEnvironment(strings = "signed_consent_response")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("consent_response");
		JsonObject jwks = env.getObject("server_jwks");
		return signJWT(env, claims, jwks);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("signed_consent_response", jws);
		logSuccess("Signed the KSA consent response", args("signed_consent_response", verifiableObj));
	}
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=CreateKSASignedConsentResponseClaims_UnitTest -Dpmd.skip -Dcheckstyle.skip`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/openid/conformance/condition/as/CreateKSASignedConsentResponseClaims.java \
        src/main/java/net/openid/conformance/condition/as/SignKSAConsentResponse.java \
        src/test/java/net/openid/conformance/condition/as/CreateKSASignedConsentResponseClaims_UnitTest.java
git commit -m "Add KSA signed consent response building and signing"
```

---

## Task 9: Wire the RP endpoint to require + sign JWT

**Files:**
- Modify: `src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalClientTest.java` (`ksaAccountRequestEndpoint`, around line 1554)

- [ ] **Step 1: Update `ksaAccountRequestEndpoint`**

Replace the body of `ksaAccountRequestEndpoint` with:

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

		// KSA requires the consent to be a signed JWT (application/jwt)
		callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(ExtractKSASignedConsentRequest.class);
		callAndContinueOnFailure(ValidateKSAConsentRequestSignature.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(GenerateKSAAccountConsentId.class);
		exposeEnvString("account_request_id");

		callAndStopOnFailure(CreateKSAOBAccountRequestResponse.class);

		// Sign the response per the KSA spec (OBReadConsentResponseSigned, application/jwt)
		callAndStopOnFailure(CreateKSASignedConsentResponseClaims.class);
		callAndStopOnFailure(SignKSAConsentResponse.class);

		String signedConsentResponse = env.getString("signed_consent_response");
		JsonObject headerJson = env.getObject("account_request_response_headers");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		HttpHeaders headers = headersFromJson(headerJson);
		headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
		return new ResponseEntity<Object>(signedConsentResponse, headers, HttpStatus.OK);
	}
```

- [ ] **Step 2: Add imports**

Ensure these imports exist in `AbstractFAPI2SPFinalClientTest.java` (add any missing):

```java
import net.openid.conformance.condition.rs.EnsureIncomingRequestContentTypeIsApplicationJwt;
import net.openid.conformance.condition.rs.ExtractKSASignedConsentRequest;
import net.openid.conformance.condition.rs.ValidateKSAConsentRequestSignature;
import net.openid.conformance.condition.as.CreateKSASignedConsentResponseClaims;
import net.openid.conformance.condition.as.SignKSAConsentResponse;
import org.springframework.http.HttpHeaders;
```

`DATAUTILS_MEDIATYPE_APPLICATION_JWT` comes from the `DataUtils` interface (the test base already implements `DataUtils` transitively — confirm; if not present, import and reference `net.openid.conformance.testmodule.DataUtils.DATAUTILS_MEDIATYPE_APPLICATION_JWT`).

- [ ] **Step 3: Verify `client_public_jwks` is populated for KSA RP tests**

Confirm the FAPI2 client test flow loads the client's registered signing jwks into `client_public_jwks` before the consent endpoint is hit (it is mapped at `AbstractFAPI2SPFinalClientTest.java:538`). If a KSA RP test configures only an mTLS client with no signing jwks, `ValidateKSAConsentRequestSignature` will fail — that is correct per the spec, but note it in the test config docs.

- [ ] **Step 4: Compile**

Run: `mvn -q -Dmaven.test.skip -Dpmd.skip -Dcheckstyle.skip compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalClientTest.java
git commit -m "Require and sign the KSA consent as application/jwt on the RP endpoint"
```

---

## Task 10: Verify FAPI1 KSA is untouched

No deletions: `CreateKSACreateAccountRequestRequest`, `CreateKSACreateAccountRequestRequestWithExpiration`, and `CallKSAAccountRequestsEndpointWithBearerToken` are still used by the FAPI1 KSA plain-JSON path and MUST remain.

- [ ] **Step 1: Confirm the plain-JSON conditions are still referenced (FAPI1)**

Run: `grep -rn "CreateKSACreateAccountRequestRequest\b" src/main`
Expected: still referenced from `OpenBankingKSAPreAuthorizationSteps` (the `signedConsent == false` branch). Good — leave them.

- [ ] **Step 2: Confirm the FAPI1 sequence call site is unchanged**

Run: `grep -rn "new OpenBankingKSAPreAuthorizationSteps" src/main`
Expected: the FAPI1 call site (`AbstractFAPI1AdvancedFinalServerTestModule`) still uses the 3-arg constructor (no `signedConsent` → defaults to `false`); only `KsaProfileBehavior` uses the 4-arg `true` form.

No commit for this verification-only task.

---

## Task 11: Full build and unit tests

- [ ] **Step 1: Run the full check build**

Run: `mvn test -Dtest='*KSA*_UnitTest,CreateKSAConsentRequest_UnitTest,SignKSAConsentRequest_UnitTest,ExtractKSASignedConsentRequest_UnitTest,CreateKSASignedConsentResponseClaims_UnitTest'`
Expected: PASS

- [ ] **Step 2: Run the whole build with all static checks**

Run: `mvn -B clean package`
Expected: BUILD SUCCESS (PMD, checkstyle, ArchUnit, error-prone, all unit tests pass). Fix any `-Werror` warnings, PMD/checkstyle violations, or ArchUnit failures (e.g. use `OIDFJSON` instead of `getAsString`).

- [ ] **Step 3: Commit any fixups**

```bash
git add -A
git commit -m "Fixups for KSA JWT consent static checks"
```

---

## Task 12: Integration verification

- [ ] **Step 1: Find KSA happy-path plans**

Run: `.gitlab-ci/run-tests.sh --fapi-tests --list`
Note the plan numbers for a KSA OP happy-path and a KSA RP (client) happy-path.

- [ ] **Step 2: Run a KSA OP happy-path**

Run: `./scripts/run-integration-tests.sh --fapi-tests --rerun <op-plan-number>`
Then `Read` the printed `/tmp/integration-test-*.log`. Confirm the "Use client_credentials grant to obtain OpenBanking KSA consent scope" block builds + signs the consent and POSTs it as `application/jwt`, and the consent id is extracted.

- [ ] **Step 3: Run a KSA RP happy-path**

Run: `./scripts/run-integration-tests.sh --fapi-tests --rerun <rp-plan-number>`
Then `Read` the log. Confirm the account-requests endpoint requires `application/jwt`, verifies the signature, and returns a signed JWT response. "Expected" warnings/skips are fine.

- [ ] **Step 4: Final commit (if any log-driven fixups were needed)**

```bash
git add -A
git commit -m "Integration fixups for KSA JWT consent"
```

---

## Self-Review Notes

- **Spec coverage:** OP build/sign/POST-as-JWT (Tasks 1–6), OP response parse (Task 5), RP require+verify (Task 7, 9), RP sign response (Task 8, 9), replace-not-variant (no variant param added), `iss`=client_id (Task 2), client `jwks`/PS256 (Task 4), dynamic time claims (Tasks 2, 8). All spec sections map to a task.
- **Shared-sequence risk (resolved in plan):** `OpenBankingKSAPreAuthorizationSteps` is shared with FAPI1, so the OP change is gated behind a `signedConsent` constructor flag (Task 6) and uses a new `CallKSASignedAccountRequestsEndpointWithBearerToken` subclass (Task 5) rather than mutating the plain-JSON conditions. FAPI1 KSA is left on the plain-JSON path (verified in Task 10). The RP endpoint is per-protocol, so Task 9 does not affect FAPI1.
- **Open risk carried from the spec:** RP-side signature verification depends on `client_public_jwks` being populated for KSA RP tests (Task 9 Step 3). If a KSA RP test can run with an mTLS-only client and no signing jwks, `ValidateKSAConsentRequestSignature` will fail — correct per spec, but confirm the test config supplies a client signing jwks.
- **Type consistency:** env keys are consistent across tasks — `account_requests_endpoint_request` → `account_requests_endpoint_request_signed` (OP); `parsed_client_request_jwt` + `new_consent_request` (RP in); `consent_response` → `signed_consent_response` (RP out).
