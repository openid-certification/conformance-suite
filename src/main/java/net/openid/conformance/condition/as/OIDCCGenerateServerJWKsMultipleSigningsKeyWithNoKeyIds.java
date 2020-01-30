package net.openid.conformance.condition.as;

public class OIDCCGenerateServerJWKsMultipleSigningsKeyWithNoKeyIds extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters() {
		this.setGenerateKids(false);
		this.setNumberOfRSASigningKeysWithNoAlg(3);
		this.setNumberOfECSigningKeysWithNoAlg(3);
	}
}
