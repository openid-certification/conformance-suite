package net.openid.conformance.openid.ssf.conditions.streams;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFValidateStreamStatusResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateStreamStatusResponse createCondition() {
		OIDSSFValidateStreamStatusResponse condition = new OIDSSFValidateStreamStatusResponse();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private OIDSSFWarnStreamStatusResponseUnknownMembers createWarnCondition() {
		OIDSSFWarnStreamStatusResponseUnknownMembers condition = new OIDSSFWarnStreamStatusResponseUnknownMembers();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		return condition;
	}

	private void prepare(String bodyJson) {
		JsonObject response = new JsonObject();
		if (bodyJson != null) {
			response.add("body_json", JsonParser.parseString(bodyJson));
		}
		env.putObject("endpoint_response", response);

		JsonObject ssf = new JsonObject();
		JsonObject stream = new JsonObject();
		stream.addProperty("stream_id", "s1");
		ssf.add("stream", stream);
		env.putObject("ssf", ssf);
	}

	@Test
	void passesForValidStatusWithReason() {
		prepare("{\"stream_id\":\"s1\",\"status\":\"paused\",\"reason\":\"maintenance\"}");
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertDoesNotThrow(() -> createWarnCondition().execute(env));
		assertEquals("paused", env.getString("ssf", "stream_status.status"));
	}

	@Test
	void passesForValidStatusWithoutReason() {
		prepare("{\"stream_id\":\"s1\",\"status\":\"enabled\"}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsForEmptyObject() {
		prepare("{}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForMissingStreamId() {
		prepare("{\"status\":\"enabled\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForOtherStreamId() {
		prepare("{\"stream_id\":\"other\",\"status\":\"enabled\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForUnknownStatusValue() {
		prepare("{\"stream_id\":\"s1\",\"status\":\"active\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForNonStringStatus() {
		prepare("{\"stream_id\":\"s1\",\"status\":1}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForNonStringReason() {
		prepare("{\"stream_id\":\"s1\",\"status\":\"enabled\",\"reason\":{\"en\":\"x\"}}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenBodyIsNotAnObject() {
		prepare("[]");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void warnsForUnknownMembers() {
		prepare("{\"stream_id\":\"s1\",\"status\":\"enabled\",\"state\":\"enabled\"}");
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertThrows(ConditionError.class, () -> createWarnCondition().execute(env));
	}
}
