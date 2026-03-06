package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateJWEHeaderAlgMatchesRequestedAlgorithm extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"response_jwe", "authorization_endpoint_request"})
	public Environment evaluate(Environment env) {
		String alg = env.getString("response_jwe", "jwe_header.alg");

		if (alg == null) {
			throw error("JWE header alg is absent");
		}

		// In OID4VP, the alg is determined by the alg parameter on the key in client_metadata.jwks
		JsonElement jwksEl = env.getElementFromObject("authorization_endpoint_request",
			"client_metadata.jwks");

		if (jwksEl == null) {
			throw error("client_metadata.jwks is not present in the authorization request");
		}

		JsonArray keys = jwksEl.getAsJsonObject().getAsJsonArray("keys");
		if (keys == null || keys.isEmpty()) {
			throw error("client_metadata.jwks.keys is empty or absent");
		}

		for (JsonElement keyEl : keys) {
			JsonObject key = keyEl.getAsJsonObject();
			JsonElement keyAlgEl = key.get("alg");
			if (keyAlgEl != null && alg.equals(OIDFJSON.getString(keyAlgEl))) {
				logSuccess("JWE header alg matches the alg from the encryption key in client_metadata.jwks",
					args("alg", alg));
				return env;
			}
		}

		throw error("JWE header alg does not match the alg of any key in client_metadata.jwks",
			args("alg", alg, "jwks", jwksEl));
	}

}
