package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class StoreOriginalClient2Configuration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "original_client_config")
	public Environment evaluate(Environment in) {

		JsonElement dynamicClientRegistrationTemplate = in.getElementFromObject("config", "client2");
		if (dynamicClientRegistrationTemplate == null || !dynamicClientRegistrationTemplate.isJsonObject()) {
			throw error("Definition for client not present in supplied configuration");
		} else {
			// we've got a client object, put it in the environment
			in.putObject("original_client_config", dynamicClientRegistrationTemplate.getAsJsonObject());

			log("Found a original_client_config object", dynamicClientRegistrationTemplate.getAsJsonObject());
			return in;
		}
	}
}
