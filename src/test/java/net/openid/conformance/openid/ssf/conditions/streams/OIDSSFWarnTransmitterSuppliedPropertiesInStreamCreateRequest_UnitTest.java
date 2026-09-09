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
public class OIDSSFWarnTransmitterSuppliedPropertiesInStreamCreateRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnTransmitterSuppliedPropertiesInStreamCreateRequest condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFWarnTransmitterSuppliedPropertiesInStreamCreateRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env.putObject("ssf", JsonParser.parseString("{}").getAsJsonObject());
	}

	private void streamInput(String json) {
		env.putObject("ssf", "stream_input", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	void passesForReceiverSuppliedPropertiesOnly() {
		streamInput("{\"events_requested\": [\"urn:example:event:a\"], \"delivery\": {\"method\": \"urn:ietf:rfc:8936\"}, \"description\": \"x\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesWhenNothingWasParsed() {
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void warnsWhenTheReceiverProposesAnAudience() {
		streamInput("{\"aud\": \"https://receiver.example.com\", \"events_requested\": []}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void warnsWhenTheReceiverSendsAStreamId() {
		streamInput("{\"stream_id\": \"mine\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
