package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIEnsureCredentialResponseIsNotAnEncryptedJwe extends VCIEnsureCredentialResponseIsEncryptedJwe {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response");
		String responseBody = OIDFJSON.getString(endpointResponse.get("body"));

		if (isJWE(responseBody)) {
			throw error("Credential error response must not be an encrypted JWE", args("response", responseBody));
		}

		logSuccess("Credential error response is not an encrypted JWE", args("body_preview", responseBody));
		return env;
	}
}
