package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnPollResponseExceedsMaxEvents_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnPollResponseExceedsMaxEvents createCondition() {
		OIDSSFWarnPollResponseExceedsMaxEvents condition = new OIDSSFWarnPollResponseExceedsMaxEvents();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		return condition;
	}

	private void prepare(String bodyJson, Integer maxEvents) {
		JsonObject response = new JsonObject();
		if (bodyJson != null) {
			response.add("body_json", JsonParser.parseString(bodyJson));
		}
		env.putObject("ssf_polling_response", response);

		JsonObject ssf = new JsonObject();
		if (maxEvents != null) {
			JsonObject request = new JsonObject();
			request.addProperty("maxEvents", maxEvents);
			request.addProperty("returnImmediately", true);
			JsonObject poll = new JsonObject();
			poll.add("request", request);
			ssf.add("poll", poll);
		}
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWhenSetsWithinMaxEvents() {
		prepare("{\"sets\":{\"jti1\":\"a.b.c\",\"jti2\":\"d.e.f\"}}", 2);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesForEmptySetsOnAcknowledgeOnlyRequest() {
		prepare("{\"sets\":{}}", 0);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesWithoutRecordedMaxEvents() {
		prepare("{\"sets\":{\"jti1\":\"a.b.c\",\"jti2\":\"d.e.f\"}}", null);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesWhenSetsIsNotAnObject() {
		prepare("{\"sets\":[\"a.b.c\"]}", 0);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void warnsWhenMoreSetsThanMaxEvents() {
		prepare("{\"sets\":{\"jti1\":\"a.b.c\",\"jti2\":\"d.e.f\"}}", 1);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void warnsWhenSetsReturnedForAcknowledgeOnlyRequest() {
		prepare("{\"sets\":{\"jti1\":\"a.b.c\"}}", 0);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
