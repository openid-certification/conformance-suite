package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class AbstractCopyConfigFromDynamicRegistrationTemplateToClientConfiguration extends AbstractCondition {

	protected abstract String getExpectedConfigName();

	@Override
	@PreEnvironment(required = "dynamic_client_registration_template")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String configName = getExpectedConfigName();
		String specifiedConfigValue = env.getString("dynamic_client_registration_template", configName);

		if (Strings.isNullOrEmpty(specifiedConfigValue)) {
			throw error(String.format("Couldn't find %s in configuration", configName));
		}

		JsonObject client = env.getObject("client");
		client.addProperty(configName, specifiedConfigValue);
		env.putObject("client", client);

		log(String.format("Copied %s from dynamic_client_registration_template to client configuration", configName), args("client", client));

		return env;
	}
}
