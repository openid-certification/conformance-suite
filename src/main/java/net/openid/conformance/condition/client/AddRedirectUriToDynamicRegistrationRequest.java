package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddRedirectUriToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "redirect_uri")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		String redirectUri = env.getString("redirect_uri");
		if (Strings.isNullOrEmpty(redirectUri)) {
			throw error("No redirect_uri found");
		}

		JsonArray redirectUris = new JsonArray();
		redirectUris.add(redirectUri);
		dynamicRegistrationRequest.add("redirect_uris", redirectUris);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added redirect_uris array to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}
}
