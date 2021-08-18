package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CallUserInfoEndpointWithBearerToken extends AbstractCallProtectedResourceWithBearerToken {

	@Override
	@PreEnvironment(required = { "access_token", "server" })
	@PostEnvironment(required = "userinfo_endpoint_response_headers", strings = "userinfo_endpoint_response")
	public Environment evaluate(Environment env) {
		return callProtectedResource(env);
	}

	protected String getUri(Environment env) {

		String resourceUri = env.getString("server", "userinfo_endpoint");
		if (Strings.isNullOrEmpty(resourceUri)){
			throw error("\"userinfo_endpoint\" missing from server configuration");
		}

		return resourceUri;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {

		env.putInteger("userinfo_endpoint_response_code", OIDFJSON.getInt(responseCode.get("code")));
		env.putString("userinfo_endpoint_response", responseBody);
		env.putObject("userinfo_endpoint_response_headers", responseHeaders);

		logSuccess("Got a response from the userinfo endpoint", args("body", responseBody, "headers", responseHeaders, "status_code", responseCode));
		return env;
	}
}
