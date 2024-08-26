package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractVpToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	@PostEnvironment(strings = {"vp_token"})
	public Environment evaluate(Environment env) {
		String vpToken = env.getString("authorization_endpoint_response", "vp_token");

		if (Strings.isNullOrEmpty(vpToken)) {
			throw error("Missing vp_token parameter");
		}

		logSuccess("vp_token found", args("vp_token", vpToken));
		env.putString("vp_token", vpToken);

		return env;
	}

}
