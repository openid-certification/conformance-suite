package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientRegistrationTypes extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client" } )
	public Environment evaluate(Environment env) {

		JsonElement clientRegistrationTypesElement = env.getElementFromObject("client", "client_registration_types");

		if (clientRegistrationTypesElement == null) {
			throw error("client_registration_types is a required parameter",
				args("client_registration_types", clientRegistrationTypesElement));
		}

		if (!clientRegistrationTypesElement.isJsonArray()) {
			throw error("client_registration_types must be an array of strings",
				args("client_registration_types", clientRegistrationTypesElement));
		}

		JsonArray clientRegistrationTypes = clientRegistrationTypesElement.getAsJsonArray();
		for (JsonElement element : clientRegistrationTypes) {
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
				throw error("client_registration_types must be an array of strings",
					args("client_registration_types", clientRegistrationTypes));
			}
		}

		logSuccess("openid_relying_party metadata contains client_registration_types",
			args("client_registration_types", clientRegistrationTypes));
		return env;
	}
}
