package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EnsureSearchResultIsPermitted_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	private void run(AbstractCondition cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	@Test
	public void permitted_succeeds() {
		env.putObject("authzen_api_endpoint_request", json("{}"));
		env.putObject("authzen_api_endpoint_decision", json("""
			{ "decision": true }"""));
		run(new EnsureSearchResultIsPermitted());
	}

	@Test
	public void denied_fails() {
		env.putObject("authzen_api_endpoint_request", json("{}"));
		env.putObject("authzen_api_endpoint_decision", json("""
			{ "decision": false }"""));
		assertThrows(ConditionError.class, () -> run(new EnsureSearchResultIsPermitted()));
	}

	@Test
	public void missingDecision_fails() {
		env.putObject("authzen_api_endpoint_request", json("{}"));
		env.putObject("authzen_api_endpoint_decision", json("""
			{ "context": { "reason": "none" } }"""));
		assertThrows(ConditionError.class, () -> run(new EnsureSearchResultIsPermitted()));
	}

	@Test
	public void nonBooleanDecision_failsCleanly() {
		// EnsureValidDecisionResponse rejects this first in the modules; the condition still
		// fails rather than throwing an unexpected type error on its own.
		env.putObject("authzen_api_endpoint_request", json("{}"));
		env.putObject("authzen_api_endpoint_decision", json("""
			{ "decision": "true" }"""));
		assertThrows(ConditionError.class, () -> run(new EnsureSearchResultIsPermitted()));
	}
}
