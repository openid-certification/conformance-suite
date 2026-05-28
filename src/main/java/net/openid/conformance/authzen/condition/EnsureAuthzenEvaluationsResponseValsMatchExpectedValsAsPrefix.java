package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates the PDP's evaluations response for short-circuit semantics
 * (`deny_on_first_deny` / `permit_on_first_permit`, spec section 7.1.2.1).
 *
 * The expected response lists the decisions in the same form the spec uses in
 * its 7.1.2.1.1 examples: every position the PDP would return if it ran the
 * full short-circuit sequence. The last entry's decision is the short-circuit
 * trigger value, and its first occurrence in the expected list marks the
 * position where the PDP must have applied the trigger.
 *
 * The actual response is accepted when:
 *   - it covers at least up to and including the trigger position; and
 *   - every decision it does return matches the expected decision at the same
 *     index.
 *
 * Truncated responses (PDP stops emitting decisions after the trigger) and
 * full-length responses (PDP emits the trigger value for every later index)
 * are both valid; a response that re-evaluates an entry past the trigger to
 * its natural decision fails.
 */
public class EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_evaluations_endpoint_expected_response", "authzen_evaluations_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonArray expected = env.getElementFromObject("authzen_evaluations_endpoint_expected_response", "evaluations").getAsJsonArray();
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

		JsonArray response = env.getElementFromObject("authzen_evaluations_endpoint_response", "evaluations").getAsJsonArray();
		if (response.size() <= triggerPosition) {
			throw error("Response stopped before the short-circuit trigger position",
				args("response_size", response.size(), "trigger_position", triggerPosition, "trigger_decision", triggerDecision));
		}
		if (response.size() > expected.size()) {
			throw error("Response has more evaluations than expected",
				args("expected_size", expected.size(), "actual_size", response.size()));
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
				throw error("Response decision at position " + i + " does not match expected — short-circuit semantic not honoured",
					args("position", i, "expected", expectedDecision, "actual", actualDecision, "trigger_decision", triggerDecision));
			}
		}
		logSuccess("Response decisions match expected short-circuit pattern",
			args("trigger_decision", triggerDecision, "trigger_position", triggerPosition, "response_size", response.size()));
		return env;
	}
}
