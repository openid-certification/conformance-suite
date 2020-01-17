package net.openid.conformance.condition.as;

public class OIDCCGenerateServerJWKsMultipleSigningsKeyWithNoKeyIds extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters()
	{
		this.setGenerateKids(false);
		this.setNumberOfRSSigningKeys(3);
		this.setNumberOfESSigningKeys(3);
		this.setNumberOfPSSigningKeys(3);
		this.setNumberOfEdSigningKeys(3);
	}
}
