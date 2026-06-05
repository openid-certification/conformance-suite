package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates the PDP's evaluations response for short-circuit semantics
 * (`deny_on_first_deny` / `permit_on_first_permit`, Section 7.1.2.1).
 *
 * The expected response lists the decisions in the same form the spec uses in
 * its 7.1.2.1.1 examples: every position the PDP would emit if it ran the full
 * short-circuit sequence. The last entry's decision is the short-circuit
 * trigger value; its first occurrence in the expected list marks the position
 * where the PDP applies the trigger.
 *
 * This condition requires the PDP to truncate the response at the trigger
 * position so that the short-circuit semantic is observable on the wire.
 * Section 7.1.2.1 describes the operations semantically (analogous to
 * boolean `&&` / `||`) without prescribing the wire shape; this test profile
 * pins truncation as the conformant behavior. The actual response is accepted
 * only when:
 *   - its length is exactly trigger_position + 1; and
 *   - every decision matches the expected decision at the same index.
 *
 * A full-length response (the PDP did not truncate) fails even if the trailing
 * entries carry the trigger value; the test asserts the short-circuit behavior
 * is observable on the wire.
 */
public class EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_evaluations_endpoint_expected_response", "authzen_evaluations_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonArray expected = readEvaluationsArray(env, "authzen_evaluations_endpoint_expected_response", "expected");
		if (expected.isEmpty()) {
			throw error("Expected evaluations list is empty; cannot determine the short-circuit trigger value");
		}
		JsonElement expectedLast = expected.get(expected.size() - 1);
		if (!expectedLast.isJsonObject() || !expectedLast.getAsJsonObject().has("decision")) {
			throw error("Expected last evaluation entry has no `decision`", args("obj", expectedLast));
		}
		boolean triggerDecision = OIDFJSON.getBoolean(expectedLast.getAsJsonObject().get("decision"));

		int triggerPosition = -1;
		for (int i = 0; i < expected.size(); i++) {
			JsonElement elem = expected.get(i);
			if (!elem.isJsonObject() || !elem.getAsJsonObject().has("decision")) {
				throw error("An expected evaluations response element is not valid",
					args("position", i, "obj", elem));
			}
			if (OIDFJSON.getBoolean(elem.getAsJsonObject().get("decision")) == triggerDecision) {
				triggerPosition = i;
				break;
			}
		}
		if (triggerPosition == -1) {
			throw error("Expected evaluations list does not contain the trigger decision",
				args("trigger_decision", triggerDecision, "expected", expected));
		}

		JsonArray response = readEvaluationsArray(env, "authzen_evaluations_endpoint_response", "actual");
		int expectedTruncatedSize = triggerPosition + 1;
		if (response.size() != expectedTruncatedSize) {
			throw error("Response did not truncate at the short-circuit trigger position; expected the response array length to be trigger_position + 1",
				args("response_size", response.size(), "expected_size", expectedTruncatedSize, "trigger_position", triggerPosition, "trigger_decision", triggerDecision));
		}
		for (int i = 0; i < response.size(); i++) {
			JsonElement actualElem = response.get(i);
			if (!actualElem.isJsonObject() || !actualElem.getAsJsonObject().has("decision")) {
				throw error("An actual evaluations response element is not valid",
					args("position", i, "obj", actualElem));
			}
			boolean expectedDecision = OIDFJSON.getBoolean(expected.get(i).getAsJsonObject().get("decision"));
			boolean actualDecision = OIDFJSON.getBoolean(actualElem.getAsJsonObject().get("decision"));
			if (expectedDecision != actualDecision) {
				throw error("Response decision at position " + i + " does not match expected",
					args("position", i, "expected", expectedDecision, "actual", actualDecision, "trigger_decision", triggerDecision));
			}
		}
		logSuccess("Response truncated at the short-circuit trigger and decisions match expected",
			args("trigger_decision", triggerDecision, "trigger_position", triggerPosition, "response_size", response.size()));
		return env;
	}

	private JsonArray readEvaluationsArray(Environment env, String objectKey, String role) {
		JsonElement elem = env.getElementFromObject(objectKey, "evaluations");
		if (elem == null) {
			throw error(role + " evaluations response has no `evaluations` array", args("env_key", objectKey));
		}
		if (!elem.isJsonArray()) {
			throw error(role + " evaluations response `evaluations` is not an array",
				args("env_key", objectKey, "value", elem));
		}
		return elem.getAsJsonArray();
	}
}
