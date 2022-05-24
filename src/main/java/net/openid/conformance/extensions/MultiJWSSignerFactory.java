package net.openid.conformance.extensions;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.produce.JWSSignerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * This class allows alternative JWS Signers, such as those using private keys
 * backed by an HSM or cloud service to be used.
 */
public class MultiJWSSignerFactory implements JWSSignerFactory {

	private static MultiJWSSignerFactory INSTANCE = new MultiJWSSignerFactory();

	private Set<AlternateJWSSignerFactory> alternateJWSSigners = new HashSet<>();
	private JWSSignerFactory defaultFactory = new DefaultJWSSignerFactory();

	public static JWSSignerFactory getInstance() {
		return INSTANCE;
	}

	@Override
	public JWSSigner createJWSSigner(JWK jwk) throws JOSEException {
		for(AlternateJWSSignerFactory alternate: alternateJWSSigners) {
			if(alternate.canSignWith(jwk)) {
				return alternate.createJWSSigner(jwk);
			}
		}
		return defaultFactory.createJWSSigner(jwk);
	}

	@Override
	public JWSSigner createJWSSigner(JWK jwk, JWSAlgorithm jwsAlgorithm) throws JOSEException {
		for(AlternateJWSSignerFactory alternate: alternateJWSSigners) {
			if(alternate.canSignWith(jwk)) {
				return alternate.createJWSSigner(jwk, jwsAlgorithm);
			}
		}
		return defaultFactory.createJWSSigner(jwk, jwsAlgorithm);
	}

	@Override
	public Set<JWSAlgorithm> supportedJWSAlgorithms() {
		return defaultFactory.supportedJWSAlgorithms();
	}

	@Override
	public JCAContext getJCAContext() {
		return defaultFactory.getJCAContext();
	}

	/**
	 * Hook for registering an alternative
	 * @param factory
	 */
	public void registerAlternative(AlternateJWSSignerFactory factory) {
		alternateJWSSigners.add(factory);
	}

}
