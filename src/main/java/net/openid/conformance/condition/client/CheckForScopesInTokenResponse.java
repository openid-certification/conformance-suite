package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckForScopesInTokenResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "scope"))) {
			logSuccess("Found scopes returned with access token",
				args("scope", env.getString("token_endpoint_response", "scope")));
			return env;
		} else {
			throw error("Couldn't find scope");
		}
	}

}
