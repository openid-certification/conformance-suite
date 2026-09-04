package net.openid.conformance.openid.ssf.conditions.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
	void shouldIgnoreNonSsfScopesAndGrantSsfSubset() {
		// RFC 6749 §3.3 lets the AS ignore unrecognised scope values — a receiver
		// requesting e.g. "openid ssf.manage" must still obtain a token.
		prepareScope("openid ssf.manage");
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertEquals("ssf.manage", env.getString("scope"));
	}

	@Test
	void shouldGrantDefaultScopesWhenOnlyNonSsfScopesRequested() {
		prepareScope("openid profile");
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertEquals("ssf.read ssf.manage", env.getString("scope"));
	}

	@Test
	void shouldGrantDefaultScopesWhenScopeIsMissing() {
		// RFC 6749 §4.4.2: scope is OPTIONAL for the client_credentials grant.
		prepareScope(null);
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertEquals("ssf.read ssf.manage", env.getString("scope"));
	}

	@Test
	void shouldGrantDefaultScopesWhenScopeIsBlank() {
		prepareScope("   ");
		assertDoesNotThrow(() -> createCondition().execute(env));
		assertEquals("ssf.read ssf.manage", env.getString("scope"));
	}
}
