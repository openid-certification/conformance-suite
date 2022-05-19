package net.openid.conformance.extensions;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import software.amazon.awssdk.services.kms.KmsClient;

import java.util.Set;

public class KmsJWSSignerFactory implements AlternateJWSSignerFactory {

	private KmsClient kmsClient;

	public KmsJWSSignerFactory(KmsClient kmsClient) {
		this.kmsClient = kmsClient;
	}

	@Override
	public boolean willUse(JWK jwk) {
		if(!(jwk instanceof RSAKey)) {
			return false;
		}
		RSAKey rsaKey = (RSAKey) jwk;
		Base64URL privateExponent = rsaKey.getPrivateExponent();
		if(privateExponent == null) {
			return false;
		}

		String exp = privateExponent.decodeToString();
		return exp.startsWith("alias");
	}

	/**
	 * Create a JWS signer based on the key.
	 *
	 * @param key
	 */
	@Override
	public JWSSigner createJWSSigner(JWK key) throws JOSEException {
		return new KmsJWSSigner(kmsClient, key);
	}

	/**
	 * Create a JWS signer based on the key and algorithm. Ensures
	 * that the key supports the given algorithm.
	 *
	 * @param key
	 * @param alg
	 */
	@Override
	public JWSSigner createJWSSigner(JWK key, JWSAlgorithm alg) throws JOSEException {
		return new KmsJWSSigner(kmsClient, key);
	}

	/**
	 * Returns the names of the supported algorithms by the JWS provider
	 * instance. These correspond to the {@code alg} JWS header parameter.
	 *
	 * @return The supported JWS algorithms, empty set if none.
	 */
	@Override
	public Set<JWSAlgorithm> supportedJWSAlgorithms() {
		return null;
	}

	/**
	 * Returns the Java Cryptography Architecture (JCA) context. May be
	 * used to set a specific JCA security provider or secure random
	 * generator.
	 *
	 * @return The JCA context. Not {@code null}.
	 */
	@Override
	public JCAContext getJCAContext() {
		return null;
	}
}
