package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Warns if the verifier included request_uri_method inside the signed request object.
 *
 * Per OID4VP section 5.1, request_uri_method is a URL query parameter that the wallet
 * needs before fetching the request_uri. Including it inside the JWT request object is
 * pointless (the wallet can't read it until after fetching), and may indicate a
 * misunderstanding of the specification.
 */
public class WarnIfRequestUriMethodInRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {

		if (env.getElementFromObject("authorization_request_object", "claims.request_uri_method") != null) {
			throw error("request_uri_method should not be included inside the signed request object. "
				+ "It is a URL query parameter that the wallet needs before fetching the request_uri. "
				+ "See https://github.com/openid/OpenID4VCI/issues/733");
		}

		logSuccess("request_uri_method is not present in the request object");
		return env;
	}
}
