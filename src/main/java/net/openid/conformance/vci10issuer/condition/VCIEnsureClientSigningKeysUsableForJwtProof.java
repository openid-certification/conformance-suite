package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * The emulated wallet signs its {@code jwt} key proofs with ES256 ({@link VCIGenerateJwtProof}),
 * one proof per signing key in the client's JWKS, so every signing key must be an EC P-256 key.
 * Checked when the client is configured, so a tester learns about an unusable key (such as the
 * RSA key a FAPI configuration typically holds) before the authorization flow runs instead of at
 * the credential request. Encryption keys ({@code use} {@code enc}) are not proof keys and are
 * ignored.
 *
 * Not applicable, and so only logged, when the selected credential configuration does not use
 * cryptographic binding or its proof type is not {@code jwt}.
 */
public class VCIEnsureClientSigningKeysUsableForJwtProof extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			log("The credential configuration does not use cryptographic binding, so no key proof is sent and the client's signing keys need not be EC P-256 keys");
			return env;
		}
		String proofTypeKey = env.getString("vci_proof_type_key");
		if (!"jwt".equals(proofTypeKey)) {
			log("The proof type is not jwt, so the client's signing keys need not be EC P-256 keys",
				args("proof_type", proofTypeKey));
			return env;
		}

		JsonElement jwks = env.getElementFromObject("client", "jwks");
		if (jwks == null) {
			throw error("Couldn't find JWKS in configuration");
		}

		JWKSet jwkSet;
		try {
			jwkSet = JWKSet.parse(jwks.toString());
		} catch (ParseException e) {
			throw error("Invalid JWKS in configuration", e, args("jwks", jwks));
		}

		List<String> unusable = new ArrayList<>();
		List<String> usable = new ArrayList<>();
		for (JWK jwk : jwkSet.getKeys()) {
			if (jwk.getKeyUse() != null && !KeyUse.SIGNATURE.equals(jwk.getKeyUse())) {
				continue;
			}
			String name = jwk.getKeyID() != null ? jwk.getKeyID() : "(no kid)";
			if (!KeyType.EC.equals(jwk.getKeyType())) {
				unusable.add(name + ": kty " + jwk.getKeyType().getValue());
			} else if (!Curve.P_256.equals(((ECKey) jwk).getCurve())) {
				unusable.add(name + ": crv " + ((ECKey) jwk).getCurve());
			} else {
				usable.add(name);
			}
		}

		if (!unusable.isEmpty()) {
			throw error("The key proof in the credential request is a JWT signed with ES256 and one proof is created "
					+ "for each signing key in the client's JWKS, so every signing key in the 'jwks' field of the "
					+ "'client' section in the test configuration ('client2' for the second client) must be an "
					+ "EC P-256 key. Remove the key, give it 'use': 'enc' if it is an encryption key, or omit the "
					+ "field so the suite generates a suitable key",
				args("unusable_signing_keys", unusable, "jwks", jwks));
		}
		if (usable.isEmpty()) {
			throw error("No signing keys found in the client's JWKS", args("jwks", jwks));
		}

		logSuccess("All signing keys in the client's JWKS are EC P-256 keys and can sign the jwt key proof",
			args("signing_keys", usable));
		return env;
	}
}
