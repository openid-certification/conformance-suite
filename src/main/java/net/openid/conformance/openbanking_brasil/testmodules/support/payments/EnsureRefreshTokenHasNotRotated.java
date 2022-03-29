package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureRefreshTokenHasNotRotated extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"token_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonObject tokenEndpointResponse = env.getObject("token_endpoint_response");
		String refreshToken = OIDFJSON.getString(tokenEndpointResponse.get("refresh_token"));
		String initialToken = env.getString("initial_refresh_token");

		if (!refreshToken.equals(initialToken)){
			throw error("Refresh tokens rotated fail");
		} else {
			logSuccess("Refresh tokens not rotated");
		}
		return env;
	}
}
