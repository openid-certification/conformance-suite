package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIssToAuthorizationEndpointResponseParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY, strings = "issuer")
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		String issuer = env.getString("issuer");

		params.addProperty("iss", issuer);

		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, params);

		logSuccess("Added Iss to authorization endpoint response params", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

}
