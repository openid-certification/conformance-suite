package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateJARMEncryptionEnc extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"server", "jarm_response"})
	public Environment evaluate(Environment env) {
		String enc = env.getString("jarm_response", "jwe_header.enc");

		JsonElement encValuesSupportedEl = env.getElementFromObject("server", "authorization_encryption_enc_values_supported");

		if(encValuesSupportedEl != null) {
			if (encValuesSupportedEl.getAsJsonArray().contains(new JsonPrimitive(enc))) {
				// Log Success
				logSuccess("JARM response Plaintext was encrypted with a supported encryption method." +
					"The JWE header 'enc' matched an entry in the 'authorization_encryption_enc_values_supported' array " +
					"returned in the server's discovery document",
					args("enc", enc, "supported", encValuesSupportedEl));

				return env;
			}

			throw error("JARM response Plaintext must be encrypted with an encryption method listed in 'authorization_encryption_enc_values_supported' returned in the server's discovery document",
				args("enc", enc, "supported", encValuesSupportedEl));
		}
		else {
			throw error("No JARM response Plaintext encryption methods found. 'authorization_encryption_enc_values_supported' is not present in the server's discovery document");
		}
	}
}
