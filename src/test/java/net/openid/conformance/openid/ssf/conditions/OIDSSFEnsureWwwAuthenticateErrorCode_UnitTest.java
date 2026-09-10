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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureWwwAuthenticateErrorCode_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureWwwAuthenticateErrorCode createCondition(String expectedErrorCode) {
		OIDSSFEnsureWwwAuthenticateErrorCode condition = new OIDSSFEnsureWwwAuthenticateErrorCode(expectedErrorCode);
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
	void passesForMatchingQuotedErrorCode() {
		prepareResponse("{\"www-authenticate\":\"Bearer realm=\\\"ssf\\\", error=\\\"invalid_token\\\", error_description=\\\"expired\\\"\"}");
		assertDoesNotThrow(() -> createCondition("invalid_token").execute(env));
	}

	@Test
	void passesForMatchingUnquotedErrorCode() {
		prepareResponse("{\"WWW-Authenticate\":\"Bearer error=invalid_token\"}");
		assertDoesNotThrow(() -> createCondition("invalid_token").execute(env));
	}

	@Test
	void passesForArrayValueAndMixedCaseParameterName() {
		prepareResponse("{\"WWW-Authenticate\":[\"Bearer scope=\\\"ssf.manage\\\", Error=\\\"insufficient_scope\\\"\"]}");
		assertDoesNotThrow(() -> createCondition("insufficient_scope").execute(env));
	}

	@Test
	void failsForDifferentErrorCode() {
		prepareResponse("{\"www-authenticate\":\"Bearer error=\\\"insufficient_scope\\\", scope=\\\"ssf.manage\\\"\"}");
		assertThrows(ConditionError.class, () -> createCondition("invalid_token").execute(env));
	}

	@Test
	void failsWhenErrorParameterIsMissing() {
		prepareResponse("{\"www-authenticate\":\"Bearer realm=\\\"ssf\\\", error_description=\\\"no code here\\\"\"}");
		assertThrows(ConditionError.class, () -> createCondition("invalid_token").execute(env));
	}

	@Test
	void failsForBareBearerChallenge() {
		prepareResponse("{\"www-authenticate\":\"Bearer\"}");
		assertThrows(ConditionError.class, () -> createCondition("invalid_token").execute(env));
	}

	@Test
	void failsWhenHeaderIsAbsent() {
		prepareResponse("{\"content-type\":\"application/json\"}");
		assertThrows(ConditionError.class, () -> createCondition("invalid_token").execute(env));
	}

	@Test
	void failsWhenHeadersAreMissing() {
		prepareResponse(null);
		assertThrows(ConditionError.class, () -> createCondition("invalid_token").execute(env));
	}

	@Test
	void failsWhenChallengeIsNotBearer() {
		prepareResponse("{\"www-authenticate\":\"Basic realm=\\\"ssf\\\"\"}");
		assertThrows(ConditionError.class, () -> createCondition("invalid_token").execute(env));
	}

	@Test
	void parsesQuotedValuesContainingCommasAndEscapes() {
		Map<String, String> params = OIDSSFEnsureWwwAuthenticateErrorCode.parseAuthParams(
			" realm=\"a, b\", error_description=\"say \\\"hi\\\"\", error=invalid_token");
		assertEquals("a, b", params.get("realm"));
		assertEquals("say \"hi\"", params.get("error_description"));
		assertEquals("invalid_token", params.get("error"));
	}
}
