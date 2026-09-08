package net.openid.conformance.openid.ssf.conditions.streams;

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
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFMoveAccessTokenToUriQueryOverride_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	/**
	 * Exposes the protected URL and header construction of the SSF endpoint call chain so the
	 * test can verify what an actual transmitter request would carry.
	 */
	private static class TestableReadStreamConfigCall extends OIDSSFReadStreamConfigCall {
		HttpHeaders headersFor(Environment env) {
			return getHeaders(env);
		}

		void configureUrl(Environment env) {
			configureResourceUrl(env);
		}
	}

	@BeforeEach
	void setup() {
		env.putString("ssf", "transmitter_metadata.configuration_endpoint", "https://transmitter.example/ssf/streams");
		env.putString("ssf", "stream.stream_id", "stream-1");
	}

	private OIDSSFMoveAccessTokenToUriQueryOverride createCondition() {
		OIDSSFMoveAccessTokenToUriQueryOverride condition = new OIDSSFMoveAccessTokenToUriQueryOverride();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private TestableReadStreamConfigCall createEndpointCall() {
		TestableReadStreamConfigCall call = new TestableReadStreamConfigCall();
		call.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return call;
	}

	@Test
	void failsWithoutAccessToken() {
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void movesTokenIntoQueryAndSuppressesAuthorizationHeader() {
		env.putString("access_token", "value", "token-1234");
		env.putString("access_token", "type", "Bearer");
		env.putObject("resource_endpoint_response_full", new JsonObject());

		createCondition().execute(env);

		assertNull(env.getObject("resource_endpoint_response_full"));

		TestableReadStreamConfigCall call = createEndpointCall();
		call.configureUrl(env);
		assertEquals("https://transmitter.example/ssf/streams?stream_id=stream-1&access_token=token-1234",
			env.getString("protected_resource_url"));

		// RFC 6750 section 2: clients MUST NOT use more than one method to transmit the
		// token - the Authorization header must not accompany the query parameter
		HttpHeaders headers = call.headersFor(env);
		assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
	}

	@Test
	void urlEncodesTokenValueInQuery() {
		env.putString("access_token", "value", "a+b/c=?&d");
		env.putString("access_token", "type", "Bearer");

		createCondition().execute(env);

		TestableReadStreamConfigCall call = createEndpointCall();
		call.configureUrl(env);
		assertEquals("https://transmitter.example/ssf/streams?stream_id=stream-1&access_token=a%2Bb%2Fc%3D%3F%26d",
			env.getString("protected_resource_url"));
	}

	@Test
	void undoRestoresHeaderTokenAndDropsQueryParameter() {
		env.putString("access_token", "value", "token-1234");
		env.putString("access_token", "type", "Bearer");

		createCondition().execute(env);
		OIDSSFMoveAccessTokenToUriQueryOverride.undo(env);

		assertNull(env.getString("ssf", "access_token_query_override"));
		assertNull(env.getString("ssf", "omit_authorization_header"));

		TestableReadStreamConfigCall call = createEndpointCall();
		call.configureUrl(env);
		assertEquals("https://transmitter.example/ssf/streams?stream_id=stream-1", env.getString("protected_resource_url"));
		HttpHeaders headers = call.headersFor(env);
		assertEquals("Bearer token-1234", headers.getFirst(HttpHeaders.AUTHORIZATION));
	}
}
