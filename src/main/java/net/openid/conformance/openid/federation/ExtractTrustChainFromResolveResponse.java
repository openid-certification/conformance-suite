package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractTrustChainFromResolveResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "federation_response_jwt")
	public Environment evaluate(Environment env) {

		JsonElement trustChainElement = env.getElementFromObject("federation_response_jwt", "claims.trust_chain");
		if (trustChainElement == null) {
			logSuccess("Resolve response does not contain a trust_chain element. Skipping extraction.");
			return env;
		}

		JsonArray trustChain = trustChainElement.getAsJsonArray();
		JsonObject trustChainFromResolver = new JsonObject();
		trustChainFromResolver.add("trust_chain", trustChain);
		env.putObject("trust_chain_from_resolver", trustChainFromResolver);

		logSuccess("Extracted trust chain from resolve response", args("trust_chain", trustChain));

		return env;
	}

}
