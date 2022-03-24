package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetDpopHtmHtuForTokenEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server", "dpop_proof_claims"})
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		String tokenEndpoint = env.getString("token_endpoint") != null ? env.getString("token_endpoint") : env.getString("server", "token_endpoint");

		if (Strings.isNullOrEmpty(tokenEndpoint)) {
			throw error("token_endpoint not found in server configuration", args("server_config", env.getObject("server")));
		}

		String resourceMethod = "POST";

		claims.addProperty("htm", resourceMethod);
		claims.addProperty("htu", tokenEndpoint);

		logSuccess("Added htm/htu to DPoP proof claims", claims);

		return env;

	}
}
