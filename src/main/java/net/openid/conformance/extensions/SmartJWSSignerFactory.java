package net.openid.conformance.extensions;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.produce.JWSSignerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class SmartJWSSignerFactory implements JWSSignerFactory {

	private static final Logger LOG = LoggerFactory.getLogger(SmartJWSSignerFactory.class);
	private static final SmartJWSSignerFactory INSTANCE = new SmartJWSSignerFactory();

	private JWSSignerFactory delegate = new DefaultJWSSignerFactory();
	private Set<AlternateJWSSignerFactory> alternateJWSSigners = new HashSet<>();

	@Override
	public JWSSigner createJWSSigner(JWK key) throws JOSEException {
		return delegate.createJWSSigner(key);
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
		for(AlternateJWSSignerFactory alternate: alternateJWSSigners) {
			if(alternate.willUse(key)) {
				return alternate.createJWSSigner(key, alg);
			}
		}
		return delegate.createJWSSigner(key, alg);
	}

	/**
	 * Returns the names of the supported algorithms by the JWS provider
	 * instance. These correspond to the {@code alg} JWS header parameter.
	 *
	 * @return The supported JWS algorithms, empty set if none.
	 */
	@Override
	public Set<JWSAlgorithm> supportedJWSAlgorithms() {
		return delegate.supportedJWSAlgorithms();
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
		return delegate.getJCAContext();
	}

	public static SmartJWSSignerFactory getInstance() {
		return INSTANCE;
	}

	public void register(AlternateJWSSignerFactory signerFactory) {
		LOG.info("JWS signer factory {} regisered", signerFactory);
		alternateJWSSigners.add(signerFactory);
	}
}
