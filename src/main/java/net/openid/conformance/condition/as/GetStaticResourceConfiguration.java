package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GetStaticResourceConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "resource", strings = "resource_id")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}

		// make sure we've got a client object
		JsonElement resource = env.getElementFromObject("config", "resource");
		if (resource == null || !resource.isJsonObject()) {
			throw error("Definition for resource not present in supplied configuration");
		} else {
			// we've got a client object, put it in the environment
			env.putObject("resource", resource.getAsJsonObject());

			// pull out the resource ID and put it in the root environment for easy access
			env.putString("resource_id", env.getString("resource", "resource_id"));

			logSuccess("Found a static resource object", resource.getAsJsonObject());
			return env;
		}
	}

}
