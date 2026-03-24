package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

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

		// OID4VP 1.0 Final Section 8.3: "The alg parameter MUST be present in the JWKs."
		boolean hasEncryptionUsableKey = false;
		for (int i = 0; i < keysEl.getAsJsonArray().size(); i++) {
			JsonElement keyEl = keysEl.getAsJsonArray().get(i);
			if (!keyEl.isJsonObject()) {
				throw error("client_metadata.jwks.keys[" + i + "] must be a JSON object",
					args("key", keyEl));
			}
			JsonObject key = keyEl.getAsJsonObject();

			if (!key.has("alg")) {
				throw error("client_metadata.jwks.keys[" + i + "] is missing 'alg'",
					args("key", key));
			}

			// OID4VP 1.0 Final Section 8.3: "the Wallet selects the public key to encrypt the
			// Authorization Response based on information about each key, such as the kty (Key Type),
			// use (Public Key Use), alg (Algorithm), and other JWK parameters."
			// A key is usable for encryption if 'use' is 'enc' or 'use' is absent.
			if (!key.has("use") || "enc".equals(OIDFJSON.getString(key.get("use")))) {
				hasEncryptionUsableKey = true;
			}
		}

		if (!hasEncryptionUsableKey) {
			throw error("client_metadata.jwks contains no key usable for encryption; "
				+ "at least one key must have 'use':'enc' or omit 'use'",
				args("jwks", jwks));
		}

		logSuccess("client_metadata.jwks contains valid encryption key(s) with alg",
			args("jwks", jwks));

		return env;
	}
}
