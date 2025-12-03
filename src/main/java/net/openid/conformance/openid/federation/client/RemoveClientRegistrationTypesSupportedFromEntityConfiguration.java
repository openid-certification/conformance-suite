package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveClientRegistrationTypesSupportedFromEntityConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement clientRegistrationTypesSupportedElement = env.getElementFromObject("server", "metadata.openid_provider.client_registration_types_supported");
		if (clientRegistrationTypesSupportedElement != null) {
			env.removeElement("server", "metadata.openid_provider.client_registration_types_supported");
			logSuccess("Removed client_registration_types_supported from entity configuration");
		}

		return env;

	}

}
