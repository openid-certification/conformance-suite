package net.openid.conformance.condition.client;

public class CopyScopeFromDynamicRegistrationTemplateToClientConfiguration extends AbstractCopyConfigFromDynamicRegistrationTemplateToClientConfiguration {

	@Override
	protected String getExpectedConfigName() {
		return "scope";
	}
}
