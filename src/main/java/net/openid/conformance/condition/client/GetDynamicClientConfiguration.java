package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GetDynamicClientConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "dynamic_client_registration_template")
	public Environment evaluate(Environment in) {

		if (!in.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}

		JsonElement dynamicClientRegistrationTemplate = in.getElementFromObject("config", "client");
		if (dynamicClientRegistrationTemplate == null || !dynamicClientRegistrationTemplate.isJsonObject()) {
			// we don't actually need anything; if no client_name is given we'll pick one
			in.putObject("dynamic_client_registration_template", new JsonObject());
			logSuccess("No client details on configuration, created an empty dynamic_client_registration_template object.");
		} else {
			// we've got a client object, put it in the environment
			in.putObject("dynamic_client_registration_template", dynamicClientRegistrationTemplate.getAsJsonObject());

			// pull out the client name and put it in the root environment for easy access (if there is one)
			String clientName = in.getString("dynamic_client_registration_template", "client_name");
			if (!Strings.isNullOrEmpty(clientName)) {
				in.putString("client_name", in.getString("dynamic_client_registration_template", "client_name"));
			}
			logSuccess("Created dynamic_client_registration_template object from the client configuration.", dynamicClientRegistrationTemplate.getAsJsonObject());
		}
		return in;
	}
}
