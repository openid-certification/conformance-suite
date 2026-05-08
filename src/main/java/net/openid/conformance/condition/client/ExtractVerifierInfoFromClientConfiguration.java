package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractVerifierInfoFromClientConfiguration extends AbstractCondition {

	public static final String ENV_KEY = "verifier_info";
	public static final String WRAPPER_PROPERTY = "verifier_info";

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonElement verifierInfoElement = env.getElementFromObject("client", "verifier_info");

		if (verifierInfoElement == null) {
			throw error("'verifier_info' field is missing from the 'Client' section in the test configuration");
		}

		JsonObject wrapper = new JsonObject();
		wrapper.add(WRAPPER_PROPERTY, verifierInfoElement);
		env.putObject(ENV_KEY, wrapper);

		logSuccess("Extracted verifier_info from client configuration", args("verifier_info", verifierInfoElement));

		return env;
	}
}
