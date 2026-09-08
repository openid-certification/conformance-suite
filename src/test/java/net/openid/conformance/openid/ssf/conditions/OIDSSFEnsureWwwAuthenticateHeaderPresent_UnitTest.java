package net.openid.conformance.openid.ssf.conditions;

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
public class OIDSSFEnsureWwwAuthenticateHeaderPresent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureWwwAuthenticateHeaderPresent createCondition() {
		OIDSSFEnsureWwwAuthenticateHeaderPresent condition = new OIDSSFEnsureWwwAuthenticateHeaderPresent();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareResponse(String headersJson) {
		JsonObject response = new JsonObject();
		if (headersJson != null) {
			response.add("headers", JsonParser.parseString(headersJson));
		}
		env.putObject("endpoint_response", response);
	}

	@Test
	void passesForBearerChallenge() {
		prepareResponse("{\"www-authenticate\":\"Bearer error=\\\"invalid_token\\\"\"}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesForHeaderNameInAnyCaseAndArrayValue() {
		prepareResponse("{\"WWW-Authenticate\":[\"Bearer error=\\\"insufficient_scope\\\", scope=\\\"ssf.manage\\\"\"]}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWhenHeaderIsAbsent() {
		prepareResponse("{\"content-type\":\"application/json\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenChallengeIsNotBearer() {
		prepareResponse("{\"www-authenticate\":\"Basic realm=\\\"ssf\\\"\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenHeadersAreMissing() {
		prepareResponse(null);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
