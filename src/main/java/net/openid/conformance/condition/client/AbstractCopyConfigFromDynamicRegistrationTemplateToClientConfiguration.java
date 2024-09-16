package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractCopyConfigFromDynamicRegistrationTemplateToClientConfiguration extends AbstractCondition {

	protected abstract String getExpectedConfigName();

	@Override
	@PreEnvironment(required = "original_client_config")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String configName = getExpectedConfigName();
		String specifiedConfigValue = env.getString("original_client_config", configName);

		if (Strings.isNullOrEmpty(specifiedConfigValue)) {
			throw error("Couldn't find %s in configuration".formatted(configName));
		}

		JsonObject client = env.getObject("client");
		client.addProperty(configName, specifiedConfigValue);
		env.putObject("client", client);

		log("Copied %s from original_client_config to client configuration".formatted(configName), args("client", client));

		return env;
	}
}
