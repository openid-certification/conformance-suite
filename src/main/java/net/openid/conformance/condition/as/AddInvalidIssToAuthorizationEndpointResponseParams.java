package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidIssToAuthorizationEndpointResponseParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY, strings = "issuer")
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		String invalidIssuer = env.getString("issuer").concat("1");

		params.addProperty("iss", invalidIssuer);

		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, params);

		logSuccess("Added invalid Issuer to authorization endpoint response params", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params, "invalid Iss", invalidIssuer));

		return env;

	}

}
