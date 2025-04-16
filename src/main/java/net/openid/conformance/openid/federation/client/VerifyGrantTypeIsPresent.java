package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VerifyGrantTypeIsPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");
		if (grantType == null) {
			throw error("Token endpoint body does not contain the mandatory 'grant_type' parameter");
		}

		logSuccess("Grant type is present", args("grant_type", grantType));

		return env;
	}
}
