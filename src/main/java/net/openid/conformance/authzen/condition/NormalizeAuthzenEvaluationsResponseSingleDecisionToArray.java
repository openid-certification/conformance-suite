package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Per spec section 7.1-2, when an Evaluations request omits the `evaluations`
 * member the PDP MAY return either the single-decision form
 * (`{"decision": <bool>}`) or the one-element evaluations array form
 * (`{"evaluations": [{"decision": <bool>}]}`).
 *
 * This condition normalizes the single-decision form into the array form so
 * the downstream validators ({@code EnsureValidEvaluationsResponse} and the
 * val-match conditions) accept either shape without modification.
 */
public class NormalizeAuthzenEvaluationsResponseSingleDecisionToArray extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_evaluations_endpoint_response")
	@PostEnvironment(required = "authzen_evaluations_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("authzen_evaluations_endpoint_response");
		if (response.has("evaluations")) {
			log("Response already has `evaluations`; no normalization needed", args("response", response));
			return env;
		}
		if (!response.has("decision")) {
			throw error("Backward-compat response has neither `evaluations` nor top-level `decision`",
				args("response", response));
		}
		JsonObject normalized = new JsonObject();
		JsonArray evaluations = new JsonArray();
		JsonObject entry = new JsonObject();
		entry.add("decision", response.get("decision"));
		evaluations.add(entry);
		normalized.add("evaluations", evaluations);
		env.putObject("authzen_evaluations_endpoint_response", normalized);
		logSuccess("Normalised single-decision response to one-element evaluations array",
			args("normalized", normalized));
		return env;
	}
}
