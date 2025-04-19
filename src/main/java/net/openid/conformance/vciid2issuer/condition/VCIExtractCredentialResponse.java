package net.openid.conformance.vciid2issuer.condition;

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

		// TODO: cope with more than 1
		JsonElement credentialObjEl = credentials.get(0);
		if (!credentialObjEl.isJsonObject()) {
			throw error("'credential' object", args("credential_response", credentialResponseBodyJson));
		}

		JsonObject credentialObj = credentialObjEl.getAsJsonObject();
		if (!credentialObj.has("credential")) {
			throw error("objects in 'credentials' array must contain a 'credential' property", args("credential_response", credentialResponseBodyJson));
		}
		JsonElement credentialEl = credentialObj.get("credential");
		if (!credentialEl.isJsonPrimitive() || !credentialEl.getAsJsonPrimitive().isString()) {
			throw error("'credential' property must be a string", args("credential_response", credentialResponseBodyJson));
		}
		String credential = OIDFJSON.getString(credentialEl);

		env.putString("credential", credential);

		logSuccess("Extracted credential", args("credential", credential));

		return env;
	}
}
