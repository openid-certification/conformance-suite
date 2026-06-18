package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureTokenScopeSufficient_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureTokenScopeSufficient createCondition(String requiredScope) {
		OIDSSFEnsureTokenScopeSufficient condition = new OIDSSFEnsureTokenScopeSufficient(requiredScope);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareEnv(String grantedScope) {
		env.putObject("ssf", "auth_result", new JsonObject());
		if (grantedScope != null) {
			env.putString("ssf", "current_token_scope", grantedScope);
		}
	}

	private boolean hasError() {
		return env.getElementFromObject("ssf", "auth_result").getAsJsonObject().has("error");
	}

	@Test
	void passesWhenRequiredScopeGranted() {
		prepareEnv("ssf.read ssf.manage");
		createCondition("ssf.manage").execute(env);
		assertFalse(hasError());
	}

	@Test
	void passesWhenReadOpAndReadScopeGranted() {
		prepareEnv("ssf.read");
		createCondition("ssf.read").execute(env);
		assertFalse(hasError());
	}

	@Test
	void failsWith403WhenManageRequiredButOnlyReadGranted() {
		prepareEnv("ssf.read");
		createCondition("ssf.manage").execute(env);
		assertTrue(hasError());
		assertEquals(403, env.getInteger("ssf", "auth_result.status_code"));
		assertEquals("insufficient_scope",
			OIDFJSON.getString(env.getElementFromObject("ssf", "auth_result.error.err")));
	}

	@Test
	void failsWhenNoScopeGranted() {
		prepareEnv(null);
		createCondition("ssf.read").execute(env);
		assertTrue(hasError());
		assertEquals(403, env.getInteger("ssf", "auth_result.status_code"));
	}
}
