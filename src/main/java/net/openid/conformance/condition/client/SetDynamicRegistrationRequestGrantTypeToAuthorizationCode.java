package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetDynamicRegistrationRequestGrantTypeToAuthorizationCode extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dynamic_registration_request"})
	public Environment evaluate(Environment env) {

		if (!env.containsObject("dynamic_registration_request")){
			throw error("No dynamic registration request object found");
		}
		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");
		JsonArray grantTypes = new JsonArray();
		grantTypes.add("authorization_code");
		dynamicRegistrationRequest.add("grant_types",grantTypes);
		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		return env;
	}
}
