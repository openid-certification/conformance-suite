package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractAddGrantTypeToDynamicRegistrationRequest extends AbstractCondition {
	protected void addGrantType(Environment env, String toAdd) {
		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");
		JsonArray grantTypes = new JsonArray();
		final String grant_types = "grant_types";
		if (dynamicRegistrationRequest.has(grant_types)) {
			grantTypes = dynamicRegistrationRequest.get(grant_types).getAsJsonArray();
		}
		grantTypes.add(toAdd);
		dynamicRegistrationRequest.add(grant_types,grantTypes);
		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added '"+toAdd+"' to '"+grant_types+"'", args(grant_types, grantTypes));
	}
}
