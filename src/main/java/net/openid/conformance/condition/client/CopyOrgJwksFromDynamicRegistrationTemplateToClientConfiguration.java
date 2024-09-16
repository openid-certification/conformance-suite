package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CopyOrgJwksFromDynamicRegistrationTemplateToClientConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "original_client_config")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String configName = "org_jwks";
		JsonElement specifiedConfigValue = env.getElementFromObject("original_client_config", configName);

		if (specifiedConfigValue == null) {
			log("No %s in original_client_config".formatted(configName));
			return env;
		}

		JsonObject client = env.getObject("client");
		client.add(configName, specifiedConfigValue);
		env.putObject("client", client);

		log("Copied %s from original_client_config to client configuration".formatted(configName), args("client", client));

		return env;
	}

}
