package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * OID4VP 1.0 Final uses response_uri instead of redirect_uri.
 * The redirect_uri parameter MUST NOT be present in the authorization request.
 */
public class CheckNoRedirectUriInVpAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {
		String redirectUri = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "redirect_uri");
		if (redirectUri != null) {
			throw error("Authorization request contains 'redirect_uri' but OID4VP 1.0 Final requires 'response_uri' instead",
				args("redirect_uri", redirectUri));
		}
		logSuccess("Authorization request does not contain 'redirect_uri'");
		return env;
	}
}
