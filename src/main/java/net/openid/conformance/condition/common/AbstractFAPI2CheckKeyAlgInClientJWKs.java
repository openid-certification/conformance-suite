package net.openid.conformance.condition.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.Curve;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public abstract class AbstractFAPI2CheckKeyAlgInClientJWKs extends AbstractCondition {

	abstract Set<String> getPermitted();

	@Override
	@PreEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {
		final Set<String> permitted = getPermitted();
		JsonElement keys = env.getElementFromObject("client_jwks", "keys");
		if (keys == null || !keys.isJsonArray()) {
			throw error("keys array not found in client JWKs");
		}

		for (JsonElement key : keys.getAsJsonArray()) {
			if (!key.isJsonObject()) {
				throw error("invalid key in client JWKs", args("key", key));
			}
			JsonObject keyObj = key.getAsJsonObject();

			if (!keyObj.has("alg")) {
				throw error("'alg' not found in client JWKS provided in the test configuration - this is "+
						"required to set the request object signing algorithm the conformance suite will use, and "+
						"should be set to a permitted alg",
					args("key", key, "permitted", permitted));
			}

			String use = keyObj.has("use") ? OIDFJSON.getString(keyObj.getAsJsonPrimitive("use")) : null;
			if (use == null || use.equals("sig")) {
				String alg = OIDFJSON.getString(keyObj.getAsJsonPrimitive("alg"));
				if (!permitted.contains(alg)) {
					throw error("client jwks contains a signing key with a non-permitted alg",
						args("key", keyObj,
							"permitted", permitted));
				}

				if("EdDSA".equals(alg)){
					if(!keyObj.has("crv")) {
						throw error("client jwks contains EdDSA alg with a missing crv parameter",
							args("key", keyObj));
					}
					String curve = OIDFJSON.getString(keyObj.getAsJsonPrimitive("crv"));
					if(!Curve.Ed25519.getName().equals(curve)) {
						throw error("client jwks contains EdDSA alg with an unsupported curve",
							args("key", keyObj));
					}
				}
			}

		}

		logSuccess("Keys in client JWKS all have permitted 'alg'",
			args("permitted", permitted));

		return env;
	}
}
