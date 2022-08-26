package net.openid.conformance.extensions;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.SecretJWK;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.proc.SecurityContext;

import java.security.KeyPair;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


public class AlternateJWSVerificationKeySelector<C extends SecurityContext> extends JWSVerificationKeySelector<C>  {

	// local copy of jwsAlgs in JWSVerificationKeySelector due to private scope
	private final Set<JWSAlgorithm> jwsAlgs;

	public AlternateJWSVerificationKeySelector(final JWSAlgorithm jwsAlg, final JWKSource<C> jwkSource){
		super(jwsAlg, jwkSource);
		this.jwsAlgs = Collections.singleton(jwsAlg);
	}

	public AlternateJWSVerificationKeySelector(final Set<JWSAlgorithm> jwsAlgs, final JWKSource<C> jwkSource) {
		super(jwsAlgs, jwkSource);
		this.jwsAlgs = jwsAlgs;
	}

	// Modified code from super.selectJWSKeys to return JWSVerifier instead of Key due to
	// EdDSA OctetKeyPair throwing a not implemented exception for toKeyPair()
	public List<JWK> selectJWSJwks(JWSHeader jwsHeader, final C context) throws JOSEException {
		if (! jwsAlgs.contains(jwsHeader.getAlgorithm())) {
			// Unexpected JWS alg
			return Collections.emptyList();
		}

		JWKMatcher jwkMatcher = createJWKMatcher(jwsHeader);
		if (jwkMatcher == null) {
			return Collections.emptyList();
		}

		List<JWK> jwkMatches = getJWKSource().get(new JWKSelector(jwkMatcher), context);
		// Get non-EdDSA keys from original function
		List<JWK> sanitizedJWKs = new LinkedList<>();

//		for (Key key: KeyConverter.toJavaKeys(jwkMatches)) {
//			if (key instanceof PublicKey || key instanceof SecretKey) {
//				sanitizedKeyList.add(key);
//			} // skip asymmetric private keys
//		}

//		public static List<Key> toJavaKeys(final List<JWK> jwkList) {
//
//			if (jwkList == null) {
//				return Collections.emptyList();
//			}
//
//			List<Key> out = new LinkedList<>();
//			for (JWK jwk: jwkList) {
//				try {
//					if (jwk instanceof AsymmetricJWK) {
//						KeyPair keyPair = ((AsymmetricJWK)jwk).toKeyPair();
//						out.add(keyPair.getPublic()); // add public
//						if (keyPair.getPrivate() != null) {
//							out.add(keyPair.getPrivate()); // add private if present
//						}
//					} else if (jwk instanceof SecretJWK) {
//						out.add(((SecretJWK)jwk).toSecretKey());
//					}
//				} catch (JOSEException e) {
//					// ignore and continue
//				}
//			}
//			return out;
//		}

		for(JWK jwk: jwkMatches) {
			if (jwk instanceof AsymmetricJWK) {
				sanitizedJWKs.add(jwk.toPublicJWK());
				if(jwk.isPrivate()) {
					sanitizedJWKs.add(jwk);
				}
			} else if (jwk instanceof SecretJWK) {
				sanitizedJWKs.add(jwk);
			}
		}
		return sanitizedJWKs;
	}

}
