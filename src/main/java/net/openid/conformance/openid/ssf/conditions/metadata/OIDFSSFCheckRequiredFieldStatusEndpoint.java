package net.openid.conformance.openid.ssf.conditions.metadata;

public class OIDFSSFCheckRequiredFieldStatusEndpoint extends AbstractOIDFSSFRequiredFieldCheck {

	@Override
	protected String getRequiredFieldName() {
		return "status_endpoint";
	}
}
