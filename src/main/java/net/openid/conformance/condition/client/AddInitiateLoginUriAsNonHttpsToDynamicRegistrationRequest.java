package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInitiateLoginUriAsNonHttpsToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "initiate_login_uri")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		String initiateLoginUri = env.getString("initiate_login_uri");

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		initiateLoginUri = initiateLoginUri.replace("https://", "http://");

		dynamicRegistrationRequest.addProperty("initiate_login_uri", initiateLoginUri);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added non-https version of initiate_login_uri to dynamic registration request", args("initiate_login_uri", initiateLoginUri));

		return env;
	}

}
