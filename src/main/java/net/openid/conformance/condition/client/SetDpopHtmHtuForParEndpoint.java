package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetDpopHtmHtuForParEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server", "dpop_proof_claims"})
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		String parEndpoint = env.getString("pushed_authorization_request_endpoint") != null ? env.getString("pushed_authorization_request_endpoint") : env.getString("server", "pushed_authorization_request_endpoint");

		if (Strings.isNullOrEmpty(parEndpoint)) {
			throw error("pushed_authorization_request_endpoint not found in server configuration", args("server_config", env.getObject("server")));
		}

		String resourceMethod = env.getString(CallPAREndpoint.HTTP_METHOD_KEY) == null ?
			"POST" : env.getString(CallPAREndpoint.HTTP_METHOD_KEY);

		claims.addProperty("htm", resourceMethod);
		claims.addProperty("htu", parEndpoint);

		logSuccess("Added htm/htu to DPoP proof claims", claims);

		return env;

	}
}
