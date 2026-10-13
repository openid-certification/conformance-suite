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
class WarnAuthzenEvaluationsResponseUnknownMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenEvaluationsResponseUnknownMembers cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenEvaluationsResponseUnknownMembers();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String json) {
		env.putObject("authzen_evaluations_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
	}

	private void assertUnknown(String json) {
		put(json);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not define"), e.getMessage());
	}

	@Test
	public void definedMembers_succeed() {
		put("""
			{ "evaluations": [ { "decision": true }, { "decision": false, "context": { "reason": "archived" } } ] }""");
		cond.execute(env);
	}

	@Test
	public void topLevelDecisionAndContext_succeed() {
		put("""
			{ "decision": true, "context": {}, "evaluations": [ { "decision": true } ] }""");
		cond.execute(env);
	}

	@Test
	public void unknownTopLevelMember_warns() {
		assertUnknown("""
			{ "evaluations": [ { "decision": true } ], "status": "ok" }""");
	}

	@Test
	public void unknownMemberInEvaluation_warns() {
		assertUnknown("""
			{ "evaluations": [ { "decision": true }, { "decision": false, "reason": "archived" } ] }""");
	}

	@Test
	public void nonObjectEvaluation_isLeftToStructureChecks() {
		put("""
			{ "evaluations": [ true ] }""");
		cond.execute(env);
	}
}
