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
public class OIDSSFEnsureStreamContainsCaepInteropEvent_UnitTest {

	private static final String STREAM_ID = "stream_123";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureStreamContainsCaepInteropEvent createCondition() {
		OIDSSFEnsureStreamContainsCaepInteropEvent condition = new OIDSSFEnsureStreamContainsCaepInteropEvent(STREAM_ID);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return condition;
	}

	private void prepareStreamConfig(List<String> eventsRequested) {
		JsonObject streamConfig = new JsonObject();
		streamConfig.addProperty("stream_id", STREAM_ID);
		if (eventsRequested != null) {
			streamConfig.add("events_requested", OIDFJSON.convertListToJsonArray(eventsRequested));
		}
		JsonObject streams = new JsonObject();
		streams.add(STREAM_ID, streamConfig);
		JsonObject ssf = new JsonObject();
		ssf.add("streams", streams);
		env.putObject("ssf", ssf);
	}

	@Test
	void shouldPassWhenAllCaepInteropEventsRequested() {
		prepareStreamConfig(List.of(
			SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE,
			SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE,
			SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE
		));
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldPassWhenSingleCaepInteropEventRequested() {
		prepareStreamConfig(List.of(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE));
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldPassWhenCaepInteropEventRequestedAmongUnrelatedEvents() {
		prepareStreamConfig(List.of(
			SsfEvents.CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE,
			SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE
		));
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenNoCaepInteropEventRequested() {
		prepareStreamConfig(List.of(
			SsfEvents.CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE,
			SsfEvents.CAEP_ASSURANCE_LEVEL_CHANGE_EVENT_TYPE
		));
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenEventsRequestedIsEmpty() {
		prepareStreamConfig(List.of());
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenEventsRequestedIsMissing() {
		prepareStreamConfig(null);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenStreamConfigIsMissing() {
		env.putObject("ssf", new JsonObject());
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
