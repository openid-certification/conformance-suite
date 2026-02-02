package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIEnsureCredentialResponseIsEncryptedJwe extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response");
		String responseBody = OIDFJSON.getString(endpointResponse.get("body"));

		// Check if the response is a JWE (starts with eyJ and has 5 parts separated by dots)
		if (!isJWE(responseBody)) {
			throw error("Credential response appears not to be an encrypted JWE",
				args("response", responseBody));
		}

		logSuccess("Credential response appears to be an encrypted JWE",
			args("body_preview", responseBody.substring(0, Math.min(100, responseBody.length()))));
		return env;
	}

	/**
	 * Checks if the string appears to be a JWE (compact serialization).
	 * A JWE has 5 parts separated by dots.
	 */
	protected boolean isJWE(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}
		// JWE compact serialization has exactly 5 parts
		String[] parts = str.split("\\.");
		return parts.length == 5;
	}
}
