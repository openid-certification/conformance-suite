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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnPushDeliveryErrorCodeMismatch_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnPushDeliveryErrorCodeMismatch createCondition(String expectedCode) {
		OIDSSFWarnPushDeliveryErrorCodeMismatch condition = new OIDSSFWarnPushDeliveryErrorCodeMismatch(expectedCode);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		return condition;
	}

	private void prepareResponse(String bodyJson) {
		JsonObject response = new JsonObject();
		response.addProperty("status", 400);
		if (bodyJson != null) {
			response.add("body_json", JsonParser.parseString(bodyJson));
		}
		env.putObject("endpoint_response", response);
	}

	@Test
	public void passesWhenTheCodeMatches() {
		prepareResponse("{\"err\":\"invalid_key\",\"description\":\"d\"}");
		assertDoesNotThrow(() -> createCondition("invalid_key").execute(env));
	}

	@Test
	public void failsWhenTheCodeDiffers() {
		prepareResponse("{\"err\":\"invalid_request\",\"description\":\"d\"}");
		ConditionError error = assertThrows(ConditionError.class, () -> createCondition("invalid_key").execute(env));
		assertTrue(error.getMessage().contains("not the code expected"));
	}

	@Test
	public void comparesCaseSensitively() {
		prepareResponse("{\"err\":\"INVALID_KEY\",\"description\":\"d\"}");
		assertThrows(ConditionError.class, () -> createCondition("invalid_key").execute(env));
	}

	@Test
	public void doesNotGradeWhenErrIsMissing() {
		// the missing member is the format check's finding
		prepareResponse("{\"description\":\"d\"}");
		assertDoesNotThrow(() -> createCondition("invalid_key").execute(env));
	}

	@Test
	public void doesNotGradeWhenBodyIsNotAJsonObject() {
		prepareResponse(null);
		assertDoesNotThrow(() -> createCondition("invalid_state").execute(env));
	}
}
