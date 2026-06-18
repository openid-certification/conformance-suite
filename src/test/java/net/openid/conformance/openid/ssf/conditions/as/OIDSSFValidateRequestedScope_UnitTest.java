package net.openid.conformance.openid.ssf.conditions.as;

import com.google.gson.JsonObject;
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
public class OIDSSFValidateRequestedScope_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateRequestedScope createCondition() {
		OIDSSFValidateRequestedScope condition = new OIDSSFValidateRequestedScope();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareScope(String scope) {
		JsonObject formParams = new JsonObject();
		if (scope != null) {
			formParams.addProperty("scope", scope);
		}
		JsonObject tokenRequest = new JsonObject();
		tokenRequest.add("body_form_params", formParams);
		env.putObject("token_endpoint_request", tokenRequest);
	}

	@Test
	void shouldPassAndEchoScopeWhenBothSsfScopesRequested() {
		prepareScope("ssf.read ssf.manage");
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertEquals("ssf.read ssf.manage", env.getString("scope"));
	}

	@Test
	void shouldPassWhenSingleSsfScopeRequested() {
		prepareScope("ssf.read");
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertEquals("ssf.read", env.getString("scope"));
	}

	@Test
	void shouldFailWhenScopeContainsUnknownValue() {
		prepareScope("ssf.read openid");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenScopeIsMissing() {
		prepareScope(null);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenScopeIsBlank() {
		prepareScope("   ");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
