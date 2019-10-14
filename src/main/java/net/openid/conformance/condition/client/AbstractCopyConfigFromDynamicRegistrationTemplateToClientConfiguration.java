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
