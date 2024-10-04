package net.openid.conformance.openid.ssf.conditions.metadata;

public class OIDSSFCheckRequiredFieldStatusEndpoint extends AbstractOIDSSFRequiredFieldCheck {

	@Override
	protected String getRequiredFieldName() {
		return "status_endpoint";
	}
}
