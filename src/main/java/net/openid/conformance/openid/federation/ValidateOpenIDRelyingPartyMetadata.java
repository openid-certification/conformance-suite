package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class ValidateOpenIDRelyingPartyMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client" } )
	public Environment evaluate(Environment env) {

		JsonElement clientRegistrationTypesElement = env.getElementFromObject("client", "client_registration_types");

		if (!clientRegistrationTypesElement.isJsonArray()) {
			throw error("client_registration_types must be an array of strings", args("client_registration_types", clientRegistrationTypesElement));
		}

		Set<String> validValues = ImmutableSet.of("automatic", "explicit");
		JsonArray clientRegistrationTypes = clientRegistrationTypesElement.getAsJsonArray();
		for (JsonElement element : clientRegistrationTypes) {
			if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
				String stringValue = OIDFJSON.getString(element);
				if (!validValues.contains(stringValue)) {
					throw error("Invalid value found in client_registration_types", args("valid", validValues, "actual", stringValue));
				}
			} else {
				throw error("client_registration_types must be an array of strings", args("client_registration_types", clientRegistrationTypes));
			}
		}

		logSuccess("openid_relying_party metadata contains client_registration_types", args("client_registration_types", clientRegistrationTypes));
		return env;
	}
}
