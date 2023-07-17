package net.openid.conformance.condition.as;

public class OIDCCGenerateServerJWKsSingleSigningKeyWithNoKeyId extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters()
	{
		this.setGenerateSigKids(false);
		this.setNumberOfRSASigningKeysWithNoAlg(1);
		this.setNumberOfECCurveP256SigningKeysWithNoAlg(1);
		this.setNumberOfECCurveSECP256KSigningKeysWithNoAlg(1);
		this.setNumberOfOKPSigningKeysWithNoAlg(1);
	}
}
