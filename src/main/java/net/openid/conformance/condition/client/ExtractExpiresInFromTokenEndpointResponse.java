package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractExpiresInFromTokenEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "expires_in")
	public Environment evaluate(Environment env) {
		JsonObject tokenEndpoint = env.getObject("token_endpoint_response");

		JsonElement expiresInValue = tokenEndpoint.get("expires_in");
		if (expiresInValue == null) {
			throw error("'expires_in' not present in the token endpoint response. RFC6749 recommends expires_in is included.", tokenEndpoint);
		}

		/* Create our cut down JsonObject with just a single value in it */
		JsonObject value = new JsonObject();
		value.add("expires_in", expiresInValue);
		env.putObject("expires_in", value);

		logSuccess("Extracted 'expires_in'", value);

		return env;

	}
}
