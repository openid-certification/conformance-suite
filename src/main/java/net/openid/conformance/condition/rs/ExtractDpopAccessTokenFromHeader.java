package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractDpopAccessTokenFromHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "incoming_dpop_access_token")
	public Environment evaluate(Environment env) {
		env.removeObject("incoming_dpop_access_token");

		String authHeader = env.getString("incoming_request", "headers.authorization");
		if (!Strings.isNullOrEmpty(authHeader)) {
			if (authHeader.toLowerCase().startsWith("dpop ")) {
				String tokenFromHeader = authHeader.substring("dpop ".length());
				if(!Strings.isNullOrEmpty(tokenFromHeader)) {
					env.putString("incoming_dpop_access_token", tokenFromHeader);
					logSuccess("Found DPoP access token", args("DPoP token", tokenFromHeader));
					return env;
				}
			}
		}
		throw error("Couldn't find DPoP access token", args("Header", authHeader));
	}

}
