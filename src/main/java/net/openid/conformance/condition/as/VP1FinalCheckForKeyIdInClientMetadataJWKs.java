package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public class VP1FinalCheckForKeyIdInClientMetadataJWKs extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {
		JsonElement clientMetadata = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata");
		if (clientMetadata == null || !clientMetadata.isJsonObject()) {
			throw error("client_metadata not found in authorization request");
		}

		JsonElement jwksElement = clientMetadata.getAsJsonObject().get("jwks");
		if (jwksElement == null || !jwksElement.isJsonObject()) {
			throw error("jwks not found in client_metadata");
		}

		JsonObject jwks = jwksElement.getAsJsonObject();
		JsonElement keys = jwks.get("keys");
		if (keys == null || !keys.isJsonArray() || keys.getAsJsonArray().isEmpty()) {
			throw error("keys array is missing or empty in client_metadata.jwks", args("jwks", jwks));
		}

		Set<String> kidValues = new HashSet<>();
		for (JsonElement key : keys.getAsJsonArray()) {
			if (!key.isJsonObject()) {
				throw error("invalid key in client_metadata.jwks, not a JSON object", args("key", key));
			}

			JsonObject keyObj = key.getAsJsonObject();
			if (!keyObj.has("kid")) {
				throw error("Each JWK in client_metadata.jwks MUST have a kid (Key ID) that uniquely identifies the key within the context of the request",
					args("key", key));
			}

			String kid = OIDFJSON.getString(keyObj.get("kid"));
			if (kid.isBlank()) {
				throw error("Each JWK in client_metadata.jwks MUST have a kid (Key ID) that uniquely identifies the key within the context of the request",
					args("key", key));
			}

			if (!kidValues.add(kid)) {
				throw error("Duplicate kid value found in client_metadata.jwks; each kid MUST uniquely identify the key",
					args("kid", kid, "jwks", jwks));
			}
		}

		logSuccess("All keys in client_metadata.jwks have unique kid values", args("jwks", jwks));
		return env;
	}
}
