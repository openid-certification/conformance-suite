package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Per OID4VP 1.0 Final §5.10.1, the verifier's response to a request_uri POST MUST have
 * Content-Type {@code application/oauth-authz-req+jwt}.
 */
public class EnsureContentTypeApplicationOauthAuthzReqJwt extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {
		return checkContentType(env, "endpoint_response", "headers.", "application/oauth-authz-req+jwt");
	}
}
