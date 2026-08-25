package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static net.openid.conformance.openid.ssf.conditions.events.OIDSSFRecordSecurityEventTokenSubjectFormat_UnitTest.setUpSetToken;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFLogObservedSubjectFormats_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFLogObservedSubjectFormats condition;

	private OIDSSFRecordSecurityEventTokenSubjectFormat recorder;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFLogObservedSubjectFormats();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		recorder = new OIDSSFRecordSecurityEventTokenSubjectFormat();
		recorder.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("ssf", new JsonObject());
	}

	private void record(String subIdJson, String eventType) {
		setUpSetToken(env, subIdJson, eventType);
		recorder.execute(env);
	}

	@Test
	void shouldFailWhenNothingWasRecorded() {
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldSummariseSimpleFormats() {
		record("{\"format\":\"opaque\",\"id\":\"stream-1\"}", SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
		record("{\"format\":\"email\",\"email\":\"user@example.com\"}", SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		record("{\"format\":\"iss_sub\",\"iss\":\"https://idp.example.com\",\"sub\":\"1\"}", SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldSummariseComplexSubjects() {
		record("""
			{"format":"complex",
			 "user":{"format":"email","email":"user@example.com"},
			 "device":{"format":"opaque","id":"device-1"}}""", SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE);
		assertDoesNotThrow(() -> condition.execute(env));
	}
}
