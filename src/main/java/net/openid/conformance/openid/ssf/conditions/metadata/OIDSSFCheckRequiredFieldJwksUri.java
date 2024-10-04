package net.openid.conformance.openid.ssf.conditions.metadata;

public class OIDSSFCheckRequiredFieldJwksUri extends AbstractOIDSSFRequiredFieldCheck {
	@Override

	protected String getRequiredFieldName() {
		return "jwks_uri";
	}
}
