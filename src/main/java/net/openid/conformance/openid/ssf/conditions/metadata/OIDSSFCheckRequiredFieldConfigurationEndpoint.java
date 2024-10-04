package net.openid.conformance.openid.ssf.conditions.metadata;

public class OIDSSFCheckRequiredFieldConfigurationEndpoint extends AbstractOIDSSFRequiredFieldCheck {

	@Override
	protected String getRequiredFieldName() {
		return "configuration_endpoint";
	}
}
