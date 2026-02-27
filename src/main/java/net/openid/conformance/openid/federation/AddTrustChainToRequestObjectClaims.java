package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTrustChainToRequestObjectClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "config", "request_object_claims" })
	public Environment evaluate(Environment env) {

		JsonElement trustChainElement = env.getElementFromObject("config", "client.trust_chain");
		JsonArray trustChain = trustChainElement.getAsJsonObject().getAsJsonArray("trust_chain");
		env.putArray("request_object_claims", "trust_chain", trustChain);

		logSuccess("Added trust chain to request object");

		return env;
	}

}
