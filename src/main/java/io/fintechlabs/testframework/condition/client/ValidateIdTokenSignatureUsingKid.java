package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

public class ValidateIdTokenSignatureUsingKid extends ValidateIdTokenSignature {

	@Override
	protected void validateIdTokenSignature(String idToken, JsonObject serverJwks) {
		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(idToken);
			JWKSet jwkSet = JWKSet.parse(serverJwks.toString());
			JWKSet jwkSetWithKeyValid = null;

			JWSHeader header = jwt.getHeader();
			String headerKeyID = header.getKeyID();
			String headerAlg = header.getAlgorithm() != null ? header.getAlgorithm().getName() : null;

			int numberOfKeyValid = 0;
			for (JWK jwkKey : jwkSet.getKeys()) {
				if ((headerKeyID != null && headerKeyID.equals(jwkKey.getKeyID()))
					&& (jwkKey.getAlgorithm() != null && jwkKey.getAlgorithm().getName().equals(headerAlg))
					&& KeyUse.SIGNATURE.equals(jwkKey.getKeyUse())) {

					numberOfKeyValid++;
					if (numberOfKeyValid > 1) {
						throw error("Found more than one key that has the right kid', alg and 'use':'sig'", args("jwks", serverJwks, "kid", headerKeyID, "alg", headerAlg, "id_token", idToken));
					}
				}
			}

			JWK key = null;

			// if a kid is given
			if (!Strings.isNullOrEmpty(headerKeyID)) {
				for (JWK jwkKey : jwkSet.getKeys()) {
					if (headerKeyID.equals(jwkKey.getKeyID())) {

						// filter by 'alg' if key has alg (matching the id_token alg) and 'use: sig' (if 'use' present in server key)
						if (jwkKey.getAlgorithm() != null && !headerAlg.equals(jwkKey.getAlgorithm().getName())) {
							continue;
						}
						if (jwkKey.getKeyUse() != null && !KeyUse.SIGNATURE.equals(jwkKey.getKeyUse())) {
							continue;
						}

						key = jwkKey;
						if (verifySignature(jwt, new JWKSet(jwkKey))) {
							// save key which is able to verify
							jwkSetWithKeyValid = new JWKSet(jwkKey);
							break;
						} else {
							throw error("Unable to verify ID token signature based on server key that found by kid/alg/'use':'sig'", args("jwks", serverJwks, "kid", headerKeyID, "alg", headerAlg, "id_token", idToken));
						}
					}
				}
				if (key == null) {
					throw error("Server JWKS does not contain a key with the correct kid that also matches (or does not have) 'use':'sig' & alg", args("jwks", serverJwks, "kid", headerKeyID, "alg", headerAlg, "id_token", idToken));
				}
			} else {
				// if a kid isn't given
				boolean validSignature = false;
				for (JWK jwkKey : jwkSet.getKeys()) {

					// filter by 'alg' if key has alg (matching the id_token alg) and 'use: sig' (if 'use' present in server key)
					if (jwkKey.getAlgorithm() != null && !headerAlg.equals(jwkKey.getAlgorithm().getName())) {
						continue;
					}
					if (jwkKey.getKeyUse() != null && !KeyUse.SIGNATURE.equals(jwkKey.getKeyUse())) {
						continue;
					}

					key = jwkKey;
					if (verifySignature(jwt, new JWKSet(jwkKey))) {
						// save key which is able to verify
						jwkSetWithKeyValid = new JWKSet(jwkKey);
						validSignature = true;
						break;
					}
				}
				if (key == null) {
					throw error("Server JWKS does not contain a key that matches 'use':'sig' (if 'use' present in server key) & alg (if 'alg' present in server key)", args("jwks", serverJwks, "kid", headerKeyID, "alg", headerAlg, "id_token", idToken));
				}
				if (!validSignature) {
					throw error("Unable to verify ID token signature based on server keys", args("jwks", serverJwks, "id_token", idToken));
				}
			}

			String publicKeySetString = jwkSetWithKeyValid.toPublicJWKSet().getKeys().size() > 0 ? jwkSetWithKeyValid.toPublicJWKSet().getKeys().iterator().next().toString() : null;
			JsonObject idTokenObject = new JsonObject();
			idTokenObject.addProperty("verifiable_jws", idToken);
			idTokenObject.addProperty("public_jwk", publicKeySetString);
			logSuccess("ID Token signature validated", args("id_token", idTokenObject));

		} catch (JOSEException | ParseException e) {
			throw error("Error validating ID Token signature", e);
		}
	}
}
