package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class NormalizeAuthzenEvaluationsResponseSingleDecisionToArray_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private NormalizeAuthzenEvaluationsResponseSingleDecisionToArray cond;

	@BeforeEach
	public void setUp() {
		cond = new NormalizeAuthzenEvaluationsResponseSingleDecisionToArray();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putResponse(String json) {
		env.putObject("authzen_evaluations_endpoint_response",
			JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void singleDecisionTrue_isWrappedInArray() {
		putResponse("""
			{ "decision": true }""");
		cond.execute(env);
		JsonObject normalized = env.getObject("authzen_evaluations_endpoint_response");
		assertTrue(normalized.has("evaluations"));
		assertEquals(1, normalized.getAsJsonArray("evaluations").size());
		assertEquals(true, OIDFJSON.getBoolean(
			normalized.getAsJsonArray("evaluations").get(0).getAsJsonObject().get("decision")));
	}

	@Test
	public void singleDecisionFalse_isWrappedInArray() {
		putResponse("""
			{ "decision": false }""");
		cond.execute(env);
		JsonObject normalized = env.getObject("authzen_evaluations_endpoint_response");
		assertEquals(false, OIDFJSON.getBoolean(
			normalized.getAsJsonArray("evaluations").get(0).getAsJsonObject().get("decision")));
	}

	@Test
	public void alreadyArrayForm_isUnchanged() {
		String original = """
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""";
		putResponse(original);
		cond.execute(env);
		JsonObject after = env.getObject("authzen_evaluations_endpoint_response");
		assertEquals(JsonParser.parseString(original).getAsJsonObject(), after);
	}

	@Test
	public void neitherEvaluationsNorDecision_fails() {
		putResponse("{}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
