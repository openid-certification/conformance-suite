package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientRegistrationOptionsToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dynamic_registration_request", "config"})
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonElement dynamicRegistrationOptions = env.getElementFromObject("config", "client.registration_options");
		if(null != dynamicRegistrationOptions) {
			JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");
			JsonObject regOptions = dynamicRegistrationOptions.getAsJsonObject();
			for(String key : regOptions.keySet()) {
				dynamicRegistrationRequest.add(key, regOptions.get(key));
			}
		}
		log("Added client registration options from config to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationOptions));
		return env;
	}
}
