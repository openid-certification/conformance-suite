package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.client.ClientRegistration;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public class ValidateClientRegistrationTypeExplicitSupported extends AbstractValidateClientRegistrationValues {

	@Override
	Set<String> getValidClientRegistrationValues() {
		return Set.of("explicit");
	}

	@Override
	@PreEnvironment(required = { "openid_provider_metadata" } )
	public Environment evaluate(Environment env) {
		String propertyName = "client_registration_types_supported";
		JsonElement clientRegistrationTypesElement = env.getElementFromObject("openid_provider_metadata", propertyName);
		validateClientRegistrationValue(clientRegistrationTypesElement, propertyName, ClientRegistration.AUTOMATIC);
		return env;
	}
}
