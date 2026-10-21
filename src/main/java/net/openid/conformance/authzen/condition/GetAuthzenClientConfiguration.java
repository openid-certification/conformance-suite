package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Copies the tester-supplied {@code config.client} object into the environment as
 * {@code client}. The suite acts as the PEP, so the credentials it presents to the PDP
 * (client_secret, api_key) live there rather than in the {@code pdp} object, which
 * discovery replaces with the fetched metadata.
 */
public class GetAuthzenClientConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonElement client = env.getElementFromObject("config", "client");
		if (client == null || !client.isJsonObject()) {
			throw error("The 'Client' section is missing from the test configuration; it must contain the credentials the suite presents to the PDP");
		}
		env.putObject("client", client.getAsJsonObject());
		logSuccess("Found a static client object", client.getAsJsonObject());
		return env;
	}

}
