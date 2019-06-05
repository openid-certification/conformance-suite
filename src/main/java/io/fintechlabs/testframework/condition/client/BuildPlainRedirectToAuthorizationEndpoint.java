package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class BuildPlainRedirectToAuthorizationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "server" })
	@PostEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("authorization_endpoint_request")) {
			throw error("Couldn't find authorization endpoint request");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String authorizationEndpoint = env.getString("server", "authorization_endpoint");

		if (Strings.isNullOrEmpty(authorizationEndpoint)) {
			throw error("Couldn't find authorization endpoint");
		}

		// send a front channel request to start things off
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authorizationEndpoint);

		for (String key : authorizationEndpointRequest.keySet()) {
			JsonElement element = authorizationEndpointRequest.get(key);

			// for nonce, state, client_id, redirect_uri, etc.
			if (element.isJsonPrimitive()) {

				builder.queryParam(key, OIDFJSON.getString(element));
			}
			// for claims
			else {

				builder.queryParam(key, element.toString());
			}

		}

		String redirectTo = builder.toUriString();

		logSuccess("Sending to authorization endpoint", args("redirect_to_authorization_endpoint", redirectTo));

		env.putString("redirect_to_authorization_endpoint", redirectTo);

		return env;
	}

}
