package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * See: 8.3. Credential Response https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3
 */
public class VCIExtractCredentialResponse extends AbstractCondition {

	@SuppressWarnings("unused")
	@Override
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response").getAsJsonObject();
		JsonObject credentialResponseBodyJson = JsonParser.parseString(OIDFJSON.getString(endpointResponse.get("body"))).getAsJsonObject();

		JsonElement credentialsEl = credentialResponseBodyJson.get("credentials");
		if (credentialsEl == null) {
			throw error("'credentials' missing from credential endpoint response", args("credential_response", credentialResponseBodyJson));
		}

		if (!credentialsEl.isJsonArray()) {
			throw error("'credentials' must be an array", args("credential_response", credentialResponseBodyJson));
		}

		JsonArray credentials = credentialsEl.getAsJsonArray();
		if (credentials.isEmpty()) {
			throw error("'credentials' array must contain at least one credential", args("credential_response", credentialResponseBodyJson));
		}

		JsonArray extractedCredentials = new JsonArray();

		for (int i = 0; i < credentials.size(); i++) {
			JsonElement credentialObjEl = credentials.get(i);
			if (!credentialObjEl.isJsonObject()) {
				throw error("'credentials' array entry at index " + i + " is not an object",
					args("credential_response", credentialResponseBodyJson));
			}

			JsonObject credentialObj = credentialObjEl.getAsJsonObject();
			if (!credentialObj.has("credential")) {
				throw error("object at index " + i + " in 'credentials' array must contain a 'credential' property",
					args("credential_response", credentialResponseBodyJson));
			}
			JsonElement credentialEl = credentialObj.get("credential");
			if (!credentialEl.isJsonPrimitive() || !credentialEl.getAsJsonPrimitive().isString()) {
				throw error("'credential' property at index " + i + " must be a string",
					args("credential_response", credentialResponseBodyJson));
			}
			extractedCredentials.add(credentialEl);
		}

		// Store all extracted credentials for iteration
		JsonObject credentialsObject = new JsonObject();
		credentialsObject.add("list", extractedCredentials);
		env.putObject("extracted_credentials", credentialsObject);

		logSuccess("Extracted " + extractedCredentials.size() + " credential(s)",
			args("credentials_count", extractedCredentials.size(), "credentials", extractedCredentials));

		return env;
	}
}
