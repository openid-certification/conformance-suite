package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * OIDCC 6.2:
 *   The contents of the resource referenced by the URL MUST be a Request Object.
 *   The scheme used in the request_uri value MUST be https, unless the target
 *   Request Object is signed in a way that is verifiable by the Authorization Server.
 *
 * We first fetch the request object from request_uri even if it's not https
 * but we throw an error if request object is not signed and request_uri was not https
 */
public class EnsureRequestUriIsHttpsOrRequestObjectIsSigned extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_request_object", "authorization_endpoint_http_request_params" })
	public Environment evaluate(Environment env) {
		String requestUri = env.getString("authorization_endpoint_http_request_params", "request_uri");
		String alg = env.getString("authorization_request_object", "header.alg");

		if(requestUri.toLowerCase().startsWith("https://")) {
			logSuccess("request_uri is a https url", args("request_uri", requestUri));
			return env;
		} else {
			//OIDCC-6.1:
			//   The Request Object MAY be signed or unsigned (plaintext). When it is plaintext,
			//   this is indicated by use of the none algorithm [JWA] in the JOSE Header.

			if (!"none".equals(alg)) {
				logSuccess("request_uri is not a https url but the request object is signed", args("request_uri", requestUri, "alg", alg));
				return env;
			} else {
				throw error("The scheme used in the request_uri value MUST be https, as the target Request Object is not signed",
					args("request_uri", requestUri, "alg", alg));
			}
		}
	}

}
