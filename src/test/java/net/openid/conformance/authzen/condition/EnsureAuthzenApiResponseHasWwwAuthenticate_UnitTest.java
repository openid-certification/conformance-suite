package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EnsureAuthzenApiResponseHasWwwAuthenticate_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthzenApiResponseHasWwwAuthenticate cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureAuthzenApiResponseHasWwwAuthenticate();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putResponse(int status, JsonObject headers) {
		JsonObject response = new JsonObject();
		response.addProperty("status", status);
		if (headers != null) {
			response.add("headers", headers);
		}
		env.putObject("authzen_api_endpoint_response", response);
	}

	@Test
	public void status401WithWwwAuthenticate_succeeds() {
		JsonObject headers = new JsonObject();
		headers.addProperty("WWW-Authenticate", "Bearer realm=\"pdp\"");
		putResponse(401, headers);
		cond.execute(env);
	}

	@Test
	public void status401WithWwwAuthenticateLowercase_succeeds() {
		// Header lookup is case-insensitive.
		JsonObject headers = new JsonObject();
		headers.addProperty("www-authenticate", "Bearer realm=\"pdp\"");
		putResponse(401, headers);
		cond.execute(env);
	}

	@Test
	public void status401WithoutWwwAuthenticate_fails() {
		JsonObject headers = new JsonObject();
		headers.addProperty("Content-Type", "application/json");
		putResponse(401, headers);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void status200_skipsTheCheck() {
		// The §11.3 SHOULD only applies to 401 responses.
		JsonObject headers = new JsonObject();
		headers.addProperty("Content-Type", "application/json");
		putResponse(200, headers);
		cond.execute(env);
	}

	@Test
	public void status403_skipsTheCheck() {
		JsonObject headers = new JsonObject();
		putResponse(403, headers);
		cond.execute(env);
	}

	@Test
	public void missingStatus_fails() {
		// Null status means the response was not captured properly; surface
		// that as a hard error rather than silently passing.
		JsonObject response = new JsonObject();
		JsonObject headers = new JsonObject();
		headers.addProperty("WWW-Authenticate", "Bearer realm=\"pdp\"");
		response.add("headers", headers);
		env.putObject("authzen_api_endpoint_response", response);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void status401NoHeadersObject_fails() {
		putResponse(401, null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
