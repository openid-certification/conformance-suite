package net.openid.conformance.openid.ssf.conditions.metadata;

public class OIDSSFCheckRequiredFieldVerificationEndpoint extends AbstractOIDSSFRequiredFieldCheck {

	@Override
	protected String getRequiredFieldName() {
		return "verification_endpoint";
	}
}
