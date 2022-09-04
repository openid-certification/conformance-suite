package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckForRefreshTokenValue extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "refresh_token"))) {
			logSuccess("Found a refresh token",
				args("refresh_token", env.getString("token_endpoint_response", "refresh_token")));
			return env;
		} else {
			throw error("Couldn't find refresh token");
		}
	}

}
