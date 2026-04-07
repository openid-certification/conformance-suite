package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import java.text.ParseException;

/**
 * Validates that the JWK Set in client_metadata contains only public keys.
 * Wallets should not send private key material in client_metadata.
 */
public class ValidateVpClientMetadataJwksKeysArePublic extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {

		JsonElement jwksEl = env.getElementFromObject(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata.jwks");
		if (jwksEl == null) {
			log("No jwks in client_metadata, skipping public key check");
			return env;
		}

		var keys = jwksEl.getAsJsonObject().getAsJsonArray("keys");
		if (keys == null || keys.isEmpty()) {
			log("No keys in client_metadata jwks");
			return env;
		}

		for (int i = 0; i < keys.size(); i++) {
			try {
				JWK jwk = JWK.parse(keys.get(i).toString());
				if (jwk instanceof OctetSequenceKey) {
					throw error("client_metadata jwks contains a symmetric key at index " + i,
						args("key", keys.get(i)));
				}
				if (jwk.isPrivate()) {
					throw error("client_metadata jwks contains a private key at index " + i,
						args("key", keys.get(i)));
				}
			} catch (ParseException e) {
				throw error("Failed to parse JWK at index " + i + " in client_metadata jwks",
					args("key", keys.get(i), "error", e.getMessage()));
			}
		}

		logSuccess("All keys in client_metadata jwks are public keys",
			args("key_count", keys.size()));
		return env;
	}
}
