package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilAddCPFAndCPNJToUserInfoClaims extends AbstractFAPIBrazilAddCPFAndCPNJToGeneratedClaims {

	@Override
	@PreEnvironment(required = { "user_info_endpoint_response", "authorization_request_object" })
	public Environment evaluate(Environment env) {
		if(addClaims(env, "user_info_endpoint_response", "user_info")) {
			logSuccess("Added claims to user_info claims", args("user_info", env.getObject("user_info_endpoint_response")));
		}
		return env;
	}

}
