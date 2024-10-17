package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractBasicClaimsFromEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "federation_response_body")
	@PostEnvironment(strings = { "federation_response_iss", "federation_response_sub" })
	public Environment evaluate(Environment env) {

		String iss = env.getString("federation_response_body", "iss");
		String sub = env.getString("federation_response_body", "sub");
		Long iat = env.getLong("federation_response_body", "iat");
		Long exp = env.getLong("federation_response_body", "exp");

		env.putString("federation_response_iss", iss);
		env.putString("federation_response_sub", sub);
		env.putLong("federation_response_iat", iat);
		env.putLong("federation_response_exp", exp);

		logSuccess("Extracted basic claims from entity statement", args("iss", iss, "sub", sub, "iat", iat, "exp", exp));

		return env;
	}

}
