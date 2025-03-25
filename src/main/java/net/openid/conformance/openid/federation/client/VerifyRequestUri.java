package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class VerifyRequestUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_http_request", "par_endpoint_response" })
	public Environment evaluate(Environment env) {

		String authorizeRequestRequestUri = env.getString("authorization_endpoint_http_request", "query_string_params.request_uri");
		String parResponseRequestUri = env.getString("par_endpoint_response", "request_uri");

		if (authorizeRequestRequestUri == null || parResponseRequestUri == null) {
		    throw error("Missing request_uri in authorization_endpoint_http_request or par_endpoint_response",
				args("authorize_request_uri", authorizeRequestRequestUri, "par_response_request_uri", parResponseRequestUri));
		}

		if (!Objects.equals(authorizeRequestRequestUri, parResponseRequestUri)) {
			throw error("Request_uri in authorization_endpoint_http_request does not match request_uri in par_endpoint_response",
                args("authorize_request_uri", authorizeRequestRequestUri, "par_response_request_uri", parResponseRequestUri));
		}

		logSuccess("Request_uri in authorization_endpoint_http_request matches request_uri in par_endpoint_response");
		return env;
	}
}
