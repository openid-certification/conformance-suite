package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.SecretJWK;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractLenientJwksCondition;
import net.openid.conformance.extensions.AlternateJWSVerificationKeySelector;
import net.openid.conformance.util.JWAUtil;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractVerifyJwsSignature extends AbstractLenientJwksCondition {

	protected void verifyJwsSignature(String token, JsonObject publicJwks, String tokenName, boolean kidRequired, String jwksName) {
		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(token);
			// parse leniently: skip keys the JOSE library cannot handle (e.g. unsupported curves
			// like Brainpool, or future PQ algorithms) so an unusable key elsewhere in the set does
			// not abort verification when a usable signing key is present; skipped keys are logged
			JWKSet jwkSet = parseJwksLenientlyLoggingSkips(publicJwks.toString(), jwksName);
			JWKSet jwkSetWithKeyValid = null;

			JWSHeader header = jwt.getHeader();
			String headerKeyID = header.getKeyID();
			JWSAlgorithm headerJwsAlg = header.getAlgorithm();
			String headerAlg = headerJwsAlg != null ? headerJwsAlg.getName() : null;
			// Only a registered JWS signature or MAC algorithm names a key that could verify this
			// signature. KeyType.forAlgorithm() is not a sufficient test on its own: it maps the JWE
			// key management algorithms onto a key type too ('dir' and 'A128KW' to oct, 'RSA-OAEP-256'
			// to RSA), and it returns null for an unregistered name, which used to be dereferenced
			// below and crash the test with an internal error instead of failing the condition.
			if (!JWAUtil.isJwsAlgorithm(headerAlg)) {
				throw error("The '" + tokenName + "' JWS header 'alg' is not a registered JWS signature or MAC "
					+ "algorithm, so its signature cannot be verified", args("alg", headerAlg, tokenName, token));
			}
			String headerKty = KeyType.forAlgorithm(headerJwsAlg).getValue();
			Base64URL headerX509CertSha256Thumbprint = header.getX509CertSHA256Thumbprint();

			int numberOfKeyValid = 0;
			for (JWK jwkKey : jwkSet.getKeys()) {
				if ((headerKeyID != null && headerKeyID.equals(jwkKey.getKeyID()))
					&& (jwkKey.getAlgorithm() != null && jwkKey.getAlgorithm().getName().equals(headerAlg))
					&& KeyUse.SIGNATURE.equals(jwkKey.getKeyUse())
					&& jwkKey.getKeyType().getValue().equals(headerKty)) {

					numberOfKeyValid++;
					if (numberOfKeyValid > 1) {
						throw error("Found more than one key in "+jwksName+" JWKS that has the right kid, kty, alg and 'use':'sig'", args("jwks", publicJwks, "kid", headerKeyID, "alg", headerAlg, "kty", headerKty, tokenName, token));
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
						if (verifySignatureWithSelectedKey(jwt, jwkKey)) {
							// save key which is able to verify
							jwkSetWithKeyValid = new JWKSet(jwkKey);
							break;
						} else {
							throw error("Unable to verify "+tokenName+" signature based on "+jwksName+" key with the correct kid, kty that also matches (or does not have) alg/x5t#S256/'use':'sig'", args("jwks", publicJwks, "kid", headerKeyID, "alg", headerAlg, "kty", headerKty, tokenName, token));
						}
					}
				}
				if (key == null) {
					throw error(jwksName+" JWKS does not contain a key with the correct kid, kty that also matches (or does not have) alg/x5t#S256/'use':'sig'", args("jwks", publicJwks, "kid", headerKeyID, "alg", headerAlg, "kty", headerKty, tokenName, token));
				}
			} else {
				// if a kid isn't given
				if (kidRequired) {
					throw error("kid value in JWT header is missing/null/empty");
				}
				boolean validSignature = false;
				for (JWK jwkKey : jwkSet.getKeys()) {

					if (!isSelectedJWKKeyBaseOnJWSHeader(headerAlg, headerKty, headerX509CertSha256Thumbprint, jwkKey)) {
						continue;
					}

					key = jwkKey;
					if (verifySignatureWithSelectedKey(jwt, jwkKey)) {
						// save key which is able to verify
						jwkSetWithKeyValid = new JWKSet(jwkKey);
						validSignature = true;
						break;
					}
				}
				if (key == null) {
					throw error(jwksName + " JWKS does not contain a key with the correct kty that also matches (or does not have) alg/x5t#S256/'use':'sig'", args("jwks", publicJwks, "kid", headerKeyID, "alg", headerAlg, "kty", headerKty, tokenName, token));
				}
				if (!validSignature) {
					throw error("Unable to verify "+tokenName+" signature based on "+jwksName+" keys", args("jwks", publicJwks, tokenName, token));
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

		// filter by 'x5t#S256' (if present in the JWS header): every thumbprint the key carries must match
		if (headerX509CertSha256Thumbprint != null) {
			for (Base64URL keyX509CertSha256Thumbprint : x509CertSha256Thumbprints(jwkKey)) {
				if (!headerX509CertSha256Thumbprint.equals(keyX509CertSha256Thumbprint)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * The certificate thumbprints a key carries: its 'x5t#S256', and the SHA-256 thumbprint of the first
	 * certificate in its 'x5c'. Empty for a bare key, which an 'x5t#S256' JWS header cannot rule out.
	 */
	private static List<Base64URL> x509CertSha256Thumbprints(JWK jwkKey) {
		List<Base64URL> thumbprints = new ArrayList<>();
		if (jwkKey.getX509CertSHA256Thumbprint() != null) {
			thumbprints.add(jwkKey.getX509CertSHA256Thumbprint());
		}
		List<X509Certificate> certChain = jwkKey.getParsedX509CertChain();
		if (certChain != null && !certChain.isEmpty()) {
			thumbprints.add(X509CertUtils.computeSHA256Thumbprint(certChain.get(0)));
		}
		return thumbprints;
	}

	/**
	 * Verifies the signature with a key {@link #isSelectedJWKKeyBaseOnJWSHeader} has already selected.
	 * The Nimbus key matcher in {@link #verifySignature(SignedJWT, JWKSet, JWSHeader)} rejects a key
	 * that carries no certificate thumbprint whenever the header has an x5t#S256, so for such a key the
	 * header thumbprint is left out of the matching; the signature is still verified over the JWT's own
	 * header.
	 */
	private boolean verifySignatureWithSelectedKey(SignedJWT jwt, JWK key) throws JOSEException {
		JWSHeader keySelectionHeader = jwt.getHeader();
		if (keySelectionHeader.getX509CertSHA256Thumbprint() != null && x509CertSha256Thumbprints(key).isEmpty()) {
			keySelectionHeader = new JWSHeader.Builder(keySelectionHeader).x509CertSHA256Thumbprint(null).build();
		}
		return verifySignature(jwt, new JWKSet(key), keySelectionHeader);
	}

	protected boolean verifySignature(SignedJWT jwt, JWKSet jwkSet) throws JOSEException {
		return verifySignature(jwt, jwkSet, jwt.getHeader());
	}

	private boolean verifySignature(SignedJWT jwt, JWKSet jwkSet, JWSHeader keySelectionHeader) throws JOSEException {
		SecurityContext context = new SimpleSecurityContext();

		JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

		AlternateJWSVerificationKeySelector<SecurityContext> selector = new AlternateJWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);

		List<JWK> jwkKeys = selector.selectJWSJwks(keySelectionHeader, context);
		JWSVerifierFactory factory = new DefaultJWSVerifierFactory();

		for(JWK jwkKey : jwkKeys) {
			JWSVerifier verifier = null;
			try {
				if (jwkKey instanceof OctetKeyPair) {
					OctetKeyPair publicKey = OctetKeyPair.parse(jwkKey.toPublicJWK().toString());
					if (Curve.Ed25519.equals(publicKey.getCurve())) {
						verifier = new Ed25519Verifier(publicKey);
					}  // else Unsupported Curve, throw exception?
				} else if (jwkKey instanceof AsymmetricJWK asyncJwkKey) {
					KeyPair keyPair = asyncJwkKey.toKeyPair();
					verifier = factory.createJWSVerifier(jwt.getHeader(), keyPair.getPublic());
				} else if (jwkKey instanceof SecretJWK secretJwkKey) {
					verifier = factory.createJWSVerifier(jwt.getHeader(), secretJwkKey.toSecretKey());
				}
			} catch (JOSEException | ParseException e) {

			}
			if (verifier != null) {
				if (jwt.verify(verifier)) {
					return true;
				} else {
					// failed to verify with this key, moving on
					// not a failure yet as it might pass a different key
				}
			}
		}
		// if we got here, it hasn't been verified on any key
		return false;
	}

	protected boolean verifyHMACSignature(SignedJWT jwt, String sharedSecret) throws JOSEException {
		JWSVerifier verifier = new MACVerifier(sharedSecret);
		return jwt.verify(verifier);
	}


}
