package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates the PDP's evaluations response for short-circuit semantics
 * (`deny_on_first_deny` / `permit_on_first_permit`, Section 7.1.2.1).
 *
 * The trigger decision comes from the `evaluations_semantic` in the request the
 * suite sent: `false` for `deny_on_first_deny`, `true` for
 * `permit_on_first_permit`. The expected response lists, in request order, the
 * decision the PDP reaches for each evaluation; it may stop at the first
 * trigger decision, as the spec's 7.1.2.1.1 examples do.
 *
 * This condition requires the PDP to truncate the response at the trigger
 * position so that the short-circuit semantic is observable on the wire.
 * Section 7.1.2.1 describes the operations semantically (analogous to
 * boolean `&&` / `||`) without prescribing the wire shape; this test profile
 * pins truncation as the conformant behavior. The actual response is accepted
 * only when:
 *   - its length is the position of the first trigger decision in the
 *     expected list plus one, or the length of the request's `evaluations`
 *     when no evaluation reaches the trigger decision; and
 *   - every decision matches the expected decision at the same index.
 *
 * A full-length response (the PDP did not truncate) fails even if the trailing
 * entries carry the trigger value; the test asserts the short-circuit behavior
 * is observable on the wire.
 */
public class EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request", "authzen_evaluations_endpoint_expected_response", "authzen_evaluations_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		boolean triggerDecision = readTriggerDecision(request);
		JsonElement requestEvalsElem = request.get("evaluations");
		if (requestEvalsElem == null || !requestEvalsElem.isJsonArray() || requestEvalsElem.getAsJsonArray().isEmpty()) {
			throw error("The batch request the suite sent has no non-empty `evaluations` array, so the short-circuit position cannot be checked",
				args("request", request));
		}
		int requestSize = requestEvalsElem.getAsJsonArray().size();

		JsonArray expected = readEvaluationsArray(env, "authzen_evaluations_endpoint_expected_response", "expected");
		if (expected.size() > requestSize) {
			throw error("The expected evaluations list is longer than the request's `evaluations`",
				args("expected_size", expected.size(), "request_size", requestSize, "expected", expected));
		}

		int triggerPosition = -1;
		for (int i = 0; i < expected.size(); i++) {
			JsonElement elem = expected.get(i);
			// The trigger position depends on every expected decision, so unlike
			// EnsureAuthzenEvaluationsResponseValsMatchExpectedVals this accepts no entry without one.
			if (!elem.isJsonObject() || !elem.getAsJsonObject().has("decision")) {
				throw error("An expected evaluations response element has no decision; a short-circuit expected response must give every decision up to the trigger",
					args("position", i, "obj", elem));
			}
			if (OIDFJSON.getBoolean(elem.getAsJsonObject().get("decision")) == triggerDecision) {
				triggerPosition = i;
				break;
			}
		}
		int expectedResponseSize;
		if (triggerPosition != -1) {
			expectedResponseSize = triggerPosition + 1;
		} else if (expected.size() == requestSize) {
			expectedResponseSize = requestSize;
		} else {
			throw error("The expected evaluations list neither reaches the trigger decision nor covers every evaluation in the request",
				args("trigger_decision", triggerDecision, "expected_size", expected.size(), "request_size", requestSize, "expected", expected));
		}

		JsonArray response = readEvaluationsArray(env, "authzen_evaluations_endpoint_response", "actual");
		if (response.size() != expectedResponseSize) {
			if (triggerPosition != -1) {
				throw error("Response did not truncate at the short-circuit trigger position; expected the response array length to be trigger_position + 1",
					args("response_size", response.size(), "expected_size", expectedResponseSize, "trigger_position", triggerPosition, "trigger_decision", triggerDecision));
			}
			throw error("No evaluation reaches the short-circuit trigger decision, so the response must have one decision for each evaluation in the request",
				args("response_size", response.size(), "expected_size", expectedResponseSize, "trigger_decision", triggerDecision));
		}
		for (int i = 0; i < response.size(); i++) {
			JsonElement actualElem = response.get(i);
			if (!actualElem.isJsonObject() || !actualElem.getAsJsonObject().has("decision")) {
				throw error("An actual evaluations response element is not valid",
					args("position", i, "obj", actualElem));
			}
			boolean expectedDecision = OIDFJSON.getBoolean(expected.get(i).getAsJsonObject().get("decision"));
			JsonElement actualDecisionElem = actualElem.getAsJsonObject().get("decision");
			if (!actualDecisionElem.isJsonPrimitive() || !actualDecisionElem.getAsJsonPrimitive().isBoolean()) {
				throw error("Response decision at position " + i + " is not a boolean",
					args("position", i, "decision", actualDecisionElem));
			}
			boolean actualDecision = OIDFJSON.getBoolean(actualDecisionElem);
			if (expectedDecision != actualDecision) {
				throw error("Response decision at position " + i + " does not match expected",
					args("position", i, "expected", expectedDecision, "actual", actualDecision, "trigger_decision", triggerDecision));
			}
		}
		if (triggerPosition != -1) {
			logSuccess("Response truncated at the short-circuit trigger and decisions match expected",
				args("trigger_decision", triggerDecision, "trigger_position", triggerPosition, "response_size", response.size()));
		} else {
			logSuccess("No evaluation reaches the short-circuit trigger decision, and the response has every expected decision",
				args("trigger_decision", triggerDecision, "response_size", response.size()));
		}
		return env;
	}

	private boolean readTriggerDecision(JsonObject request) {
		JsonElement options = request.get("options");
		JsonElement semantic = options != null && options.isJsonObject() ? options.getAsJsonObject().get("evaluations_semantic") : null;
		if (semantic != null && semantic.isJsonPrimitive() && semantic.getAsJsonPrimitive().isString()) {
			String value = OIDFJSON.getString(semantic);
			if ("deny_on_first_deny".equals(value)) {
				return false;
			}
			if ("permit_on_first_permit".equals(value)) {
				return true;
			}
		}
		throw error("The request the suite sent has no short-circuit `evaluations_semantic` (`deny_on_first_deny` or `permit_on_first_permit`) in its `options`",
			args("options", options));
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
