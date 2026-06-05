package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 7.2 RECOMMENDED — when the response includes an `evaluations` array,
 * the top-level `decision` field be omitted. This condition throws on violation
 * so the caller can decide whether to surface it as a WARNING or FAILURE.
 */
public class EnsureNoTopLevelDecisionWhenEvaluationsPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_evaluations_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("authzen_evaluations_endpoint_response");
		boolean hasEvaluations = response.has("evaluations") && response.get("evaluations").isJsonArray();
		boolean hasTopLevelDecision = response.has("decision");
		if (hasEvaluations && hasTopLevelDecision) {
			throw error("Response contains both `evaluations` and a top-level `decision`; Section 7.2 RECOMMENDED that the top-level decision be omitted when evaluations is returned.",
				args("response", response));
		}
		logSuccess("Response does not carry a redundant top-level `decision` alongside `evaluations`");
		return env;
	}
}
