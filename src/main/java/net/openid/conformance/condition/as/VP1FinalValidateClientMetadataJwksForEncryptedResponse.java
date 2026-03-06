package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VP1FinalValidateClientMetadataJwksForEncryptedResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {

		JsonElement clientMetadata = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata");
		if (clientMetadata == null || !clientMetadata.isJsonObject()) {
			throw error("client_metadata is missing or not a JSON object");
		}

		JsonElement jwksEl = clientMetadata.getAsJsonObject().get("jwks");
		if (jwksEl == null) {
			throw error("client_metadata.jwks is missing but is required when using encrypted response mode",
				args("client_metadata", clientMetadata));
		}
		if (!jwksEl.isJsonObject()) {
			throw error("client_metadata.jwks must be a JSON object",
				args("jwks", jwksEl));
		}

		JsonObject jwks = jwksEl.getAsJsonObject();
		JsonElement keysEl = jwks.get("keys");
		if (keysEl == null || !keysEl.isJsonArray() || keysEl.getAsJsonArray().isEmpty()) {
			throw error("client_metadata.jwks.keys must be a non-empty array",
				args("jwks", jwks));
		}

		for (int i = 0; i < keysEl.getAsJsonArray().size(); i++) {
			JsonElement keyEl = keysEl.getAsJsonArray().get(i);
			if (!keyEl.isJsonObject()) {
				throw error("client_metadata.jwks.keys[" + i + "] must be a JSON object",
					args("key", keyEl));
			}
			JsonObject key = keyEl.getAsJsonObject();

			if (!key.has("alg")) {
				throw error("client_metadata.jwks.keys[" + i + "] is missing 'alg' which is needed to determine the encryption algorithm",
					args("key", key));
			}

			if (!key.has("use")) {
				log("client_metadata.jwks.keys[" + i + "] does not contain 'use' field; it is recommended to include 'use': 'enc' for encryption keys",
					args("key", key));
			}
		}

		logSuccess("client_metadata.jwks contains valid encryption key(s) with alg",
			args("jwks", jwks));

		return env;
	}
}
