package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class GetResourceEndpointConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "resource")
	public Environment evaluate(Environment env) {

		JsonElement resource = env.getElementFromObject("config", "resource");
		if (resource == null || !resource.isJsonObject()) {
			throw error("Couldn't find resource endpoint object in configuration");
		} else {
			env.putObject("resource", resource.getAsJsonObject());

			logSuccess("Found a resource endpoint object", resource.getAsJsonObject());
			return env;
		}
	}

}
