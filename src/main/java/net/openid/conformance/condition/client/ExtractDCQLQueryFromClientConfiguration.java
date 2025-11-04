package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.testmodule.Environment;

public class ExtractDCQLQueryFromClientConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonElement dcqlElement = env.getElementFromObject("client", "dcql");

		if (dcqlElement == null) {
			throw error("dcql not found in client configuration");
		}

		if (!dcqlElement.isJsonObject()) {
			throw error("dcql in client configuration is not a JSON object",
				args("dcql", dcqlElement));
		}

		JsonObject dcql = dcqlElement.getAsJsonObject();
		env.putObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY, dcql);

		logSuccess("Extracted dcql from client configuration", args("dcql", dcql));

		return env;
	}
}
