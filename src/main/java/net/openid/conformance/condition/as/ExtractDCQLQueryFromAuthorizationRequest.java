package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractDCQLQueryFromAuthorizationRequest extends AbstractCondition {

	public static final String ENV_KEY = "dcql_query";

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonElement dcqlElement = env.getElementFromObject(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "dcql_query");

		if (dcqlElement == null) {
			throw error("dcql_query not found in authorization request parameters");
		}

		if (!dcqlElement.isJsonObject()) {
			throw error("dcql_query in authorization request is not a JSON object",
				args("dcql_query", dcqlElement));
		}

		JsonObject dcql = dcqlElement.getAsJsonObject();

		env.putObject(ENV_KEY, dcql);

		logSuccess("Extracted dcql_query from authorization request", args("dcql", dcql));

		return env;
	}
}
