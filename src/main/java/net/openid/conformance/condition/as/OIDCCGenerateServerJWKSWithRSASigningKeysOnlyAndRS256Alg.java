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
		this.setNumberOfECSigningKeysWithNoAlg(0);
		this.setNumberOfOKPSigningKeysWithNoAlg(0);
		//note that in this test, all keys will have alg set to RS256
		this.setNumberOfRSASigningKeysWithNoAlg(0);

		this.setNumberOfRSSigningKeys(3);
		this.setRsSigningAlgorithm(JWSAlgorithm.RS256);
	}
}
