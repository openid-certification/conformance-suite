package net.openid.conformance.condition.as;

import com.nimbusds.jose.JWSAlgorithm;

/**
 * There won't be EC keys in the JWKS
 */
public class OIDCCGenerateServerJWKSWithRSASigningKeysOnlyAndRS256Alg extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters() {
		this.setNumberOfECEncKeys(2);
		this.setNumberOfRSAEncKeys(2);
		this.setNumberOfESSigningKeys(0);
		this.setNumberOfEdSigningKeys(0);
		this.setNumberOfPSSigningKeys(0);
		this.setRsSigningAlgorithm(JWSAlgorithm.RS256);
	}
}
