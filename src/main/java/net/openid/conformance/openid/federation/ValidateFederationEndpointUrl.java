package net.openid.conformance.openid.federation;

import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateFederationEndpointUrl extends ValidateUrlRequirements {

	@Override
	protected boolean allowsQueryParameters() {
		return true;
	}

	@Override
	@PreEnvironment(strings = { "federation_endpoint_url" } )
	public Environment evaluate(Environment env) {
		String label = "Federation endpoint URL";
		String fieldName = "federation_endpoint_url";
		String url = env.getString("federation_endpoint_url");
		return validateUrlRequirements(new JsonPrimitive(url), fieldName, label, env);
	}

}
