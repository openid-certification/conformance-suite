package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureAuthorizationParametersMatchRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"http_request_params_source"}, required = {"authorization_endpoint_http_request", "authorization_request_object"})
	public Environment evaluate(Environment env) {
		String paramsSource = env.getString("http_request_params_source");
		JsonObject params = env.getElementFromObject("authorization_endpoint_http_request", paramsSource).getAsJsonObject();

		for (String claim : params.keySet()) {
			// make sure every claim in the "params" input is included in the request object

			// don't count the "request" param itself
			if (claim.equals("request")) {
				continue;
			}

			String requestObj = env.getString("authorization_request_object", "claims." + claim);
			String param = env.getString("authorization_endpoint_http_request", paramsSource + "." + claim);

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
