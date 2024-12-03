package net.openid.conformance.openid.ssf.conditions.metadata;

public class OIDFSSFCheckRequiredFieldConfigurationEndpoint extends AbstractOIDFSSFRequiredFieldCheck {

	@Override
	protected String getRequiredFieldName() {
		return "configuration_endpoint";
	}
}
