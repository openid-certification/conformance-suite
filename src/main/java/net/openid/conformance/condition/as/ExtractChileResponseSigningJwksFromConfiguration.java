package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.util.List;

/**
 * Loads the key the emulated Chile bank signs the x-jws-signature of its resource responses with
 * (CMF-XJWS-5.3.1) from the 'response signing jwks' field of the test configuration. The profile
 * requires PS256 and a kid, so the set must hold exactly one RSA private key with a kid.
 */
public class ExtractChileResponseSigningJwksFromConfiguration extends AbstractCondition {

	public static final String RESPONSE_SIGNING_JWKS = "chile_response_signing_jwks";

	public static final String RESPONSE_SIGNING_PUBLIC_JWKS = "chile_response_signing_public_jwks";

	private static final String FIELD = "'response signing jwks' field in the 'Server' section of the test configuration";

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = {RESPONSE_SIGNING_JWKS, RESPONSE_SIGNING_PUBLIC_JWKS})
	public Environment evaluate(Environment env) {
		JsonElement configured = env.getElementFromObject("config", "server.response_signing_jwks");
		if (configured == null) {
			throw error("The " + FIELD + " is missing. The Chile profile requires the key the test suite signs resource responses with.");
		}
		if (!configured.isJsonObject()) {
			throw error("The " + FIELD + " is not a JSON Web Key Set", args("response_signing_jwks", configured));
		}

		JWKSet jwks;
		try {
			jwks = JWKUtil.parseJWKSet(configured.toString());
		} catch (ParseException e) {
			throw error("The " + FIELD + " is not a valid JSON Web Key Set", e, args("response_signing_jwks", configured));
		}

		List<JWK> keys = jwks.getKeys();
		if (keys.size() != 1) {
			throw error("The " + FIELD + " must hold exactly one key", args("key_count", keys.size()));
		}
		JWK key = keys.get(0);
		if (!(key instanceof RSAKey)) {
			throw error("The key in the " + FIELD + " must be an RSA key, as the Chile profile signs responses with PS256",
				args("kty", key.getKeyType().getValue()));
		}
		if (!key.isPrivate()) {
			throw error("The key in the " + FIELD + " has no private part, so the test suite cannot sign with it");
		}
		if (key.getKeyID() == null || key.getKeyID().isEmpty()) {
			throw error("The key in the " + FIELD + " has no 'kid'; the Chile profile requires the response signature to identify its key");
		}

		JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(jwks);
		JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(jwks);
		env.putObject(RESPONSE_SIGNING_JWKS, privateJwks);
		env.putObject(RESPONSE_SIGNING_PUBLIC_JWKS, publicJwks);

		logSuccess("Loaded the key the test suite signs Chile resource responses with",
			args("kid", key.getKeyID(), "response_signing_public_jwks", publicJwks));
		return env;
	}
}
