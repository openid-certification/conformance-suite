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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsureAuthzenEvaluationsResponseValsMatchExpectedVals_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthzenEvaluationsResponseValsMatchExpectedVals cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureAuthzenEvaluationsResponseValsMatchExpectedVals();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String expectedJson, String actualJson) {
		JsonObject expected = JsonParser.parseString(expectedJson).getAsJsonObject();
		JsonObject actual = JsonParser.parseString(actualJson).getAsJsonObject();
		env.putObject("authzen_evaluations_endpoint_expected_response", expected);
		env.putObject("authzen_evaluations_endpoint_response", actual);
	}

	@Test
	public void matchingDecisions_succeed() {
		put("""
			{ "evaluations": [ { "decision": true }, { "decision": false }, { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false }, { "decision": true } ] }""");
		cond.execute(env);
	}

	@Test
	public void emptyArrays_succeed() {
		put("""
			{ "evaluations": [] }""",
			"""
			{ "evaluations": [] }""");
		cond.execute(env);
	}

	@Test
	public void sizeMismatch_throws() {
		put("""
			{ "evaluations": [ { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": true } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("number of evaluations"));
	}

	@Test
	public void decisionMismatch_throws() {
		put("""
			{ "evaluations": [ { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "decision": false } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not match expected"));
	}

	@Test
	public void expectedElementMissingDecision_throws() {
		put("""
			{ "evaluations": [ { } ] }""",
			"""
			{ "evaluations": [ { "decision": true } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("expected evaluations response element"));
	}

	@Test
	public void actualElementMissingDecision_throws() {
		put("""
			{ "evaluations": [ { "decision": true } ] }""",
			"""
			{ "evaluations": [ { } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("actual evaluations response element"));
	}

	@Test
	public void positionalMismatch_throws() {
		put("""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": false }, { "decision": true } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not match expected"));
	}
}
