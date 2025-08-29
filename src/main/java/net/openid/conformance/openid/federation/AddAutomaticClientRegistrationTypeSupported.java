package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAutomaticClientRegistrationTypeSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement clientRegistrationTypesSupportedElement = env.getElementFromObject("server", "metadata.openid_provider.client_registration_types_supported");
		JsonArray clientRegistrationTypesSupported = clientRegistrationTypesSupportedElement.getAsJsonArray();

		clientRegistrationTypesSupported.add("automatic");

		logSuccess("Added automatic client_registration_types_supported to openid_provider configuration", args("client_registration_types_supported", clientRegistrationTypesSupported));

		return env;
	}

}
