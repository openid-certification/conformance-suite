package net.openid.conformance.openid.ssf.conditions.metadata;

public class OIDFSSFCheckRequiredFieldJwksUri extends AbstractOIDFSSFRequiredFieldCheck {
	@Override

	protected String getRequiredFieldName() {
		return "jwks_uri";
	}
}
