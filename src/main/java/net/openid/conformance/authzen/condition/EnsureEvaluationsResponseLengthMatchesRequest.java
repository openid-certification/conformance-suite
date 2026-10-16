package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 7.2 requires the response `evaluations` array to have the same
 * length as the request `evaluations` array. Requests without a non-empty
 * `evaluations` array (the backward-compatibility shape of Section 7.1) are
 * answered with a single decision and validated as such, so they never reach
 * this check; one that does fails, as the length cannot be compared.
 */
public class EnsureEvaluationsResponseLengthMatchesRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request", "authzen_evaluations_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		JsonElement requestEvalsElem = request.get("evaluations");
		if (requestEvalsElem == null || !requestEvalsElem.isJsonArray() || requestEvalsElem.getAsJsonArray().isEmpty()) {
			throw error("The batch request the suite sent has no non-empty `evaluations` array, so the length of the response's `evaluations` cannot be checked against it",
				args("request", request));
		}
		JsonArray requestEvals = requestEvalsElem.getAsJsonArray();

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
