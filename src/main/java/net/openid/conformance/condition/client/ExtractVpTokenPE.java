package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractVpTokenPE extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	@PostEnvironment(strings = {"credential"})
	public Environment evaluate(Environment env) {
		String vpToken = env.getString("authorization_endpoint_response", "vp_token");

		if (Strings.isNullOrEmpty(vpToken)) {
			throw error("Missing vp_token parameter");
		}

		logSuccess("vp_token parsed", args("credential", vpToken));
		env.putString("credential", vpToken);

		return env;
	}

}
