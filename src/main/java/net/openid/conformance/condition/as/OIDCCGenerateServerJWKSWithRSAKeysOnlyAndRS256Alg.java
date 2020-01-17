package net.openid.conformance.condition.as;

import com.nimbusds.jose.JWSAlgorithm;

/**
 * There won't be EC keys in the JWKS
 */
public class OIDCCGenerateServerJWKSWithRSAKeysOnlyAndRS256Alg extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters()
	{
		this.setNumberOfECEncKeys(0);
		this.setNumberOfESSigningKeys(0);
		this.setNumberOfEdSigningKeys(0);
		this.setNumberOfPSSigningKeys(0);
		this.setRsSigningAlgorithm(JWSAlgorithm.RS256);
	}
}
