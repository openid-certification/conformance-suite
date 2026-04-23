package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Warns if cnf.jwk in an SD-JWT credential contains fields not defined in RFC 7517/7518.
 */
public class WarnIfUnknownFieldsInCredentialCnfJwk extends AbstractCondition {

	private static final Set<String> KNOWN_JWK_FIELDS = Set.of(
		"kty", "use", "key_ops", "alg", "kid",
		"x5u", "x5c", "x5t", "x5t#S256",
		"crv", "x", "y",  // EC
		"n", "e",          // RSA
		"d"                // private key indicator, checked by ValidateCnfJwkFields
	);

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {

		JsonElement jwkEl = env.getElementFromObject("sdjwt", "credential.claims.cnf.jwk");
		if (jwkEl == null || !jwkEl.isJsonObject()) {
			log("No cnf.jwk found, skipping check");
			return env;
		}

		JsonObject jwk = jwkEl.getAsJsonObject();
		List<String> unknownFields = new ArrayList<>();
		for (String key : jwk.keySet()) {
			if (!KNOWN_JWK_FIELDS.contains(key)) {
				unknownFields.add(key);
			}
		}

		if (!unknownFields.isEmpty()) {
			throw error("cnf.jwk contains unknown fields not defined in RFC 7517/7518",
				args("unknown_fields", unknownFields, "jwk", jwkEl));
		}

		logSuccess("cnf.jwk contains only known fields");
		return env;
	}
}
