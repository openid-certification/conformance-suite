package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

public abstract class AbstractVerifyJwsSignatureUsingKid extends AbstractVerifyJwsSignature {

	@Override
	protected void verifyJwsSignature(String token, JsonObject publicJwks, String tokenName) {
		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(token);
			JWKSet jwkSet = JWKSet.parse(publicJwks.toString());
			JWKSet jwkSetWithKeyValid = null;

			JWSHeader header = jwt.getHeader();
			String headerKeyID = header.getKeyID();
			String headerAlg = header.getAlgorithm() != null ? header.getAlgorithm().getName() : null;
			String headerKty = KeyType.forAlgorithm(new Algorithm(headerAlg)).getValue();
			Base64URL headerX509CertSha256Thumbprint = header.getX509CertSHA256Thumbprint();

			int numberOfKeyValid = 0;
			for (JWK jwkKey : jwkSet.getKeys()) {
				if ((headerKeyID != null && headerKeyID.equals(jwkKey.getKeyID()))
					&& (jwkKey.getAlgorithm() != null && jwkKey.getAlgorithm().getName().equals(headerAlg))
					&& KeyUse.SIGNATURE.equals(jwkKey.getKeyUse())
					&& jwkKey.getKeyType().getValue().equals(headerKty)) {

					numberOfKeyValid++;
					if (numberOfKeyValid > 1) {
						throw error("Found more than one key that has the right kid, kty, alg and 'use':'sig'", args("jwks", publicJwks, "kid", headerKeyID, "alg", headerAlg, "kty", headerKty, tokenName, token));
					}
				}
			}

			JWK key = null;
			// if a kid is given
			if (!Strings.isNullOrEmpty(headerKeyID)) {
				for (JWK jwkKey : jwkSet.getKeys()) {
					if (headerKeyID.equals(jwkKey.getKeyID())) {

						if (!isSelectedJWKKeyBaseOnJWSHeader(headerAlg, headerKty, headerX509CertSha256Thumbprint, jwkKey)) {
							continue;
						}

						key = jwkKey;
						if (verifySignature(jwt, new JWKSet(jwkKey))) {
							// save key which is able to verify
							jwkSetWithKeyValid = new JWKSet(jwkKey);
							break;
						} else {
							throw error("Unable to verify "+tokenName+" signature based on server key with the correct kid, kty that also matches (or does not have) alg/x5t#S256/'use':'sig'", args("jwks", publicJwks, "kid", headerKeyID, "alg", headerAlg, "kty", headerKty, tokenName, token));
						}
					}
				}
				if (key == null) {
					throw error("Server JWKS does not contain a key with the correct kid, kty that also matches (or does not have) alg/x5t#S256/'use':'sig'", args("jwks", publicJwks, "kid", headerKeyID, "alg", headerAlg, "kty", headerKty, tokenName, token));
				}
			} else {
				// if a kid isn't given
				boolean validSignature = false;
				for (JWK jwkKey : jwkSet.getKeys()) {

					if (!isSelectedJWKKeyBaseOnJWSHeader(headerAlg, headerKty, headerX509CertSha256Thumbprint, jwkKey)) {
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
					throw error("Server JWKS does not contain a key with the correct kty that also matches (or does not have) alg/x5t#S256/'use':'sig'", args("jwks", publicJwks, "kid", headerKeyID, "alg", headerAlg, "kty", headerKty, tokenName, token));
				}
				if (!validSignature) {
					throw error("Unable to verify "+tokenName+" signature based on server keys", args("jwks", publicJwks, tokenName, token));
				}
			}

			String publicKeySetString = jwkSetWithKeyValid.toPublicJWKSet().getKeys().size() > 0 ? jwkSetWithKeyValid.toPublicJWKSet().getKeys().iterator().next().toString() : null;
			JsonObject tokenObject = new JsonObject();
			tokenObject.addProperty("verifiable_jws", token);
			tokenObject.addProperty("public_jwk", publicKeySetString);
			logSuccess(tokenName + " signature validated", args(tokenName, tokenObject));

		} catch (JOSEException | ParseException e) {
			throw error("Error validating "+tokenName+" signature", e);
		}
	}

	private boolean isSelectedJWKKeyBaseOnJWSHeader(String headerAlg, String headerKty, Base64URL headerX509CertSha256Thumbprint, JWK jwkKey) {
		// filter by 'kty'
		if (!jwkKey.getKeyType().getValue().equals(headerKty)) {
			return false;
		}

		// filter by 'alg' if key has alg (matching the token alg)
		if (jwkKey.getAlgorithm() != null && !headerAlg.equals(jwkKey.getAlgorithm().getName())) {
			return false;
		}

		// filter by 'use: sig' (if 'use' present in server key)
		if (jwkKey.getKeyUse() != null && !KeyUse.SIGNATURE.equals(jwkKey.getKeyUse())) {
			return false;
		}

		// filter by 'x5t#S256' (if 'x5t#S256' present in JWS header)
		if (headerX509CertSha256Thumbprint != null && !headerX509CertSha256Thumbprint.toString().equals(jwkKey.getX509CertSHA256Thumbprint().toString())) {
			return false;
		}

		return true;
	}

}
