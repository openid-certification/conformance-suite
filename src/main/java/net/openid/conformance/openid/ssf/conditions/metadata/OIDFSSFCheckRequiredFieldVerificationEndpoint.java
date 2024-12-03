package net.openid.conformance.openid.ssf.conditions.metadata;

public class OIDFSSFCheckRequiredFieldVerificationEndpoint extends AbstractOIDFSSFRequiredFieldCheck {

	@Override
	protected String getRequiredFieldName() {
		return "verification_endpoint";
	}
}
