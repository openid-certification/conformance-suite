package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractTrustChainFromRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"trust_chain", "authorization_request_object"})
	public Environment evaluate(Environment env) {
		JsonElement trustChainInRequestObject = env.getElementFromObject("authorization_request_object", "claims.trust_chain");

		if (trustChainInRequestObject != null && trustChainInRequestObject.isJsonArray()) {
			env.putArray("trust_chain", "trust_chain", trustChainInRequestObject.getAsJsonArray());

			logSuccess("Successfully extracted trust_chain from authorization request object and added to environment",
				args("trust_chain", trustChainInRequestObject));
		} else {
			log("No trust_chain array found in authorization request object");
		}

		return env;
	}
}
