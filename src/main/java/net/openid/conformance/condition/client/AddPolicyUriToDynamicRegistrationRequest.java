package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddPolicyUriToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "policy_uri")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		String policyUri = env.getString("policy_uri");

		dynamicRegistrationRequest.addProperty("policy_uri", policyUri);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added policy_uri to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}

}
