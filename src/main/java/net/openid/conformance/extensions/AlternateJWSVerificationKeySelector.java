package net.openid.conformance.extensions;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.SecretJWK;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;

import java.util.ArrayList;
import java.util.Collections;
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
	@SuppressWarnings("MixedMutabilityReturnType")
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
		List<JWK> sanitizedJWKs = new ArrayList<>();
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
