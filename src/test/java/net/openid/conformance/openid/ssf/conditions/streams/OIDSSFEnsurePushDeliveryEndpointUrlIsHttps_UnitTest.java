package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsurePushDeliveryEndpointUrlIsHttps_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsurePushDeliveryEndpointUrlIsHttps condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFEnsurePushDeliveryEndpointUrlIsHttps();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void streamInput(String json) {
		JsonObject ssf = new JsonObject();
		ssf.add("stream_input", JsonParser.parseString(json).getAsJsonObject());
		env.putObject("ssf", ssf);
	}

	@Test
	void passesForAnHttpsPushEndpoint() {
		streamInput("{\"delivery\": {\"method\": \"urn:ietf:rfc:8935\", \"endpoint_url\": \"https://receiver.example.com/push\"}}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void failsForAnHttpPushEndpoint() {
		// RFC 8935 2.1: the push endpoint is "a TLS-enabled HTTP endpoint"
		streamInput("{\"delivery\": {\"method\": \"urn:ietf:rfc:8935\", \"endpoint_url\": \"http://receiver.example.com/push\"}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForAnEndpointThatIsNotAUrl() {
		streamInput("{\"delivery\": {\"method\": \"urn:ietf:rfc:8935\", \"endpoint_url\": \"not a url\"}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void ignoresAPollRequest() {
		streamInput("{\"delivery\": {\"method\": \"urn:ietf:rfc:8936\"}}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void ignoresARequestWithoutDelivery() {
		streamInput("{\"events_requested\": [\"urn:example:event:a\"]}");
		assertDoesNotThrow(() -> condition.execute(env));
	}
}
