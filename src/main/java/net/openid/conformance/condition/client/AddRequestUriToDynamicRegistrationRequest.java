package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddRequestUriToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "request_uri" } )
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		String requestUri = env.getString("request_uri", "fullUrl");
		if (Strings.isNullOrEmpty(requestUri)) {
			throw error("No request_uri found in environment; this is likely a bug in the test module");
		}

		JsonArray requestUris = new JsonArray();
		requestUris.add(requestUri);
		dynamicRegistrationRequest.add("request_uris", requestUris);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added request_uris array to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}
}
