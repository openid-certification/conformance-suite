package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SaveInitialRefreshToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"token_endpoint_response"})
	@PostEnvironment(strings = "initial_refresh_token")
	public Environment evaluate(Environment env) {
		JsonObject tokenEndpointResponse = env.getObject("token_endpoint_response");
		String refreshToken = OIDFJSON.getString(tokenEndpointResponse.get("refresh_token"));

		logSuccess("Refresh token saved " + refreshToken);
		env.putString("initial_refresh_token", refreshToken);

		return env;
	}
}
