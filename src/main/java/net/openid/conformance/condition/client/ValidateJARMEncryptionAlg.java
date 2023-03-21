package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateJARMEncryptionAlg extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"server", "jarm_response"})

	public Environment evaluate(Environment env) {
		String alg = env.getString("jarm_response", "jwe_header.alg");

		JsonElement algValuesSupportedEl = env.getElementFromObject("server", "authorization_encryption_alg_values_supported");

		if(algValuesSupportedEl != null) {
			if (algValuesSupportedEl.getAsJsonArray().contains(new JsonPrimitive(alg))) {
				// Log Success
				logSuccess("JARM response CEK was encrypted with a permitted algorithm",
					args("alg", alg, "permitted", algValuesSupportedEl));

				return env;
			}

			throw error("JARM response CEK must be encrypted with a permitted alg",
				args("alg", alg, "permitted", algValuesSupportedEl));
		}
		else {
			throw error("No permitted JARM CEK response encryption algorithms found.");
		}
	}
}
