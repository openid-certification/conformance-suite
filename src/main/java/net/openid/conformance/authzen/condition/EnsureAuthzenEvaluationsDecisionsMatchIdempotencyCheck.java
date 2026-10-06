package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Asserts that the current Evaluations API response returns the same decisions, in the
 * same order, as the body captured by {@link CaptureAuthzenResponseBodyForIdempotencyCheck}
 * on the first iteration of an idempotency test loop.
 *
 * <p>Only {@code evaluations[i].decision} is compared. Each evaluation's {@code context}
 * may carry environmental information that differs between calls (Section 5.5.1).
 */
public class EnsureAuthzenEvaluationsDecisionsMatchIdempotencyCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response", strings = "authzen_idempotency_first_response_body")
	public Environment evaluate(Environment env) {
		JsonObject expected = JsonParser.parseString(env.getString("authzen_idempotency_first_response_body")).getAsJsonObject();
		JsonElement actualElem = env.getElementFromObject("authzen_api_endpoint_response", "body_json");
		if (actualElem == null) {
			throw error("Current iteration has no JSON response body");
		}
		if (!actualElem.isJsonObject()) {
			throw error("Current iteration response body is not a JSON object",
				args("body_json", actualElem));
		}

		JsonArray expectedDecisions = getDecisions(expected, "first_iteration_body");
		JsonArray actualDecisions = getDecisions(actualElem.getAsJsonObject(), "current_iteration_body");
		if (!expectedDecisions.equals(actualDecisions)) {
			throw error("Decisions changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_decisions", expectedDecisions, "current_iteration_decisions", actualDecisions));
		}
		logSuccess("Decisions matched first iteration", args("decisions", actualDecisions));
		return env;
	}

	private JsonArray getDecisions(JsonObject body, String argName) {
		JsonElement evaluations = body.get("evaluations");
		if (evaluations == null || !evaluations.isJsonArray()) {
			throw error("Evaluations response has no `evaluations` array", args(argName, body));
		}
		JsonArray decisions = new JsonArray();
		for (JsonElement evaluation : evaluations.getAsJsonArray()) {
			JsonElement decision = evaluation.isJsonObject() ? evaluation.getAsJsonObject().get("decision") : null;
			decisions.add(decision == null ? JsonNull.INSTANCE : decision);
		}
		return decisions;
	}
}
