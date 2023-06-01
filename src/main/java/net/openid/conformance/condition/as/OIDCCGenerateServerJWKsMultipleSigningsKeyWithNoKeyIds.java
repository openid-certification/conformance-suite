package net.openid.conformance.condition.as;

public class OIDCCGenerateServerJWKsMultipleSigningsKeyWithNoKeyIds extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters() {
		this.setGenerateSigKids(false);
		this.setNumberOfRSASigningKeysWithNoAlg(3);
		this.setNumberOfECCurveP256SigningKeysWithNoAlg(3);
		this.setNumberOfECCurveSECP256KSigningKeysWithNoAlg(3);
		this.setNumberOfOKPSigningKeysWithNoAlg(3);
	}
}
