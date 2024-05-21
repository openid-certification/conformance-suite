package net.openid.conformance.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.security.PrivateKey;
import java.text.ParseException;

public class KeyManager {


	@Value("${fintechlabs.jwks}")
	private String jwksString;

	@Value("${fintechlabs.signingKey}")
	private String signingKeyId;

	private JWKSet jwkSet;

	@PostConstruct
	public void initializeKeyManager() {
		// parse the string as a JWK Set
		try {
			jwkSet = JWKSet.parse(jwksString);

			// make sure the jwkSet has a key with the indicated ID
			JWK jwk = jwkSet.getKeyByKeyId(signingKeyId);

			if (jwk == null) {
				throw new IllegalStateException("Couldn't find the signing key " + signingKeyId);
			}

		} catch (ParseException e) {
			throw new IllegalStateException("Error trying to build a JWK Set", e);
		}
	}

	public PrivateKey getSigningPrivateKey() {
		JWK signingKey = jwkSet.getKeyByKeyId(signingKeyId);
		KeyType keyType = signingKey.getKeyType();

		try {
			if (keyType.equals(KeyType.RSA)) {
				return ((RSAKey)signingKey).toPrivateKey();
			} else if (keyType.equals(KeyType.EC)) {
				return ((ECKey)signingKey).toPrivateKey();
			} else if (keyType.equals(KeyType.OKP)) {
				return ((OctetKeyPair)signingKey).toPrivateKey();
			} else {
				return null;
			}
		} catch (JOSEException e) {
			return null;
		}
	}

	/**
	 * Get only the public keys in this key set.
	 */
	public JWKSet getPublicKeys() {
		return jwkSet.toPublicJWKSet();
	}

}
