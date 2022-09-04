package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ReorderGrantTypesInDynamicRegistrationRequest extends AbstractCondition {

	protected JsonArray reverseJsonArray(JsonArray array) {
		JsonArray reversed = new JsonArray();
		for (int i = array.size() - 1; i >= 0; i--) {
			JsonElement el = array.get(i);
			reversed.add(el);
		}

		return reversed;
	}

	@Override
	@PreEnvironment(required = {"dynamic_registration_request"})
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");
		final String grant_types = "grant_types";
		JsonArray grantTypes = dynamicRegistrationRequest.get(grant_types).getAsJsonArray();

		JsonArray reversedGrantType = reverseJsonArray(grantTypes);

		dynamicRegistrationRequest.add(grant_types, reversedGrantType);

		log("Reversed '"+grant_types+"' array in dynamic registration request", args(grant_types, grantTypes));
		return env;
	}

}
