package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnCaepEventUnknownMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnCaepEventUnknownMembers condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFWarnCaepEventUnknownMembers();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void setUpCaepEvent(String eventType, String eventDataJson) {
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", new JsonObject());
		ssf.getAsJsonObject("caep_event").add("data", JsonParser.parseString(eventDataJson));
		ssf.getAsJsonObject("caep_event").addProperty("type", eventType);
		env.putObject("ssf", ssf);
	}

	@Test
	void everyCaepEventTypeHasAMemberList() {
		for (String eventType : SsfEvents.CAEP_EVENT_TYPES) {
			assertTrue(OIDSSFWarnCaepEventUnknownMembers.EVENT_SPECIFIC_MEMBERS.containsKey(eventType), eventType);
		}
	}

	@Test
	void passesForCommonMembersOnly() {
		setUpCaepEvent(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE,
			"{\"event_timestamp\":1615304991,\"initiating_entity\":\"policy\",\"reason_admin\":{\"en\":\"Policy Violation\"},\"reason_user\":{\"en\":\"Not compliant\"}}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForEventSpecificMembers() {
		setUpCaepEvent(SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE,
			"{\"event_timestamp\":1615304991,\"credential_type\":\"fido2-roaming\",\"change_type\":\"create\","
				+ "\"fido2_aaguid\":\"accced6a-63f5-490a-9eea-e59bc1896cfc\",\"friendly_name\":\"Jane's USB authenticator\","
				+ "\"x509_issuer\":\"CN=x\",\"x509_serial\":\"1\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForEmptyEvent() {
		setUpCaepEvent(SsfEvents.CAEP_SESSION_ESTABLISHED_EVENT_TYPE, "{}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void failsForMisspelledCommonMember() {
		setUpCaepEvent(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, "{\"event_timestmp\":1615304991}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForMemberOfAnotherEventType() {
		setUpCaepEvent(SsfEvents.CAEP_SESSION_PRESENTED_EVENT_TYPE, "{\"fp_ua\":\"abc\",\"acr\":\"AAL2\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForProprietaryMember() {
		setUpCaepEvent(SsfEvents.CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE,
			"{\"principal\":\"USER\",\"current_level\":\"LOW\",\"x_vendor_score\":42}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void passesForEventTypeWithoutMemberList() {
		setUpCaepEvent("https://example.com/event-type/custom", "{\"anything\":true}");
		assertDoesNotThrow(() -> condition.execute(env));
	}
}
