package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Per Section 7.1: when an Access Evaluations request omits the {@code evaluations}
 * member or sends it as an empty array, the PDP MUST behave in a
 * backwards-compatible manner with the single Access Evaluation API — it returns
 * a single Access Evaluation response with a top-level {@code decision} and MUST
 * NOT return an {@code evaluations} array.
 *
 * <p>This condition fails when the response carries an {@code evaluations} array
 * (for example a PDP that returns both a top-level {@code decision} and an
 * {@code evaluations} array, or only the array form).
 */
public class EnsureBackwardCompatResponseHasNoEvaluationsArray extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_decision")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("authzen_api_endpoint_decision");
		if (response.has("evaluations")) {
			throw error("Backward-compatible Evaluations response (the request omitted `evaluations` or sent it empty) MUST behave like the single Access Evaluation API and return a top-level `decision` without an `evaluations` array",
				args("response", response));
		}
		logSuccess("Backward-compatible Evaluations response correctly returned a single decision without an `evaluations` array");
		return env;
	}
}
