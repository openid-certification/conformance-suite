package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureNoRefreshTokenInTokenResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		if (Strings.isNullOrEmpty(env.getString("token_endpoint_response", "refresh_token"))) {
			logSuccess("No refresh token found");
			return env;
		} else {
			throw error("Unexpected refresh tokeni found");
		}
	}

}
