package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAccessDeniedResponse extends AbstractCondition {

	@Override
	@PostEnvironment(required = { "token_endpoint_response" })
	public Environment evaluate(Environment env) {

		JsonObject tokenEndpointResponse = new JsonObject();
		tokenEndpointResponse.addProperty("error", "access_denied");

		env.putObject("token_endpoint_response", tokenEndpointResponse);

		logSuccess("Created token endpoint response", tokenEndpointResponse);

		return env;

	}

}
