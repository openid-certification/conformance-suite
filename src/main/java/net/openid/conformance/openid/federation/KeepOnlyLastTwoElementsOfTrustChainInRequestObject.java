package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class KeepOnlyLastTwoElementsOfTrustChainInRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims" })
	public Environment evaluate(Environment env) {

		JsonElement trustChainElement = env.getElementFromObject("request_object_claims", "trust_chain");
		if (trustChainElement == null ||!trustChainElement.isJsonArray()) {
			throw error("No trust_chain array found in request object claims");
		}

		JsonArray trustChain = trustChainElement.getAsJsonArray();
		if (trustChain.size() <= 2) {
			log("Trust chain is already invalid as it does not contain at least three elements.");
			return env;
		}

		JsonArray trustChainWithOnlyTwoLastElements = new JsonArray();
		trustChainWithOnlyTwoLastElements.add(trustChain.get(trustChain.size() - 2));
		trustChainWithOnlyTwoLastElements.add(trustChain.get(trustChain.size() - 1));

		env.putArray("request_object_claims", "trust_chain", trustChainWithOnlyTwoLastElements);

		logSuccess("Added truncated trust chain to request object (only the last two elements of the original chain)");

		return env;
	}

}
