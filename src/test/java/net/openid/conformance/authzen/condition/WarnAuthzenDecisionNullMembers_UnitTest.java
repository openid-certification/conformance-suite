package net.openid.conformance.authzen.condition;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class WarnAuthzenDecisionNullMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenDecisionNullMembers cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenDecisionNullMembers();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void put(String json) {
		env.putObject("authzen_api_endpoint_decision", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void noNullMembers_succeeds() {
		put("""
			{ "decision": true, "context": { "reason": "ok" } }""");
		cond.execute(env);
	}

	@Test
	public void nullContext_fails() {
		put("""
			{ "decision": true, "context": null }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
