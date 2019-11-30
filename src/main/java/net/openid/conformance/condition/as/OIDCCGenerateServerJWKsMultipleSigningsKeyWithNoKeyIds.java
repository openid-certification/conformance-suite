package net.openid.conformance.condition.as;

public class OIDCCGenerateServerJWKsMultipleSigningsKeyWithNoKeyIds extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters()
	{
		this.setGenerateKids(false);
		this.setSigningKeyTypeToUse(KeyTypeToUse.RSA);
		this.setNumberOfRSASigningKeys(3);
	}
}
