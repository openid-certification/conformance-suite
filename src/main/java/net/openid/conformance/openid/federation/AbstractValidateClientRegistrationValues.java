package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openid.federation.client.ClientRegistration;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.Set;

public abstract class AbstractValidateClientRegistrationValues extends AbstractCondition {

	abstract Set<String> getValidClientRegistrationValues();

	protected void validateClientRegistrationValues(JsonElement clientRegistrationValuesElement, String propertyName) {
		if (clientRegistrationValuesElement == null || !clientRegistrationValuesElement.isJsonArray()) {
			throw error("client_registration_types must be an array", args("client_registration_types", clientRegistrationValuesElement));
		}
		JsonArray clientRegistrationValues = clientRegistrationValuesElement.getAsJsonArray();
		for (JsonElement clientRegistrationValueElement : clientRegistrationValues) {
			String clientRegistrationValue = OIDFJSON.getString(clientRegistrationValueElement);
			if (!getValidClientRegistrationValues().contains(clientRegistrationValue)) {
				throw error("""
					Value %s found that the conformance suite doesn't recognize. \
					This may indicate that a specification has been wrongly implemented, \
					or that the server implements an extension the conformance suite is currently aware of.
					""".formatted(clientRegistrationValue), args(propertyName, clientRegistrationValues));
			}
		}

		logSuccess("The metadata contains valid %s".formatted(propertyName),
			args(propertyName, clientRegistrationValues));
	}

	protected void validateClientRegistrationValue(JsonElement clientRegistrationValuesElement, String propertyName, ClientRegistration clientRegistrationType) {
		if (clientRegistrationValuesElement == null || !clientRegistrationValuesElement.isJsonArray()) {
			throw error("client_registration_types must be an array", args("client_registration_types", clientRegistrationValuesElement));
		}
		List<String> clientRegistrationValues = OIDFJSON.convertJsonArrayToList(clientRegistrationValuesElement.getAsJsonArray());
		if (!clientRegistrationValues.contains(clientRegistrationType.toString())) {
			throw error("Client registration type %s not found".formatted(clientRegistrationType), args(propertyName, clientRegistrationValues));
		}

		logSuccess("Client registration type %s found".formatted(propertyName), args(propertyName, clientRegistrationValues));
	}
}
