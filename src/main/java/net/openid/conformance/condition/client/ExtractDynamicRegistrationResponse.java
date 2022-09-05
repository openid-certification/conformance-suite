package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractDynamicRegistrationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		JsonObject client = (JsonObject) env.getElementFromObject("dynamic_registration_endpoint_response", "body_json");
		if (client == null) {
			throw error("No json response from dynamic registration endpoint");
		}

		env.putObject("client", client.deepCopy());

		JsonElement clientId = client.get("client_id");
		if (clientId == null) {
			throw error("no client id in dynamic registration response");
		}

		logSuccess("Extracted client from dynamic registration response",
			args("client_id", clientId));

		return env;
	}

}
