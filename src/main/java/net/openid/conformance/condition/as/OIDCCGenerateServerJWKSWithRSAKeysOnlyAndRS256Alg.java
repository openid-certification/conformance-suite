package net.openid.conformance.condition.as;

import com.nimbusds.jose.JWSAlgorithm;

/**
 * There won't be EC keys in the JWKS
 */
public class OIDCCGenerateServerJWKSWithRSAKeysOnlyAndRS256Alg extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters()
	{
		this.setSigningKeyTypeToUse(KeyTypeToUse.RSA);
		this.setNumberOfECEncKeys(0);
		this.setNumberOfECSigningKeys(0);
		this.setSigningAlgorithmForRSAKeys(JWSAlgorithm.RS256);
	}
}
