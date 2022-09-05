package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
