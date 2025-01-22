package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientRegistrationTypesSupportedValues extends AbstractValidateClientRegistrationValues {

	@Override
	@PreEnvironment(required = { "openid_provider_metadata" } )
	public Environment evaluate(Environment env) {
		String propertyName = "client_registration_types_supported";
		JsonElement clientRegistrationTypesElement = env.getElementFromObject("openid_provider_metadata", propertyName);
		validateClientRegistrationValues(clientRegistrationTypesElement, propertyName);
		return env;
	}

}
