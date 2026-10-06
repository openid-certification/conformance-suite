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
class EnsureValidEvaluationsResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureValidEvaluationsResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureValidEvaluationsResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putResponse(String json) {
		env.putObject("authzen_evaluations_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void validEvaluations_succeeds() {
		putResponse("""
			{ "evaluations": [ { "decision": true }, { "decision": false, "context": { "reason": "denied" } } ] }""");
		cond.execute(env);
	}

	@Test
	public void missingEvaluations_throws() {
		putResponse("""
			{ "decision": true }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("No evaluations element"));
	}

	@Test
	public void evaluationsNotArray_throws() {
		putResponse("""
			{ "evaluations": { "decision": true } }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is not an array"));
	}

	@Test
	public void elementNotObject_throws() {
		putResponse("""
			{ "evaluations": [ true ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is not an object"));
	}

	@Test
	public void elementMissingDecision_throws() {
		putResponse("""
			{ "evaluations": [ { "context": {} } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not contain decision"));
	}

	@Test
	public void elementNonBooleanDecision_throws() {
		putResponse("""
			{ "evaluations": [ { "decision": 1 } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not a boolean"));
	}

	@Test
	public void elementStringContext_throws() {
		putResponse("""
			{ "evaluations": [ { "decision": true }, { "decision": false, "context": "reason" } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("context that is not an object"));
	}
}
