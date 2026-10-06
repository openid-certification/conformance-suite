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
class EnsureAuthzenEvaluationsDecisionsMatchIdempotencyCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthzenEvaluationsDecisionsMatchIdempotencyCheck cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureAuthzenEvaluationsDecisionsMatchIdempotencyCheck();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putFirstAndCurrent(String firstJson, String currentJson) {
		JsonObject response = new JsonObject();
		response.add("body_json", JsonParser.parseString(currentJson));
		env.putObject("authzen_api_endpoint_response", response);
		env.putString("authzen_idempotency_first_response_body", firstJson);
	}

	@Test
	public void decisionsMatch_succeeds() {
		putFirstAndCurrent("""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""");
		cond.execute(env);
	}

	@Test
	public void evaluationContextDiffers_succeeds() {
		// Section 5.5.1: per-evaluation context may carry environmental information.
		putFirstAndCurrent("""
			{ "evaluations": [ { "decision": true, "context": { "trace_id": "a" } }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true, "context": { "trace_id": "b" } }, { "decision": false } ] }""");
		cond.execute(env);
	}

	@Test
	public void decisionDiffers_fails() {
		putFirstAndCurrent("""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": true } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void decisionsReordered_fails() {
		// Evaluations responses are positional, so order is part of the answer.
		putFirstAndCurrent("""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": false }, { "decision": true } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void evaluationsCountDiffers_fails() {
		putFirstAndCurrent("""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void decisionMissingInCurrent_fails() {
		putFirstAndCurrent("""
			{ "evaluations": [ { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "context": {} } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void currentBodyWithoutEvaluations_fails() {
		putFirstAndCurrent("""
			{ "evaluations": [ { "decision": true } ] }""",
			"""
			{ "decision": true }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingCurrentBody_fails() {
		env.putObject("authzen_api_endpoint_response", new JsonObject());
		env.putString("authzen_idempotency_first_response_body", "{ \"evaluations\": [] }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
