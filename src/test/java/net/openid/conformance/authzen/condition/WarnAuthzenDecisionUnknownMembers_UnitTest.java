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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class WarnAuthzenDecisionUnknownMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenDecisionUnknownMembers cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenDecisionUnknownMembers();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String json) {
		env.putObject("authzen_api_endpoint_decision", JsonParser.parseString(json).getAsJsonObject());
	}

	private void assertUnknown(String json) {
		put(json);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not define"), e.getMessage());
	}

	@Test
	public void decisionOnly_succeeds() {
		put("""
			{ "decision": true }""");
		cond.execute(env);
	}

	@Test
	public void decisionAndContext_succeeds() {
		put("""
			{ "decision": false, "context": { "reason": "denied" } }""");
		cond.execute(env);
	}

	@Test
	public void unknownTopLevelMember_warns() {
		assertUnknown("""
			{ "decision": true, "reason": "ok" }""");
	}

	@Test
	public void evaluationsMember_warns() {
		assertUnknown("""
			{ "decision": true, "evaluations": [] }""");
	}
}
