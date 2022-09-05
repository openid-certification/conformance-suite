package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetDpopHtmHtuForResourceEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dpop_proof_claims", strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		String resourceEndpoint = env.getString("protected_resource_url");

		String resourceMethod = "GET";
		String configuredMethod = env.getString("resource", "resourceMethod");
		if (!Strings.isNullOrEmpty(configuredMethod)) {
			resourceMethod = configuredMethod;
		}

		claims.addProperty("htm", resourceMethod);
		claims.addProperty("htu", resourceEndpoint);

		logSuccess("Added htm/htu to DPoP proof claims", claims);

		return env;

	}
}
