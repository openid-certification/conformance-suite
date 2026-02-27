package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractTrustChainFromRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"trust_chain", "authorization_request_object"})
	public Environment evaluate(Environment env) {
		JsonElement trustChainInHeader = env.getElementFromObject("authorization_request_object", "header.trust_chain");
		JsonElement trustChainInClaims = env.getElementFromObject("authorization_request_object", "claims.trust_chain");

		if (trustChainInHeader != null && trustChainInHeader.isJsonArray()) {
			env.putArray("trust_chain", "trust_chain", trustChainInHeader.getAsJsonArray());

			logSuccess("Successfully extracted trust_chain from authorization request object header and added to environment",
				args("trust_chain", trustChainInHeader));
		} else if (trustChainInClaims != null && trustChainInClaims.isJsonArray()) {
			env.putArray("trust_chain", "trust_chain", trustChainInClaims.getAsJsonArray());

			throw error("The trust_chain was found in the JWT claims, but it is RECOMMENDED to use the JWS header instead.",
				args("trust_chain", trustChainInClaims));
		} else {
			log("No trust_chain array found in authorization request object (checked both header and claims)");
		}

		return env;
	}
}
