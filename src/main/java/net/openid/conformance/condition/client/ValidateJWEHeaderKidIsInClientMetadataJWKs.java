package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateJWEHeaderKidIsInClientMetadataJWKs extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "response_jwe", "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {
		String kid = env.getString("response_jwe", "jwe_header.kid");
		if (kid == null || kid.isBlank()) {
			throw error("JWE header does not contain a kid referencing a key from client_metadata.jwks");
		}

		JsonElement jwksElement = env.getElementFromObject("authorization_endpoint_request", "client_metadata.jwks");
		if (jwksElement == null || !jwksElement.isJsonObject()) {
			throw error("client_metadata.jwks not found in authorization_endpoint_request");
		}

		JsonElement keys = jwksElement.getAsJsonObject().get("keys");
		if (keys == null || !keys.isJsonArray()) {
			throw error("keys array not found in client_metadata.jwks");
		}

		for (JsonElement key : keys.getAsJsonArray()) {
			if (!key.isJsonObject()) {
				continue;
			}
			JsonObject keyObj = key.getAsJsonObject();
			if (keyObj.has("kid") && kid.equals(OIDFJSON.getString(keyObj.get("kid")))) {
				logSuccess("JWE header kid references a key from client_metadata.jwks",
					args("kid", kid));
				return env;
			}
		}

		throw error("JWE header kid does not match any key in client_metadata.jwks",
			args("kid", kid, "jwks", jwksElement));
	}
}
