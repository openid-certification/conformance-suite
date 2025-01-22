package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientRegistrationTypesValues extends AbstractValidateClientRegistrationValues {

	@Override
	@PreEnvironment(required = { "client" } )
	public Environment evaluate(Environment env) {
		String propertyName = "client_registration_types";
		JsonElement clientRegistrationTypesElement = env.getElementFromObject("client", propertyName);
		validateClientRegistrationValues(clientRegistrationTypesElement, propertyName);
		return env;
	}

}
