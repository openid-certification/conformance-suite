package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jwt.EncryptedJWT;


import java.text.ParseException;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.util.JWTUtil;

public abstract class AbstractVerifyJweEncryption extends AbstractCondition {

	protected boolean verifyJweEncryption(String token, JsonObject publicJwks, String tokenName) {
		try {
			// Translate the token into nimbus objects
			JWT jwt = JWTUtil.parseJWT(token);

			// We are only interested in encrypted JWTs.
			if(jwt instanceof EncryptedJWT encryptedJWT) {

				JWEHeader header   = encryptedJWT.getHeader();
				String headerKeyID = header.getKeyID();
				String headerAlg   = header.getAlgorithm() != null ? header.getAlgorithm().getName() : null;

				if (headerAlg == null) {
					throw error("The JWE header does not contain alg. This is required.", args(tokenName, token));
				}

				String headerKty   = KeyType.forAlgorithm(new Algorithm(headerAlg)).getValue();

				// We are only interested in JWTs encrypted with an asymmetric algorithm
				if (JWEAlgorithm.Family.ASYMMETRIC.contains(header.getAlgorithm())) {
					JWKSet jwkSet = JWKSet.parse(publicJwks.toString());

					// Check for matches in the key set.
					int numberOfValidKeys	= 0;
					int numberOfValidKeysWithKid = 0;

					for (JWK jwkKey : jwkSet.getKeys()) {
						// Match on key type
						if (jwkKey.getKeyType().getValue().equals(headerKty)) {
							numberOfValidKeys++;

							// If a kid was specified note a match.
							if (headerKeyID != null && headerKeyID.equals(jwkKey.getKeyID())) {
								numberOfValidKeysWithKid++;
							}
						}
					}

					// Multiple keys matched, none matching the kid hint.
					if (headerKeyID != null && numberOfValidKeys > 1 && numberOfValidKeysWithKid == 0) {
						throw error("Found multiple keys in JWKS of the correct type, but none with matching kid hint.", args("jwks", publicJwks, "kid", headerKeyID, "kty", headerKty, tokenName, token));
					}

					// Multiple keys matched, including the kid hint.
					if (headerKeyID != null && numberOfValidKeysWithKid > 1) {
						throw error("Found multiple keys in JWKS of the correct type and with the same kid hint.", args("jwks", publicJwks, "kid", headerKeyID, "kty", headerKty, tokenName, token));
					}

					// Multiple keys matched, no kid hint.
					if (headerKeyID == null && numberOfValidKeys > 1) {
						throw error("Found multiple keys in JWKS of the correct type but no kid hint in JWE header.", args("jwks", publicJwks, "kty", headerKty, tokenName, token));
					}

					// Single key matched, kid hint does not match.
					if (headerKeyID != null && numberOfValidKeys == 1 && numberOfValidKeysWithKid==0) {
						throw error("Single key in JWKS of the correct type, but does not match kid hint.", args("jwks", publicJwks, "kid", headerKeyID, "kty", headerKty, tokenName, token));
					}

					// The encryption has been verified.
					return true;
				}
			}

			// The encryption algorithm, if any, was not asymmetric.
			return false;
		} catch (ParseException e) {
			throw error("Error validating " + tokenName + " encryption", e);
		}
	}
}
