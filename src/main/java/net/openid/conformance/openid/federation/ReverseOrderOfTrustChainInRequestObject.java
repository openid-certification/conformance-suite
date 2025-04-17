package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ReverseOrderOfTrustChainInRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims" })
	public Environment evaluate(Environment env) {

		JsonElement trustChainElement = env.getElementFromObject("request_object_claims", "trust_chain");
		if (trustChainElement == null ||!trustChainElement.isJsonArray()) {
			throw error("No trust_chain array found in request object claims");
		}

		JsonArray trustChain = trustChainElement.getAsJsonArray();
		JsonArray reversedTrustChain = new JsonArray();
		for (int i = trustChain.size() - 1; i >= 0; i--) {
			reversedTrustChain.add(trustChain.get(i));
		}
		env.putArray("request_object_claims", "trust_chain", reversedTrustChain);

		logSuccess("Added reverse order trust chain to request object");

		return env;
	}

}
