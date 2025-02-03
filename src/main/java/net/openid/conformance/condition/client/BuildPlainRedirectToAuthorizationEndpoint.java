package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

public class BuildPlainRedirectToAuthorizationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "server" })
	@PostEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("authorization_endpoint_request")) {
			throw error("Couldn't find authorization endpoint request");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String authorizationEndpoint = env.getString("authorization_endpoint") != null ? env.getString("authorization_endpoint") : env.getString("server", "authorization_endpoint");

		if (Strings.isNullOrEmpty(authorizationEndpoint)) {
			throw error("Couldn't find authorization endpoint");
		}

		// send a front channel request to start things off
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(authorizationEndpoint);

		for (String key : authorizationEndpointRequest.keySet()) {
			JsonElement element = authorizationEndpointRequest.get(key);

			// for nonce, state, client_id, redirect_uri, etc.
			if (element.isJsonPrimitive()) {
				if (key.equals("max_age")) {
					builder.queryParam(key, OIDFJSON.getNumber(element));
				} else {
					builder.queryParam(key, OIDFJSON.getString(element));
				}
			}
			// for claims
			else {

				builder.queryParam(key, element.toString());
			}

		}

		String redirectTo = builder.toUriString();

		logSuccess("Sending to authorization endpoint", args("redirect_to_authorization_endpoint", redirectTo, "auth_request", authorizationEndpointRequest));

		env.putString("redirect_to_authorization_endpoint", redirectTo);

		return env;
	}

}
