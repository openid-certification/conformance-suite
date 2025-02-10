package net.openid.conformance.openid.ssf.mock;

import net.openid.conformance.condition.as.OIDCCGenerateServerJWKs;

public class OIDSSFGenerateServerJWKs extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters() {

		setNumberOfOKPSigningKeysWithNoAlg(0);
		setNumberOfRSASigningKeysWithNoAlg(0);
		setNumberOfECCurveP256SigningKeysWithNoAlg(0);
		setNumberOfECCurveSECP256KSigningKeysWithNoAlg(0);

		setNumberOfECEncKeys(0);
		setNumberOfRSAEncKeys(0);

		setNumberOfRSSigningKeys(1);
		setNumberOfES256SigningKeys(1);

		setGenerateEncKids(false);
		setGenerateSigKids(true);
	}
}
