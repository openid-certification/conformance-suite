package net.openid.conformance.openid.federation;

import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateFederationUrl extends ValidateUrlRequirements {

	@Override
	@PreEnvironment(strings = { "federation_endpoint_url" } )
	public Environment evaluate(Environment env) {
		String label = "Entity identifier";
		String fieldName = "entity_identifier";
		String url = env.getString("federation_endpoint_url");
		return validateUrlRequirements(new JsonPrimitive(url), fieldName, label, env);
	}

}
