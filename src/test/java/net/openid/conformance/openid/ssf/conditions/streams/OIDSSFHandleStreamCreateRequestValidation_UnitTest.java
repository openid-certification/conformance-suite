package net.openid.conformance.openid.ssf.conditions.streams;

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
public class OIDSSFHandleStreamCreateRequestValidation_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFHandleStreamCreateRequestValidation condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFHandleStreamCreateRequestValidation();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env.putObject("ssf", JsonParser.parseString("{}").getAsJsonObject());
	}

	private void streamInput(String json) {
		env.putObject("ssf", "stream_input", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	void acceptsAMinimalCreateRequest() {
		streamInput("{}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void acceptsTransmitterSuppliedPropertiesInACreateRequest() {
		// their presence is a sender-side WARNING raised by a separate condition, not a
		// reason to reject the request (SSF 1.0 8.1.1.1, Table 1)
		streamInput("{\"aud\": \"https://receiver.example.com\", \"events_requested\": []}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void rejectsAPushDeliveryWithoutEndpointUrl() {
		streamInput("{\"delivery\": {\"method\": \"urn:ietf:rfc:8935\"}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void rejectsAnUnknownDeliveryMethod() {
		streamInput("{\"delivery\": {\"method\": \"urn:example:carrier-pigeon\"}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void rejectsEventsRequestedThatIsNotAnArray() {
		streamInput("{\"events_requested\": \"urn:example:event:a\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
