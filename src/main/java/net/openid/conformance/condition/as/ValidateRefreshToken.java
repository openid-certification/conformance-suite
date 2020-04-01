package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * https://tools.ietf.org/html/rfc6749#section-6
 * The authorization server MUST:
 *  ...
 *    o  validate the refresh token.
 */
public class ValidateRefreshToken extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "refresh_token", required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String refreshTokenInEnv = env.getString("refresh_token");
		String refreshTokenInRequest = env.getString("token_endpoint_request", "body_form_params.refresh_token");
		if(refreshTokenInRequest==null) {
			throw error("Request does not contain a refresh_token parameter",
						args("form_parameters",
							env.getElementFromObject("token_endpoint_request", "body_form_params")));
		}

		if(!refreshTokenInRequest.equals(refreshTokenInEnv)) {
			throw error("Invalid refresh_token parameter.",
						args("expected", refreshTokenInEnv, "actual", refreshTokenInRequest));
		}
		logSuccess("refresh_token parameter matches the expected value.", args("refresh_token", refreshTokenInRequest));
		return env;
	}

}
