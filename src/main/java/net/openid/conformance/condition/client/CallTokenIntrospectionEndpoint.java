package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CallTokenIntrospectionEndpoint extends AbstractCallOAuthEndpoint {

	public static final String RESPONSE_KEY = "introspection_endpoint_response";

	@Override
	@PreEnvironment(required = { "server", "introspection_endpoint_request_form_parameters" })
	@PostEnvironment(required = RESPONSE_KEY)
	public Environment evaluate(Environment env) {

		// set by AddMTLSEndpointAliasesToEnvironment when the profile uses MTLS endpoint variants
		String introspectionEndpoint = env.getString("introspection_endpoint");
		if (Strings.isNullOrEmpty(introspectionEndpoint)) {
			introspectionEndpoint = env.getString("server", "introspection_endpoint");
		}
		if (Strings.isNullOrEmpty(introspectionEndpoint)) {
			throw error("The server configuration does not contain an introspection_endpoint");
		}

		return callOAuthEndpoint(env, null, "introspection_endpoint_request_form_parameters",
			"introspection_endpoint_request_headers", introspectionEndpoint, "introspection endpoint", RESPONSE_KEY);
	}

}
