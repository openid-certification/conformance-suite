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
public class OIDSSFValidatePollResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidatePollResponse createCondition() {
		OIDSSFValidatePollResponse condition = new OIDSSFValidatePollResponse();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private OIDSSFWarnPollResponseUnknownMembers createWarnCondition() {
		OIDSSFWarnPollResponseUnknownMembers condition = new OIDSSFWarnPollResponseUnknownMembers();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		return condition;
	}

	private void prepare(String contentType, String bodyJson, Integer maxEvents) {
		JsonObject response = new JsonObject();
		JsonObject headers = new JsonObject();
		if (contentType != null) {
			headers.addProperty("content-type", contentType);
		}
		response.add("headers", headers);
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
	void passesForWellFormedResponse() {
		prepare("application/json", "{\"sets\":{\"jti1\":\"a.b.c\",\"jti2\":\"d.e.f\"},\"moreAvailable\":false}", 10);
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertDoesNotThrow(() -> createWarnCondition().execute(env));
	}

	@Test
	void passesForEmptySetsWithCharsetParameter() {
		prepare("application/json;charset=UTF-8", "{\"sets\":{}}", 10);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesWithoutRecordedRequest() {
		prepare("application/json", "{\"sets\":{\"jti1\":\"a.b.c\"}}", null);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWhenSetsIsMissing() {
		prepare("application/json", "{}", 10);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenSetsIsAnArray() {
		prepare("application/json", "{\"sets\":[\"a.b.c\"]}", 10);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenSetValueIsNotAString() {
		prepare("application/json", "{\"sets\":{\"jti1\":{\"iss\":\"x\"}}}", 10);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenMoreAvailableIsNotBoolean() {
		prepare("application/json", "{\"sets\":{},\"moreAvailable\":\"true\"}", 10);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void passesWhenMoreSetsThanMaxEvents() {
		prepare("application/json", "{\"sets\":{\"jti1\":\"a.b.c\",\"jti2\":\"d.e.f\"}}", 1);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsForWrongContentType() {
		prepare("text/plain", "{\"sets\":{}}", 10);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForMissingContentType() {
		prepare(null, "{\"sets\":{}}", 10);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenBodyIsNotAnObject() {
		prepare("application/json", "[]", 10);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void warnsForUnknownMembers() {
		prepare("application/json", "{\"sets\":{},\"more_available\":true}", 10);
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertThrows(ConditionError.class, () -> createWarnCondition().execute(env));
	}
}
