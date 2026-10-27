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
class EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix_UnitTest {

	private static final String DENY = "deny_on_first_deny";

	private static final String PERMIT = "permit_on_first_permit";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putRequest(String optionsJson, int evaluationCount) {
		StringBuilder evaluations = new StringBuilder();
		for (int i = 0; i < evaluationCount; i++) {
			evaluations.append(i == 0 ? "" : ", ").append("{ \"action\": { \"name\": \"read\" } }");
		}
		String options = optionsJson == null ? "" : ", \"options\": " + optionsJson;
		env.putObject("authzen_api_endpoint_request", JsonParser.parseString(
			"{ \"evaluations\": [ " + evaluations + " ]" + options + " }").getAsJsonObject());
	}

	private void put(String semantic, int evaluationCount, String expectedJson, String actualJson) {
		putRequest("{ \"evaluations_semantic\": \"" + semantic + "\" }", evaluationCount);
		JsonObject expected = JsonParser.parseString(expectedJson).getAsJsonObject();
		JsonObject actual = JsonParser.parseString(actualJson).getAsJsonObject();
		env.putObject("authzen_evaluations_endpoint_expected_response", expected);
		env.putObject("authzen_evaluations_endpoint_response", actual);
	}

	@Test
	public void nonBooleanDecision_throws() {
		put(DENY, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": 0 } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not a boolean"), e.getMessage());
	}

	@Test
	public void truncatedResponseMatchingPrefix_succeeds() {
		put(DENY, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""");
		cond.execute(env);
	}

	@Test
	public void expectedListingDecisionsAfterTrigger_truncatesAtFirstTrigger() {
		put(DENY, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": false }, { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""");
		cond.execute(env);
	}

	@Test
	public void triggerAtFirstPosition_truncatedResponseHasOneEntry() {
		put(PERMIT, 2, """
			{ "evaluations": [ { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "decision": true } ] }""");
		cond.execute(env);
	}

	@Test
	public void denyOnFirstDenyWithNoDeny_fullLengthResponseSucceeds() {
		put(DENY, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": true }, { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": true }, { "decision": true } ] }""");
		cond.execute(env);
	}

	@Test
	public void permitOnFirstPermitWithNoPermit_fullLengthResponseSucceeds() {
		put(PERMIT, 2, """
			{ "evaluations": [ { "decision": false }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": false }, { "decision": false } ] }""");
		cond.execute(env);
	}

	@Test
	public void expectedEntryWithoutDecision_fails() {
		put(DENY, 2, """
			{ "evaluations": [ { }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void triggerNeverReached_truncatedResponseFails() {
		put(DENY, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": true }, { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "decision": true } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("one decision for each evaluation"));
	}

	@Test
	public void triggerTakenFromSemanticNotFromExpectedList() {
		// Under permit_on_first_permit the trigger is true, so the expected list
		// [true, false] stops at position 0 whatever its last decision is.
		put(PERMIT, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("did not truncate"));
	}

	@Test
	public void fullLengthResponseEvenWithTriggerFilled_fails() {
		put(DENY, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false }, { "decision": false } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void responseShorterThanTriggerPosition_fails() {
		put(DENY, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void responseDecisionDiffersAtIndex_fails() {
		put(DENY, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": false }, { "decision": false } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void expectedListShortWithoutTrigger_fails() {
		put(DENY, 3, """
			{ "evaluations": [ { "decision": true }, { "decision": true } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": true } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("neither reaches the trigger decision"));
	}

	@Test
	public void expectedListLongerThanRequest_fails() {
		put(DENY, 1, """
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""",
			"""
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyExpected_fails() {
		put(DENY, 2, """
			{ "evaluations": [] }""",
			"""
			{ "evaluations": [] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void requestWithoutShortCircuitSemantic_fails() {
		putRequest("{ \"evaluations_semantic\": \"execute_all\" }", 2);
		env.putObject("authzen_evaluations_endpoint_expected_response", JsonParser.parseString("""
			{ "evaluations": [ { "decision": true } ] }""").getAsJsonObject());
		env.putObject("authzen_evaluations_endpoint_response", JsonParser.parseString("""
			{ "evaluations": [ { "decision": true } ] }""").getAsJsonObject());
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no short-circuit `evaluations_semantic`"));
	}

	@Test
	public void requestWithoutOptions_fails() {
		putRequest(null, 2);
		env.putObject("authzen_evaluations_endpoint_expected_response", JsonParser.parseString("""
			{ "evaluations": [ { "decision": true } ] }""").getAsJsonObject());
		env.putObject("authzen_evaluations_endpoint_response", JsonParser.parseString("""
			{ "evaluations": [ { "decision": true } ] }""").getAsJsonObject());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
