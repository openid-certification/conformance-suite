package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureAuthorizationParametersMatchRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params", "authorization_request_object"})
	public Environment evaluate(Environment env) {
		JsonObject params = env.getObject("authorization_endpoint_http_request_params");

		for (String claim : params.keySet()) {
			// make sure every claim in the "params" input is included in the request object

			// skip "request" and "request_uri"
			if (claim.equals("request") || claim.equals("request_uri")) {
				continue;
			}

			String requestObj = env.getString("authorization_request_object", "claims." + claim);
			String param = env.getString("authorization_endpoint_http_request_params", claim);

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
