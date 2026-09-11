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
public class OIDSSFWarnUnknownEventsRequestedInStreamRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnUnknownEventsRequestedInStreamRequest condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFWarnUnknownEventsRequestedInStreamRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void prepare(String streamInputJson) {
		JsonObject ssf = JsonParser.parseString("""
			{"default_config": {"events_supported": ["urn:example:event:a", "urn:example:event:b"]}}
			""").getAsJsonObject();
		if (streamInputJson != null) {
			ssf.add("stream_input", JsonParser.parseString(streamInputJson).getAsJsonObject());
		}
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWhenEveryRequestedTypeIsSupported() {
		prepare("{\"events_requested\": [\"urn:example:event:a\"]}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesWithoutEventsRequested() {
		prepare("{\"description\": \"no events_requested\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void warnsForAnUnknownRequestedType() {
		prepare("{\"events_requested\": [\"urn:example:event:a\", \"urn:example:event:typo\"]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void warnsForANonStringEntry() {
		prepare("{\"events_requested\": [42]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
