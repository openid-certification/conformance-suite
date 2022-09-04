package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateRefreshTokenNotRotated extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"token_endpoint_response"}, strings = "refresh_token")
	public Environment evaluate(Environment env) {
		String newRefreshToken = env.getString("token_endpoint_response", "refresh_token");
		if(newRefreshToken==null) {
			logSuccess("Token endpoint response does not contain a refresh token");
			return env;
		}
		String currentRefreshToken = env.getString("refresh_token");
		if (currentRefreshToken.equals(newRefreshToken)) {
			logSuccess("Token endpoint response refresh token is the same as the existing refresh token");
			return env;
		}

		throw error("Refresh token is different (has been rotated) in the token endpoint response. Rotating the refresh token does not provide any security benefits but can introduce new problems. See https://bitbucket.org/openid/fapi/issues/456/", args(
			"new_refresh_token", newRefreshToken,
			"old_refresh_token", currentRefreshToken
		));
	}
}
