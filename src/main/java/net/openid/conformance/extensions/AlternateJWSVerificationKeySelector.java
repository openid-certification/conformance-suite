package net.openid.conformance.extensions;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyConverter;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.SecretJWK;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.proc.SecurityContext;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
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

	// Modified code from super.selectJWSKeys to return JWSVerifier instead of Key due to
	// EdDSA OctetKeyPair throwing a not implemented exception for toKeyPair()
	public List<JWSVerifier> selectJWSVerifiers(JWSHeader jwsHeader, final C context) throws JOSEException {
		List<JWK> matchedJWKs = selectJWSJwks(jwsHeader, context);
		List<JWSVerifier> sanitizedVerifiers = new LinkedList<>();

		JWSVerifierFactory factory = new DefaultJWSVerifierFactory();

		for(JWK jwk : matchedJWKs) {
			try {
				if(jwk instanceof OctetKeyPair) {
					sanitizedVerifiers.add(null);
					OctetKeyPair publicKey = OctetKeyPair.parse(jwk.toPublicJWK().toString());
					if(Curve.Ed25519.equals(publicKey.getCurve())) {
						sanitizedVerifiers.add(new Ed25519Verifier(publicKey));
					} else {
						// Unsupported Curve
					}
				} else if (jwk instanceof AsymmetricJWK) {
					KeyPair keyPair = ((AsymmetricJWK)jwk).toKeyPair();
					sanitizedVerifiers.add(factory.createJWSVerifier(jwsHeader, keyPair.getPublic()));
				} else if (jwk instanceof SecretJWK) {
					sanitizedVerifiers.add(factory.createJWSVerifier(jwsHeader, ((SecretJWK)jwk).toSecretKey()));
				}
			} catch (JOSEException | ParseException e) {
				// ignore and continue
			}
		}

		return sanitizedVerifiers;
	}


//	// Modified code from super.selectJWSKeys to return JWSVerifier instead of Key due to
//	// EdDSA OctetKeyPair throwing a not implemented exception for toKeyPair()
//	public List<JWSVerifier> selectJWSVerifiers(JWSHeader jwsHeader, final C context) throws JOSEException, ParseException {
//		if (! jwsAlgs.contains(jwsHeader.getAlgorithm())) {
//			// Unexpected JWS alg
//			return Collections.emptyList();
//		}
//
//		JWKMatcher jwkMatcher = createJWKMatcher(jwsHeader);
//		if (jwkMatcher == null) {
//			return Collections.emptyList();
//		}
//
//		List<JWK> jwkMatches = getJWKSource().get(new JWKSelector(jwkMatcher), context);
//		// Get non-EdDSA keys from original function
//		List<Key> sanitizedKeyList = selectJWSKeys(jwsHeader, context);
//
//		List<JWSVerifier> sanitizedVerifiers = new LinkedList<>();
//
//		JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
//		for(Key key: sanitizedKeyList) {
//			JWSVerifier verifier = factory.createJWSVerifier(jwsHeader, key);
//			sanitizedVerifiers.add(verifier);
//		}
//
//		// Get Verifiers for EdDSA Keys
//		for (JWK jwkKey: jwkMatches) {
//			if(jwkKey instanceof OctetKeyPair ) {
//				OctetKeyPair publicKey = OctetKeyPair.parse(jwkKey.toPublicJWK().toString());
//				if(Curve.Ed25519.equals(publicKey.getCurve())) {
//					sanitizedVerifiers.add(new Ed25519Verifier(publicKey));
//				} else {
//					// Unsupported Curve
//				}
//			}
//		}
//		return sanitizedVerifiers;
//	}




//		@Override
//	public List<Key> selectJWSKeys(JWSHeader jwsHeader, SecurityContext context) throws KeySourceException {
//		if (! jwsAlgs.contains(jwsHeader.getAlgorithm())) {
//			// Unexpected JWS alg
//			return Collections.emptyList();
//		}
//
//		JWKMatcher jwkMatcher = createJWKMatcher(jwsHeader);
//		if (jwkMatcher == null) {
//			return Collections.emptyList();
//		}
//
//		List<JWK> jwkMatches = getJWKSource().get(new JWKSelector(jwkMatcher), context);
//
//		List<Key> sanitizedKeyList = new LinkedList<>();
//
//		for (Key key: KeyConverter.toJavaKeys(jwkMatches)) {
//			if (key instanceof PublicKey || key instanceof SecretKey) {
//				sanitizedKeyList.add(key);
//			} // skip asymmetric private keys
//		}
//
//		return sanitizedKeyList;
//	}

}
