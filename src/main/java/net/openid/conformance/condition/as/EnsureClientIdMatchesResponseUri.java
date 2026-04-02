package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * For the redirect_uri Client Identifier Scheme (OID4VP 1.0 Final section 5.9.2),
 * the client_id value MUST be set to the redirect_uri value. In the context of
 * OID4VP direct_post, this is the response_uri.
 */
public class EnsureClientIdMatchesResponseUri extends AbstractCondition {

	private static final String REDIRECT_URI_PREFIX = "redirect_uri:";

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {
		String clientId = env.getString(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY,
			CreateEffectiveAuthorizationRequestParameters.CLIENT_ID);
		String responseUri = env.getString(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "response_uri");

		if (clientId == null) {
			throw error("client_id is missing from the authorization request");
		}
		if (responseUri == null) {
			throw error("response_uri is missing from the authorization request");
		}

		// Per OID4VP 1.0 Final section 5.9.3, the client_id includes the scheme prefix
		// (e.g. "redirect_uri:https://example.com/callback"). Strip it before comparing.
		String origClientId = clientId;
		if (clientId.startsWith(REDIRECT_URI_PREFIX)) {
			origClientId = clientId.substring(REDIRECT_URI_PREFIX.length());
		}

		if (!origClientId.equals(responseUri)) {
			throw error("For redirect_uri client_id prefix, the client_id (without prefix) must equal response_uri",
				args("client_id", clientId, "orig_client_id", origClientId, "response_uri", responseUri));
		}

		logSuccess("client_id matches response_uri",
			args("client_id", clientId, "response_uri", responseUri));
		return env;
	}
}
