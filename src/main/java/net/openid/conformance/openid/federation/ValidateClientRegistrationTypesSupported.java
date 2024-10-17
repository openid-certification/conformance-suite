package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientRegistrationTypesSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "openid_provider_metadata" } )
	public Environment evaluate(Environment env) {

		JsonElement clientRegistrationTypesSupportedElement = env.getElementFromObject("openid_provider_metadata", "client_registration_types_supported");

		if (clientRegistrationTypesSupportedElement == null) {
			throw error("client_registration_types_supported is a required parameter",
				args("client_registration_types_supported", clientRegistrationTypesSupportedElement));
		}

		if (!clientRegistrationTypesSupportedElement.isJsonArray()) {
			throw error("client_registration_types_supported must be an array of strings",
				args("client_registration_types_supported", clientRegistrationTypesSupportedElement));
		}

		JsonArray clientRegistrationTypesSupported = clientRegistrationTypesSupportedElement.getAsJsonArray();
		for (JsonElement element : clientRegistrationTypesSupported) {
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
				throw error("client_registration_types_supported must be an array of strings",
					args("client_registration_types_supported", clientRegistrationTypesSupported));
			}
		}

		logSuccess("openid_provider metadata contains client_registration_types_supported",
			args("client_registration_types_supported", clientRegistrationTypesSupported));
		return env;
	}
}
