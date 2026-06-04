package net.openid.conformance.authzen.condition;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String expectedJson, String actualJson) {
		JsonObject expected = JsonParser.parseString(expectedJson).getAsJsonObject();
		JsonObject actual = JsonParser.parseString(actualJson).getAsJsonObject();
		env.putObject("authzen_evaluations_endpoint_expected_response", expected);
		env.putObject("authzen_evaluations_endpoint_response", actual);
	}

	@Test
	public void truncatedResponseMatchingPrefix_succeeds() {
		// deny_on_first_deny: trigger = false. Expected ideal short-circuit
		// shape is [true, false, false]; PDP that truncates emits [true, false].
		put("""
			{ "evaluations": [ { "decision": true }, { "decision": false }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""");
		cond.execute(env);
	}

	@Test
	public void triggerAtFirstPosition_truncatedResponseHasOneEntry() {
		// permit_on_first_permit: trigger = true at position 0. Truncated
		// response has length 1.
		put("""
			{ "evaluations": [ { "decision": true }, { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "decision": true } ] }""");
		cond.execute(env);
	}

	@Test
	public void fullLengthResponseEvenWithTriggerFilled_fails() {
		// A PDP that did not truncate but emitted the trigger value for every
		// later position is rejected — the conformance suite expects the
		// short-circuit to be observable on the wire.
		put("""
			{ "evaluations": [ { "decision": true }, { "decision": false }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false }, { "decision": false } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void responseShorterThanTriggerPosition_fails() {
		// Trigger is at position 1; response of length 1 stopped too early.
		put("""
			{ "evaluations": [ { "decision": true }, { "decision": false }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void responseDecisionDiffersAtIndex_fails() {
		// First decision should be true (natural deny-on-first-deny pre-trigger)
		// but PDP returned false.
		put("""
			{ "evaluations": [ { "decision": true }, { "decision": false }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": false }, { "decision": false } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyExpected_fails() {
		put("""
			{ "evaluations": [] }""",
			"""
			{ "evaluations": [] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
