package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFHandleAuthorizationHeader_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFHandleAuthorizationHeader createCondition() {
		OIDSSFHandleAuthorizationHeader condition = new OIDSSFHandleAuthorizationHeader();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareRequest(String authorizationHeader) {
		JsonObject headers = new JsonObject();
		if (authorizationHeader != null) {
			headers.addProperty("authorization", authorizationHeader);
		}
		JsonObject incomingRequest = new JsonObject();
		incomingRequest.add("headers", headers);
		env.putObject("incoming_request", incomingRequest);
	}

	private void putIssuedToken(String token, long expiresAt, String scope) {
		JsonObject record = new JsonObject();
		record.addProperty("client_id", "ssf-test-client");
		if (scope != null) {
			record.addProperty("scope", scope);
		}
		record.addProperty("expires_at", expiresAt);
		JsonObject issuedTokens = new JsonObject();
		issuedTokens.add(token, record);
		env.putObject("ssf", "issued_tokens", issuedTokens);
	}

	private boolean hasError() {
		return env.getElementFromObject("ssf", "auth_result").getAsJsonObject().has("error");
	}

	// ----- STATIC mode -----

	@Test
	void staticModeAcceptsMatchingToken() {
		env.putString("ssf", "transmitter_access_token", "static-token");
		prepareRequest("Bearer static-token");

		createCondition().execute(env);

		assertFalse(hasError());
	}

	@Test
	void staticModeRejectsMismatchedToken() {
		env.putString("ssf", "transmitter_access_token", "static-token");
		prepareRequest("Bearer wrong-token");

		createCondition().execute(env);

		assertTrue(hasError());
		assertEquals(401, env.getInteger("ssf", "auth_result.status_code"));
	}

	@Test
	void missingHeaderIsUnauthorized() {
		env.putString("ssf", "transmitter_access_token", "static-token");
		prepareRequest(null);

		createCondition().execute(env);

		assertTrue(hasError());
	}

	// ----- DYNAMIC mode -----

	@Test
	void dynamicModeAcceptsValidUnexpiredTokenAndStashesScope() {
		env.putString("ssf", "auth_mode", SsfAuthMode.DYNAMIC.name());
		putIssuedToken("dyn-token", Instant.now().getEpochSecond() + 900, "ssf.read ssf.manage");
		prepareRequest("Bearer dyn-token");

		createCondition().execute(env);

		assertFalse(hasError());
		assertEquals("ssf.read ssf.manage", env.getString("ssf", "current_token_scope"));
	}

	@Test
	void dynamicModeRejectsUnknownToken() {
		env.putString("ssf", "auth_mode", SsfAuthMode.DYNAMIC.name());
		putIssuedToken("dyn-token", Instant.now().getEpochSecond() + 900, "ssf.read");
		prepareRequest("Bearer some-other-token");

		createCondition().execute(env);

		assertTrue(hasError());
	}

	@Test
	void dynamicModeRejectsExpiredToken() {
		env.putString("ssf", "auth_mode", SsfAuthMode.DYNAMIC.name());
		putIssuedToken("dyn-token", Instant.now().getEpochSecond() - 1, "ssf.read");
		prepareRequest("Bearer dyn-token");

		createCondition().execute(env);

		assertTrue(hasError());
	}

	@Test
	void dynamicModeRejectsNonBearerScheme() {
		env.putString("ssf", "auth_mode", SsfAuthMode.DYNAMIC.name());
		putIssuedToken("dyn-token", Instant.now().getEpochSecond() + 900, "ssf.read");
		prepareRequest("Basic dyn-token");

		createCondition().execute(env);

		assertTrue(hasError());
	}
}
