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
public class OIDSSFCheckSupportedEventsForStream_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFCheckSupportedEventsForStream createCondition() {
		OIDSSFCheckSupportedEventsForStream condition = new OIDSSFCheckSupportedEventsForStream();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
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
	void shouldPassWhenAllScimEventsSupported() {
		prepareStreamConfig(List.copyOf(SsfEvents.SCIM_EVENT_TYPES));
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldPassWhenSingleScimEventSupported() {
		prepareStreamConfig(List.of(SsfEvents.SCIM_FEED_ADD_EVENT_TYPE));
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldPassWhenScimEventsMixedWithSsfCaepAndRiscEvents() {
		prepareStreamConfig(List.of(
			SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE,
			SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE,
			SsfEvents.RISC_ACCOUNT_DISABLED_EVENT_TYPE,
			SsfEvents.SCIM_PROV_CREATE_FULL_EVENT_TYPE,
			SsfEvents.SCIM_MISC_ASYNCRESP_EVENT_TYPE
		));
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenUnknownEventTypeSupported() {
		prepareStreamConfig(List.of(
			SsfEvents.SCIM_FEED_ADD_EVENT_TYPE,
			"urn:ietf:params:scim:event:made:up"
		));
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void shouldPassWhenEventsSupportedIsEmpty() {
		prepareStreamConfig(List.of());
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenEventsSupportedIsMissing() {
		prepareStreamConfig(null);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
