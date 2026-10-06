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
class EnsureValidDecisionResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureValidDecisionResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureValidDecisionResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putDecision(String json) {
		env.putObject("authzen_api_endpoint_decision", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void booleanDecision_succeeds() {
		putDecision("""
			{ "decision": true }""");
		cond.execute(env);
	}

	@Test
	public void objectContext_succeeds() {
		putDecision("""
			{ "decision": false, "context": { "reason": "archived" } }""");
		cond.execute(env);
	}

	@Test
	public void missingDecision_throws() {
		putDecision("""
			{ "context": {} }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not contain decision"));
	}

	@Test
	public void stringDecision_throws() {
		putDecision("""
			{ "decision": "true" }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not a boolean"));
	}

	@Test
	public void stringContext_throws() {
		putDecision("""
			{ "decision": true, "context": "reason" }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("context is not an object"));
	}

	@Test
	public void arrayContext_throws() {
		putDecision("""
			{ "decision": true, "context": [] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("context is not an object"));
	}
}
