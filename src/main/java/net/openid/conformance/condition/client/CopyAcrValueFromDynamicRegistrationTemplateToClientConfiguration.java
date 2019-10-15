package net.openid.conformance.condition.client;

public class CopyAcrValueFromDynamicRegistrationTemplateToClientConfiguration extends AbstractCopyConfigFromDynamicRegistrationTemplateToClientConfiguration {

	@Override
	protected String getExpectedConfigName() {
		return "acr_value";
	}
}
