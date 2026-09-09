package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents createCondition() {
		var condition = new OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareStreamConfig(List<String> eventsSupported) {
		JsonObject stream = new JsonObject();
		if (eventsSupported != null) {
			stream.add("events_supported", OIDFJSON.convertListToJsonArray(eventsSupported));
		}
		JsonObject ssf = new JsonObject();
		ssf.add("stream", stream);
		env.putObject("ssf", ssf);
	}

	@Test
	void shouldPassWhenAQualifyingUseCaseIsSupported() {
		prepareStreamConfig(List.of(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE, SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE));
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldPassWhenEventsSupportedIsOmitted() {
		// SSF 1.0 8.1.1: events_supported is Transmitter-Supplied, OPTIONAL. Whether the
		// transmitter supports a qualifying use case is decided on events_delivered.
		prepareStreamConfig(null);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenNoQualifyingUseCaseIsSupported() {
		prepareStreamConfig(List.of(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE, SsfEvents.RISC_ACCOUNT_DISABLED_EVENT_TYPE));
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenOnlyRiskLevelChangeIsSupported() {
		// risk-level-change is a use case of the WG head only, not of the published draft-01
		prepareStreamConfig(List.of(SsfEvents.CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE));
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenEventsSupportedIsNotAnArray() {
		JsonObject stream = new JsonObject();
		stream.addProperty("events_supported", SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		JsonObject ssf = new JsonObject();
		ssf.add("stream", stream);
		env.putObject("ssf", ssf);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
