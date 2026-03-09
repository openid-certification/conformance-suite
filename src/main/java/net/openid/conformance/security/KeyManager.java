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

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.JWSAlgorithm;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import java.util.Base64;

public class KeyManager {


//	@Value("${fintechlabs.jwks}")
//	private String jwksString;

	//@Value("${fintechlabs.signingKey}")
	//private String signingKeyId;

	//@Value("${fintechlabs.privateLinkSigningKey}")
	//private String privateLinkSigningKeyId;

	@Value("${fintechlabs.signingKey:}")
	private String signingKey;

	@Value("${fintechlabs.privateLinkSigningKey:}")
	private String privateLinkSigningKey;

	private JWKSet jwkSet;
	private String signingKeyId;
	private JWK    privateLinkJWK;
	private boolean privateLinkJWKConfigured;

	private RSAKey generateRSAKey(String kid)
	{
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(2048);
			KeyPair keyPair = gen.generateKeyPair();

			return new RSAKey.Builder((RSAPublicKey)keyPair.getPublic())
				.privateKey((RSAPrivateKey)keyPair.getPrivate())
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(JWSAlgorithm.PS256)
				.keyID(kid)
				.build();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	// FIXME Need to send back message if we had to generate the key
	@PostConstruct
	public void initializeKeyManager() {
		List<JWK> keyList = new ArrayList<>();
		JWK jwk = null;

		if (signingKey.isEmpty()) {
			signingKeyId = UUID.randomUUID().toString();

			// Generate a signing key if none configured.
			jwk = generateRSAKey(signingKeyId);

			if (jwk == null) {
				throw new IllegalStateException("Couldn't create signing key");
			}
		}
		else {
			// Decode the Base64 encoded signing key and add to the key set.
			try {
				String decodedKey = new String(Base64.getDecoder().decode(signingKey));

				jwk = JWK.parse(decodedKey);
				signingKeyId = jwk.getKeyID();
				keyList.add(jwk);
			} catch (IllegalArgumentException | ParseException e) {
				throw new IllegalStateException("Couldn't decode signing key");
			}
		}

		// Add this key to the key set for publcation at the 'jwks' endpoint.
		keyList.add(jwk);

		// Create the key set.
		jwkSet = new JWKSet(keyList);

		if (privateLinkSigningKey.isEmpty()) {
			// Generate a private link signing key if none configured.
			privateLinkJWK = generateRSAKey(UUID.randomUUID().toString());

			if (privateLinkJWK == null) {
				throw new IllegalStateException("Couldn't create private link signing key");
			}

			privateLinkJWKConfigured = false;
		}
		else {
			// Decode the Base64 encoded private link signing key.
			try {
				String decodedKey = new String(Base64.getDecoder().decode(privateLinkSigningKey));

				privateLinkJWK = JWK.parse(decodedKey);
			} catch (IllegalArgumentException | ParseException e) {
				throw new IllegalStateException("Couldn't dsecode private link signing key");
			}

			privateLinkJWKConfigured = true;
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

	public boolean privateLinkKeyWasConfigured() {
		return privateLinkJWKConfigured;
	}

	public JWK getPrivateLinkKey() {
		return privateLinkJWK;
	}

}
