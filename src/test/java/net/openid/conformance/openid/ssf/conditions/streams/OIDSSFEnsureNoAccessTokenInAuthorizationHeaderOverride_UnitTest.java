package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	/**
	 * Exposes the protected header construction of the SSF endpoint call chain so the test
	 * can verify what an actual transmitter request would carry.
	 */
	private static class TestableReadStreamConfigCall extends OIDSSFReadStreamConfigCall {
		HttpHeaders headersFor(Environment env) {
			return getHeaders(env);
		}
	}

	private OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride createCondition() {
		OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride condition = new OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private TestableReadStreamConfigCall createEndpointCall() {
		TestableReadStreamConfigCall call = new TestableReadStreamConfigCall();
		call.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return call;
	}

	@Test
	void setsOmitFlagAndClearsStaleResponse() {
		env.putObject("resource_endpoint_response_full", new JsonObject());

		createCondition().execute(env);

		assertEquals("true", env.getString("ssf", "omit_authorization_header"));
		assertNull(env.getObject("resource_endpoint_response_full"));
	}

	@Test
	void requestCarriesNoAuthorizationHeaderEvenWithoutAccessToken() {
		// no access_token in the environment at all: the request must still be buildable
		// (the module probes unauthenticated transmitter behavior)
		createCondition().execute(env);

		HttpHeaders headers = assertDoesNotThrow(() -> createEndpointCall().headersFor(env));
		assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
	}

	@Test
	void requestCarriesNoAuthorizationHeaderDespiteConfiguredAccessToken() {
		env.putString("access_token", "value", "token-1234");
		env.putString("access_token", "type", "Bearer");

		createCondition().execute(env);

		HttpHeaders headers = createEndpointCall().headersFor(env);
		assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
	}

	@Test
	void undoRestoresBearerHeader() {
		env.putString("access_token", "value", "token-1234");
		env.putString("access_token", "type", "Bearer");

		createCondition().execute(env);
		OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride.undo(env);

		assertNull(env.getString("ssf", "omit_authorization_header"));
		HttpHeaders headers = createEndpointCall().headersFor(env);
		assertEquals("Bearer token-1234", headers.getFirst(HttpHeaders.AUTHORIZATION));
	}
}
