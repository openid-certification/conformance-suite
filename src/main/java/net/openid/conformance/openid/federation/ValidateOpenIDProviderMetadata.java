package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class ValidateOpenIDProviderMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "openid_provider_metadata" } )
	public Environment evaluate(Environment env) {

		JsonElement clientRegistrationTypesSupportedElement = env.getElementFromObject("openid_provider_metadata", "client_registration_types_supported");

		if (!clientRegistrationTypesSupportedElement.isJsonArray()) {
			throw error("client_registration_types_supported must be an array of strings", args("client_registration_types_supported", clientRegistrationTypesSupportedElement));
		}

		Set<String> validValues = ImmutableSet.of("automatic", "explicit");
		JsonArray clientRegistrationTypesSupported = clientRegistrationTypesSupportedElement.getAsJsonArray();
		for (JsonElement element : clientRegistrationTypesSupported) {
			if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
				String stringValue = OIDFJSON.getString(element);
				if (!validValues.contains(stringValue)) {
					throw error("Invalid value found in client_registration_types_supported", args("valid", validValues, "actual", stringValue));
				}
			} else {
				throw error("client_registration_types_supported must be an array of strings", args("client_registration_types_supported", clientRegistrationTypesSupported));
			}
		}

		logSuccess("openid_provider metadata contains client_registration_types_supported", args("client_registration_types_supported", clientRegistrationTypesSupported));
		return env;
	}
}
