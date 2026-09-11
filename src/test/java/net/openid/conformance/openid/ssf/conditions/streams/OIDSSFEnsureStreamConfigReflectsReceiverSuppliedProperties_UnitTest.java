package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties.Operation;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties_UnitTest {

	private static final String FULL_SENT = """
		{"stream_id": "s1", "description": "d", "events_requested": ["urn:a", "urn:b"],
		 "delivery": {"method": "urn:ietf:rfc:8935", "endpoint_url": "https://rx.example.com/push"}}
		""";

	private static final String MATCHING_STREAM = """
		{"stream_id": "s1", "iss": "https://tx.example.com", "aud": "https://rx.example.com", "description": "d",
		 "events_supported": ["urn:a", "urn:b", "urn:c"], "events_requested": ["urn:b", "urn:a"], "events_delivered": ["urn:a"],
		 "delivery": {"method": "urn:ietf:rfc:8935", "endpoint_url": "https://rx.example.com/push"}}
		""";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties condition(Operation operation) {
		var condition = new OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(operation);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepare(String sent, String stream) {
		JsonObject ssf = new JsonObject();
		ssf.add("expected_stream_config", JsonParser.parseString(sent).getAsJsonObject());
		ssf.add("stream", JsonParser.parseString(stream).getAsJsonObject());
		env.putObject("ssf", ssf);
	}

	@Test
	void matchingConfigurationPassesForBothOperations() {
		prepare(FULL_SENT, MATCHING_STREAM);
		assertDoesNotThrow(() -> condition(Operation.UPDATE).execute(env));
		assertDoesNotThrow(() -> condition(Operation.REPLACE).execute(env));
	}

	@Test
	void ignoredDescriptionFails() {
		prepare(FULL_SENT, MATCHING_STREAM.replace("\"description\": \"d\"", "\"description\": \"old\""));
		assertThrows(ConditionError.class, () -> condition(Operation.UPDATE).execute(env));
	}

	@Test
	void truncatedDescriptionPasses() {
		String sent = FULL_SENT.replace("\"description\": \"d\"", "\"description\": \"a long description\"");
		prepare(sent, MATCHING_STREAM.replace("\"description\": \"d\"", "\"description\": \"a long\""));
		assertDoesNotThrow(() -> condition(Operation.UPDATE).execute(env));
		assertDoesNotThrow(() -> condition(Operation.REPLACE).execute(env));
	}

	@Test
	void emptiedDescriptionFails() {
		prepare(FULL_SENT, MATCHING_STREAM.replace("\"description\": \"d\"", "\"description\": \"\""));
		assertThrows(ConditionError.class, () -> condition(Operation.UPDATE).execute(env));
	}

	@Test
	void ignoredEventsRequestedFails() {
		prepare(FULL_SENT, MATCHING_STREAM.replace("[\"urn:b\", \"urn:a\"]", "[\"urn:c\"]"));
		assertThrows(ConditionError.class, () -> condition(Operation.UPDATE).execute(env));
	}

	@Test
	void eventsDeliveredOutsideTheSentRequestFails() {
		prepare(FULL_SENT, MATCHING_STREAM.replace("\"events_delivered\": [\"urn:a\"]", "\"events_delivered\": [\"urn:a\", \"urn:c\"]"));
		assertThrows(ConditionError.class, () -> condition(Operation.REPLACE).execute(env));
	}

	@Test
	void omittedDescriptionMustBeDeletedByReplaceButKeptByUpdate() {
		String sentWithoutDescription = FULL_SENT.replace("\"description\": \"d\", ", "");
		prepare(sentWithoutDescription, MATCHING_STREAM);
		assertDoesNotThrow(() -> condition(Operation.UPDATE).execute(env), "PATCH: 'properties missing in the request MUST NOT be changed'");
		assertThrows(ConditionError.class, () -> condition(Operation.REPLACE).execute(env), "PUT: 'Missing Receiver-Supplied properties MUST be interpreted as requested to be deleted'");
	}

	@Test
	void omittedDeliveryOnReplaceMeansPoll() {
		String sentWithoutDelivery = """
			{"stream_id": "s1", "description": "d", "events_requested": ["urn:a", "urn:b"]}
			""";
		prepare(sentWithoutDelivery, MATCHING_STREAM);
		assertThrows(ConditionError.class, () -> condition(Operation.REPLACE).execute(env), "a push delivery survived a PUT that omitted delivery");
		prepare(sentWithoutDelivery, MATCHING_STREAM.replace("urn:ietf:rfc:8935", "urn:ietf:rfc:8936"));
		assertDoesNotThrow(() -> condition(Operation.REPLACE).execute(env));
	}

	@Test
	void changedPushEndpointFails() {
		prepare(FULL_SENT, MATCHING_STREAM.replace("https://rx.example.com/push\"}}", "https://rx.example.com/other\"}}"));
		assertThrows(ConditionError.class, () -> condition(Operation.UPDATE).execute(env));
	}
}
