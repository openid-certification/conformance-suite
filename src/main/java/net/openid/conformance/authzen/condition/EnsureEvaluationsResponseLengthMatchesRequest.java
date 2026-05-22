package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Spec section 7.2-1 requires the response `evaluations` array to have the same
 * length as the request `evaluations` array. When the request omits `evaluations`
 * or sends an empty array (backward-compat shape) this check is skipped.
 */
public class EnsureEvaluationsResponseLengthMatchesRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request", "authzen_evaluations_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		JsonElement requestEvalsElem = request.get("evaluations");
		if (requestEvalsElem == null || !requestEvalsElem.isJsonArray()) {
			log("Request has no `evaluations` array — skipping length-match check");
			return env;
		}
		JsonArray requestEvals = requestEvalsElem.getAsJsonArray();
		if (requestEvals.isEmpty()) {
			log("Request `evaluations` array is empty — skipping length-match check");
			return env;
		}

		JsonObject response = env.getObject("authzen_evaluations_endpoint_response");
		JsonElement responseEvalsElem = response.get("evaluations");
		if (responseEvalsElem == null || !responseEvalsElem.isJsonArray()) {
			throw error("Response is missing `evaluations` array while request supplied one", args("response", response));
		}
		JsonArray responseEvals = responseEvalsElem.getAsJsonArray();
		if (responseEvals.size() != requestEvals.size()) {
			throw error("Response `evaluations` length does not match request length",
				args("request_length", requestEvals.size(),
					"response_length", responseEvals.size(),
					"response", response));
		}
		logSuccess("Response `evaluations` length matches request length",
			args("length", requestEvals.size()));
		return env;
	}
}
