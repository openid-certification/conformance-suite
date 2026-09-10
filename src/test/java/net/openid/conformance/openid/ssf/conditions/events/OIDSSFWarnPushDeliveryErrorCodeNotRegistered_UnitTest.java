package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnPushDeliveryErrorCodeNotRegistered_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnPushDeliveryErrorCodeNotRegistered condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFWarnPushDeliveryErrorCodeNotRegistered();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
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
	public void passesForEveryRegisteredCode() {
		for (String code : OIDSSFWarnPushDeliveryErrorCodeNotRegistered.REGISTERED_ERROR_CODES) {
			prepareResponse("{\"err\":\"" + code + "\",\"description\":\"d\"}");
			assertDoesNotThrow(() -> condition.execute(env), code);
		}
	}

	@Test
	public void passesForInvalidStateFromSsf() {
		prepareResponse("{\"err\":\"invalid_state\",\"description\":\"state mismatch\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void failsForUnregisteredCode() {
		prepareResponse("{\"err\":\"bad_signature\",\"description\":\"d\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsForRegisteredCodeInDifferentCase() {
		prepareResponse("{\"err\":\"Invalid_Key\",\"description\":\"d\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void doesNotGradeWhenErrIsMissing() {
		// the missing member is the format check's finding, not a registry finding
		prepareResponse("{\"description\":\"d\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void doesNotGradeWhenErrIsNotAString() {
		prepareResponse("{\"err\":1}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void doesNotGradeWhenBodyIsNotAJsonObject() {
		prepareResponse(null);
		assertDoesNotThrow(() -> condition.execute(env));
		prepareResponse("[\"invalid_key\"]");
		assertDoesNotThrow(() -> condition.execute(env));
	}
}
