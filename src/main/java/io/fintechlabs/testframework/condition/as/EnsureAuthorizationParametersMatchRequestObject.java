package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureAuthorizationParametersMatchRequestObject extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public EnsureAuthorizationParametersMatchRequestObject(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "authorization_request_object"})
	public Environment evaluate(Environment env) {

		JsonObject params = env.getElementFromObject("authorization_endpoint_request", "params").getAsJsonObject();

		for (String claim : params.keySet()) {
			// make sure every claim in the "params" input is included in the request object

			// don't count the "request" param itself
			if (claim.equals("request")) {
				continue;
			}

			String requestObj = env.getString("authorization_request_object", "claims." + claim);
			String param = env.getString("authorization_endpoint_request", "params." + claim);

			if (Strings.isNullOrEmpty(requestObj) || Strings.isNullOrEmpty(param)) {
				throw error("Couldn't find required claim", args("claim", claim, "request_obj", requestObj, "param", param));
			}

			// make sure the values match
			if (!requestObj.equals(param)) {
				throw error("Claim value didn't match", args("claim", claim, "request_obj", requestObj, "param", param));
			}

		}

		logSuccess("All claims in the query parameters exist in the request object", args("claims", params.keySet()));

		return env;

	}

}
