package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateJWEHeaderCtyJson extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		String cty = env.getString("response_jwe", "jwe_header.cty");

		if (cty == null) {
			logSuccess("Optional JWE header cty is absent");
			return env;
		}

		if (!cty.equals("json")) {
			throw error("JWE header cty, if present, must be json", args("expected", "json", "actual", cty));
		}

		logSuccess("JWE header cty value is 'json'");
		return env;
	}

}
