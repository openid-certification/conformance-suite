package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateJARMSigningAlg extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"server", "jarm_response"})
	public Environment evaluate(Environment env) {
		String alg = env.getString("jarm_response", "jws_header.alg");

		JsonElement algValuesSupportedEl = env.getElementFromObject("server", "authorization_signing_alg_values_supported");

		if(algValuesSupportedEl != null) {
			if (algValuesSupportedEl.getAsJsonArray().contains(new JsonPrimitive(alg))) {
				// Log Success
				logSuccess("JARM response was signed with a supported algorithm" +
					"The JWS header 'alg' matched an entry in the 'authorization_signing_alg_values_supported' array " +
					"returned in the server's discovery document",
					args("alg", alg, "supported", algValuesSupportedEl));

				return env;
			}

			throw error("JARM response must be signed with an encryption algorithm listed in 'authorization_signing_alg_values_supported' returned in the server's discovery document",
				args("alg", alg, "supported", algValuesSupportedEl));
		}
		else {
			throw error("No JARM response signing algorithms found. 'authorization_signing_alg_values_supported' is not present in the server's discovery document");
		}
	}
}
