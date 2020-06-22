package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddPostLogoutRedirectUriToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "post_logout_redirect_uri")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		String postLogoutRedirectUri = env.getString("post_logout_redirect_uri");

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		JsonArray uris = new JsonArray();
		uris.add(postLogoutRedirectUri);

		dynamicRegistrationRequest.add("post_logout_redirect_uris", uris);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added post_logout_redirect_uris to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}

}
